package com.kelasin.app.data.supabase

import com.kelasin.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SupabaseRestClient {

    private data class HttpResult(
        val statusCode: Int,
        val body: String
    )

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
        }
    }

    @Volatile
    private var accessToken: String? = null

    private val hasValidConfig: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    fun setAccessToken(token: String?) {
        accessToken = token?.trim()?.takeIf { it.isNotBlank() }
    }

    fun clearAccessToken() {
        accessToken = null
    }

    suspend fun checkConnection(): Boolean {
        selectRows(
            table = "users",
            selectColumns = "id",
            limit = 1
        )
        return true
    }

    suspend fun signUp(email: String, password: String, metadata: JSONObject? = null): JSONObject {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
        if (metadata != null && metadata.length() > 0) {
            payload.put("data", metadata)
        }
        val body = payload.toString()
        val result = executeAuth(
            method = HttpMethod.Post,
            path = "/auth/v1/signup",
            requestBody = body
        )
        ensureSuccess(result, "sign up")
        return JSONObject(result.body)
    }

    suspend fun signInWithPassword(email: String, password: String): JSONObject {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
        val result = executeAuth(
            method = HttpMethod.Post,
            path = "/auth/v1/token?grant_type=password",
            requestBody = body
        )
        ensureSuccess(result, "sign in")
        return JSONObject(result.body)
    }

    suspend fun refreshSession(refreshToken: String): JSONObject {
        val body = JSONObject()
            .put("refresh_token", refreshToken)
            .toString()
        val result = executeAuth(
            method = HttpMethod.Post,
            path = "/auth/v1/token?grant_type=refresh_token",
            requestBody = body
        )
        ensureSuccess(result, "refresh session")
        return JSONObject(result.body)
    }

    suspend fun getCurrentAuthUser(token: String? = null): JSONObject {
        val bearerToken = token?.trim()?.takeIf { it.isNotBlank() } ?: accessToken
            ?: throw IllegalStateException("Access token kosong. Tidak bisa mengambil user auth saat ini.")
        val result = executeAuth(
            method = HttpMethod.Get,
            path = "/auth/v1/user",
            bearerToken = bearerToken
        )
        ensureSuccess(result, "fetch current auth user")
        return JSONObject(result.body)
    }

    suspend fun selectRows(
        table: String,
        filters: List<Pair<String, String>> = emptyList(),
        order: String? = null,
        limit: Int? = null,
        selectColumns: String = "*"
    ): List<JSONObject> {
        val query = buildList {
            add("select" to selectColumns)
            addAll(filters)
            if (!order.isNullOrBlank()) add("order" to order)
            if (limit != null) add("limit" to limit.toString())
        }

        val result = execute(
            method = HttpMethod.Get,
            table = table,
            queryParams = query
        )
        ensureSuccess(result, "select from $table")
        return parseRows(result.body, "select from $table")
    }

    suspend fun insertRow(table: String, payload: JSONObject): JSONObject {
        val result = execute(
            method = HttpMethod.Post,
            table = table,
            queryParams = emptyList(),
            requestBody = payload.toString(),
            returnRepresentation = true
        )
        ensureSuccess(result, "insert into $table")
        val rows = parseRows(result.body, "insert into $table")
        return rows.firstOrNull()
            ?: throw IllegalStateException("Insert into $table succeeded but returned no row")
    }

    suspend fun upsertRow(
        table: String,
        payload: JSONObject,
        onConflict: String
    ): JSONObject {
        val result = execute(
            method = HttpMethod.Post,
            table = table,
            queryParams = listOf("on_conflict" to onConflict),
            requestBody = payload.toString(),
            returnRepresentation = true,
            preferDirectives = listOf("resolution=merge-duplicates")
        )
        ensureSuccess(result, "upsert into $table")
        val rows = parseRows(result.body, "upsert into $table")
        return rows.firstOrNull()
            ?: throw IllegalStateException("Upsert into $table succeeded but returned no row")
    }

    suspend fun insertRows(table: String, payload: JSONArray): List<JSONObject> {
        if (payload.length() == 0) return emptyList()
        val result = execute(
            method = HttpMethod.Post,
            table = table,
            queryParams = emptyList(),
            requestBody = payload.toString(),
            returnRepresentation = true
        )
        ensureSuccess(result, "bulk insert into $table")
        return parseRows(result.body, "bulk insert into $table")
    }

    suspend fun updateRow(
        table: String,
        payload: JSONObject,
        filters: List<Pair<String, String>>
    ): JSONObject? {
        require(filters.isNotEmpty()) { "Update $table requires at least one filter" }
        val result = execute(
            method = HttpMethod.Patch,
            table = table,
            queryParams = filters,
            requestBody = payload.toString(),
            returnRepresentation = true
        )
        ensureSuccess(result, "update $table")
        return parseRows(result.body, "update $table").firstOrNull()
    }

    suspend fun deleteRows(
        table: String,
        filters: List<Pair<String, String>>
    ): List<JSONObject> {
        require(filters.isNotEmpty()) { "Delete $table requires at least one filter" }
        val result = execute(
            method = HttpMethod.Delete,
            table = table,
            queryParams = filters,
            requestBody = null,
            returnRepresentation = true
        )
        ensureSuccess(result, "delete from $table")
        return parseRows(result.body, "delete from $table")
    }

    private suspend fun execute(
        method: HttpMethod,
        table: String,
        queryParams: List<Pair<String, String>>,
        requestBody: String? = null,
        returnRepresentation: Boolean = false,
        preferDirectives: List<String> = emptyList()
    ): HttpResult {
        ensureConfig()
        val endpoint = buildEndpoint(table, queryParams)
        val response = client.request(endpoint) {
            this.method = method
            header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            header(
                HttpHeaders.Authorization,
                "Bearer ${accessToken ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY}"
            )
            accept(ContentType.Application.Json)
            val preferValues = buildList {
                if (returnRepresentation) add("return=representation")
                addAll(preferDirectives)
            }
            if (preferValues.isNotEmpty()) {
                header("Prefer", preferValues.joinToString(","))
            }
            if (requestBody != null) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(requestBody)
            }
        }
        return HttpResult(
            statusCode = response.status.value,
            body = response.bodyAsText()
        )
    }

    private suspend fun executeAuth(
        method: HttpMethod,
        path: String,
        requestBody: String? = null,
        bearerToken: String? = null
    ): HttpResult {
        ensureConfig()
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val endpoint = "$baseUrl$path"
        val response = client.request(endpoint) {
            this.method = method
            header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            header(
                HttpHeaders.Authorization,
                "Bearer ${bearerToken ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY}"
            )
            accept(ContentType.Application.Json)
            if (requestBody != null) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(requestBody)
            }
        }
        return HttpResult(
            statusCode = response.status.value,
            body = response.bodyAsText()
        )
    }

    private fun parseRows(rawBody: String, action: String): List<JSONObject> {
        val body = rawBody.trim()
        if (body.isBlank()) return emptyList()

        return when {
            body.startsWith("[") -> {
                val array = JSONArray(body)
                List(array.length()) { index -> array.getJSONObject(index) }
            }

            body.startsWith("{") -> listOf(JSONObject(body))
            else -> throw IllegalStateException("Unexpected Supabase response while $action: $body")
        }
    }

    private fun ensureSuccess(result: HttpResult, action: String) {
        if (result.statusCode !in 200..299) {
            val body = if (result.body.isBlank()) "<empty>" else result.body
            throw IllegalStateException("Supabase failed to $action (HTTP ${result.statusCode}): $body")
        }
    }

    private fun ensureConfig() {
        if (!hasValidConfig) {
            throw IllegalStateException("Supabase config belum lengkap. Cek SUPABASE_URL dan SUPABASE_PUBLISHABLE_KEY.")
        }
    }

    private fun buildEndpoint(table: String, queryParams: List<Pair<String, String>>): String {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val endpoint = "$baseUrl/rest/v1/$table"
        if (queryParams.isEmpty()) return endpoint

        val query = queryParams.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$endpoint?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

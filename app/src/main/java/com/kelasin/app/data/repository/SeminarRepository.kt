package com.kelasin.app.data.repository

import com.kelasin.app.data.supabase.SupabaseRestClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class SeminarItem(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val speaker: String,
    val quota: Int,
    val category: String,
    val createdAt: Long,
    val colorHex: String? = null,
    val postMessage: String? = null,
    val isSelectionEnabled: Boolean = false,
    val customQuestions: String? = null, // Stored as JSON string
    // Realtime count (fetched separately via JOIN)
    val registeredCount: Int = 0
) {
    val remainingQuota: Int get() = (quota - registeredCount).coerceAtLeast(0)
    val isFull: Boolean get() = registeredCount >= quota
}

internal fun nowIsoSeminar(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

internal fun parseIsoSeminar(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val clean = value.replace(Regex("(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?$"), "")
        sdf.parse(clean)?.time ?: 0L
    }.getOrDefault(0L)
}

class SeminarRepository {

    /** Fetch all seminars with their registration counts. */
    suspend fun getSeminars(): Result<List<SeminarItem>> = runCatching {
        val rows = SupabaseRestClient.selectRows(
            table = "seminars",
            order = "created_at.asc",
            selectColumns = "id,title,description,date,location,speaker,quota,category,created_at,color_hex,post_message,is_selection_enabled,custom_questions"
        )
        val seminars = rows.map { obj -> obj.toSeminarItem() }

        // Fetch registration counts in one call
        val counts = getRegistrationCounts().getOrDefault(emptyMap())
        seminars.map { s -> s.copy(registeredCount = counts[s.id] ?: 0) }
    }

    /** Fetch a single seminar by id with registration count. */
    suspend fun getSeminarById(id: String): Result<SeminarItem> = runCatching {
        val rows = SupabaseRestClient.selectRows(
            table = "seminars",
            filters = listOf("id" to "eq.$id"),
            selectColumns = "id,title,description,date,location,speaker,quota,category,created_at,color_hex,post_message,is_selection_enabled,custom_questions"
        )
        val item = rows.firstOrNull()?.toSeminarItem()
            ?: throw IllegalStateException("Seminar not found")
        val count = getRegistrationCountForSeminar(id).getOrDefault(0)
        item.copy(registeredCount = count)
    }

    /** Returns map of seminarId -> count of registrations */
    suspend fun getRegistrationCounts(): Result<Map<String, Int>> = runCatching {
        val rows = SupabaseRestClient.selectRows(
            table = "seminar_registrations",
            selectColumns = "seminar_id"
        )
        rows.mapNotNull { it.optString("seminar_id", null).takeIf { s -> !s.isNullOrBlank() } }
            .groupingBy { it }.eachCount()
    }

    suspend fun getRegistrationCountForSeminar(seminarId: String): Result<Int> = runCatching {
        val rows = SupabaseRestClient.selectRows(
            table = "seminar_registrations",
            filters = listOf("seminar_id" to "eq.$seminarId"),
            selectColumns = "id"
        )
        rows.size
    }

    suspend fun insertSeminar(
        title: String,
        description: String,
        date: String,
        location: String,
        speaker: String,
        quota: Int,
        category: String,
        colorHex: String? = null,
        postMessage: String? = null,
        isSelectionEnabled: Boolean = false,
        customQuestions: String? = null
    ): Result<SeminarItem> = runCatching {
        val body = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("title", title)
            put("description", description)
            put("date", date)
            put("location", location)
            put("speaker", speaker)
            put("quota", quota)
            put("category", category)
            if (colorHex != null) put("color_hex", colorHex)
            if (postMessage != null) put("post_message", postMessage)
            put("is_selection_enabled", isSelectionEnabled)
            if (customQuestions != null) put("custom_questions", org.json.JSONArray(customQuestions))
            put("created_at", nowIsoSeminar())
        }
        val row = SupabaseRestClient.insertRow("seminars", body)
        row.toSeminarItem()
    }

    suspend fun updateSeminar(
        id: String,
        title: String,
        description: String,
        date: String,
        location: String,
        speaker: String,
        quota: Int,
        category: String,
        colorHex: String? = null,
        postMessage: String? = null,
        isSelectionEnabled: Boolean = false,
        customQuestions: String? = null
    ): Result<Unit> = runCatching {
        val body = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("date", date)
            put("location", location)
            put("speaker", speaker)
            put("quota", quota)
            put("category", category)
            if (colorHex != null) put("color_hex", colorHex) else put("color_hex", JSONObject.NULL)
            if (postMessage != null) put("post_message", postMessage) else put("post_message", JSONObject.NULL)
            put("is_selection_enabled", isSelectionEnabled)
            if (customQuestions != null) put("custom_questions", org.json.JSONArray(customQuestions)) else put("custom_questions", JSONObject.NULL)
        }
        SupabaseRestClient.updateRow(
            table = "seminars",
            payload = body,
            filters = listOf("id" to "eq.$id")
        )
        Unit
    }

    suspend fun deleteSeminar(id: String): Result<Unit> = runCatching {
        SupabaseRestClient.deleteRows(
            table = "seminars",
            filters = listOf("id" to "eq.$id")
        )
        Unit
    }

    private fun JSONObject.toSeminarItem() = SeminarItem(
        id = optString("id", ""),
        title = optString("title", "-"),
        description = optString("description", "-"),
        date = optString("date", "-"),
        location = optString("location", "-"),
        speaker = optString("speaker", "-"),
        quota = optInt("quota", 0),
        category = optString("category", "-"),
        colorHex = optString("color_hex", null).takeIf { it != "null" && it.isNotBlank() },
        postMessage = optString("post_message", null).takeIf { it != "null" && it.isNotBlank() },
        isSelectionEnabled = optBoolean("is_selection_enabled", false),
        customQuestions = optJSONArray("custom_questions")?.toString() ?: optString("custom_questions", null).takeIf { it != "null" && it.isNotBlank() },
        createdAt = parseIsoSeminar(optString("created_at", null))
    )
}

package com.kelasin.app.data.repository

import com.kelasin.app.data.supabase.SupabaseRestClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class SeminarRegistrationPayload(
    val nama: String,
    val email: String,
    val nomorHp: String,
    val jenisKelamin: String,
    val seminar: String,
    val seminarId: String? = null,
    val customAnswers: String? = null // Stored as JSON string
)

data class RegistrationHistoryItem(
    val id: String,
    val userId: String,
    val nama: String,
    val email: String,
    val nomorHp: String,
    val jenisKelamin: String,
    val seminar: String,
    val seminarId: String,
    val createdAt: Long,
    val selectionStatus: String,
    val customAnswers: String? = null,
    val colorHex: String? = null,
    val isSelectionEnabled: Boolean = false
)

private fun nowIso8601(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

private fun parseIso8601(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val clean = value.replace(Regex("(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?$"), "")
        sdf.parse(clean)?.time ?: 0L
    }.getOrDefault(0L)
}

class SeminarRegistrationRepository {

    suspend fun submitRegistration(
        userId: String?,
        payload: SeminarRegistrationPayload
    ): Result<Unit> = runCatching {
        val trimmedUserId = userId?.trim()
        val body = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("nama", payload.nama)
            put("email", payload.email)
            put("nomor_hp", payload.nomorHp)
            put("jenis_kelamin", payload.jenisKelamin)
            put("seminar", payload.seminar)
            put("created_at", nowIso8601())
            if (!trimmedUserId.isNullOrEmpty()) {
                put("user_id", trimmedUserId)
            }
            if (!payload.seminarId.isNullOrEmpty()) {
                put("seminar_id", payload.seminarId)
            }
            put("selection_status", "Belum diseleksi")
            if (payload.customAnswers != null) {
                put("custom_answers", org.json.JSONObject(payload.customAnswers))
            }
        }
        SupabaseRestClient.insertRow(table = "seminar_registrations", payload = body)
        Unit
    }

    /** Check if a user already registered for a seminar */
    suspend fun hasUserRegistered(userId: String, seminarId: String): Result<Boolean> =
        runCatching {
            if (userId.isBlank() || seminarId.isBlank()) return@runCatching false
            val rows = SupabaseRestClient.selectRows(
                table = "seminar_registrations",
                filters = listOf("user_id" to "eq.$userId", "seminar_id" to "eq.$seminarId"),
                selectColumns = "id",
                limit = 1
            )
            rows.isNotEmpty()
        }

    suspend fun getRegistrationsByUser(userId: String): Result<List<RegistrationHistoryItem>> =
        runCatching {
            if (userId.isBlank()) return@runCatching emptyList()
            SupabaseRestClient.selectRows(
                table = "seminar_registrations",
                filters = listOf("user_id" to "eq.$userId"),
                order = "created_at.desc",
                selectColumns = "id,user_id,nama,email,nomor_hp,jenis_kelamin,seminar,seminar_id,created_at,selection_status,custom_answers,seminars(color_hex,is_selection_enabled)"
            ).map { it.toHistoryItem() }
        }

    suspend fun getAllRegistrations(): Result<List<RegistrationHistoryItem>> =
        runCatching {
            SupabaseRestClient.selectRows(
                table = "seminar_registrations",
                order = "created_at.desc",
                selectColumns = "id,user_id,nama,email,nomor_hp,jenis_kelamin,seminar,seminar_id,created_at,selection_status,custom_answers,seminars(color_hex,is_selection_enabled)"
            ).map { it.toHistoryItem() }
        }

    suspend fun getRegistrationsBySeminarId(seminarId: String): Result<List<RegistrationHistoryItem>> =
        runCatching {
            SupabaseRestClient.selectRows(
                table = "seminar_registrations",
                filters = listOf("seminar_id" to "eq.$seminarId"),
                order = "created_at.desc",
                selectColumns = "id,user_id,nama,email,nomor_hp,jenis_kelamin,seminar,seminar_id,created_at,selection_status,custom_answers,seminars(color_hex,is_selection_enabled)"
            ).map { it.toHistoryItem() }
        }

    suspend fun deleteRegistration(id: String): Result<Unit> = runCatching {
        SupabaseRestClient.deleteRows(
            table = "seminar_registrations",
            filters = listOf("id" to "eq.$id")
        )
        Unit
    }

    suspend fun updateSelectionStatus(id: String, status: String): Result<Unit> = runCatching {
        val payload = JSONObject().apply { put("selection_status", status) }
        SupabaseRestClient.updateRow(
            table = "seminar_registrations",
            payload = payload,
            filters = listOf("id" to "eq.$id")
        )
        Unit
    }

    private fun JSONObject.toHistoryItem() = RegistrationHistoryItem(
        id = optString("id", ""),
        userId = optString("user_id", ""),
        nama = optString("nama", "-"),
        email = optString("email", "-"),
        nomorHp = optString("nomor_hp", "-"),
        jenisKelamin = optString("jenis_kelamin", "-"),
        seminar = optString("seminar", "-"),
        seminarId = optString("seminar_id", ""),
        selectionStatus = optString("selection_status", "Belum diseleksi"),
        customAnswers = optJSONObject("custom_answers")?.toString() ?: optString("custom_answers", null).takeIf { it != "null" && it.isNotBlank() },
        createdAt = parseIso8601(optString("created_at", null)),
        colorHex = optJSONObject("seminars")?.optString("color_hex", null)?.takeIf { it != "null" && it.isNotBlank() },
        isSelectionEnabled = optJSONObject("seminars")?.optBoolean("is_selection_enabled", false) ?: false
    )
}

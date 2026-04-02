package com.kelasin.app.data.supabase

import com.kelasin.app.data.entity.AbsensiEntity
import com.kelasin.app.data.entity.CatatanEntity
import com.kelasin.app.data.entity.ChatMessageEntity
import com.kelasin.app.data.entity.MahasiswaEntity
import com.kelasin.app.data.entity.MataKuliahEntity
import com.kelasin.app.data.entity.MateriEntity
import com.kelasin.app.data.entity.Prioritas
import com.kelasin.app.data.entity.StatusAbsensi
import com.kelasin.app.data.entity.StatusTugas
import com.kelasin.app.data.entity.TipeMateri
import com.kelasin.app.data.entity.TugasEntity
import com.kelasin.app.data.entity.UserEntity
import org.json.JSONObject

fun JSONObject.toUserEntity(): UserEntity = UserEntity(
    id = stringValue("id"),
    nama = stringValue("nama"),
    email = stringValue("email"),
    username = stringValue("username"),
    profilePicUrl = stringValue("profile_pic_url"),
    passwordHash = stringValue("password_hash"),
    role = stringValue("role").ifBlank { "MAHASISWA" },
    createdAt = longValue("created_at", System.currentTimeMillis())
)

fun UserEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    put("nama", nama)
    put("email", email)
    put("username", username)
    put("profile_pic_url", profilePicUrl)
    put("password_hash", passwordHash)
    put("role", role)
    put("created_at", createdAt)
}

fun JSONObject.toMataKuliahEntity(): MataKuliahEntity = MataKuliahEntity(
    id = longValue("id"),
    nama = stringValue("nama"),
    kode = stringValue("kode"),
    dosen = stringValue("dosen"),
    sks = intValue("sks", 0),
    hari = stringValue("hari"),
    jamMulai = stringValue("jam_mulai"),
    jamSelesai = stringValue("jam_selesai"),
    ruangan = stringValue("ruangan"),
    emailDosen = stringValue("email_dosen"),
    noHpDosen = stringValue("no_hp_dosen"),
    warna = stringValue("warna", "#6750A4"),
    userId = stringValue("user_id")
)

fun MataKuliahEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    put("nama", nama)
    put("kode", kode)
    put("dosen", dosen)
    put("sks", sks)
    put("hari", hari)
    put("jam_mulai", jamMulai)
    put("jam_selesai", jamSelesai)
    put("ruangan", ruangan)
    put("email_dosen", emailDosen)
    put("no_hp_dosen", noHpDosen)
    put("warna", warna)
    put("user_id", userId)
}

fun JSONObject.toTugasEntity(): TugasEntity = TugasEntity(
    id = longValue("id"),
    mataKuliahId = longValue("mata_kuliah_id"),
    judul = stringValue("judul"),
    deskripsi = stringValue("deskripsi"),
    deadline = longValue("deadline"),
    prioritas = enumOrDefault(stringValue("prioritas"), Prioritas.SEDANG),
    status = enumOrDefault(stringValue("status"), StatusTugas.BELUM),
    pertemuanKe = intValue("pertemuan_ke", 1),
    fileUri = stringValue("file_uri"),
    linkUrl = stringValue("link_url"),
    reminderEnabled = booleanValue("reminder_enabled", true),
    userId = stringValue("user_id"),
    createdAt = longValue("created_at", System.currentTimeMillis())
)

fun TugasEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    put("mata_kuliah_id", mataKuliahId)
    put("judul", judul)
    put("deskripsi", deskripsi)
    put("deadline", deadline)
    put("prioritas", prioritas.name)
    put("status", status.name)
    put("pertemuan_ke", pertemuanKe)
    put("file_uri", fileUri)
    put("link_url", linkUrl)
    put("reminder_enabled", reminderEnabled)
    put("user_id", userId)
    put("created_at", createdAt)
}

fun JSONObject.toCatatanEntity(): CatatanEntity = CatatanEntity(
    id = longValue("id"),
    mataKuliahId = nullableLongValue("mata_kuliah_id"),
    labelKustom = stringValue("label_kustom"),
    judul = stringValue("judul"),
    isi = stringValue("isi"),
    tag = stringValue("tag"),
    fileUri = stringValue("file_uri"),
    userId = stringValue("user_id"),
    updatedAt = longValue("updated_at", System.currentTimeMillis()),
    isMuted = booleanValue("is_muted", false)
)

fun CatatanEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    if (mataKuliahId == null) {
        put("mata_kuliah_id", JSONObject.NULL)
    } else {
        put("mata_kuliah_id", mataKuliahId)
    }
    put("label_kustom", labelKustom)
    put("judul", judul)
    put("isi", isi)
    put("tag", tag)
    put("file_uri", fileUri)
    put("user_id", userId)
    put("updated_at", updatedAt)
    put("is_muted", isMuted)
}

fun JSONObject.toChatMessageEntity(): ChatMessageEntity = ChatMessageEntity(
    id = longValue("id"),
    roomId = longValue("room_id"),
    senderUserId = stringValue("sender_user_id"),
    senderName = stringValue("sender_name"),
    text = stringValue("text"),
    createdAt = longValue("created_at", System.currentTimeMillis()),
    status = stringValue("status", "SENT"),
    isEdited = booleanValue("is_edited", false),
    senderProfilePic = stringValue("sender_profile_pic")
)

fun ChatMessageEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    put("room_id", roomId)
    put("sender_user_id", senderUserId)
    put("sender_name", senderName)
    put("text", text)
    put("created_at", createdAt)
    put("status", status)
    put("is_edited", isEdited)
    put("sender_profile_pic", senderProfilePic)
}

fun JSONObject.toMateriEntity(): MateriEntity = MateriEntity(
    id = longValue("id"),
    mataKuliahId = longValue("mata_kuliah_id"),
    judul = stringValue("judul"),
    deskripsi = stringValue("deskripsi"),
    tipe = enumOrDefault(stringValue("tipe"), TipeMateri.LINK),
    url = stringValue("url"),
    fileUri = stringValue("file_uri"),
    isBookmarked = booleanValue("is_bookmarked", false),
    userId = stringValue("user_id"),
    createdAt = longValue("created_at", System.currentTimeMillis())
)

fun MateriEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    put("mata_kuliah_id", mataKuliahId)
    put("judul", judul)
    put("deskripsi", deskripsi)
    put("tipe", tipe.name)
    put("url", url)
    put("file_uri", fileUri)
    put("is_bookmarked", isBookmarked)
    put("user_id", userId)
    put("created_at", createdAt)
}

fun JSONObject.toAbsensiEntity(): AbsensiEntity = AbsensiEntity(
    id = longValue("id"),
    mataKuliahId = longValue("mata_kuliah_id"),
    mahasiswaId = longValue("mahasiswa_id"),
    tanggal = longValue("tanggal"),
    status = enumOrDefault(stringValue("status"), StatusAbsensi.BELUM_DIPILIH),
    pertemuanKe = intValue("pertemuan_ke", 1),
    keterangan = stringValue("keterangan"),
    userId = stringValue("user_id")
)

fun AbsensiEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    put("mata_kuliah_id", mataKuliahId)
    put("mahasiswa_id", mahasiswaId)
    put("tanggal", tanggal)
    put("status", status.name)
    put("pertemuan_ke", pertemuanKe)
    put("keterangan", keterangan)
    put("user_id", userId)
}

fun JSONObject.toMahasiswaEntity(): MahasiswaEntity = MahasiswaEntity(
    id = longValue("id"),
    nama = stringValue("nama")
)

fun MahasiswaEntity.toSupabaseJson(includeId: Boolean = false): JSONObject = JSONObject().apply {
    if (includeId) put("id", id)
    put("nama", nama)
}

private fun JSONObject.stringValue(key: String, default: String = ""): String {
    if (!has(key) || isNull(key)) return default
    return optString(key, default)
}

private fun JSONObject.longValue(key: String, default: Long = 0L): Long {
    if (!has(key) || isNull(key)) return default
    val raw = get(key)
    return when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull() ?: default
        else -> default
    }
}

private fun JSONObject.nullableLongValue(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    val raw = get(key)
    return when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }
}

private fun JSONObject.intValue(key: String, default: Int = 0): Int {
    if (!has(key) || isNull(key)) return default
    val raw = get(key)
    return when (raw) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull() ?: default
        else -> default
    }
}

private fun JSONObject.booleanValue(key: String, default: Boolean = false): Boolean {
    if (!has(key) || isNull(key)) return default
    val raw = get(key)
    return when (raw) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is String -> raw.equals("true", ignoreCase = true) || raw == "1"
        else -> default
    }
}

private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, default: T): T {
    if (raw.isBlank()) return default
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
}

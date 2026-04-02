package com.kelasin.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kelasin.app.data.KelasinDatabase
import com.kelasin.app.data.entity.UserEntity
import com.kelasin.app.data.supabase.SupabaseRestClient
import com.kelasin.app.data.supabase.toUserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "kelasin_prefs")

class AuthRepository(private val context: Context) {
    private val tag = "AuthRepository"
    private val localUserDao by lazy { KelasinDatabase.getInstance(context).userDao() }

    companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_ROLE = stringPreferencesKey("user_role")
    }

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    val currentUserName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: ""
    }

    val currentUserRole: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ROLE] ?: "MAHASIGMA"
    }

    suspend fun register(
        nama: String,
        username: String,
        email: String,
        password: String,
        role: String
    ): Result<UserEntity> {
        val trimmedName = nama.trim()
        val normalizedEmail = email.trim().lowercase()
        val normalizedUsername = username.trim().lowercase()
        if (trimmedName.isBlank() || normalizedUsername.isBlank() || normalizedEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama, username, email, dan password wajib diisi"))
        }

        return runCatching {
            val existingByEmailCloud = fetchUserCloudByEmail(normalizedEmail)
            val existingByUsernameCloud = fetchUserCloudByUsername(normalizedUsername)
            val existingByEmailLocal = localUserDao.findByEmail(normalizedEmail)
            val existingByUsernameLocal = localUserDao.findByUsername(normalizedUsername)
            val existingUser = existingByEmailCloud
                ?: existingByUsernameCloud
                ?: existingByEmailLocal
                ?: existingByUsernameLocal

            if (existingByEmailCloud != null || existingByEmailLocal != null) {
                throw IllegalStateException("Email $normalizedEmail sudah terdaftar. Silakan gunakan email lain.")
            }
            if (existingByUsernameCloud != null || existingByUsernameLocal != null) {
                throw IllegalStateException("Username $normalizedUsername sudah dipakai. Silakan pilih username lain.")
            }

            val entity = UserEntity(
                id = java.util.UUID.randomUUID().toString(),
                nama = trimmedName,
                email = normalizedEmail,
                username = normalizedUsername,
                passwordHash = password.sha256(),
                role = role,
                createdAt = System.currentTimeMillis()
            )

            localUserDao.upsert(entity)
            upsertUserCloud(entity)
            saveSession(entity)
            entity
        }.onFailure {
            Log.e(tag, "Register failed", it)
        }
    }

    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        val normalizedIdentifier = identifier.trim().lowercase()
        if (normalizedIdentifier.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email/username dan password wajib diisi"))
        }

        return runCatching {
            // INTERCEPT ATMIN
            if (normalizedIdentifier == "mahesa" && password == "123anjay") {
                var atminUser = localUserDao.findByIdentifier("mahesa") ?: fetchAndCacheUserCloud("mahesa")
                
                if (atminUser == null) {
                    atminUser = UserEntity(
                        id = SharedCloudScope.USER_ID,
                        nama = "Atmin Kelasin",
                        email = "mahesa@kelasin.com",
                        username = "mahesa",
                        passwordHash = "123anjay".sha256(),
                        role = "ATMIN", // Not ADMIN, but ATMIN as requested
                        createdAt = System.currentTimeMillis()
                    )
                    localUserDao.upsert(atminUser)
                    runCatching { upsertUserCloud(atminUser) }
                } else if (atminUser.role != "ATMIN" || atminUser.passwordHash != "123anjay".sha256()) {
                    atminUser = atminUser.copy(role = "ATMIN", passwordHash = "123anjay".sha256())
                    localUserDao.upsert(atminUser)
                    runCatching { upsertUserCloud(atminUser) }
                }
                
                saveSession(atminUser)
                return@runCatching atminUser
            }

            val localUser = localUserDao.findByIdentifier(normalizedIdentifier)
            val user = localUser ?: fetchAndCacheUserCloud(normalizedIdentifier)
                ?: throw IllegalStateException("Akun tidak ditemukan")

            if (user.passwordHash.isBlank()) {
                throw IllegalStateException("Akun ini belum punya password. Silakan daftar ulang.")
            }
            if (user.passwordHash != password.sha256()) {
                throw IllegalStateException("Password salah")
            }

            // Re-fetch from cloud on every login to ensure profile data is always fresh
            val freshUser = runCatching { fetchAndCacheUserByIdCloud(user.id) }.getOrNull() ?: user
            // Preserve password hash if cloud row doesn't have it (cloud may omit it)
            val finalUser = if (freshUser.passwordHash.isBlank()) freshUser.copy(passwordHash = user.passwordHash) else freshUser
            localUserDao.upsert(finalUser)

            saveSession(finalUser)
            finalUser
        }.onFailure {
            Log.e(tag, "Login failed", it)
        }
    }

    suspend fun restoreSessionToken() {
        val prefs = context.dataStore.data.first()
        val userId = prefs[KEY_USER_ID]?.trim().orEmpty()
        if (userId.isBlank()) {
            clearPersistedSession()
            return
        }

        runCatching {
            val user = fetchAndCacheUserByIdCloud(userId) ?: localUserDao.findById(userId)
            if (user == null) {
                // If it's the legacy shared ID, we might need to handle it or just clear
                if (userId == SharedCloudScope.USER_ID) {
                    clearPersistedSession()
                    return
                }
                clearPersistedSession()
                return
            }
            // Update session if needed
            context.dataStore.edit { next ->
                next[KEY_USER_ID] = user.id
                next[KEY_USER_NAME] = user.nama.ifBlank { SharedCloudScope.USER_NAME }
                next[KEY_USER_ROLE] = user.role
            }
        }.onFailure {
            Log.e(tag, "restoreSession failed", it)
            // DON'T clear session on network failure, keep whatever we have
        }
    }

    suspend fun logout() {
        runCatching {
            clearPersistedSession()
        }.onFailure {
            Log.e(tag, "Logout failed", it)
        }
    }
    
    suspend fun getUserProfile(userId: String): UserEntity? {
        return fetchAndCacheUserByIdCloud(userId) ?: localUserDao.findById(userId)
    }

    suspend fun updateProfile(userId: String, newNama: String, newUsername: String, profilePicUrl: String? = null, newBio: String = ""): Result<UserEntity> {
        return runCatching {
            val local = localUserDao.findById(userId) ?: throw IllegalStateException("User tidak ditemukan")
            val normalizedName = newNama.trim()
            val normalizedUsername = newUsername.trim().lowercase()
            if (normalizedName.isBlank()) throw IllegalArgumentException("Nama tidak boleh kosong")
            if (normalizedUsername.isBlank()) throw IllegalArgumentException("Username tidak boleh kosong")
            
            // Validate username is not taken by someone else
            if (normalizedUsername != local.username) {
                val existing = fetchUserCloudByUsername(normalizedUsername) ?: localUserDao.findByUsername(normalizedUsername)
                if (existing != null && existing.id != userId) {
                    throw IllegalStateException("Username sudah dipakai orang lain")
                }
            }
            val picBase = profilePicUrl ?: local.displayProfilePic
            val combinedProfilePicUrl = if (newBio.isNotBlank()) "$picBase[BIO]${newBio.trim()}" else picBase
            
            val updated = local.copy(
                nama = normalizedName,
                username = normalizedUsername,
                profilePicUrl = combinedProfilePicUrl
            )
            upsertUserCloudOrThrow(updated, includePasswordHash = false)
            localUserDao.upsert(updated)
            
            // Update session data
            val prefs = context.dataStore.data.first()
            if (prefs[KEY_USER_ID] == userId || userId == SharedCloudScope.USER_ID) {
                context.dataStore.edit { next -> next[KEY_USER_NAME] = normalizedName }
            }
            updated
        }
    }

    private suspend fun saveSession(user: UserEntity) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_USER_NAME] = user.nama.ifBlank { SharedCloudScope.USER_NAME }
            prefs[KEY_USER_ROLE] = user.role
        }
    }

    private suspend fun clearPersistedSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_ROLE)
        }
    }

    private suspend fun upsertUserCloud(user: UserEntity) {
        runCatching {
            upsertUserCloudOrThrow(user, includePasswordHash = true)
        }.onFailure {
            Log.e(tag, "Cloud upsert user failed (local auth remains active)", it)
        }
    }

    private suspend fun upsertUserCloudOrThrow(user: UserEntity, includePasswordHash: Boolean) {
        var currentPayload = buildUserCloudPayload(user, includePasswordHash = includePasswordHash)
        try {
            SupabaseRestClient.upsertRow(
                table = "users",
                payload = currentPayload,
                onConflict = "id"
            )
        } catch (e: Exception) {
            val msg = e.message.orEmpty().lowercase()
            val isSchemaError = "pgrst204" in msg || "schema cache" in msg || "column" in msg
            
            if (isSchemaError) {
                // Determine which columns to strip based on the error message
                val stripPassword = "password_hash" in msg || "password hash" in msg
                val stripProfilePic = "profile_pic_url" in msg || "profile_pic" in msg
                
                if (stripPassword || stripProfilePic) {
                    val fallbackPayload = JSONObject()
                        .put("id", user.id)
                        .put("nama", user.nama)
                        .put("email", user.email)
                        .put("username", user.username)
                        .put("role", user.role)
                        .put("created_at", user.createdAt)
                    
                    if (!stripProfilePic) fallbackPayload.put("profile_pic_url", user.profilePicUrl)
                    if (includePasswordHash && !stripPassword) fallbackPayload.put("password_hash", user.passwordHash)
                    
                    SupabaseRestClient.upsertRow(
                        table = "users",
                        payload = fallbackPayload,
                        onConflict = "id"
                    )
                    return
                }
            }
            throw e
        }
    }

    private suspend fun ensureSharedCloudUser(displayName: String, role: String) {
        val username = SharedCloudScope.USER_USERNAME.take(30)
        runCatching {
            val payload = JSONObject()
                .put("id", SharedCloudScope.USER_ID)
                .put("nama", displayName.ifBlank { SharedCloudScope.USER_NAME })
                .put("email", SharedCloudScope.USER_EMAIL)
                .put("username", username)
                .put("profile_pic_url", "")
                .put("role", role.ifBlank { "MAHASIGMA" })
                .put("created_at", System.currentTimeMillis())

            SupabaseRestClient.upsertRow(
                table = "users",
                payload = payload,
                onConflict = "id"
            )
        }.onFailure {
            Log.e(tag, "Failed to ensure shared cloud user", it)
        }
    }

    private suspend fun fetchAndCacheUserCloud(identifier: String): UserEntity? {
        val entity = fetchUserCloudByEmail(identifier)
            ?: fetchUserCloudByUsername(identifier)
            ?: return null
        val local = localUserDao.findById(entity.id)
        val merged = mergeUserWithLocalPassword(entity, local)
        localUserDao.upsert(merged)
        return merged
    }

    private suspend fun fetchUserCloudByEmail(email: String): UserEntity? {
        val row = runCatching {
            SupabaseRestClient.selectRows(
                table = "users",
                filters = listOf("email" to "eq.$email"),
                limit = 1
            ).firstOrNull()
        }.getOrNull() ?: return null
        return row.toUserEntity()
    }

    private suspend fun fetchUserCloudByUsername(username: String): UserEntity? {
        val row = runCatching {
            SupabaseRestClient.selectRows(
                table = "users",
                filters = listOf("username" to "eq.$username"),
                limit = 1
            ).firstOrNull()
        }.getOrNull() ?: return null
        return row.toUserEntity()
    }

    private suspend fun fetchAndCacheUserByIdCloud(userId: String): UserEntity? {
        val row = runCatching {
            SupabaseRestClient.selectRows(
                table = "users",
                filters = listOf("id" to "eq.$userId"),
                limit = 1
            ).firstOrNull()
        }.getOrNull() ?: return null

        val entity = row.toUserEntity()
        val local = localUserDao.findById(entity.id)
        val merged = mergeUserWithLocalPassword(entity, local)
        localUserDao.upsert(merged)
        return merged
    }

    private fun mergeUserWithLocalPassword(cloud: UserEntity, local: UserEntity?): UserEntity {
        var merged = cloud
        
        // Merge password if cloud is missing it
        if (merged.passwordHash.isBlank() && local?.passwordHash?.isNotBlank() == true) {
            merged = merged.copy(passwordHash = local.passwordHash)
        }
        
        // Merge profile info if cloud is missing it (e.g. schema mismatch or lag)
        if (merged.profilePicUrl.isBlank() && local?.profilePicUrl?.isNotBlank() == true) {
            merged = merged.copy(profilePicUrl = local.profilePicUrl)
        }
        
        return merged
    }

    private fun buildUserCloudPayload(user: UserEntity, includePasswordHash: Boolean): JSONObject {
        val payload = JSONObject()
            .put("id", user.id)
            .put("nama", user.nama)
            .put("email", user.email)
            .put("username", user.username)
            .put("profile_pic_url", user.profilePicUrl)
            .put("role", user.role)
            .put("created_at", user.createdAt)
        if (includePasswordHash) {
            payload.put("password_hash", user.passwordHash)
        }
        return payload
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

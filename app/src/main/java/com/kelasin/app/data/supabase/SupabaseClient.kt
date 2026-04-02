package com.kelasin.app.data.supabase

import android.content.Context
import android.util.Log
import com.kelasin.app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    private const val TAG = "SupabaseSync"
    private const val DB_NAME = "kelasin.db"

    private val hasValidConfig: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Storage)
            install(Auth)
            install(Postgrest)
            install(Realtime)
            
            defaultSerializer = KotlinXSerializer(Json { 
                ignoreUnknownKeys = true 
                coerceInputValues = true
            })
        }
    }

    suspend fun uploadDatabaseBackup(context: Context, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!hasValidConfig) {
                Log.e(TAG, "Supabase config belum lengkap. Cek SUPABASE_URL dan SUPABASE_PUBLISHABLE_KEY.")
                return@withContext false
            }
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@withContext false
            val bytes = dbFile.readBytes()
            val bucket = client.storage["backups"]
            val filePath = "$userId/$DB_NAME"
            bucket.upload(path = filePath, data = bytes) { upsert = true }
            Log.d(TAG, "Backup uploaded to $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            false
        }
    }

    suspend fun downloadDatabaseBackup(context: Context, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!hasValidConfig) {
                Log.e(TAG, "Supabase config belum lengkap. Cek SUPABASE_URL dan SUPABASE_PUBLISHABLE_KEY.")
                return@withContext false
            }
            val bucket = client.storage["backups"]
            val filePath = "$userId/$DB_NAME"
            val bytes = bucket.downloadPublic(path = filePath)
            val dbFile = context.getDatabasePath(DB_NAME)
            dbFile.parentFile?.mkdirs()
            dbFile.writeBytes(bytes)
            Log.d(TAG, "Backup restored from $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            false
        }
    }
}

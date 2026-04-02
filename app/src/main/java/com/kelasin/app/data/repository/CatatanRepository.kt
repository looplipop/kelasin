package com.kelasin.app.data.repository

import android.content.Context
import android.util.Log
import com.kelasin.app.data.KelasinDatabase
import com.kelasin.app.data.entity.CatatanEntity
import com.kelasin.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CatatanInsertDto(
    @SerialName("mata_kuliah_id") val mataKuliahId: Long? = null,
    @SerialName("label_kustom") val labelKustom: String = "",
    @SerialName("judul") val judul: String,
    @SerialName("isi") val isi: String,
    @SerialName("tag") val tag: String = "",
    @SerialName("file_uri") val fileUri: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("is_muted") val isMuted: Boolean = false
)

class CatatanRepository(context: Context) {
    private val tag = "CatatanRepository"
    private val localDao by lazy { KelasinDatabase.getInstance(context).catatanDao() }
    private val client = SupabaseClient.client
    private val cacheByScope = mutableMapOf<String, MutableStateFlow<List<CatatanEntity>>>()
    private val seedMutexByUser = mutableMapOf<String, Mutex>()
    
    @Volatile
    private var lastKnownUserId: String? = null

    fun getAll(userId: String): Flow<List<CatatanEntity>> {
        lastKnownUserId = SharedCloudScope.USER_ID
        return state("all").onStart {
            runCatching { refreshAllRooms(SharedCloudScope.USER_ID) }
                .onFailure { Log.e(tag, "refresh getAll failed", it) }
        }
    }

    fun getByMataKuliah(userId: String, mkId: Long): Flow<List<CatatanEntity>> =
        getAll(userId).map { items ->
            items.filter { it.mataKuliahId == mkId }
                .sortedByDescending { it.updatedAt }
        }

    fun search(userId: String, query: String): Flow<List<CatatanEntity>> {
        val term = query.trim()
        if (term.isBlank()) return getAll(userId)
        return getAll(userId).map { items ->
            items.filter {
                it.judul.contains(term, ignoreCase = true) ||
                    it.isi.contains(term, ignoreCase = true) ||
                    it.tag.contains(term, ignoreCase = true)
            }.sortedByDescending { it.updatedAt }
        }
    }

    suspend fun getById(id: Long): CatatanEntity? = runCatching {
        client.postgrest["catatan"]
            .select { filter { eq("id", id) } }
            .decodeSingle<CatatanEntity>()
    }.getOrNull() ?: localDao.getById(id)

    suspend fun insert(item: CatatanEntity): Long {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        lastKnownUserId = SharedCloudScope.USER_ID
        
        // Use a DTO to exclude 'id' so Supabase can auto-generate it
        val insertPayload = CatatanInsertDto(
            mataKuliahId = sharedItem.mataKuliahId,
            labelKustom = sharedItem.labelKustom,
            judul = sharedItem.judul,
            isi = sharedItem.isi,
            tag = sharedItem.tag,
            fileUri = sharedItem.fileUri,
            userId = SharedCloudScope.USER_ID,
            updatedAt = sharedItem.updatedAt,
            isMuted = sharedItem.isMuted
        )

        val cloudResult = runCatching {
            client.postgrest["catatan"]
                .insert(insertPayload) { select() }
                .decodeSingle<CatatanEntity>()
        }
        
        val saved = if (cloudResult.isSuccess) {
            cloudResult.getOrThrow()
        } else {
            Log.e(tag, "insert cloud failed, fallback local", cloudResult.exceptionOrNull())
            // For local insert, Room handles ID 0 correctly (auto-generate)
            val localId = localDao.insert(sharedItem)
            localDao.getById(localId) ?: sharedItem.copy(id = localId)
        }
        
        runCatching { localDao.insert(saved) }
            .onFailure { Log.e(tag, "insert local cache failed", it) }
            
        return saved.id
    }

    suspend fun update(item: CatatanEntity) {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        lastKnownUserId = SharedCloudScope.USER_ID
        
        runCatching {
            client.postgrest["catatan"].update(sharedItem) {
                filter { eq("id", sharedItem.id) }
            }
        }.onFailure {
            Log.e(tag, "update cloud failed for id=${sharedItem.id}", it)
        }
        
        runCatching { localDao.update(sharedItem) }
            .onFailure { Log.e(tag, "local update failed", it) }
            
        refreshAllRooms(SharedCloudScope.USER_ID)
    }

    suspend fun delete(item: CatatanEntity) {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        lastKnownUserId = SharedCloudScope.USER_ID
        
        runCatching {
            client.postgrest["catatan"].delete {
                filter { eq("id", sharedItem.id) }
            }
        }.onFailure {
            Log.e(tag, "delete cloud failed for id=${sharedItem.id}", it)
        }
        
        runCatching { localDao.delete(sharedItem) }
            .onFailure { Log.e(tag, "local delete failed", it) }
            
        refreshAllRooms(SharedCloudScope.USER_ID)
    }

    suspend fun ensureDefaultRooms(ownerUserId: String) {
        val normalizedOwnerId = SharedCloudScope.USER_ID
        lastKnownUserId = normalizedOwnerId
        seedMutex(normalizedOwnerId).withLock {
            val defaults = defaultRooms(normalizedOwnerId)
            
            // 1. Fetch cloud data to see what's already there
            val cloudData = runCatching {
                client.postgrest["catatan"]
                    .select { filter { eq("user_id", normalizedOwnerId) } }
                    .decodeList<CatatanEntity>()
            }.getOrDefault(emptyList())
            
            val cloudTitles = cloudData.associateBy { it.judul.normalizedTitle() }

            // 2. For each default room, ensure it exists in Cloud and Local with same ID
            defaults.forEach { default ->
                val normalizedTitle = default.judul.normalizedTitle()
                val existingCloud = cloudTitles[normalizedTitle]
                
                val roomToPersist = if (existingCloud != null) {
                    existingCloud
                } else {
                    // Not in cloud, insert it
                    val insertPayload = CatatanInsertDto(
                        judul = default.judul,
                        isi = default.isi,
                        tag = default.tag,
                        userId = normalizedOwnerId,
                        updatedAt = default.updatedAt,
                        isMuted = default.isMuted
                    )
                    runCatching {
                        client.postgrest["catatan"]
                            .insert(insertPayload) { select() }
                            .decodeSingle<CatatanEntity>()
                    }.getOrDefault(default)
                }
                
                // Ensure local has it with the correct ID
                runCatching { localDao.insert(roomToPersist) }
            }
            
            refreshAllRooms(normalizedOwnerId)
        }
    }

    suspend fun refreshAllRooms() {
        val userId = lastKnownUserId ?: SharedCloudScope.USER_ID
        refreshAllRooms(userId)
    }

    private suspend fun refreshAllRooms(userId: String) {
        val cloudData = runCatching {
            client.postgrest["catatan"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<CatatanEntity>()
        }.getOrNull()

        if (cloudData != null && cloudData.isNotEmpty()) {
            val localRooms = localDao.getAll(userId).firstOrNull().orEmpty()
            val cloudIds = cloudData.map { it.id }.toSet()
            
            // Delete local rooms not in cloud
            localRooms.filter { it.id !in cloudIds }.forEach { localDao.delete(it) }

            cloudData.forEach { cloudRow ->
                // Check if we have a local room with the same title but different ID
                val existingLocalWithSameTitle = localRooms.find { it.judul.normalizedTitle() == cloudRow.judul.normalizedTitle() }
                
                if (existingLocalWithSameTitle != null && existingLocalWithSameTitle.id != cloudRow.id) {
                    localDao.delete(existingLocalWithSameTitle)
                }
                localDao.insert(cloudRow)
            }
        }

        val merged = localDao.getAll(userId).firstOrNull()
            .orEmpty()
            .sortedByDescending { it.updatedAt }

        state("all").value = merged
    }

    private fun state(scope: String): MutableStateFlow<List<CatatanEntity>> =
        synchronized(cacheByScope) {
            cacheByScope.getOrPut(scope) { MutableStateFlow(emptyList()) }
        }

    private fun seedMutex(userId: String): Mutex =
        synchronized(seedMutexByUser) {
            seedMutexByUser.getOrPut(userId) { Mutex() }
        }

    private fun defaultRooms(ownerUserId: String): List<CatatanEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CatatanEntity(
                judul = "Lobby Umum",
                isi = "Ruang obrolan umum semua pengguna.",
                tag = "umum,chat",
                userId = ownerUserId,
                updatedAt = now
            ),
            CatatanEntity(
                judul = "Info Tugas",
                isi = "Diskusi tugas dan deadline bareng.",
                tag = "tugas,deadline",
                userId = ownerUserId,
                updatedAt = now
            ),
            CatatanEntity(
                judul = "Sharing Materi",
                isi = "Share link materi atau catatan belajar.",
                tag = "materi,belajar",
                userId = ownerUserId,
                updatedAt = now
            )
        )
    }

    private fun String.normalizedTitle(): String = trim().lowercase()
}

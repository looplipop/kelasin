package com.kelasin.app.data.repository

import android.content.Context
import android.util.Log
import com.kelasin.app.data.KelasinDatabase
import com.kelasin.app.data.entity.AbsensiEntity
import com.kelasin.app.data.entity.StatusAbsensi
import com.kelasin.app.data.supabase.SupabaseClient
import com.kelasin.app.data.repository.SharedCloudScope
import org.json.JSONArray
import com.kelasin.app.data.supabase.SupabaseRestClient
import com.kelasin.app.data.supabase.toSupabaseJson
import com.kelasin.app.data.supabase.toAbsensiEntity
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class AbsensiRepository(context: Context) {
    private val tag = "AbsensiRepository"
    private val localDao by lazy { KelasinDatabase.getInstance(context).absensiDao() }
    private val cacheByUser = mutableMapOf<String, MutableStateFlow<List<AbsensiEntity>>>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val subscribedUsers = mutableSetOf<String>()

    fun getByMataKuliah(userId: String, mkId: Long): Flow<List<AbsensiEntity>> =
        getAll(userId).map { items ->
            items.filter { it.mataKuliahId == mkId }
                .sortedByDescending { it.tanggal }
        }

    fun getAll(userId: String): Flow<List<AbsensiEntity>> =
        state(userId).onStart { 
            subscribeToRealtime(userId)
            runCatching { refresh(userId) }.onFailure { Log.e(tag, "refresh getAll failed", it) } 
        }

    suspend fun countHadir(userId: String, mkId: Long): Int =
        runCatching { localDao.countHadir(SharedCloudScope.USER_ID, mkId) }
            .onFailure { Log.e(tag, "countHadir local failed user=$userId mk=$mkId", it) }
            .getOrElse {
                runCatching {
                    loadByMataKuliah(userId, mkId).count { it.status == StatusAbsensi.HADIR }
                }.getOrDefault(0)
            }

    suspend fun countTotal(userId: String, mkId: Long): Int =
        runCatching { localDao.countTotal(SharedCloudScope.USER_ID, mkId) }
            .onFailure { Log.e(tag, "countTotal local failed user=$userId mk=$mkId", it) }
            .getOrElse {
                runCatching { loadByMataKuliah(userId, mkId).size }.getOrDefault(0)
            }

    suspend fun getPersentase(userId: String, mkId: Long): Float {
        val total = countTotal(userId, mkId)
        if (total == 0) return 0f
        return countHadir(userId, mkId).toFloat() / total * 100f
    }

    suspend fun insert(item: AbsensiEntity): Long {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        runCatching {
            SupabaseRestClient.insertRow(
                table = "absensi",
                payload = sharedItem.toSupabaseJson()
            )
        }.onFailure {
            Log.e(tag, "insert cloud failed, fallback local", it)
        }
        val localId = localDao.insert(sharedItem)
        val saved = localDao.getByPertemuan(sharedItem.userId, sharedItem.mataKuliahId, sharedItem.pertemuanKe)
            .firstOrNull { it.id == localId } ?: sharedItem.copy(id = localId)
        refresh(sharedItem.userId)
        return saved.id
    }

    suspend fun insertAll(items: List<AbsensiEntity>) {
        if (items.isEmpty()) return
        val sharedItems = items.map { it.copy(userId = SharedCloudScope.USER_ID) }
        runCatching {
            val payload = JSONArray().apply {
                sharedItems.forEach { put(it.toSupabaseJson()) }
            }
            SupabaseRestClient.insertRows("absensi", payload)
        }.onFailure {
            Log.e(tag, "insertAll failed size=${sharedItems.size}", it)
        }
        runCatching { localDao.insertAll(sharedItems) }
            .onFailure { Log.e(tag, "insertAll local failed", it) }
        refresh(sharedItems.first().userId)
    }

    suspend fun update(item: AbsensiEntity) {
        val sharedItem = item
        runCatching { localDao.update(sharedItem) }.onFailure { Log.e(tag, "local update failed", it) }
        
        val currentState = state(SharedCloudScope.USER_ID).value.toMutableList()
        val index = currentState.indexOfFirst { it.id == sharedItem.id }
        if (index != -1) {
            currentState[index] = sharedItem
            state(SharedCloudScope.USER_ID).value = currentState
        }

        scope.launch {
            runCatching {
                SupabaseRestClient.updateRow(
                    table = "absensi",
                    payload = sharedItem.toSupabaseJson(),
                    filters = listOf("id" to "eq.${sharedItem.id}")
                )
            }.onFailure { Log.e(tag, "update failed for id=${sharedItem.id}", it) }
            runCatching { refresh(sharedItem.userId) }
        }
    }

    suspend fun delete(item: AbsensiEntity) {
        val sharedItem = item
        runCatching { localDao.delete(sharedItem) }.onFailure { Log.e(tag, "local delete failed", it) }
        refresh(sharedItem.userId)
        
        scope.launch {
            runCatching {
                SupabaseRestClient.deleteRows(
                    table = "absensi",
                    filters = listOf("id" to "eq.${sharedItem.id}")
                )
            }.onFailure { Log.e(tag, "delete failed for id=${sharedItem.id}", it) }
        }
    }

    suspend fun getByPertemuan(userId: String, mkId: Long, pertemuan: Int) =
        runCatching {
            val local = localDao.getByPertemuan(SharedCloudScope.USER_ID, mkId, pertemuan)
                .sortedWith(compareByDescending<AbsensiEntity> { it.tanggal }.thenByDescending { it.id })
            if (local.isNotEmpty()) return@runCatching local
            loadByMataKuliah(userId, mkId)
                .filter { it.pertemuanKe == pertemuan }
                .sortedWith(compareByDescending<AbsensiEntity> { it.tanggal }.thenByDescending { it.id })
        }.onFailure {
            Log.e(tag, "getByPertemuan failed user=$userId mk=$mkId p=$pertemuan", it)
        }.getOrElse {
            runCatching { localDao.getByPertemuan(SharedCloudScope.USER_ID, mkId, pertemuan) }.getOrDefault(emptyList())
        }

    suspend fun deleteDuplicateRowsByMahasiswa(userId: String, mkId: Long, pertemuan: Int) =
        dedupeAbsensi(userId, mkId, pertemuan)

    private suspend fun refresh(userId: String) {
        Log.d(tag, "refresh called for user=$userId, current state size=${state(userId).value.size}")
        val cloudData = runCatching { loadAll(userId) }
            .onFailure { Log.e(tag, "refresh cloud loadAll failed for user=$userId", it) }
            .getOrNull()

        if (cloudData != null && cloudData.isNotEmpty()) {
            Log.d(tag, "Found ${cloudData.size} rows in cloud for user=$userId. Updating local cache...")
            runCatching {
                // To keep it simple and consistent: clear local shared data and re-insert from cloud
                localDao.deleteByUserId(SharedCloudScope.USER_ID)
                localDao.insertAll(cloudData.map { it.copy(userId = SharedCloudScope.USER_ID) })
                state(userId).value = cloudData
            }.onFailure { Log.e(tag, "refresh sync to local failed", it) }
        } else {
            Log.d(tag, "cloudData is ${if (cloudData == null) "null (failed)" else "empty"}. Keeping local data for user=$userId")
            // Fallback to local
            val localData = runCatching { localDao.getAll(SharedCloudScope.USER_ID).firstOrNull().orEmpty() }
                .getOrElse { emptyList() }
            if (localData.isNotEmpty() || state(userId).value.isEmpty()) {
                state(userId).value = localData
            }
        }
    }

    private suspend fun loadAll(userId: String): List<AbsensiEntity> =
        SupabaseRestClient.selectRows(
            table = "absensi",
            filters = listOf("user_id" to "eq.$userId")
        )
            .map { it.toAbsensiEntity() }
            .sortedByDescending { it.tanggal }

    private suspend fun loadByMataKuliah(userId: String, mkId: Long): List<AbsensiEntity> =
        SupabaseRestClient.selectRows(
            table = "absensi",
            filters = listOf(
                "mata_kuliah_id" to "eq.$mkId",
                "user_id" to "eq.$userId"
            )
        )
            .map { it.toAbsensiEntity() }
            .sortedByDescending { it.tanggal }

    private suspend fun dedupeAbsensi(userId: String, mkId: Long, pertemuan: Int) {
        val cloudOk = runCatching {
            val rows = getByPertemuan(userId, mkId, pertemuan)
            val keepByMahasiswa = mutableSetOf<Long>()
            rows.sortedBy { it.id }.forEach { row ->
                if (!keepByMahasiswa.add(row.mahasiswaId)) {
                    SupabaseRestClient.deleteRows(
                        table = "absensi",
                        filters = listOf("id" to "eq.${row.id}")
                    )
                }
            }
            refresh(userId)
        }.onFailure {
            Log.e(tag, "dedupeAbsensi failed user=$userId mk=$mkId p=$pertemuan", it)
        }.isSuccess
        if (!cloudOk) {
            runCatching {
                localDao.deleteDuplicateRowsByMahasiswa(SharedCloudScope.USER_ID, mkId, pertemuan)
                refresh(SharedCloudScope.USER_ID)
            }.onFailure { Log.e(tag, "dedupeAbsensi local fallback failed", it) }
        }
    }

    private fun subscribeToRealtime(userId: String) {
        if (!subscribedUsers.add(userId)) return
        scope.launch {
            runCatching {
                val client = SupabaseClient.client
                if (client.realtime.status.value != io.github.jan.supabase.realtime.Realtime.Status.CONNECTED) {
                    client.realtime.connect()
                }
                val channel = client.realtime.channel("absensi-realtime-$userId")
                val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "absensi"
                }
                flow.onEach { refresh(userId) }.launchIn(this)
                channel.subscribe()
                Log.d(tag, "Subscribed to realtime absensi for user=$userId")
            }.onFailure {
                Log.e(tag, "Realtime subscription failed for user=$userId", it)
                subscribedUsers.remove(userId)
            }
        }
    }

    private fun state(userId: String): MutableStateFlow<List<AbsensiEntity>> =
        synchronized(cacheByUser) {
            cacheByUser.getOrPut(userId) { MutableStateFlow(emptyList()) }
        }
}

package com.kelasin.app.data.repository

import android.content.Context
import android.util.Log
import com.kelasin.app.data.KelasinDatabase
import com.kelasin.app.data.entity.StatusTugas
import com.kelasin.app.data.entity.TugasEntity
import com.kelasin.app.data.supabase.SupabaseRestClient
import com.kelasin.app.data.supabase.toSupabaseJson
import com.kelasin.app.data.supabase.toTugasEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class TugasRepository(context: Context) {
    private val tag = "TugasRepository"
    private val localDao by lazy { KelasinDatabase.getInstance(context).tugasDao() }
    private val cacheByUser = mutableMapOf<String, MutableStateFlow<List<TugasEntity>>>()

    fun getAll(userId: String): Flow<List<TugasEntity>> =
        state(SharedCloudScope.USER_ID).onStart {
            runCatching { refresh(SharedCloudScope.USER_ID) }
                .onFailure { Log.e(tag, "refresh getAll failed", it) }
        }

    fun getByMataKuliah(userId: String, mkId: Long): Flow<List<TugasEntity>> =
        getAll(userId).map { items ->
            items.filter { it.mataKuliahId == mkId }
                .sortedBy { it.deadline }
        }

    fun getUpcoming(userId: String): Flow<List<TugasEntity>> =
        getAll(userId).map { items ->
            val now = System.currentTimeMillis()
            items.filter { it.status != StatusTugas.SELESAI && it.deadline >= now }
                .sortedBy { it.deadline }
                .take(5)
        }

    fun getInRange(userId: String, dari: Long, sampai: Long): Flow<List<TugasEntity>> =
        getAll(userId).map { items ->
            items.filter { it.deadline in dari..sampai }
                .sortedBy { it.deadline }
        }

    suspend fun getById(id: Long): TugasEntity? = runCatching {
        SupabaseRestClient.selectRows(
            table = "tugas",
            filters = listOf("id" to "eq.$id"),
            limit = 1
        ).firstOrNull()?.toTugasEntity()
    }.onFailure {
        Log.e(tag, "getById failed for id=$id", it)
    }.getOrNull() ?: runCatching { localDao.getById(id) }.getOrNull()

    suspend fun insert(item: TugasEntity): Long {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        val cloudResult = runCatching {
            SupabaseRestClient.insertRow(
                table = "tugas",
                payload = sharedItem.toSupabaseJson()
            ).toTugasEntity()
        }
        val saved = if (cloudResult.isSuccess) {
            cloudResult.getOrThrow()
        } else {
            Log.e(tag, "insert cloud failed, fallback local", cloudResult.exceptionOrNull())
            val localId = localDao.insert(sharedItem)
            localDao.getById(localId) ?: sharedItem.copy(id = localId)
        }
        runCatching { localDao.insert(saved) }.onFailure { Log.e(tag, "local cache insert failed", it) }
        refresh(SharedCloudScope.USER_ID)
        return saved.id
    }

    suspend fun update(item: TugasEntity) {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        runCatching {
            SupabaseRestClient.updateRow(
                table = "tugas",
                payload = sharedItem.toSupabaseJson(),
                filters = listOf("id" to "eq.${sharedItem.id}")
            )
        }.onFailure {
            Log.e(tag, "update failed for id=${sharedItem.id}", it)
        }
        runCatching { localDao.update(sharedItem) }.onFailure { Log.e(tag, "local update failed", it) }
        refresh(SharedCloudScope.USER_ID)
    }

    suspend fun delete(item: TugasEntity) {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        runCatching {
            SupabaseRestClient.deleteRows(
                table = "tugas",
                filters = listOf("id" to "eq.${sharedItem.id}")
            )
        }.onFailure {
            Log.e(tag, "delete failed for id=${sharedItem.id}", it)
        }
        runCatching { localDao.delete(sharedItem) }.onFailure { Log.e(tag, "local delete failed", it) }
        refresh(SharedCloudScope.USER_ID)
    }

    private suspend fun refresh(userId: String) {
        val current = state(SharedCloudScope.USER_ID).value
        val localData = runCatching { localDao.getAll(SharedCloudScope.USER_ID).firstOrNull().orEmpty() }
            .getOrElse {
                Log.e(tag, "refresh local failed for user=$userId", it)
                current
            }
        val cloudData = runCatching { loadAll(userId) }
            .onFailure { Log.e(tag, "refresh failed for user=$userId", it) }
            .getOrNull()
            
        if (cloudData != null) {
            val cloudIds = cloudData.map { it.id }.toSet()
            val localIds = localData.map { it.id }.toSet()
            val toDelete = localIds - cloudIds
            
            toDelete.forEach { id -> localDao.getById(id)?.let { localDao.delete(it) } }
            cloudData.forEach { localDao.insert(it) }
            
            state(SharedCloudScope.USER_ID).value = cloudData
        } else {
            state(SharedCloudScope.USER_ID).value = localData
        }
    }

    private suspend fun loadAll(userId: String): List<TugasEntity> =
        SupabaseRestClient.selectRows(
            table = "tugas"
        )
            .map { it.toTugasEntity() }
            .sortedBy { it.deadline }

    private fun state(userId: String): MutableStateFlow<List<TugasEntity>> =
        synchronized(cacheByUser) {
            cacheByUser.getOrPut(userId) { MutableStateFlow(emptyList()) }
        }
}

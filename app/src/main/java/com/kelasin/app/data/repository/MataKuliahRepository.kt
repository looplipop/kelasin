package com.kelasin.app.data.repository

import android.content.Context
import android.util.Log
import com.kelasin.app.data.KelasinDatabase
import com.kelasin.app.data.entity.MataKuliahEntity
import com.kelasin.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import com.kelasin.app.data.repository.SharedCloudScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MataKuliahRepository(context: Context) {
    private val tag = "MataKuliahRepo"
    private val localDao by lazy { KelasinDatabase.getInstance(context).mataKuliahDao() }
    private val client = SupabaseClient.client
    private val cacheByUser = mutableMapOf<String, MutableStateFlow<List<MataKuliahEntity>>>()
    private val seedMutexByUser = mutableMapOf<String, Mutex>()

    fun getAll(userId: String): Flow<List<MataKuliahEntity>> =
        state(SharedCloudScope.USER_ID).onStart {
            runCatching { refresh(SharedCloudScope.USER_ID) }
                .onFailure { Log.e(tag, "refresh getAll failed", it) }
        }

    fun getByHari(userId: String, hari: String): Flow<List<MataKuliahEntity>> =
        getAll(userId).map { items ->
            items.filter { it.hari.equals(hari, ignoreCase = true) }
                .sortedBy { it.jamMulai }
        }

    suspend fun getById(id: Long): MataKuliahEntity? = runCatching {
        client.postgrest["mata_kuliah"]
            .select { filter { eq("id", id) } }
            .decodeSingle<MataKuliahEntity>()
    }.getOrNull() ?: localDao.getById(id)

    suspend fun insert(item: MataKuliahEntity): Long {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        val cloudResult = runCatching {
            client.postgrest["mata_kuliah"]
                .insert(sharedItem) { select() }
                .decodeSingle<MataKuliahEntity>()
        }
        val saved = if (cloudResult.isSuccess) {
            cloudResult.getOrThrow()
        } else {
            Log.e(tag, "insert cloud failed, fallback local", cloudResult.exceptionOrNull())
            val localId = localDao.insert(sharedItem)
            localDao.getById(localId) ?: sharedItem.copy(id = localId)
        }
        runCatching { localDao.insert(saved) }
            .onFailure { Log.e(tag, "insert local cache failed", it) }
        refresh(SharedCloudScope.USER_ID)
        return saved.id
    }

    suspend fun update(item: MataKuliahEntity) {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        runCatching {
            client.postgrest["mata_kuliah"].update(sharedItem) {
                filter { eq("id", sharedItem.id) }
            }
        }.onFailure {
            Log.e(tag, "update cloud failed for id=${sharedItem.id}", it)
        }
        runCatching { localDao.update(sharedItem) }.onFailure { Log.e(tag, "local update failed", it) }
        refresh(SharedCloudScope.USER_ID)
    }

    suspend fun delete(item: MataKuliahEntity) {
        val sharedItem = item.copy(userId = SharedCloudScope.USER_ID)
        runCatching {
            client.postgrest["mata_kuliah"].delete {
                filter { eq("id", sharedItem.id) }
            }
        }.onFailure {
            Log.e(tag, "delete cloud failed for id=${sharedItem.id}", it)
        }
        runCatching { localDao.delete(sharedItem) }.onFailure { Log.e(tag, "local delete failed", it) }
        refresh(SharedCloudScope.USER_ID)
    }

    suspend fun seedMataKuliah(userId: String) {
        val normalizedUserId = SharedCloudScope.USER_ID
        seedMutex(normalizedUserId).withLock {
            val palette = listOf(
                "#1565C0", "#2E7D32", "#6A1B9A", "#E65100",
                "#00838F", "#AD1457", "#4527A0", "#558B2F", "#00695C", "#EF6C00"
            )
            val seeds = listOf(
                MataKuliahEntity(nama="TEKNIK KOMPILASI", kode="B203", dosen="Fahmi Abdullah, S.T., M.Kom.", sks=3, hari="Senin", jamMulai="10:00", jamSelesai="12:00", ruangan="B 203", emailDosen="tasik.fahmi@gmail.com", noHpDosen="085323043220", warna=palette[0], userId=normalizedUserId),
                MataKuliahEntity(nama="COMPUTER SECURITY", kode="B201", dosen="Yasti Aisyah Primianjani, S.Kom.", sks=3, hari="Selasa", jamMulai="10:00", jamSelesai="12:00", ruangan="B 201", emailDosen="yasti@sttbandung.ac.id", noHpDosen="087823331817", warna=palette[1], userId=normalizedUserId),
                MataKuliahEntity(nama="DIGITAL PRENEURSHIP", kode="B204", dosen="Dhany Indra Gunawan, S.T., M.Kom.", sks=3, hari="Selasa", jamMulai="13:00", jamSelesai="15:00", ruangan="B 204", emailDosen="dhaindgun@gmail.com", noHpDosen="085718829330", warna=palette[2], userId=normalizedUserId),
                MataKuliahEntity(nama="OBJECT ORIENTED ANALYSIS AND DESIGN", kode="B301", dosen="Danny Aidil Rismayadi, S.SI., M.Kom.", sks=3, hari="Rabu", jamMulai="13:00", jamSelesai="15:00", ruangan="B 301", emailDosen="danny.sttb@gmail.com", noHpDosen="081321100222", warna=palette[3], userId=normalizedUserId),
                MataKuliahEntity(nama="JARINGAN KOMPUTER II", kode="B203", dosen="Hena Sulaeman, ST.", sks=3, hari="Rabu", jamMulai="15:00", jamSelesai="17:00", ruangan="B 203", emailDosen="henasulaiman50@gmail.com", noHpDosen="089501245089", warna=palette[4], userId=normalizedUserId),
                MataKuliahEntity(nama="PEMOGRAMAN MOBILE I", kode="B202", dosen="Erryck Norrys, S.Kom.", sks=3, hari="Kamis", jamMulai="10:00", jamSelesai="12:00", ruangan="B 202", emailDosen="-", noHpDosen="081292438529", warna=palette[5], userId=normalizedUserId),
                MataKuliahEntity(nama="PEMOGRAM. BERORIENTASI OBJEK II", kode="B101", dosen="Iwan Ridwan, S.T., M.Kom.", sks=3, hari="Kamis", jamMulai="13:00", jamSelesai="15:00", ruangan="B 101", emailDosen="ir.pegasus75@gmail.com", noHpDosen="82218870024", warna=palette[6], userId=normalizedUserId)
            )

            runCatching {
                val existingLocalKeys = localDao.getAll(normalizedUserId).firstOrNull()
                    .orEmpty()
                    .map { it.normalizedSeedKey() }
                    .toSet()
                val missingLocalSeeds = seeds.filter { it.normalizedSeedKey() !in existingLocalKeys }
                missingLocalSeeds.forEach { localDao.insert(it) }
                
                val cloudData = client.postgrest["mata_kuliah"]
                    .select { filter { eq("user_id", normalizedUserId) } }
                    .decodeList<MataKuliahEntity>()
                
                val cloudKeys = cloudData.map { it.normalizedSeedKey() }.toSet()
                val missingOnCloud = seeds.filter { it.normalizedSeedKey() !in cloudKeys }
                if (missingOnCloud.isNotEmpty()) {
                    client.postgrest["mata_kuliah"].insert(missingOnCloud)
                }
            }.onFailure { Log.e(tag, "seedMataKuliah failed", it) }
            refresh(normalizedUserId)
        }
    }

    private suspend fun refresh(userId: String) {
        val cloudData = runCatching {
            client.postgrest["mata_kuliah"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<MataKuliahEntity>()
        }.onFailure { Log.e(tag, "refresh cloud load failed", it) }
         .getOrNull()

        if (cloudData != null && cloudData.isNotEmpty()) {
            val localItems = localDao.getAll(userId).firstOrNull().orEmpty()
            val cloudIds = cloudData.map { it.id }.toSet()
            localItems.filter { it.id !in cloudIds }.forEach { localDao.delete(it) }
            cloudData.forEach { localDao.insert(it) }
        }

        val latest = runCatching {
            localDao.getAll(userId).firstOrNull().orEmpty()
        }.getOrElse { emptyList() }
        
        state(userId).value = latest.sortedWith(compareBy<MataKuliahEntity> { dayRank(it.hari) }.thenBy { it.jamMulai })
    }

    private fun state(userId: String): MutableStateFlow<List<MataKuliahEntity>> =
        synchronized(cacheByUser) {
            cacheByUser.getOrPut(userId) { MutableStateFlow(emptyList()) }
        }

    private fun seedMutex(userId: String): Mutex =
        synchronized(seedMutexByUser) {
            seedMutexByUser.getOrPut(userId) { Mutex() }
        }

    private fun MataKuliahEntity.normalizedSeedKey(): String = listOf(
        nama.trim().lowercase(),
        kode.trim().lowercase(),
        hari.trim().lowercase(),
        jamMulai.trim(),
        jamSelesai.trim()
    ).joinToString("|")

    private fun dayRank(hari: String): Int = when (hari) {
        "Senin" -> 1
        "Selasa" -> 2
        "Rabu" -> 3
        "Kamis" -> 4
        "Jumat" -> 5
        "Sabtu" -> 6
        "Minggu" -> 7
        else -> 99
    }
}

package com.kelasin.app.data.repository

import android.content.Context
import android.util.Log
import com.kelasin.app.data.KelasinDatabase
import com.kelasin.app.data.entity.MahasiswaEntity
import com.kelasin.app.data.entity.MataKuliahEntity
import com.kelasin.app.data.supabase.SupabaseRestClient
import com.kelasin.app.data.supabase.toMahasiswaEntity
import com.kelasin.app.data.supabase.toSupabaseJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

class MahasiswaRepository(context: Context) {
    private val tag = "MahasiswaRepository"
    private val localDao by lazy { KelasinDatabase.getInstance(context).mahasiswaDao() }
    private val state = MutableStateFlow<List<MahasiswaEntity>>(emptyList())
    private val seedMutexByOwner = mutableMapOf<String, Mutex>()
    private val mataKuliahRepo by lazy { MataKuliahRepository(context) }

    fun getAll(): Flow<List<MahasiswaEntity>> =
        state.onStart { runCatching { refresh() }.onFailure { Log.e(tag, "refresh getAll failed", it) } }

    suspend fun insert(item: MahasiswaEntity): Long {
        val cloudResult = runCatching {
            val payload = item.toSupabaseJson()
            SupabaseRestClient.insertRow(
                table = "mahasiswa",
                payload = payload
            ).toMahasiswaEntity()
        }
        val saved = if (cloudResult.isSuccess) {
            cloudResult.getOrThrow()
        } else {
            Log.e(tag, "insert cloud failed, fallback local", cloudResult.exceptionOrNull())
            val localId = localDao.insert(item)
            val localList = localDao.getAll().firstOrNull().orEmpty()
            localList.find { it.id == localId } ?: item.copy(id = localId)
        }
        runCatching { localDao.insert(saved) }
            .onFailure { Log.e(tag, "insert local cache failed", it) }
        refresh()
        return saved.id
    }

    suspend fun update(item: MahasiswaEntity) {
        runCatching {
            SupabaseRestClient.updateRow(
                table = "mahasiswa",
                payload = item.toSupabaseJson(),
                filters = listOf("id" to "eq.${item.id}")
            )
        }.onFailure {
            Log.e(tag, "update failed for id=${item.id}", it)
        }
        runCatching { localDao.update(item) }.onFailure { Log.e(tag, "local update failed", it) }
        refresh()
    }

    suspend fun delete(item: MahasiswaEntity) {
        runCatching {
            SupabaseRestClient.deleteRows(
                table = "mahasiswa",
                filters = listOf("id" to "eq.${item.id}")
            )
        }.onFailure {
            Log.e(tag, "delete failed for id=${item.id}", it)
        }
        runCatching { localDao.delete(item) }.onFailure { Log.e(tag, "local delete failed", it) }
        refresh()
    }

    suspend fun seedMahasiswa() {
        val ownerId = SharedCloudScope.USER_ID

        seedMutex(ownerId).withLock {
            ensureMataKuliahSeed(ownerId)
            runCatching {
                val existingLocalNames = localDao.getAll().firstOrNull()
                    .orEmpty()
                    .map { it.normalizedName() }
                    .toSet()
                val missingLocal = defaultMahasiswaNames()
                    .filter { it.normalizedName() !in existingLocalNames }
                if (missingLocal.isNotEmpty()) {
                    localDao.insertAll(missingLocal.map { MahasiswaEntity(nama = it) })
                }
            }.onFailure {
                Log.e(tag, "seedMahasiswa local ensure failed", it)
            }

            runCatching {
                val names = defaultMahasiswaNames()
                val existingNames = loadAll()
                    .map { it.normalizedName() }
                    .toSet()
                val missingNames = names.filter { it.normalizedName() !in existingNames }
                if (missingNames.isNotEmpty()) {
                    val payload = JSONArray().apply {
                        missingNames.forEach { name ->
                            put(MahasiswaEntity(nama = name).toSupabaseJson())
                        }
                    }
                    SupabaseRestClient.insertRows("mahasiswa", payload)
                }
            }.onFailure {
                Log.e(tag, "seedMahasiswa failed", it)
            }
            refresh()
        }
    }

    suspend fun refresh() {
        val current = state.value
        val localData = runCatching { localDao.getAll().firstOrNull().orEmpty() }
            .getOrElse {
                Log.e(tag, "refresh local fallback failed", it)
                current
            }
        
        state.value = localData
        
        runCatching {
            val cloudData = loadAll()
            if (cloudData.isNotEmpty()) {
                // Keep cloud IDs exactly as-is so foreign keys (e.g. absensi.mahasiswaId) stay consistent.
                localDao.deleteAll()
                localDao.insertAll(cloudData)
                state.value = cloudData.sortedBy { it.nama }
            } else {
                val cloudNames = cloudData.map { it.normalizedName() }.toSet()
                val localNames = localData.map { it.normalizedName() }.toSet()

                val toDeleteLocal = localData.filter { it.normalizedName() !in cloudNames }
                if (toDeleteLocal.isNotEmpty()) {
                    toDeleteLocal.forEach { localDao.delete(it) }
                }

                val missingInLocal = cloudData.filter { it.normalizedName() !in localNames }
                if (missingInLocal.isNotEmpty()) {
                    missingInLocal.forEach { localDao.insert(it) }
                }

                val mergedData = localDao.getAll().firstOrNull().orEmpty()
                state.value = mergedData.sortedBy { it.nama }
            }
        }.onFailure { Log.e(tag, "refresh sync failed", it) }
    }

    private suspend fun loadAll(): List<MahasiswaEntity> =
        SupabaseRestClient.selectRows(
            table = "mahasiswa",
            order = "nama.asc"
        ).map { it.toMahasiswaEntity() }

    fun setCurrentOwner(userId: String?) {
        // Shared cloud mode: ignore per-user owner scope
    }

    private fun seedMutex(ownerId: String): Mutex =
        synchronized(seedMutexByOwner) {
            seedMutexByOwner.getOrPut(ownerId) { Mutex() }
        }

    private suspend fun ensureMataKuliahSeed(ownerId: String) {
        runCatching {
            mataKuliahRepo.seedMataKuliah(ownerId)
            val matkulList = mataKuliahRepo.getAll(ownerId).firstOrNull().orEmpty()
            if (matkulList.isEmpty()) {
                val fallback = defaultMataKuliahSeed(ownerId)
                fallback.forEach { mataKuliahRepo.insert(it) }
            }
        }.onFailure {
            Log.e(tag, "ensureMataKuliahSeed failed", it)
        }
    }

    private fun defaultMataKuliahSeed(ownerId: String): List<MataKuliahEntity> {
        val palette = listOf(
            "#1565C0", "#2E7D32", "#6A1B9A", "#E65100",
            "#00838F", "#AD1457", "#4527A0"
        )
        return listOf(
            MataKuliahEntity(nama="TEKNIK KOMPILASI", kode="B203", dosen="Fahmi Abdullah, S.T., M.Kom.", sks=3, hari="Senin", jamMulai="10:00", jamSelesai="12:00", ruangan="B 203", emailDosen="tasik.fahmi@gmail.com", noHpDosen="085323043220", warna=palette[0], userId=ownerId),
            MataKuliahEntity(nama="COMPUTER SECURITY", kode="B201", dosen="Yasti Aisyah Primianjani, S.Kom.", sks=3, hari="Selasa", jamMulai="10:00", jamSelesai="12:00", ruangan="B 201", emailDosen="yasti@sttbandung.ac.id", noHpDosen="087823331817", warna=palette[1], userId=ownerId),
            MataKuliahEntity(nama="DIGITAL PRENEURSHIP", kode="B204", dosen="Dhany Indra Gunawan, S.T., M.Kom.", sks=3, hari="Selasa", jamMulai="13:00", jamSelesai="15:00", ruangan="B 204", emailDosen="dhaindgun@gmail.com", noHpDosen="085718829330", warna=palette[2], userId=ownerId),
            MataKuliahEntity(nama="OBJECT ORIENTED ANALYSIS AND DESIGN", kode="B301", dosen="Danny Aidil Rismayadi, S.SI., M.Kom.", sks=3, hari="Rabu", jamMulai="13:00", jamSelesai="15:00", ruangan="B 301", emailDosen="danny.sttb@gmail.com", noHpDosen="081321100222", warna=palette[3], userId=ownerId),
            MataKuliahEntity(nama="JARINGAN KOMPUTER II", kode="B203", dosen="Hena Sulaeman, ST.", sks=3, hari="Rabu", jamMulai="15:00", jamSelesai="17:00", ruangan="B 203", emailDosen="henasulaiman50@gmail.com", noHpDosen="089501245089", warna=palette[4], userId=ownerId),
            MataKuliahEntity(nama="PEMOGRAMAN MOBILE I", kode="B202", dosen="Erryck Norrys, S.Kom.", sks=3, hari="Kamis", jamMulai="10:00", jamSelesai="12:00", ruangan="B 202", emailDosen="-", noHpDosen="081292438529", warna=palette[5], userId=ownerId),
            MataKuliahEntity(nama="PEMOGRAM. BERORIENTASI OBJEK II", kode="B101", dosen="Iwan Ridwan, S.T., M.Kom.", sks=3, hari="Kamis", jamMulai="13:00", jamSelesai="15:00", ruangan="B 101", emailDosen="ir.pegasus75@gmail.com", noHpDosen="82218870024", warna=palette[6], userId=ownerId)
        )
    }

    private fun defaultMahasiswaNames(): List<String> = listOf(
        "Abdurrouf Faiz Al Farisyi", "Adrian", "ADRIAN FATHURRAHMAN", "Ahmad Agung Maulana", "Ahmad Kurnia",
        "Aliya Kusuma Dewi", "Alwin Kogoya", "Andrian Maulana Dzikwan", "Anisa Febrianti", "Annida Nur Zahra",
        "Arif Rahmansyah", "Arifin hidayat", "Ayipnoor Irfan Putra Wahyudin", "Budi Nur Bhakti", "Dafa Irsyad Nashrullah",
        "Deo Ary Anggara", "Deza Wulani Rahayu", "Dhenia Putri Nuraini", "Diki Wahyu Permana", "Diky Raihan Subagja",
        "Effendy Gabriel Putra", "Ega Silfhia", "Fahridzal Nur Sidiq", "Fajar Fathurrohman", "Fatih Ahmad Hosam Abiyasa",
        "Fitri Aulia", "Gema Rajab Fauzan", "Abil Fida Ismail", "Azriel Muhamad Bintang", "Danny Ahmad Gunawan",
        "DAVID AZHAR JULI NURBANI", "Dimas Arya Purnama Alam", "Fadli Septian Sutaryana", "Falix Iqbal Wahyudi", "FARREL IRAWAN",
        "IMAMMUL KHOER MUTTAQIN", "Khalifah Alvito Danendra Irwansyah", "LUKMAN MUHAMMAD FAZA", "Mohammad Rifaldy", "MUHAMAD ARGA REKSAPATI",
        "Muhamad Rizaldy", "Muhamad Suwandi", "MAHESA SATRIA DARUSSALAM", "YUDHISTIANA NUR HADI FIRDAUS", "Faisal Ramdhani Riyadi"
    )

    private fun MahasiswaEntity.normalizedName(): String = nama.trim().lowercase()
    private fun String.normalizedName(): String = trim().lowercase()
}

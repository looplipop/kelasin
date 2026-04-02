package com.kelasin.app.data.dao

import androidx.room.*
import com.kelasin.app.data.entity.AbsensiEntity
import com.kelasin.app.data.entity.StatusAbsensi
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsensiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AbsensiEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AbsensiEntity>)

    @Update
    suspend fun update(item: AbsensiEntity)

    @Delete
    suspend fun delete(item: AbsensiEntity)

    @Query("SELECT * FROM absensi WHERE mataKuliahId = :mkId AND userId = :userId ORDER BY tanggal DESC")
    fun getByMataKuliah(userId: String, mkId: Long): Flow<List<AbsensiEntity>>

    @Query("SELECT COUNT(*) FROM absensi WHERE mataKuliahId = :mkId AND status = 'HADIR' AND userId = :userId")
    suspend fun countHadir(userId: String, mkId: Long): Int

    @Query("SELECT COUNT(*) FROM absensi WHERE mataKuliahId = :mkId AND userId = :userId")
    suspend fun countTotal(userId: String, mkId: Long): Int

    @Query("SELECT * FROM absensi WHERE userId = :userId ORDER BY tanggal DESC")
    fun getAll(userId: String): Flow<List<AbsensiEntity>>

    @Query(
        """
        SELECT * FROM absensi
        WHERE userId = :userId
          AND mataKuliahId = :mkId
          AND pertemuanKe = :pertemuan
        ORDER BY tanggal DESC, id DESC
        """
    )
    suspend fun getByPertemuan(userId: String, mkId: Long, pertemuan: Int): List<AbsensiEntity>

    @Query("DELETE FROM absensi WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query(
        """
        DELETE FROM absensi
        WHERE userId = :userId
          AND mataKuliahId = :mkId
          AND pertemuanKe = :pertemuan
          AND id NOT IN (
              SELECT MIN(id)
              FROM absensi
              WHERE userId = :userId
                AND mataKuliahId = :mkId
                AND pertemuanKe = :pertemuan
              GROUP BY mahasiswaId
          )
        """
    )
    suspend fun deleteDuplicateRowsByMahasiswa(userId: String, mkId: Long, pertemuan: Int)
}

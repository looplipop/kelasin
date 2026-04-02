package com.kelasin.app.data.dao

import androidx.room.*
import com.kelasin.app.data.entity.TugasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TugasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TugasEntity): Long

    @Update
    suspend fun update(item: TugasEntity)

    @Delete
    suspend fun delete(item: TugasEntity)

    @Query("SELECT * FROM tugas WHERE userId = :userId ORDER BY deadline ASC")
    fun getAll(userId: String): Flow<List<TugasEntity>>

    @Query("SELECT * FROM tugas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TugasEntity?

    @Query("SELECT * FROM tugas WHERE mataKuliahId = :mkId AND userId = :userId ORDER BY deadline ASC")
    fun getByMataKuliah(userId: String, mkId: Long): Flow<List<TugasEntity>>

    @Query("SELECT * FROM tugas WHERE deadline BETWEEN :dari AND :sampai AND userId = :userId ORDER BY deadline ASC")
    fun getInRange(userId: String, dari: Long, sampai: Long): Flow<List<TugasEntity>>

    @Query("SELECT * FROM tugas WHERE status != 'SELESAI' AND userId = :userId ORDER BY deadline ASC LIMIT 5")
    fun getUpcoming(userId: String): Flow<List<TugasEntity>>
}

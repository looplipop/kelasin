package com.kelasin.app.data.dao

import androidx.room.*
import com.kelasin.app.data.entity.CatatanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatatanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CatatanEntity): Long

    @Update
    suspend fun update(item: CatatanEntity)

    @Delete
    suspend fun delete(item: CatatanEntity)

    @Query("SELECT * FROM catatan WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAll(userId: String): Flow<List<CatatanEntity>>

    @Query("SELECT * FROM catatan WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CatatanEntity?

    @Query("SELECT * FROM catatan WHERE mataKuliahId = :mkId AND userId = :userId ORDER BY updatedAt DESC")
    fun getByMataKuliah(userId: String, mkId: Long): Flow<List<CatatanEntity>>

    @Query("SELECT * FROM catatan WHERE (judul LIKE '%' || :query || '%' OR isi LIKE '%' || :query || '%' OR tag LIKE '%' || :query || '%') AND userId = :userId ORDER BY updatedAt DESC")
    fun search(userId: String, query: String): Flow<List<CatatanEntity>>
}

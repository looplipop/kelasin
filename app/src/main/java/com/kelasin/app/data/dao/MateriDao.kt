package com.kelasin.app.data.dao

import androidx.room.*
import com.kelasin.app.data.entity.MateriEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MateriDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MateriEntity): Long

    @Update
    suspend fun update(item: MateriEntity)

    @Delete
    suspend fun delete(item: MateriEntity)

    @Query("SELECT * FROM materi WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAll(userId: String): Flow<List<MateriEntity>>

    @Query("SELECT * FROM materi WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MateriEntity?

    @Query("SELECT * FROM materi WHERE userId = :userId AND mataKuliahId = :mkId ORDER BY createdAt DESC")
    fun getByMataKuliah(userId: String, mkId: Long): Flow<List<MateriEntity>>

    @Query("SELECT * FROM materi WHERE userId = :userId AND isBookmarked = 1 ORDER BY createdAt DESC")
    fun getBookmarked(userId: String): Flow<List<MateriEntity>>
}

package com.kelasin.app.data.dao

import androidx.room.*
import com.kelasin.app.data.entity.MahasiswaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MahasiswaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MahasiswaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MahasiswaEntity>)

    @Update
    suspend fun update(item: MahasiswaEntity)

    @Delete
    suspend fun delete(item: MahasiswaEntity)

    @Query("SELECT * FROM mahasiswa ORDER BY nama ASC")
    fun getAll(): Flow<List<MahasiswaEntity>>

    @Query("SELECT COUNT(*) FROM mahasiswa")
    fun count(): Flow<Int>

    @Query("DELETE FROM mahasiswa")
    suspend fun deleteAll()
}

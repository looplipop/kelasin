package com.kelasin.app.data.dao

import androidx.room.*
import com.kelasin.app.data.entity.MataKuliahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MataKuliahDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MataKuliahEntity): Long

    @Update
    suspend fun update(item: MataKuliahEntity)

    @Delete
    suspend fun delete(item: MataKuliahEntity)

    @Query(
        """
        SELECT * FROM mata_kuliah
        WHERE userId = :userId
        ORDER BY
            CASE hari
                WHEN 'Senin' THEN 1
                WHEN 'Selasa' THEN 2
                WHEN 'Rabu' THEN 3
                WHEN 'Kamis' THEN 4
                WHEN 'Jumat' THEN 5
                WHEN 'Sabtu' THEN 6
                WHEN 'Minggu' THEN 7
                ELSE 99
            END,
            jamMulai
        """
    )
    fun getAll(userId: String): Flow<List<MataKuliahEntity>>

    @Query("SELECT * FROM mata_kuliah WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MataKuliahEntity?

    @Query("SELECT * FROM mata_kuliah WHERE hari = :hari AND userId = :userId ORDER BY jamMulai")
    fun getByHari(userId: String, hari: String): Flow<List<MataKuliahEntity>>
}

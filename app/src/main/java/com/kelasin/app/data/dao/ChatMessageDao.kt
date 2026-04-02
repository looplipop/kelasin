package com.kelasin.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kelasin.app.data.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Update
    suspend fun update(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY created_at ASC")
    fun getMessagesByRoom(roomId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY created_at ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE status = 'SENDING' OR status = 'FAILED'")
    suspend fun getPendingMessages(): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chat_messages WHERE room_id = :roomId")
    suspend fun deleteByRoom(roomId: Long)
}

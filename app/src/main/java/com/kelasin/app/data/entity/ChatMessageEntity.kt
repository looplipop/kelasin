package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @SerialName("room_id")
    @ColumnInfo(name = "room_id")
    val roomId: Long,
    @SerialName("sender_user_id")
    @ColumnInfo(name = "sender_user_id")
    val senderUserId: String,
    @SerialName("sender_name")
    @ColumnInfo(name = "sender_name")
    val senderName: String,
    val text: String,
    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "SENT", // SENT, SENDING, FAILED
    @SerialName("is_edited")
    @ColumnInfo(name = "is_edited")
    val isEdited: Boolean = false,
    @SerialName("sender_profile_pic")
    @ColumnInfo(name = "sender_profile_pic")
    val senderProfilePic: String? = null
)

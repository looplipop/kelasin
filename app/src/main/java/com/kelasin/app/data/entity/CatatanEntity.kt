package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "catatan")
data class CatatanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @SerialName("mata_kuliah_id")
    val mataKuliahId: Long? = null,   // null = tidak terikat matkul
    @SerialName("label_kustom")
    val labelKustom: String = "",      // Label bebas jika tidak pakai matkul
    val judul: String,
    val isi: String,
    val tag: String = "",              // comma-separated tags
    @SerialName("file_uri")
    val fileUri: String = "",          // attachment file URI
    @SerialName("user_id")
    val userId: String,
    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("is_muted")
    val isMuted: Boolean = false       // Mute notifications for this note
)

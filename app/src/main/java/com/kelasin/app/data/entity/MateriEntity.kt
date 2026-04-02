package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipeMateri { LINK, PDF, GAMBAR }

@Entity(tableName = "materi")
data class MateriEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mataKuliahId: Long,
    val judul: String,
    val deskripsi: String = "",
    val tipe: TipeMateri = TipeMateri.LINK,
    val url: String = "",
    val fileUri: String = "",
    val isBookmarked: Boolean = false,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

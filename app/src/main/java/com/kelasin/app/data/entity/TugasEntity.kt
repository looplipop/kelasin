package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Prioritas { TINGGI, SEDANG, RENDAH }
enum class StatusTugas { BELUM, PROSES, SELESAI }

@Entity(tableName = "tugas")
data class TugasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mataKuliahId: Long,
    val judul: String,
    val deskripsi: String = "",
    val deadline: Long,         // timestamp millis
    val prioritas: Prioritas = Prioritas.SEDANG,
    val status: StatusTugas = StatusTugas.BELUM,
    val pertemuanKe: Int = 1,
    val fileUri: String = "",
    val linkUrl: String = "",
    val reminderEnabled: Boolean = true,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

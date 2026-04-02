package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StatusAbsensi { HADIR, SAKIT, IZIN, ALPHA, BELUM_DIPILIH }

@Entity(tableName = "absensi")
data class AbsensiEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mataKuliahId: Long,
    val mahasiswaId: Long = 0L,
    val tanggal: Long,                    // timestamp millis
    val status: StatusAbsensi = StatusAbsensi.BELUM_DIPILIH,
    val pertemuanKe: Int,
    val keterangan: String = "",
    val userId: String
)

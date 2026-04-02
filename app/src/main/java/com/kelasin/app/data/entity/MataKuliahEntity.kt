package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "mata_kuliah")
data class MataKuliahEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val kode: String,
    val dosen: String,
    val sks: Int,
    val hari: String,       // Senin, Selasa, dst
    @SerialName("jam_mulai")
    val jamMulai: String,   // "08:00"
    @SerialName("jam_selesai")
    val jamSelesai: String, // "10:00"
    val ruangan: String,
    @SerialName("email_dosen")
    val emailDosen: String = "",
    @SerialName("no_hp_dosen")
    val noHpDosen: String = "",
    val warna: String = "#6750A4", // hex color untuk UI
    @SerialName("user_id")
    val userId: String
)

package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mahasiswa",
    indices = [Index(value = ["nama"], unique = true)]
)
data class MahasiswaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String
) {
    val nomorAbsen: Int
        get() = Regex("\\[NO:(\\d+)\\]").find(nama)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        
    val displayNama: String
        get() = nama.replace(Regex("\\[NO:\\d+\\]"), "").trim()
}

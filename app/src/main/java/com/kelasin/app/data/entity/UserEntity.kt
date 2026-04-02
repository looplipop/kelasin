package com.kelasin.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val nama: String,
    val email: String,
    val username: String = "",
    val profilePicUrl: String = "",
    val passwordHash: String = "",
    val role: String = "MAHASISWA",
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayBio: String 
        get() = if (profilePicUrl.contains("[BIO]")) profilePicUrl.substringAfter("[BIO]").trim() else ""
    val displayProfilePic: String 
        get() = if (profilePicUrl.contains("[BIO]")) profilePicUrl.substringBefore("[BIO]") else profilePicUrl
}

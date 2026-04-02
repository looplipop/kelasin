package com.kelasin.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kelasin.app.data.dao.*
import com.kelasin.app.data.entity.*

@Database(
    entities = [
        UserEntity::class,
        MataKuliahEntity::class,
        TugasEntity::class,
        CatatanEntity::class,
        MateriEntity::class,
        AbsensiEntity::class,
        MahasiswaEntity::class,
        ChatMessageEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KelasinDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun mataKuliahDao(): MataKuliahDao
    abstract fun tugasDao(): TugasDao
    abstract fun catatanDao(): CatatanDao
    abstract fun materiDao(): MateriDao
    abstract fun absensiDao(): AbsensiDao
    abstract fun mahasiswaDao(): MahasiswaDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: KelasinDatabase? = null

        fun getInstance(context: Context): KelasinDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KelasinDatabase::class.java,
                    "kelasin.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }

                instance
            }
        }
    }
}

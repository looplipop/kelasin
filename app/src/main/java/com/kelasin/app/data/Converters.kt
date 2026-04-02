package com.kelasin.app.data

import androidx.room.TypeConverter
import com.kelasin.app.data.entity.Prioritas
import com.kelasin.app.data.entity.StatusAbsensi
import com.kelasin.app.data.entity.StatusTugas
import com.kelasin.app.data.entity.TipeMateri

class Converters {
    @TypeConverter
    fun fromPrioritas(value: Prioritas) = value.name

    @TypeConverter
    fun toPrioritas(value: String) = enumValueOf<Prioritas>(value)

    @TypeConverter
    fun fromStatusTugas(value: StatusTugas) = value.name

    @TypeConverter
    fun toStatusTugas(value: String) = enumValueOf<StatusTugas>(value)

    @TypeConverter
    fun fromStatusAbsensi(value: StatusAbsensi) = value.name

    @TypeConverter
    fun toStatusAbsensi(value: String) = enumValueOf<StatusAbsensi>(value)

    @TypeConverter
    fun fromTipeMateri(value: TipeMateri) = value.name

    @TypeConverter
    fun toTipeMateri(value: String) = enumValueOf<TipeMateri>(value)
}

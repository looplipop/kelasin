package com.kelasin.app.data.entity

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AcademicEvent(
    val title: String,
    val startDateStr: String,
    val endDateStr: String,
    val color: Color,
    val isHoliday: Boolean = false
) {
    val startMillis: Long
    val endMillis: Long
    init {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedStartMillis = fmt.parse(startDateStr)?.time ?: 0L
        val parsedEndMillis = fmt.parse(endDateStr)?.time ?: parsedStartMillis
        startMillis = normalizeStartOfDay(parsedStartMillis)
        endMillis = normalizeEndOfDay(parsedEndMillis)
    }
}

val EventLibur = Color(0xFFE53935)
val EventKuliah = Color(0xFF81D4FA)
val EventUjian = Color(0xFFFFCC80)
val EventPerwalian = Color(0xFFAED581)
val EventLainnya = Color(0xFFE0E0E0)
val EventSusulan = Color(0xFFFFB300)

val utbAcademicEvents = listOf(
    // September 2025
    AcademicEvent("Maulid Nabi Muhammad SAW", "2025-09-05", "2025-09-05", EventLibur, true),
    AcademicEvent("PKKMB/Pena Baru", "2025-09-07", "2025-09-12", EventPerwalian),
    AcademicEvent("Perwalian Semester Ganjil TA 2025/2026", "2025-09-14", "2025-09-19", EventPerwalian),
    AcademicEvent("Perwalian Maba 2025", "2025-09-22", "2025-09-24", EventPerwalian),
    AcademicEvent("Pertemuan 1", "2025-09-29", "2025-10-05", EventKuliah),
    
    // Oktober 2025
    AcademicEvent("Pertemuan 2 - 7", "2025-10-06", "2025-11-09", EventKuliah),
    
    // November 2025
    AcademicEvent("Minggu Kuliah Pengganti", "2025-11-10", "2025-11-15", EventLainnya),
    AcademicEvent("UTS Semester Ganjil 2025/2026", "2025-11-16", "2025-11-26", EventUjian),
    AcademicEvent("UTS Susulan Sem Ganjil", "2025-11-27", "2025-11-30", EventSusulan),
    
    // Desember 2025
    AcademicEvent("Pertemuan 8 - 12", "2025-12-01", "2026-01-04", EventKuliah),
    AcademicEvent("Hari Raya Natal", "2025-12-25", "2025-12-25", EventLibur, true),
    AcademicEvent("Cuti Bersama Hari Raya Natal", "2025-12-26", "2025-12-26", EventLibur, true),
    
    // Januari 2026
    AcademicEvent("Tahun Baru Masehi", "2026-01-01", "2026-01-01", EventLibur, true),
    AcademicEvent("Pertemuan 13 - 14", "2026-01-05", "2026-01-18", EventKuliah),
    AcademicEvent("Isra Mi'raj Nabi Muhammad SAW", "2026-01-16", "2026-01-16", EventLibur, true),
    AcademicEvent("Minggu Kuliah Pengganti", "2026-01-19", "2026-01-24", EventLainnya),
    AcademicEvent("UAS Semester Ganjil 2025/2026", "2026-01-25", "2026-02-04", EventUjian),
    
    // Februari 2026
    AcademicEvent("UAS Susulan Sem Ganjil", "2026-02-05", "2026-02-08", EventSusulan),
    AcademicEvent("Yudisium Ganjil 2025/2026", "2026-02-07", "2026-02-07", Color(0xFF4CAF50)),
    AcademicEvent("Perwalian Semester Genap 2025/2026", "2026-02-09", "2026-02-15", EventPerwalian),
    AcademicEvent("Batas Akhir Pembayaran Semester Genap", "2026-02-14", "2026-02-14", EventLainnya),
    AcademicEvent("Pertemuan 1 - 2", "2026-02-16", "2026-03-01", EventKuliah),
    AcademicEvent("Tahun Baru Imlek 2577", "2026-02-17", "2026-02-17", EventLibur, true),
    
    // Maret 2026
    AcademicEvent("Pertemuan 3 - 5", "2026-03-02", "2026-03-15", EventKuliah),
    AcademicEvent("Hari Suci Nyepi Tahun Baru Saka 1948", "2026-03-15", "2026-03-15", EventLibur, true),
    AcademicEvent("Hari Raya Idul Fitri 1447 Hijriah", "2026-03-20", "2026-03-21", EventLibur, true),
    AcademicEvent("Pertemuan 6", "2026-03-30", "2026-04-05", EventKuliah),
    
    // April 2026
    AcademicEvent("Wafat Yesus Kristus (Jumat Agung)", "2026-04-03", "2026-04-03", EventLibur, true),
    AcademicEvent("Pertemuan 7", "2026-04-06", "2026-04-12", EventKuliah),
    AcademicEvent("Minggu Kuliah Pengganti", "2026-04-13", "2026-04-18", EventLainnya),
    AcademicEvent("UTS Semester Genap 2025/2026", "2026-04-19", "2026-04-29", EventUjian),
    AcademicEvent("Susulan UTS Sem Genap", "2026-04-30", "2026-05-03", EventSusulan),
    
    // Mei 2026
    AcademicEvent("Hari Buruh Internasional", "2026-05-01", "2026-05-01", EventLibur, true),
    AcademicEvent("Pertemuan 8 - 11", "2026-05-04", "2026-05-31", EventKuliah),
    AcademicEvent("Kenaikan Yesus Kristus", "2026-05-14", "2026-05-14", EventLibur, true),
    AcademicEvent("Hari Raya Idul Adha 1447 Hijriah", "2026-05-27", "2026-05-27", EventLibur, true),
    AcademicEvent("Hari Raya Waisak 2570 BE", "2026-05-31", "2026-05-31", EventLibur, true),
    
    // Juni 2026
    AcademicEvent("Hari Lahir Pancasila", "2026-06-01", "2026-06-01", EventLibur, true),
    AcademicEvent("Pertemuan 12 - 14", "2026-06-01", "2026-06-21", EventKuliah),
    AcademicEvent("Tahun Baru Islam 1448 Hijriah", "2026-06-17", "2026-06-17", EventLibur, true),
    AcademicEvent("Minggu Kuliah Pengganti", "2026-06-22", "2026-06-27", EventLainnya),
    AcademicEvent("UAS Semester Genap 2025/2026", "2026-06-28", "2026-07-08", EventUjian),
    
    // Juli 2026
    AcademicEvent("Pendaftaran & Pembayaran Semester Antara", "2026-07-01", "2026-07-04", EventPerwalian),
    AcademicEvent("UAS Susulan Semester Genap 2025/2026", "2026-07-09", "2026-07-12", EventSusulan),
    AcademicEvent("Yudisium Genap 2025/2026", "2026-07-11", "2026-07-11", Color(0xFF4CAF50)),
    AcademicEvent("Perwalian Semester Ganjil 2026/2027", "2026-07-13", "2026-07-19", EventPerwalian),
    AcademicEvent("Pertemuan 1 - 7 Semester Antara", "2026-07-20", "2026-08-01", EventUjian),
    
    // Agustus 2026
    AcademicEvent("Pertemuan 7 - 14 Semester Antara", "2026-08-01", "2026-08-20", EventUjian),
    AcademicEvent("UTS Semester Antara", "2026-08-03", "2026-08-04", EventSusulan),
    AcademicEvent("Hari Kemerdekaan RI", "2026-08-17", "2026-08-17", EventLibur, true),
    AcademicEvent("UAS Semester Antara", "2026-08-21", "2026-08-22", EventUjian),
    AcademicEvent("Maulid Nabi Muhammad SAW", "2026-08-25", "2026-08-25", EventLibur, true)
)

fun getEventsForMonth(year: Int, month: Int): List<AcademicEvent> {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfMonth = cal.timeInMillis
    
    cal.add(Calendar.MONTH, 1)
    val endOfMonth = cal.timeInMillis - 1
    
    return utbAcademicEvents.filter { event ->
        // Does the event span across or into this month?
        (event.startMillis <= endOfMonth && event.endMillis >= startOfMonth)
    }.sortedBy { it.startMillis }
}

fun getColorsForDate(dateMillis: Long): List<Color> {
    val normalizedDateMillis = normalizeStartOfDay(dateMillis)
    val activeEvents = utbAcademicEvents.filter { event ->
        normalizedDateMillis in event.startMillis..event.endMillis
    }
    if (activeEvents.isEmpty()) return emptyList()

    return activeEvents
        .sortedWith(
            compareBy<AcademicEvent>(
                { if (it.isHoliday) 0 else 1 },
                { if (it.color != EventKuliah) 0 else 1 },
                { it.startMillis }
            )
        )
        .map { it.color }
        .distinct()
        .take(2)
}

fun getColorForDate(dateMillis: Long): Color? {
    return getColorsForDate(dateMillis).firstOrNull()
}

private fun normalizeStartOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun normalizeEndOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

package com.kelasin.app.ui.main

import java.util.Locale

internal fun shortHari(hari: String): String {
    return when (hari.trim().lowercase(Locale.ROOT)) {
        "senin" -> "SEN"
        "selasa" -> "SEL"
        "rabu" -> "RAB"
        "kamis" -> "KAM"
        "jumat", "jum'at", "jumat." -> "JUM"
        "sabtu" -> "SAB"
        "minggu" -> "MIN"
        else -> hari.trim().take(4).uppercase(Locale.ROOT)
    }
}

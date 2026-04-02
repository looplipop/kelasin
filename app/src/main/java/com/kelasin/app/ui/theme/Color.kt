package com.kelasin.app.ui.theme

import androidx.compose.ui.graphics.Color

// --- Kelasin Design System — White + Blue Minimalist ---

// Primary Brand
val KelasinPrimary    = Color(0xFF1565C0)  // Deep Blue
val KelasinPrimaryVar = Color(0xFF1E88E5)  // Medium Blue
val KelasinPrimaryLight = Color(0xFFBBDEFB) // Light Blue
val KelasinAccent     = Color(0xFF0D47A1)  // Dark Blue
val KelasinSecondary  = Color(0xFF1E88E5)  // alias — same as PrimaryVar
val KelasinDarkBannerBlue = Color(0xFF2E4D8F) // Lighter dark blue for banner visibility

// Surfaces
val KelasinBackground = Color(0xFFF5F8FF)  // Nearly-white with blue tint
val KelasinSurface    = Color(0xFFFFFFFF)  // Pure white
val KelasinSurfaceVar = Color(0xFFEEF4FF)  // Card/chip subtle blue

// Text
val KelasinOnPrimary   = Color(0xFFFFFFFF)
val KelasinOnBackground = Color(0xFF0D1B2A) // Near-black
val KelasinSubtext     = Color(0xFF546E7A)  // Blue-grey

// Utility
val KelasinDivider     = Color(0xFFE3ECF7)
val KelasinError       = Color(0xFFD32F2F)
val KelasinWarning     = Color(0xFFF57C00)
val KelasinSuccess     = Color(0xFF388E3C)

// Matkul palette — 10 distinct colours for per-Matkul coloring
val MatkulColors = listOf(
    "#1565C0", // Deep Blue
    "#2E7D32", // Green
    "#6A1B9A", // Purple
    "#E65100", // Deep Orange
    "#00838F", // Teal
    "#AD1457", // Pink
    "#4527A0", // Indigo
    "#558B2F", // Light Green
    "#00695C", // Dark Teal
    "#1565C0", // fallback
)

// Priority colors
val PriorityTinggi  = Color(0xFFD32F2F)
val PrioritasSedang = Color(0xFFF57C00)
val PrioritasRendah = Color(0xFF388E3C)

// Absensi status colors
val StatusHadir = Color(0xFF388E3C)
val StatusSakit = Color(0xFFF57C00)
val StatusIzin  = Color(0xFF1976D2)
val StatusAlpha = Color(0xFFD32F2F)

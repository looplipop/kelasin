package com.kelasin.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kelasin.app.R

// Google Fonts Setup
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PlusJakartaSans = FontFamily(
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Bold)
)

val Inter = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium)
)

// Dynamic Typography
val KelasinTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = Inter),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = Inter),
    bodySmall = Typography().bodySmall.copy(fontFamily = Inter),
    labelLarge = Typography().labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    labelMedium = Typography().labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    labelSmall = Typography().labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium)
)

private val LightColorScheme = lightColorScheme(
    primary          = KelasinPrimary,
    onPrimary        = KelasinOnPrimary,
    primaryContainer = KelasinPrimaryLight,
    secondary        = KelasinPrimaryVar,
    background       = KelasinBackground,
    surface          = KelasinSurface,
    surfaceVariant   = KelasinSurfaceVar,
    onBackground     = KelasinOnBackground,
    onSurface        = KelasinOnBackground,
    error            = KelasinError,
)

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF60A5FA), // Lighter blue for dark mode
    onPrimary        = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    secondary        = Color(0xFF93C5FD),
    background       = Color(0xFF0F172A), // Slate-900
    surface          = Color(0xFF273549), // Slightly lighter Slate for better card visibility
    surfaceVariant   = Color(0xFF3B4D66), 
    onBackground     = Color(0xFFF1F5F9), // Slate-100
    onSurface        = Color(0xFFF1F5F9),
    error            = Color(0xFFF87171),
)

private val KelasinShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Local Theme mode provider
val LocalThemeMode = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun KelasinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalThemeMode provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = KelasinTypography,
            shapes = KelasinShapes,
            content     = content
        )
    }
}

/**
 * Call this from any screen to dynamically change the status bar color/icon appearance.
 * useDarkIcons = true means status bar icons akan GELAP (untuk background terang)
 * useDarkIcons = false means status bar icons akan TERANG/putih (untuk background gelap)
 */
@Composable
fun DynamicStatusBar(statusBarColor: Color, useDarkIcons: Boolean? = null) {
    val view = LocalView.current
    val darkTheme = LocalThemeMode.current
    
    // Auto-detect: jika background terang (luminance > 0.5), gunakan dark icons
    val autoDetectDarkIcons = statusBarColor.luminance() > 0.5f
    
    // Final decision: jika dalam dark theme dan background gelap, paksa light icons
    val finalDarkIcons = when {
        useDarkIcons != null -> useDarkIcons // Manual override
        darkTheme && !autoDetectDarkIcons -> false // Dark theme + dark background = light icons
        else -> autoDetectDarkIcons // Auto-detect based on luminance
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor.toArgb()
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = finalDarkIcons
        }
    }
}

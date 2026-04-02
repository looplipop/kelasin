package com.kelasin.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.LocalThemeMode

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (String) -> Unit,
    onGoRegister: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val transition = rememberInfiniteTransition(label = "role-selection-bg")
    val orbTopY by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb-top"
    )
    val orbBottomY by transition.animateFloat(
        initialValue = 8f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb-bottom"
    )
    val darkTheme = LocalThemeMode.current
    val bgGradient = if (darkTheme) {
        listOf(Color(0xFF0D1B2A), Color(0xFF1454A3), Color(0xFF1E88E5))
    } else {
        listOf(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF60A5FA))
    }
    val primaryTextColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (darkTheme) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)

    DynamicStatusBar(
        statusBarColor = bgGradient.first(),
        useDarkIcons = !darkTheme
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    bgGradient
                )
            )
    ) {
        Box(
            Modifier
                .offset(x = (-48).dp, y = (32 + orbTopY).dp)
                .size(210.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0x301E88E5) else Color(0x2393C5FD))
                .blur(56.dp)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 36.dp, y = (-24 + orbBottomY).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0x280D47A1) else Color(0x22FFFFFF))
                .blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(450)) + slideInVertically { -32 } + scaleIn(initialScale = 0.94f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Kelasin",
                        color = primaryTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pilih role untuk masuk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(650, delayMillis = 120)) + slideInVertically { 120 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RoleOptionCard(
                        title = "Masuk sebagai Mahasigma",
                        subtitle = "Akses jadwal, tugas, materi, catatan, dan absensi",
                        icon = Icons.Filled.School,
                        onClick = { onRoleSelected("MAHASIGMA") }
                    )
                    RoleOptionCard(
                        title = "Masuk sebagai Atmin",
                        subtitle = "Kelola data mata kuliah, tugas, materi, dan absensi kelas",
                        icon = Icons.Filled.AdminPanelSettings,
                        onClick = { onRoleSelected("ATMIN") }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val darkTheme = LocalThemeMode.current
    val cardBg = if (darkTheme) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.86f)
    val iconBoxBg = if (darkTheme) Color.White.copy(alpha = 0.18f) else KelasinPrimary.copy(alpha = 0.15f)
    val titleColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (darkTheme) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTint = if (darkTheme) Color.White else KelasinPrimary

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBoxBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
            Icon(
                Icons.Filled.ArrowForward,
                null,
                tint = iconTint.copy(alpha = 0.85f)
            )
        }
    }
}

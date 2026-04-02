package com.kelasin.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.LocalThemeMode

@Composable
fun LoginScreen(
    authState: AuthState,
    role: String,
    onLogin: (identifier: String, password: String) -> Unit,
    onGoRegister: () -> Unit,
    onBackToRoleSelection: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val errorMsg = if (authState is AuthState.Error) authState.message else null

    // ── Entry animations ──
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Infinite floating animation for logo orbs
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orb1Y by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f, label = "orb1",
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val orb2Y by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = -6f, label = "orb2",
        animationSpec = infiniteRepeatable(tween(3100, easing = LinearEasing), RepeatMode.Reverse)
    )
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f, label = "logoScale",
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )
    val darkTheme = LocalThemeMode.current
    val bgGradient = if (darkTheme) {
        listOf(Color(0xFF0D1B2A), Color(0xFF1565C0), Color(0xFF1E88E5))
    } else {
        listOf(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF60A5FA))
    }
    val primaryTextColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (darkTheme) Color.White.copy(0.70f) else MaterialTheme.colorScheme.onBackground.copy(0.72f)
    val cardBg = if (darkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.82f)
    val cardOverlayTop = if (darkTheme) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.95f)
    val cardOverlayBottom = if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.78f)
    val fieldBorderFocused = if (darkTheme) Color.White.copy(0.7f) else KelasinPrimary.copy(alpha = 0.65f)
    val fieldBorderUnfocused = if (darkTheme) Color.White.copy(0.3f) else KelasinPrimary.copy(alpha = 0.35f)
    val fieldContainerFocused = if (darkTheme) Color.White.copy(0.08f) else Color.White.copy(0.92f)
    val fieldContainerUnfocused = if (darkTheme) Color.White.copy(0.04f) else Color.White.copy(0.84f)
    val fieldTextColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val iconTintColor = if (darkTheme) Color.White.copy(0.8f) else KelasinPrimary

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
        // ── Decorative blurred orbs ──
        Box(
            Modifier
                .offset(y = orb1Y.dp)
                .size(220.dp)
                .offset((-60).dp, 30.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0x350D47A1) else Color(0x2393C5FD))
                .blur(60.dp)
        )
        Box(
            Modifier
                .offset(y = orb2Y.dp)
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(20.dp, 120.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0x301E88E5) else Color(0x22FFFFFF))
                .blur(50.dp)
        )
        Box(
            Modifier
                .size(160.dp)
                .align(Alignment.BottomStart)
                .offset((-30).dp, 30.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0x281565C0) else Color(0x22BFDBFE))
                .blur(40.dp)
        )

        // ── Main content scrollable column ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()   // Respects transparent status bar
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Back button row
            AnimatedVisibility(visible = visible, enter = slideInVertically { -40 } + fadeIn()) {
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onBackToRoleSelection) {
                        Icon(Icons.Filled.ArrowBack, null, tint = primaryTextColor.copy(0.86f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ganti Role", color = primaryTextColor.copy(0.86f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Animated logo ──
            AnimatedVisibility(visible = visible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(600))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .scale(logoScale)
                            .size(100.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)))
                            )
                            .then(
                                Modifier.background(Color.White.copy(0.08f))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glassmorphism inner ring
                        Box(
                            Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.School,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "Kelasin",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        color = primaryTextColor,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Manajemen Kuliah Mahasiswa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Glassmorphism Login Card ──
            AnimatedVisibility(visible = visible, enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { 100 } + fadeIn(tween(700, delayMillis = 200))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(cardBg)
                        .background(
                            Brush.verticalGradient(
                                listOf(cardOverlayTop, cardOverlayBottom)
                            )
                        )
                ) {
                    Column(Modifier.padding(28.dp)) {
                        Text(
                            "Masuk sebagai $role",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            "Selamat datang kembali",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )
                        Spacer(Modifier.height(24.dp))

                        // Identifier field (email or username)
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("Email / Username", color = secondaryTextColor) },
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = iconTintColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = fieldBorderFocused,
                                unfocusedBorderColor = fieldBorderUnfocused,
                                focusedTextColor = fieldTextColor,
                                unfocusedTextColor = fieldTextColor,
                                cursorColor = fieldTextColor,
                                focusedContainerColor = fieldContainerFocused,
                                unfocusedContainerColor = fieldContainerUnfocused
                            )
                        )
                        Spacer(Modifier.height(14.dp))

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = secondaryTextColor) },
                            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = iconTintColor) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        null, tint = iconTintColor
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = fieldBorderFocused,
                                unfocusedBorderColor = fieldBorderUnfocused,
                                focusedTextColor = fieldTextColor,
                                unfocusedTextColor = fieldTextColor,
                                cursorColor = fieldTextColor,
                                focusedContainerColor = fieldContainerFocused,
                                unfocusedContainerColor = fieldContainerUnfocused
                            )
                        )

                        // Error message
                        AnimatedVisibility(visible = errorMsg != null) {
                            Text(
                                errorMsg ?: "",
                                color = Color(0xFFFF8A80),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Login button — gradient, pill shape
                        Button(
                            onClick = { onLogin(identifier, password) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            enabled = authState !is AuthState.Loading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (authState is AuthState.Loading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Login, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Masuk", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (role != "ATMIN") {
                            // Register row
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Belum punya akun?", color = secondaryTextColor, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = onGoRegister) {
                                    Text("Daftar", color = if (darkTheme) Color(0xFF90CAF9) else KelasinPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

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
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinError
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.LocalThemeMode

@Composable
fun RegisterScreen(
    authState: AuthState,
    role: String,
    onRegister: (nama: String, username: String, email: String, password: String, confirm: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    
    // Assignment Fields
    var gender by remember { mutableStateOf("Laki-laki") }
    val hobbies = remember { mutableStateListOf<String>() }
    var prodi by remember { mutableStateOf("Informatika") }
    var expandedProdi by remember { mutableStateOf(false) }
    val prodis = listOf("Informatika", "Sistem Informasi", "Teknik Elektro", "Teknik Industri", "Arsitektur")
    var showConfirmDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }

    val errorMsg = if (authState is AuthState.Error) authState.message else null
    LaunchedEffect(Unit) { visible = true }

    val transition = rememberInfiniteTransition(label = "register-bg")
    val orb1Y by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )
    val orb2Y by transition.animateFloat(
        initialValue = 8f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )
    val darkTheme = LocalThemeMode.current
    val bgGradient = if (darkTheme) {
        listOf(Color(0xFF0D1B2A), Color(0xFF1565C0), Color(0xFF1E88E5))
    } else {
        listOf(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF60A5FA))
    }
    val primaryTextColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (darkTheme) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
    val cardBg = if (darkTheme) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.84f)
    val cardOverlayTop = if (darkTheme) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.95f)
    val cardOverlayBottom = if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.78f)
    val iconTintColor = if (darkTheme) Color.White.copy(alpha = 0.85f) else KelasinPrimary

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
                .offset(x = (-50).dp, y = (34 + orb1Y).dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0x340D47A1) else Color(0x2393C5FD))
                .blur(56.dp)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 26.dp, y = (-20 + orb2Y).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(if (darkTheme) Color(0x2E1E88E5) else Color(0x22FFFFFF))
                .blur(54.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -40 }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBackToLogin, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Icon(Icons.Filled.ArrowBack, null, tint = primaryTextColor.copy(alpha = 0.86f))
                        Spacer(Modifier.size(4.dp))
                        Text("Kembali", color = primaryTextColor.copy(alpha = 0.86f))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn(tween(550))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PersonAdd, null, tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Daftar Akun $role",
                        style = MaterialTheme.typography.headlineMedium,
                        color = primaryTextColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Lengkapi data di bawah untuk mulai memakai Kelasin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(700, delayMillis = 150)) + slideInVertically { 100 }
    ) {
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
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Nama
                val namaError = if (nama.isNotEmpty() && nama.trim().length < 2) "Nama terlalu pendek" else null
                GlassTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = "Nama Lengkap",
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = iconTintColor) },
                    isError = namaError != null
                )
                if (namaError != null) {
                    Text(namaError, color = Color(0xFFFFAB91), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 14.dp))
                }

                // Username
                val usernameError = when {
                    username.isNotEmpty() && username.length < 3 -> "Username minimal 3 karakter"
                    username.isNotEmpty() && username.contains(" ") -> "Username tidak boleh ada spasi"
                    else -> null
                }
                GlassTextField(
                    value = username,
                    onValueChange = { username = it.replace(" ", "") },
                    label = "Username",
                    leadingIcon = { Icon(Icons.Filled.AccountCircle, null, tint = iconTintColor) },
                    isError = usernameError != null
                )
                if (usernameError != null) {
                    Text(usernameError, color = Color(0xFFFFAB91), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 14.dp))
                }

                // Email
                val emailError = if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Format email tidak valid" else null
                GlassTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = { Icon(Icons.Filled.Email, null, tint = iconTintColor) },
                    isError = emailError != null
                )
                if (emailError != null) {
                    Text(emailError, color = Color(0xFFFFAB91), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 14.dp))
                }

                // Password
                val pwStrength = when {
                    password.length >= 10 && password.any { it.isUpperCase() } && password.any { it.isDigit() } -> 3
                    password.length >= 8 -> 2
                    password.isNotEmpty() -> 1
                    else -> 0
                }
                val pwStrengthLabel = listOf("", "Lemah", "OK", "Kuat")[pwStrength]
                val pwStrengthColor = listOf(Color.Transparent, KelasinError, Color(0xFFFF8F00), Color(0xFF2E7D32))[pwStrength]
                val passwordError = if (password.isNotEmpty() && password.length < 8) "Password minimal 8 karakter" else null

                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = iconTintColor) },
                    isError = passwordError != null,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                null,
                                tint = iconTintColor
                            )
                        }
                    }
                )
                // Password strength bar
                if (password.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) { i ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (i < pwStrength) pwStrengthColor else (if (darkTheme) Color.White.copy(0.15f) else Color.Black.copy(0.1f)))
                                )
                            }
                        }
                        Text(pwStrengthLabel, color = pwStrengthColor, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 2.dp))
                    }
                }
                if (passwordError != null) {
                    Text(passwordError, color = Color(0xFFFFAB91), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 14.dp))
                }

                // Confirm password
                val confirmError = if (confirm.isNotEmpty() && confirm != password) "Password tidak cocok" else null
                GlassTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = "Konfirmasi Password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = iconTintColor) },
                    isError = confirmError != null,
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                null,
                                tint = iconTintColor
                            )
                        }
                    }
                )
                if (confirmError != null) {
                    Text(confirmError, color = Color(0xFFFFAB91), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 24.dp))
                }

                Spacer(Modifier.height(8.dp))
                Divider(color = primaryTextColor.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))

                // --- Assignment Requirement: Selection Controls (RadioGroup) ---
                Text("Jenis Kelamin", style = MaterialTheme.typography.titleSmall, color = primaryTextColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("Laki-laki", "Perempuan").forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = item }) {
                            RadioButton(
                                selected = gender == item,
                                onClick = { gender = item },
                                colors = RadioButtonDefaults.colors(selectedColor = KelasinPrimary, unselectedColor = secondaryTextColor)
                            )
                            Text(item, color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // --- Assignment Requirement: Checkbox (min 3) ---
                Text("Hobi (Pilih min. 3)", style = MaterialTheme.typography.titleSmall, color = primaryTextColor)
                val hobbyList = listOf("Coding", "Membaca", "Olahraga", "Musik", "Traveling")
                Column {
                    hobbyList.chunked(2).forEach { row ->
                        Row {
                            row.forEach { h ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable {
                                    if (h in hobbies) hobbies.remove(h) else hobbies.add(h)
                                }) {
                                    Checkbox(
                                        checked = h in hobbies,
                                        onCheckedChange = { if (it) hobbies.add(h) else hobbies.remove(h) },
                                        colors = CheckboxDefaults.colors(checkedColor = KelasinPrimary, uncheckedColor = secondaryTextColor)
                                    )
                                    Text(h, color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                val hobbyError = if (hobbies.isNotEmpty() && hobbies.size < 3) "Pilih minimal 3 hobi" else null
                if (hobbyError != null) {
                    Text(hobbyError, color = Color(0xFFFFAB91), style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.height(8.dp))

                // --- Assignment Requirement: Spinner (ExposedDropdownMenu) ---
                Text("Program Studi", style = MaterialTheme.typography.titleSmall, color = primaryTextColor)
                Spacer(Modifier.height(4.dp))
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = expandedProdi,
                    onExpandedChange = { expandedProdi = !expandedProdi }
                ) {
                    OutlinedTextField(
                        value = prodi,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProdi) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor,
                            focusedContainerColor = cardBg.copy(0.3f),
                            unfocusedContainerColor = cardBg.copy(0.1f)
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProdi,
                        onDismissRequest = { expandedProdi = false }
                    ) {
                        prodis.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    prodi = selection
                                    expandedProdi = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(visible = errorMsg != null) {
                    Text(
                        errorMsg ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFAB91)
                    )
                }

                Spacer(Modifier.height(4.dp))

                val isFormValid = namaError == null && usernameError == null && emailError == null &&
                    passwordError == null && confirmError == null && hobbyError == null &&
                    nama.isNotBlank() && username.isNotBlank() && email.isNotBlank() &&
                    password.isNotBlank() && confirm.isNotBlank() && hobbies.size >= 3

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = authState !is AuthState.Loading && isFormValid,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isFormValid)
                                    Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1565C0)))
                                else
                                    Brush.horizontalGradient(listOf(Color.Gray.copy(0.4f), Color.Gray.copy(0.4f))),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.4.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PersonAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                                Text("Daftar Sekarang", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sudah punya akun?", color = secondaryTextColor)
                    TextButton(onClick = onBackToLogin) {
                        Icon(Icons.Filled.Login, null, tint = if (darkTheme) Color(0xFF90CAF9) else KelasinPrimary, modifier = Modifier.size(16.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                        Text("Masuk", color = if (darkTheme) Color(0xFF90CAF9) else KelasinPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }  // Box
        }  // AnimatedVisibility
        }  // Column (scrollable)

        // --- Assignment Requirement: AlertDialog Confirmation ---
        if (showConfirmDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Konfirmasi Pendaftaran") },
                text = { Text("Apakah data yang Anda masukkan sudah benar?\n\nNama: $nama\nProdi: $prodi\nGender: $gender") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        onRegister(nama.trim(), username.trim(), email.trim(), password, confirm)
                    }) {
                        Text("Ya, Daftar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cek Lagi")
                    }
                }
            )
        }
    }  // Box (gradient bg)
}  // RegisterScreen

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false
) {
    val darkTheme = LocalThemeMode.current
    val labelColor = if (darkTheme) Color.White.copy(alpha = 0.74f) else MaterialTheme.colorScheme.onSurfaceVariant
    val borderFocused = when {
        isError -> Color(0xFFFF8A80)
        else -> if (darkTheme) Color.White.copy(alpha = 0.7f) else KelasinPrimary.copy(alpha = 0.65f)
    }
    val borderUnfocused = when {
        isError -> Color(0xFFFF8A80).copy(alpha = 0.6f)
        else -> if (darkTheme) Color.White.copy(alpha = 0.28f) else KelasinPrimary.copy(alpha = 0.32f)
    }
    val textColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val containerFocused = if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.92f)
    val containerUnfocused = if (darkTheme) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.84f)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label, color = labelColor) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderFocused,
            unfocusedBorderColor = borderUnfocused,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = textColor,
            focusedContainerColor = containerFocused,
            unfocusedContainerColor = containerUnfocused,
            errorBorderColor = Color(0xFFFF8A80),
            errorTextColor = textColor
        )
    )
}

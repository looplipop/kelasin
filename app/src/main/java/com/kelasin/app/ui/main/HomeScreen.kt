package com.kelasin.app.ui.main

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelasin.app.data.entity.MataKuliahEntity
import com.kelasin.app.data.entity.TugasEntity
import com.kelasin.app.ui.theme.KelasinSubtext
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.data.repository.TugasRepository
import com.kelasin.app.data.repository.AuthRepository
import com.kelasin.app.ui.detail.DetailActivity
import com.kelasin.app.data.repository.UserPreferencesRepository
import com.kelasin.app.data.supabase.SupabaseRestClient
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.KelasinSecondary
import com.kelasin.app.ui.theme.KelasinSuccess
import com.kelasin.app.ui.theme.KelasinError
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinPrimaryLight
import com.kelasin.app.ui.theme.KelasinDarkBannerBlue
import com.kelasin.app.ui.components.InitialsAvatar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.io.IOException
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: String,
    userName: String,
    userRole: String,
    authRepo: AuthRepository,
    tugasRepo: TugasRepository,
    mkRepo: MataKuliahRepository,
    prefsRepo: UserPreferencesRepository,
    themeMode: Int,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val upcomingTugas by tugasRepo.getUpcoming(userId).collectAsStateWithLifecycle(emptyList())
    val mataKuliahList by mkRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    val today = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("id", "ID")) ?: "Senin"
    val todayMatkul by mkRepo.getByHari(userId, today).collectAsStateWithLifecycle(emptyList())
    val mataKuliahNameById = remember(mataKuliahList) { mataKuliahList.associate { it.id to it.nama } }
    val scope = rememberCoroutineScope()
    var checkingCloud by remember { mutableStateOf(false)     }
    var cloudStatusMessage by remember { mutableStateOf<String?>(null) }
    var cloudReady by remember { mutableStateOf<Boolean?>(null) }
    val themeRevealProgress = remember { Animatable(0f) }
    val themeRevealOpacity = remember { Animatable(0f) }
    var themeRevealColor by remember { mutableStateOf<Color?>(null) }
    var isThemeAnimating by remember { mutableStateOf(false) }
    // Theme transition tuning (you can tweak these values)
    val themeSwitchTriggerProgress = 0.48f
    val themePreSwitchDurationMs = 120
    val themeFinishDurationMs = 220
    val themeFadeDurationMs = 120
    val themeOverlayStartAlpha = 0.78f
    val themeRevealOriginInsetX = 42.dp
    val themeRevealOriginInsetY = 46.dp
    val themeRevealRadiusMultiplier = 0.76f
    val isDarkModeActive = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    LaunchedEffect(Unit) {
        checkingCloud = true
        val probe = runCatching { SupabaseRestClient.checkConnection() }
        val status = probe.getOrDefault(false)
        cloudReady = status
        cloudStatusMessage = if (status) "Cloud database aktif" else cloudStatusText(probe.exceptionOrNull())
        checkingCloud = false
    }
    
    var showProfileSheet by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<com.kelasin.app.data.entity.UserEntity?>(null) }

    LaunchedEffect(userId) {
        userProfile = authRepo.getUserProfile(userId)
    }

    var showLongPressDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                val topBarBg = if (isDarkModeActive) KelasinDarkBannerBlue else KelasinPrimary
                Box(
                    modifier = Modifier.fillMaxWidth().background(topBarBg)
                ) {
                    Column {
                        Spacer(modifier = Modifier.statusBarsPadding())
                        DynamicStatusBar(
                            statusBarColor = topBarBg,
                            useDarkIcons = !isDarkModeActive
                        )
                        TopAppBar(
                            title = { Text("Kelasin") },
                            actions = {
                                IconButton(
                                    onClick = { showProfileSheet = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    InitialsAvatar(
                                        name = userProfile?.nama ?: userName, 
                                        size = 32.dp, 
                                        profilePicUrl = userProfile?.displayProfilePic
                                    )
                                }
                                IconButton(
                                    enabled = !isThemeAnimating,
                                    onClick = {
                                        scope.launch {
                                            if (isThemeAnimating) return@launch
                                            isThemeAnimating = true
                                            try {
                                                val targetDarkMode = !isDarkModeActive
                                                themeRevealColor = if (targetDarkMode) Color(0xFF0F172A) else Color(0xFFEFF6FF)
                                                themeRevealProgress.snapTo(0f)
                                                themeRevealOpacity.snapTo(themeOverlayStartAlpha)

                                                // Keep old theme visible first, then let the new color "erode" it smoothly.
                                                themeRevealProgress.animateTo(
                                                    targetValue = themeSwitchTriggerProgress,
                                                    animationSpec = tween(durationMillis = themePreSwitchDurationMs, easing = FastOutSlowInEasing)
                                                )

                                                prefsRepo.setThemeMode(if (targetDarkMode) 2 else 1)

                                                val expandJob = launch {
                                                    themeRevealProgress.animateTo(
                                                        targetValue = 1f,
                                                        animationSpec = tween(durationMillis = themeFinishDurationMs, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                                val fadeJob = launch {
                                                    themeRevealOpacity.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = tween(durationMillis = themeFadeDurationMs, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                                expandJob.join()
                                                fadeJob.join()
                                            } finally {
                                                themeRevealProgress.snapTo(0f)
                                                themeRevealOpacity.snapTo(0f)
                                                themeRevealColor = null
                                                isThemeAnimating = false
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isDarkModeActive) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = if (isDarkModeActive) "Ganti ke Light Mode" else "Ganti ke Dark Mode"
                                    )
                                }
                                IconButton(onClick = onLogout) { 
                                    Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = Color.White.copy(alpha = 0.8f)) 
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                // Enhanced Greeting Card with animated background & date
                var greetingVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(100); greetingVisible = true }
                val greeting = remember {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    when {
                        hour in 5..11 -> "Selamat Pagi"
                        hour in 12..17 -> "Selamat Sore"
                        else -> "Selamat Malam"
                    }
                }
                val dateFmt = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id")) }
                val dateStr = remember { dateFmt.format(Date()) }

                val greetingBgColor = if (isDarkModeActive) {
                    KelasinDarkBannerBlue
                } else {
                    KelasinPrimary // Sama dengan topbar saat light
                }

                AnimatedVisibility(visible = greetingVisible, enter = slideInVertically(spring(dampingRatio = 0.7f)) { -60 } + fadeIn(tween(400))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(greetingBgColor)
                    ) {
                        // Decorative circles
                        Box(Modifier.size(120.dp).offset(x = (-30).dp, y = (-30).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
                        Box(Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))

                        Column(Modifier.padding(20.dp)) {
                            Text(greeting + ",", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                            Text(
                                userName.split(" ").take(2).joinToString(" ").ifBlank { userName },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarToday, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }



            // Cloud Database Section
            item {
                val pulseAnim = rememberInfiniteTransition(label = "pulse")
                val pulseScale by pulseAnim.animateFloat(
                    initialValue = 1f, targetValue = 1.6f, label = "pulse-scale",
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Cloud,
                                null,
                                tint = KelasinPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Cloud Database",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Data aplikasi tersimpan langsung di Supabase Postgres",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val indicatorColor = when (cloudReady) {
                                true -> KelasinSuccess
                                false -> KelasinError
                                null -> KelasinPrimary
                            }
                            // Pulsating indicator
                            Box(contentAlignment = Alignment.Center) {
                                if (cloudReady == null) {
                                    Box(
                                        modifier = Modifier
                                            .size((10 * pulseScale).dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(indicatorColor.copy(alpha = 0.3f))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(indicatorColor)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                cloudStatusMessage ?: "Mengecek koneksi cloud...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (cloudReady == false) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Info,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Mode offline aktif, data lokal tetap dipakai.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                checkingCloud = true
                                scope.launch {
                                    val probe = runCatching { SupabaseRestClient.checkConnection() }
                                    val status = probe.getOrDefault(false)
                                    cloudReady = status
                                    cloudStatusMessage = if (status) "Cloud database aktif" else cloudStatusText(probe.exceptionOrNull())
                                    checkingCloud = false
                                }
                            },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { 
                                        showLongPressDialog = true 
                                    }
                                )
                            },
                            enabled = !checkingCloud,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KelasinPrimary)
                        ) {
                            if (checkingCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = KelasinPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("Cek Koneksi Cloud")
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = KelasinPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Jadwal Hari Ini ($today)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            if (todayMatkul.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("Tidak ada kuliah hari ini", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                itemsIndexed(todayMatkul) { index, mk ->
                    BouncyListItem(index = index) {
                        JadwalCard(mk) {
                            context.startActivity(Intent(context, DetailActivity::class.java).apply {
                                putExtra("TYPE", "MATKUL")
                                putExtra("ID", mk.id)
                            })
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = KelasinPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Tugas Mendekat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            if (upcomingTugas.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("Tidak ada tugas mendekat", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                itemsIndexed(upcomingTugas) { index, tugas ->
                    BouncyListItem(index = index) {
                        TugasHomeCard(
                            tugas = tugas,
                            mataKuliahNama = mataKuliahNameById[tugas.mataKuliahId] ?: "Mata kuliah tidak ditemukan"
                        ) {
                            context.startActivity(Intent(context, DetailActivity::class.java).apply {
                                putExtra("TYPE", "TUGAS")
                                putExtra("ID", tugas.id)
                            })
                        }
                    }
                }
            }
        }
        }

        themeRevealColor?.let { revealColor ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
                val radius = (maxRadius * themeRevealRadiusMultiplier * themeRevealProgress.value).coerceAtLeast(1f)
                val centerPoint = Offset(size.width - themeRevealOriginInsetX.toPx(), themeRevealOriginInsetY.toPx())
                val overlayAlpha = themeRevealOpacity.value
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to revealColor.copy(alpha = 1.0f * overlayAlpha),
                            0.55f to revealColor.copy(alpha = 0.96f * overlayAlpha),
                            0.82f to revealColor.copy(alpha = 0.78f * overlayAlpha),
                            1f to revealColor.copy(alpha = 0.18f * overlayAlpha)
                        ),
                        center = centerPoint,
                        radius = radius
                    ),
                    radius = radius,
                    center = centerPoint
                )
            }
        }
        }
    }
    
    // --- Assignment Requirement: Gesture Interaction (Long Press on Button) ---
    if (showLongPressDialog) {
        AlertDialog(
            onDismissRequest = { showLongPressDialog = false },
            title = { Text("Informasi Sistem (Long Press)") },
            text = { Text("Versi Aplikasi: 2.1.0\nRole: $userRole\nStatus Cloud: ${if (cloudReady == true) "Terhubung" else "Terputus"}\n\nIni adalah aksi tambahan yang dipicu melalui Long Press pada tombol.") },
            confirmButton = {
                TextButton(onClick = { showLongPressDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
    
    if (showProfileSheet) {
        var isEditing by remember { mutableStateOf(false) }
        var editNama by remember { mutableStateOf(userProfile?.nama ?: userName) }
        var editUsername by remember { mutableStateOf(userProfile?.username ?: "") }
        var editBio by remember { mutableStateOf(userProfile?.displayBio ?: "") }
        var editProfilePicBase64 by remember { mutableStateOf<String?>(userProfile?.displayProfilePic?.takeIf { it.isNotBlank() }) }
        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val base64 = runCatching {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        val ratio = 512f / maxOf(bitmap.width, bitmap.height)
                        val scaled = if (ratio < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
                        val outputStream = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream)
                        android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                    }.getOrNull()
                    if (base64 != null) {
                        editProfilePicBase64 = base64
                    }
                }
            }
        }

        ModalBottomSheet(onDismissRequest = { showProfileSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userProfile == null) {
                    CircularProgressIndicator()
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable(enabled = isEditing) {
                            photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            InitialsAvatar(
                                name = editNama.ifBlank { "User" }, 
                                size = 80.dp, 
                                profilePicUrl = if (isEditing) editProfilePicBase64 else userProfile?.displayProfilePic
                            )
                            if (isEditing) {
                                Box(modifier = Modifier.size(80.dp).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Photo", tint = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Profil Pengguna", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    if (isEditing) {
                        OutlinedTextField(
                            value = editNama,
                            onValueChange = { editNama = it },
                            label = { Text("Nama Lengkap") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { editUsername = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(userProfile!!.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = KelasinError, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            OutlinedButton(onClick = { isEditing = false; errorMessage = null }) {
                                Text("Batal")
                            }
                            Button(onClick = {
                                isSaving = true
                                errorMessage = null
                                scope.launch {
                                    val res = authRepo.updateProfile(userId, editNama, editUsername, editProfilePicBase64, editBio)
                                    isSaving = false
                                    if (res.isSuccess) {
                                        userProfile = res.getOrNull()
                                        isEditing = false
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "Gagal memperbarui profil"
                                    }
                                }
                            }, enabled = !isSaving) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Simpan")
                                }
                            }
                        }
                    } else {
                        Text(userProfile!!.nama, style = MaterialTheme.typography.titleMedium)
                        Text("@${userProfile!!.username}", style = MaterialTheme.typography.bodyMedium, color = KelasinPrimary)
                        Text(userProfile!!.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { isEditing = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Profil")
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    LaunchedEffect(showProfileSheet) {
        if (showProfileSheet) {
            userProfile = authRepo.getUserProfile(userId)
        }
    }
}

private fun cloudStatusText(error: Throwable?): String {
    val cause = error?.message.orEmpty().lowercase()
    if ("config belum lengkap" in cause || "supabase_url" in cause || "supabase_publishable_key" in cause) {
        return "Cloud belum dikonfigurasi. Aplikasi jalan di mode offline."
    }
    if (error is IOException || "unable to resolve host" in cause || "timeout" in cause || "failed to connect" in cause) {
        return "Cloud tidak dapat dijangkau. Aplikasi jalan di mode offline."
    }
    return "Cloud database belum terhubung"
}

@Composable
fun JadwalCard(mk: MataKuliahEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(KelasinPrimary), contentAlignment = Alignment.Center) {
                Text(mk.nama.take(2).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(mk.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${mk.jamMulai} - ${mk.jamSelesai} | ${mk.ruangan}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TugasHomeCard(
    tugas: TugasEntity,
    mataKuliahNama: String,
    onClick: () -> Unit
) {
    val deadlineText = remember(tugas.deadline) { formatUpcomingDeadlineText(tugas.deadline) }
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = KelasinPrimary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    mataKuliahNama,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(tugas.judul, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Deadline: $deadlineText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = {}, label = { Text(tugas.prioritas.name, style = MaterialTheme.typography.labelSmall) })
        }
    }
}

private fun formatUpcomingDeadlineText(deadlineMillis: Long): String {
    val dayMillis = 24L * 60L * 60L * 1000L
    val nowCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val deadlineCal = Calendar.getInstance().apply {
        timeInMillis = deadlineMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val totalDays = ((deadlineCal.timeInMillis - nowCal.timeInMillis) / dayMillis).coerceAtLeast(0L)
    val timeText = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(deadlineMillis))
    return when {
        totalDays < 7L -> "${totalDays} hari lagi ($timeText)"
        totalDays < 365L -> {
            val weeks = totalDays / 7
            val days = totalDays % 7
            "${weeks} minggu ${days} hari lagi ($timeText)"
        }

        else -> {
            val years = totalDays / 365
            val remainingDays = totalDays % 365
            val weeks = remainingDays / 7
            val days = remainingDays % 7
            when {
                weeks > 0 && days > 0 -> "${years} tahun ${weeks} minggu ${days} hari lagi ($timeText)"
                weeks > 0 -> "${years} tahun ${weeks} minggu lagi ($timeText)"
                else -> "${years} tahun ${days} hari lagi ($timeText)"
            }
        }
    }
}

@Composable
fun BouncyListItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        isVisible = true
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.7f,
                stiffness = 400f
            )
        ) + androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    ) {
        content()
    }
}

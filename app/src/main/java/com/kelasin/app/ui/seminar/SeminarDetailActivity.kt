package com.kelasin.app.ui.seminar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelasin.app.data.repository.SeminarItem
import com.kelasin.app.data.repository.SeminarRegistrationRepository
import com.kelasin.app.data.repository.SeminarRepository
import com.kelasin.app.ui.theme.*
import kotlinx.coroutines.launch

class SeminarDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra(SeminarContract.EXTRA_USER_ID).orEmpty()
        val userName = intent.getStringExtra(SeminarContract.EXTRA_USER_NAME).orEmpty()
        val userRole = intent.getStringExtra(SeminarContract.EXTRA_USER_ROLE).orEmpty()
        val seminarId = intent.getStringExtra(SeminarContract.EXTRA_SEMINAR_ID).orEmpty()

        setContent {
            KelasinTheme {
                SeminarDetailScreen(
                    userId = userId,
                    userName = userName,
                    userRole = userRole,
                    seminarId = seminarId,
                    seminarRepo = remember { SeminarRepository() },
                    regRepo = remember { SeminarRegistrationRepository() },
                    onBack = { finish() },
                    onRegister = { seminar ->
                        startActivity(Intent(this, SeminarRegistrationActivity::class.java).apply {
                            putExtra(SeminarContract.EXTRA_USER_ID, userId)
                            putExtra(SeminarContract.EXTRA_USER_NAME, userName)
                            putExtra(SeminarContract.EXTRA_PRESELECTED_SEMINAR, seminar.title)
                            putExtra(SeminarContract.EXTRA_PRESELECTED_SEMINAR_ID, seminar.id)
                        })
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Do not call recreate() here to avoid infinite loops. The screen refreshes via LaunchedEffect or explicit pull-to-refresh.
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeminarDetailScreen(
    userId: String,
    userName: String,
    userRole: String,
    seminarId: String,
    seminarRepo: SeminarRepository,
    regRepo: SeminarRegistrationRepository,
    onBack: () -> Unit,
    onRegister: (SeminarItem) -> Unit
) {
    DynamicStatusBar(statusBarColor = KelasinPrimary, useDarkIcons = false)
    val scope = rememberCoroutineScope()
    var seminar by remember { mutableStateOf<SeminarItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var alreadyRegistered by remember { mutableStateOf(false) }
    var checkingReg by remember { mutableStateOf(false) }

    fun load() {
        if (seminarId.isBlank()) { error = "ID Seminar tidak valid"; loading = false; return }
        scope.launch {
            loading = true; error = null
            seminarRepo.getSeminarById(seminarId).fold(
                onSuccess = { item ->
                    seminar = item
                    // Check if user already registered
                    if (userId.isNotBlank()) {
                        checkingReg = true
                        regRepo.hasUserRegistered(userId, seminarId).fold(
                            onSuccess = { alreadyRegistered = it },
                            onFailure = {}
                        )
                        checkingReg = false
                    }
                },
                onFailure = { error = it.message }
            )
            loading = false
        }
    }

    LaunchedEffect(seminarId) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(seminar?.title ?: "Detail Seminar", fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Filled.Refresh, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KelasinPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = KelasinPrimary)
                    Text("Memuat detail seminar...")
                }
                error != null -> ErrorState(error!!, ::load)
                seminar != null -> SeminarDetailContent(
                    seminar = seminar!!,
                    alreadyRegistered = alreadyRegistered,
                    checkingReg = checkingReg,
                    onRegister = { onRegister(seminar!!) }
                )
            }
        }
    }
}

@Composable
private fun SeminarDetailContent(
    seminar: SeminarItem,
    alreadyRegistered: Boolean,
    checkingReg: Boolean,
    onRegister: () -> Unit
) {
    val accentColor = remember(seminar.colorHex, seminar.category) {
        if (!seminar.colorHex.isNullOrBlank()) {
            try { Color(android.graphics.Color.parseColor(seminar.colorHex)) } catch (e: Exception) { categoryColor(seminar.category) }
        } else {
            categoryColor(seminar.category)
        }
    }
    val pct = if (seminar.quota > 0) (seminar.registeredCount.toFloat() / seminar.quota).coerceIn(0f, 1f) else 1f
    val animPct by animateFloatAsState(pct, tween(800, easing = EaseOutCubic), label = "quota-bar")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // Hero banner
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(.7f)))).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(.2f)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text(seminar.category, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Text(seminar.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                if (seminar.speaker.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(.25f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(seminar.speaker, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(.92f))
                    }
                }
            }
        }

        // Quota tracker
        Card(Modifier.fillMaxWidth().padding(16.dp, 14.dp, 16.dp, 0.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Kapasitas & Kuota", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val (bgCol, txtCol) = if (seminar.isFull) Pair(KelasinError.copy(.15f), KelasinError) else Pair(KelasinSuccess.copy(.15f), KelasinSuccess)
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(bgCol).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(if (seminar.isFull) "PENUH" else "TERSEDIA", style = MaterialTheme.typography.labelSmall, color = txtCol, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuotaStatBox("Terdaftar", seminar.registeredCount.toString(), KelasinError, Modifier.weight(1f))
                    QuotaStatBox("Tersisa", seminar.remainingQuota.toString(), KelasinSuccess, Modifier.weight(1f))
                    QuotaStatBox("Kapasitas", seminar.quota.toString(), accentColor, Modifier.weight(1f))
                }
                LinearProgressIndicator(
                    progress = { animPct },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (seminar.isFull) KelasinError else accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("${(pct * 100).toInt()}% kapasitas terisi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Info cards
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailInfoRow(Icons.Filled.CalendarToday, "Waktu Pelaksanaan", seminar.date, accentColor)
            DetailInfoRow(Icons.Filled.LocationOn, "Lokasi", seminar.location, accentColor)
            if (seminar.speaker.isNotBlank()) DetailInfoRow(Icons.Filled.RecordVoiceOver, "Pembicara", seminar.speaker, accentColor)

            if (seminar.description.isNotBlank()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(.5f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tentang Seminar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(seminar.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // CTA Button
            when {
                checkingReg -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = KelasinPrimary, strokeWidth = 3.dp)
                }
                alreadyRegistered -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = KelasinSuccess.copy(.12f))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = KelasinSuccess, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Kamu Sudah Terdaftar!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = KelasinSuccess)
                            Text("Cek detail di tab 'Riwayat Saya'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                seminar.isFull -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = KelasinError.copy(.1f))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Block, null, tint = KelasinError, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Kuota Penuh", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = KelasinError)
                            Text("Seminar ini sudah tidak menerima pendaftaran.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> Button(
                    onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(Icons.Filled.AppRegistration, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Daftar Sekarang — Sisa ${seminar.remainingQuota} Kursi", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
private fun QuotaStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(.1f)).padding(12.dp, 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DetailInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, accentColor: Color) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

package com.kelasin.app.ui.seminar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import com.kelasin.app.data.repository.RegistrationHistoryItem
import com.kelasin.app.data.repository.SeminarItem
import com.kelasin.app.data.repository.SeminarRegistrationRepository
import com.kelasin.app.data.repository.SeminarRepository
import com.kelasin.app.ui.components.InitialsAvatar
import com.kelasin.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/** Shows full details of a single registration (from riwayat saya) */
class RegistrationDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val regId = intent.getStringExtra(SeminarContract.EXTRA_REGISTRATION_ID).orEmpty()
        val seminarId = intent.getStringExtra(SeminarContract.EXTRA_SEMINAR_ID).orEmpty()
        val userId = intent.getStringExtra(SeminarContract.EXTRA_USER_ID).orEmpty()
        val userPic = intent.getStringExtra(SeminarContract.EXTRA_USER_PIC)

        setContent {
            KelasinTheme {
                RegistrationDetailScreen(
                    regId = regId, seminarId = seminarId, userId = userId, userPic = userPic,
                    regRepo = remember { SeminarRegistrationRepository() },
                    seminarRepo = remember { SeminarRepository() },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationDetailScreen(
    regId: String, seminarId: String, userId: String, userPic: String?,
    regRepo: SeminarRegistrationRepository,
    seminarRepo: SeminarRepository,
    onBack: () -> Unit
) {
    DynamicStatusBar(statusBarColor = KelasinPrimary, useDarkIcons = false)
    val scope = rememberCoroutineScope()
    var reg by remember { mutableStateOf<RegistrationHistoryItem?>(null) }
    var seminar by remember { mutableStateOf<SeminarItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true; error = null
            // load all user registrations and find this one
            regRepo.getRegistrationsByUser(userId).fold(
                onSuccess = { list -> reg = list.firstOrNull { it.id == regId } ?: list.firstOrNull { it.seminarId == seminarId } },
                onFailure = { error = it.message }
            )
            if (seminarId.isNotBlank()) {
                seminarRepo.getSeminarById(seminarId).fold(
                    onSuccess = { seminar = it },
                    onFailure = {}
                )
            }
            loading = false
            visible = true
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Pendaftaran", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KelasinPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = KelasinPrimary); Text("Memuat data...")
                }
                error != null -> ErrorState(error!!, ::load)
                reg != null -> AnimatedVisibility(visible, enter = slideInVertically(tween(300)) { 60 } + fadeIn(tween(350))) {
                    RegistrationDetailContent(reg = reg!!, seminar = seminar, userPic = userPic)
                }
                else -> Box(Modifier.align(Alignment.Center)) { Text("Data tidak ditemukan.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun RegistrationDetailContent(reg: RegistrationHistoryItem, seminar: SeminarItem?, userPic: String?) {
    val accentColor = remember(seminar?.colorHex, reg.seminar) {
        if (!seminar?.colorHex.isNullOrBlank()) {
            try { Color(android.graphics.Color.parseColor(seminar!!.colorHex)) } catch (e: Exception) { categoryColor(reg.seminar) }
        } else {
            categoryColor(reg.seminar)
        }
    }
    val dateStr = remember(reg.createdAt) {
        if (reg.createdAt > 0L) SimpleDateFormat("EEEE, dd MMMM yyyy · HH:mm", Locale("id", "ID")).format(Date(reg.createdAt)) else "-"
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Hero
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(.65f)))).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(name = reg.nama, size = 48.dp, profilePicUrl = userPic)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Pendaftaran Berhasil", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(.9f))
                        Text("Terdaftar pada $dateStr", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(.8f))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(reg.seminar, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Personal info card
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text("Data Peserta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    RegInfoRow(Icons.Filled.Person, "Nama", reg.nama, accentColor)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(.5f))
                    RegInfoRow(Icons.Filled.Email, "Email", reg.email, accentColor)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(.5f))
                    RegInfoRow(Icons.Filled.Phone, "No. HP", reg.nomorHp, accentColor)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(.5f))
                    RegInfoRow(Icons.Filled.Wc, "Jenis Kelamin", reg.jenisKelamin, accentColor)
                }
            }

            // Seminar info card (if available)
            if (seminar != null) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("Info Seminar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        RegInfoRow(Icons.Filled.CalendarToday, "Waktu", seminar.date, accentColor)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(.5f))
                        RegInfoRow(Icons.Filled.LocationOn, "Lokasi", seminar.location, accentColor)
                        if (seminar.speaker.isNotBlank()) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(.5f))
                            RegInfoRow(Icons.Filled.RecordVoiceOver, "Pembicara", seminar.speaker, accentColor)
                        }
                    }
                }
            }

            // Custom Answers card (if available)
            if (!reg.customAnswers.isNullOrBlank()) {
                val answers = remember(reg.customAnswers) {
                    val list = mutableListOf<Pair<String, String>>()
                    try {
                        val obj = org.json.JSONObject(reg.customAnswers)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            list.add(key to obj.getString(key))
                        }
                    } catch (e: Exception) {
                        list.add("Raw Data" to reg.customAnswers)
                    }
                    list
                }
                
                if (answers.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text("Jawaban Tambahan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            answers.forEachIndexed { index, (k, v) ->
                                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(.5f))
                                RegInfoRow(Icons.Filled.QuestionAnswer, k, v, accentColor)
                            }
                        }
                    }
                }
            }

            // Status badge
            val displayStatus = if (seminar?.isSelectionEnabled == false || (!reg.isSelectionEnabled && seminar == null)) "Terdaftar" else reg.selectionStatus
            val statusColor = when (displayStatus) {
                "Lolos" -> KelasinSuccess
                "Tidak Lolos" -> KelasinError
                "Terdaftar" -> KelasinPrimary
                else -> Color(0xFFEAB308) // Yellow
            }
            
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = statusColor.copy(.08f))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(statusColor.copy(.15f)), contentAlignment = Alignment.Center) {
                        Icon(if (displayStatus == "Belum diseleksi") Icons.Filled.HourglassEmpty else Icons.Filled.VerifiedUser, null, tint = statusColor, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Status: $displayStatus", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = statusColor)
                        Text("ID: ${reg.id.take(8).uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (displayStatus == "Lolos") {
                            Text("Pendaftaran disetujui", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (displayStatus == "Belum diseleksi") {
                            Text("Menunggu konfirmasi admin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (displayStatus == "Terdaftar") {
                            Text("Berhasil terdaftar ke acara ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            // Post Message / Link info
            if (seminar != null && !seminar.postMessage.isNullOrBlank()) {
                val showMessage = !seminar.isSelectionEnabled || reg.selectionStatus == "Lolos"
                if (showMessage) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KelasinPrimary.copy(.05f)), border = BorderStroke(1.dp, KelasinPrimary.copy(.2f))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Info, null, tint = KelasinPrimary, modifier = Modifier.size(20.dp))
                                Text("Informasi Lanjutan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = KelasinPrimary)
                            }
                            Text(seminar.postMessage, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
private fun RegInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, accentColor: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(accentColor.copy(.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

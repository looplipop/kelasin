package com.kelasin.app.ui.seminar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelasin.app.data.repository.RegistrationHistoryItem
import com.kelasin.app.data.repository.SeminarItem
import com.kelasin.app.data.repository.SeminarRegistrationRepository
import com.kelasin.app.data.repository.SeminarRepository
import com.kelasin.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.kelasin.app.ui.components.InitialsAvatar

class SeminarHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra(SeminarContract.EXTRA_USER_ID).orEmpty()
        val userName = intent.getStringExtra(SeminarContract.EXTRA_USER_NAME).orEmpty()
        val userRole = intent.getStringExtra(SeminarContract.EXTRA_USER_ROLE).orEmpty()
        val userPic = intent.getStringExtra(SeminarContract.EXTRA_USER_PIC)

        setContent {
            KelasinTheme {
                SeminarHomeScreen(
                    userId = userId,
                    userName = userName,
                    userRole = userRole,
                    userPic = userPic,
                    seminarRepo = remember { SeminarRepository() },
                    regRepo = remember { SeminarRegistrationRepository() },
                    onBack = { finish() },
                    onOpenAdmin = {
                        startActivity(Intent(this, AdminSeminarActivity::class.java))
                    },
                    onOpenRegistration = { seminar ->
                        startActivity(Intent(this, SeminarRegistrationActivity::class.java).apply {
                            putExtra(SeminarContract.EXTRA_USER_ID, userId)
                            putExtra(SeminarContract.EXTRA_USER_NAME, userName)
                            putExtra(SeminarContract.EXTRA_USER_PIC, userPic)
                            putExtra(SeminarContract.EXTRA_PRESELECTED_SEMINAR, seminar.title)
                            putExtra(SeminarContract.EXTRA_PRESELECTED_SEMINAR_ID, seminar.id)
                        })
                    },
                    onOpenDetail = { seminar ->
                        startActivity(Intent(this, SeminarDetailActivity::class.java).apply {
                            putExtra(SeminarContract.EXTRA_USER_ID, userId)
                            putExtra(SeminarContract.EXTRA_USER_NAME, userName)
                            putExtra(SeminarContract.EXTRA_USER_ROLE, userRole)
                            putExtra(SeminarContract.EXTRA_SEMINAR_ID, seminar.id)
                        })
                    },
                    onOpenRegistrationDetail = { reg ->
                        startActivity(Intent(this, RegistrationDetailActivity::class.java).apply {
                            putExtra(SeminarContract.EXTRA_REGISTRATION_ID, reg.id)
                            putExtra(SeminarContract.EXTRA_SEMINAR_ID, reg.seminarId)
                            putExtra(SeminarContract.EXTRA_USER_ID, userId)
                            putExtra(SeminarContract.EXTRA_USER_PIC, userPic)
                        })
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeminarHomeScreen(
    userId: String,
    userName: String,
    userRole: String,
    userPic: String?,
    seminarRepo: SeminarRepository,
    regRepo: SeminarRegistrationRepository,
    onBack: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenRegistration: (SeminarItem) -> Unit,
    onOpenDetail: (SeminarItem) -> Unit,
    onOpenRegistrationDetail: (RegistrationHistoryItem) -> Unit
) {
    DynamicStatusBar(statusBarColor = KelasinPrimary, useDarkIcons = false)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    var seminars by remember { mutableStateOf<List<SeminarItem>>(emptyList()) }
    var seminarsLoading by remember { mutableStateOf(false) }
    var seminarsError by remember { mutableStateOf<String?>(null) }

    var riwayatList by remember { mutableStateOf<List<RegistrationHistoryItem>>(emptyList()) }
    var riwayatLoading by remember { mutableStateOf(false) }
    var riwayatError by remember { mutableStateOf<String?>(null) }

    var refreshKey by remember { mutableIntStateOf(0) }

    fun loadSeminars() {
        scope.launch {
            seminarsLoading = true; seminarsError = null
            seminarRepo.getSeminars().fold(
                onSuccess = { seminars = it },
                onFailure = { seminarsError = it.message }
            )
            seminarsLoading = false
        }
    }

    fun loadRiwayat() {
        if (userId.isBlank()) { riwayatError = "User ID tidak tersedia"; return }
        scope.launch {
            riwayatLoading = true; riwayatError = null
            regRepo.getRegistrationsByUser(userId).fold(
                onSuccess = { riwayatList = it },
                onFailure = { riwayatError = it.message }
            )
            riwayatLoading = false
        }
    }

    LaunchedEffect(refreshKey) { loadSeminars() }
    LaunchedEffect(selectedTab, refreshKey) { if (selectedTab == 1) loadRiwayat() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seminar Registration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) { Icon(Icons.Filled.Refresh, null) }
                    if (userRole == "ATMIN") {
                        IconButton(onClick = onOpenAdmin) { Icon(Icons.Filled.ManageAccounts, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KelasinPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Greeting banner
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(KelasinPrimary, KelasinSecondary)))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(name = userName, size = 42.dp, profilePicUrl = userPic)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        if (userName.isNotBlank())
                            Text("Halo, $userName 👋", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${seminars.size} seminar tersedia · ${seminars.count { !it.isFull }} masih buka",
                            color = Color.White.copy(.88f), style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = KelasinPrimary,
                indicator = { pos ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[selectedTab]), color = KelasinPrimary)
                }
            ) {
                listOf("Seminar Tersedia" to Icons.Filled.School, "Riwayat Saya" to Icons.Filled.History).forEachIndexed { i, (label, icon) ->
                    Tab(
                        selected = selectedTab == i, onClick = { selectedTab = i },
                        text = { Text(label, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                        icon = { Icon(icon, null, Modifier.size(15.dp)) },
                        selectedContentColor = KelasinPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedContent(targetState = selectedTab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "tab") { tab ->
                when (tab) {
                    0 -> SeminarListTabContent(
                        seminars = seminars, loading = seminarsLoading, error = seminarsError,
                        onRefresh = { refreshKey++ }, onClickSeminar = onOpenDetail, onRegister = onOpenRegistration
                    )
                    1 -> RiwayatTabContent(
                        list = riwayatList, loading = riwayatLoading, error = riwayatError,
                        onRefresh = { loadRiwayat() },
                        onClickItem = onOpenRegistrationDetail,
                        onRegisterNew = { selectedTab = 0 }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeminarListTabContent(
    seminars: List<SeminarItem>, loading: Boolean, error: String?,
    onRefresh: () -> Unit, onClickSeminar: (SeminarItem) -> Unit, onRegister: (SeminarItem) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = KelasinPrimary)
                Text("Memuat seminar...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error != null -> ErrorState(error, onRefresh)
            seminars.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.School, null, tint = KelasinPrimary.copy(.4f), modifier = Modifier.size(52.dp))
                Text("Belum Ada Seminar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text("Admin belum menambahkan seminar.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(seminars) { index, s ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 60L); visible = true }
                    AnimatedVisibility(visible, enter = slideInVertically(spring(.75f)) { 50 } + fadeIn(tween(280))) {
                        SeminarCard(s, onClick = { onClickSeminar(s) }, onRegister = { onRegister(s) })
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun SeminarCard(s: SeminarItem, onClick: () -> Unit, onRegister: () -> Unit) {
    val accentColor = remember(s.colorHex, s.category) {
        if (!s.colorHex.isNullOrBlank()) {
            try { Color(android.graphics.Color.parseColor(s.colorHex)) } catch (e: Exception) { categoryColor(s.category) }
        } else {
            categoryColor(s.category)
        }
    }
    val isFull = s.isFull
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(.7f)))).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(s.category, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                    QuotaBadge(s.remainingQuota, s.quota, isFull)
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(s.description.take(90) + if (s.description.length > 90) "..." else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(Icons.Filled.CalendarToday, s.date.take(16), Modifier.weight(1f))
                    MetaChip(Icons.Filled.LocationOn, s.location.substringBefore(",").take(20), Modifier.weight(1f))
                }
                if (s.speaker.isNotBlank()) Text("🎤 ${s.speaker}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!isFull) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Daftar Sekarang", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tap kartu untuk detail penuh", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaBadge(remaining: Int, quota: Int, isFull: Boolean) {
    val bg = if (isFull) KelasinError.copy(.18f) else KelasinSuccess.copy(.18f)
    val textColor = if (isFull) Color.White else Color.White
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (isFull) KelasinError.copy(.6f) else KelasinSuccess.copy(.4f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(if (isFull) Icons.Filled.Block else Icons.Filled.People, null, tint = textColor, modifier = Modifier.size(10.dp))
            Text(if (isFull) "PENUH" else "Sisa $remaining/$quota", color = textColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RiwayatTabContent(
    list: List<RegistrationHistoryItem>, loading: Boolean, error: String?,
    onRefresh: () -> Unit, onClickItem: (RegistrationHistoryItem) -> Unit, onRegisterNew: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = KelasinPrimary)
                Text("Memuat riwayat...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error != null -> ErrorState(error, onRefresh)
            list.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(KelasinPrimary.copy(.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.History, null, tint = KelasinPrimary, modifier = Modifier.size(38.dp))
                }
                Text("Belum Ada Pendaftaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Kamu belum pernah mendaftar seminar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = onRegisterNew, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary)) {
                    Icon(Icons.Filled.AppRegistration, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Lihat Seminar")
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${list.size} pendaftaran", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Refresh, null, tint = KelasinPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                itemsIndexed(list) { index, item ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 50L); visible = true }
                    AnimatedVisibility(visible, enter = slideInVertically(spring(.8f)) { 30 } + fadeIn(tween(260))) {
                        RiwayatCard(item = item, onClick = { onClickItem(item) })
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun RiwayatCard(item: RegistrationHistoryItem, onClick: () -> Unit) {
    val accentColor = remember(item.colorHex, item.seminar) {
        if (!item.colorHex.isNullOrBlank()) {
            try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (e: Exception) { categoryColor(item.seminar) }
        } else {
            categoryColor(item.seminar)
        }
    }
    val dateStr = remember(item.createdAt) {
        if (item.createdAt > 0L) SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(item.createdAt)) else "-"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(64.dp).clip(RoundedCornerShape(4.dp)).background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(.6f)))))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    
                    val displayStatus = if (!item.isSelectionEnabled) "Terdaftar" else item.selectionStatus
                    val statusColor = when (displayStatus) {
                        "Lolos" -> KelasinSuccess
                        "Tidak Lolos" -> KelasinError
                        "Terdaftar" -> KelasinPrimary
                        else -> Color(0xFFEAB308) // Yellow for Belum diseleksi
                    }
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(statusColor.copy(.12f)).border(1.dp, statusColor.copy(.4f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(if (displayStatus == "Belum diseleksi") Icons.Filled.HourglassEmpty else Icons.Filled.CheckCircle, null, tint = statusColor, modifier = Modifier.size(10.dp))
                            Text(displayStatus, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
                Text(item.seminar, style = MaterialTheme.typography.bodySmall, color = accentColor, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.email} · ${item.jenisKelamin}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📅 $dateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.7f))
                    Spacer(Modifier.weight(1f))
                    Text("Tap untuk detail →", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
    }
}

@Composable
internal fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Error, null, tint = KelasinError, modifier = Modifier.size(40.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary)) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Coba Lagi")
            }
        }
    }
}

internal fun categoryColor(category: String): Color = when {
    category.contains("AI", true) || category.contains("Tech", true) || category.contains("Teknologi", true) -> Color(0xFF6750A4)
    category.contains("UI", true) || category.contains("UX", true) || category.contains("Desain", true) -> Color(0xFF0077B6)
    category.contains("Cyber", true) || category.contains("Keamanan", true) -> Color(0xFFD00000)
    category.contains("Data", true) -> Color(0xFF2A9D8F)
    category.contains("Bisnis", true) || category.contains("Startup", true) -> Color(0xFFF4A261)
    else -> Color(0xFF546E7A)
}

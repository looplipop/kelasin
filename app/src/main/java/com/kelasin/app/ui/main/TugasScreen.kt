package com.kelasin.app.ui.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kelasin.app.data.entity.*
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.data.repository.TugasRepository
import com.kelasin.app.ui.theme.*
import com.kelasin.app.ui.viewer.FileViewerActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasScreen(
    userId: String,
    userRole: String,
    tugasRepo: TugasRepository,
    mkRepo: MataKuliahRepository,
    navController: NavController?
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val darkTheme = LocalThemeMode.current
    val tugasList by tugasRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    val mkList by mkRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<TugasEntity?>(null) }
    var selectedMk by remember { mutableStateOf<MataKuliahEntity?>(null) }
    var filterStatus by remember { mutableStateOf<StatusTugas?>(null) }

    val filtered = tugasList
        .let { list -> if (selectedMk != null) list.filter { it.mataKuliahId == selectedMk!!.id } else list }
        .let { list -> if (filterStatus != null) list.filter { it.status == filterStatus } else list }

    BackHandler(enabled = selectedMk != null) {
        selectedMk = null
    }

    Scaffold(
        topBar = {
            val topBarBg = if (darkTheme) KelasinDarkBannerBlue else KelasinPrimary
            Box(
                modifier = Modifier.fillMaxWidth().background(topBarBg)
            ) {
                Column {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    DynamicStatusBar(
                        statusBarColor = topBarBg,
                        useDarkIcons = !darkTheme
                    )
                    TopAppBar(
                        title = {
                            if (selectedMk != null) {
                                Text(selectedMk!!.nama, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            } else {
                                Text("Tugas", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        navigationIcon = {
                    if (selectedMk != null) {
                        IconButton(onClick = { selectedMk = null }) { Icon(Icons.Filled.ArrowBack, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedMk != null && userRole == "ATMIN") {
                FloatingActionButton(onClick = { editItem = null; showDialog = true }, containerColor = KelasinPrimary, shape = CircleShape) {
                    Icon(Icons.Filled.Add, null, tint = Color.White)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(targetState = selectedMk, transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            }, label = "tugas_tier") { currentMk ->
                if (currentMk == null) {
                    // ── Tier 1: Matkul picker ──
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                "Pilih Mata Kuliah",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        items(mkList, key = { it.id }) { mk ->
                            val mkColor = runCatching { Color(android.graphics.Color.parseColor(mk.warna)) }.getOrElse { KelasinPrimary }
                            val mkTugas = tugasList.filter { it.mataKuliahId == mk.id }
                            Card(
                                onClick = { selectedMk = mk },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(mkColor), contentAlignment = Alignment.Center) {
                                        Text(shortHari(mk.hari), color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            mk.nama,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text("${mkTugas.size} tugas", color = KelasinSubtext, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Filled.ChevronRight, null, tint = KelasinSubtext)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                } else {
                    // ── Tier 2: Tugas list for selected Matkul ──
                    Column(Modifier.fillMaxSize()) {
                        // Status filter chips
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("Semua") })
                            }
                            items(StatusTugas.values().toList()) { s ->
                                FilterChip(selected = filterStatus == s, onClick = { filterStatus = s }, label = { Text(s.name) })
                            }
                        }
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.AssignmentTurnedIn, null, modifier = Modifier.size(64.dp), tint = KelasinPrimaryLight)
                                    Text("Belum ada tugas di sini", color = KelasinSubtext)
                                }
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(filtered, key = { _, it -> it.id }) { index, tugas ->
                                    BouncyListItem(index = index) {
                                        TugasCard(
                                            tugas = tugas, userRole = userRole, mkNama = currentMk.nama,
                                            mkColor = runCatching { Color(android.graphics.Color.parseColor(currentMk.warna)) }.getOrElse { KelasinPrimary },
                                            onClick = {
                                                context.startActivity(Intent(context, com.kelasin.app.ui.detail.DetailActivity::class.java).apply {
                                                    putExtra("TYPE", "TUGAS"); putExtra("ID", tugas.id)
                                                })
                                            },
                                            onEdit = { editItem = tugas; showDialog = true },
                                            onDelete = { 
                                                scope.launch { 
                                                    tugasRepo.delete(tugas) 
                                                    com.kelasin.app.worker.ReminderWorker.cancelReminder(context, tugas.id)
                                                } 
                                            },
                                            onToggleDone = {
                                                scope.launch {
                                                    val next = if (tugas.status == StatusTugas.SELESAI) StatusTugas.BELUM else StatusTugas.SELESAI
                                                    tugasRepo.update(tugas.copy(status = next))
                                                    if (next == StatusTugas.SELESAI) {
                                                        com.kelasin.app.worker.ReminderWorker.cancelReminder(context, tugas.id)
                                                    } else {
                                                        com.kelasin.app.worker.ReminderWorker.scheduleReminder(context, tugas.id, tugas.judul, currentMk.nama, tugas.deadline)
                                                    }
                                                }
                                            },
                                            onOpenFile = {
                                                if (tugas.fileUri.isNotBlank()) {
                                                    context.startActivity(Intent(context, FileViewerActivity::class.java).apply {
                                                        putExtra("FILE_URI", tugas.fileUri); putExtra("TITLE", tugas.judul)
                                                    })
                                                }
                                            }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedMk != null) {
        val currentMkForDialog = selectedMk!!
        TugasDialog(
            existing = editItem, userId = userId, mkList = mkList, preselectedMk = currentMkForDialog,
            onDismiss = { showDialog = false },
            onSave = { t ->
                scope.launch { 
                    val newId = if (t.id == 0L) tugasRepo.insert(t) else { tugasRepo.update(t); t.id }
                    if (t.status != StatusTugas.SELESAI) {
                        com.kelasin.app.worker.ReminderWorker.scheduleReminder(context, newId, t.judul, currentMkForDialog.nama, t.deadline)
                    } else {
                        com.kelasin.app.worker.ReminderWorker.cancelReminder(context, newId)
                    }
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun TugasCard(
    tugas: TugasEntity,
    userRole: String,
    mkNama: String,
    mkColor: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: () -> Unit,
    onOpenFile: () -> Unit
) {
    val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id"))
    val prioColor = when (tugas.prioritas) { Prioritas.TINGGI -> PriorityTinggi; Prioritas.SEDANG -> PrioritasSedang; Prioritas.RENDAH -> PrioritasRendah }
    val isDone = tugas.status == StatusTugas.SELESAI

    // Deadline countdown
    val now = System.currentTimeMillis()
    val diffMs = tugas.deadline - now
    val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
    val deadlineLabel = when {
        isDone -> null
        diffDays < 0 -> "Terlambat ${-diffDays} hari"
        diffDays == 0 -> "Hari ini!"
        diffDays == 1 -> "Besok!"
        else -> "$diffDays hari lagi"
    }
    val deadlineLabelColor = when {
        isDone -> null
        diffDays < 0 -> KelasinError
        diffDays == 0 -> Color(0xFFFF6F00)
        diffDays <= 3 -> Color(0xFFFF8F00)
        else -> Color(0xFF2E7D32)
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            // Colored accent bar on the left
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        if (isDone) KelasinSubtext.copy(alpha = 0.3f) else prioColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(Modifier.padding(12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = isDone, onCheckedChange = { onToggleDone() }, colors = CheckboxDefaults.colors(checkedColor = KelasinPrimary))
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            tugas.judul,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDone) KelasinSubtext else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                        Text("Pertemuan ${tugas.pertemuanKe}", color = KelasinSubtext, style = MaterialTheme.typography.bodySmall)
                    }
                    // Priority badge
                    Surface(shape = RoundedCornerShape(8.dp), color = prioColor.copy(alpha = if (isDone) 0.06f else 0.15f)) {
                        Text(
                            tugas.prioritas.name,
                            color = if (isDone) KelasinSubtext else prioColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Deadline row with countdown
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(13.dp), tint = KelasinSubtext)
                    Text(fmt.format(Date(tugas.deadline)), color = KelasinSubtext, style = MaterialTheme.typography.bodySmall)
                    if (deadlineLabel != null && deadlineLabelColor != null) {
                        Surface(shape = RoundedCornerShape(6.dp), color = deadlineLabelColor.copy(alpha = 0.12f)) {
                            Text(
                                deadlineLabel,
                                color = deadlineLabelColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Attachment row
                if (tugas.fileUri.isNotBlank() || tugas.linkUrl.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = KelasinDivider)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (tugas.fileUri.isNotBlank()) {
                            OutlinedButton(onClick = onOpenFile, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lampiran", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (tugas.linkUrl.isNotBlank()) {
                            val ctx = LocalContext.current
                            OutlinedButton(onClick = {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(tugas.linkUrl)))
                            }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Filled.Link, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Link", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (userRole == "ATMIN") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp), tint = KelasinSubtext)
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", style = MaterialTheme.typography.labelSmall, color = KelasinSubtext)
                        }
                        TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp), tint = KelasinError)
                            Spacer(Modifier.width(4.dp))
                            Text("Hapus", style = MaterialTheme.typography.labelSmall, color = KelasinError)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasDialog(
    existing: TugasEntity?,
    userId: String,
    mkList: List<MataKuliahEntity>,
    preselectedMk: MataKuliahEntity?,
    onDismiss: () -> Unit,
    onSave: (TugasEntity) -> Unit
) {
    var judul by remember(existing) { mutableStateOf(existing?.judul ?: "") }
    var deskripsi by remember(existing) { mutableStateOf(existing?.deskripsi ?: "") }
    var selectedMk by remember(existing) { mutableStateOf(mkList.firstOrNull { it.id == existing?.mataKuliahId } ?: preselectedMk ?: mkList.firstOrNull()) }
    var prioritas by remember(existing) { mutableStateOf(existing?.prioritas ?: Prioritas.SEDANG) }
    var deadline by remember(existing) { mutableStateOf(existing?.deadline ?: System.currentTimeMillis()) }
    var pertemuanText by remember(existing) { mutableStateOf(existing?.pertemuanKe?.toString() ?: "1") }
    var fileUri by remember(existing) { mutableStateOf(existing?.fileUri ?: "") }
    var linkUrl by remember(existing) { mutableStateOf(existing?.linkUrl ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDateMillis by remember { mutableStateOf(0L) }
    var expandedPertemuan by remember { mutableStateOf(false) }

    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id"))
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            fileUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Tugas" else "Edit Tugas", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(judul, { judul = it }, label = { Text("Judul Tugas") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(deskripsi, { deskripsi = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth(), maxLines = 3) }
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedPertemuan,
                        onExpandedChange = { expandedPertemuan = !expandedPertemuan }
                    ) {
                        OutlinedTextField(
                            value = pertemuanText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pertemuan Ke") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPertemuan) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPertemuan,
                            onDismissRequest = { expandedPertemuan = false }
                        ) {
                            (1..16).forEach { num ->
                                DropdownMenuItem(
                                    text = { Text("Pertemuan $num") },
                                    onClick = {
                                        pertemuanText = num.toString()
                                        expandedPertemuan = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Deadline picker
                item {
                    OutlinedTextField(
                        value = fmt.format(Date(deadline)),
                        onValueChange = {},
                        label = { Text("Deadline") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.CalendarMonth, null, tint = KelasinPrimary)
                            }
                        }
                    )
                }
                // Link + File attachment
                item {
                    Text("Lampiran & Link", style = MaterialTheme.typography.labelMedium, color = KelasinSubtext)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(linkUrl, { linkUrl = it }, label = { Text("Link URL (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Link, null, tint = KelasinSubtext) })
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { launcher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (fileUri.isBlank()) "Pilih File Lampiran" else "Ganti File Lampiran")
                    }
                    if (fileUri.isNotBlank()) {
                        Text("File terpilih", style = MaterialTheme.typography.labelSmall, color = StatusHadir, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                // Prioritas chips
                item {
                    Text("Prioritas", style = MaterialTheme.typography.labelMedium, color = KelasinSubtext)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Prioritas.values().forEach { p ->
                            FilterChip(selected = prioritas == p, onClick = { prioritas = p }, label = { Text(p.name) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary), onClick = {
                if (selectedMk != null && judul.isNotBlank()) {
                    onSave(TugasEntity(
                        id = existing?.id ?: 0L,
                        mataKuliahId = selectedMk!!.id,
                        judul = judul, deskripsi = deskripsi, deadline = deadline,
                        prioritas = prioritas, pertemuanKe = pertemuanText.toIntOrNull() ?: 1,
                        fileUri = fileUri, linkUrl = linkUrl, userId = userId
                    ))
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )

    if (showDatePicker) {
        KelasinDatePicker(initialMillis = deadline, onDismiss = { showDatePicker = false }) { millis ->
            tempDateMillis = millis
            showDatePicker = false
            showTimePicker = true
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = deadline }
        val initStr = "%02d:%02d".format(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        KelasinTimePicker(initial = initStr, onDismiss = { showTimePicker = false }) { h, m ->
            val cal = Calendar.getInstance().apply { timeInMillis = tempDateMillis }
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
            deadline = cal.timeInMillis
            showTimePicker = false
        }
    }
}

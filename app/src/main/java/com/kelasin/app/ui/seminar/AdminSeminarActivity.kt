package com.kelasin.app.ui.seminar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

class AdminSeminarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KelasinTheme {
                AdminSeminarScreen(
                    seminarRepo = remember { SeminarRepository() },
                    regRepo = remember { SeminarRegistrationRepository() },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSeminarScreen(
    seminarRepo: SeminarRepository,
    regRepo: SeminarRegistrationRepository,
    onBack: () -> Unit
) {
    DynamicStatusBar(statusBarColor = KelasinPrimary, useDarkIcons = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var seminars by remember { mutableStateOf<List<SeminarItem>>(emptyList()) }
    var registrations by remember { mutableStateOf<List<RegistrationHistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Dialog state
    var showFormDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    
    var formTitle by remember { mutableStateOf("") }
    var formDesc by remember { mutableStateOf("") }
    var formDate by remember { mutableStateOf("") }
    var formLocation by remember { mutableStateOf("") }
    var formSpeaker by remember { mutableStateOf("") }
    var formQuota by remember { mutableStateOf("100") }
    var formCategory by remember { mutableStateOf("Teknologi") }
    var formColorHex by remember { mutableStateOf("") }
    var formPostMessage by remember { mutableStateOf("") }
    var formIsSelectionEnabled by remember { mutableStateOf(false) }
    var formCustomTextQ by remember { mutableStateOf("") }
    var formCustomOptionsLabel by remember { mutableStateOf("") }
    var formCustomOptions by remember { mutableStateOf("") }
    var formIsMultipleChoice by remember { mutableStateOf(false) }
    var formLoading by remember { mutableStateOf(false) }
    
    val colorPalette = listOf(
        "#38BDF8", // KelasinPrimary
        "#1E3A8A", // KelasinBlue
        "#8B5CF6", // KelasinPurple
        "#EC4899", // KelasinPink
        "#EF4444", // KelasinError
        "#F59E0B", // Orange
        "#22C55E", // KelasinSuccess
        "#10B981", // Emerald
        "#64748B"  // Slate
    )
    
    var showDatePicker by remember { mutableStateOf(false) }

    // Delete states
    var deleteTarget by remember { mutableStateOf<SeminarItem?>(null) }
    var deleteRegTarget by remember { mutableStateOf<RegistrationHistoryItem?>(null) }
    var filterBySeminar by remember { mutableStateOf<String?>(null) }

    val snackbar = remember { SnackbarHostState() }

    fun loadSeminars() {
        scope.launch {
            loading = true; error = null
            seminarRepo.getSeminars().fold(
                onSuccess = { seminars = it },
                onFailure = { error = it.message }
            )
            loading = false
        }
    }

    fun loadRegistrations() {
        scope.launch {
            loading = true; error = null
            regRepo.getAllRegistrations().fold(
                onSuccess = { registrations = it },
                onFailure = { error = it.message }
            )
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadSeminars() }
    LaunchedEffect(selectedTab) { if (selectedTab == 1) loadRegistrations() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Seminar", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = {
                            isEditing = false
                            formTitle = ""; formDesc = ""; formDate = ""; formLocation = ""; formSpeaker = ""; formQuota = "100"; formCategory = "Teknologi"
                            formColorHex = ""; formPostMessage = ""; formIsSelectionEnabled = false
                            formCustomTextQ = ""; formCustomOptionsLabel = ""; formCustomOptions = ""; formIsMultipleChoice = false
                            showFormDialog = true
                        }) { Icon(Icons.Filled.Add, "Tambah") }
                    }
                    IconButton(onClick = { if (selectedTab == 0) loadSeminars() else loadRegistrations() }) { Icon(Icons.Filled.Refresh, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KelasinPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[selectedTab]), color = KelasinPrimary) }) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Seminar") }, icon = { Icon(Icons.Filled.School, null, Modifier.size(16.dp)) }, selectedContentColor = KelasinPrimary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; filterBySeminar = null }, text = { Text("Peserta") }, icon = { Icon(Icons.Filled.People, null, Modifier.size(16.dp)) }, selectedContentColor = KelasinPrimary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KelasinPrimary) }
                error != null -> ErrorState(error!!, { if (selectedTab == 0) loadSeminars() else loadRegistrations() })
                selectedTab == 0 -> AdminSeminarList(
                    seminars = seminars,
                    onEdit = { s ->
                        isEditing = true; editingId = s.id
                        formTitle = s.title; formDesc = s.description; formDate = s.date; formLocation = s.location
                        formSpeaker = s.speaker; formQuota = s.quota.toString(); formCategory = s.category
                        formColorHex = s.colorHex ?: ""
                        formPostMessage = s.postMessage ?: ""
                        formIsSelectionEnabled = s.isSelectionEnabled
                        // simple parse for editing
                        formCustomTextQ = ""; formCustomOptionsLabel = ""; formCustomOptions = ""; formIsMultipleChoice = false
                        try {
                            if (!s.customQuestions.isNullOrBlank()) {
                                val arr = org.json.JSONArray(s.customQuestions)
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    if (obj.getString("type") == "text") formCustomTextQ = obj.getString("label")
                                    if (obj.getString("type") == "checkbox") {
                                        formCustomOptionsLabel = obj.optString("label", "Pilihan")
                                        formIsMultipleChoice = obj.optBoolean("isMultipleChoice", false)
                                        val opts = obj.getJSONArray("options")
                                        val optList = mutableListOf<String>()
                                        for (j in 0 until opts.length()) optList.add(opts.getString(j))
                                        formCustomOptions = optList.joinToString(",")
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                        showFormDialog = true
                    },
                    onDelete = { deleteTarget = it },
                    onViewPeserta = { s -> filterBySeminar = s.title; selectedTab = 1; loadRegistrations() }
                )
                selectedTab == 1 -> AdminPesertaList(
                    registrations = registrations, filterBySeminar = filterBySeminar, regRepo = regRepo,
                    onClearFilter = { filterBySeminar = null }, onDelete = { deleteRegTarget = it },
                    onRefresh = { loadRegistrations() }
                )
            }
        }
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val cal = Calendar.getInstance()
                        cal.set(year, month, dayOfMonth, hourOfDay, minute)
                        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy | HH:mm", Locale("id", "ID"))
                        formDate = sdf.format(cal.time)
                        showDatePicker = false
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).apply {
                    setOnCancelListener { showDatePicker = false }
                }.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showDatePicker = false }
        }.show()
    }

    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { if (!formLoading) showFormDialog = false },
            title = { Text(if (isEditing) "Edit Seminar" else "Tambah Seminar", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(formTitle, { formTitle = it }, label = { Text("Judul Seminar") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(formDesc, { formDesc = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    
                    Box(Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = formDate,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Tanggal & Waktu") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Filled.CalendarToday, null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // Invisible box to catch clicks
                        Box(Modifier.matchParentSize().background(Color.Transparent).clickable { showDatePicker = true })
                    }
                    
                    OutlinedTextField(formLocation, { formLocation = it }, label = { Text("Lokasi") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(formSpeaker, { formSpeaker = it }, label = { Text("Pembicara") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(formQuota, { formQuota = it.filter { c -> c.isDigit() } }, label = { Text("Kuota Maks") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(formCategory, { formCategory = it }, label = { Text("Kategori") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    
                    Text("Warna Aksen Seminar", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorPalette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { formColorHex = hex }
                                    .border(
                                        width = if (formColorHex == hex) 3.dp else 0.dp,
                                        color = if (formColorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    
                    OutlinedTextField(formPostMessage, { formPostMessage = it }, label = { Text("Pesan/Link Setelah Daftar (Opsional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = formIsSelectionEnabled, onCheckedChange = { formIsSelectionEnabled = it })
                        Text("Aktifkan Sistem Seleksi Peserta", style = MaterialTheme.typography.bodySmall)
                    }

                    Text("Pertanyaan Tambahan (Opsional)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(formCustomTextQ, { formCustomTextQ = it }, label = { Text("Pertanyaan Esai (cth: Alasan mendaftar?)") }, modifier = Modifier.fillMaxWidth())
                    
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    OutlinedTextField(formCustomOptionsLabel, { formCustomOptionsLabel = it }, label = { Text("Pertanyaan Pilihan (cth: Pilih Sesi)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(formCustomOptions, { formCustomOptions = it }, label = { Text("Pilihan (pisahkan dengan koma)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = formIsMultipleChoice, onCheckedChange = { formIsMultipleChoice = it })
                        Text("Boleh pilih lebih dari satu (Ceklis)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (formTitle.isBlank()) return@Button
                    formLoading = true
                    scope.launch {
                        val q = formQuota.toIntOrNull() ?: 100
                        val c = formCategory.trim().ifBlank { "Umum" }
                        val hex = formColorHex.trim().takeIf { it.isNotBlank() }
                        val postMsg = formPostMessage.trim().takeIf { it.isNotBlank() }
                        
                        var customQJson: String? = null
                        if (formCustomTextQ.isNotBlank() || formCustomOptions.isNotBlank()) {
                            val arr = org.json.JSONArray()
                            if (formCustomTextQ.isNotBlank()) {
                                arr.put(org.json.JSONObject().put("type", "text").put("label", formCustomTextQ.trim()))
                            }
                            if (formCustomOptions.isNotBlank()) {
                                val opts = org.json.JSONArray()
                                formCustomOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { opts.put(it) }
                                val qLabel = formCustomOptionsLabel.trim().ifBlank { "Pilih Opsi" }
                                if (opts.length() > 0) arr.put(org.json.JSONObject().put("type", "checkbox").put("label", qLabel).put("options", opts).put("isMultipleChoice", formIsMultipleChoice))
                            }
                            if (arr.length() > 0) customQJson = arr.toString()
                        }

                        val res = if (isEditing && editingId != null) {
                            seminarRepo.updateSeminar(editingId!!, formTitle, formDesc, formDate, formLocation, formSpeaker, q, c, hex, postMsg, formIsSelectionEnabled, customQJson)
                        } else {
                            seminarRepo.insertSeminar(formTitle, formDesc, formDate, formLocation, formSpeaker, q, c, hex, postMsg, formIsSelectionEnabled, customQJson)
                        }
                        res.fold(
                            onSuccess = {
                                formLoading = false; showFormDialog = false; loadSeminars()
                                snackbar.showSnackbar(if (isEditing) "Seminar diperbarui" else "Seminar ditambahkan")
                            },
                            onFailure = { formLoading = false; snackbar.showSnackbar(it.message ?: "Gagal menyimpan") }
                        )
                    }
                }, enabled = formTitle.isNotBlank() && !formLoading, colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary)) {
                    if (formLoading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text("Simpan")
                }
            },
            dismissButton = { TextButton(onClick = { showFormDialog = false }, enabled = !formLoading) { Text("Batal") } }
        )
    }

    deleteTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus Seminar?") }, text = { Text("Seminar \"${s.title}\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        seminarRepo.deleteSeminar(s.id).fold(
                            onSuccess = { loadSeminars(); snackbar.showSnackbar("Dihapus") },
                            onFailure = { snackbar.showSnackbar(it.message ?: "Gagal") }
                        )
                        deleteTarget = null
                    }
                }) { Text("Hapus", color = KelasinError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }

    deleteRegTarget?.let { r ->
        AlertDialog(
            onDismissRequest = { deleteRegTarget = null },
            title = { Text("Hapus Peserta?") }, text = { Text("Peserta \"${r.nama}\" akan dihapus dari seminar.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        regRepo.deleteRegistration(r.id).fold(
                            onSuccess = { loadRegistrations(); snackbar.showSnackbar("Peserta dihapus") },
                            onFailure = { snackbar.showSnackbar(it.message ?: "Gagal") }
                        )
                        deleteRegTarget = null
                    }
                }) { Text("Hapus", color = KelasinError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteRegTarget = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun AdminSeminarList(
    seminars: List<SeminarItem>,
    onEdit: (SeminarItem) -> Unit, onDelete: (SeminarItem) -> Unit, onViewPeserta: (SeminarItem) -> Unit
) {
    if (seminars.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada seminar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(seminars) { _, s ->
            val color = categoryColor(s.category)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color, color.copy(.7f)))).padding(12.dp, 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(s.category, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Terisi: ${s.registeredCount}/${s.quota}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("📅 ${s.date} · 📍 ${s.location}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onViewPeserta(s) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = KelasinPrimary)) {
                                Icon(Icons.Filled.People, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Peserta (${s.registeredCount})", style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { onEdit(s) }, modifier = Modifier.size(36.dp).background(KelasinPrimary.copy(.1f), RoundedCornerShape(8.dp))) {
                                Icon(Icons.Filled.Edit, null, tint = KelasinPrimary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDelete(s) }, modifier = Modifier.size(36.dp).background(KelasinError.copy(.1f), RoundedCornerShape(8.dp))) {
                                Icon(Icons.Filled.Delete, null, tint = KelasinError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun AdminPesertaList(
    registrations: List<RegistrationHistoryItem>, filterBySeminar: String?, regRepo: SeminarRegistrationRepository,
    onClearFilter: () -> Unit, onDelete: (RegistrationHistoryItem) -> Unit, onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var detailTarget by remember { mutableStateOf<RegistrationHistoryItem?>(null) }

    detailTarget?.let { r ->
        AlertDialog(
            onDismissRequest = { detailTarget = null },
            title = { Text("Detail Peserta") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Nama: ${r.nama}", fontWeight = FontWeight.Bold)
                    Text("Email: ${r.email}")
                    Text("No HP: ${r.nomorHp}")
                    Text("Seminar: ${r.seminar}")
                    Text("Status: ${r.selectionStatus}", color = KelasinPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (!r.customAnswers.isNullOrBlank()) {
                        Text("Jawaban Form:", fontWeight = FontWeight.Bold)
                        val answers = remember(r.customAnswers) {
                            val list = mutableListOf<Pair<String, String>>()
                            try {
                                val obj = org.json.JSONObject(r.customAnswers)
                                val keys = obj.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    list.add(key to obj.getString(key))
                                }
                            } catch (e: Exception) {
                                list.add("Raw Data" to r.customAnswers)
                            }
                            list
                        }
                        for ((k, v) in answers) {
                            Text("- $k: $v", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Ubah Status Seleksi:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { scope.launch { regRepo.updateSelectionStatus(r.id, "Lolos"); detailTarget = null; onRefresh() } }, colors = ButtonDefaults.buttonColors(containerColor = KelasinSuccess)) { Text("Lolos") }
                        Button(onClick = { scope.launch { regRepo.updateSelectionStatus(r.id, "Tidak Lolos"); detailTarget = null; onRefresh() } }, colors = ButtonDefaults.buttonColors(containerColor = KelasinError)) { Text("Tidak Lolos") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { detailTarget = null }) { Text("Tutup") } }
        )
    }

    val filtered = if (filterBySeminar != null) registrations.filter { it.seminar == filterBySeminar } else registrations
    Column(Modifier.fillMaxSize()) {
        if (filterBySeminar != null) {
            Row(Modifier.fillMaxWidth().padding(16.dp, 8.dp).clip(RoundedCornerShape(8.dp)).background(KelasinPrimary.copy(.1f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FilterList, null, tint = KelasinPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Filter: $filterBySeminar", style = MaterialTheme.typography.labelSmall, color = KelasinPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onClearFilter, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, null, tint = KelasinPrimary, modifier = Modifier.size(16.dp)) }
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada peserta", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("${filtered.size} Peserta Terdaftar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                itemsIndexed(filtered) { _, r ->
                    val dateStr = remember(r.createdAt) { if (r.createdAt > 0L) SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(r.createdAt)) else "-" }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(KelasinPrimary.copy(.1f)), contentAlignment = Alignment.Center) {
                                Text(r.nama.take(2).uppercase(), color = KelasinPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.nama, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(r.seminar, style = MaterialTheme.typography.labelSmall, color = KelasinPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${r.email} · ${r.nomorHp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Status: ${r.selectionStatus}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if(r.selectionStatus=="Lolos") KelasinSuccess else if(r.selectionStatus=="Tidak Lolos") KelasinError else Color(0xFFEAB308))
                            }
                            IconButton(onClick = { detailTarget = r }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Visibility, null, tint = KelasinPrimary, modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = { onDelete(r) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.DeleteOutline, null, tint = KelasinError, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

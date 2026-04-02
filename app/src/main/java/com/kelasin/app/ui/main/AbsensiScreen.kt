package com.kelasin.app.ui.main

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelasin.app.data.entity.*
import com.kelasin.app.data.repository.AbsensiRepository
import com.kelasin.app.data.repository.MahasiswaRepository
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.ui.theme.*
import com.kelasin.app.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsensiScreen(
    userId: String,
    userRole: String,
    absensiRepo: AbsensiRepository,
    mkRepo: MataKuliahRepository,
    mhsRepo: MahasiswaRepository
) {
    val scope = rememberCoroutineScope()
    val darkTheme = LocalThemeMode.current
    val mkList by mkRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    val mhsList by mhsRepo.getAll().collectAsStateWithLifecycle(emptyList())
    var selectedMk by remember { mutableStateOf<MataKuliahEntity?>(null) }
    var selectedPertemuan by remember { mutableStateOf(1) } // Default ke pertemuan 1
    var expandedPertemuan by remember { mutableStateOf(false) }
    var showMahasiswaManager by remember { mutableStateOf(false) }
    // Track which (mkId, pertemuanKe) combos we've already tried seeding this session
    val seededSessions = remember { mutableSetOf<Pair<Long, Int>>() }
    val healedSessions = remember { mutableSetOf<Pair<Long, Int>>() }

    val context = LocalContext.current
    
    var localStatusOverrides by remember { mutableStateOf<Map<Long, StatusAbsensi>>(emptyMap()) }
    LaunchedEffect(selectedMk, selectedPertemuan) { localStatusOverrides = emptyMap() }

    // Semua absensi untuk Mk yang dipilih
    val allAbsensiForMk by (selectedMk?.let { absensiRepo.getByMataKuliah(userId, it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())

    // Filter absensi untuk pertemuan yang dipilih
    val records = allAbsensiForMk
        .filter { it.pertemuanKe == selectedPertemuan }
        .distinctBy { it.mahasiswaId }
    val isAllHadir = records.isNotEmpty() && records.all { it.status == StatusAbsensi.HADIR }

    // Healing mapping lama: jika mahasiswaId pada absensi sudah tidak cocok dengan data mahasiswa lokal,
    // map ulang berdasarkan urutan agar nama tidak tampil "-" semua.
    LaunchedEffect(selectedMk?.id, selectedPertemuan, records, mhsList) {
        val mk = selectedMk ?: return@LaunchedEffect
        if (records.isEmpty() || mhsList.isEmpty()) return@LaunchedEffect
        val key = Pair(mk.id, selectedPertemuan)
        if (healedSessions.contains(key)) return@LaunchedEffect

        val knownIds = mhsList.map { it.id }.toSet()
        val unknown = records.filter { it.mahasiswaId !in knownIds }
        if (unknown.isEmpty()) return@LaunchedEffect

        healedSessions.add(key)
        val knownMahasiswaIds = records
            .filter { it.mahasiswaId in knownIds }
            .map { it.mahasiswaId }
            .toSet()
        val availableMahasiswa = mhsList
            .filter { it.id !in knownMahasiswaIds }
            .sortedWith(compareBy({ it.nomorAbsen }, { it.displayNama }))
        unknown.sortedBy { it.id }
            .zip(availableMahasiswa)
            .forEach { (record, mhs) ->
                if (record.mahasiswaId != mhs.id) {
                    absensiRepo.update(record.copy(mahasiswaId = mhs.id))
                }
            }
    }

    BackHandler(enabled = showMahasiswaManager) {
        showMahasiswaManager = false
    }

    BackHandler(enabled = selectedMk != null && !showMahasiswaManager) {
        selectedMk = null
        selectedPertemuan = 1
    }

    if (showMahasiswaManager) {
        MahasiswaManagerUI(mhsRepo = mhsRepo, onClose = { showMahasiswaManager = false })
        return
    }

    // Seeding pertemuan baru — only once per (mkId, pertemuan) per session
    LaunchedEffect(selectedMk?.id, selectedPertemuan, mhsList) {
        val mk = selectedMk ?: return@LaunchedEffect
        if (mhsList.isEmpty()) return@LaunchedEffect
        val key = Pair(mk.id, selectedPertemuan)
        if (!seededSessions.add(key)) return@LaunchedEffect

        absensiRepo.deleteDuplicateRowsByMahasiswa(userId, mk.id, selectedPertemuan)
        val existing = absensiRepo.getByPertemuan(userId, mk.id, selectedPertemuan)
        val existingMahasiswaIds = existing.map { it.mahasiswaId }.toSet()

        val newRecords = mhsList
            .filter { it.id !in existingMahasiswaIds }
            .map { m ->
                AbsensiEntity(
                    mataKuliahId = mk.id,
                    pertemuanKe = selectedPertemuan,
                    mahasiswaId = m.id,
                    status = StatusAbsensi.BELUM_DIPILIH,
                    tanggal = System.currentTimeMillis(),
                    userId = userId
                )
            }
        if (newRecords.isNotEmpty()) {
            absensiRepo.insertAll(newRecords)
        }
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
                                Text("Absensi", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        navigationIcon = {
                            if (selectedMk != null) {
                                IconButton(onClick = { selectedMk = null; selectedPertemuan = 1 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                            }
                        },
                        actions = {
                            if (selectedMk == null && userRole == "ATMIN") {
                                IconButton(onClick = { showMahasiswaManager = true }) {
                                    Icon(Icons.Filled.ManageAccounts, "Kelola Mahasiswa", tint = Color.White)
                                }
                            } else if (records.isNotEmpty()) {
                                // CSV Export
                                IconButton(onClick = { exportCsv(context, selectedMk!!, selectedPertemuan, mhsList, records) }) {
                                    Icon(
                                        Icons.Filled.Description,
                                        "Export CSV",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                // PDF Export
                                IconButton(onClick = {
                                    val pdfFile = PdfGenerator.generateAbsensiPdf(context, selectedMk!!, selectedPertemuan, mhsList, records)
                                    if (pdfFile != null) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Buka PDF"))
                                    } else {
                                        Toast.makeText(context, "Gagal buat PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        Icons.Filled.PictureAsPdf,
                                        "Export PDF",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(targetState = selectedMk, transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            }, label = "absensi_tier") { currentMk ->
                if (currentMk == null) {
                    // == TIER 1: Pilih Mata Kuliah ==
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
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
                            Card(onClick = { selectedMk = mk }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
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
                                    }
                                    Icon(Icons.Filled.ChevronRight, null, tint = KelasinSubtext)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                } else {
                    // == TIER 2: Table View ==
                    val tableBg = MaterialTheme.colorScheme.background
                    val tableBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    Column(Modifier.fillMaxSize()) {
                        // Header Filter (Pertemuan Dropdown & Hadir Semua Checkbox)
                        Surface(color = tableBg, shadowElevation = 0.dp, tonalElevation = 0.dp) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Dropdown 1-16
                                ExposedDropdownMenuBox(
                                    expanded = expandedPertemuan,
                                    onExpandedChange = { expandedPertemuan = !expandedPertemuan },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = "Pertemuan $selectedPertemuan",
                                        onValueChange = {},
                                        readOnly = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPertemuan) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).heightIn(min = 48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = KelasinDivider,
                                            focusedBorderColor = KelasinPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedPertemuan,
                                        onDismissRequest = { expandedPertemuan = false }
                                    ) {
                                        (1..16).forEach { num ->
                                            DropdownMenuItem(
                                                text = { Text("Pertemuan $num") },
                                                onClick = { selectedPertemuan = num; expandedPertemuan = false }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                // Tandai Hadir Semua
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                    if (records.isNotEmpty() && userRole == "ATMIN") {
                                        scope.launch {
                                            val targetStatus = if (isAllHadir) StatusAbsensi.BELUM_DIPILIH else StatusAbsensi.HADIR
                                            records.forEach { r ->
                                                if (r.status != targetStatus) {
                                                    absensiRepo.update(r.copy(status = targetStatus))
                                                }
                                            }
                                        }
                                    }
                                }.padding(4.dp)) {
                                    Checkbox(
                                        checked = isAllHadir,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = KelasinPrimary)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Tandai Hadir Semua",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Table Section
                        if (mhsList.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Belum ada data mahasigma.\nKetuk ikon 👥 di kanan atas untuk mengelola data.",
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else if (records.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = KelasinPrimary)
                            }
                        } else {
                            val mhsMap = remember(mhsList) { mhsList.associateBy { it.id } }
                            val sortedRecords = remember(records, mhsMap) {
                                records.sortedWith(compareBy({ mhsMap[it.mahasiswaId]?.nomorAbsen ?: 0 }, { mhsMap[it.mahasiswaId]?.displayNama ?: "" }))
                            }
                            
                            // Horizontal scroll for table
                            val scrollState = rememberScrollState()
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(scrollState)
                                    .background(tableBg)
                            ) {
                                // Table Header
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(tableBg)
                                        .border(1.dp, tableBorder)
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("No", modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                    Text("Nama Mahasigma", modifier = Modifier.width(220.dp).padding(start = 6.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text("HADIR", modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = TextAlign.Center)
                                    Text("ALPHA", modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = TextAlign.Center)
                                    Text("SAKIT", modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = TextAlign.Center)
                                    Text("IZIN", modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = TextAlign.Center)
                                }
                                
                                // Table Body
                                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                    itemsIndexed(sortedRecords, key = { _, item -> item.id }) { index, record ->
                                        val mhs = mhsMap[record.mahasiswaId]
                                    val renderStatus = localStatusOverrides[record.id] ?: record.status
                                    AnimatedVisibility(visible = userRole != "MAHASISWA" || renderStatus != StatusAbsensi.BELUM_DIPILIH) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .background(tableBg)
                                                .border(0.5.dp, tableBorder)
                                                .padding(vertical = 10.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (mhs?.nomorAbsen != null && mhs.nomorAbsen > 0) "${mhs.nomorAbsen}" else "${index + 1}",
                                                modifier = Modifier.width(30.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                mhs?.displayNama ?: "-",
                                                modifier = Modifier.width(220.dp).padding(start = 6.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            
                                            // Radio buttons
                                            Box(Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                                                RadioButton(selected = renderStatus == StatusAbsensi.HADIR, onClick = { if (userRole == "ATMIN") { localStatusOverrides = localStatusOverrides + (record.id to StatusAbsensi.HADIR); scope.launch { absensiRepo.update(record.copy(status = StatusAbsensi.HADIR)) } } }, colors = RadioButtonDefaults.colors(selectedColor = KelasinPrimary), modifier = Modifier.scale(0.9f))
                                            }
                                            Box(Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                                                RadioButton(selected = renderStatus == StatusAbsensi.ALPHA, onClick = { if (userRole == "ATMIN") { localStatusOverrides = localStatusOverrides + (record.id to StatusAbsensi.ALPHA); scope.launch { absensiRepo.update(record.copy(status = StatusAbsensi.ALPHA)) } } }, colors = RadioButtonDefaults.colors(selectedColor = KelasinPrimary), modifier = Modifier.scale(0.9f))
                                            }
                                            Box(Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                                                RadioButton(selected = renderStatus == StatusAbsensi.SAKIT, onClick = { if (userRole == "ATMIN") { localStatusOverrides = localStatusOverrides + (record.id to StatusAbsensi.SAKIT); scope.launch { absensiRepo.update(record.copy(status = StatusAbsensi.SAKIT)) } } }, colors = RadioButtonDefaults.colors(selectedColor = KelasinPrimary), modifier = Modifier.scale(0.9f))
                                            }
                                            Box(Modifier.width(55.dp), contentAlignment = Alignment.Center) {
                                                RadioButton(selected = renderStatus == StatusAbsensi.IZIN, onClick = { if (userRole == "ATMIN") { localStatusOverrides = localStatusOverrides + (record.id to StatusAbsensi.IZIN); scope.launch { absensiRepo.update(record.copy(status = StatusAbsensi.IZIN)) } } }, colors = RadioButtonDefaults.colors(selectedColor = KelasinPrimary), modifier = Modifier.scale(0.9f))
                                            }
                                        }
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
    }
}

private fun exportCsv(context: Context, mk: MataKuliahEntity, pertemuan: Int, mhsList: List<MahasiswaEntity>, records: List<AbsensiEntity>) {
    try {
        val fileName = "Absensi_${mk.kode}_Pertemuan_${pertemuan}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        val mhsMap = mhsList.associateBy { it.id }
        file.printWriter().use { out ->
            val cetakDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id")).format(Date(System.currentTimeMillis()))
            out.println("Nama Mata Kuliah,${mk.nama}")
            out.println("Nama Dosen,${mk.dosen}")
            out.println("Pertemuan Ke,$pertemuan")
            out.println("Tanggal Cetak,$cetakDate")
            out.println("")
            out.println("No,Nama Mahasigma,Status,Tanggal")
            var i = 1
            // Urutkan absen by nama mahasiswa
            val sorted = records.sortedWith(compareBy({ r -> mhsMap[r.mahasiswaId]?.nomorAbsen ?: 0 }, { r -> mhsMap[r.mahasiswaId]?.displayNama ?: "" }))
            sorted.forEach { a ->
                val mhs = mhsMap[a.mahasiswaId]
                val tgl = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id")).format(Date(a.tanggal))
                val statusText = if (a.status == StatusAbsensi.BELUM_DIPILIH) "" else a.status.name
                out.println("$i,${mhs?.displayNama ?: "-"},$statusText,$tgl")
                i++
            }
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Buka CSV"))
        Toast.makeText(context, "Berhasil export ke ${file.absolutePath}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal export: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahasiswaManagerUI(
    mhsRepo: MahasiswaRepository,
    onClose: () -> Unit
) {
    val mhsList by mhsRepo.getAll().collectAsStateWithLifecycle(emptyList())
    val sortedMhsList = remember(mhsList) { mhsList.sortedWith(compareBy({ it.nomorAbsen }, { it.displayNama })) }
    var namaBaru by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf<Long?>(null) }
    var editNama by remember { mutableStateOf("") }
    var editNomor by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                Text("Kelola Data Mahasigma", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = namaBaru,
                    onValueChange = { namaBaru = it },
                    label = { Text("Nama Mahasigma Baru") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = namaBaru.isNotBlank() && !isLoading,
                    onClick = {
                        isLoading = true
                        scope.launch {
                            mhsRepo.insert(MahasiswaEntity(nama = namaBaru.trim()))
                            namaBaru = ""
                            isLoading = false
                        }
                    }
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Tambah")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (mhsList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data mahasigma.\nSilakan tambah secara manual atau pulihkan data.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(sortedMhsList, key = { _, it -> it.id }) { index, mhs ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (editId == mhs.id) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            val listStateNumPicker = androidx.compose.foundation.lazy.rememberLazyListState(
                                                initialFirstVisibleItemIndex = (editNomor.toIntOrNull() ?: 1).coerceIn(1, 99) - 1
                                            )
                                            val currentNum = remember(listStateNumPicker.firstVisibleItemIndex) {
                                                listStateNumPicker.firstVisibleItemIndex + 1
                                            }
                                            LaunchedEffect(currentNum) { editNomor = currentNum.toString() }
                                            Box(Modifier.width(70.dp).height(80.dp)) {
                                                androidx.compose.foundation.lazy.LazyColumn(
                                                    state = listStateNumPicker,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp),
                                                    flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listStateNumPicker)
                                                ) {
                                                    items(99) { idx ->
                                                        val num = idx + 1
                                                        val isSelected = (editNomor.toIntOrNull() ?: 1) == num
                                                        Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = num.toString().padStart(2, '0'),
                                                                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                                                color = if (isSelected) KelasinPrimary else KelasinSubtext,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                                    val y = size.height / 2 - 16.dp.toPx()
                                                    val y2 = size.height / 2 + 16.dp.toPx()
                                                    drawLine(color = androidx.compose.ui.graphics.Color(0xFF1565C0), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
                                                    drawLine(color = androidx.compose.ui.graphics.Color(0xFF1565C0), start = Offset(0f, y2), end = Offset(size.width, y2), strokeWidth = 1.5.dp.toPx())
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            OutlinedTextField(
                                                value = editNama,
                                                onValueChange = { editNama = it },
                                                label = { Text("Nama") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            IconButton(onClick = {
                                                scope.launch {
                                                    val cleanNama = editNama.replace(Regex("\\[NO:\\d+\\]"), "").trim()
                                                    mhsRepo.update(mhs.copy(nama = "$cleanNama [NO:${editNomor.trim()}]"))
                                                    editId = null
                                                }
                                            }) { Icon(Icons.Filled.Check, "Simpan", tint = KelasinSuccess) }
                                            IconButton(onClick = { editId = null }) { Icon(Icons.Filled.Close, "Batal") }
                                        }
                                    }
                                } else {
                                    // Numbering on the left
                                    Box(
                                        Modifier
                                            .width(40.dp)
                                            .padding(end = 4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = (index + 1).toString().padStart(2, '0') + ".",
                                            fontWeight = FontWeight.Bold,
                                            color = KelasinPrimary.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    Column(Modifier.weight(1f)) {
                                        Text(mhs.displayNama, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        if (mhs.nomorAbsen > 0) {
                                            Text("Urutan: ${mhs.nomorAbsen}", style = MaterialTheme.typography.labelSmall, color = KelasinSubtext)
                                        }
                                    }

                                    // Swap Controls removed as per user request


                                    Spacer(Modifier.width(4.dp))

                                    IconButton(onClick = {
                                        editId = mhs.id
                                        editNama = mhs.displayNama
                                        editNomor = if (mhs.nomorAbsen > 0) mhs.nomorAbsen.toString() else (index + 1).toString()
                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Edit, "Edit", tint = KelasinPrimary, modifier = Modifier.size(18.dp)) }
                                    
                                    IconButton(onClick = {
                                        scope.launch { mhsRepo.delete(mhs) }
                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Hapus", tint = KelasinError, modifier = Modifier.size(18.dp)) }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(80.dp).navigationBarsPadding())
                    }
                }
            }
        }
    }
}

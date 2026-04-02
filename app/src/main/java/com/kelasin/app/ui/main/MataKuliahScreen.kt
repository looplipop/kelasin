package com.kelasin.app.ui.main

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kelasin.app.data.entity.MataKuliahEntity
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.ui.detail.DetailActivity
import com.kelasin.app.ui.theme.*
import kotlinx.coroutines.launch

private val MATKUL_PALETTE = listOf(
    "#1565C0", "#2E7D32", "#6A1B9A", "#E65100",
    "#00838F", "#AD1457", "#4527A0", "#558B2F",
    "#00695C", "#EF6C00", "#D32F2F", "#0277BD"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MataKuliahScreen(
    userId: String,
    userRole: String,
    repo: MataKuliahRepository,
    navController: NavController?
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val darkTheme = LocalThemeMode.current
    val mataKuliahList by repo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<MataKuliahEntity?>(null) }

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
                        title = { Text("Mata Kuliah", fontWeight = FontWeight.SemiBold) },
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
        floatingActionButton = {
            if (userRole == "ATMIN") {
                FloatingActionButton(
                    onClick = { editItem = null; showDialog = true },
                    containerColor = KelasinPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, "Tambah", tint = Color.White)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (mataKuliahList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.School, null, modifier = Modifier.size(72.dp), tint = KelasinPrimaryLight)
                    Text("Belum ada mata kuliah", color = KelasinSubtext, style = MaterialTheme.typography.bodyLarge)
                    Text("Tekan + untuk menambahkan", color = KelasinSubtext, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            if (userRole == "MAHASIGMA") {
                val chunkedList = mataKuliahList.chunked(2)
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(chunkedList, key = { _, rowItems -> rowItems.first().id }) { index, rowItems ->
                        BouncyListItem(index = index) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                if (rowItems.size == 2) {
                                    Box(Modifier.weight(1f).padding(end = 5.dp)) {
                                        MataKuliahGridCard(mk = rowItems[0], onClick = {
                                            context.startActivity(Intent(context, DetailActivity::class.java).apply { putExtra("TYPE", "MATKUL"); putExtra("ID", rowItems[0].id) })
                                        })
                                    }
                                    Box(Modifier.weight(1f).padding(start = 5.dp)) {
                                        MataKuliahGridCard(mk = rowItems[1], onClick = {
                                            context.startActivity(Intent(context, DetailActivity::class.java).apply { putExtra("TYPE", "MATKUL"); putExtra("ID", rowItems[1].id) })
                                        })
                                    }
                                } else {
                                    Box(Modifier.fillMaxWidth(0.5f)) {
                                        MataKuliahGridCard(mk = rowItems[0], onClick = {
                                            context.startActivity(Intent(context, DetailActivity::class.java).apply { putExtra("TYPE", "MATKUL"); putExtra("ID", rowItems[0].id) })
                                        })
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(mataKuliahList, key = { _, it -> it.id }) { index, mk ->
                        BouncyListItem(index = index) {
                            MataKuliahCard(
                                mk = mk, userRole = userRole,
                                onClick = {
                                    context.startActivity(Intent(context, DetailActivity::class.java).apply {
                                        putExtra("TYPE", "MATKUL"); putExtra("ID", mk.id)
                                    })
                                },
                                onEdit = { editItem = mk; showDialog = true },
                                onDelete = { scope.launch { repo.delete(mk) } }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showDialog) {
        MataKuliahDialog(
            existing = editItem, userId = userId,
            onDismiss = { showDialog = false },
            onSave = { mk ->
                scope.launch { if (mk.id == 0L) repo.insert(mk) else repo.update(mk) }
                showDialog = false
            }
        )
    }
}

@Composable
fun MataKuliahCard(
    mk: MataKuliahEntity,
    userRole: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val mkColor = remember(mk.warna) {
        runCatching { Color(android.graphics.Color.parseColor(mk.warna)) }.getOrElse { KelasinPrimary }
    }
    val hariSingkat = remember(mk.hari) { shortHari(mk.hari) }
    val infoJadwal = remember(mk.hari, mk.jamMulai, mk.jamSelesai) {
        buildString {
            append(mk.hari)
            append(" · ")
            append(mk.jamMulai)
            append("–")
            append(mk.jamSelesai)
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(mkColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    hariSingkat,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    mk.nama,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(mk.dosen, style = MaterialTheme.typography.bodySmall, color = KelasinSubtext)
                Text(infoJadwal, color = KelasinSubtext, style = MaterialTheme.typography.bodySmall)
                if (mk.ruangan.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = mkColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            mk.ruangan,
                            style = MaterialTheme.typography.labelSmall,
                            color = mkColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (userRole == "ATMIN") {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, null, tint = KelasinSubtext, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = KelasinError, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun MataKuliahGridCard(
    mk: MataKuliahEntity,
    onClick: () -> Unit
) {
    val mkColor = remember(mk.warna) { runCatching { Color(android.graphics.Color.parseColor(mk.warna)) }.getOrElse { KelasinPrimary } }
    val darkTheme = com.kelasin.app.ui.theme.LocalThemeMode.current
    val hariSingkat = remember(mk.hari) { com.kelasin.app.ui.main.shortHari(mk.hari) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp) // Adjusted height to comfortably fit 5 distinct items vertically
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.2f),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (darkTheme) MaterialTheme.colorScheme.surface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Calendar Header Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(mkColor)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Calendar rings (visual only)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                }
                Text(
                    text = mk.hari.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }
            
            // Calendar Body Block
            Column(
                Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mk.nama,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    if (mk.dosen.isNotBlank()) {
                        Text(
                            text = mk.dosen,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = KelasinSubtext,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Jam
                Text(
                    text = "${mk.jamMulai} - ${mk.jamSelesai}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Ruangan
                if (mk.ruangan.isNotBlank() && mk.ruangan != "-") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = mk.ruangan,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MataKuliahDialog(
    existing: MataKuliahEntity?,
    userId: String,
    onDismiss: () -> Unit,
    onSave: (MataKuliahEntity) -> Unit
) {
    var nama       by remember(existing) { mutableStateOf(existing?.nama ?: "") }
    var kode       by remember(existing) { mutableStateOf(existing?.kode ?: "") }
    var dosen      by remember(existing) { mutableStateOf(existing?.dosen ?: "") }
    var sks        by remember(existing) { mutableStateOf(existing?.sks?.toString() ?: "3") }
    var hari       by remember(existing) { mutableStateOf(existing?.hari ?: "Senin") }
    var jamMulai   by remember(existing) { mutableStateOf(existing?.jamMulai ?: "08:00") }
    var jamSelesai by remember(existing) { mutableStateOf(existing?.jamSelesai ?: "10:00") }
    var ruangan    by remember(existing) { mutableStateOf(existing?.ruangan ?: "") }
    var emailDosen by remember(existing) { mutableStateOf(existing?.emailDosen ?: "") }
    var noHpDosen  by remember(existing) { mutableStateOf(existing?.noHpDosen ?: "") }
    var warna      by remember(existing) { mutableStateOf(existing?.warna ?: MATKUL_PALETTE[0]) }

    // Time picker state
    var showJamMulaiPicker   by remember { mutableStateOf(false) }
    var showJamSelesaiPicker by remember { mutableStateOf(false) }

    val hariOptions = listOf("Senin","Selasa","Rabu","Kamis","Jumat","Sabtu","Minggu")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Mata Kuliah" else "Edit Mata Kuliah", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(nama, { nama = it }, label = { Text("Nama Mata Kuliah") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(kode, { kode = it }, label = { Text("Kode") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(dosen, { dosen = it }, label = { Text("Dosen") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(sks, { sks = it }, label = { Text("SKS") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                // Hari chips
                item {
                    Text("Hari", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(hariOptions) { h ->
                            FilterChip(selected = hari == h, onClick = { hari = h }, label = { Text(h) })
                        }
                    }
                }
                // Time fields with icon
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = jamMulai,
                            onValueChange = { jamMulai = it },
                            label = { Text("Mulai") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showJamMulaiPicker = true }) {
                                    Icon(Icons.Filled.Schedule, null, tint = KelasinPrimary)
                                }
                            }
                        )
                        OutlinedTextField(
                            value = jamSelesai,
                            onValueChange = { jamSelesai = it },
                            label = { Text("Selesai") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showJamSelesaiPicker = true }) {
                                    Icon(Icons.Filled.Schedule, null, tint = KelasinPrimary)
                                }
                            }
                        )
                    }
                }
                item { OutlinedTextField(ruangan, { ruangan = it }, label = { Text("Ruangan") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(emailDosen, { emailDosen = it }, label = { Text("Email Dosen") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(noHpDosen, { noHpDosen = it }, label = { Text("No HP Dosen") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                // Color picker
                item {
                    Text("Warna Matkul", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        items(MATKUL_PALETTE) { hex ->
                            val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { KelasinPrimary }
                            val isSelected = warna == hex
                            Box(
                                Modifier
                                    .size(if (isSelected) 36.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { warna = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary), onClick = {
                onSave(MataKuliahEntity(
                    id = existing?.id ?: 0L,
                    nama = nama, kode = kode, dosen = dosen,
                    sks = sks.toIntOrNull() ?: 3, hari = hari,
                    jamMulai = jamMulai, jamSelesai = jamSelesai,
                    ruangan = ruangan, emailDosen = emailDosen,
                    noHpDosen = noHpDosen, warna = warna, userId = userId
                ))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )

    // Time pickers
    if (showJamMulaiPicker) {
        KelasinTimePicker(
            initial = jamMulai,
            onDismiss = { showJamMulaiPicker = false }
        ) { h, m -> jamMulai = "%02d:%02d".format(h, m); showJamMulaiPicker = false }
    }
    if (showJamSelesaiPicker) {
        KelasinTimePicker(
            initial = jamSelesai,
            onDismiss = { showJamSelesaiPicker = false }
        ) { h, m -> jamSelesai = "%02d:%02d".format(h, m); showJamSelesaiPicker = false }
    }
}

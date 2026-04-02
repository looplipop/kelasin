package com.kelasin.app.ui.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.kelasin.app.data.repository.MateriRepository
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.ui.theme.*
import com.kelasin.app.ui.viewer.FileViewerActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriScreen(
    userId: String,
    userRole: String,
    repo: MateriRepository,
    mkRepo: MataKuliahRepository,
    navController: NavController?
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val darkTheme = LocalThemeMode.current
    val materiList by repo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    val mkList by mkRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<MateriEntity?>(null) }
    var selectedMk by remember { mutableStateOf<MataKuliahEntity?>(null) }

    val filtered = if (selectedMk != null) materiList.filter { it.mataKuliahId == selectedMk!!.id } else materiList

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
                            if (selectedMk != null) Text(selectedMk!!.nama, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            else Text("Materi", fontWeight = FontWeight.SemiBold)
                        },
                        navigationIcon = {
                            if (selectedMk != null) { IconButton(onClick = { selectedMk = null }) { Icon(Icons.Filled.ArrowBack, null) } }
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
            }, label = "materi_tier") { currentMk ->
                if (currentMk == null) {
                    // Tier 1: Matkul list
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
                            val count = materiList.count { it.mataKuliahId == mk.id }
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
                                        Text("$count materi", color = KelasinSubtext, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Filled.ChevronRight, null, tint = KelasinSubtext)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                } else {
                    // Tier 2: Materi list for selected Matkul
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Folder, null, modifier = Modifier.size(64.dp), tint = KelasinPrimaryLight)
                                Text("Belum ada materi", color = KelasinSubtext)
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered, key = { it.id }) { materi ->
                                MateriCard(
                                    materi = materi,
                                    mkColor = runCatching { Color(android.graphics.Color.parseColor(currentMk.warna)) }.getOrElse { KelasinPrimary },
                                    userRole = userRole,
                                    onOpenLink = {
                                        if (materi.url.isNotBlank()) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(materi.url)))
                                        }
                                    },
                                    onOpenFile = {
                                        if (materi.fileUri.isNotBlank()) {
                                            context.startActivity(Intent(context, FileViewerActivity::class.java).apply {
                                                putExtra("FILE_URI", materi.fileUri); putExtra("TITLE", materi.judul)
                                            })
                                        }
                                    },
                                    onEdit = { editItem = materi; showDialog = true },
                                    onDelete = { scope.launch { repo.delete(materi) } },
                                    onToggleBookmark = { scope.launch { repo.update(materi.copy(isBookmarked = !materi.isBookmarked)) } }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedMk != null) {
        MateriDialog(
            existing = editItem, userId = userId, mkList = mkList, preselectedMk = selectedMk,
            onDismiss = { showDialog = false },
            onSave = { m ->
                scope.launch { if (m.id == 0L) repo.insert(m) else repo.update(m) }
                showDialog = false
            }
        )
    }
}

@Composable
fun MateriCard(
    materi: MateriEntity,
    mkColor: Color,
    userRole: String,
    onOpenLink: () -> Unit,
    onOpenFile: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val tipeIcon = when (materi.tipe) { TipeMateri.PDF -> Icons.Filled.PictureAsPdf; TipeMateri.GAMBAR -> Icons.Filled.Image; else -> Icons.Filled.Link }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(mkColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(tipeIcon, null, tint = mkColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        materi.judul,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (materi.deskripsi.isNotBlank()) Text(materi.deskripsi, color = KelasinSubtext, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                    Icon(if (materi.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, tint = mkColor, modifier = Modifier.size(20.dp))
                }
            }
            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (materi.url.isNotBlank()) {
                    OutlinedButton(onClick = onOpenLink, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Icon(Icons.Filled.Link, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                        Text("Buka Link", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (materi.fileUri.isNotBlank()) {
                    OutlinedButton(onClick = onOpenFile, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                        Text("Buka File", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (userRole == "ATMIN") {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Edit, null, tint = KelasinSubtext, modifier = Modifier.size(16.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Delete, null, tint = KelasinError, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriDialog(
    existing: MateriEntity?,
    userId: String,
    mkList: List<MataKuliahEntity>,
    preselectedMk: MataKuliahEntity?,
    onDismiss: () -> Unit,
    onSave: (MateriEntity) -> Unit
) {
    var judul by remember(existing) { mutableStateOf(existing?.judul ?: "") }
    var deskripsi by remember(existing) { mutableStateOf(existing?.deskripsi ?: "") }
    var url by remember(existing) { mutableStateOf(existing?.url ?: "") }
    var fileUri by remember(existing) { mutableStateOf(existing?.fileUri ?: "") }
    var selectedMk by remember(existing) { mutableStateOf(mkList.firstOrNull { it.id == existing?.mataKuliahId } ?: preselectedMk ?: mkList.firstOrNull()) }
    var tipe by remember(existing) { mutableStateOf(existing?.tipe ?: TipeMateri.LINK) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            fileUri = uri.toString()
            tipe = TipeMateri.PDF
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Materi" else "Edit Materi", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(judul, { judul = it }, label = { Text("Judul Materi") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(deskripsi, { deskripsi = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth(), maxLines = 3) }
                item {
                    OutlinedTextField(url, { url = it }, label = { Text("Link URL (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Link, null, tint = KelasinSubtext) })
                }
                item {
                    OutlinedButton(onClick = { launcher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (fileUri.isBlank()) "Pilih File Lampiran" else "Ganti File Lampiran")
                    }
                    if (fileUri.isNotBlank()) Text("File terpilih", style = MaterialTheme.typography.labelSmall, color = StatusHadir, modifier = Modifier.padding(top = 4.dp))
                }
                item {
                    Text("Tipe Materi", style = MaterialTheme.typography.labelMedium, color = KelasinSubtext)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        TipeMateri.values().forEach { t -> FilterChip(selected = tipe == t, onClick = { tipe = t }, label = { Text(t.name) }) }
                    }
                }
            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary), onClick = {
                if (selectedMk != null && judul.isNotBlank()) {
                    onSave(MateriEntity(id = existing?.id ?: 0L, mataKuliahId = selectedMk!!.id, judul = judul, deskripsi = deskripsi,
                        tipe = tipe, url = url, fileUri = fileUri, isBookmarked = existing?.isBookmarked ?: false, userId = userId))
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

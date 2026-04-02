package com.kelasin.app.ui.detail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelasin.app.data.entity.*
import com.kelasin.app.data.repository.CatatanRepository
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.data.repository.TugasRepository
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinDarkBannerBlue
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.KelasinPrimaryLight
import com.kelasin.app.ui.theme.LocalThemeMode
import com.kelasin.app.ui.theme.KelasinTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * DetailActivity — halaman detail data.
 * Intent extras:
 *   TYPE: String = "MATKUL" | "TUGAS" | "CATATAN"
 *   ID  : Long   = id dari item
 */
class DetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getStringExtra("TYPE") ?: "MATKUL"
        val id   = intent.getLongExtra("ID", -1L)
        val mkRepo = MataKuliahRepository(this)
        val tugasRepo = TugasRepository(this)
        val catatanRepo = CatatanRepository(this)
        val prefsRepo = com.kelasin.app.data.repository.UserPreferencesRepository(this)

        setContent {
            val themeMode by prefsRepo.themeMode.collectAsStateWithLifecycle(initialValue = 0)
            val isDark = when(themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            KelasinTheme(darkTheme = isDark) {
                val darkTheme = LocalThemeMode.current
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
                                @OptIn(ExperimentalMaterial3Api::class)
                                TopAppBar(
                                    title = { Text(when(type) { "TUGAS" -> "Detail Tugas"; "CATATAN" -> "Detail Catatan"; else -> "Detail Mata Kuliah" }) },
                                    navigationIcon = {
                                        IconButton(onClick = { finish() }) {
                                            Icon(Icons.Filled.ArrowBack, "Kembali", tint = Color.White)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        titleContentColor = Color.White,
                                        navigationIconContentColor = Color.White
                                    ),
                                    windowInsets = WindowInsets(0, 0, 0, 0)
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (type) {
                            "TUGAS"    -> TugasDetailContent(id, tugasRepo, mkRepo)
                            "CATATAN"  -> CatatanDetailContent(id, catatanRepo, mkRepo)
                            "MATKUL"   -> MatkulDetailContent(id, mkRepo)
                            else       -> Text("Tidak dikenali", Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatkulDetailContent(id: Long, repo: MataKuliahRepository) {
    var mk by remember { mutableStateOf<MataKuliahEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        isLoading = true
        error = null
        mk = runCatching { repo.getById(id) }
            .onFailure { error = it.message ?: "Gagal memuat detail mata kuliah" }
            .getOrNull()
        isLoading = false
    }

    when {
        isLoading -> LoadingState()
        error != null -> ErrorState(error!!)
        mk == null -> EmptyState("Data mata kuliah tidak ditemukan")
        else -> {
            val m = mk!!
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("Nama", m.nama)
                DetailRow("Kode", m.kode)
                DetailRow("Dosen", m.dosen)
                if (m.emailDosen.isNotBlank() && m.emailDosen != "-") DetailRow("Email Dosen", m.emailDosen)
                if (m.noHpDosen.isNotBlank() && m.noHpDosen != "-") DetailRow("No HP Dosen", m.noHpDosen)
                DetailRow("SKS", "${m.sks}")
                DetailRow("Jadwal", "${m.hari}, ${m.jamMulai} – ${m.jamSelesai}")
                DetailRow("Ruangan", m.ruangan)
            }
        }
    }
}

@Composable
fun TugasDetailContent(
    id: Long,
    tugasRepo: TugasRepository,
    mkRepo: MataKuliahRepository
) {
    var tugas by remember { mutableStateOf<TugasEntity?>(null) }
    var mkNama by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val fmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id"))

    LaunchedEffect(id) {
        isLoading = true
        error = null
        runCatching {
            val found = tugasRepo.getById(id)
            val mk = found?.let { mkRepo.getById(it.mataKuliahId) }
            tugas = found
            mkNama = mk?.nama ?: "-"
        }.onFailure {
            error = it.message ?: "Gagal memuat detail tugas"
        }
        isLoading = false
    }

    when {
        isLoading -> LoadingState()
        error != null -> ErrorState(error!!)
        tugas == null -> EmptyState("Data tugas tidak ditemukan")
        else -> {
            val t = tugas!!
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("Judul", t.judul)
                DetailRow("Mata Kuliah", mkNama)
                DetailRow("Deadline", fmt.format(Date(t.deadline)))
                DetailRow("Pertemuan Ke", t.pertemuanKe.toString())
                DetailRow("Prioritas", t.prioritas.name)
                DetailRow("Status", t.status.name)
                if (t.fileUri.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    val context = LocalContext.current
                    Button(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse(t.fileUri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) { e.printStackTrace() }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AttachFile, "Lampiran", Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Buka Lampiran")
                    }
                }
                if (t.deskripsi.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Deskripsi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(Modifier.fillMaxWidth()) {
                        Text(t.deskripsi, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun CatatanDetailContent(
    id: Long,
    catatanRepo: CatatanRepository,
    mkRepo: MataKuliahRepository
) {
    var catatan by remember { mutableStateOf<CatatanEntity?>(null) }
    var mkNama by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id"))

    LaunchedEffect(id) {
        isLoading = true
        error = null
        runCatching {
            val found = catatanRepo.getById(id)
            val mk = found?.mataKuliahId?.let { mkRepo.getById(it) }
            catatan = found
            mkNama = mk?.nama ?: found?.labelKustom?.ifBlank { "-" }.orEmpty()
        }.onFailure {
            error = it.message ?: "Gagal memuat detail catatan"
        }
        isLoading = false
    }

    when {
        isLoading -> LoadingState()
        error != null -> ErrorState(error!!)
        catatan == null -> EmptyState("Data catatan tidak ditemukan")
        else -> {
            val c = catatan!!
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(c.judul, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    mkNama,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Diupdate: ${fmt.format(Date(c.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Text(c.isi, style = MaterialTheme.typography.bodyLarge)
                if (c.tag.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        c.tag.split(",").forEach { tag ->
                            if (tag.isNotBlank()) AssistChip(onClick = {}, label = { Text("#${tag.trim()}") })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

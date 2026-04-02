package com.kelasin.app.ui.viewer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinDarkBannerBlue
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.KelasinPrimaryLight
import com.kelasin.app.ui.theme.LocalThemeMode
import com.kelasin.app.ui.theme.KelasinTheme
import java.io.BufferedReader
import java.io.InputStreamReader

class FileViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val rawUri = intent.getStringExtra("FILE_URI") ?: ""
        val title  = intent.getStringExtra("TITLE") ?: "Lampiran"

        setContent {
            KelasinTheme {
                FileViewerScreen(rawUri = rawUri, title = title, onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(rawUri: String, title: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val darkTheme = LocalThemeMode.current

    val isImage = rawUri.hasImageExtension() || rawUri.isImageMimeFromUri(context)
    val isText  = rawUri.hasTextExtension()
    val isWeb   = rawUri.startsWith("http://") || rawUri.startsWith("https://")

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
                        title = { Text(title, maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Tutup") }
                        },
                        actions = {
                            if (!isWeb) {
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(rawUri.toUri(), context.contentResolver.getType(rawUri.toUri()) ?: "*/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini.", Toast.LENGTH_SHORT).show()
                                    }
                                }) { Icon(Icons.Filled.OpenInBrowser, "Buka di Aplikasi Lain") }
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isWeb -> {
                    // For web links, open browser directly
                    LaunchedEffect(rawUri) {
                        val intent = Intent(Intent.ACTION_VIEW, rawUri.toUri())
                        context.startActivity(intent)
                        onClose()
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KelasinPrimary)
                    }
                }
                isImage -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(rawUri.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().background(Color.Black)
                    )
                }
                isText -> {
                    var text by remember { mutableStateOf("Memuat...") }
                    LaunchedEffect(rawUri) {
                        text = try {
                            val stream = context.contentResolver.openInputStream(rawUri.toUri())!!
                            BufferedReader(InputStreamReader(stream)).readText().also { stream.close() }
                        } catch (e: Exception) { "Gagal memuat file: ${e.message}" }
                    }
                    Text(
                        text = text,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
                else -> {
                    // PDF, DOCX, XLSX — send to system viewer
                    LaunchedEffect(rawUri) {
                        try {
                            val mimeType = context.contentResolver.getType(rawUri.toUri()) ?: "*/*"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(rawUri.toUri(), mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Install aplikasi PDF / Office viewer untuk membuka file ini.", Toast.LENGTH_LONG).show()
                        }
                        onClose()
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = KelasinPrimary)
                            Text("Membuka file...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun String.hasImageExtension(): Boolean {
    val lower = this.lowercase()
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
           lower.endsWith(".png") || lower.endsWith(".webp") ||
           lower.endsWith(".gif") || lower.endsWith(".bmp")
}

private fun String.hasTextExtension(): Boolean {
    val lower = this.lowercase()
    return lower.endsWith(".txt") || lower.endsWith(".csv") ||
           lower.endsWith(".json") || lower.endsWith(".xml") ||
           lower.endsWith(".md")
}

private fun String.isImageMimeFromUri(context: android.content.Context): Boolean {
    return try {
        val mime = context.contentResolver.getType(this.toUri()) ?: ""
        mime.startsWith("image/")
    } catch (e: Exception) { false }
}

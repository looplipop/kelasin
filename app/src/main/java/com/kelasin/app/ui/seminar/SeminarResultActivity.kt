package com.kelasin.app.ui.seminar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelasin.app.ui.main.MainActivity
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.KelasinSuccess
import com.kelasin.app.ui.theme.KelasinTheme

class SeminarResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getStringExtra(SeminarContract.EXTRA_USER_ID).orEmpty()
        val userName = intent.getStringExtra(SeminarContract.EXTRA_USER_NAME).orEmpty()
        val nama = intent.getStringExtra(SeminarContract.EXTRA_NAMA).orEmpty()
        val email = intent.getStringExtra(SeminarContract.EXTRA_EMAIL).orEmpty()
        val nomorHp = intent.getStringExtra(SeminarContract.EXTRA_NOMOR_HP).orEmpty()
        val jenisKelamin = intent.getStringExtra(SeminarContract.EXTRA_JENIS_KELAMIN).orEmpty()
        val seminar = intent.getStringExtra(SeminarContract.EXTRA_SEMINAR).orEmpty()

        setContent {
            KelasinTheme {
                SeminarResultScreen(
                    nama = nama,
                    email = email,
                    nomorHp = nomorHp,
                    jenisKelamin = jenisKelamin,
                    seminar = seminar,
                    onBack = { finish() },
                    onBackToMain = {
                        startActivity(
                            Intent(this@SeminarResultActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                        )
                        finish()
                    },
                    onRegisterAgain = {
                        startActivity(
                            Intent(this@SeminarResultActivity, SeminarRegistrationActivity::class.java).apply {
                                putExtra(SeminarContract.EXTRA_USER_ID, userId)
                                putExtra(SeminarContract.EXTRA_USER_NAME, userName)
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SeminarResultScreen(
    nama: String,
    email: String,
    nomorHp: String,
    jenisKelamin: String,
    seminar: String,
    onBack: () -> Unit,
    onBackToMain: () -> Unit,
    onRegisterAgain: () -> Unit
) {
    DynamicStatusBar(
        statusBarColor = KelasinPrimary,
        useDarkIcons = false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hasil Pendaftaran", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KelasinPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            KelasinSuccess.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(18.dp)
                .navigationBarsPadding()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(KelasinSuccess.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = KelasinSuccess,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Text(
                        "Pendaftaran Berhasil",
                        style = MaterialTheme.typography.headlineSmall,
                        color = KelasinSuccess,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Berikut data pendaftaran seminar Anda:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SeminarResultRow("Nama", nama)
                    SeminarResultRow("Email", email)
                    SeminarResultRow("Nomor HP", nomorHp)
                    SeminarResultRow("Jenis Kelamin", jenisKelamin)
                    SeminarResultRow("Seminar", seminar)

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onBackToMain,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KelasinPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Kembali ke Halaman Utama")
                    }

                    OutlinedButton(
                        onClick = onRegisterAgain,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Daftar Seminar Lagi")
                    }
                }
            }
        }
    }
}

@Composable
private fun SeminarResultRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

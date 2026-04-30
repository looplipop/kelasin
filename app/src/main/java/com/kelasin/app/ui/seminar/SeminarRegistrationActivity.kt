package com.kelasin.app.ui.seminar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kelasin.app.data.repository.SeminarItem
import com.kelasin.app.data.repository.SeminarRegistrationPayload
import com.kelasin.app.data.repository.SeminarRegistrationRepository
import com.kelasin.app.data.repository.SeminarRepository
import com.kelasin.app.ui.components.InitialsAvatar
import com.kelasin.app.ui.theme.DynamicStatusBar
import com.kelasin.app.ui.theme.KelasinError
import com.kelasin.app.ui.theme.KelasinPrimary
import com.kelasin.app.ui.theme.KelasinSecondary
import com.kelasin.app.ui.theme.KelasinTheme
import kotlinx.coroutines.launch

class SeminarRegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userId = intent.getStringExtra(SeminarContract.EXTRA_USER_ID).orEmpty()
        val userName = intent.getStringExtra(SeminarContract.EXTRA_USER_NAME).orEmpty()
        val userPic = intent.getStringExtra(SeminarContract.EXTRA_USER_PIC)
        val preselectedSeminar = intent.getStringExtra(SeminarContract.EXTRA_PRESELECTED_SEMINAR)
        val preselectedSeminarId = intent.getStringExtra(SeminarContract.EXTRA_PRESELECTED_SEMINAR_ID)

        setContent {
            KelasinTheme {
                SeminarRegistrationScreen(
                    userId = userId,
                    userName = userName,
                    userPic = userPic,
                    initialSeminarTitle = preselectedSeminar,
                    initialSeminarId = preselectedSeminarId,
                    seminarRepository = remember { SeminarRepository() },
                    repository = remember { SeminarRegistrationRepository() },
                    onBack = { finish() },
                    onSuccess = { nama, email, nomorHp, jenisKelamin, seminarTitle ->
                        startActivity(
                            Intent(this@SeminarRegistrationActivity, SeminarResultActivity::class.java).apply {
                                putExtra(SeminarContract.EXTRA_USER_ID, userId)
                                putExtra(SeminarContract.EXTRA_USER_NAME, userName)
                                putExtra(SeminarContract.EXTRA_NAMA, nama)
                                putExtra(SeminarContract.EXTRA_EMAIL, email)
                                putExtra(SeminarContract.EXTRA_NOMOR_HP, nomorHp)
                                putExtra(SeminarContract.EXTRA_JENIS_KELAMIN, jenisKelamin)
                                putExtra(SeminarContract.EXTRA_SEMINAR, seminarTitle)
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeminarRegistrationScreen(
    userId: String,
    userName: String,
    userPic: String?,
    initialSeminarTitle: String?,
    initialSeminarId: String?,
    seminarRepository: SeminarRepository,
    repository: SeminarRegistrationRepository,
    onBack: () -> Unit,
    onSuccess: (nama: String, email: String, nomorHp: String, jenisKelamin: String, seminarTitle: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nomorHp by remember { mutableStateOf("") }
    var jenisKelamin by remember { mutableStateOf<String?>(null) }
    
    // We store the list of available seminars from backend
    var availableSeminars by remember { mutableStateOf<List<SeminarItem>>(emptyList()) }
    var selectedSeminar by remember { mutableStateOf<SeminarItem?>(null) }
    
    var seminarExpanded by remember { mutableStateOf(false) }
    var persetujuan by remember { mutableStateOf(false) }

    // Custom answers map: key is the question label, value is the answer (string)
    val customAnswers = remember { mutableStateMapOf<String, String>() }

    var touchedNama by remember { mutableStateOf(false) }
    var touchedEmail by remember { mutableStateOf(false) }
    var touchedHp by remember { mutableStateOf(false) }
    var touchedGender by remember { mutableStateOf(false) }
    var touchedSeminar by remember { mutableStateOf(initialSeminarId != null) }
    var touchedPersetujuan by remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        seminarRepository.getSeminars().onSuccess { seminars ->
            // Filter out full seminars
            availableSeminars = seminars.filter { !it.isFull }
            
            if (initialSeminarId != null) {
                // Preselect if it's in the available list (meaning not full)
                selectedSeminar = availableSeminars.find { it.id == initialSeminarId }
            }
        }
    }

    fun validateNama(value: String): String? = when {
        value.isBlank() -> "Nama wajib diisi"
        value.trim().length < 2 -> "Nama minimal 2 karakter"
        else -> null
    }
    fun validateEmail(value: String): String? = when {
        value.isBlank() -> "Email wajib diisi"
        !value.contains("@") -> "Email harus mengandung @"
        value.startsWith("@") || value.endsWith("@") -> "Format email tidak valid"
        else -> null
    }
    fun validateNomorHp(value: String): String? = when {
        value.isBlank() -> "Nomor HP wajib diisi"
        value.any { !it.isDigit() } -> "Nomor HP hanya boleh angka"
        !value.startsWith("08") -> "Nomor HP harus diawali 08"
        value.length !in 10..13 -> "Nomor HP harus 10–13 digit"
        else -> null
    }

    val namaError = validateNama(nama)
    val emailError = validateEmail(email)
    val hpError = validateNomorHp(nomorHp)
    val genderError = if (jenisKelamin == null) "Jenis kelamin wajib dipilih" else null
    val seminarError = if (selectedSeminar == null) "Pilihan seminar wajib dipilih (atau kuota sudah penuh)" else null
    val persetujuanError = if (!persetujuan) "Anda harus menyetujui data yang diinput benar" else null

    fun isFormValid(): Boolean = namaError == null && emailError == null && hpError == null && genderError == null && seminarError == null && persetujuanError == null

    DynamicStatusBar(statusBarColor = KelasinPrimary, useDarkIcons = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pendaftaran Seminar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KelasinPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(KelasinPrimary.copy(alpha = 0.08f), MaterialTheme.colorScheme.background)))
                .imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(name = userName, size = 48.dp, profilePicUrl = userPic)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(if (userName.isBlank()) "Halo, lengkapi data" else "Halo $userName,", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Pastikan data pendaftaran Anda benar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedTextField(nama, { nama = it; touchedNama = true }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), isError = touchedNama && namaError != null, leadingIcon = { Icon(Icons.Filled.Person, null) }, supportingText = { if (touchedNama && namaError != null) Text(namaError, color = KelasinError) })
            OutlinedTextField(email, { email = it.trim(); touchedEmail = true }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), isError = touchedEmail && emailError != null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), leadingIcon = { Icon(Icons.Filled.Email, null) }, supportingText = { if (touchedEmail && emailError != null) Text(emailError, color = KelasinError) })
            OutlinedTextField(nomorHp, { nomorHp = it.trim(); touchedHp = true }, label = { Text("Nomor HP") }, modifier = Modifier.fillMaxWidth(), isError = touchedHp && hpError != null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), leadingIcon = { Icon(Icons.Filled.Phone, null) }, supportingText = { if (touchedHp && hpError != null) Text(hpError, color = KelasinError) })

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Jenis Kelamin", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(jenisKelamin == "Laki-laki", { jenisKelamin = "Laki-laki"; touchedGender = true }); Text("Laki-laki") }
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(jenisKelamin == "Perempuan", { jenisKelamin = "Perempuan"; touchedGender = true }); Text("Perempuan") }
                }
                if (touchedGender && genderError != null) Text(genderError, color = KelasinError, style = MaterialTheme.typography.labelSmall)
            }

            ExposedDropdownMenuBox(expanded = seminarExpanded, onExpandedChange = { seminarExpanded = !seminarExpanded; touchedSeminar = true }) {
                OutlinedTextField(
                    value = selectedSeminar?.title ?: (if (initialSeminarTitle != null && selectedSeminar == null) "$initialSeminarTitle (Penuh)" else ""),
                    onValueChange = {}, readOnly = true, label = { Text("Pilihan Seminar") }, modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
                    isError = touchedSeminar && seminarError != null, leadingIcon = { Icon(Icons.Filled.Event, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seminarExpanded) },
                    supportingText = { if (touchedSeminar && seminarError != null) Text(seminarError, color = KelasinError) }
                )
                ExposedDropdownMenu(expanded = seminarExpanded, onDismissRequest = { seminarExpanded = false }) {
                    availableSeminars.forEach { option ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(option.title) },
                            onClick = { selectedSeminar = option; touchedSeminar = true; seminarExpanded = false },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Moving persetujuan to the bottom
            // Dynamic Custom Questions
            val customQuestionList = remember(selectedSeminar?.customQuestions) {
                val list = mutableListOf<Map<String, Any>>()
                val qs = selectedSeminar?.customQuestions
                if (!qs.isNullOrBlank()) {
                    try {
                        val arr = org.json.JSONArray(qs)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val type = obj.getString("type")
                            val label = obj.getString("label")
                            val isMultiple = if (obj.has("isMultipleChoice")) obj.getBoolean("isMultipleChoice") else false
                            val item = mutableMapOf<String, Any>("type" to type, "label" to label, "isMultipleChoice" to isMultiple)
                            if (type == "checkbox") {
                                val options = obj.getJSONArray("options")
                                val optList = mutableListOf<String>()
                                for (j in 0 until options.length()) {
                                    optList.add(options.getString(j))
                                }
                                item["options"] = optList
                            }
                            list.add(item)
                        }
                    } catch (e: Exception) { }
                }
                list
            }

            if (customQuestionList.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Pertanyaan Tambahan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                for (item in customQuestionList) {
                    val type = item["type"] as String
                    val label = item["label"] as String

                    if (type == "text") {
                        OutlinedTextField(
                            value = customAnswers[label] ?: "",
                            onValueChange = { customAnswers[label] = it },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (type == "checkbox") {
                        val options = item["options"] as List<String>
                        val isMultiple = item["isMultipleChoice"] as Boolean
                        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Column {
                            if (isMultiple) {
                                val currentSelections = (customAnswers[label] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
                                for (opt in options) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = currentSelections.contains(opt),
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) currentSelections.add(opt) else currentSelections.remove(opt)
                                                customAnswers[label] = currentSelections.joinToString(",")
                                            }
                                        )
                                        Text(opt)
                                    }
                                }
                            } else {
                                val currentSelection = customAnswers[label]
                                for (opt in options) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = (currentSelection == opt),
                                            onClick = { customAnswers[label] = opt }
                                        )
                                        Text(opt)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(persetujuan, { persetujuan = it; touchedPersetujuan = true })
                Text("Saya menyetujui data yang diinput benar", style = MaterialTheme.typography.bodyMedium)
            }
            if (touchedPersetujuan && persetujuanError != null) Text(persetujuanError, color = KelasinError, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))


            Button(
                onClick = {
                    touchedNama = true; touchedEmail = true; touchedHp = true; touchedGender = true; touchedSeminar = true; touchedPersetujuan = true
                    if (!isFormValid()) {
                        scope.launch { snackbarHostState.showSnackbar(if (!persetujuan) "Centang persetujuan sebelum submit" else "Periksa kembali input yang salah") }
                        return@Button
                    }
                    showConfirmDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isSubmitting, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary)
            ) { Text(if (isSubmitting) "Menyimpan..." else "Submit Pendaftaran") }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Konfirmasi Data") },
            text = { Text("Apakah data yang Anda isi sudah benar dan valid?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    if (isSubmitting) return@TextButton
                    val s = selectedSeminar ?: return@TextButton
                    scope.launch {
                        isSubmitting = true
                        val ansJson = if (customAnswers.isNotEmpty()) {
                            val jo = org.json.JSONObject()
                            customAnswers.forEach { (k, v) -> jo.put(k, v) }
                            jo.toString()
                        } else null

                        val res = repository.submitRegistration(
                            userId = userId,
                            payload = SeminarRegistrationPayload(
                                nama = nama.trim(), email = email.trim(), nomorHp = nomorHp.trim(),
                                jenisKelamin = requireNotNull(jenisKelamin), seminar = s.title, seminarId = s.id,
                                customAnswers = ansJson
                            )
                        )
                        isSubmitting = false
                        res.fold(
                            onSuccess = { onSuccess(nama.trim(), email.trim(), nomorHp.trim(), requireNotNull(jenisKelamin), s.title) },
                            onFailure = { snackbarHostState.showSnackbar(it.message ?: "Gagal menyimpan") }
                        )
                    }
                }) { Text("Ya, Simpan") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Batal") } }
        )
    }
}

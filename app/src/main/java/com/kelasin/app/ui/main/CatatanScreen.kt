package com.kelasin.app.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import com.kelasin.app.ui.components.InitialsAvatar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kelasin.app.data.entity.CatatanEntity
import com.kelasin.app.data.entity.ChatMessageEntity
import com.kelasin.app.data.entity.MataKuliahEntity
import com.kelasin.app.data.repository.CatatanRepository
import com.kelasin.app.data.repository.ChatRepository
import com.kelasin.app.data.repository.MataKuliahRepository
import com.kelasin.app.data.repository.SharedCloudScope
import com.kelasin.app.data.repository.AuthRepository
import com.kelasin.app.data.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import com.kelasin.app.data.supabase.SupabaseRestClient
import com.kelasin.app.ui.theme.*
import com.kelasin.app.ui.viewer.FileViewerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private data class LegacyChatMessage(
    val sender: String,
    val text: String,
    val time: Long
)

private const val CHAT_SEPARATOR = "\n---CHAT---\n"

private sealed class ChatUiItem {
    data class Header(val date: String) : ChatUiItem()
    data class Message(val message: ChatMessageEntity) : ChatUiItem()
}

private fun formatChatDate(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)

    val msgCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val msgDay = msgCalendar.get(Calendar.DAY_OF_YEAR)
    val msgYear = msgCalendar.get(Calendar.YEAR)

    return when {
        msgDay == today && msgYear == year -> "Hari ini"
        msgDay == today - 1 && msgYear == year -> "Kemarin"
        msgYear == year -> SimpleDateFormat("EEEE, d MMM", Locale("id")).format(Date(timestamp))
        else -> SimpleDateFormat("d MMM yyyy", Locale("id")).format(Date(timestamp))
    }
}

private fun decodeLegacyChatMessages(catatan: CatatanEntity): List<LegacyChatMessage> {
    val source = catatan.isi.trim()
    if (source.isBlank()) return emptyList()

    val payload = if (source.contains(CHAT_SEPARATOR)) source.substringAfter(CHAT_SEPARATOR).trim() else source
    if (!payload.trimStart().startsWith("[")) {
        return listOf(
            LegacyChatMessage(
                sender = catatan.judul.ifBlank { "Catatan" },
                text = source,
                time = catatan.updatedAt
            )
        )
    }

    return runCatching {
        val arr = JSONArray(payload)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val text = obj.optString("text")
                if (text.isBlank()) continue
                add(
                    LegacyChatMessage(
                        sender = obj.optString("sender").ifBlank { catatan.judul.ifBlank { "Catatan" } },
                        text = text,
                        time = obj.optLong("time", catatan.updatedAt)
                    )
                )
            }
        }
    }.getOrElse {
        listOf(
            LegacyChatMessage(
                sender = catatan.judul.ifBlank { "Catatan" },
                text = source,
                time = catatan.updatedAt
            )
        )
    }
}

private fun isLegacyChatPayload(content: String): Boolean {
    val trimmed = content.trim()
    if (trimmed.isBlank()) return false
    if (trimmed.startsWith("[")) return true
    if (trimmed.contains(CHAT_SEPARATOR)) return true
    return runCatching { JSONArray(trimmed); true }.getOrDefault(false)
}

private fun cloudStatusText(error: Throwable?): String {
    val cause = error?.message.orEmpty().lowercase()
    if ("config belum lengkap" in cause || "supabase_url" in cause || "supabase_publishable_key" in cause) {
        return "Cloud belum dikonfigurasi. Aplikasi jalan di mode offline."
    }
    if (error is IOException || "unable to resolve host" in cause || "timeout" in cause || "failed to connect" in cause) {
        return "Cloud tidak dapat dijangkau. Aplikasi jalan di mode offline."
    }
    return "Cloud database belum terhubung"
}

private const val CHAT_NOTIFICATION_CHANNEL_ID = "kelasin_chat_channel"

private fun ensureChatNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHAT_NOTIFICATION_CHANNEL_ID,
            "Kelasin Chat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi chat pada Catatan"
        }
        manager.createNotificationChannel(channel)
    }
}

private fun maybeShowChatNotification(
    context: Context,
    room: CatatanEntity,
    latestMessage: ChatMessageEntity?
) {
    if (latestMessage == null || room.isMuted) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    ensureChatNotificationChannel(context)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        room.id.toInt(),
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = NotificationCompat.Builder(context, CHAT_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.sym_action_chat)
        .setContentTitle("Pesan baru: ${room.judul}")
        .setContentText("${latestMessage.senderName}: ${chatPreviewText(latestMessage.text).take(80)}")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    NotificationManagerCompat.from(context).notify((room.id % Int.MAX_VALUE).toInt(), builder.build())
}

private fun isRealChatContent(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return false
    if (trimmed.startsWith("[IMG]") && trimmed.endsWith("[/IMG]")) return true
    if (trimmed.startsWith("[FILE]") && trimmed.endsWith("[/FILE]")) return true
    return trimmed.isNotEmpty()
}

private fun chatPreviewText(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.startsWith("[IMG]") && trimmed.endsWith("[/IMG]")) return "Mengirim foto"
    if (trimmed.startsWith("[FILE]") && trimmed.endsWith("[/FILE]")) return "Mengirim file"
    return trimmed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatanScreen(
    userId: String,
    userName: String,
    userRole: String,
    repo: CatatanRepository,
    chatRepo: ChatRepository,
    mkRepo: MataKuliahRepository,
    authRepo: AuthRepository,
    navController: NavController?,
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val darkTheme = LocalThemeMode.current
    val mkList by mkRepo.getAll(userId).collectAsStateWithLifecycle(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val catatanList by (if (searchQuery.isBlank()) repo.getAll(userId) else repo.search(userId, searchQuery)).collectAsStateWithLifecycle(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<CatatanEntity?>(null) }
    var activeChat by remember { mutableStateOf<CatatanEntity?>(null) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var cloudStatusMessage by remember { mutableStateOf<String?>(null) }
    var cloudReady by remember { mutableStateOf<Boolean?>(null) }
    val migratedLegacyRooms = remember { mutableSetOf<Long>() }

    val activeChatState = activeChat?.let { selected ->
        catatanList.find { it.id == selected.id } ?: selected
    }
    val roomMessages by (
        activeChatState?.let { chatRepo.getMessages(it.id) } ?: flowOf(emptyList())
    ).collectAsStateWithLifecycle(emptyList())
    val allLocalMessages by chatRepo.getAllMessages().collectAsStateWithLifecycle(emptyList())
    val allRoomMessages = remember(allLocalMessages) { allLocalMessages.groupBy { it.roomId } }
    val lastNotifiedMessage = remember { mutableStateMapOf<Long, Long>() }

    val sharedPrefs = context.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(catatanList) {
        catatanList.forEach { room ->
            runCatching { chatRepo.refresh(room.id) }
        }
    }

    LaunchedEffect(activeChatState?.id, catatanList.map { it.id }) {
        if (activeChatState != null) return@LaunchedEffect
        while (true) {
            catatanList.forEach { room ->
                runCatching { chatRepo.refresh(room.id) }
            }
            delay(3500)
        }
    }

    LaunchedEffect(activeChatState?.id, roomMessages) {
        val room = activeChatState ?: return@LaunchedEffect
        // Update last read time for this room
        sharedPrefs.edit().putLong("last_read_${room.id}", System.currentTimeMillis()).apply()

        val unresolvedMine = roomMessages.filter { msg ->
            msg.senderUserId == userId &&
                (msg.status.equals("FAILED", ignoreCase = true) || msg.status.equals("SENDING", ignoreCase = true))
        }
        if (unresolvedMine.isEmpty()) return@LaunchedEffect

        unresolvedMine.forEach { msg ->
            runCatching {
                chatRepo.reconcileMessageStatus(
                    roomId = room.id,
                    localMessageId = msg.id,
                    senderUserId = msg.senderUserId,
                    text = msg.text,
                    createdAt = msg.createdAt
                )
            }
        }
    }

    LaunchedEffect(allRoomMessages, catatanList, activeChatState?.id) {
        val now = System.currentTimeMillis()
        catatanList.forEach { room ->
            if (activeChatState?.id == room.id) return@forEach
            val latest = allRoomMessages[room.id]?.maxByOrNull { it.createdAt } ?: return@forEach
            if (latest.senderUserId == userId) return@forEach
            if (!isRealChatContent(latest.text)) return@forEach
            val lastRead = sharedPrefs.getLong("last_read_${room.id}", 0L)
            val alreadyNotifiedAt = lastNotifiedMessage[room.id] ?: 0L
            val cooldownPassed = now - alreadyNotifiedAt > 5_000L
            if (latest.createdAt > lastRead && latest.createdAt > alreadyNotifiedAt && cooldownPassed) {
                maybeShowChatNotification(context, room, latest)
                lastNotifiedMessage[room.id] = latest.createdAt
            }
        }
    }

    LaunchedEffect(Unit) {
        val probe = runCatching { SupabaseRestClient.checkConnection() }
        val status = probe.getOrDefault(false)
        cloudReady = status
        cloudStatusMessage = if (status) "Cloud database aktif" else cloudStatusText(probe.exceptionOrNull())
    }

    LaunchedEffect(activeChatState?.id, activeChatState?.isi) {
        val room = activeChatState ?: return@LaunchedEffect
        if (!migratedLegacyRooms.add(room.id)) return@LaunchedEffect
        if (roomMessages.isNotEmpty()) return@LaunchedEffect
        if (!isLegacyChatPayload(room.isi)) return@LaunchedEffect
        val probe = runCatching { SupabaseRestClient.checkConnection() }
        val cloudAvailable = probe.getOrDefault(false)
        if (!cloudAvailable) {
            cloudReady = false
            cloudStatusMessage = cloudStatusText(probe.exceptionOrNull())
            return@LaunchedEffect
        }
        cloudReady = true
        cloudStatusMessage = "Cloud database aktif"
        val legacy = decodeLegacyChatMessages(room)
        if (legacy.isEmpty()) return@LaunchedEffect

        runCatching {
            legacy.forEach { legacyMsg ->
                val insertedId = chatRepo.sendMessage(
                    roomId = room.id,
                    senderUserId = userId,
                    senderName = legacyMsg.sender,
                    text = legacyMsg.text,
                    createdAt = legacyMsg.time
                )
                if (insertedId <= 0L) {
                    error("Gagal menyimpan pesan legacy ke cloud")
                }
            }
            val cleaned = room.copy(
                isi = "",
                updatedAt = legacy.maxOfOrNull { it.time } ?: System.currentTimeMillis()
            )
            repo.update(cleaned)
            repo.refreshAllRooms()
            chatRepo.refresh(room.id)
        }.onFailure {
            sendError = "Gagal migrasi pesan lama"
            migratedLegacyRooms.remove(room.id)
        }
    }

    LaunchedEffect(activeChatState?.id) {
        if (activeChatState != null) return@LaunchedEffect
        while (true) {
            runCatching { repo.refreshAllRooms() }
            delay(5000)
        }
    }

    LaunchedEffect(activeChatState?.id) {
        val roomId = activeChatState?.id ?: return@LaunchedEffect
        sendError = null
        while (true) {
            runCatching {
                chatRepo.refresh(roomId)
                repo.refreshAllRooms()
            }.onFailure {
                sendError = "Gagal sinkronisasi chat"
            }
            delay(2500)
        }
    }

    LaunchedEffect(activeChatState?.id) {
        onToggleBottomBar(activeChatState == null)
    }

    BackHandler(enabled = activeChatState != null) {
        activeChat = null
    }

    Scaffold(
        topBar = {
            val topBarBg = if (darkTheme) Color(0xFF1F3B73) else KelasinPrimary
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
                            if (activeChatState != null) {
                                Text(
                                    activeChatState.judul,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text("Catatan", fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            if (activeChatState != null) {
                                IconButton(onClick = { activeChat = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
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
            if (activeChatState == null) {
                FloatingActionButton(
                    onClick = { editItem = null; showDialog = true },
                    containerColor = KelasinPrimary,
                    contentColor = Color.White
                ) { Icon(Icons.Filled.Add, null) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (activeChatState != null) {
            activeChatState?.let { chat ->
                CatatanChatScreen(
                    catatan = chat,
                    messages = roomMessages,
                    myUserId = userId,
                    userName = userName,
                    userRole = userRole,
                    sendError = sendError,
                    onSend = { text, isAnon ->
                                scope.launch {
                                    chatRepo.sendMessage(chat.id, userId, userName, text, isAnonymous = isAnon)
                                    repo.refreshAllRooms()
                                }
                    },
                    onClose = { activeChat = null },
                    onRetry = { msg -> scope.launch { chatRepo.retryMessage(msg) } },
                    onEdit = { msg, newText -> scope.launch { chatRepo.updateMessage(msg, newText) } },
                    onDelete = { msg, isAdmin -> scope.launch { chatRepo.deleteMessage(msg, isAdmin) } },
                    authRepo = authRepo,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (cloudReady == false && !cloudStatusMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = cloudStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari room atau topik...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = KelasinSubtext) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = KelasinDivider,
                        focusedBorderColor = KelasinPrimary
                    )
                )

                if (catatanList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Notes, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("Belum ada room chat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(catatanList, key = { it.id }) { catatan ->
                            val mk = mkList.find { it.id == catatan.mataKuliahId }
                            val messagesForThisRoom = allRoomMessages[catatan.id] ?: emptyList()
                            CatatanCard(
                                catatan = catatan, mk = mk, userRole = userRole,
                                roomMessages = if (activeChatState?.id == catatan.id) roomMessages else emptyList(),
                                allMessages = messagesForThisRoom,
                                currentUserId = userId,
                                sharedPrefs = sharedPrefs,
                                onClick = {
                                    activeChat = catatan 
                                    onToggleBottomBar(false)
                                },
                                onEdit = { editItem = catatan; showDialog = true },
                                onDelete = {
                                    scope.launch {
                                        repo.delete(catatan)
                                        if (activeChatState?.id == catatan.id) activeChat = null
                                    }
                                },
                                onOpenFile = {
                                    if (catatan.fileUri.isNotBlank()) {
                                        val intent = Intent(context, FileViewerActivity::class.java)
                                        intent.putExtra("FILE_URI", catatan.fileUri)
                                        intent.putExtra("TITLE", catatan.judul)
                                        context.startActivity(intent)
                                    }
                                },
                                onToggleMute = {
                                    scope.launch {
                                        repo.update(catatan.copy(isMuted = !catatan.isMuted))
                                        android.widget.Toast.makeText(
                                            context,
                                            if (!catatan.isMuted) "Notifikasi disenyapkan" else "Notifikasi diaktifkan",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CatatanDialog(
            existing = editItem, userId = userId, mkList = mkList,
            onDismiss = { showDialog = false },
            onSave = { c ->
                scope.launch {
                    if (c.id == 0L) repo.insert(c) else repo.update(c)
                    repo.refreshAllRooms()
                }
                showDialog = false
            }
        )
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatatanChatScreen(
    catatan: CatatanEntity,
    messages: List<ChatMessageEntity>,
    myUserId: String,
    userName: String,
    userRole: String,
    sendError: String?,
    onSend: (String, Boolean) -> Unit,
    onClose: () -> Unit,
    onRetry: (ChatMessageEntity) -> Unit,
    onEdit: (ChatMessageEntity, String) -> Unit,
    onDelete: (ChatMessageEntity, Boolean) -> Unit,
    authRepo: com.kelasin.app.data.repository.AuthRepository,
    modifier: Modifier = Modifier
) {
    // SMART FILTERING: If we have multiple messages for the same text/user, 
    // prefer the one that is "SENT". This hides flickering "FAILED" messages.
    val visibleMessages = remember(messages) {
        messages.groupBy { 
            // Group by sender + normalized text + rounded timestamp (within 2 min)
            val normalizedText = it.text.trim().removeSuffix("\u200E")
            "${it.senderUserId}|$normalizedText|${it.createdAt / 120_000}" 
        }.map { (_, group) ->
            // If any message in the group is SENT, pick that one. Otherwise pick the latest one.
            group.find { it.status.uppercase() == "SENT" } ?: group.maxBy { it.createdAt }
        }.sortedBy { it.createdAt }
    }
 
    val uiItems = remember(visibleMessages) {
        val items = mutableListOf<ChatUiItem>()
        var lastDateStr = ""
        visibleMessages.forEach { msg ->
            val dateStr = formatChatDate(msg.createdAt)
            if (dateStr != lastDateStr) {
                items.add(ChatUiItem.Header(dateStr))
                lastDateStr = dateStr
            }
            items.add(ChatUiItem.Message(msg))
        }
        items
    }

    var input by remember { mutableStateOf("") }
    val darkTheme = com.kelasin.app.ui.theme.LocalThemeMode.current
    var isAnonymous by remember { mutableStateOf(false) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale("id")) }
    val listState = rememberLazyListState()
    
    var selectedMessageForAction by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var isEditingMsg by remember { mutableStateOf(false) }
    var editInput by remember { mutableStateOf("") }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    
    var showProfileForUserId by remember { mutableStateOf<String?>(null) }
    var profileUserData by remember { mutableStateOf<com.kelasin.app.data.entity.UserEntity?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    val profileCache = remember { mutableStateMapOf<String, com.kelasin.app.data.entity.UserEntity>() }

    LaunchedEffect(showProfileForUserId) {
        val targetUserId = showProfileForUserId?.trim()?.takeIf { it.isNotBlank() }
        if (targetUserId == null) {
            profileUserData = null
            isLoadingProfile = false
            return@LaunchedEffect
        }

        isLoadingProfile = true
        try {
            val resolvedProfile = authRepo.getUserProfile(targetUserId)
            
            if (showProfileForUserId?.trim() != targetUserId) return@LaunchedEffect
            profileUserData = resolvedProfile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            if (showProfileForUserId?.trim() != targetUserId) return@LaunchedEffect
            profileUserData = com.kelasin.app.data.entity.UserEntity(
                id = targetUserId,
                nama = "Gagal memuat profil",
                email = "-",
                username = "error"
            )
        } finally {
            if (showProfileForUserId?.trim() == targetUserId) {
                isLoadingProfile = false
            }
        }
    }
    
    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.lastIndex)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val uploadContent: (android.net.Uri, Boolean) -> Unit = { uri, isImageOnly ->
        isUploading = true
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    val cr = context.contentResolver
                    val type = cr.getType(uri) ?: ""
                    val isImage = type.startsWith("image/")
                    
                    val originalName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
                    val sanitizedName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val fileName = "${System.currentTimeMillis()}_$sanitizedName"
                    val path = "chat_media/${catatan.id}/$fileName"

                    SupabaseClient.client.storage["backups"].upload(path, bytes) { upsert = true }
                    val publicUrl = SupabaseClient.client.storage["backups"].publicUrl(path)

                    val textPayload = if (isImage) "[IMG]$publicUrl[/IMG]" else "[FILE]$publicUrl[/FILE]"

                    withContext(Dispatchers.Main) {
                        onSend(textPayload, isAnonymous)
                        isUploading = false
                    }
                }
            } catch(e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Gagal upload: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    isUploading = false
                }
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadContent(uri, false)
    }
    
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadContent(uri, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(name = catatan.judul, size = 36.dp, profilePicUrl = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(catatan.judul, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Online", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (darkTheme) Color(0xFF1F3B73) else KelasinPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Column {
                    if (isAnonymous) {
                        Box(
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Mode Anonim Aktif: Nama disembunyikan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(onClick = { isAnonymous = !isAnonymous }, modifier = Modifier.padding(bottom = 4.dp)) {
                            Icon(if (isAnonymous) Icons.Filled.VisibilityOff else Icons.Filled.PersonOutline, null, tint = if (isAnonymous) KelasinPrimary else KelasinSubtext)
                        }

                        IconButton(
                            onClick = { fileLauncher.launch("*/*") },
                            modifier = Modifier.padding(bottom = 4.dp),
                            enabled = !isUploading
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = KelasinPrimary)
                            } else {
                                Icon(Icons.Filled.AttachFile, null, tint = KelasinSubtext)
                            }
                        }
                        
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                            placeholder = { Text("Ketik pesan...") },
                            maxLines = 5,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KelasinPrimary,
                                unfocusedBorderColor = if (darkTheme) {
                                    KelasinDivider
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                                },
                                focusedContainerColor = if (darkTheme) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (darkTheme) MaterialTheme.colorScheme.surface else Color.White,
                                disabledContainerColor = if (darkTheme) MaterialTheme.colorScheme.surface else Color.White
                            )
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = input.isEmpty(),
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it })
                        ) {
                            IconButton(
                                onClick = { imageLauncher.launch("image/*") },
                                enabled = !isUploading,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = KelasinPrimary
                                    )
                                } else {
                                    Icon(Icons.Filled.CameraAlt, null, tint = KelasinSubtext)
                                }
                            }
                        }

                        Spacer(Modifier.width(4.dp))
                        FilledIconButton(
                            onClick = {
                                val msg = input.trim()
                                if (msg.isNotEmpty() && !isUploading) {
                                    onSend(msg, isAnonymous)
                                    input = ""
                                }
                            },
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(48.dp),
                            enabled = !isUploading,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = KelasinPrimary, contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp).offset(x = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(if (darkTheme) Color(0xFF111827) else Color(0xFFF0F2F5))) {
            if (visibleMessages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada pesan", color = KelasinSubtext)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiItems) { item ->
                        when (item) {
                            is ChatUiItem.Header -> {
                                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Surface(
                                        color = if (darkTheme) Color(0xFF374151).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(8.dp),
                                        shadowElevation = 0.5.dp
                                    ) {
                                        Text(
                                            item.date,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = (if (darkTheme) Color.White else KelasinSubtext).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            is ChatUiItem.Message -> {
                                val msg = item.message
                                // IDENTITY FIX - CASE INSENSITIVE
                                val isMine = msg.senderUserId.equals(myUserId, ignoreCase = true)
                                val isAnonymousMsg = msg.senderName.equals("Anonymous", ignoreCase = true)
                                val isEdited = msg.text.endsWith("\u200E")
                                val displayText = if (isEdited) msg.text.removeSuffix("\u200E") else msg.text
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    if (!isMine) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    if (!isAnonymousMsg) {
                                                        showProfileForUserId = msg.senderUserId.trim().takeIf { it.isNotBlank() }
                                                    }
                                                }
                                        ) {
                                            InitialsAvatar(
                                                name = if (isAnonymousMsg) "Anonymous" else msg.senderName, 
                                                size = 32.dp,
                                                profilePicUrl = msg.senderProfilePic
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f, fill = false).pointerInput(msg.id) {
                                            detectTapGestures(onLongPress = { selectedMessageForAction = msg })
                                        },
                                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                                    ) {
                                        if (!isMine) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    if (isAnonymousMsg) "Anonymous" else msg.senderName, 
                                                    style = MaterialTheme.typography.labelSmall, 
                                                    color = KelasinSubtext, 
                                                    modifier = Modifier
                                                        .padding(start = 4.dp, bottom = 2.dp)
                                                        .clickable {
                                                            if (!isAnonymousMsg) {
                                                                showProfileForUserId = msg.senderUserId.trim().takeIf { it.isNotBlank() }
                                                            }
                                                        }
                                                )
                                                if (!isAnonymousMsg && msg.senderUserId == SharedCloudScope.USER_ID) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Verified, null, tint = KelasinPrimary, modifier = Modifier.size(12.dp).padding(bottom = 2.dp))
                                                }
                                            }
                                        }
                                        Surface(
                                            color = if (isMine) KelasinPrimary else if (darkTheme) Color(0xFF374151) else Color(0xFFF3F4F6),
                                            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp, topEnd = 16.dp,
                                                bottomStart = if (isMine) 16.dp else 4.dp,
                                                bottomEnd = if (isMine) 4.dp else 16.dp
                                            ),
                                            shadowElevation = 0.5.dp
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                if (displayText.startsWith("[IMG]") && displayText.endsWith("[/IMG]")) {
                                                    val url = displayText.substringAfter("[IMG]").substringBefore("[/IMG]")
                                                    coil.compose.AsyncImage(
                                                        model = url, contentDescription = "Attachment", 
                                                        modifier = Modifier
                                                            .sizeIn(maxWidth = 200.dp, maxHeight = 250.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { fullscreenImageUrl = url }, 
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                    )
                                                } else if (displayText.startsWith("[FILE]") && displayText.endsWith("[/FILE]")) {
                                                    val url = displayText.substringAfter("[FILE]").substringBefore("[/FILE]")
                                                    val rawFileName = url.substringAfterLast("/").substringBefore("?")
                                                    val displayName = if (rawFileName.contains("_")) rawFileName.substringAfter("_") else rawFileName
                                                    val ext = displayName.substringAfterLast(".", "").lowercase()
                                                    val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                                                     Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                                                         val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                                         val localFile = java.io.File(downloadsDir, displayName)
                                                         
                                                         if (localFile.exists()) {
                                                             try {
                                                                 val contentUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", localFile)
                                                                 val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                                     setDataAndType(contentUri, mimeType)
                                                                     addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                 }
                                                                 context.startActivity(android.content.Intent.createChooser(intent, "Buka $displayName"))
                                                             } catch (e: Exception) {
                                                                 android.widget.Toast.makeText(context, "Gagal membuka: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                             }
                                                         } else {
                                                             val request = android.app.DownloadManager.Request(android.net.Uri.parse(url)).apply {
                                                                 setTitle(displayName)
                                                                 setMimeType(mimeType)
                                                                 setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                                 setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, displayName)
                                                             }
                                                             (context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(request)
                                                             android.widget.Toast.makeText(context, "Mengunduh ${displayName}...", android.widget.Toast.LENGTH_SHORT).show()
                                                         }
                                                     }) {
                                                        Icon(Icons.Filled.Description, null, tint = if (isMine) Color.White else KelasinPrimary, modifier = Modifier.size(32.dp))
                                                        Spacer(Modifier.width(8.dp))
                                                        Column {
                                                            Text(displayName.take(30), fontWeight = FontWeight.Bold, color = if (isMine) Color.White else KelasinPrimary, style = MaterialTheme.typography.bodyMedium)
                                                            Text("Ketuk untuk buka/unduh", color = (if (isMine) Color.White else KelasinSubtext).copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                } else {
                                                    Text(displayText, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp))
                                                }
                                                Row(modifier = Modifier.align(Alignment.End).padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    if (isEdited) Text("(diedit) ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = (if (isMine) Color.White else KelasinSubtext).copy(alpha = 0.7f))
                                                    Text(timeFmt.format(Date(msg.createdAt)), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = (if (isMine) Color.White else KelasinSubtext).copy(alpha = 0.7f))
                                                    if (isMine) {
                                                        Spacer(Modifier.width(4.dp))
                                                        val statusColor = when (msg.status.uppercase(Locale.getDefault())) {
                                                            "SENT" -> Color(0xFF4CAF50)
                                                            "FAILED" -> Color(0xFFF44336)
                                                            else -> Color(0xFFFFC107)
                                                        }
                                                        Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Chat Actions Menu & Edit Dialog
        if (selectedMessageForAction != null) {
            val msg = selectedMessageForAction!!
            val isMineAction = msg.senderUserId == myUserId
            
            if (isEditingMsg) {
                AlertDialog(
                    onDismissRequest = { isEditingMsg = false; selectedMessageForAction = null },
                    title = { Text("Edit Pesan", fontWeight = FontWeight.SemiBold) },
                    text = { OutlinedTextField(value = editInput, onValueChange = { editInput = it }, modifier = Modifier.fillMaxWidth(), maxLines = 5, shape = RoundedCornerShape(12.dp)) },
                    confirmButton = { TextButton(onClick = { if (editInput.isNotBlank()) onEdit(msg, editInput); isEditingMsg = false; selectedMessageForAction = null }) { Text("Simpan") } },
                    dismissButton = { TextButton(onClick = { isEditingMsg = false }) { Text("Batal") } }
                )
            } else {
                ModalBottomSheet(onDismissRequest = { selectedMessageForAction = null }, containerColor = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(bottom = 24.dp)) {
                        Text("Aksi Obrolan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        if (isEditingMsg) {
                            ListItem(headlineContent = { Text("Batal Edit") }, leadingContent = { Icon(Icons.Filled.Cancel, null) }, modifier = Modifier.clickable { isEditingMsg = false; selectedMessageForAction = null }.padding(horizontal = 8.dp))
                        } else {
                            ListItem(headlineContent = { Text("Salin Teks") }, leadingContent = { Icon(Icons.Filled.ContentCopy, null) }, modifier = Modifier.clickable { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text.removeSuffix("\u200E"))); selectedMessageForAction = null }.padding(horizontal = 8.dp))
                            val isMediaMsg = msg.text.startsWith("[IMG]") || msg.text.startsWith("[FILE]")
                            if (isMineAction && !isMediaMsg) {
                                ListItem(headlineContent = { Text("Edit Pesan") }, leadingContent = { Icon(Icons.Filled.Edit, null) }, modifier = Modifier.clickable { editInput = msg.text.removeSuffix("\u200E"); isEditingMsg = true }.padding(horizontal = 8.dp))
                                ListItem(headlineContent = { Text("Hapus Pesan", color = KelasinError) }, leadingContent = { Icon(Icons.Filled.Delete, null, tint = KelasinError) }, modifier = Modifier.clickable { onDelete(msg, false); selectedMessageForAction = null }.padding(horizontal = 8.dp))
                            } else if (isMineAction && isMediaMsg) {
                                ListItem(headlineContent = { Text("Hapus Pesan", color = KelasinError) }, leadingContent = { Icon(Icons.Filled.Delete, null, tint = KelasinError) }, modifier = Modifier.clickable { onDelete(msg, false); selectedMessageForAction = null }.padding(horizontal = 8.dp))
                            } else if (userRole == "ATMIN") {
                                ListItem(headlineContent = { Text("Hapus Pesan", color = KelasinError) }, leadingContent = { Icon(Icons.Filled.Delete, null, tint = KelasinError) }, modifier = Modifier.clickable { onDelete(msg, true); selectedMessageForAction = null }.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    
    // Fullscreen image viewer
    if (fullscreenImageUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullscreenImageUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).clickable { fullscreenImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = fullscreenImageUrl,
                    contentDescription = "Gambar penuh",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val url = fullscreenImageUrl ?: return@IconButton
                            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(url)).apply {
                                setTitle(fileName)
                                setMimeType("image/jpeg")
                                setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                            }
                            (context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(request)
                            android.widget.Toast.makeText(context, "Mengunduh gambar...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Filled.Download, "Unduh", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { fullscreenImageUrl = null }) {
                        Icon(Icons.Filled.Close, "Tutup", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
    
    if (showProfileForUserId != null) {
        ModalBottomSheet(onDismissRequest = { showProfileForUserId = null }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoadingProfile) {
                    CircularProgressIndicator(color = KelasinPrimary, modifier = Modifier.padding(32.dp))
                } else if (profileUserData != null) {
                    val user = profileUserData!!
                    InitialsAvatar(name = user.nama, size = 100.dp, profilePicUrl = user.displayProfilePic)
                    Spacer(Modifier.height(16.dp))
                    Text(user.nama, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Bio", style = MaterialTheme.typography.labelMedium, color = KelasinPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(user.displayBio.ifBlank { "Belum ada bio." }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}
}

@Composable
fun CatatanCard(
    catatan: CatatanEntity,
    mk: MataKuliahEntity?,
    userRole: String,
    roomMessages: List<ChatMessageEntity>,
    allMessages: List<ChatMessageEntity> = emptyList(), // All messages for unread count
    currentUserId: String,
    sharedPrefs: android.content.SharedPreferences,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit,
    onToggleMute: () -> Unit = {}
) {
    val fmt = SimpleDateFormat("dd MMM", Locale("id"))

    // Determine accent color from matkul
    val accentColor = if (mk != null) {
        runCatching { Color(android.graphics.Color.parseColor(mk.warna)) }.getOrElse { KelasinPrimary }
    } else {
        KelasinSubtext.copy(alpha = 0.6f)
    }

    val labelText = when {
        mk != null -> mk.nama
        catatan.labelKustom.isNotBlank() -> catatan.labelKustom
        else -> "Catatan Umum"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { onToggleMute() }
            )
        }
    ) {
        // Colored top strip
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accentColor)
        )
        
        val lastReadTime = sharedPrefs.getLong("last_read_${catatan.id}", 0L)
        
        // Count unread messages (messages after last read time)
        val messagesCount = allMessages.count {
            it.createdAt > lastReadTime &&
                it.senderUserId != currentUserId &&
                isRealChatContent(it.text)
        }
        
        // If we have messages, use that count. 
        // If no messages synced yet but updatedAt > lastRead, we know there's at least 1 new thing.
        val hasUnread = (catatan.updatedAt > lastReadTime + 1000) && (allMessages.lastOrNull()?.senderUserId != currentUserId)
        val unreadCount = if (messagesCount > 0) messagesCount else if (hasUnread) 1 else 0

        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category badge
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        labelText.take(20),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))
                
                // Mute icon
                if (catatan.isMuted) {
                    Icon(
                        Icons.Filled.NotificationsOff,
                        contentDescription = "Senyap",
                        modifier = Modifier.size(16.dp),
                        tint = KelasinSubtext
                    )
                    Spacer(Modifier.width(6.dp))
                }
                
                // Unread message count badge
                if (unreadCount > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(KelasinError)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "$unreadCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                if (unreadCount == 0 && hasUnread) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(KelasinError))
                    Spacer(Modifier.width(6.dp))
                }
                Text(fmt.format(Date(catatan.updatedAt)), style = MaterialTheme.typography.bodySmall, color = if (hasUnread) KelasinError else KelasinSubtext, fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal)
                if (userRole == "ATMIN") {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, null, tint = KelasinSubtext, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = KelasinError, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                catatan.judul,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val preview = when {
                roomMessages.isNotEmpty() -> {
                    val last = roomMessages.maxByOrNull { it.createdAt }
                    if (last != null) "${last.senderName}: ${chatPreviewText(last.text)}" else ""
                }
                allMessages.isNotEmpty() -> {
                    val last = allMessages.maxByOrNull { it.createdAt }
                    if (last != null) "${last.senderName}: ${chatPreviewText(last.text)}" else ""
                }
                isLegacyChatPayload(catatan.isi) -> {
                    val last = decodeLegacyChatMessages(catatan).lastOrNull()
                    if (last != null) "${last.sender}: ${last.text}" else ""
                }
                catatan.isi.isNotBlank() -> catatan.isi
                else -> ""
            }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    preview.take(120) + if (preview.length > 120) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelasinSubtext
                )
            }
            if (catatan.tag.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    catatan.tag.split(",").take(3).forEach { tag ->
                        if (tag.isNotBlank()) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("#${tag.trim()}", style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                }
            }
            if (catatan.fileUri.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenFile,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buka Lampiran", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatanDialog(
    existing: CatatanEntity?,
    userId: String,
    mkList: List<MataKuliahEntity>,
    onDismiss: () -> Unit,
    onSave: (CatatanEntity) -> Unit
) {
    var judul by remember(existing) { mutableStateOf(existing?.judul ?: "") }
    var isi by remember(existing) { mutableStateOf(existing?.isi ?: "") }
    var tag by remember(existing) { mutableStateOf(existing?.tag ?: "") }
    var fileUri by remember(existing) { mutableStateOf(existing?.fileUri ?: "") }
    var labelKustom by remember(existing) { mutableStateOf(existing?.labelKustom ?: "") }

    // null = tidak pakai matkul (pakai label kustom)
    var selectedMk by remember(existing) {
        mutableStateOf(mkList.find { it.id == existing?.mataKuliahId })
    }
    var useKustom by remember(existing) { mutableStateOf(existing?.mataKuliahId == null && existing != null) }
    var expandedMk by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            fileUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Catatan" else "Edit Catatan", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(judul, { judul = it }, label = { Text("Judul") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(isi, { isi = it }, label = { Text("Isi Catatan") }, modifier = Modifier.fillMaxWidth(), maxLines = 5) }
                item { OutlinedTextField(tag, { tag = it }, label = { Text("Tag (pisah koma)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }

                // Kategori: matkul or kustom
                item {
                    Text("Kategori", style = MaterialTheme.typography.labelMedium, color = KelasinSubtext)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = useKustom,
                            onCheckedChange = { useKustom = it; if (it) selectedMk = null else labelKustom = "" },
                            colors = SwitchDefaults.colors(checkedThumbColor = KelasinPrimary, checkedTrackColor = KelasinPrimaryVar)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (useKustom) "Label Kustom" else "Mata Kuliah", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (useKustom) {
                    item {
                        OutlinedTextField(
                            value = labelKustom,
                            onValueChange = { labelKustom = it },
                            label = { Text("Label Kustom (e.g. Rapat, Acara...)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Label, null, tint = KelasinSubtext) }
                        )
                    }
                } else {
                    // Matkul dropdown (opsional — bisa juga pilih "Tanpa Matkul")
                    item {
                        ExposedDropdownMenuBox(
                            expanded = expandedMk,
                            onExpandedChange = { expandedMk = !expandedMk }
                        ) {
                            OutlinedTextField(
                                value = selectedMk?.nama ?: "— Tanpa Matkul —",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mata Kuliah (opsional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMk) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedMk, onDismissRequest = { expandedMk = false }) {
                                DropdownMenuItem(text = { Text("— Tanpa Matkul —") }, onClick = { selectedMk = null; expandedMk = false })
                                mkList.forEach { mk ->
                                    val mkColor = runCatching { Color(android.graphics.Color.parseColor(mk.warna)) }.getOrElse { KelasinPrimary }
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(mkColor))
                                                Spacer(Modifier.width(8.dp))
                                                Text(mk.nama)
                                            }
                                        },
                                        onClick = { selectedMk = mk; expandedMk = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // File attachment
                item {
                    Text("Lampiran File", style = MaterialTheme.typography.labelMedium, color = KelasinSubtext)
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { fileLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (fileUri.isBlank()) "Pilih File Lampiran" else "Ganti File Lampiran")
                    }
                    if (fileUri.isNotBlank()) {
                        Text("File terpilih", style = MaterialTheme.typography.labelSmall, color = StatusHadir, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary),
                onClick = {
                    if (judul.isNotBlank()) {
                        onSave(
                            CatatanEntity(
                                id = existing?.id ?: 0L,
                                mataKuliahId = selectedMk?.id,
                                labelKustom = if (useKustom) labelKustom else "",
                                judul = judul,
                                isi = isi,
                                tag = tag,
                                fileUri = fileUri,
                                userId = userId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

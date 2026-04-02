package com.kelasin.app.data.repository

import android.content.Context
import android.util.Log
import com.kelasin.app.data.KelasinDatabase
import com.kelasin.app.data.entity.ChatMessageEntity
import com.kelasin.app.data.supabase.SupabaseClient
import com.kelasin.app.data.supabase.SupabaseRestClient
import com.kelasin.app.data.supabase.toChatMessageEntity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ChatMessageInsertDto(
    @SerialName("room_id") val roomId: Long,
    @SerialName("sender_user_id") val senderUserId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("text") val text: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("status") val status: String,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("sender_profile_pic") val senderProfilePic: String? = null
)

class ChatRepository(private val context: Context) {
    private val tag = "ChatRepository"
    private val db = KelasinDatabase.getInstance(context)
    private val localDao = db.chatMessageDao()
    private val client = SupabaseClient.client
    private val scope = CoroutineScope(Dispatchers.IO)
    private val deliveryConfirmWindowMs = 2 * 60_000L
    private val recentDeliveryWindowMs = 10 * 60_000L
    private val duplicateMatchWindowMs = 7 * 24 * 60 * 60_000L
    private val realtimeJson = Json { ignoreUnknownKeys = true }
    
    // We keep track of subscribed channels to avoid double subscription
    private val subscribedRooms = mutableSetOf<Long>()

    init {
        // Ensure realtime connection is open
        scope.launch {
            runCatching {
                if (client.realtime.status.value != io.github.jan.supabase.realtime.Realtime.Status.CONNECTED) {
                    client.realtime.connect()
                }
            }.onFailure { Log.e(tag, "Failed to connect global realtime", it) }
        }
    }

    fun getMessages(roomId: Long): Flow<List<ChatMessageEntity>> =
        localDao.getMessagesByRoom(roomId).onStart { 
            subscribeToRoomIfNeeded(roomId)
            // Initial refresh from cloud to local
            scope.launch { refresh(roomId) }
        }

    fun getAllMessages(): Flow<List<ChatMessageEntity>> = localDao.getAllMessages()

    suspend fun sendMessage(
        roomId: Long,
        senderUserId: String,
        senderName: String,
        text: String,
        createdAt: Long = System.currentTimeMillis(),
        isAnonymous: Boolean = false
    ): Long {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return 0L
        
        val actualSenderName = if (isAnonymous) "Anonymous" else senderName.ifBlank { "User" }
        
        val userProfile = runCatching { KelasinDatabase.getInstance(context).userDao().findById(senderUserId) }.getOrNull()
        val avatar = userProfile?.profilePicUrl

        // 1. Save to local first as SENDING
        val localMsg = ChatMessageEntity(
            roomId = roomId,
            senderUserId = senderUserId,
            senderName = actualSenderName,
            text = trimmed,
            createdAt = createdAt,
            status = "SENDING",
            isEdited = false,
            senderProfilePic = avatar
        )
        val localId = localDao.insert(localMsg)
        val msgToUpsert = localMsg.copy(id = localId)

        // 2. Try to send to Cloud
        scope.launch {
            try {
                val insertPayload = ChatMessageInsertDto(
                    roomId = roomId,
                    senderUserId = senderUserId,
                    senderName = actualSenderName,
                    text = trimmed,
                    createdAt = createdAt,
                    status = "SENT",
                    isEdited = false,
                    senderProfilePic = avatar
                )
                Log.d(tag, "Attempting to insert chat message to cloud: $insertPayload")
                
                // KEY FIX: Use insert WITHOUT select() to avoid response decode errors.
                // If this line finishes without throwing, insert was 100% successful.
                client.postgrest["chat_messages"].insert(insertPayload)
                
                // Reconcile instead of marking SENT immediately so we get the REAL cloud ID.
                Log.d(tag, "Cloud insert OK for room=$roomId. Reconciling to get real ID.")
                val reconciled = reconcileMessageStatus(roomId, localId, senderUserId, trimmed, createdAt)
                if (!reconciled) {
                    // Fallback to SENT only if reconcile failed (very rare if insert just succeeded)
                    localDao.update(msgToUpsert.copy(status = "SENT"))
                }
                
                // Then refresh in background to ensure sync.
                runCatching { refresh(roomId) }
                    .onFailure { Log.w(tag, "Post-send refresh failed room=$roomId", it) }
                    
            } catch (e: Exception) {
                Log.e(tag, "sendMessage cloud FAILED for room=$roomId", e)
                if (e is io.github.jan.supabase.exceptions.RestException) {
                    Log.e(tag, "Supabase RestException: ${e.error} - ${e.description}")
                }
                
                // Give Realtime a brief chance to confirm delivery
                delay(4000)
                
                // Check if message actually made it to cloud despite the error
                val reconciled = reconcileMessageStatus(
                    roomId = roomId,
                    localMessageId = localId,
                    senderUserId = senderUserId,
                    text = trimmed,
                    createdAt = createdAt
                )
                
                if (!reconciled) {
                    Log.w(tag, "Message localId=$localId truly not in cloud. Marking FAILED.")
                    // Only mark FAILED if the message is still in pending state
                    val stillPending = localDao.getPendingMessages().any { it.id == localId }
                    if (stillPending) {
                        localDao.update(msgToUpsert.copy(status = "FAILED"))
                    }
                }
            }
        }
        
        return localId
    }

    suspend fun retryMessage(msg: ChatMessageEntity) {
        val updated = msg.copy(status = "SENDING")
        localDao.update(updated)
        
        runCatching {
            val insertPayload = ChatMessageInsertDto(
                roomId = msg.roomId,
                senderUserId = msg.senderUserId,
                senderName = msg.senderName,
                text = msg.text,
                createdAt = msg.createdAt,
                status = "SENT"
            )
            // KEY FIX: No select() → no decode errors. Mark SENT immediately on success.
            client.postgrest["chat_messages"].insert(insertPayload)
            localDao.update(msg.copy(status = "SENT"))
            runCatching { refresh(msg.roomId) }
        }.onFailure { throwable ->
            Log.e(tag, "retryMessage failed for id=${msg.id}", throwable)

            delay(2000)

            val reconciled = reconcileMessageStatus(
                roomId = msg.roomId,
                localMessageId = msg.id,
                senderUserId = msg.senderUserId,
                text = msg.text,
                createdAt = msg.createdAt
            )

            if (!reconciled) {
                runCatching { refresh(msg.roomId) }
                val stillPending = localDao.getPendingMessages().any { it.id == msg.id }
                if (stillPending) {
                    localDao.update(msg.copy(status = "FAILED"))
                }
            }
        }
    }

    suspend fun reconcileMessageStatus(
        roomId: Long,
        localMessageId: Long,
        senderUserId: String,
        text: String,
        createdAt: Long
    ): Boolean {
        val cloudMatch = runCatching {
            findDeliveredMessage(
                roomId = roomId,
                senderUserId = senderUserId,
                text = text,
                createdAt = createdAt
            )
        }.onFailure {
            Log.w(tag, "reconcileMessageStatus lookup failed for localId=$localMessageId", it)
        }.getOrNull() ?: return false

        localDao.deleteById(localMessageId)
        localDao.insert(cloudMatch.copy(status = "SENT"))
        return true
    }

    suspend fun updateMessage(msg: ChatMessageEntity, newText: String) {
        val updated = msg.copy(text = newText + "\u200E")
        localDao.update(updated)
        runCatching {
            client.postgrest["chat_messages"].update(mapOf("text" to updated.text)) {
                filter { eq("id", msg.id) }
            }
        }.onFailure { Log.e(tag, "updateMessage failed", it) }
    }

    suspend fun deleteMessage(msg: ChatMessageEntity, isAdmin: Boolean = false) {
        if (isAdmin) {
            val updated = msg.copy(text = "🚫 Pesan ini dihapus oleh Atmin")
            localDao.update(updated)
            runCatching {
                client.postgrest["chat_messages"].update(mapOf("text" to updated.text)) {
                    filter { eq("id", msg.id) }
                }
            }.onFailure { Log.e(tag, "Admin deleteMessage failed", it) }
        } else {
            localDao.deleteById(msg.id)
            runCatching {
                client.postgrest["chat_messages"].delete {
                    filter { eq("id", msg.id) }
                }
            }.onFailure { Log.e(tag, "deleteMessage failed", it) }
        }
    }

    suspend fun refresh(roomId: Long) {
        runCatching {
            val messages = fetchRoomMessagesWithFallback(roomId)
            val pending = localDao.getPendingMessages().filter { it.roomId == roomId }
            
            // Sync cloud messages to local
            messages.forEach { cloudMsg ->
                // AGGRESSIVE DEDUPLICATION: Delete any message that looks like this but is marked failed/sending
                pending.filter { 
                    isLikelySameDelivery(it, cloudMsg)
                }.forEach { 
                    Log.d(tag, "Refresh cleanup: deleting duplicate local msg id=${it.id}")
                    localDao.deleteById(it.id) 
                }
                localDao.insert(cloudMsg.copy(status = "SENT"))
            }
        }.onFailure { Log.e(tag, "refresh failed room=$roomId", it) }
    }

    private suspend fun findDeliveredMessage(
        roomId: Long,
        senderUserId: String,
        text: String,
        createdAt: Long
    ): ChatMessageEntity? {
        val cloudMessages = fetchRoomMessagesWithFallback(roomId)
        val targetCreatedAt = normalizeEpochMillis(createdAt)
        val now = System.currentTimeMillis()

        val candidates = cloudMessages
            .asSequence()
            .filter { it.senderUserId == senderUserId }
            .filter { it.text.trim() == text.trim() }
            .toList()

        val closeByTimestamp = candidates
            .filter { abs(normalizeEpochMillis(it.createdAt) - targetCreatedAt) <= deliveryConfirmWindowMs }
            .minByOrNull { abs(normalizeEpochMillis(it.createdAt) - targetCreatedAt) }

        if (closeByTimestamp != null) return closeByTimestamp

        return candidates
            .filter { abs(normalizeEpochMillis(it.createdAt) - now) <= recentDeliveryWindowMs }
            .minByOrNull { abs(normalizeEpochMillis(it.createdAt) - now) }
    }

    private suspend fun fetchRoomMessagesWithFallback(roomId: Long): List<ChatMessageEntity> {
        val typedResult = runCatching {
            client.postgrest["chat_messages"]
                .select {
                    filter { eq("room_id", roomId) }
                }
                .decodeList<ChatMessageEntity>()
        }

        if (typedResult.isSuccess) {
            return typedResult.getOrThrow()
        }

        Log.w(tag, "Typed decode failed for room=$roomId, falling back to raw mapper", typedResult.exceptionOrNull())

        return SupabaseRestClient.selectRows(
            table = "chat_messages",
            filters = listOf("room_id" to "eq.$roomId"),
            order = "created_at.asc"
        ).map { row ->
            row.toChatMessageEntity()
        }
    }

    private fun isLikelySameDelivery(localPending: ChatMessageEntity, cloudMsg: ChatMessageEntity): Boolean {
        // Case-insensitive ID check for extra safety
        if (!localPending.senderUserId.equals(cloudMsg.senderUserId, ignoreCase = true)) return false
        
        // Normalize text (trim, remove invisible markers if any)
        val t1 = localPending.text.replace("\u200E", "").trim()
        val t2 = cloudMsg.text.replace("\u200E", "").trim()
        if (t1 != t2) return false
        
        val localCreatedAt = normalizeEpochMillis(localPending.createdAt)
        val cloudCreatedAt = normalizeEpochMillis(cloudMsg.createdAt)
        
        // Huge window (7 days) for refresh, but logically it should be just a few minutes
        return abs(localCreatedAt - cloudCreatedAt) <= duplicateMatchWindowMs
    }

    private fun normalizeEpochMillis(raw: Long): Long {
        if (raw <= 0L) return raw
        return when {
            raw < 10_000_000_000L -> raw * 1000L       // seconds -> millis
            raw > 10_000_000_000_000L -> raw / 1000L   // micros -> millis
            else -> raw
        }
    }

    private fun subscribeToRoomIfNeeded(roomId: Long) {
        synchronized(subscribedRooms) {
            if (subscribedRooms.contains(roomId)) return
            subscribedRooms.add(roomId)
        }

        scope.launch {
            runCatching {
                val channel = client.realtime.channel("room-$roomId")
                val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "chat_messages"
                }
                flow.onEach { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            val newMsg = runCatching {
                                realtimeJson.decodeFromJsonElement<ChatMessageEntity>(action.record)
                            }.onFailure {
                                Log.w(tag, "Realtime insert decode failed room=$roomId", it)
                            }.getOrNull() ?: return@onEach

                            if (newMsg.roomId == roomId) {
                                Log.d(tag, "Realtime Insert: ${newMsg.text} from ${newMsg.senderName}")
                                // AGGRESSIVE DEDUPLICATION: Delete any message that looks like this but is marked failed/sending
                                val pending = localDao.getPendingMessages().filter { it.roomId == roomId }
                                pending.filter { 
                                    isLikelySameDelivery(it, newMsg)
                                }.forEach { 
                                    Log.d(tag, "Cleaning up duplicate id=${it.id}")
                                    localDao.deleteById(it.id) 
                                }
                                localDao.insert(newMsg.copy(status = "SENT"))
                            }
                        }
                        is PostgresAction.Update -> {
                            val updatedMsg = runCatching {
                                realtimeJson.decodeFromJsonElement<ChatMessageEntity>(action.record)
                            }.onFailure {
                                Log.w(tag, "Realtime update decode failed room=$roomId", it)
                            }.getOrNull() ?: return@onEach

                            if (updatedMsg.roomId == roomId) localDao.insert(updatedMsg.copy(status = "SENT"))
                        }
                        is PostgresAction.Delete -> {
                            val oldId = action.oldRecord["id"]?.let { 
                                if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toLongOrNull() else null
                            }
                            if (oldId != null) {
                                localDao.deleteById(oldId)
                            }
                        }
                        else -> {}
                    }
                }.launchIn(scope)
                channel.subscribe()
                Log.d(tag, "Subscribed to realtime changes for room_id=$roomId")
            }.onFailure {
                Log.e(tag, "Failed to subscribe to room=$roomId", it)
                synchronized(subscribedRooms) { subscribedRooms.remove(roomId) }
            }
        }
    }
}

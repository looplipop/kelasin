package com.kelasin.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun InitialsAvatar(name: String, size: Dp = 40.dp, profilePicUrl: String? = null) {
    if (!profilePicUrl.isNullOrBlank()) {
        val bitmap = remember(profilePicUrl) {
            runCatching {
                val pureBase64 = if (profilePicUrl.contains(",")) profilePicUrl.substringAfter(",") else profilePicUrl
                val bytes = Base64.decode(pureBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Profile Avatar",
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            return
        }
    }
    
    val initials = remember(name) {
        val n = name.replace(Regex("[^A-Za-z0-9 ]"), "").trim()
        val parts = n.split(" ").filter { it.isNotBlank() }
        when (parts.size) {
            0 -> "?"
            1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }
    val bgColor = remember(name) {
        val colors = listOf(
            Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), 
            Color(0xFF9575CD), Color(0xFF7986CB), Color(0xFF64B5F6), 
            Color(0xFF4FC3F7), Color(0xFF4DD0E1), Color(0xFF4DB6AC), 
            Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFF8A65)
        )
        colors[abs(name.hashCode() % colors.size)]
    }
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4).sp)
    }
}

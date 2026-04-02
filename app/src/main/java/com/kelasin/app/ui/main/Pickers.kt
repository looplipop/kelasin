package com.kelasin.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelasin.app.ui.theme.KelasinPrimary

/**
 * Material3 Time Picker wrapped in a dialog.
 * initial: "HH:mm" string
 * onConfirm: (hour, minute)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelasinTimePicker(
    initial: String = "08:00",
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val parts = initial.split(":")
    val initH = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initM = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val state = rememberTimePickerState(initialHour = initH, initialMinute = initM, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Waktu", fontWeight = FontWeight.SemiBold) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary),
                   onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

/**
 * Material3 DatePicker wrapped in a dialog.
 * onConfirm: selected millis timestamp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelasinDatePicker(
    initialMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (millis: Long) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = KelasinPrimary), onClick = {
                state.selectedDateMillis?.let { onConfirm(it) }
            }) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    ) {
        DatePicker(state = state, showModeToggle = true)
    }
}

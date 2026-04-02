package com.kelasin.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.kelasin.app.R
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val judul = inputData.getString("judul") ?: "Tugas"
        val matkul = inputData.getString("matkul") ?: "Mata Kuliah"
        val message = "Waktu tersisa 1 JAM untuk mengumpulkan tugas: $judul ($matkul)!"

        showNotification(judul, message)
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "kelasin_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Kelasin Reminders", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan Deadline: $title")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            }
        } else {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
        }
    }

    companion object {
        fun scheduleReminder(context: Context, tugasId: Long, judul: String, matkul: String, deadlineMillis: Long) {
            val oneHourMillis = 3600_000L
            val delay = deadlineMillis - System.currentTimeMillis() - oneHourMillis

            if (delay > 0) {
                val inputData = Data.Builder()
                    .putString("judul", judul)
                    .putString("matkul", matkul)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("tugas_$tugasId")
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "reminder_$tugasId",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }

        fun cancelReminder(context: Context, tugasId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$tugasId")
        }
    }
}

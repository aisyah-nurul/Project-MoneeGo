package com.example.appmoneego.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.appmoneego.MainActivity
import com.example.appmoneego.R

object NotificationHelper {

    const val CHANNEL_ID   = "moneego_reminder_channel"
    const val CHANNEL_NAME = "MoneeGo Reminder"
    const val NOTIF_ID     = 1001

    // ── Buat notification channel (wajib untuk Android 8+) ───────────────────
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat harian MoneeGo"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // ── Tampilkan notifikasi ──────────────────────────────────────────────────
    fun showNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("MoneeGo Reminder")
            .setContentText("Jangan lupa catat pengeluaranmu hari ini! 💰")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // ── Cek permission sebelum notify (wajib untuk Android 13+) ──────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
            }
            // Kalau tidak ada permission, diam saja — tidak crash
        } else {
            // Android 12 ke bawah langsung notify
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        }
    }
}
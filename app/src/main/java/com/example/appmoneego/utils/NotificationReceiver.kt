package com.example.appmoneego.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.appmoneego.utils.NotificationHelper

// ── BroadcastReceiver yang dipanggil AlarmManager setiap hari ────────────────
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Pastikan channel sudah ada sebelum tampilkan notifikasi
        NotificationHelper.createChannel(context)
        NotificationHelper.showNotification(context)
    }
}
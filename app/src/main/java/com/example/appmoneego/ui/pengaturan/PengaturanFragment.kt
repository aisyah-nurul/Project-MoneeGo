package com.example.appmoneego.ui.pengaturan

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.appmoneego.R
import java.util.Locale

class PengaturanFragment : Fragment() {

    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pengaturan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("moneego_prefs", Context.MODE_PRIVATE)

        // ── Bahasa ─────────────────────────────────────────────────────────────
        val tvBahasaValue = view.findViewById<TextView>(R.id.tv_bahasa_value)
        val savedLang = prefs.getString("language", "id") ?: "id"
        tvBahasaValue.text = if (savedLang == "en") "English" else "Bahasa Indonesia"

        view.findViewById<LinearLayout>(R.id.ll_bahasa).setOnClickListener {
            tampilkanDialogBahasa()
        }

        // ── Ekspor ─────────────────────────────────────────────────────────────
        view.findViewById<LinearLayout>(R.id.btn_ekspor)?.setOnClickListener {
            // TODO: implementasi ekspor CSV
        }

        // ── Dark Mode ──────────────────────────────────────────────────────────
        val switchDarkMode = view.findViewById<Switch>(R.id.switch_dark_mode)
        val isDark = prefs.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDark

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate() // ← WAJIB TAMBAH INI
        }

        // ── Notifikasi ─────────────────────────────────────────────────────────
        val switchNotifikasi = view.findViewById<Switch>(R.id.switch_notifikasi)
        val isNotifOn = prefs.getBoolean("notifikasi", true)
        switchNotifikasi.isChecked = isNotifOn

        switchNotifikasi.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifikasi", isChecked).apply()
            if (isChecked) aktifkanNotifikasi() else nonaktifkanNotifikasi()
        }
    }

    // ── Notifikasi helpers ─────────────────────────────────────────────────────
    private fun aktifkanNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "moneego_channel",
                "Pengingat MoneeGo",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifikasi pengingat keuangan MoneeGo" }
            val manager = requireContext()
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun nonaktifkanNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = requireContext()
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel("moneego_channel")
        }
    }

    // ── Bahasa ─────────────────────────────────────────────────────────────────
    private fun tampilkanDialogBahasa() {
        val bahasa = arrayOf("Bahasa Indonesia", "English")
        val savedLang = prefs.getString("language", "id") ?: "id"
        val selectedIndex = if (savedLang == "en") 1 else 0

        AlertDialog.Builder(requireContext())
            .setTitle(if (savedLang == "en") "Select Language" else "Pilih Bahasa")
            .setSingleChoiceItems(bahasa, selectedIndex) { dialog, which ->
                val langCode = if (which == 1) "en" else "id"
                if (langCode != savedLang) simpanDanApplyBahasa(langCode)
                dialog.dismiss()
            }
            .setNegativeButton(if (savedLang == "en") "Cancel" else "Batal", null)
            .show()
    }

    private fun simpanDanApplyBahasa(langCode: String) {
        prefs.edit().putString("language", langCode).apply()
        Locale.setDefault(Locale(langCode))
        requireActivity().recreate()
    }
}
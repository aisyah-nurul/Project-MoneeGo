package com.example.appmoneego.ui.pengaturan

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.appmoneego.R
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.utils.AlarmScheduler
import com.example.appmoneego.utils.ExportUtils
import com.example.appmoneego.utils.NotificationHelper
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.TransaksiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PengaturanFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var transaksiViewModel: TransaksiViewModel
    private lateinit var dompetViewModel: DompetViewModel

    // ── Permission launcher untuk ekspor (Android < 10) ──────────────────────
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) mulaiEkspor()
        else Toast.makeText(
            requireContext(),
            "Izin storage diperlukan untuk ekspor",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ── Permission launcher untuk notifikasi (Android 13+) ───────────────────
    private val requestNotifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tampilkanTimePickerDialog()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.notif_permission_ditolak),
                Toast.LENGTH_SHORT
            ).show()
            view?.findViewById<Switch>(R.id.switch_notifikasi)?.isChecked = false
            prefs.edit().putBoolean("notifikasi", false).apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_pengaturan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("moneego_prefs", Context.MODE_PRIVATE)
        transaksiViewModel = ViewModelProvider(this)[TransaksiViewModel::class.java]
        dompetViewModel    = ViewModelProvider(this)[DompetViewModel::class.java]

        // ── Buat notification channel saat fragment dibuka ────────────────────
        NotificationHelper.createChannel(requireContext())

        // ── Bahasa ────────────────────────────────────────────────────────────
        val tvBahasaValue = view.findViewById<TextView>(R.id.tv_bahasa_value)
        val savedLang = prefs.getString("language", "id") ?: "id"
        tvBahasaValue.text = if (savedLang == "en") "English" else "Bahasa Indonesia"
        view.findViewById<LinearLayout>(R.id.ll_bahasa).setOnClickListener {
            tampilkanDialogBahasa()
        }

        // ── Ekspor ────────────────────────────────────────────────────────────
        view.findViewById<LinearLayout>(R.id.btn_ekspor)?.setOnClickListener {
            cekDanEkspor()
        }

        // ── Dark Mode ─────────────────────────────────────────────────────────
        val switchDarkMode = view.findViewById<Switch>(R.id.switch_dark_mode)
        val isDark = prefs.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDark
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate()
        }

        // ── Notifikasi ────────────────────────────────────────────────────────
        val switchNotifikasi  = view.findViewById<Switch>(R.id.switch_notifikasi)
        val llWaktuNotifikasi = view.findViewById<LinearLayout>(R.id.ll_waktu_notifikasi)
        val tvWaktuNotifikasi = view.findViewById<TextView>(R.id.tv_waktu_notifikasi)
        val tvUbahWaktu       = view.findViewById<TextView>(R.id.tv_ubah_waktu)

        val isNotifOn   = prefs.getBoolean("notifikasi", false)
        val savedHour   = prefs.getInt("notif_hour", 8)
        val savedMinute = prefs.getInt("notif_minute", 0)

        switchNotifikasi.isChecked         = isNotifOn
        tvWaktuNotifikasi.text             = String.format(Locale.getDefault(), "%02d:%02d", savedHour, savedMinute)
        llWaktuNotifikasi.visibility       = if (isNotifOn) View.VISIBLE else View.GONE

        switchNotifikasi.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifikasi", isChecked).apply()
            if (isChecked) {
                cekDanAktifkanNotifikasi()
                llWaktuNotifikasi.visibility = View.VISIBLE
            } else {
                AlarmScheduler.cancel(requireContext())
                llWaktuNotifikasi.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notif_dimatikan),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Tombol ubah waktu
        tvUbahWaktu.setOnClickListener       { tampilkanTimePickerDialog() }
        llWaktuNotifikasi.setOnClickListener  { tampilkanTimePickerDialog() }

    } // ← TUTUP onViewCreated

    // ── Cek permission notifikasi (Android 13+) ───────────────────────────────
    private fun cekDanAktifkanNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                tampilkanTimePickerDialog()
            } else {
                requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            tampilkanTimePickerDialog()
        }
    }

    // ── TimePicker ────────────────────────────────────────────────────────────
    private fun tampilkanTimePickerDialog() {
        val savedHour   = prefs.getInt("notif_hour", 8)
        val savedMinute = prefs.getInt("notif_minute", 0)

        val picker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
            .setHour(savedHour)
            .setMinute(savedMinute)
            .setTitleText(getString(R.string.pilih_waktu_notifikasi))
            .setTheme(R.style.MoneeGoTimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            val hour   = picker.hour
            val minute = picker.minute

            prefs.edit()
                .putInt("notif_hour", hour)
                .putInt("notif_minute", minute)
                .apply()

            AlarmScheduler.scheduleDaily(requireContext(), hour, minute)

            val jamFormatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            view?.findViewById<TextView>(R.id.tv_waktu_notifikasi)?.text = jamFormatted

            Toast.makeText(
                requireContext(),
                getString(R.string.notif_diaktifkan, jamFormatted),
                Toast.LENGTH_SHORT
            ).show()
        }

        picker.show(parentFragmentManager, "MoneeGoTimePicker")
    }

    // ── Ekspor CSV ────────────────────────────────────────────────────────────
    private fun cekDanEkspor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { mulaiEkspor(); return }
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mulaiEkspor()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun mulaiEkspor() {
        lifecycleScope.launch {
            try {
                val db            = MoneeGoDatabase.getDatabase(requireContext())
                val transaksiList = withContext(Dispatchers.IO) { db.transaksiDao().getAllTransaksiOnce() }
                val dompetList    = withContext(Dispatchers.IO) { db.dompetDao().getAllDompetOnce() }
                val namaFile      = withContext(Dispatchers.IO) {
                    ExportUtils.exportTransaksiToCsv(requireContext(), transaksiList, dompetList)
                }

                if (namaFile != null) {
                    Toast.makeText(requireContext(), "✅ Berhasil disimpan: Download/$namaFile",
                        Toast.LENGTH_SHORT).show()

                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val projection = arrayOf(android.provider.MediaStore.Downloads._ID)
                        val selection  = "${android.provider.MediaStore.Downloads.DISPLAY_NAME} = ?"
                        val cursor = requireContext().contentResolver.query(
                            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            projection, selection, arrayOf(namaFile), null
                        )
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val id = it.getLong(
                                    it.getColumnIndexOrThrow(android.provider.MediaStore.Downloads._ID))
                                android.content.ContentUris.withAppendedId(
                                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                            } else null
                        }
                    } else {
                        val file = java.io.File(
                            android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS), namaFile)
                        androidx.core.content.FileProvider.getUriForFile(
                            requireContext(), "${requireContext().packageName}.provider", file)
                    }

                    if (uri != null) {
                        val sheetsIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "text/csv")
                            setPackage("com.google.android.apps.docs.editors.sheets")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        if (sheetsIntent.resolveActivity(requireContext().packageManager) != null) {
                            startActivity(sheetsIntent)
                        } else {
                            val fallback = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(android.content.Intent.createChooser(fallback, "Bagikan ke"))
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "❌ Ekspor gagal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Bahasa ────────────────────────────────────────────────────────────────
    private fun tampilkanDialogBahasa() {
        val bahasa        = arrayOf("Bahasa Indonesia", "English")
        val savedLang     = prefs.getString("language", "id") ?: "id"
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
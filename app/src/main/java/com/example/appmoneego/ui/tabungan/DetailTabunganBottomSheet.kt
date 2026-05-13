package com.example.appmoneego.ui.tabungan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Tabungan
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetailTabunganBottomSheet(
    private var tabungan: Tabungan,
    private val onUpdated: (Tabungan) -> Unit,
    private val onDeleted: (Tabungan) -> Unit
) : BottomSheetDialogFragment() {

    private var nominalAngka = 0.0
    private var dompetDipilih: Dompet? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_detail_tabungan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNama       = view.findViewById<TextView>(R.id.tvDetailNama)
        val tvHariIni    = view.findViewById<TextView>(R.id.tvDetailHariIni)
        val tvTerkumpul  = view.findViewById<TextView>(R.id.tvDetailTerkumpul)
        val btnTutup     = view.findViewById<ImageButton>(R.id.btnTutupDetail)
        val btnEdit      = view.findViewById<ImageButton>(R.id.btnEditTabungan)
        val btnHapus     = view.findViewById<ImageButton>(R.id.btnHapusTabungan)
        val etNominal    = view.findViewById<EditText>(R.id.etNominalTabung)
        val btnPilihDompet = view.findViewById<Button>(R.id.btnPilihDompet)
        val btnKonfirmasi  = view.findViewById<Button>(R.id.btnKonfirmasiTabung)
        val switchPrioritas = view.findViewById<Switch>(R.id.switchPrioritas)

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        // Isi info
        tvNama.text      = tabungan.nama
        tvHariIni.text   = "Hari ini: ${sdf.format(Date())}"
        tvTerkumpul.text = formatRupiah(tabungan.terkumpul)

        btnTutup.setOnClickListener { dismiss() }

        // Format nominal saat ketik
        etNominal.addTextChangedListener(object : TextWatcher {
            var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val raw = s.toString().replace(Regex("[^0-9]"), "")
                nominalAngka = raw.toDoubleOrNull() ?: 0.0
                val formatted = if (nominalAngka > 0)
                    NumberFormat.getNumberInstance(Locale("id", "ID")).format(nominalAngka)
                else ""
                etNominal.setText(formatted)
                etNominal.setSelection(formatted.length)
                isEditing = false
            }
        })

        // Pilih dompet
        btnPilihDompet.setOnClickListener {
            lifecycleScope.launch {
                val dompetList = withContext(Dispatchers.IO) {
                    MoneeGoDatabase.getDatabase(requireContext()).dompetDao()
                        .getAllDompet().value ?: emptyList()
                }
                if (dompetList.isEmpty()) {
                    Toast.makeText(requireContext(), "Belum ada dompet", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val namaList = dompetList.map { "${it.nama} (${formatRupiah(it.saldo)})" }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Dompet")
                    .setItems(namaList) { _, i ->
                        dompetDipilih = dompetList[i]
                        btnPilihDompet.text = dompetList[i].nama
                    }
                    .show()
            }
        }

        // Konfirmasi tabung
        btnKonfirmasi.setOnClickListener {
            if (nominalAngka <= 0) {
                etNominal.error = "Masukkan nominal"
                return@setOnClickListener
            }
            val dp = dompetDipilih
            if (dp == null) {
                Toast.makeText(requireContext(), "Pilih dompet dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dp.saldo < nominalAngka) {
                Toast.makeText(requireContext(), "Saldo ${dp.nama} tidak cukup", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = MoneeGoDatabase.getDatabase(requireContext())
                withContext(Dispatchers.IO) {
                    val newTerkumpul = (tabungan.terkumpul + nominalAngka)
                        .coerceAtMost(tabungan.targetNominal)
                    val updated = tabungan.copy(terkumpul = newTerkumpul)
                    db.tabunganDao().update(updated)
                    db.dompetDao().kurangiSaldo(dp.id, nominalAngka)
                    tabungan = updated
                }
                onUpdated(tabungan)
                tvTerkumpul.text = formatRupiah(tabungan.terkumpul)
                Toast.makeText(requireContext(), "Berhasil menabung!", Toast.LENGTH_SHORT).show()
                if (tabungan.terkumpul >= tabungan.targetNominal) {
                    Toast.makeText(requireContext(), "🎉 Target ${tabungan.nama} tercapai!", Toast.LENGTH_LONG).show()
                }
                dismiss()
            }
        }

        // Hapus tabungan
        btnHapus.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Target?")
                .setMessage("Target \"${tabungan.nama}\" akan dihapus permanen.")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            MoneeGoDatabase.getDatabase(requireContext())
                                .tabunganDao().delete(tabungan)
                        }
                        onDeleted(tabungan)
                        dismiss()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Edit (buka dialog edit - untuk sekarang Toast dulu)
        btnEdit.setOnClickListener {
            Toast.makeText(requireContext(), "Edit segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatRupiah(value: Double): String =
        "Rp${String.format("%,.0f", value).replace(",", ".")}"
}
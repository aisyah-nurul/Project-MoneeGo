package com.example.appmoneego.ui.tabungan

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Tabungan
import com.example.appmoneego.utils.CurrencyFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetailTabunganBottomSheet(
    private val tabungan: Tabungan,
    private val onUpdated: (Tabungan) -> Unit,
    private val onDeleted: (Tabungan) -> Unit
) : BottomSheetDialogFragment() {

    private var jumlahAngka: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_detail_tabungan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNama        = view.findViewById<TextView>(R.id.tvDetailNama)
        val tvHariIni     = view.findViewById<TextView>(R.id.tvDetailHariIni)
        val tvTerkumpul   = view.findViewById<TextView>(R.id.tvDetailTerkumpul)
        val etNominal     = view.findViewById<EditText>(R.id.etNominalTabung)
        val btnPilihDompet= view.findViewById<Button>(R.id.btnPilihDompet)
        val btnKonfirmasi = view.findViewById<Button>(R.id.btnKonfirmasiTabung)
        // ImageView bukan ImageButton (sesuai XML yang sudah diupdate)
        val btnEdit       = view.findViewById<ImageView>(R.id.btnEditTabungan)
        val btnHapus      = view.findViewById<ImageView>(R.id.btnHapusTabungan)
        val btnTutup      = view.findViewById<ImageView>(R.id.btnTutupDetail)
        val switchPrioritas = view.findViewById<Switch>(R.id.switchPrioritas)

        tvNama.text      = tabungan.nama
        tvTerkumpul.text = CurrencyFormatter.format(tabungan.terkumpul)

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id"))
        tvHariIni.text = "Hari ini: ${sdf.format(Date())}"

        // Format input nominal
        etNominal.addTextChangedListener(object : TextWatcher {
            var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val raw = s.toString().replace(Regex("[^0-9]"), "")
                jumlahAngka = raw.toLongOrNull() ?: 0L
                val formatted = if (jumlahAngka > 0)
                    NumberFormat.getNumberInstance(Locale("id", "ID")).format(jumlahAngka)
                else ""
                etNominal.setText(formatted)
                etNominal.setSelection(formatted.length)
                isEditing = false
            }
        })

        btnTutup.setOnClickListener { dismiss() }

        btnPilihDompet.setOnClickListener {
            Toast.makeText(requireContext(), "Pilih dompet (coming soon)", Toast.LENGTH_SHORT).show()
        }

        btnKonfirmasi.setOnClickListener {
            if (jumlahAngka <= 0) {
                etNominal.error = "Masukkan jumlah tabungan"
                return@setOnClickListener
            }
            val updated = tabungan.copy(
                terkumpul = tabungan.terkumpul + jumlahAngka.toDouble()
            )
            onUpdated(updated)
            Toast.makeText(
                requireContext(),
                "Berhasil menabung ${CurrencyFormatter.format(jumlahAngka.toDouble())}!",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }

        // Fix: TambahTabunganDialog hanya punya onSimpan, bukan tabungan
        btnEdit.setOnClickListener {
            dismiss()
            TambahTabunganDialog { updated ->
                onUpdated(updated)
            }.show(parentFragmentManager, "EditTabungan")
        }

        btnHapus.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Target")
                .setMessage("Yakin ingin menghapus target '${tabungan.nama}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    onDeleted(tabungan)
                    Toast.makeText(requireContext(), "Target dihapus", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        switchPrioritas.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "'${tabungan.nama}' dijadikan prioritas", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
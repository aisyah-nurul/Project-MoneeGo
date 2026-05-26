package com.example.appmoneego.ui.tabungan

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Tabungan
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TambahTabunganDialog(
    private val onSimpan: (Tabungan) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).also {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            it.behavior.skipCollapsed = true
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_tambah_tabungan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNama     = view.findViewById<TextInputEditText>(R.id.etNamaTabungan)
        val etTarget   = view.findViewById<TextInputEditText>(R.id.etTargetNominal)
        val etDeadline = view.findViewById<TextInputEditText>(R.id.etDeadlineTabungan)
        val btnSimpan  = view.findViewById<Button>(R.id.btnSimpanTabungan)

        var targetAngka   = 0.0
        var deadlineMs: Long? = null
        var kategoriDipilih = "Lainnya"
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        // Mapping kategori
        val kategoriMap = mapOf(
            view.findViewById<LinearLayout>(R.id.btnKategoriElektronik) to "Elektronik",
            view.findViewById<LinearLayout>(R.id.btnKategoriKendaraan)  to "Kendaraan",
            view.findViewById<LinearLayout>(R.id.btnKategoriLiburan)    to "Liburan",
            view.findViewById<LinearLayout>(R.id.btnKategoriRumah)      to "Rumah",
            view.findViewById<LinearLayout>(R.id.btnKategoriPendidikan) to "Pendidikan",
            view.findViewById<LinearLayout>(R.id.btnKategoriLainnya)    to "Lainnya"
        )

        // Highlight kategori terpilih
        fun highlight(selected: LinearLayout) {
            kategoriMap.keys.forEach { btn ->
                btn.alpha = if (btn == selected) 1f else 0.5f
            }
        }

        // Default: Lainnya
        highlight(view.findViewById(R.id.btnKategoriLainnya))
        kategoriMap.forEach { (btn, kategori) ->
            btn.setOnClickListener {
                kategoriDipilih = kategori
                highlight(btn)
            }
        }

        // Format nominal otomatis Rupiah
        etTarget.addTextChangedListener(object : TextWatcher {
            var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val raw = s.toString().replace(Regex("[^0-9]"), "")
                targetAngka = raw.toDoubleOrNull() ?: 0.0
                val formatted = if (targetAngka > 0)
                    NumberFormat.getNumberInstance(Locale("id", "ID")).format(targetAngka)
                else ""
                etTarget.setText(formatted)
                etTarget.setSelection(formatted.length)
                isEditing = false
            }
        })

        // Date picker deadline
        etDeadline.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                deadlineMs = cal.timeInMillis
                etDeadline.setText(sdf.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Simpan
        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            if (nama.isEmpty()) {
                etNama.error = "Nama target wajib diisi"
                return@setOnClickListener
            }
            if (targetAngka <= 0) {
                etTarget.error = "Target nominal wajib diisi"
                return@setOnClickListener
            }
            onSimpan(
                Tabungan(
                    nama          = nama,
                    targetNominal = targetAngka,
                    terkumpul     = 0.0,
                    deadline      = deadlineMs
                )
            )
            dismiss()
        }
    }
}
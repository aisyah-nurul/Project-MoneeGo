package com.example.appmoneego.ui.hutang

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.appmoneego.R
import com.example.appmoneego.databinding.DialogTambahHutangBinding
import com.example.appmoneego.model.Hutang
import com.example.appmoneego.model.JenisHutang
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TambahHutangDialog(
    private val onSimpan: (Hutang) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogTambahHutangBinding? = null
    private val binding get() = _binding!!

    private var jenisHutangDipilih: JenisHutang = JenisHutang.KARTU_KREDIT
    private var tanggalDipilih: Date? = null
    private var jumlahAngka: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTambahHutangBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupJenisHutang()
        setupJumlahInput()
        setupDatePicker()
        setupSimpan()
    }

    private fun setupJenisHutang() {
        val buttons = mapOf(
            binding.btnKartuKredit      to JenisHutang.KARTU_KREDIT,
            binding.btnPinjol           to JenisHutang.PINJOL,
            binding.btnCicilan          to JenisHutang.CICILAN,
            binding.btnPinjamanBank     to JenisHutang.PINJAMAN_BANK,
            binding.btnPinjamKeKerabat  to JenisHutang.PINJAM_KE_KERABAT,
            binding.btnLainnya          to JenisHutang.LAINNYA
        )
        updateSelectedJenis(JenisHutang.KARTU_KREDIT, buttons)
        buttons.forEach { (tv, jenis) ->
            tv.setOnClickListener {
                jenisHutangDipilih = jenis
                updateSelectedJenis(jenis, buttons)
                binding.layoutLimitKredit.visibility =
                    if (jenis == JenisHutang.KARTU_KREDIT) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateSelectedJenis(selected: JenisHutang, buttons: Map<TextView, JenisHutang>) {
        buttons.forEach { (tv, jenis) ->
            if (jenis == selected) {
                tv.setBackgroundResource(R.drawable.bg_jenis_selected)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.setBackgroundResource(R.drawable.bg_jenis_unselected)
                tv.setTextColor(Color.parseColor("#555555"))
            }
        }
    }

    private fun setupJumlahInput() {
        binding.etJumlah.addTextChangedListener(object : TextWatcher {
            var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val raw = s.toString().replace(Regex("[^0-9]"), "")
                jumlahAngka = raw.toLongOrNull() ?: 0L
                val formatted = if (jumlahAngka > 0)
                    "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(jumlahAngka)
                else ""
                binding.etJumlah.setText(formatted)
                binding.etJumlah.setSelection(formatted.length)
                isEditing = false
            }
        })
    }

    private fun setupDatePicker() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        binding.etJatuhTempo.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                tanggalDipilih = cal.time
                binding.etJatuhTempo.setText(sdf.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupSimpan() {
        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNama.text.toString().trim()
            if (nama.isEmpty()) {
                binding.etNama.error = "Nama hutang wajib diisi"
                return@setOnClickListener
            }
            if (jumlahAngka <= 0) {
                binding.etJumlah.error = "Jumlah hutang wajib diisi"
                return@setOnClickListener
            }
            val limitKredit = if (jenisHutangDipilih == JenisHutang.KARTU_KREDIT) {
                binding.etLimitKredit.text.toString().replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            } else 0L

            onSimpan(Hutang(
                nama = nama,
                jenisHutang = jenisHutangDipilih,
                jumlah = jumlahAngka,
                limitKredit = limitKredit,
                jatuhTempo = tanggalDipilih
            ))
            dismiss()
            Toast.makeText(requireContext(), "Hutang berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.appmoneego.ui.hutang

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.CicilanEntity
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Hutang
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CicilanBottomSheetFragment : BottomSheetDialogFragment() {

    private var hutang: Hutang? = null
    private var onSaved: ((Hutang) -> Unit)? = null
    private var onDeleted: ((Hutang) -> Unit)? = null

    private var daftarDompet: List<Dompet> = emptyList()
    private var selectedDompetId   = 0
    private var selectedDompetNama = ""
    private var isSumberDanaOpen   = false

    companion object {
        private const val ARG_HUTANG = "hutang"

        fun newInstance(hutang: Hutang): CicilanBottomSheetFragment {
            val frag = CicilanBottomSheetFragment()
            frag.arguments = Bundle().apply { putSerializable(ARG_HUTANG, hutang) }
            return frag
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        hutang = arguments?.getSerializable(ARG_HUTANG) as? Hutang
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_cicilan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val h = hutang ?: return

        val tvNama              = view.findViewById<TextView>(R.id.tvSheetNama)
        val tvSisa              = view.findViewById<TextView>(R.id.tvSheetSisa)
        val tvSudahDibayar      = view.findViewById<TextView>(R.id.tvSheetSudahDibayar)
        val tvPersen            = view.findViewById<TextView>(R.id.tvSheetPersen)
        val progressBar         = view.findViewById<ProgressBar>(R.id.progressSheet)
        val rvRiwayat           = view.findViewById<RecyclerView>(R.id.rvRiwayatCicilan)
        val layoutForm          = view.findViewById<View>(R.id.layoutFormCicilan)
        val layoutLunas         = view.findViewById<View>(R.id.layoutLunas)
        val etNominal           = view.findViewById<EditText>(R.id.etNominalCicilan)
        val etTanggal           = view.findViewById<EditText>(R.id.etTanggalCicilan)
        val etCatatan           = view.findViewById<EditText>(R.id.etCatatanCicilan)
        val btnSimpan           = view.findViewById<Button>(R.id.btnSimpanCicilan)
        val btnBatal            = view.findViewById<Button>(R.id.btnBatalCicilan)
        val btnHapus            = view.findViewById<Button>(R.id.btnHapusHutang)
        val llSumberDanaHeader  = view.findViewById<LinearLayout>(R.id.llSumberDanaHeader)
        val tvSumberDana        = view.findViewById<TextView>(R.id.tvSumberDanaCicilan)
        val ivChevron           = view.findViewById<ImageView>(R.id.ivChevronSumberDana)
        val llOpsiDompet        = view.findViewById<LinearLayout>(R.id.llOpsiDompet)

        val db         = MoneeGoDatabase.getDatabase(requireContext())
        val cicilanDao = db.cicilanDao()
        val hutangDao  = db.hutangDao()
        val dompetDao  = db.dompetDao()
        val sdf        = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun renderOpsiDompet() {
            llOpsiDompet.removeAllViews()
            if (daftarDompet.isEmpty()) {
                llOpsiDompet.addView(TextView(requireContext()).apply {
                    text = "Belum ada dompet"; textSize = 13f
                    setTextColor(0xFF888888.toInt())
                    setPadding(48, 24, 48, 24)
                })
                return
            }
            daftarDompet.forEach { dompet ->
                val row = LinearLayout(requireContext()).apply {
                    orientation  = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 120)
                    setPadding(48, 0, 48, 0)
                    gravity      = android.view.Gravity.CENTER_VERTICAL
                    isClickable  = true; isFocusable = true
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        selectedDompetId   = dompet.id
                        selectedDompetNama = dompet.nama
                        tvSumberDana.text  = dompet.nama
                        tvSumberDana.setTextColor(0xFF1A1A2E.toInt())
                        isSumberDanaOpen = false
                        llOpsiDompet.visibility = View.GONE
                        ivChevron.rotation = 0f
                        renderOpsiDompet()
                    }
                }
                row.addView(TextView(requireContext()).apply {
                    text = dompet.nama; textSize = 14f
                    setTextColor(0xFF1A1A2E.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(RadioButton(requireContext()).apply {
                    isChecked   = (dompet.id == selectedDompetId)
                    isClickable = false; isFocusable = false
                    buttonTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#534AB7"))
                })
                llOpsiDompet.addView(row)
                if (dompet != daftarDompet.last()) {
                    llOpsiDompet.addView(View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ).apply { setMargins(48, 0, 48, 0) }
                        setBackgroundColor(0xFFEEEEEE.toInt())
                    })
                }
            }
        }

        lifecycleScope.launch {
            daftarDompet = withContext(Dispatchers.IO) {
                dompetDao.getAllDompetSync()
            }
            if (daftarDompet.isNotEmpty() && selectedDompetId == 0) {
                selectedDompetId   = daftarDompet[0].id
                selectedDompetNama = daftarDompet[0].nama
                tvSumberDana?.text = selectedDompetNama
                tvSumberDana?.setTextColor(0xFF1A1A2E.toInt())
            }
            renderOpsiDompet()
        }

        llSumberDanaHeader?.setOnClickListener {
            isSumberDanaOpen = !isSumberDanaOpen
            llOpsiDompet.visibility = if (isSumberDanaOpen) View.VISIBLE else View.GONE
            ivChevron.animate().rotation(if (isSumberDanaOpen) 180f else 0f)
                .setDuration(200).start()
        }

        fun refreshInfo(current: Hutang) {
            val sisa   = (current.totalHutang - current.sudahDibayar).coerceAtLeast(0L)
            val persen = if (current.totalHutang > 0)
                ((current.sudahDibayar.toDouble() / current.totalHutang) * 100).toInt() else 0
            tvNama?.text          = current.nama
            tvSisa?.text          = formatRupiah(sisa)
            tvSudahDibayar?.text  = formatRupiah(current.sudahDibayar)
            tvPersen?.text        = "$persen%"
            progressBar?.progress = persen
        }

        refreshInfo(h)

        if (h.selesai) {
            layoutForm?.visibility  = View.GONE
            layoutLunas?.visibility = View.VISIBLE
        } else {
            layoutForm?.visibility  = View.VISIBLE
            layoutLunas?.visibility = View.GONE
        }

        etTanggal?.setText(sdf.format(Date()))
        etTanggal?.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    cal.set(y, m, d)
                    etTanggal.setText(sdf.format(cal.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        fun loadRiwayat() {
            lifecycleScope.launch {
                val riwayat = withContext(Dispatchers.IO) {
                    cicilanDao.getCicilanByHutangId(h.id)
                }
                rvRiwayat?.layoutManager = LinearLayoutManager(requireContext())
                rvRiwayat?.adapter = RiwayatCicilanAdapter(
                    list         = riwayat,
                    daftarDompet = daftarDompet,
                    onHapus      = { cicilan ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Hapus Cicilan?")
                            .setMessage(
                                "Cicilan sebesar ${formatRupiah(cicilan.nominal)} " +
                                        "akan dihapus dan sisa hutang akan disesuaikan."
                            )
                            .setPositiveButton("Hapus") { _, _ ->
                                lifecycleScope.launch {
                                    val updatedHutang = withContext(Dispatchers.IO) {
                                        cicilanDao.deleteCicilanById(cicilan.id)
                                        val currentHutang = hutangDao.getHutangById(h.id)
                                            ?: return@withContext null
                                        val newSudahDibayar =
                                            (currentHutang.sudahDibayar - cicilan.nominal)
                                                .coerceAtLeast(0L)
                                        val updated = currentHutang.copy(
                                            sudahDibayar = newSudahDibayar,
                                            selesai = newSudahDibayar >= currentHutang.totalHutang
                                        )
                                        if (cicilan.dompetId != 0) {
                                            val dompet = dompetDao.getDompetById(cicilan.dompetId)
                                            dompet?.let {
                                                dompetDao.update(
                                                    it.copy(saldo = it.saldo + cicilan.nominal)
                                                )
                                            }
                                        }
                                        hutangDao.updateHutang(updated)
                                        updated
                                    }
                                    if (updatedHutang != null) {
                                        refreshInfo(updatedHutang)
                                        loadRiwayat()
                                        onSaved?.invoke(updatedHutang)
                                    }
                                }
                            }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                )
            }
        }

        loadRiwayat()

        btnHapus?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Hutang?")
                .setMessage(
                    "Hutang \"${h.nama}\" dan semua riwayat cicilannya akan dihapus permanen."
                )
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            cicilanDao.deleteCicilanByHutangId(h.id)
                            hutangDao.deleteHutang(h)
                        }
                        onDeleted?.invoke(h)
                        Toast.makeText(
                            requireContext(),
                            "Hutang ${h.nama} dihapus",
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        btnSimpan?.setOnClickListener {
            val nominalStr = etNominal?.text.toString().replace("[^0-9]".toRegex(), "")
            val nominal    = nominalStr.toLongOrNull() ?: 0L
            if (nominal <= 0L) {
                etNominal?.error = "Masukkan nominal cicilan"
                return@setOnClickListener
            }
            if (selectedDompetId == 0) {
                Toast.makeText(requireContext(), "Pilih sumber dana", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tanggal = etTanggal?.text.toString().ifBlank { sdf.format(Date()) }
            val catatan = etCatatan?.text.toString()

            lifecycleScope.launch {
                val updatedHutang = withContext(Dispatchers.IO) {
                    val cicilanBaru = CicilanEntity(
                        id           = UUID.randomUUID().toString(),
                        hutangId     = h.id,
                        nominal      = nominal,
                        tanggalBayar = tanggal,
                        catatan      = catatan,
                        dompetId     = selectedDompetId
                    )
                    cicilanDao.insertCicilan(cicilanBaru)
                    val dompet = dompetDao.getDompetById(selectedDompetId)
                    dompet?.let {
                        dompetDao.update(it.copy(saldo = it.saldo - nominal))
                    }
                    val newSudahDibayar =
                        (h.sudahDibayar + nominal).coerceAtMost(h.totalHutang)
                    val updated = h.copy(
                        sudahDibayar = newSudahDibayar,
                        selesai      = newSudahDibayar >= h.totalHutang
                    )
                    hutangDao.updateHutang(updated)
                    updated
                }
                onSaved?.invoke(updatedHutang)
                if (updatedHutang.selesai) {
                    Toast.makeText(
                        requireContext(),
                        "🎉 Hutang ${h.nama} lunas!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dismiss()
            }
        }

        btnBatal?.setOnClickListener { dismiss() }
    }

    fun setOnCicilanSavedListener(listener: (Hutang) -> Unit)  { onSaved   = listener }
    fun setOnHutangDeletedListener(listener: (Hutang) -> Unit) { onDeleted = listener }

    private fun formatRupiah(value: Long): String =
        "Rp${String.format("%,d", value).replace(",", ".")}"
}
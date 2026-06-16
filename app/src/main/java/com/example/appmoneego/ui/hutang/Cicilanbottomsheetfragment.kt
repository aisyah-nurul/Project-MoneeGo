package com.example.appmoneego.ui.hutang

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.appmoneego.data.entity.Transaksi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
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

    // ── FIX BUG 1: mapping icon berdasarkan jenis dompet — sama persis
    // dengan DompetAdapter.getIconRes(), supaya konsisten di seluruh app ──
    private fun getIconDompet(jenis: String): Int = when (jenis) {
        "Rekening Bank"  -> R.drawable.ic_wallet_bank
        "Dompet Digital" -> R.drawable.ic_wallet_digital
        "Uang Tunai"     -> R.drawable.ic_wallet_cash
        "Investasi"      -> R.drawable.ic_wallet_investasi
        "Tabungan"       -> R.drawable.ic_wallet_tabungan
        else             -> R.drawable.ic_wallet_lainnya
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val h = hutang ?: return

        val tvNama             = view.findViewById<TextView>(R.id.tvSheetNama)
        val tvSisa             = view.findViewById<TextView>(R.id.tvSheetSisa)
        val tvSudahDibayar     = view.findViewById<TextView>(R.id.tvSheetSudahDibayar)
        val tvPersen           = view.findViewById<TextView>(R.id.tvSheetPersen)
        val progressBar        = view.findViewById<ProgressBar>(R.id.progressSheet)
        val rvRiwayat          = view.findViewById<RecyclerView>(R.id.rvRiwayatCicilan)
        val layoutForm         = view.findViewById<View>(R.id.layoutFormCicilan)
        val layoutLunas        = view.findViewById<View>(R.id.layoutLunas)
        val etNominal          = view.findViewById<EditText>(R.id.etNominalCicilan)
        val etTanggal          = view.findViewById<EditText>(R.id.etTanggalCicilan)
        val etCatatan          = view.findViewById<EditText>(R.id.etCatatanCicilan)
        val btnSimpan          = view.findViewById<Button>(R.id.btnSimpanCicilan)
        val btnBatal           = view.findViewById<Button>(R.id.btnBatalCicilan)
        val btnHapus           = view.findViewById<Button>(R.id.btnHapusHutang)
        val llSumberDanaHeader = view.findViewById<LinearLayout>(R.id.llSumberDanaHeader)
        val tvSumberDana       = view.findViewById<TextView>(R.id.tvSumberDanaCicilan)
        val ivSumberDanaIcon   = view.findViewById<ImageView>(R.id.ivSumberDanaIcon)
        val ivChevron          = view.findViewById<ImageView>(R.id.ivChevronSumberDana)
        val llOpsiDompet       = view.findViewById<LinearLayout>(R.id.llOpsiDompet)

        val db           = MoneeGoDatabase.getDatabase(requireContext())
        val cicilanDao   = db.cicilanDao()
        val hutangDao    = db.hutangDao()
        val dompetDao    = db.dompetDao()
        val transaksiDao = db.transaksiDao()
        val sdf          = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Konversi dp ke px — dipakai untuk ukuran icon dompet di dropdown
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        // Nilai numerik murni untuk nominal cicilan
        var nominalAngka: Long = 0L

        // ── Render opsi dompet — FIX BUG 1: tambah icon sesuai jenis dompet ────
        fun renderOpsiDompet() {
            llOpsiDompet.removeAllViews()
            if (daftarDompet.isEmpty()) {
                llOpsiDompet.addView(TextView(requireContext()).apply {
                    text = context.getString(R.string.no_wallet_yet)
                    textSize = 13f
                    setTextColor(0xFF888888.toInt()) // MY CHANGE: warna abu-abu lebih netral
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
                    isClickable  = true
                    isFocusable  = true
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        selectedDompetId   = dompet.id
                        selectedDompetNama = dompet.nama
                        tvSumberDana.text  = dompet.nama
                        tvSumberDana.setTextColor(0xFF1A1A2E.toInt()) // MY CHANGE: warna teks gelap
                        // FIX BUG 1: update icon header sesuai dompet yang dipilih
                        ivSumberDanaIcon?.setImageResource(getIconDompet(dompet.jenis))
                        isSumberDanaOpen        = false
                        llOpsiDompet.visibility = View.GONE
                        ivChevron.rotation      = 0f
                        renderOpsiDompet()
                    }
                }
                // FIX BUG 1: icon dompet di setiap baris pilihan
                row.addView(ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                        marginEnd = dp(10)
                    }
                    setImageResource(getIconDompet(dompet.jenis))
                })
                row.addView(TextView(requireContext()).apply {
                    text = dompet.nama
                    textSize = 14f
                    setTextColor(0xFF1A1A2E.toInt()) // MY CHANGE: warna teks gelap
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(RadioButton(requireContext()).apply {
                    isChecked      = (dompet.id == selectedDompetId)
                    isClickable    = false
                    isFocusable    = false
                    buttonTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#7A9DB5"))
                })
                llOpsiDompet.addView(row)
                if (dompet != daftarDompet.last()) {
                    llOpsiDompet.addView(View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            3
                        ).apply {
                            setMargins(0, 0, 0, 0)
                        }
                        setBackgroundColor(
                            android.graphics.Color.parseColor("#B7D0E0")
                        )
                    })
                }
            }
        }

        // ── Load dompet ───────────────────────────────────────────────────────
        lifecycleScope.launch {
            daftarDompet = withContext(Dispatchers.IO) { dompetDao.getAllDompetSync() }
            if (daftarDompet.isNotEmpty() && selectedDompetId == 0) {
                selectedDompetId   = daftarDompet[0].id
                selectedDompetNama = daftarDompet[0].nama
                tvSumberDana?.text = selectedDompetNama
                tvSumberDana?.setTextColor(0xFF1A1A2E.toInt()) // MY CHANGE: warna teks gelap
                // FIX BUG 1: set icon header sesuai dompet default pertama
                ivSumberDanaIcon?.setImageResource(getIconDompet(daftarDompet[0].jenis))
            }
            renderOpsiDompet()
        }

        llSumberDanaHeader?.setOnClickListener {
            isSumberDanaOpen        = !isSumberDanaOpen
            llOpsiDompet.visibility = if (isSumberDanaOpen) View.VISIBLE else View.GONE
            ivChevron.animate()
                .rotation(if (isSumberDanaOpen) 180f else 0f)
                .setDuration(200).start()
        }

        // ── Info hutang ───────────────────────────────────────────────────────
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

        // ── Tanggal ───────────────────────────────────────────────────────────
        etTanggal?.setText(sdf.format(Date()))
        etTanggal?.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                etTanggal.setText(sdf.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // ── TextWatcher auto-format rupiah di field Nominal Cicilan ───────────
        etNominal?.addTextChangedListener(object : TextWatcher {
            var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val raw = s.toString().replace(Regex("[^0-9]"), "")
                nominalAngka = raw.toLongOrNull() ?: 0L
                val formatted = if (nominalAngka > 0)
                    NumberFormat.getNumberInstance(Locale("id", "ID")).format(nominalAngka)
                else ""
                etNominal.setText(formatted)
                etNominal.setSelection(formatted.length)
                isEditing = false
            }
        })

        // ── Riwayat cicilan ───────────────────────────────────────────────────
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
                            .setTitle(getString(R.string.delete_installment_title))
                            .setMessage(getString(R.string.delete_installment_message, formatRupiah(cicilan.nominal)))
                            .setPositiveButton(getString(R.string.delete)){ _, _ ->
                                lifecycleScope.launch {
                                    val updatedHutang = withContext(Dispatchers.IO) {

                                        // FIX BUG 2: hapus juga transaksi terkait
                                        // di Riwayat Transaksi, kalau ada
                                        if (cicilan.transaksiId != 0) {
                                            val transaksiTerkait =
                                                transaksiDao.getTransaksiById(cicilan.transaksiId)
                                            transaksiTerkait?.let { transaksiDao.delete(it) }
                                        }

                                        // Kembalikan saldo dompet
                                        if (cicilan.dompetId != 0) {
                                            val dompet = dompetDao.getDompetById(cicilan.dompetId)
                                            dompet?.let {
                                                dompetDao.update(
                                                    it.copy(saldo = it.saldo + cicilan.nominal))
                                            }
                                        }

                                        cicilanDao.deleteCicilanById(cicilan.id)

                                        val currentHutang = hutangDao.getHutangById(h.id)
                                            ?: return@withContext null

                                        // FIX BUG 3: recalc sudahDibayar dari
                                        // data cicilan yang MASIH TERSISA —
                                        // bukan kurangi cache lama
                                        val totalSudahDibayar = cicilanDao
                                            .getCicilanByHutangId(h.id)
                                            .sumOf { it.nominal }

                                        val updated = currentHutang.copy(
                                            sudahDibayar = totalSudahDibayar,
                                            selesai = currentHutang.totalHutang > 0 &&
                                                    totalSudahDibayar >= currentHutang.totalHutang
                                        )
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
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show()
                    }
                )
            }
        }

        loadRiwayat()

        // ── Hapus hutang ──────────────────────────────────────────────────────
        btnHapus?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_debt_title))
                .setMessage(getString(R.string.delete_debt_message, h.nama))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            cicilanDao.deleteCicilanByHutangId(h.id)
                            hutangDao.deleteHutang(h)
                        }
                        onDeleted?.invoke(h)
                        Toast.makeText(requireContext(), getString(R.string.debt_deleted, h.nama), Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // ── Simpan cicilan ────────────────────────────────────────────────────
        btnSimpan?.setOnClickListener {
            if (nominalAngka <= 0L) {
                etNominal?.error = getString(R.string.enter_installment_amount)
                return@setOnClickListener
            }
            if (selectedDompetId == 0) {
                Toast.makeText(requireContext(), getString(R.string.select_fund_source), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tanggal = etTanggal?.text.toString().ifBlank { sdf.format(Date()) }
            val catatan = etCatatan?.text.toString()

            lifecycleScope.launch {
                val updatedHutang = withContext(Dispatchers.IO) {
                    val tanggalLong = try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .parse(tanggal)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    // FIX BUG 2: insert transaksi DULU agar dapat ID-nya,
                    // baru simpan cicilan dengan transaksiId yang terhubung
                    val transaksiId = transaksiDao.insert(
                        Transaksi(
                            nominal  = nominalAngka.toDouble(),
                            jenis    = "PENGELUARAN",
                            kategori = "Hutang",
                            catatan  = "Cicilan ${h.nama}" +
                                    if (catatan.isNotEmpty()) " - $catatan" else "",
                            tanggal  = tanggalLong,
                            dompetId = selectedDompetId
                        )
                    ).toInt()

                    cicilanDao.insertCicilan(
                        CicilanEntity(
                            id           = UUID.randomUUID().toString(),
                            hutangId     = h.id,
                            nominal      = nominalAngka,
                            tanggalBayar = tanggal,
                            catatan      = catatan,
                            dompetId     = selectedDompetId,
                            transaksiId  = transaksiId
                        )
                    )

                    val dompet = dompetDao.getDompetById(selectedDompetId)
                    dompet?.let {
                        dompetDao.update(it.copy(saldo = it.saldo - nominalAngka))
                    }

                    // FIX BUG 3: recalc sudahDibayar dari total cicilan aktual
                    val totalSudahDibayar = cicilanDao
                        .getCicilanByHutangId(h.id)
                        .sumOf { it.nominal }

                    val updated = h.copy(
                        sudahDibayar = totalSudahDibayar,
                        selesai      = h.totalHutang > 0 && totalSudahDibayar >= h.totalHutang
                    )
                    hutangDao.updateHutang(updated)
                    updated
                }
                onSaved?.invoke(updatedHutang)
                if (updatedHutang.selesai) {
                    Toast.makeText(requireContext(), getString(R.string.debt_paid_off, h.nama), Toast.LENGTH_SHORT).show()
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
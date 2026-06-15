package com.example.appmoneego.ui.tabungan

import android.app.AlertDialog
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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.CicilanEntity
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Tabungan
import com.example.appmoneego.ui.hutang.RiwayatCicilanAdapter
import com.example.appmoneego.utils.CurrencyFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetailTabunganBottomSheet(
    private val tabungan: Tabungan,
    private val onUpdated: (Tabungan) -> Unit,
    private val onDeleted: (Tabungan) -> Unit,
    // ── FITUR BARU: callback dipanggil saat user konfirmasi "Sudah Membeli" ──
    private val onSudahDigunakan: (Int) -> Unit = {}
) : BottomSheetDialogFragment() {

    private var jumlahAngka: Long = 0L
    private var daftarDompet: List<Dompet> = emptyList()
    private var selectedDompetId   = 0
    private var selectedDompetNama = ""
    private var isDropdownOpen     = false

    private val tabunganViewModel: TabunganViewModel by activityViewModels()

    // ── Apakah target sudah selesai (100%) ────────────────────────────────────
    // Dipakai untuk menentukan mode tampilan: form input vs tombol "Sudah Membeli"
    private val isSelesai: Boolean
        get() = tabungan.terkumpul >= tabungan.targetNominal

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_detail_tabungan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNama           = view.findViewById<TextView>(R.id.tvDetailNama)
        val tvHariIni        = view.findViewById<TextView>(R.id.tvDetailHariIni)
        val tvTerkumpul      = view.findViewById<TextView>(R.id.tvDetailTerkumpul)
        val tvSisa           = view.findViewById<TextView>(R.id.tvDetailSisa)
        val etNominal        = view.findViewById<EditText>(R.id.etNominalTabung)
        val etTanggal        = view.findViewById<EditText>(R.id.etTanggalTabung)
        val btnKonfirmasi    = view.findViewById<Button>(R.id.btnKonfirmasiTabung)
        val btnSudahMembeli  = view.findViewById<Button>(R.id.btnSudahMembeli)
        val btnHapus         = view.findViewById<Button>(R.id.btnHapusTabungan)
        val switchPrioritas  = view.findViewById<Switch>(R.id.switchPrioritas)
        val rvRiwayat        = view.findViewById<RecyclerView>(R.id.rvRiwayatTabungan)

        // Grup view yang disembunyikan saat mode SELESAI
        val labelNominal     = view.findViewById<TextView>(R.id.labelNominalTabungan)
        val containerNominal = view.findViewById<LinearLayout>(R.id.containerNominalTabungan)
        val labelTanggal     = view.findViewById<TextView>(R.id.labelTanggalTabungan)
        val containerTanggal = view.findViewById<View>(R.id.containerTanggalTabungan)
        val labelSumber      = view.findViewById<TextView>(R.id.labelSumberDana)
        val containerSumber  = view.findViewById<View>(R.id.containerSumberDana)
        val containerPrioritas = view.findViewById<LinearLayout>(R.id.containerPrioritas)

        val llDropdownHeader = view.findViewById<LinearLayout>(R.id.llSumberDanaHeader)
        val ivIkonDompet     = view.findViewById<ImageView>(R.id.ivIkonDompetDipilih)
        val tvNamaDompet     = view.findViewById<TextView>(R.id.tvSumberDanaTabungan)
        val ivChevron        = view.findViewById<ImageView>(R.id.ivChevronSumberDanaTabungan)
        val llOpsiDompet     = view.findViewById<LinearLayout>(R.id.llOpsiDompetTabungan)
        val dividerDompet    = view.findViewById<View>(R.id.dividerSumberDanaTabungan)

        val db         = MoneeGoDatabase.getDatabase(requireContext())
        val cicilanDao = db.cicilanDao()
        val dompetDao  = db.dompetDao()
        val sdf        = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // ── Info dasar ────────────────────────────────────────────────────────
        tvNama.text      = tabungan.nama
        tvTerkumpul.text = CurrencyFormatter.format(tabungan.terkumpul)
        tvSisa.text      = CurrencyFormatter.format(
            (tabungan.targetNominal - tabungan.terkumpul).coerceAtLeast(0.0)
        )
        tvHariIni.text = "Hari ini: ${
            SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date())
        }"

        // ══════════════════════════════════════════════════════════════════════
        // FITUR BARU: Mode tampilan berdasarkan status target
        //
        // Mode BERJALAN (terkumpul < target):
        //   - Tampilkan form input nominal, tanggal, sumber dana
        //   - Tampilkan tombol KONFIRMASI
        //   - Tampilkan switch prioritas
        //   - Sembunyikan btnSudahMembeli
        //
        // Mode SELESAI (terkumpul >= target):
        //   - Sembunyikan form input nominal, tanggal, sumber dana
        //   - Sembunyikan tombol KONFIRMASI
        //   - Sembunyikan switch prioritas
        //   - Tampilkan tombol "✓ Saya Sudah Membeli Impian Ini"
        //   - Jika sudahDigunakan = true → sembunyikan juga btnSudahMembeli
        //     (user sudah konfirmasi sebelumnya, tidak perlu menekan lagi)
        // ══════════════════════════════════════════════════════════════════════
        if (isSelesai) {
            // Sembunyikan semua elemen form input
            labelNominal.visibility     = View.GONE
            containerNominal.visibility = View.GONE
            labelTanggal.visibility     = View.GONE
            containerTanggal.visibility = View.GONE
            labelSumber.visibility      = View.GONE
            containerSumber.visibility  = View.GONE
            containerPrioritas.visibility = View.GONE
            btnKonfirmasi.visibility    = View.GONE

            // Tampilkan atau sembunyikan btnSudahMembeli
            if (tabungan.sudahDigunakan) {
                // Sudah pernah dikonfirmasi → sembunyikan tombol
                btnSudahMembeli.visibility = View.GONE
            } else {
                // Belum dikonfirmasi → tampilkan tombol
                btnSudahMembeli.visibility = View.VISIBLE
                btnSudahMembeli.setOnClickListener {
                    tampilkanDialogKonfirmasiMembeli()
                }
            }
        } else {
            // Mode BERJALAN — tampilan normal
            labelNominal.visibility     = View.VISIBLE
            containerNominal.visibility = View.VISIBLE
            labelTanggal.visibility     = View.VISIBLE
            containerTanggal.visibility = View.VISIBLE
            labelSumber.visibility      = View.VISIBLE
            containerSumber.visibility  = View.VISIBLE
            containerPrioritas.visibility = View.VISIBLE
            btnKonfirmasi.visibility    = View.VISIBLE
            btnSudahMembeli.visibility  = View.GONE
        }

        // ── Tanggal bayar ─────────────────────────────────────────────────────
        etTanggal.setText(sdf.format(Date()))
        etTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                etTanggal.setText(sdf.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // ── Helper resolve drawable ───────────────────────────────────────────
        fun resolveIkon(namaIkon: String): Int {
            val resId = requireContext().resources.getIdentifier(
                namaIkon, "drawable", requireContext().packageName)
            return if (resId != 0) resId else R.drawable.ic_wallet_lainnya
        }

        // ── Render dropdown dompet ────────────────────────────────────────────
        fun renderDropdown() {
            llOpsiDompet.removeAllViews()
            if (daftarDompet.isEmpty()) {
                llOpsiDompet.addView(TextView(requireContext()).apply {
                    text = "Belum ada dompet"
                    textSize = 13f
                    setTextColor(0xFF888888.toInt())
                    setPadding(48, 24, 48, 24)
                })
                return
            }
            daftarDompet.forEach { dompet ->
                val row = LinearLayout(requireContext()).apply {
                    orientation  = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    setPadding(48, 24, 48, 24)
                    gravity     = android.view.Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        selectedDompetId   = dompet.id
                        selectedDompetNama = dompet.nama
                        tvNamaDompet.text  = dompet.nama
                        tvNamaDompet.setTextColor(0xFF1A1A2E.toInt())
                        ivIkonDompet.setImageResource(resolveIkon(dompet.ikon))
                        try {
                            ivIkonDompet.setColorFilter(
                                android.graphics.Color.parseColor(dompet.warna))
                        } catch (e: Exception) { ivIkonDompet.clearColorFilter() }
                        ivIkonDompet.visibility = View.VISIBLE
                        isDropdownOpen          = false
                        llOpsiDompet.visibility = View.GONE
                        dividerDompet.visibility = View.GONE
                        ivChevron.animate().rotation(0f).setDuration(200).start()
                        renderDropdown()
                    }
                }
                val ivIkon = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(56, 56).apply { marginEnd = 24 }
                    setImageResource(resolveIkon(dompet.ikon))
                    try { setColorFilter(android.graphics.Color.parseColor(dompet.warna))
                    } catch (e: Exception) { clearColorFilter() }
                }
                val tvNama2 = TextView(requireContext()).apply {
                    text = dompet.nama
                    textSize = 14f
                    setTextColor(0xFF1A1A2E.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val ivCentang = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(40, 40)
                    setImageResource(R.drawable.ic_check_green)
                    visibility = if (dompet.id == selectedDompetId) View.VISIBLE else View.GONE
                }
                row.addView(ivIkon)
                row.addView(tvNama2)
                row.addView(ivCentang)
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

        fun toggleDropdown() {
            isDropdownOpen          = !isDropdownOpen
            llOpsiDompet.visibility = if (isDropdownOpen) View.VISIBLE else View.GONE
            dividerDompet.visibility = if (isDropdownOpen) View.VISIBLE else View.GONE
            ivChevron.animate()
                .rotation(if (isDropdownOpen) 180f else 0f)
                .setDuration(200).start()
        }

        llDropdownHeader.setOnClickListener { toggleDropdown() }

        // ── Riwayat cicilan ───────────────────────────────────────────────────
        fun loadRiwayat() {
            lifecycleScope.launch {
                val riwayat: List<CicilanEntity> = withContext(Dispatchers.IO) {
                    cicilanDao.getCicilanByHutangId(tabungan.id.toString())
                }
                rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
                rvRiwayat.adapter = RiwayatCicilanAdapter(
                    list         = riwayat,
                    daftarDompet = daftarDompet,
                    onHapus      = { cicilan: CicilanEntity ->
                        // Sembunyikan tombol hapus cicilan jika target sudah selesai
                        // (tidak bisa hapus cicilan dari target yang sudah 100%)
                        if (isSelesai) {
                            Toast.makeText(
                                requireContext(),
                                "Target sudah selesai, cicilan tidak bisa dihapus",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@RiwayatCicilanAdapter
                        }
                        AlertDialog.Builder(requireContext())
                            .setTitle("Hapus Cicilan?")
                            .setMessage(
                                "Tabungan sebesar ${CurrencyFormatter.format(cicilan.nominal.toDouble())} " +
                                        "akan dihapus dan total terkumpul akan disesuaikan."
                            )
                            .setPositiveButton("Hapus") { _, _ ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        cicilanDao.deleteCicilanById(cicilan.id)
                                        if (cicilan.dompetId != 0) {
                                            val d = dompetDao.getDompetById(cicilan.dompetId)
                                            d?.let {
                                                dompetDao.update(
                                                    it.copy(saldo = it.saldo + cicilan.nominal))
                                            }
                                        }
                                    }
                                    val newTerkumpul =
                                        (tabungan.terkumpul - cicilan.nominal.toDouble())
                                            .coerceAtLeast(0.0)
                                    val updated = tabungan.copy(terkumpul = newTerkumpul)
                                    onUpdated(updated)
                                    tvTerkumpul.text = CurrencyFormatter.format(newTerkumpul)
                                    tvSisa.text = CurrencyFormatter.format(
                                        (tabungan.targetNominal - newTerkumpul).coerceAtLeast(0.0))
                                    loadRiwayat()
                                }
                            }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                )
            }
        }

        // ── Load dompet dari DB → setelah selesai baru load riwayat ──────────
        lifecycleScope.launch {
            daftarDompet = withContext(Dispatchers.IO) { dompetDao.getAllDompetSync() }
            if (daftarDompet.isNotEmpty() && selectedDompetId == 0) {
                val first = daftarDompet[0]
                selectedDompetId   = first.id
                selectedDompetNama = first.nama
                tvNamaDompet.text  = first.nama
                tvNamaDompet.setTextColor(0xFF1A1A2E.toInt())
                ivIkonDompet.setImageResource(resolveIkon(first.ikon))
                try {
                    ivIkonDompet.setColorFilter(
                        android.graphics.Color.parseColor(first.warna))
                } catch (e: Exception) { ivIkonDompet.clearColorFilter() }
                ivIkonDompet.visibility = View.VISIBLE
            }
            renderDropdown()
            loadRiwayat()
        }

        // ── Format input nominal ──────────────────────────────────────────────
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

        // ── Konfirmasi tabungan (hanya mode BERJALAN) ─────────────────────────
        btnKonfirmasi.setOnClickListener {
            if (jumlahAngka <= 0) {
                etNominal.error = "Masukkan jumlah tabungan"
                return@setOnClickListener
            }
            if (selectedDompetId == 0) {
                Toast.makeText(requireContext(), "Pilih sumber dana dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    cicilanDao.insertCicilan(
                        CicilanEntity(
                            id           = UUID.randomUUID().toString(),
                            hutangId     = tabungan.id.toString(),
                            nominal      = jumlahAngka,
                            tanggalBayar = etTanggal.text.toString().ifBlank { sdf.format(Date()) },
                            catatan      = "",
                            dompetId     = selectedDompetId
                        )
                    )
                    val dompet = dompetDao.getDompetById(selectedDompetId)
                    dompet?.let { dompetDao.update(it.copy(saldo = it.saldo - jumlahAngka)) }
                }
                val newTerkumpul = tabungan.terkumpul + jumlahAngka.toDouble()
                onUpdated(tabungan.copy(terkumpul = newTerkumpul))
                Toast.makeText(
                    requireContext(),
                    "Berhasil menabung ${CurrencyFormatter.format(jumlahAngka.toDouble())}!",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }

        // ── Hapus target ──────────────────────────────────────────────────────
        btnHapus.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Target")
                .setMessage("Yakin ingin menghapus target '${tabungan.nama}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            cicilanDao.deleteCicilanByHutangId(tabungan.id.toString())
                        }
                        onDeleted(tabungan)
                        Toast.makeText(requireContext(), "Target dihapus", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // ── Switch prioritas (hanya mode BERJALAN) ────────────────────────────
        switchPrioritas.isChecked = tabungan.isPriority
        switchPrioritas.setOnCheckedChangeListener { _, isChecked ->
            tabunganViewModel.setPrioritas(tabungan.id, isChecked)
            val pesan = if (isChecked)
                "'${tabungan.nama}' dijadikan Target Tabungan Prioritas"
            else
                "'${tabungan.nama}' tidak lagi menjadi prioritas"
            Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnTutupDetail).setOnClickListener { dismiss() }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FITUR BARU: Dialog konfirmasi "Saya Sudah Membeli Impian Ini"
    //
    // Flow:
    //   1. User tap tombol "✓ Saya Sudah Membeli Impian Ini"
    //   2. Muncul dialog: "Apakah dana tabungan ini sudah digunakan..."
    //   3a. User pilih "Belum" → dialog tutup, tidak ada perubahan
    //   3b. User pilih "Sudah" → panggil onSudahDigunakan(tabungan.id)
    //       → TabunganViewModel.tandaiSudahDigunakan()
    //       → Room update sudahDigunakan = true
    //       → LiveData trigger → TabunganFragment recalculate totalTerkumpul
    //       → Tabungan tetap di tab SELESAI, tapi tidak dihitung ke total
    //       → BottomSheet ditutup
    // ══════════════════════════════════════════════════════════════════════════
    private fun tampilkanDialogKonfirmasiMembeli() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Pembelian")
            .setMessage(
                "Apakah dana tabungan \"${tabungan.nama}\" sudah digunakan untuk membeli impian yang dituju?\n\n" +
                        "Dana ini tidak akan lagi dihitung ke Total Terkumpul setelah dikonfirmasi."
            )
            .setPositiveButton("Sudah") { _, _ ->
                onSudahDigunakan(tabungan.id)
                Toast.makeText(
                    requireContext(),
                    "\"${tabungan.nama}\" telah ditandai sebagai selesai digunakan 🎉",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
            .setNegativeButton("Belum", null)
            .show()
    }
}
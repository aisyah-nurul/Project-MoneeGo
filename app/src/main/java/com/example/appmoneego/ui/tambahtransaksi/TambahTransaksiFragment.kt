package com.example.appmoneego.ui.tambahtransaksi

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.DateUtils
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.TransaksiViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.UUID

class TambahTransaksiFragment : Fragment() {

    private lateinit var viewModel:       TransaksiViewModel
    private lateinit var dompetViewModel: DompetViewModel

    private var isEditMode      = false
    private var editId          = 0
    private var editNominalLama = 0.0
    private var editJenisLama   = "PENGELUARAN"
    private var editTransferId: String? = null

    private var nominalString    = ""
    private var jenisTransaksi   = "PENGELUARAN"
    private var selectedKategori = "Makanan"
    private var selectedTanggal: Long = System.currentTimeMillis()
    private var lastSavedTransaksi: Transaksi? = null
    private var isGridKategoriOpen = false
    private var isSumberDanaOpen   = false
    private var isTanggalOpen      = false
    private var catatanString      = ""
    private var isUpperCase        = true
    private var isKeyboardModeTransfer = false

    private var daftarDompet: List<Dompet> = emptyList()
    private var selectedDompetId   = 0
    private var selectedDompetNama = ""

    private var selectedDompetAsalId     = 0
    private var selectedDompetAsalNama   = ""
    private var selectedDompetTujuanId   = 0
    private var selectedDompetTujuanNama = ""

    data class KategoriItem(val nama: String, val iconRes: Int)

    private val kategoriPengeluaran get() = listOf(
        KategoriItem(getString(R.string.kat_makanan),           R.drawable.ic_makanan),
        KategoriItem(getString(R.string.kat_fashion),           R.drawable.ic_fashion),
        KategoriItem(getString(R.string.kat_transportasi),      R.drawable.ic_transportasi),
        KategoriItem(getString(R.string.kat_pendidikan),        R.drawable.ic_pendidikan),
        KategoriItem(getString(R.string.kat_sosial),            R.drawable.ic_sosial),
        KategoriItem(getString(R.string.kat_kesehatan),         R.drawable.ic_kesehatan),
        KategoriItem(getString(R.string.kat_rumah_tangga),      R.drawable.ic_rumahtangga),
        KategoriItem(getString(R.string.kat_kebutuhan_pribadi), R.drawable.ic_kebutuhanpribadi)
    )
    private val kategoriPemasukan get() = listOf(
        KategoriItem(getString(R.string.kat_gaji),      R.drawable.ic_gaji),
        KategoriItem(getString(R.string.kat_bonus),     R.drawable.ic_bonus),
        KategoriItem(getString(R.string.kat_freelance), R.drawable.ic_freelance),
        KategoriItem(getString(R.string.kat_investasi), R.drawable.ic_investasi),
        KategoriItem(getString(R.string.kat_hadiah),    R.drawable.ic_hadiah),
        KategoriItem(getString(R.string.kat_penjualan), R.drawable.ic_penjualan)
    )

    private lateinit var tvNominal:           TextView
    private lateinit var tvKategori:          TextView
    private lateinit var ivKategoriIcon:      ImageView
    private lateinit var ivChevronKategori:   ImageView
    private lateinit var gridKategori:        GridLayout
    private lateinit var dividerKategori:     View
    private lateinit var llKategoriHeader:    LinearLayout
    private lateinit var tvTanggal:           TextView
    private lateinit var ivChevronTanggal:    ImageView
    private lateinit var llTanggalHeader:     LinearLayout
    private lateinit var llCustomCalendar:    LinearLayout
    private lateinit var dividerTanggal:      View
    private lateinit var tvBulanTahun:        TextView
    private lateinit var btnPrevMonth:        ImageButton
    private lateinit var btnNextMonth:        ImageButton
    private lateinit var gridNamaHari:        GridLayout
    private lateinit var gridTanggal:         GridLayout
    private val calendarMonth = Calendar.getInstance()
    private lateinit var tvCatatan:           TextView
    private lateinit var tvSumberDana:        TextView
    private lateinit var ivChevronSumberDana: ImageView
    private lateinit var llSumberDanaHeader:  LinearLayout
    private lateinit var llSumberDanaOptions: LinearLayout
    private lateinit var dividerSumberDana:   View
    private lateinit var tabPengeluaran:      TextView
    private lateinit var tabPemasukan:        TextView
    private lateinit var tabRiwayat:          TextView
    private lateinit var llNumpad:            View
    private lateinit var llKeyboardHuruf:     View
    private lateinit var tvPreviewCatatan:    TextView
    private lateinit var kbShift:             Button
    private lateinit var llAksiCatatan:       View
    private lateinit var btnTransaksiBaru:    Button
    private lateinit var btnHapusTransaksi:   Button
    private lateinit var llFormNormal:        LinearLayout
    private lateinit var llFormTransfer:      LinearLayout
    private lateinit var tvDompetAsal:        TextView
    private lateinit var tvDompetTujuan:      TextView
    private lateinit var cardDompetAsal:      CardView
    private lateinit var cardDompetTujuan:    CardView
    private lateinit var tvCatatanTransfer:   TextView
    private lateinit var cardCatatanTransfer: CardView
    private lateinit var llStateTercatat:     LinearLayout
    private lateinit var tvLabelTercatat:     TextView
    private lateinit var tvNominalTercatat:   TextView
    private lateinit var tvCatatanTercatat:   TextView
    private lateinit var tvKategoriTercatat:  TextView
    private lateinit var tvTanggalTercatat:   TextView
    private lateinit var tvDompetTercatat:    TextView
    private lateinit var tvLabelBaris1:       TextView
    private lateinit var tvLabelBaris2:       TextView
    private lateinit var tvLabelBaris3:       TextView
    private lateinit var ivIconBaris1:        ImageView
    private lateinit var ivIconBaris2:        ImageView
    private lateinit var ivIconBaris3:        ImageView
    private lateinit var btnSimpan:           Button
    private lateinit var btnEnter:            Button

    private val hurufMap = mapOf(
        R.id.kb_q to "q", R.id.kb_w to "w", R.id.kb_e to "e", R.id.kb_r to "r",
        R.id.kb_t to "t", R.id.kb_y to "y", R.id.kb_u to "u", R.id.kb_i to "i",
        R.id.kb_o to "o", R.id.kb_p to "p", R.id.kb_a to "a", R.id.kb_s to "s",
        R.id.kb_d to "d", R.id.kb_f to "f", R.id.kb_g to "g", R.id.kb_h to "h",
        R.id.kb_j to "j", R.id.kb_k to "k", R.id.kb_l to "l", R.id.kb_z to "z",
        R.id.kb_x to "x", R.id.kb_c to "c", R.id.kb_v to "v", R.id.kb_b to "b",
        R.id.kb_n to "n", R.id.kb_m to "m"
    )

    private fun buatBgTabAktif(): GradientDrawable = GradientDrawable().apply {
        shape        = GradientDrawable.RECTANGLE
        cornerRadius = 10f * resources.displayMetrics.density
        setColor("#4A6FA5".toColorInt())
    }

    private fun buatBgTabNonAktif(): GradientDrawable = GradientDrawable().apply {
        shape        = GradientDrawable.RECTANGLE
        cornerRadius = 10f * resources.displayMetrics.density
        setColor(Color.parseColor("#E8E3D8"))
    }

    private fun updateTabUI(aktif: String) {
        mapOf(
            "PENGELUARAN" to tabPengeluaran,
            "PEMASUKAN"   to tabPemasukan,
            "TRANSFER"    to tabRiwayat
        ).forEach { (jenis, tab) ->
            if (jenis == aktif) {
                tab.background = buatBgTabAktif()
                tab.setTextColor(Color.WHITE)
            } else {
                tab.background = buatBgTabNonAktif()
                tab.setTextColor(Color.parseColor("#6B8FA3"))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tambah_transaksi, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            ?.visibility = View.GONE

        viewModel       = ViewModelProvider(this)[TransaksiViewModel::class.java]
        dompetViewModel = ViewModelProvider(this)[DompetViewModel::class.java]

        bindViews(view)
        setupKeyboardListener(view)
        setupTabListener()

        // ── Cek mode edit SEBELUM setupNumpad ─────────────────────────────
        arguments?.let { args ->
            if (args.containsKey("edit_id")) {
                isEditMode      = true
                editId          = args.getInt("edit_id")
                editNominalLama = args.getDouble("edit_nominal")
                editJenisLama   = args.getString("edit_jenis", "PENGELUARAN")!!
                editTransferId  = args.getString("edit_transfer_id", null)

                nominalString    = editNominalLama.toLong().toString()
                selectedKategori = args.getString("edit_kategori", "Makanan")!!
                catatanString    = args.getString("edit_catatan",  "")!!
                selectedTanggal  = args.getLong("edit_tanggal", System.currentTimeMillis())
                selectedDompetId = args.getInt("edit_dompet_id", 0)
                jenisTransaksi   = editJenisLama

                Log.d("EditTransaksi",
                    "MODE EDIT — editId=$editId nominal=$editNominalLama " +
                            "jenis=$editJenisLama kategori=$selectedKategori " +
                            "dompetId=$selectedDompetId transferId=$editTransferId")

                view.findViewById<TextView>(R.id.tv_title)?.text = "EDIT TRANSAKSI"
                view.findViewById<LinearLayout>(R.id.ll_tab_jenis)?.visibility = View.GONE

                switchTabForEdit(jenisTransaksi)
            }
        }

        // setupNumpad dipanggil SETELAH isEditMode di-set
        setupNumpad(view)
        setupKeyboardHuruf(view)
        setupFormFields(view)
        setupCatatan(view)
        setupAksiCatatan()
        updateTanggalDisplay()
        setupGridKategori(kategoriPengeluaran)
        updateTabUI("PENGELUARAN")

        dompetViewModel.allDompet.observe(viewLifecycleOwner) { dompetList ->
            daftarDompet = dompetList
            renderOpsiSumberDana()
            if (isEditMode) prefillEditForm()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            ?.visibility = View.VISIBLE
    }

    private fun switchTabForEdit(jenis: String) {
        jenisTransaksi = jenis
        updateTabUI(jenis)
        if (jenis == "TRANSFER") {
            llFormNormal.visibility   = View.GONE
            llFormTransfer.visibility = View.VISIBLE
            setupCardDompetTransfer()
        } else {
            llFormNormal.visibility   = View.VISIBLE
            llFormTransfer.visibility = View.GONE
            val daftar = if (jenis == "PEMASUKAN") kategoriPemasukan else kategoriPengeluaran
            setupGridKategori(daftar)
        }
    }

    private fun prefillEditForm() {
        updateNominalDisplay()
        updateTanggalDisplay()

        val daftar = if (jenisTransaksi == "PEMASUKAN") kategoriPemasukan else kategoriPengeluaran
        setupGridKategori(daftar)
        updateTabUI(jenisTransaksi)

        tvKategori.text = selectedKategori
        val iconItem = daftar.find { it.nama == selectedKategori }
        if (iconItem != null) ivKategoriIcon.setImageResource(iconItem.iconRes)

        tampilkanCatatan()

        val dompetDipilih = daftarDompet.find { it.id == selectedDompetId }
        if (dompetDipilih != null) {
            selectedDompetNama = dompetDipilih.nama
            tvSumberDana.text  = dompetDipilih.nama
        }
        renderOpsiSumberDana()
    }

    // ── Simpan mode edit ──────────────────────────────────────────────────────
    private fun simpanEditTransaksi(view: View) {
        Log.d("EditTransaksi",
            "simpanEditTransaksi — editId=$editId nominal=$nominalString " +
                    "jenis=$jenisTransaksi kategori=$selectedKategori dompetId=$selectedDompetId")

        val nominal = nominalString.toDoubleOrNull() ?: 0.0
        if (nominal <= 0.0) {
            Snackbar.make(view, "Nominal harus lebih dari 0", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (selectedDompetId == 0) {
            Snackbar.make(view, "Pilih sumber dana", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (editId == 0) {
            Log.e("EditTransaksi", "editId = 0 — update dibatalkan!")
            Snackbar.make(view, "Error: ID transaksi tidak valid", Snackbar.LENGTH_LONG).show()
            return
        }

        val updated = Transaksi(
            id         = editId,
            nominal    = nominal,
            jenis      = jenisTransaksi,
            kategori   = selectedKategori,
            catatan    = catatanString,
            tanggal    = selectedTanggal,
            dompetId   = selectedDompetId,
            transferId = editTransferId
        )

        Log.d("EditTransaksi", "update object: $updated")
        viewModel.update(updated, editNominalLama, editJenisLama)
        Snackbar.make(view, "Transaksi berhasil diperbarui", Snackbar.LENGTH_SHORT).show()
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun bindViews(view: View) {
        tvNominal           = view.findViewById(R.id.tv_nominal)
        tvKategori          = view.findViewById(R.id.tv_kategori)
        ivKategoriIcon      = view.findViewById(R.id.iv_kategori_icon)
        ivChevronKategori   = view.findViewById(R.id.iv_chevron_kategori)
        gridKategori        = view.findViewById(R.id.grid_kategori)
        dividerKategori     = view.findViewById(R.id.divider_kategori)
        llKategoriHeader    = view.findViewById(R.id.ll_kategori_header)
        tvTanggal           = view.findViewById(R.id.tv_tanggal)
        ivChevronTanggal    = view.findViewById(R.id.iv_chevron_tanggal)
        llTanggalHeader     = view.findViewById(R.id.ll_tanggal_header)
        llCustomCalendar    = view.findViewById(R.id.ll_custom_calendar)
        dividerTanggal      = view.findViewById(R.id.divider_tanggal)
        tvBulanTahun        = view.findViewById(R.id.tv_bulan_tahun)
        btnPrevMonth        = view.findViewById(R.id.btn_prev_month)
        btnNextMonth        = view.findViewById(R.id.btn_next_month)
        gridNamaHari        = view.findViewById(R.id.grid_nama_hari)
        gridTanggal         = view.findViewById(R.id.grid_tanggal)
        tvCatatan           = view.findViewById(R.id.tv_catatan)
        tvCatatanTercatat   = view.findViewById(R.id.tv_catatan_tercatat)
        tvKategoriTercatat  = view.findViewById(R.id.tv_kategori_tercatat)
        tvTanggalTercatat   = view.findViewById(R.id.tv_tanggal_tercatat)
        tvDompetTercatat    = view.findViewById(R.id.tv_dompet_tercatat)
        tvLabelBaris1       = view.findViewById(R.id.tv_label_baris1)
        tvLabelBaris2       = view.findViewById(R.id.tv_label_baris2)
        tvLabelBaris3       = view.findViewById(R.id.tv_label_baris3)
        ivIconBaris1        = view.findViewById(R.id.iv_icon_baris1)
        ivIconBaris2        = view.findViewById(R.id.iv_icon_baris2)
        ivIconBaris3        = view.findViewById(R.id.iv_icon_baris3)
        tvSumberDana        = view.findViewById(R.id.tv_sumber_dana)
        ivChevronSumberDana = view.findViewById(R.id.iv_chevron_sumber_dana)
        llSumberDanaHeader  = view.findViewById(R.id.ll_sumber_dana_header)
        llSumberDanaOptions = view.findViewById(R.id.ll_sumber_dana_options)
        dividerSumberDana   = view.findViewById(R.id.divider_sumber_dana)
        tabPengeluaran      = view.findViewById(R.id.tab_pengeluaran)
        tabPemasukan        = view.findViewById(R.id.tab_pemasukan)
        tabRiwayat          = view.findViewById(R.id.tab_riwayat)
        llNumpad            = view.findViewById(R.id.ll_numpad)
        llKeyboardHuruf     = view.findViewById(R.id.ll_keyboard_huruf)
        tvPreviewCatatan    = view.findViewById(R.id.tv_preview_catatan)
        kbShift             = view.findViewById(R.id.kb_shift)
        llAksiCatatan       = view.findViewById(R.id.ll_aksi_catatan)
        btnTransaksiBaru    = view.findViewById(R.id.btn_transaksi_baru)
        btnHapusTransaksi   = view.findViewById(R.id.btn_hapus_transaksi)
        llStateTercatat     = view.findViewById(R.id.ll_state_tercatat)
        tvLabelTercatat     = view.findViewById(R.id.tv_label_tercatat)
        tvNominalTercatat   = view.findViewById(R.id.tv_nominal_tercatat)
        llFormNormal        = view.findViewById(R.id.ll_form_normal)
        llFormTransfer      = view.findViewById(R.id.ll_form_transfer)
        tvDompetAsal        = view.findViewById(R.id.tv_dompet_asal)
        tvDompetTujuan      = view.findViewById(R.id.tv_dompet_tujuan)
        cardDompetAsal      = view.findViewById(R.id.card_dompet_asal)
        cardDompetTujuan    = view.findViewById(R.id.card_dompet_tujuan)
        tvCatatanTransfer   = view.findViewById(R.id.tv_catatan_transfer)
        cardCatatanTransfer = view.findViewById(R.id.card_catatan_transfer)
        btnSimpan           = view.findViewById(R.id.btn_simpan)
        btnEnter            = view.findViewById(R.id.btn_enter)

        view.findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        cardCatatanTransfer.setOnClickListener { bukaKeyboardHurufTransfer() }
        tvNominal.setOnClickListener {
            if (llKeyboardHuruf.visibility == View.VISIBLE) {
                llKeyboardHuruf.visibility = View.GONE
                setupKeyboardHuruf(requireView())
            }
            llNumpad.visibility      = View.VISIBLE
            llAksiCatatan.visibility = View.GONE
        }
    }

    private fun setupKeyboardListener(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(0, 0, 0, navHeight)
            insets
        }
    }

    private fun setupTabListener() {
        tabPengeluaran.setOnClickListener { switchTab("PENGELUARAN") }
        tabPemasukan.setOnClickListener   { switchTab("PEMASUKAN") }
        tabRiwayat.setOnClickListener     { switchTab("TRANSFER") }
    }

    private fun switchTab(jenis: String) {
        if (isEditMode) return
        jenisTransaksi = jenis
        updateTabUI(jenis)
        if (jenis == "TRANSFER") {
            llFormNormal.visibility   = View.GONE
            llFormTransfer.visibility = View.VISIBLE
            setupCardDompetTransfer()
        } else {
            llFormNormal.visibility   = View.VISIBLE
            llFormTransfer.visibility = View.GONE
            val daftar = if (jenis == "PEMASUKAN") kategoriPemasukan else kategoriPengeluaran
            selectedKategori = daftar[0].nama
            tvKategori.text  = selectedKategori
            ivKategoriIcon.setImageResource(daftar[0].iconRes)
            tutupGridKategori()
            setupGridKategori(daftar)
        }
        showStateInput()
    }

    private fun setupCardDompetTransfer() {
        cardDompetAsal.setOnClickListener   { tampilkanPilihDompetDialog(isAsal = true) }
        cardDompetTujuan.setOnClickListener { tampilkanPilihDompetDialog(isAsal = false) }
    }

    private fun tampilkanPilihDompetDialog(isAsal: Boolean) {
        if (daftarDompet.isEmpty()) {
            Snackbar.make(requireView(), getString(R.string.label_belum_ada_dompet),
                Snackbar.LENGTH_SHORT).show()
            return
        }
        val namaList = daftarDompet.map { it.nama }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(
                if (isAsal) getString(R.string.dialog_pilih_dompet_asal)
                else        getString(R.string.dialog_pilih_dompet_tujuan)
            )
            .setItems(namaList) { _, index ->
                val dompet = daftarDompet[index]
                if (isAsal) {
                    if (dompet.id == selectedDompetTujuanId) {
                        Snackbar.make(requireView(),
                            getString(R.string.error_dompet_sama_tujuan),
                            Snackbar.LENGTH_SHORT).show()
                        return@setItems
                    }
                    selectedDompetAsalId   = dompet.id
                    selectedDompetAsalNama = dompet.nama
                    tvDompetAsal.text      = dompet.nama
                    tvDompetAsal.setTextColor(0xFF1A1A2E.toInt())
                } else {
                    if (dompet.id == selectedDompetAsalId) {
                        Snackbar.make(requireView(),
                            getString(R.string.error_dompet_sama_asal),
                            Snackbar.LENGTH_SHORT).show()
                        return@setItems
                    }
                    selectedDompetTujuanId   = dompet.id
                    selectedDompetTujuanNama = dompet.nama
                    tvDompetTujuan.text      = dompet.nama
                    tvDompetTujuan.setTextColor(0xFF1A1A2E.toInt())
                }
            }
            .setNegativeButton(getString(R.string.btn_batal), null).show()
    }

    private fun resetFormTransfer() {
        selectedDompetAsalId     = 0; selectedDompetAsalNama   = ""
        selectedDompetTujuanId   = 0; selectedDompetTujuanNama = ""
        tvDompetAsal.text        = getString(R.string.pilih_dompet)
        tvDompetAsal.setTextColor(0xFF888888.toInt())
        tvDompetTujuan.text      = getString(R.string.pilih_dompet)
        tvDompetTujuan.setTextColor(0xFF888888.toInt())
        catatanString = ""
        tampilkanCatatanTransfer()
    }

    private fun renderOpsiSumberDana() {
        llSumberDanaOptions.removeAllViews()
        if (daftarDompet.isEmpty()) {
            llSumberDanaOptions.addView(TextView(requireContext()).apply {
                text = getString(R.string.label_belum_ada_dompet); textSize = 13f
                setTextColor(0xFF888888.toInt()); setPadding(48, 24, 48, 24)
            })
            selectedDompetId   = 0
            selectedDompetNama = ""
            tvSumberDana.text  = getString(R.string.label_pilih_dompet)
            return
        }
        // Hanya set default ke dompet[0] kalau bukan mode edit dan belum ada pilihan
        if (selectedDompetId == 0 && !isEditMode) {
            selectedDompetId   = daftarDompet[0].id
            selectedDompetNama = daftarDompet[0].nama
            tvSumberDana.text  = selectedDompetNama
        }
        daftarDompet.forEach { dompet ->
            val row = LinearLayout(requireContext()).apply {
                orientation  = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 128)
                setPadding(48, 0, 48, 0)
                gravity      = android.view.Gravity.CENTER_VERTICAL
                isClickable  = true; isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener { pilihDompet(dompet) }
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
                    Color.parseColor("#4A6FA5"))
            })
            llSumberDanaOptions.addView(row)
            if (dompet != daftarDompet.last()) {
                llSumberDanaOptions.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).apply { setMargins(48, 0, 48, 0) }
                    setBackgroundColor(0xFFF0EDE8.toInt())
                })
            }
        }
    }

    private fun pilihDompet(dompet: Dompet) {
        selectedDompetId   = dompet.id
        selectedDompetNama = dompet.nama
        tvSumberDana.text  = dompet.nama
        renderOpsiSumberDana()
        tutupSumberDana()
    }

    private fun tampilkanCatatan() {
        if (catatanString.isEmpty()) {
            tvCatatan.text = getString(R.string.hint_catatan)
            tvCatatan.setTextColor(0xFFB0BEC5.toInt())
        } else {
            tvCatatan.text = catatanString
            tvCatatan.setTextColor(0xFF1A1A2E.toInt())
        }
    }

    private fun tampilkanCatatanTransfer() {
        if (catatanString.isEmpty()) {
            tvCatatanTransfer.text = getString(R.string.hint_tambah_catatan)
            tvCatatanTransfer.setTextColor(0xFFB0BEC5.toInt())
        } else {
            tvCatatanTransfer.text = catatanString
            tvCatatanTransfer.setTextColor(0xFF1A1A2E.toInt())
        }
    }

    private fun setupKeyboardHuruf(view: View) {
        isKeyboardModeTransfer = false
        hurufMap.forEach { (id, karakter) ->
            view.findViewById<Button>(id)?.setOnClickListener {
                catatanString += if (isUpperCase) karakter.uppercase() else karakter
                updateSemuaTampilan()
                if (isUpperCase) { isUpperCase = false; updateShiftUI() }
            }
        }
        kbShift.setOnClickListener { isUpperCase = !isUpperCase; updateShiftUI() }
        view.findViewById<Button>(R.id.kb_spasi)?.setOnClickListener {
            catatanString += " "; updateSemuaTampilan()
        }
        view.findViewById<Button>(R.id.kb_hapus)?.apply {
            setOnClickListener {
                if (catatanString.isNotEmpty()) {
                    catatanString = catatanString.dropLast(1); updateSemuaTampilan()
                }
            }
            setOnLongClickListener { catatanString = ""; updateSemuaTampilan(); true }
        }
        view.findViewById<Button>(R.id.kb_123)?.setOnClickListener     { tutupKeyboardHuruf() }
        view.findViewById<Button>(R.id.kb_selesai)?.setOnClickListener { simpanCatatan() }
    }

    private fun setupKeyboardHurufTransfer() {
        isKeyboardModeTransfer = true
        hurufMap.forEach { (id, karakter) ->
            view?.findViewById<Button>(id)?.setOnClickListener {
                catatanString += if (isUpperCase) karakter.uppercase() else karakter
                updateSemuaTampilanTransfer()
                if (isUpperCase) { isUpperCase = false; updateShiftUI() }
            }
        }
        kbShift.setOnClickListener { isUpperCase = !isUpperCase; updateShiftUI() }
        view?.findViewById<Button>(R.id.kb_spasi)?.setOnClickListener {
            catatanString += " "; updateSemuaTampilanTransfer()
        }
        view?.findViewById<Button>(R.id.kb_hapus)?.apply {
            setOnClickListener {
                if (catatanString.isNotEmpty()) {
                    catatanString = catatanString.dropLast(1); updateSemuaTampilanTransfer()
                }
            }
            setOnLongClickListener { catatanString = ""; updateSemuaTampilanTransfer(); true }
        }
        view?.findViewById<Button>(R.id.kb_123)?.setOnClickListener     { tutupKeyboardHurufTransfer() }
        view?.findViewById<Button>(R.id.kb_selesai)?.setOnClickListener { simpanCatatanTransfer() }
    }

    private fun bukaKeyboardHuruf() {
        if (isGridKategoriOpen) tutupGridKategori()
        if (isSumberDanaOpen)   tutupSumberDana()
        if (isTanggalOpen)      tutupTanggal()
        updateSemuaTampilan()
        isUpperCase = true; updateShiftUI()
        setupKeyboardHuruf(requireView())
        llNumpad.visibility        = View.GONE
        llAksiCatatan.visibility   = View.GONE
        llKeyboardHuruf.visibility = View.VISIBLE
    }

    private fun bukaKeyboardHurufTransfer() {
        updateSemuaTampilanTransfer()
        isUpperCase = true; updateShiftUI()
        setupKeyboardHurufTransfer()
        llNumpad.visibility        = View.GONE
        llAksiCatatan.visibility   = View.GONE
        llKeyboardHuruf.visibility = View.VISIBLE
    }

    private fun tutupKeyboardHuruf() {
        llKeyboardHuruf.visibility = View.GONE
        if (isEditMode) { llNumpad.visibility = View.VISIBLE; return }
        if (lastSavedTransaksi != null) llAksiCatatan.visibility = View.VISIBLE
        else llNumpad.visibility = View.VISIBLE
    }

    private fun tutupKeyboardHurufTransfer() {
        llKeyboardHuruf.visibility = View.GONE
        llNumpad.visibility        = View.VISIBLE
        setupKeyboardHuruf(requireView())
    }

    private fun updateSemuaTampilan() {
        tvPreviewCatatan.text = catatanString.ifEmpty { null }
        tvPreviewCatatan.hint = if (catatanString.isEmpty()) getString(R.string.hint_catatan) else null
        tampilkanCatatan()
    }

    private fun updateSemuaTampilanTransfer() {
        tvPreviewCatatan.text = catatanString.ifEmpty { null }
        tvPreviewCatatan.hint = if (catatanString.isEmpty()) getString(R.string.hint_catatan) else null
        tampilkanCatatanTransfer()
    }

    private fun updateShiftUI() {
        kbShift.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isUpperCase) Color.parseColor("#4A6FA5") else Color.parseColor("#C8C4BA"))
        kbShift.setTextColor(
            if (isUpperCase) Color.WHITE else Color.parseColor("#1A1A2E"))
        hurufMap.forEach { (id, k) ->
            view?.findViewById<Button>(id)?.text = if (isUpperCase) k.uppercase() else k
        }
    }

    private fun simpanCatatan() {
        tampilkanCatatan()
        if (isEditMode) { tutupKeyboardHuruf(); return }
        lastSavedTransaksi?.let { t ->
            val updated = t.copy(catatan = catatanString)
            viewModel.update(updated, t.nominal, t.jenis)
            lastSavedTransaksi       = updated
            tvCatatanTercatat.text   = catatanString
            tvCatatanTercatat.visibility =
                if (catatanString.isEmpty()) View.GONE else View.VISIBLE
        }
        tutupKeyboardHuruf()
        if (lastSavedTransaksi != null) {
            llStateTercatat.visibility = View.VISIBLE
            llAksiCatatan.visibility   = View.VISIBLE
        }
    }

    private fun simpanCatatanTransfer() {
        tampilkanCatatanTransfer()
        tutupKeyboardHurufTransfer()
    }

    private fun setupCatatan(view: View) {
        view.findViewById<CardView>(R.id.card_catatan).apply {
            isClickable = true; isFocusable = true
            setOnClickListener { bukaKeyboardHuruf() }
        }
    }

    private fun setupNumpad(view: View) {
        mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2",
            R.id.btn_3 to "3", R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7", R.id.btn_8 to "8",
            R.id.btn_9 to "9"
        ).forEach { (id, digit) ->
            view.findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }
        view.findViewById<Button>(R.id.btn_hapus).apply {
            setOnClickListener {
                if (nominalString.isNotEmpty()) {
                    nominalString = nominalString.dropLast(1); updateNominalDisplay()
                }
            }
            setOnLongClickListener { nominalString = ""; updateNominalDisplay(); true }
        }
        view.findViewById<Button>(R.id.btn_bintang).setOnClickListener { bukaKeyboardHuruf() }

        // Tentukan fungsi simpan berdasarkan isEditMode yang sudah di-set sebelumnya
        val aksiSimpan: (View) -> Unit = if (isEditMode) {
            { v -> simpanEditTransaksi(v) }
        } else {
            { v -> simpanTransaksi(v) }
        }
        btnSimpan.setOnClickListener { aksiSimpan(view) }
        btnEnter.setOnClickListener  { aksiSimpan(view) }

        view.findViewById<Button>(R.id.btn_plus).setOnClickListener  { }
        view.findViewById<Button>(R.id.btn_minus).setOnClickListener { }
    }

    private fun appendDigit(digit: String) {
        if (nominalString.length >= 12) return
        if (nominalString.isEmpty() && digit == "0") return
        nominalString += digit; updateNominalDisplay()
    }

    private fun updateNominalDisplay() {
        val angka = nominalString.toLongOrNull() ?: 0L
        tvNominal.text = if (angka == 0L) "Rp 0" else CurrencyFormatter.format(angka.toDouble())
    }

    private fun setupGridKategori(daftar: List<KategoriItem>) {
        gridKategori.removeAllViews()
        daftar.forEach { item ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_kategori_grid, gridKategori, false)
            val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            itemView.layoutParams = GridLayout.LayoutParams(spec, spec).apply {
                width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT
            }
            itemView.findViewById<ImageView>(R.id.iv_item_kategori_icon)
                .setImageResource(item.iconRes)
            itemView.findViewById<TextView>(R.id.tv_item_kategori_nama).text = item.nama
            updateItemSelectedState(itemView, item.nama == selectedKategori)
            itemView.setOnClickListener {
                selectedKategori = item.nama
                tvKategori.text  = item.nama
                ivKategoriIcon.setImageResource(item.iconRes)
                for (i in 0 until gridKategori.childCount) {
                    val child = gridKategori.getChildAt(i)
                    updateItemSelectedState(child,
                        child.findViewById<TextView>(R.id.tv_item_kategori_nama)
                            .text.toString() == selectedKategori)
                }
                tutupGridKategori()
            }
            gridKategori.addView(itemView)
        }
    }

    private fun updateItemSelectedState(itemView: View, isSelected: Boolean) {
        itemView.findViewById<LinearLayout>(R.id.ll_item_kategori_bg).setBackgroundResource(
            if (isSelected) R.drawable.bg_kategori_selected else android.R.color.transparent)
        itemView.findViewById<TextView>(R.id.tv_item_kategori_nama).setTextColor(
            if (isSelected) requireContext().getColor(android.R.color.black)
            else 0xFF555555.toInt())
    }

    private fun toggleGridKategori() {
        if (isGridKategoriOpen) tutupGridKategori() else bukaGridKategori()
    }
    private fun bukaGridKategori() {
        isGridKategoriOpen = true
        gridKategori.visibility    = View.VISIBLE
        dividerKategori.visibility = View.VISIBLE
        animateChevron(ivChevronKategori, 0f, 180f)
    }
    private fun tutupGridKategori() {
        isGridKategoriOpen = false
        gridKategori.visibility    = View.GONE
        dividerKategori.visibility = View.GONE
        animateChevron(ivChevronKategori, 180f, 0f)
    }

    private fun toggleTanggal() { if (isTanggalOpen) tutupTanggal() else bukaTanggal() }
    private fun bukaTanggal() {
        isTanggalOpen = true
        llCustomCalendar.visibility = View.VISIBLE
        dividerTanggal.visibility   = View.VISIBLE
        animateChevron(ivChevronTanggal, 0f, 180f)
        if (isGridKategoriOpen) tutupGridKategori()
        if (isSumberDanaOpen)   tutupSumberDana()
        calendarMonth.timeInMillis = selectedTanggal; renderKalender()
    }
    private fun tutupTanggal() {
        isTanggalOpen = false
        llCustomCalendar.visibility = View.GONE
        dividerTanggal.visibility   = View.GONE
        animateChevron(ivChevronTanggal, 180f, 0f)
    }

    private fun toggleSumberDana() {
        if (isSumberDanaOpen) tutupSumberDana() else bukaSumberDana()
    }
    private fun bukaSumberDana() {
        isSumberDanaOpen = true
        llSumberDanaOptions.visibility = View.VISIBLE
        dividerSumberDana.visibility   = View.VISIBLE
        animateChevron(ivChevronSumberDana, 0f, 180f)
        if (isGridKategoriOpen) tutupGridKategori()
        if (isTanggalOpen)      tutupTanggal()
    }
    private fun tutupSumberDana() {
        isSumberDanaOpen = false
        llSumberDanaOptions.visibility = View.GONE
        dividerSumberDana.visibility   = View.GONE
        animateChevron(ivChevronSumberDana, 180f, 0f)
    }

    private fun animateChevron(iv: ImageView, from: Float, to: Float) {
        ObjectAnimator.ofFloat(iv, "rotation", from, to).apply {
            duration = 200; interpolator = AccelerateDecelerateInterpolator(); start()
        }
    }

    private fun setupFormFields(view: View) {
        llKategoriHeader.setOnClickListener   { toggleGridKategori() }
        llSumberDanaHeader.setOnClickListener { toggleSumberDana() }
        llTanggalHeader.setOnClickListener    { toggleTanggal() }
        calendarMonth.timeInMillis = selectedTanggal; setupNamaHari()
        btnPrevMonth.setOnClickListener { calendarMonth.add(Calendar.MONTH, -1); renderKalender() }
        btnNextMonth.setOnClickListener { calendarMonth.add(Calendar.MONTH,  1); renderKalender() }
    }

    private val namaHari get() = listOf(
        getString(R.string.hari_sen), getString(R.string.hari_sel),
        getString(R.string.hari_rab), getString(R.string.hari_kam),
        getString(R.string.hari_jum), getString(R.string.hari_sab),
        getString(R.string.hari_min)
    )

    private val namaBulan get() = listOf(
        getString(R.string.bulan_jan), getString(R.string.bulan_feb),
        getString(R.string.bulan_mar), getString(R.string.bulan_apr),
        getString(R.string.bulan_mei), getString(R.string.bulan_jun),
        getString(R.string.bulan_jul), getString(R.string.bulan_agu),
        getString(R.string.bulan_sep), getString(R.string.bulan_okt),
        getString(R.string.bulan_nov), getString(R.string.bulan_des)
    )

    private fun setupNamaHari() {
        gridNamaHari.removeAllViews()
        namaHari.forEach { hari ->
            gridNamaHari.addView(TextView(requireContext()).apply {
                text = hari; textSize = 10f; setTextColor(0xFF888888.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply { width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT }
            })
        }
    }

    private fun renderKalender() {
        gridTanggal.removeAllViews()
        val cal   = calendarMonth.clone() as Calendar
        val bulan = cal.get(Calendar.MONTH); val tahun = cal.get(Calendar.YEAR)
        tvBulanTahun.text = getString(R.string.format_bulan_tahun, namaBulan[bulan], tahun)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val dow    = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (dow == Calendar.SUNDAY) 6 else dow - 2
        val total  = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayCal = Calendar.getInstance()
        val selCal   = Calendar.getInstance().apply { timeInMillis = selectedTanggal }
        repeat(offset) { gridTanggal.addView(buatSelKosong()) }
        for (day in 1..total) {
            val isToday    = day == todayCal.get(Calendar.DAY_OF_MONTH) &&
                    bulan == todayCal.get(Calendar.MONTH) &&
                    tahun == todayCal.get(Calendar.YEAR)
            val isSelected = day == selCal.get(Calendar.DAY_OF_MONTH) &&
                    bulan == selCal.get(Calendar.MONTH) &&
                    tahun == selCal.get(Calendar.YEAR)
            gridTanggal.addView(TextView(requireContext()).apply {
                text = day.toString(); textSize = 13f
                gravity = android.view.Gravity.CENTER; setPadding(0, 8, 0, 8)
                when {
                    isSelected -> {
                        setTextColor(0xFFFFFFFF.toInt())
                        background = buatBgBulat("#4A6FA5")
                    }
                    isToday    -> {
                        setTextColor(0xFF4A6FA5.toInt())
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    else       -> setTextColor(0xFF1A1A2E.toInt())
                }
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply { width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT }
                val d = day
                setOnClickListener {
                    selectedTanggal = Calendar.getInstance().apply {
                        set(tahun, bulan, d, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    updateTanggalDisplay(); renderKalender(); tutupTanggal()
                }
            })
        }
    }

    private fun buatSelKosong() = View(requireContext()).apply {
        layoutParams = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply { width = 0; height = 40 }
    }

    private fun buatBgBulat(hexColor: String): android.graphics.drawable.Drawable {
        val color = Color.parseColor(hexColor)
        return object : android.graphics.drawable.Drawable() {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                .apply { this.color = color }
            override fun draw(canvas: android.graphics.Canvas) {
                canvas.drawCircle(
                    bounds.centerX().toFloat(), bounds.centerY().toFloat(),
                    minOf(bounds.width(), bounds.height()) / 2f * 0.85f, paint
                )
            }
            override fun setAlpha(a: Int)                                  { paint.alpha = a }
            override fun setColorFilter(cf: android.graphics.ColorFilter?) { paint.colorFilter = cf }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    private fun updateTanggalDisplay() {
        val today     = DateUtils.getStartOfDay(System.currentTimeMillis())
        val yesterday = today - 86_400_000L
        val selected  = DateUtils.getStartOfDay(selectedTanggal)
        tvTanggal.text = when (selected) {
            today     -> "${getString(R.string.label_hari_ini)}, ${DateUtils.formatTanggal(selectedTanggal)}"
            yesterday -> "${getString(R.string.label_kemarin)}, ${DateUtils.formatTanggal(selectedTanggal)}"
            else      -> DateUtils.formatTanggal(selectedTanggal)
        }
    }

    private fun setupAksiCatatan() {
        btnTransaksiBaru.setOnClickListener { showStateInput() }
        btnHapusTransaksi.setOnClickListener {
            lastSavedTransaksi?.let { transaksi ->
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.dialog_hapus_transaksi_title))
                    .setMessage(getString(R.string.dialog_hapus_transaksi_pesan))
                    .setPositiveButton(getString(R.string.dialog_hapus_transaksi_konfirmasi)) { _, _ ->
                        viewModel.delete(transaksi)
                        showStateInput()
                    }
                    .setNegativeButton(getString(R.string.btn_batal), null)
                    .show()
            }
        }
    }

    private fun simpanTransaksi(view: View) {
        val nominal = nominalString.toDoubleOrNull() ?: 0.0
        if (nominal <= 0.0) {
            Snackbar.make(view, "Nominal harus lebih dari 0", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (jenisTransaksi == "TRANSFER") {
            if (selectedDompetAsalId == 0) {
                Snackbar.make(view, "Pilih dompet asal", Snackbar.LENGTH_SHORT).show()
                return
            }
            if (selectedDompetTujuanId == 0) {
                Snackbar.make(view, "Pilih dompet tujuan", Snackbar.LENGTH_SHORT).show()
                return
            }
            val catatanTransfer = catatanString
            val transferId      = UUID.randomUUID().toString()
            viewModel.insert(Transaksi(
                nominal    = nominal, jenis = "PENGELUARAN", kategori = "Transfer",
                catatan    = catatanTransfer.ifEmpty { "Transfer ke $selectedDompetTujuanNama" },
                tanggal    = selectedTanggal, dompetId = selectedDompetAsalId,
                transferId = transferId
            ))
            viewModel.insert(Transaksi(
                nominal    = nominal, jenis = "PEMASUKAN", kategori = "Transfer",
                catatan    = catatanTransfer.ifEmpty { "Transfer dari $selectedDompetAsalNama" },
                tanggal    = selectedTanggal, dompetId = selectedDompetTujuanId,
                transferId = transferId
            ))
            lastSavedTransaksi = Transaksi(
                nominal    = nominal, jenis = "TRANSFER", kategori = "Transfer",
                catatan    = catatanTransfer, tanggal = selectedTanggal,
                dompetId   = 0, transferId = transferId
            )
            val namaAsal     = selectedDompetAsalNama
            val namaTujuan   = selectedDompetTujuanNama
            val catatanFinal = catatanString
            resetFormTransfer()
            showStateTercatat(nominal, namaAsal, namaTujuan, catatanFinal)
            return
        }

        if (selectedDompetId == 0) {
            Snackbar.make(view, "Pilih sumber dana", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (isGridKategoriOpen) tutupGridKategori()
        if (isSumberDanaOpen)   tutupSumberDana()
        if (isTanggalOpen)      tutupTanggal()

        val transaksi = Transaksi(
            nominal  = nominal,
            jenis    = jenisTransaksi,
            kategori = selectedKategori,
            catatan  = catatanString,
            tanggal  = selectedTanggal,
            dompetId = selectedDompetId
        )
        viewModel.insert(transaksi)
        lastSavedTransaksi = transaksi
        showStateTercatat(nominal)
    }

    private fun showStateInput() {
        llStateTercatat.visibility = View.GONE
        llNumpad.visibility        = View.VISIBLE
        llKeyboardHuruf.visibility = View.GONE
        llAksiCatatan.visibility   = View.GONE
        lastSavedTransaksi         = null
        nominalString              = ""
        tvNominal.text             = "Rp 0"
        catatanString              = ""
        tampilkanCatatan()
        updateTanggalDisplay()
        setupKeyboardHuruf(requireView())
    }

    private fun showStateTercatat(
        nominal: Double,
        dompetAsalOverride:   String = "",
        dompetTujuanOverride: String = "",
        catatanOverride:      String = ""
    ) {
        llNumpad.visibility        = View.GONE
        llKeyboardHuruf.visibility = View.GONE
        tvLabelTercatat.text = when (jenisTransaksi) {
            "PEMASUKAN" -> getString(R.string.label_pemasukan_tercatat)
            "TRANSFER"  -> getString(R.string.label_transfer_tercatat)
            else        -> getString(R.string.label_pengeluaran_tercatat)
        }
        tvNominalTercatat.text = CurrencyFormatter.format(nominal)
        if (jenisTransaksi == "TRANSFER") {
            tvLabelBaris1.text = getString(R.string.label_dompet_asal)
            tvLabelBaris2.text = getString(R.string.label_dompet_tujuan)
            tvLabelBaris3.text = getString(R.string.label_tanggal)
            ivIconBaris1.setImageResource(R.drawable.ic_wallet)
            ivIconBaris2.setImageResource(R.drawable.ic_wallet)
            ivIconBaris3.setImageResource(R.drawable.ic_tanggal)
            tvKategoriTercatat.text = dompetAsalOverride
            tvTanggalTercatat.text  = dompetTujuanOverride
            tvDompetTercatat.text   = DateUtils.formatTanggal(selectedTanggal)
        } else {
            tvLabelBaris1.text = getString(R.string.label_kategori)
            tvLabelBaris2.text = getString(R.string.label_tanggal)
            tvLabelBaris3.text = getString(R.string.label_dompet)
            val daftar  = if (jenisTransaksi == "PEMASUKAN") kategoriPemasukan else kategoriPengeluaran
            val iconRes = daftar.find { it.nama == selectedKategori }?.iconRes ?: R.drawable.ic_makanan
            ivIconBaris1.setImageResource(iconRes)
            ivIconBaris2.setImageResource(R.drawable.ic_tanggal)
            ivIconBaris3.setImageResource(R.drawable.ic_wallet)
            tvKategoriTercatat.text = selectedKategori
            tvTanggalTercatat.text  = DateUtils.formatTanggal(selectedTanggal)
            tvDompetTercatat.text   = selectedDompetNama
        }
        tvCatatanTercatat.text = if (jenisTransaksi == "TRANSFER") catatanOverride else catatanString
        llStateTercatat.visibility = View.VISIBLE
        llAksiCatatan.visibility   = View.VISIBLE
        nominalString  = ""
        tvNominal.text = "Rp 0"
        tampilkanCatatan()
        updateTanggalDisplay()
    }
}
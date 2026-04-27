package com.example.appmoneego.ui.tambahtransaksi

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.DateUtils
import com.example.appmoneego.viewmodel.TransaksiViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*

class TambahTransaksiFragment : Fragment() {

    private lateinit var viewModel: TransaksiViewModel

    private var nominalString = ""
    private var jenisTransaksi = "PENGELUARAN"
    private var selectedKategori = "Makanan"
    private var selectedTanggal: Long = System.currentTimeMillis()
    private var selectedSumberDana = "Tunai"
    private var lastSavedTransaksi: Transaksi? = null
    private var isGridKategoriOpen = false
    private var isSumberDanaOpen = false
    private var isTanggalOpen = false

    private var catatanString = ""
    private var isUpperCase = true

    data class KategoriItem(val nama: String, val iconRes: Int)

    private val kategoriPengeluaran = listOf(
        KategoriItem("Makanan",           R.drawable.ic_makanan),
        KategoriItem("Fashion",           R.drawable.ic_fashion),
        KategoriItem("Transportasi",      R.drawable.ic_transportasi),
        KategoriItem("Pendidikan",        R.drawable.ic_pendidikan),
        KategoriItem("Sosial",            R.drawable.ic_sosial),
        KategoriItem("Kesehatan",         R.drawable.ic_kesehatan),
        KategoriItem("Rumah Tangga",      R.drawable.ic_rumahtangga),
        KategoriItem("Kebutuhan Pribadi", R.drawable.ic_kebutuhanpribadi)
    )
    private val kategoriPemasukan = listOf(
        KategoriItem("Gaji",      R.drawable.ic_gaji),
        KategoriItem("Bonus",     R.drawable.ic_bonus),
        KategoriItem("Freelance", R.drawable.ic_freelance),
        KategoriItem("Investasi", R.drawable.ic_investasi),
        KategoriItem("Hadiah",    R.drawable.ic_hadiah),
        KategoriItem("Penjualan", R.drawable.ic_penjualan)
    )

    private lateinit var tvNominal: TextView
    private lateinit var tvKategori: TextView
    private lateinit var ivKategoriIcon: ImageView
    private lateinit var ivChevronKategori: ImageView
    private lateinit var gridKategori: GridLayout
    private lateinit var dividerKategori: View
    private lateinit var llKategoriHeader: LinearLayout
    private lateinit var tvTanggal: TextView
    private lateinit var ivChevronTanggal: ImageView
    private lateinit var llTanggalHeader: LinearLayout
    private lateinit var llCustomCalendar: LinearLayout
    private lateinit var dividerTanggal: View
    private lateinit var tvBulanTahun: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var gridNamaHari: GridLayout
    private lateinit var gridTanggal: GridLayout
    private val calendarMonth = Calendar.getInstance()
    private lateinit var tvCatatan: TextView
    private lateinit var tvSumberDana: TextView
    private lateinit var ivChevronSumberDana: ImageView
    private lateinit var llSumberDanaHeader: LinearLayout
    private lateinit var llSumberDanaOptions: LinearLayout
    private lateinit var dividerSumberDana: View
    private lateinit var rbTunai: RadioButton
    private lateinit var rbEwallet: RadioButton
    private lateinit var rbBank: RadioButton
    private lateinit var tabPengeluaran: TextView
    private lateinit var tabPemasukan: TextView
    private lateinit var tabRiwayat: TextView
    private lateinit var llNumpad: View
    private lateinit var llKeyboardHuruf: View
    private lateinit var tvPreviewCatatan: TextView
    private lateinit var kbShift: Button
    private lateinit var llBannerTercatat: View
    private lateinit var tvBannerTercatat: TextView
    private lateinit var llAksiCatatan: View
    private lateinit var btnTambahCatatan: Button
    private lateinit var btnHapusCatatan: Button

    private val hurufMap = mapOf(
        R.id.kb_q to "q", R.id.kb_w to "w", R.id.kb_e to "e", R.id.kb_r to "r",
        R.id.kb_t to "t", R.id.kb_y to "y", R.id.kb_u to "u", R.id.kb_i to "i",
        R.id.kb_o to "o", R.id.kb_p to "p", R.id.kb_a to "a", R.id.kb_s to "s",
        R.id.kb_d to "d", R.id.kb_f to "f", R.id.kb_g to "g", R.id.kb_h to "h",
        R.id.kb_j to "j", R.id.kb_k to "k", R.id.kb_l to "l", R.id.kb_z to "z",
        R.id.kb_x to "x", R.id.kb_c to "c", R.id.kb_v to "v", R.id.kb_b to "b",
        R.id.kb_n to "n", R.id.kb_m to "m"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tambah_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[TransaksiViewModel::class.java]

        bindViews(view)
        setupKeyboardListener(view)
        setupTabListener()
        setupNumpad(view)
        setupKeyboardHuruf(view)
        setupFormFields(view)
        setupCatatan(view)
        setupAksiCatatan()
        updateTanggalDisplay()
        setupGridKategori(kategoriPengeluaran)
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
        tvSumberDana        = view.findViewById(R.id.tv_sumber_dana)
        ivChevronSumberDana = view.findViewById(R.id.iv_chevron_sumber_dana)
        llSumberDanaHeader  = view.findViewById(R.id.ll_sumber_dana_header)
        llSumberDanaOptions = view.findViewById(R.id.ll_sumber_dana_options)
        dividerSumberDana   = view.findViewById(R.id.divider_sumber_dana)
        rbTunai             = view.findViewById(R.id.rb_tunai)
        rbEwallet           = view.findViewById(R.id.rb_ewallet)
        rbBank              = view.findViewById(R.id.rb_bank)
        tabPengeluaran      = view.findViewById(R.id.tab_pengeluaran)
        tabPemasukan        = view.findViewById(R.id.tab_pemasukan)
        tabRiwayat          = view.findViewById(R.id.tab_riwayat)
        llNumpad            = view.findViewById(R.id.ll_numpad)
        llKeyboardHuruf     = view.findViewById(R.id.ll_keyboard_huruf)
        tvPreviewCatatan    = view.findViewById(R.id.tv_preview_catatan)
        kbShift             = view.findViewById(R.id.kb_shift)
        llBannerTercatat    = view.findViewById(R.id.ll_banner_tercatat)
        tvBannerTercatat    = view.findViewById(R.id.tv_banner_tercatat)
        llAksiCatatan       = view.findViewById(R.id.ll_aksi_catatan)
        btnTambahCatatan    = view.findViewById(R.id.btn_tambah_catatan)
        btnHapusCatatan     = view.findViewById(R.id.btn_hapus_catatan)
        view.findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupKeyboardListener(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomPadding = if (imeHeight > navHeight) imeHeight - navHeight else 0
            view.setPadding(0, 0, 0, bottomPadding)
            insets
        }
    }

    // ── Tab ────────────────────────────────────────────────────────────────────
    private fun setupTabListener() {
        tabPengeluaran.setOnClickListener { switchTab("PENGELUARAN") }
        tabPemasukan.setOnClickListener   { switchTab("PEMASUKAN") }

        // ← DIUBAH: Tab Transfer — tampil snackbar "segera hadir"
        tabRiwayat.setOnClickListener {
            Snackbar.make(requireView(), "Fitur Transfer segera hadir!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun switchTab(jenis: String) {
        jenisTransaksi = jenis
        val inactiveColor = 0xFF888888.toInt()
        listOf(tabPengeluaran, tabPemasukan, tabRiwayat).forEach {
            it.setTextColor(inactiveColor)
            it.setBackgroundResource(android.R.color.transparent)
        }
        val activeTab = if (jenis == "PEMASUKAN") tabPemasukan else tabPengeluaran
        activeTab.setTextColor(requireContext().getColor(android.R.color.black))
        activeTab.setBackgroundResource(R.drawable.bg_tab_selected)
        val daftar = if (jenis == "PEMASUKAN") kategoriPemasukan else kategoriPengeluaran
        selectedKategori = daftar[0].nama
        tvKategori.text  = selectedKategori
        ivKategoriIcon.setImageResource(daftar[0].iconRes)
        tutupGridKategori()
        setupGridKategori(daftar)
        showStateInput()
    }

    // ── Custom Keyboard Huruf ──────────────────────────────────────────────────
    private fun setupKeyboardHuruf(view: View) {
        hurufMap.forEach { (id, karakter) ->
            view.findViewById<Button>(id).setOnClickListener {
                val huruf = if (isUpperCase) karakter.uppercase() else karakter
                catatanString += huruf
                updatePreviewCatatan()
                if (isUpperCase) {
                    isUpperCase = false
                    updateShiftUI()
                }
            }
        }

        kbShift.setOnClickListener {
            isUpperCase = !isUpperCase
            updateShiftUI()
        }

        view.findViewById<Button>(R.id.kb_spasi).setOnClickListener {
            catatanString += " "
            updatePreviewCatatan()
        }

        view.findViewById<Button>(R.id.kb_hapus).setOnClickListener {
            if (catatanString.isNotEmpty()) {
                catatanString = catatanString.dropLast(1)
                updatePreviewCatatan()
            }
        }
        view.findViewById<Button>(R.id.kb_hapus).setOnLongClickListener {
            catatanString = ""
            updatePreviewCatatan()
            true
        }

        view.findViewById<Button>(R.id.kb_123).setOnClickListener { tutupKeyboardHuruf() }
        view.findViewById<Button>(R.id.kb_selesai).setOnClickListener { simpanCatatan() }
    }

    private fun updateShiftUI() {
        kbShift.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isUpperCase) android.graphics.Color.parseColor("#4A6FA5")
            else android.graphics.Color.parseColor("#C8C4BA")
        )
        kbShift.setTextColor(
            if (isUpperCase) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#1A1A2E")
        )
        hurufMap.forEach { (id, karakter) ->
            view?.findViewById<Button>(id)?.text =
                if (isUpperCase) karakter.uppercase() else karakter
        }
    }

    private fun updatePreviewCatatan() {
        tvPreviewCatatan.text = if (catatanString.isEmpty()) null else catatanString
        tvPreviewCatatan.hint = if (catatanString.isEmpty()) "Tulis catatan..." else null
    }

    private fun bukaKeyboardHuruf() {
        if (isGridKategoriOpen) tutupGridKategori()
        if (isSumberDanaOpen) tutupSumberDana()
        if (isTanggalOpen) tutupTanggal()
        catatanString = tvCatatan.text.toString().let {
            if (it == "Tambah catatan...") "" else it
        }
        updatePreviewCatatan()
        isUpperCase = true
        updateShiftUI()
        llNumpad.visibility        = View.GONE
        llAksiCatatan.visibility   = View.GONE
        llKeyboardHuruf.visibility = View.VISIBLE
    }

    private fun tutupKeyboardHuruf() {
        llKeyboardHuruf.visibility = View.GONE
        if (lastSavedTransaksi != null) llAksiCatatan.visibility = View.VISIBLE
        else llNumpad.visibility = View.VISIBLE
    }

    private fun simpanCatatan() {
        tvCatatan.text = if (catatanString.isEmpty()) "Tambah catatan..." else catatanString
        tvCatatan.setTextColor(if (catatanString.isEmpty()) 0xFFAAAAAA.toInt() else 0xFF1A1A2E.toInt())
        lastSavedTransaksi?.let { t ->
            val updated = t.copy(catatan = catatanString)
            viewModel.update(updated, t.nominal, t.jenis)
            lastSavedTransaksi = updated
        }
        tutupKeyboardHuruf()
    }

    private fun setupCatatan(view: View) {
        view.findViewById<androidx.cardview.widget.CardView>(R.id.card_catatan)
            .setOnClickListener { bukaKeyboardHuruf() }
    }

    // ── Numpad ─────────────────────────────────────────────────────────────────
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
                    nominalString = nominalString.dropLast(1)
                    updateNominalDisplay()
                }
            }
            setOnLongClickListener { nominalString = ""; updateNominalDisplay(); true }
        }
        view.findViewById<Button>(R.id.btn_bintang).setOnClickListener { bukaKeyboardHuruf() }
        view.findViewById<Button>(R.id.btn_simpan).setOnClickListener { simpanTransaksi(view) }
        view.findViewById<Button>(R.id.btn_enter).setOnClickListener  { simpanTransaksi(view) }
        view.findViewById<Button>(R.id.btn_plus).setOnClickListener   { }
        view.findViewById<Button>(R.id.btn_minus).setOnClickListener  { }
    }

    private fun appendDigit(digit: String) {
        if (nominalString.length >= 12) return
        if (nominalString.isEmpty() && digit == "0") return
        nominalString += digit
        updateNominalDisplay()
    }

    private fun updateNominalDisplay() {
        val angka = nominalString.toLongOrNull() ?: 0L
        tvNominal.text = if (angka == 0L) "Rp 0" else CurrencyFormatter.format(angka.toDouble())
    }

    // ── Grid Kategori ──────────────────────────────────────────────────────────
    private fun setupGridKategori(daftar: List<KategoriItem>) {
        gridKategori.removeAllViews()
        daftar.forEach { item ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_kategori_grid, gridKategori, false)
            val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            val params = GridLayout.LayoutParams(spec, spec).apply { width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT }
            itemView.layoutParams = params
            itemView.findViewById<ImageView>(R.id.iv_item_kategori_icon).setImageResource(item.iconRes)
            itemView.findViewById<TextView>(R.id.tv_item_kategori_nama).text = item.nama
            updateItemSelectedState(itemView, item.nama == selectedKategori)
            itemView.setOnClickListener {
                selectedKategori = item.nama; tvKategori.text = item.nama
                ivKategoriIcon.setImageResource(item.iconRes)
                for (i in 0 until gridKategori.childCount) {
                    val child = gridKategori.getChildAt(i)
                    updateItemSelectedState(child, child.findViewById<TextView>(R.id.tv_item_kategori_nama).text.toString() == selectedKategori)
                }
                tutupGridKategori()
            }
            gridKategori.addView(itemView)
        }
    }

    private fun updateItemSelectedState(itemView: View, isSelected: Boolean) {
        itemView.findViewById<LinearLayout>(R.id.ll_item_kategori_bg).setBackgroundResource(
            if (isSelected) R.drawable.bg_kategori_selected else android.R.color.transparent
        )
        itemView.findViewById<TextView>(R.id.tv_item_kategori_nama).setTextColor(
            if (isSelected) requireContext().getColor(android.R.color.black) else 0xFF555555.toInt()
        )
    }

    private fun toggleGridKategori() { if (isGridKategoriOpen) tutupGridKategori() else bukaGridKategori() }
    private fun bukaGridKategori() {
        isGridKategoriOpen = true; gridKategori.visibility = View.VISIBLE
        dividerKategori.visibility = View.VISIBLE; animateChevron(ivChevronKategori, 0f, 180f)
    }
    private fun tutupGridKategori() {
        isGridKategoriOpen = false; gridKategori.visibility = View.GONE
        dividerKategori.visibility = View.GONE; animateChevron(ivChevronKategori, 180f, 0f)
    }

    // ── Tanggal ────────────────────────────────────────────────────────────────
    private fun toggleTanggal() { if (isTanggalOpen) tutupTanggal() else bukaTanggal() }
    private fun bukaTanggal() {
        isTanggalOpen = true; llCustomCalendar.visibility = View.VISIBLE
        dividerTanggal.visibility = View.VISIBLE; animateChevron(ivChevronTanggal, 0f, 180f)
        if (isGridKategoriOpen) tutupGridKategori()
        if (isSumberDanaOpen) tutupSumberDana()
        calendarMonth.timeInMillis = selectedTanggal; renderKalender()
    }
    private fun tutupTanggal() {
        isTanggalOpen = false; llCustomCalendar.visibility = View.GONE
        dividerTanggal.visibility = View.GONE; animateChevron(ivChevronTanggal, 180f, 0f)
    }

    // ── Sumber Dana ────────────────────────────────────────────────────────────
    private fun toggleSumberDana() { if (isSumberDanaOpen) tutupSumberDana() else bukaSumberDana() }
    private fun bukaSumberDana() {
        isSumberDanaOpen = true; llSumberDanaOptions.visibility = View.VISIBLE
        dividerSumberDana.visibility = View.VISIBLE; animateChevron(ivChevronSumberDana, 0f, 180f)
        if (isGridKategoriOpen) tutupGridKategori()
    }
    private fun tutupSumberDana() {
        isSumberDanaOpen = false; llSumberDanaOptions.visibility = View.GONE
        dividerSumberDana.visibility = View.GONE; animateChevron(ivChevronSumberDana, 180f, 0f)
    }
    private fun pilihSumberDana(opsi: String) {
        selectedSumberDana = opsi; tvSumberDana.text = opsi
        rbTunai.isChecked = (opsi == "Tunai"); rbEwallet.isChecked = (opsi == "E-Wallet"); rbBank.isChecked = (opsi == "Bank")
        tutupSumberDana()
    }

    private fun animateChevron(iv: ImageView, from: Float, to: Float) {
        ObjectAnimator.ofFloat(iv, "rotation", from, to).apply {
            duration = 200; interpolator = AccelerateDecelerateInterpolator(); start()
        }
    }

    // ── Form fields ────────────────────────────────────────────────────────────
    private fun setupFormFields(view: View) {
        llKategoriHeader.setOnClickListener { toggleGridKategori() }
        llSumberDanaHeader.setOnClickListener { toggleSumberDana() }
        llTanggalHeader.setOnClickListener { toggleTanggal() }
        view.findViewById<LinearLayout>(R.id.ll_option_tunai).setOnClickListener { pilihSumberDana("Tunai") }
        view.findViewById<LinearLayout>(R.id.ll_option_ewallet).setOnClickListener { pilihSumberDana("E-Wallet") }
        view.findViewById<LinearLayout>(R.id.ll_option_bank).setOnClickListener { pilihSumberDana("Bank") }
        calendarMonth.timeInMillis = selectedTanggal
        setupNamaHari()
        btnPrevMonth.setOnClickListener { calendarMonth.add(Calendar.MONTH, -1); renderKalender() }
        btnNextMonth.setOnClickListener { calendarMonth.add(Calendar.MONTH, 1); renderKalender() }
    }

    // ── Custom Calendar ────────────────────────────────────────────────────────
    private val NAMA_HARI  = listOf("SEN","SEL","RAB","KAM","JUM","SAB","MIN")
    private val NAMA_BULAN = listOf("Januari","Februari","Maret","April","Mei","Juni","Juli","Agustus","September","Oktober","November","Desember")

    private fun setupNamaHari() {
        gridNamaHari.removeAllViews()
        NAMA_HARI.forEach { hari ->
            gridNamaHari.addView(TextView(requireContext()).apply {
                text = hari; textSize = 10f; setTextColor(0xFF888888.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f), GridLayout.spec(GridLayout.UNDEFINED,1f)).apply { width=0; height=GridLayout.LayoutParams.WRAP_CONTENT }
            })
        }
    }

    private fun renderKalender() {
        gridTanggal.removeAllViews()
        val cal = calendarMonth.clone() as Calendar
        val bulan = cal.get(Calendar.MONTH); val tahun = cal.get(Calendar.YEAR)
        tvBulanTahun.text = "${NAMA_BULAN[bulan]} $tahun"
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (dow == Calendar.SUNDAY) 6 else dow - 2
        val total = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayCal = Calendar.getInstance()
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedTanggal }
        repeat(offset) { gridTanggal.addView(buatSelKosong()) }
        for (day in 1..total) {
            val isToday    = day == todayCal.get(Calendar.DAY_OF_MONTH) && bulan == todayCal.get(Calendar.MONTH) && tahun == todayCal.get(Calendar.YEAR)
            val isSelected = day == selCal.get(Calendar.DAY_OF_MONTH) && bulan == selCal.get(Calendar.MONTH) && tahun == selCal.get(Calendar.YEAR)
            gridTanggal.addView(TextView(requireContext()).apply {
                text = day.toString(); textSize = 13f; gravity = android.view.Gravity.CENTER; setPadding(0,8,0,8)
                when {
                    isSelected -> { setTextColor(0xFFFFFFFF.toInt()); background = buatBgBulat("#4A6FA5") }
                    isToday    -> { setTextColor(0xFF4A6FA5.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD) }
                    else       -> setTextColor(0xFF1A1A2E.toInt())
                }
                layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f), GridLayout.spec(GridLayout.UNDEFINED,1f)).apply { width=0; height=GridLayout.LayoutParams.WRAP_CONTENT }
                val d = day
                setOnClickListener {
                    selectedTanggal = Calendar.getInstance().apply { set(tahun,bulan,d,0,0,0); set(Calendar.MILLISECOND,0) }.timeInMillis
                    updateTanggalDisplay(); renderKalender(); tutupTanggal()
                }
            })
        }
    }

    private fun buatSelKosong() = View(requireContext()).apply {
        layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f), GridLayout.spec(GridLayout.UNDEFINED,1f)).apply { width=0; height=40 }
    }

    private fun buatBgBulat(hexColor: String): android.graphics.drawable.Drawable {
        val color = android.graphics.Color.parseColor(hexColor)
        return object : android.graphics.drawable.Drawable() {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            override fun draw(canvas: android.graphics.Canvas) {
                canvas.drawCircle(bounds.centerX().toFloat(), bounds.centerY().toFloat(), minOf(bounds.width(),bounds.height())/2f*0.85f, paint)
            }
            override fun setAlpha(a: Int) { paint.alpha = a }
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
            today     -> "Hari ini, ${DateUtils.formatTanggal(selectedTanggal)}"
            yesterday -> "Kemarin, ${DateUtils.formatTanggal(selectedTanggal)}"
            else      -> DateUtils.formatTanggal(selectedTanggal)
        }
    }

    // ── Aksi Catatan ───────────────────────────────────────────────────────────
    private fun setupAksiCatatan() {
        btnTambahCatatan.setOnClickListener { bukaKeyboardHuruf() }
        btnHapusCatatan.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Hapus Catatan?")
                .setMessage("Catatan akan dihapus dari transaksi ini.")
                .setPositiveButton("Hapus") { _, _ ->
                    catatanString = ""; tvCatatan.text = "Tambah catatan..."
                    tvCatatan.setTextColor(0xFFAAAAAA.toInt())
                    lastSavedTransaksi?.let { t ->
                        val updated = t.copy(catatan = "")
                        viewModel.update(updated, t.nominal, t.jenis)
                        lastSavedTransaksi = updated
                    }
                }
                .setNegativeButton("Batal", null).show()
        }
    }

    // ── Simpan ─────────────────────────────────────────────────────────────────
    private fun simpanTransaksi(view: View) {
        val nominal = nominalString.toDoubleOrNull() ?: 0.0
        if (nominal <= 0.0) {
            Snackbar.make(view, "Nominal harus lebih dari 0", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (isGridKategoriOpen) tutupGridKategori()
        if (isSumberDanaOpen) tutupSumberDana()
        if (isTanggalOpen) tutupTanggal()
        val transaksi = Transaksi(
            nominal  = nominal,
            jenis    = jenisTransaksi,
            kategori = selectedKategori,
            catatan  = catatanString,
            tanggal  = selectedTanggal,
            dompetId = 0
        )
        viewModel.insert(transaksi)
        lastSavedTransaksi = transaksi
        showStateTercatat()
    }

    private fun showStateInput() {
        llNumpad.visibility         = View.VISIBLE
        llKeyboardHuruf.visibility  = View.GONE
        llBannerTercatat.visibility = View.GONE
        llAksiCatatan.visibility    = View.GONE
        lastSavedTransaksi          = null
    }

    private fun showStateTercatat() {
        llNumpad.visibility         = View.GONE
        llKeyboardHuruf.visibility  = View.GONE
        llBannerTercatat.visibility = View.VISIBLE
        llAksiCatatan.visibility    = View.VISIBLE
        tvBannerTercatat.text = if (jenisTransaksi == "PEMASUKAN") "PEMASUKAN TERCATAT" else "PENGELUARAN TERCATAT"
        nominalString = ""; tvNominal.text = "Rp 0"
        catatanString = ""; tvCatatan.text = "Tambah catatan..."
        tvCatatan.setTextColor(0xFFAAAAAA.toInt())
        updateTanggalDisplay()
    }
}
package com.example.appmoneego.ui.dashboard

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoneego.R
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.VisibilityPrefs
import com.example.appmoneego.viewmodel.DashboardViewModel
import java.util.Calendar

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel

    // ── View saldo & bulan ────────────────────────────────────────────────────
    private lateinit var tvSaldo:       TextView
    private lateinit var tvPemasukan:   TextView
    private lateinit var tvPengeluaran: TextView
    private lateinit var tvSelisih:     TextView
    private lateinit var ivToggleSaldo: ImageView
    private lateinit var tvBulan:       TextView
    private lateinit var btnPrevBulan:  TextView
    private lateinit var btnNextBulan:  TextView

    // ── View insight ──────────────────────────────────────────────────────────
    private lateinit var tvInsightRingkasan:    TextView
    private lateinit var tvInsightTips:         TextView
    private lateinit var cardRingkasan:         CardView
    private lateinit var viewIndikatorRingkasan: View

    // ── State mata ────────────────────────────────────────────────────────────
    private var isSaldoVisible = true

    // Nilai mentah untuk keperluan toggle mata
    private var nilaiSaldo       = "Rp 0"
    private var nilaiPemasukan   = "Rp 0"
    private var nilaiPengeluaran = "Rp 0"
    private var nilaiSelisih     = "Rp 0"

    // Nilai double untuk hitung selisih dan warna
    private var rawPemasukan   = 0.0
    private var rawPengeluaran = 0.0

    // Calendar bulan yang sedang ditampilkan
    private val calBulanAktif: Calendar = Calendar.getInstance()

    private val NAMA_BULAN = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isSaldoVisible = VisibilityPrefs.isNominalVisible(requireContext())

        initViews(view)
        setupViewModel()
        setupToggleSaldo()
        setupNavigasiBulan()
        setupClickListeners(view)
        syncIkonMata()
        updateLabelBulan()
    }

    // ── Init Views ────────────────────────────────────────────────────────────

    private fun initViews(view: View) {
        tvSaldo              = view.findViewById(R.id.tv_saldo)
        tvPemasukan          = view.findViewById(R.id.tv_pemasukan)
        tvPengeluaran        = view.findViewById(R.id.tv_pengeluaran)
        tvSelisih            = view.findViewById(R.id.tv_selisih)
        ivToggleSaldo        = view.findViewById(R.id.iv_toggle_saldo)
        tvBulan              = view.findViewById(R.id.tv_bulan)
        btnPrevBulan         = view.findViewById(R.id.btn_prev_bulan)
        btnNextBulan         = view.findViewById(R.id.btn_next_bulan)
        tvInsightRingkasan    = view.findViewById(R.id.tv_insight_ringkasan)
        tvInsightTips         = view.findViewById(R.id.tv_insight_tips)
        cardRingkasan         = view.findViewById(R.id.card_ringkasan)
        viewIndikatorRingkasan = view.findViewById(R.id.view_indikator_ringkasan)
    }

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private fun setupViewModel() {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Saldo total semua dompet
        dashboardViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            nilaiSaldo   = CurrencyFormatter.format(total ?: 0.0)
            tvSaldo.text = if (isSaldoVisible) nilaiSaldo else "Rp ***"
        }

        // Pemasukan bulan aktif
        dashboardViewModel.totalPemasukan.observe(viewLifecycleOwner) { pemasukan ->
            rawPemasukan     = pemasukan ?: 0.0
            nilaiPemasukan   = CurrencyFormatter.format(rawPemasukan)
            tvPemasukan.text = if (isSaldoVisible) nilaiPemasukan else "***"
            hitungDanTampilkanSelisih()
        }

        // Pengeluaran bulan aktif
        dashboardViewModel.totalPengeluaran.observe(viewLifecycleOwner) { pengeluaran ->
            rawPengeluaran     = pengeluaran ?: 0.0
            nilaiPengeluaran   = CurrencyFormatter.format(rawPengeluaran)
            tvPengeluaran.text = if (isSaldoVisible) nilaiPengeluaran else "***"
            hitungDanTampilkanSelisih()
        }

        // ── Insight Ringkasan ─────────────────────────────────────────────────
        dashboardViewModel.insightRingkasan.observe(viewLifecycleOwner) { insight ->
            tvInsightRingkasan.text = insight.pesan

            // Ubah warna kartu dan indikator berdasarkan tipe insight
            when (insight.tipe) {
                "WARNING" -> {
                    // Merah — pengeluaran tinggi atau tidak aktif mencatat
                    cardRingkasan.setCardBackgroundColor(0xFFFFCDD2.toInt())
                    setIndikatorWarna(viewIndikatorRingkasan, 0xFFF44336.toInt())
                }
                "SUCCESS" -> {
                    // Hijau — pengeluaran turun
                    cardRingkasan.setCardBackgroundColor(0xFFC8E6C9.toInt())
                    setIndikatorWarna(viewIndikatorRingkasan, 0xFF4CAF50.toInt())
                }
                else -> {
                    // INFO — default merah muda
                    cardRingkasan.setCardBackgroundColor(0xFFFFCDD2.toInt())
                    setIndikatorWarna(viewIndikatorRingkasan, 0xFFF44336.toInt())
                }
            }
        }

        // ── Tips MoneeGo ──────────────────────────────────────────────────────
        dashboardViewModel.insightTips.observe(viewLifecycleOwner) { tips ->
            tvInsightTips.text = tips.pesan
        }
    }

    /**
     * Ubah warna lingkaran indikator secara programatik.
     * Drawable bg_circle_indikator_merah dan bg_circle_indikator_kuning
     * dipakai sebagai template; warnanya di-override di sini.
     */
    private fun setIndikatorWarna(v: View, warna: Int) {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(warna)
        v.background = drawable
    }

    // ── Hitung & Tampilkan Selisih ────────────────────────────────────────────

    private fun hitungDanTampilkanSelisih() {
        val selisih = rawPemasukan - rawPengeluaran

        nilaiSelisih = when {
            selisih > 0  -> CurrencyFormatter.format(selisih)
            selisih < 0  -> "-${CurrencyFormatter.format(-selisih)}"
            else         -> CurrencyFormatter.format(0.0)
        }

        val warnaSelisih = when {
            selisih > 0  -> 0xFF4CAF50.toInt()
            selisih < 0  -> 0xFFF44336.toInt()
            else         -> 0xFF1A1A2E.toInt()
        }

        tvSelisih.text = if (isSaldoVisible) nilaiSelisih else "***"
        tvSelisih.setTextColor(warnaSelisih)
    }

    // ── Navigasi Bulan ────────────────────────────────────────────────────────

    private fun setupNavigasiBulan() {
        btnPrevBulan.setOnClickListener {
            calBulanAktif.add(Calendar.MONTH, -1)
            updateLabelBulan()
            dashboardViewModel.loadBulan(calBulanAktif)
        }
        btnNextBulan.setOnClickListener {
            calBulanAktif.add(Calendar.MONTH, 1)
            updateLabelBulan()
            dashboardViewModel.loadBulan(calBulanAktif)
        }
    }

    private fun updateLabelBulan() {
        val bulan = calBulanAktif.get(Calendar.MONTH)
        val tahun = calBulanAktif.get(Calendar.YEAR)
        tvBulan.text = "${NAMA_BULAN[bulan]} $tahun"
    }

    // ── Toggle Mata ───────────────────────────────────────────────────────────

    private fun setupToggleSaldo() {
        ivToggleSaldo.setOnClickListener {
            isSaldoVisible = !isSaldoVisible
            VisibilityPrefs.setNominalVisible(requireContext(), isSaldoVisible)
            syncIkonMata()
            refreshTampilan()
        }
    }

    private fun syncIkonMata() {
        ivToggleSaldo.setImageResource(
            if (isSaldoVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
        )
    }

    private fun refreshTampilan() {
        if (isSaldoVisible) {
            tvSaldo.text       = nilaiSaldo
            tvPemasukan.text   = nilaiPemasukan
            tvPengeluaran.text = nilaiPengeluaran
            tvSelisih.text     = nilaiSelisih
        } else {
            tvSaldo.text       = "Rp ***"
            tvPemasukan.text   = "***"
            tvPengeluaran.text = "***"
            tvSelisih.text     = "***"
        }
        hitungDanTampilkanSelisih()
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners(view: View) {
        view.findViewById<LinearLayout>(R.id.btn_dompet).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_dompet)
        }
        view.findViewById<LinearLayout>(R.id.btn_tabungan).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tabungan)
        }
        view.findViewById<LinearLayout>(R.id.btn_hutang).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_hutang)
        }
        view.findViewById<View>(R.id.btn_buat_target).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tabungan)
        }
    }
}
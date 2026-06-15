package com.example.appmoneego.ui.dashboard

import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoneego.R
import com.example.appmoneego.ui.tabungan.TabunganViewModel
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.VisibilityPrefs
import com.example.appmoneego.viewmodel.DashboardViewModel
import java.util.Calendar

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var tabunganViewModel: TabunganViewModel

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
    private lateinit var tvInsightRingkasan:     TextView
    private lateinit var tvInsightTips:          TextView
    private lateinit var cardRingkasan:          CardView
    private lateinit var viewIndikatorRingkasan: View

    // ── View prioritas tabungan ───────────────────────────────────────────────
    private lateinit var tvNamaPrioritas:   TextView
    private lateinit var progressPrioritas: ProgressBar
    private lateinit var tvProgressPersen:  TextView
    private lateinit var tvSisaPrioritas:   TextView
    private lateinit var btnBuatTarget:     Button
    private lateinit var btnGantiPrioritas: TextView

    // ── State ─────────────────────────────────────────────────────────────────
    private var isSaldoVisible = true
    private var nilaiSaldo       = "Rp 0"
    private var nilaiPemasukan   = "Rp 0"
    private var nilaiPengeluaran = "Rp 0"
    private var nilaiSelisih     = "Rp 0"
    private var rawPemasukan   = 0.0
    private var rawPengeluaran = 0.0

    private val calBulanAktif: Calendar = Calendar.getInstance()

    private val NAMA_BULAN get() = listOf(
        getString(R.string.bulan_jan), getString(R.string.bulan_feb),
        getString(R.string.bulan_mar), getString(R.string.bulan_apr),
        getString(R.string.bulan_mei), getString(R.string.bulan_jun),
        getString(R.string.bulan_jul), getString(R.string.bulan_agu),
        getString(R.string.bulan_sep), getString(R.string.bulan_okt),
        getString(R.string.bulan_nov), getString(R.string.bulan_des)
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
        dashboardViewModel.updateContext(requireContext())
        setupTabunganPrioritas()
        setupToggleSaldo()
        setupNavigasiBulan()
        setupClickListeners(view)
        syncIkonMata()
        updateLabelBulan()
    }
    override fun onResume() {
        super.onResume()
        dashboardViewModel.updateContext(requireContext())
        setupTabunganPrioritas() // ← tambahkan ini
    }

    private fun initViews(view: View) {
        tvSaldo               = view.findViewById(R.id.tv_saldo)
        tvPemasukan           = view.findViewById(R.id.tv_pemasukan)
        tvPengeluaran         = view.findViewById(R.id.tv_pengeluaran)
        tvSelisih             = view.findViewById(R.id.tv_selisih)
        ivToggleSaldo         = view.findViewById(R.id.iv_toggle_saldo)
        tvBulan               = view.findViewById(R.id.tv_bulan)
        btnPrevBulan          = view.findViewById(R.id.btn_prev_bulan)
        btnNextBulan          = view.findViewById(R.id.btn_next_bulan)
        tvInsightRingkasan    = view.findViewById(R.id.tv_insight_ringkasan)
        tvInsightTips         = view.findViewById(R.id.tv_insight_tips)
        cardRingkasan         = view.findViewById(R.id.card_ringkasan)
        viewIndikatorRingkasan = view.findViewById(R.id.view_indikator_ringkasan)

        tvNamaPrioritas   = view.findViewById(R.id.tv_nama_prioritas)
        progressPrioritas = view.findViewById(R.id.progress_prioritas)
        tvProgressPersen  = view.findViewById(R.id.tv_progress_persen)
        tvSisaPrioritas   = view.findViewById(R.id.tv_sisa_prioritas)
        btnBuatTarget     = view.findViewById(R.id.btn_buat_target)
        btnGantiPrioritas = view.findViewById(R.id.btn_ganti_prioritas)
    }

    private fun setupViewModel() {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        dashboardViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            nilaiSaldo   = CurrencyFormatter.format(total ?: 0.0)
            tvSaldo.text = if (isSaldoVisible) nilaiSaldo else "Rp ***"
        }

        dashboardViewModel.totalPemasukan.observe(viewLifecycleOwner) { pemasukan ->
            rawPemasukan     = pemasukan ?: 0.0
            nilaiPemasukan   = CurrencyFormatter.format(rawPemasukan)
            tvPemasukan.text = if (isSaldoVisible) nilaiPemasukan else "***"
            hitungDanTampilkanSelisih()
        }

        dashboardViewModel.totalPengeluaran.observe(viewLifecycleOwner) { pengeluaran ->
            rawPengeluaran     = pengeluaran ?: 0.0
            nilaiPengeluaran   = CurrencyFormatter.format(rawPengeluaran)
            tvPengeluaran.text = if (isSaldoVisible) nilaiPengeluaran else "***"
            hitungDanTampilkanSelisih()
        }

        dashboardViewModel.insightRingkasan.observe(viewLifecycleOwner) { insight ->
            tvInsightRingkasan.text = insight.pesan
            when (insight.tipe) {
                "WARNING" -> {
                    cardRingkasan.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                            R.color.insight_red_bg))
                }
                "SUCCESS" -> {
                    cardRingkasan.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                            R.color.insight_yellow_bg))
                }
                else -> {
                    cardRingkasan.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                            R.color.insight_red_bg)
                    )
                    setIndikatorWarna(viewIndikatorRingkasan, ContextCompat.getColor(requireContext(),
                            R.color.expense_red)
                    )
                }
            }
        }

        dashboardViewModel.insightTips.observe(viewLifecycleOwner) { tips ->
            tvInsightTips.text = tips.pesan
        }
    }

    private fun setupTabunganPrioritas() {
        tabunganViewModel = ViewModelProvider(this)[TabunganViewModel::class.java]

        tabunganViewModel.tabunganList.removeObservers(viewLifecycleOwner)
        tabunganViewModel.tabunganList.observe(viewLifecycleOwner) { list ->
            val prioritas = list
                .filter { it.terkumpul < it.targetNominal }
                .minByOrNull { it.targetNominal - it.terkumpul }

            if (prioritas == null) {
                tvNamaPrioritas.text  = getString(R.string.label_belum_ada_target) // ← fix
                progressPrioritas.progress = 0
                tvProgressPersen.text = getString(R.string.label_progress_persen, 0) // ← fix
                tvSisaPrioritas.text  = ""
                btnBuatTarget.visibility     = View.VISIBLE
                btnGantiPrioritas.visibility = View.GONE
            } else {
                val persen = if (prioritas.targetNominal > 0)
                    ((prioritas.terkumpul / prioritas.targetNominal) * 100).toInt() else 0
                val sisa = prioritas.targetNominal - prioritas.terkumpul

                tvNamaPrioritas.text        = prioritas.nama
                progressPrioritas.progress  = persen
                tvProgressPersen.text       = getString(R.string.label_progress_persen, persen) // ← fix
                tvSisaPrioritas.text        = getString(R.string.label_sisa_nominal,     // ← fix
                    CurrencyFormatter.format(sisa))
                btnBuatTarget.visibility     = View.GONE
                btnGantiPrioritas.visibility = View.VISIBLE
            }
        }
    }

    private fun setIndikatorWarna(v: View, warna: Int) {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(warna)
        v.background = drawable
    }

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
            else -> ContextCompat.getColor(
                requireContext(),
                R.color.expense_red
            )
        }
        tvSelisih.text = if (isSaldoVisible) nilaiSelisih else "***"
        tvSelisih.setTextColor(warnaSelisih)
    }

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
        btnBuatTarget.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tabungan)
        }
        btnGantiPrioritas.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tabungan)
        }
    }
}
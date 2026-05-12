package com.example.appmoneego.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

    private lateinit var tvSaldo:       TextView
    private lateinit var tvPemasukan:   TextView
    private lateinit var tvPengeluaran: TextView
    private lateinit var ivToggleSaldo: ImageView
    private lateinit var tvBulan:       TextView
    private lateinit var btnPrevBulan:  TextView
    private lateinit var btnNextBulan:  TextView

    // State mata — dibaca dari SharedPreferences agar persisten lintas fragment
    private var isSaldoVisible = true

    // Nilai asli untuk di-toggle tanpa query ulang ke DB
    private var nilaiSaldo       = "Rp 0"
    private var nilaiPemasukan   = "Rp 0"
    private var nilaiPengeluaran = "Rp 0"

    // Calendar yang merepresentasikan bulan yang sedang ditampilkan.
    // Di-clone dari Calendar.getInstance() agar selalu mulai dari bulan saat ini.
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

        // Baca state mata dari SharedPreferences
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
        tvSaldo       = view.findViewById(R.id.tv_saldo)
        tvPemasukan   = view.findViewById(R.id.tv_pemasukan)
        tvPengeluaran = view.findViewById(R.id.tv_pengeluaran)
        ivToggleSaldo = view.findViewById(R.id.iv_toggle_saldo)
        tvBulan       = view.findViewById(R.id.tv_bulan)
        btnPrevBulan  = view.findViewById(R.id.btn_prev_bulan)
        btnNextBulan  = view.findViewById(R.id.btn_next_bulan)
    }

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private fun setupViewModel() {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Saldo total semua dompet — tidak terpengaruh navigasi bulan
        dashboardViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            nilaiSaldo   = CurrencyFormatter.format(total ?: 0.0)
            tvSaldo.text = if (isSaldoVisible) nilaiSaldo else "Rp ***"
        }

        // Pemasukan & Pengeluaran — berubah sesuai bulan aktif
        dashboardViewModel.totalPemasukan.observe(viewLifecycleOwner) { pemasukan ->
            nilaiPemasukan   = CurrencyFormatter.format(pemasukan ?: 0.0)
            tvPemasukan.text = if (isSaldoVisible) nilaiPemasukan else "***"
        }

        dashboardViewModel.totalPengeluaran.observe(viewLifecycleOwner) { pengeluaran ->
            nilaiPengeluaran   = CurrencyFormatter.format(pengeluaran ?: 0.0)
            tvPengeluaran.text = if (isSaldoVisible) nilaiPengeluaran else "***"
        }
    }

    // ── Navigasi Bulan ────────────────────────────────────────────────────────

    private fun setupNavigasiBulan() {
        btnPrevBulan.setOnClickListener {
            // Mundur 1 bulan
            calBulanAktif.add(Calendar.MONTH, -1)
            updateLabelBulan()
            dashboardViewModel.loadBulan(calBulanAktif)
        }

        btnNextBulan.setOnClickListener {
            // Maju 1 bulan
            calBulanAktif.add(Calendar.MONTH, 1)
            updateLabelBulan()
            dashboardViewModel.loadBulan(calBulanAktif)
        }
    }

    private fun updateLabelBulan() {
        val bulan = calBulanAktif.get(Calendar.MONTH)   // 0–11
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
        } else {
            tvSaldo.text       = "Rp ***"
            tvPemasukan.text   = "***"
            tvPengeluaran.text = "***"
        }
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
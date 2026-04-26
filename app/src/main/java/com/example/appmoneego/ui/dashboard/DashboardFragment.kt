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
import com.example.appmoneego.viewmodel.DashboardViewModel

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel

    private lateinit var tvSaldo: TextView
    private lateinit var tvPemasukan: TextView
    private lateinit var tvPengeluaran: TextView
    private lateinit var ivToggleSaldo: ImageView

    // true = angka terlihat, false = disembunyikan (***)
    private var isSaldoVisible = true

    // Simpan nilai terakhir supaya bisa ditampilkan kembali saat toggle
    private var nilaiSaldo      = "Rp0"
    private var nilaiPemasukan  = "Rp0"
    private var nilaiPengeluaran = "Rp0"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupViewModels()
        setupToggleSaldo()
        setupClickListeners(view)
    }

    private fun initViews(view: View) {
        tvSaldo        = view.findViewById(R.id.tv_saldo)
        tvPemasukan    = view.findViewById(R.id.tv_pemasukan)
        tvPengeluaran  = view.findViewById(R.id.tv_pengeluaran)
        ivToggleSaldo  = view.findViewById(R.id.iv_toggle_saldo)
    }

    private fun setupViewModels() {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Observe saldo total semua dompet
        dashboardViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            nilaiSaldo = CurrencyFormatter.format(total ?: 0.0)
            tvSaldo.text = if (isSaldoVisible) nilaiSaldo else "Rp ***"
        }

        // Observe pemasukan bulan ini
        dashboardViewModel.totalPemasukan.observe(viewLifecycleOwner) { pemasukan ->
            nilaiPemasukan = CurrencyFormatter.format(pemasukan ?: 0.0)
            tvPemasukan.text = if (isSaldoVisible) nilaiPemasukan else "***"
        }

        // Observe pengeluaran bulan ini
        dashboardViewModel.totalPengeluaran.observe(viewLifecycleOwner) { pengeluaran ->
            nilaiPengeluaran = CurrencyFormatter.format(pengeluaran ?: 0.0)
            tvPengeluaran.text = if (isSaldoVisible) nilaiPengeluaran else "***"
        }
    }

    private fun setupToggleSaldo() {
        ivToggleSaldo.setOnClickListener {
            isSaldoVisible = !isSaldoVisible

            if (isSaldoVisible) {
                // Tampilkan nilai asli
                tvSaldo.text        = nilaiSaldo
                tvPemasukan.text    = nilaiPemasukan
                tvPengeluaran.text  = nilaiPengeluaran
                // Icon mata terbuka
                ivToggleSaldo.setImageResource(R.drawable.ic_eye)
            } else {
                // Sembunyikan dengan bintang
                tvSaldo.text        = "Rp ***"
                tvPemasukan.text    = "***"
                tvPengeluaran.text  = "***"
                // Icon mata tertutup
                ivToggleSaldo.setImageResource(R.drawable.ic_eye_off)
            }
        }
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
        view.findViewById<View>(R.id.btn_buat_target).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tabungan)
        }
    }
}
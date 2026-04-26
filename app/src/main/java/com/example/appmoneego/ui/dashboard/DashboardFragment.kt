package com.example.appmoneego.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoneego.R
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.TransaksiViewModel

class DashboardFragment : Fragment() {

    private lateinit var dompetViewModel: DompetViewModel
    private lateinit var transaksiViewModel: TransaksiViewModel

    private lateinit var tvSaldo: TextView
    private lateinit var tvPemasukan: TextView
    private lateinit var tvPengeluaran: TextView

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
        setupClickListeners(view)
    }

    private fun initViews(view: View) {
        tvSaldo       = view.findViewById(R.id.tv_saldo)
        tvPemasukan   = view.findViewById(R.id.tv_pemasukan)
        tvPengeluaran = view.findViewById(R.id.tv_pengeluaran)
    }

    private fun setupViewModels() {
        // Saldo total dari semua dompet
        dompetViewModel = ViewModelProvider(this)[DompetViewModel::class.java]
        dompetViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            tvSaldo.text = CurrencyFormatter.format(total ?: 0.0)
        }

        // Pemasukan & pengeluaran bulan ini
        transaksiViewModel = ViewModelProvider(this)[TransaksiViewModel::class.java]
        transaksiViewModel.allTransaksi.observe(viewLifecycleOwner) { list ->
            val pemasukan   = list.filter { it.jenis == "PEMASUKAN" }.sumOf { it.nominal }
            val pengeluaran = list.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
            tvPemasukan.text   = CurrencyFormatter.format(pemasukan)
            tvPengeluaran.text = CurrencyFormatter.format(pengeluaran)
        }
    }

    private fun setupClickListeners(view: View) {
        // Shortcut Dompet
        view.findViewById<LinearLayout>(R.id.btn_dompet).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_dompet)
        }

        // Shortcut Tabungan
        view.findViewById<LinearLayout>(R.id.btn_tabungan).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tabungan)
        }

        // Shortcut Hutang
        view.findViewById<LinearLayout>(R.id.btn_hutang).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_hutang)
        }

        // Tombol buat target tabungan
        view.findViewById<View>(R.id.btn_buat_target).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tabungan)
        }
    }
}
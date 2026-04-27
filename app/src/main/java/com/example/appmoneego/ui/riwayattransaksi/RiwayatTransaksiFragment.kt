package com.example.appmoneego.ui.riwayattransaksi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.adapter.TransaksiAdapter
import com.example.appmoneego.utils.DateUtils
import com.example.appmoneego.viewmodel.TransaksiViewModel
import java.util.*

class RiwayatTransaksiFragment : Fragment() {

    private lateinit var viewModel: TransaksiViewModel
    private lateinit var adapter: TransaksiAdapter

    // Views
    private lateinit var tvJenisFilter: TextView
    private lateinit var tvTanggalHeader: TextView
    private lateinit var tvPersenMasuk: TextView
    private lateinit var tvPersenKeluar: TextView
    private lateinit var progressRatio: ProgressBar
    private lateinit var btnToggleKalender: ImageView
    private lateinit var cardKalender: androidx.cardview.widget.CardView
    private lateinit var calendarView: android.widget.CalendarView
    private lateinit var rvTransaksi: RecyclerView

    // State
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var isKalenderVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_riwayat_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[TransaksiViewModel::class.java]

        bindViews(view)
        setupRecyclerView()
        setupKalender()
        loadTransaksiHariIni()
    }

    private fun bindViews(view: View) {
        tvJenisFilter     = view.findViewById(R.id.tv_jenis_filter)
        tvTanggalHeader   = view.findViewById(R.id.tv_tanggal_header)
        tvPersenMasuk     = view.findViewById(R.id.tv_persen_masuk)
        tvPersenKeluar    = view.findViewById(R.id.tv_persen_keluar)
        progressRatio     = view.findViewById(R.id.progress_ratio)
        btnToggleKalender = view.findViewById(R.id.btn_toggle_kalender)
        cardKalender      = view.findViewById(R.id.card_kalender)
        calendarView      = view.findViewById(R.id.calendar_view)
        rvTransaksi       = view.findViewById(R.id.rv_transaksi)
    }

    private fun setupRecyclerView() {
        adapter = TransaksiAdapter()
        rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        rvTransaksi.adapter = adapter
    }

    private fun setupKalender() {
        // Toggle tampil/sembunyikan kalender
        btnToggleKalender.setOnClickListener {
            isKalenderVisible = !isKalenderVisible
            cardKalender.visibility = if (isKalenderVisible) View.VISIBLE else View.GONE
        }

        // Saat tanggal dipilih di kalender
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            selectedDateMillis = cal.timeInMillis

            // Sembunyikan kalender setelah pilih
            isKalenderVisible = false
            cardKalender.visibility = View.GONE

            loadTransaksiByTanggal(selectedDateMillis)
        }
    }

    private fun loadTransaksiHariIni() {
        selectedDateMillis = System.currentTimeMillis()
        loadTransaksiByTanggal(selectedDateMillis)
    }

    private fun loadTransaksiByTanggal(timeMillis: Long) {
        val start = DateUtils.getStartOfDay(timeMillis)
        val end   = DateUtils.getEndOfDay(timeMillis)

        tvTanggalHeader.text = DateUtils.formatTanggal(timeMillis)

        viewModel.getByDateRange(start, end).observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)

            // Hitung total pemasukan dan pengeluaran
            val totalMasuk  = list.filter { it.jenis == "PEMASUKAN" }.sumOf { it.nominal }
            val totalKeluar = list.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
            val total       = totalMasuk + totalKeluar

            // Update header info
            tvJenisFilter.text = if (totalKeluar >= totalMasuk) "Pengeluaran" else "Pemasukan"

            // Update legend persentase
            if (total > 0) {
                val persenMasuk  = ((totalMasuk / total) * 100).toInt()
                val persenKeluar = 100 - persenMasuk
                tvPersenMasuk.text  = " Pemasukan $persenMasuk%"
                tvPersenKeluar.text = " Pengeluaran $persenKeluar%"
                progressRatio.progress = persenMasuk
            } else {
                tvPersenMasuk.text  = " Pemasukan 0%"
                tvPersenKeluar.text = " Pengeluaran 0%"
                progressRatio.progress = 50
            }
        }
    }
}
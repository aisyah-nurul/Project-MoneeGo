package com.example.appmoneego.ui.analisis

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.adapter.KategoriAnalisisAdapter
import com.example.appmoneego.adapter.TransaksiAnalisisAdapter
import com.example.appmoneego.model.KategoriSummary
import com.example.appmoneego.viewmodel.AnalisisViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.data.PieEntry

class AnalisisFragment : Fragment() {

    private val viewModel: AnalisisViewModel by viewModels()

    private lateinit var kategoriAdapter: KategoriAnalisisAdapter
    private lateinit var transaksiAdapter: TransaksiAnalisisAdapter

    private lateinit var tabPengeluaran: TextView
    private lateinit var tabPemasukan: TextView
    private lateinit var subTabKategori: TextView
    private lateinit var subTabDompet: TextView
    private lateinit var pieChart: PieChart
    private lateinit var tvFilterTipe: TextView
    private lateinit var tvBulanTahun: TextView
    private lateinit var tvTotalNominal: TextView
    private lateinit var tvTerbesarNama: TextView
    private lateinit var tvTerbesarPersen: TextView
    private lateinit var rvKategori: RecyclerView
    private lateinit var rvTransaksiTerbaru: RecyclerView
    private lateinit var btnPrevBulan: ImageView
    private lateinit var btnNextBulan: ImageView
    private lateinit var tvEmptyTransaksi: TextView
    private lateinit var btnTambahTransaksi: ImageView

    private val pieColors = listOf(
        "#2C3E6B", "#4A6E8A", "#6B8FA3", "#8FAEC0"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_analisis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews(view)
        setupAdapters()
        setupClickListeners()
        observeViewModel()
    }

    private fun bindViews(view: View) {
        tabPengeluaran = view.findViewById(R.id.tabPengeluaran)
        tabPemasukan = view.findViewById(R.id.tabPemasukan)
        subTabKategori = view.findViewById(R.id.subTabKategori)
        subTabDompet = view.findViewById(R.id.subTabDompet)
        pieChart = view.findViewById(R.id.pieChart)
        tvFilterTipe = view.findViewById(R.id.tvFilterTipe)
        tvBulanTahun = view.findViewById(R.id.tvBulanTahun)
        tvTotalNominal = view.findViewById(R.id.tvTotalNominal)
        tvTerbesarNama = view.findViewById(R.id.tvTerbesarNama)
        tvTerbesarPersen = view.findViewById(R.id.tvTerbesarPersen)
        rvKategori = view.findViewById(R.id.rvKategori)
        rvTransaksiTerbaru = view.findViewById(R.id.rvTransaksiTerbaru)
        btnPrevBulan = view.findViewById(R.id.btnPrevBulan)
        btnNextBulan = view.findViewById(R.id.btnNextBulan)
        tvEmptyTransaksi = view.findViewById(R.id.tvEmptyTransaksi)
        btnTambahTransaksi = view.findViewById(R.id.btnTambahTransaksi)
    }

    private fun setupAdapters() {
        kategoriAdapter = KategoriAnalisisAdapter()
        rvKategori.layoutManager = LinearLayoutManager(requireContext())
        rvKategori.adapter = kategoriAdapter

        transaksiAdapter = TransaksiAnalisisAdapter()
        rvTransaksiTerbaru.layoutManager = LinearLayoutManager(requireContext())
        rvTransaksiTerbaru.adapter = transaksiAdapter
    }

    private fun setupClickListeners() {

        setActiveMainTab(true)

        tabPengeluaran.setOnClickListener {
            viewModel.setTab("PENGELUARAN")
            setActiveMainTab(true)
        }

        tabPemasukan.setOnClickListener {
            viewModel.setTab("PEMASUKAN")
            setActiveMainTab(false)
        }

        subTabKategori.setOnClickListener {
            viewModel.setSubTab("kategori")
            tvFilterTipe.text = "Oleh Kategori"
        }

        subTabDompet.setOnClickListener {
            viewModel.setSubTab("dompet")
            tvFilterTipe.text = "Oleh Dompet"
        }

        btnPrevBulan.setOnClickListener {
            viewModel.prevBulan()
        }

        btnNextBulan.setOnClickListener {
            viewModel.nextBulan()
        }

        btnTambahTransaksi.setOnClickListener {
            findNavController().navigate(R.id.tambahTransaksiFragment)
        }
    }

    private fun observeViewModel() {

        viewModel.transaksiTerbaru.observe(viewLifecycleOwner) {
            transaksiAdapter.submitList(it)
            tvEmptyTransaksi.visibility =
                if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.kategoriSummaryPengeluaran.observe(viewLifecycleOwner) {
            if (viewModel.activeTab.value == "PENGELUARAN") {
                updateUI(it, viewModel.totalPengeluaran.value ?: 0.0)
            }
        }

        viewModel.kategoriSummaryPemasukan.observe(viewLifecycleOwner) {
            if (viewModel.activeTab.value == "PEMASUKAN") {
                updateUI(it, viewModel.totalPemasukan.value ?: 0.0)
            }
        }
    }

    private fun updateUI(list: List<KategoriSummary>, total: Double) {
        kategoriAdapter.submitList(list)
        updatePieChart(list)

        tvTotalNominal.text = formatRupiah(total)

        val terbesar = list.firstOrNull()
        tvTerbesarNama.text = terbesar?.nama ?: "-"
        tvTerbesarPersen.text = terbesar?.let { "${it.persentase.toInt()}%" } ?: "-"
    }

    private fun updatePieChart(dataList: List<KategoriSummary>) {
        if (dataList.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "Tidak ada data"
            return
        }

        val entries: List<PieEntry> = dataList.map { item: KategoriSummary ->
            PieEntry(item.persentase.toFloat(), item.nama)
        }

        val colors = dataList.mapIndexed { i, _ ->
            Color.parseColor(pieColors[i % pieColors.size])
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueFormatter = PercentFormatter(pieChart)
            valueTextColor = Color.WHITE
            valueTextSize = 11f
        }

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.invalidate()
    }

    private fun setActiveMainTab(isPengeluaran: Boolean) {
        if (isPengeluaran) {
            tabPengeluaran.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.tab_active_bg)
            )
            tabPemasukan.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.tab_inactive_bg)
            )
        } else {
            tabPemasukan.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.tab_active_bg)
            )
            tabPengeluaran.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.tab_inactive_bg)
            )
        }
    }

    private fun formatRupiah(amount: Double): String {
        return "Rp ${String.format("%,.0f", amount).replace(',', '.')}"
    }
}
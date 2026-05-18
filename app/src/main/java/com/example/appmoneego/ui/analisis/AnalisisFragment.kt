package com.example.appmoneego.ui.analisis

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.adapter.KategoriAnalisisAdapter
import com.example.appmoneego.adapter.TransaksiAnalisisAdapter
import com.example.appmoneego.model.KategoriSummary
import com.example.appmoneego.viewmodel.AnalisisViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomsheet.BottomSheetDialog

class AnalisisFragment : Fragment() {

    private val viewModel: AnalisisViewModel by viewModels()

    private lateinit var kategoriAdapter: KategoriAnalisisAdapter

    private lateinit var tabPengeluaran: TextView
    private lateinit var tabPemasukan: TextView
    private lateinit var pieChart: PieChart
    private lateinit var tvFilterTipe: TextView
    private lateinit var tvBulanTahun: TextView
    private lateinit var tvLabelTotal: TextView
    private lateinit var tvTotalNominal: TextView
    private lateinit var tvBulanLabel: TextView
    private lateinit var tvTerbesarNama: TextView
    private lateinit var tvTerbesarPersen: TextView
    private lateinit var rvKategori: RecyclerView
    private lateinit var btnPrevBulan: TextView
    private lateinit var btnNextBulan: TextView
    private lateinit var layoutFilterTipe: LinearLayout

    private var filterMode = "kategori"

    // Warna pie chart palette navy Moneego
    private val pieColors = listOf(
        "#2C3E6B", "#4A6E8A", "#6B8FA3", "#8FAEC0",
        "#9ABFD0", "#AECFDB", "#5B8DB8", "#7BA7BC",
        "#B0C9D4", "#C8DCE4"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_analisis, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupPieChart()
        setupAdapter()
        setupClick()
        observeViewModel()
    }

    // ── Bind ─────────────────────────────────────────────────────────────────

    private fun bindViews(view: View) {
        tabPengeluaran   = view.findViewById(R.id.tabPengeluaran)
        tabPemasukan     = view.findViewById(R.id.tabPemasukan)
        pieChart         = view.findViewById(R.id.pieChart)
        tvFilterTipe     = view.findViewById(R.id.tvFilterTipe)
        tvBulanTahun     = view.findViewById(R.id.tvBulanTahun)
        tvLabelTotal     = view.findViewById(R.id.tvLabelTotal)
        tvTotalNominal   = view.findViewById(R.id.tvTotalNominal)
        tvBulanLabel     = view.findViewById(R.id.tvBulanLabel)
        tvTerbesarNama   = view.findViewById(R.id.tvTerbesarNama)
        tvTerbesarPersen = view.findViewById(R.id.tvTerbesarPersen)
        rvKategori       = view.findViewById(R.id.rvKategori)
        btnPrevBulan     = view.findViewById(R.id.btnPrevBulan)
        btnNextBulan     = view.findViewById(R.id.btnNextBulan)
        layoutFilterTipe = view.findViewById(R.id.layoutFilterTipe)
    }

    // ── Pie Chart ─────────────────────────────────────────────────────────────

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled   = false
            legend.isEnabled        = false
            isDrawHoleEnabled       = true
            holeRadius              = 36f
            transparentCircleRadius = 40f
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(80)
            setUsePercentValues(false) // kita handle sendiri supaya bulat
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(10f)
            setTouchEnabled(true)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val entry = e as? PieEntry ?: return
                    showBottomSheetDetail(entry.label ?: return)
                }
                override fun onNothingSelected() {}
            })
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        kategoriAdapter = KategoriAnalisisAdapter { item ->
            showBottomSheetDetail(item.nama)
        }
        rvKategori.layoutManager = LinearLayoutManager(requireContext())
        rvKategori.adapter = kategoriAdapter
    }

    // ── Click ─────────────────────────────────────────────────────────────────

    private fun setupClick() {
        setActiveTab(true)

        tabPengeluaran.setOnClickListener {
            viewModel.setTab("PENGELUARAN")
            setActiveTab(true)
            refreshCurrentData()
        }

        tabPemasukan.setOnClickListener {
            viewModel.setTab("PEMASUKAN")
            setActiveTab(false)
            refreshCurrentData()
        }

        layoutFilterTipe.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menu.add(0, 0, 0, "Oleh Kategori")
            popup.menu.add(0, 1, 1, "Oleh Dompet")
            popup.setOnMenuItemClickListener { item ->
                filterMode = if (item.itemId == 1) "dompet" else "kategori"
                tvFilterTipe.text = item.title
                viewModel.setSubTab(filterMode)
                refreshCurrentData()
                true
            }
            popup.show()
        }

        btnPrevBulan.setOnClickListener { viewModel.prevBulan() }
        btnNextBulan.setOnClickListener { viewModel.nextBulan() }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private fun observeViewModel() {

        // Label bulan update
        viewModel.currentCal.observe(viewLifecycleOwner) {
            val label = viewModel.getLabelBulan()
            tvBulanTahun.text = label
            tvBulanLabel.text = label
        }

        // Kategori pengeluaran
        viewModel.kategoriSummaryPengeluaran.observe(viewLifecycleOwner) { list ->
            if (viewModel.activeTab.value == "PENGELUARAN" && filterMode == "kategori") {
                updateUI(list, viewModel.totalPengeluaran.value ?: 0.0)
            }
        }

        // Kategori pemasukan
        viewModel.kategoriSummaryPemasukan.observe(viewLifecycleOwner) { list ->
            if (viewModel.activeTab.value == "PEMASUKAN" && filterMode == "kategori") {
                updateUI(list, viewModel.totalPemasukan.value ?: 0.0)
            }
        }

        // Total
        viewModel.totalPengeluaran.observe(viewLifecycleOwner) { total ->
            if (viewModel.activeTab.value == "PENGELUARAN") {
                tvTotalNominal.text = formatRupiah(total)
            }
        }
        viewModel.totalPemasukan.observe(viewLifecycleOwner) { total ->
            if (viewModel.activeTab.value == "PEMASUKAN") {
                tvTotalNominal.text = formatRupiah(total)
            }
        }

        // Dompet summary (diupdate setiap allDompet atau transaksi berubah)
        viewModel.allDompet.observe(viewLifecycleOwner) { dompetList ->
            viewModel.rebuildDompetSummary(dompetList)
        }

        viewModel.transaksiPengeluaran.observe(viewLifecycleOwner) {
            viewModel.allDompet.value?.let { d -> viewModel.rebuildDompetSummary(d) }
            if (viewModel.activeTab.value == "PENGELUARAN" && filterMode == "kategori") {
                viewModel.kategoriSummaryPengeluaran.value?.let { list ->
                    updateUI(list, viewModel.totalPengeluaran.value ?: 0.0)
                }
            }
        }

        viewModel.transaksiPemasukan.observe(viewLifecycleOwner) {
            viewModel.allDompet.value?.let { d -> viewModel.rebuildDompetSummary(d) }
            if (viewModel.activeTab.value == "PEMASUKAN" && filterMode == "kategori") {
                viewModel.kategoriSummaryPemasukan.value?.let { list ->
                    updateUI(list, viewModel.totalPemasukan.value ?: 0.0)
                }
            }
        }

        // Dompet summary pengeluaran
        viewModel.dompetSummaryPengeluaran.observe(viewLifecycleOwner) { list ->
            if (viewModel.activeTab.value == "PENGELUARAN" && filterMode == "dompet") {
                updateUI(list, viewModel.totalPengeluaran.value ?: 0.0)
            }
        }

        // Dompet summary pemasukan
        viewModel.dompetSummaryPemasukan.observe(viewLifecycleOwner) { list ->
            if (viewModel.activeTab.value == "PEMASUKAN" && filterMode == "dompet") {
                updateUI(list, viewModel.totalPemasukan.value ?: 0.0)
            }
        }
    }

    // ── Update UI ─────────────────────────────────────────────────────────────

    private fun updateUI(list: List<KategoriSummary>, total: Double) {
        kategoriAdapter.submitList(list)
        updateChart(list)

        val tab = viewModel.activeTab.value ?: "PENGELUARAN"
        tvLabelTotal.text   = if (tab == "PENGELUARAN") "Total Pengeluaran" else "Total Pemasukan"
        tvTotalNominal.text = formatRupiah(total)
        tvBulanLabel.text   = viewModel.getLabelBulan()

        val top = list.firstOrNull()
        tvTerbesarNama.text   = top?.nama ?: "-"
        tvTerbesarPersen.text = top?.let { "${it.persentase.toInt()}% dari total" } ?: "-"
    }

    private fun refreshCurrentData() {
        val tab   = viewModel.activeTab.value ?: "PENGELUARAN"
        val total = if (tab == "PENGELUARAN")
            viewModel.totalPengeluaran.value ?: 0.0
        else
            viewModel.totalPemasukan.value ?: 0.0

        if (filterMode == "kategori") {
            val list = if (tab == "PENGELUARAN")
                viewModel.kategoriSummaryPengeluaran.value ?: emptyList()
            else
                viewModel.kategoriSummaryPemasukan.value ?: emptyList()
            updateUI(list, total)
        } else {
            val list = if (tab == "PENGELUARAN")
                viewModel.dompetSummaryPengeluaran.value ?: emptyList()
            else
                viewModel.dompetSummaryPemasukan.value ?: emptyList()
            updateUI(list, total)
        }
    }

    // ── Chart ─────────────────────────────────────────────────────────────────

    private fun updateChart(list: List<KategoriSummary>) {
        if (list.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "Tidak ada data"
            pieChart.setCenterTextSize(11f)
            pieChart.setCenterTextColor(Color.parseColor("#90A4AE"))
            pieChart.invalidate()
            return
        }

        // Pakai jumlah asli sebagai value, tapi formatter tampilkan persen bulat
        val entries = list.map { PieEntry(it.jumlah.toFloat(), it.nama) }
        val colors  = list.mapIndexed { i, _ ->
            Color.parseColor(pieColors[i % pieColors.size])
        }

        val total = list.sumOf { it.jumlah }.toFloat()

        val dataSet = PieDataSet(entries, "").apply {
            this.colors    = colors
            // Formatter custom: tampilkan persen bulat tanpa desimal
            valueFormatter = object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    val persen = if (total > 0) Math.round(value / total * 100) else 0
                    return "$persen%"
                }
            }
            valueTextColor = Color.WHITE
            valueTextSize  = 10f
            sliceSpace     = 2f
            selectionShift = 5f
        }

        pieChart.apply {
            data = PieData(dataSet)
            centerText = ""
            animateY(600)
            invalidate()
        }
    }

    // ── Bottom Sheet Detail ───────────────────────────────────────────────────

    private fun showBottomSheetDetail(nama: String) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottom_sheet_transaksi, null)

        val tvKategori = v.findViewById<TextView>(R.id.tvBottomKategori)
        val tvBulan    = v.findViewById<TextView>(R.id.tvBottomBulan)
        val tvTotal    = v.findViewById<TextView>(R.id.tvBottomTotal)
        val tvEmpty    = v.findViewById<TextView>(R.id.tvBottomEmpty)
        val rv         = v.findViewById<RecyclerView>(R.id.rvBottomTransaksi)

        tvKategori.text = nama
        tvBulan.text    = viewModel.getLabelBulan()

        val transaksiAdapter = TransaksiAnalisisAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = transaksiAdapter

        viewModel.selectKategori(nama)
        viewModel.transaksiByKategori.observe(viewLifecycleOwner) { list ->
            val tab      = viewModel.activeTab.value ?: "PENGELUARAN"
            val filtered = list.filter { it.jenis == tab }
            transaksiAdapter.submitList(filtered)
            val total = filtered.sumOf { it.nominal }
            tvTotal.text = formatRupiah(total)
            tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility      = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        }

        dialog.setContentView(v)
        dialog.setOnDismissListener { viewModel.selectKategori(null) }
        dialog.show()
    }

    // ── Tab Styling ───────────────────────────────────────────────────────────

    private fun setActiveTab(isPengeluaran: Boolean) {
        val activeColor   = ContextCompat.getColor(requireContext(), R.color.tab_active_bg)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.tab_inactive_bg)
        val activeText    = ContextCompat.getColor(requireContext(), R.color.tab_active_text)
        val inactiveText  = ContextCompat.getColor(requireContext(), R.color.tab_inactive_text)

        if (isPengeluaran) {
            tabPengeluaran.setBackgroundColor(activeColor)
            tabPengeluaran.setTextColor(activeText)
            tabPemasukan.setBackgroundColor(inactiveColor)
            tabPemasukan.setTextColor(inactiveText)
        } else {
            tabPemasukan.setBackgroundColor(activeColor)
            tabPemasukan.setTextColor(activeText)
            tabPengeluaran.setBackgroundColor(inactiveColor)
            tabPengeluaran.setTextColor(inactiveText)
        }
    }

    private fun formatRupiah(amount: Double): String =
        "Rp ${String.format("%,.0f", amount).replace(',', '.')}"
}
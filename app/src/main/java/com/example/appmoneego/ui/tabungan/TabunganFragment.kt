package com.example.appmoneego.ui.tabungan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoneego.R
import com.example.appmoneego.adapter.TabunganAdapter
import com.example.appmoneego.databinding.FragmentTabunganBinding
import com.example.appmoneego.ui.hutang.HutangViewModel
import com.example.appmoneego.ui.hutang.sisaHutang
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.VisibilityPrefs
import com.google.android.material.tabs.TabLayout

class TabunganFragment : Fragment() {

    private var _binding: FragmentTabunganBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel:       TabunganViewModel
    private lateinit var hutangViewModel: HutangViewModel
    private lateinit var adapter:         TabunganAdapter

    // 0 = Berjalan, 1 = Selesai
    private var tabAktif = 0

    // ── State privasi saldo (sama persis dengan DompetFragment) ──────────────
    private var nominalVisible = true
    private var totalTerkumpul = 0.0   // simpan nilai terakhir agar bisa di-mask/unmask

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabunganBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Baca state privasi dari SharedPreferences — sama seperti Dompet
        nominalVisible = VisibilityPrefs.isNominalVisible(requireContext())

        viewModel       = ViewModelProvider(this)[TabunganViewModel::class.java]
        hutangViewModel = ViewModelProvider(this)[HutangViewModel::class.java]

        setupAdapter()
        setupTabListener()
        setupObservers()
        setupHutangBanner()
        setupClickListeners()
        setupTombolMata()   // ← setup icon mata
        syncIkonMata()      // ← sinkron ikon awal
    }

    // ── onResume: sinkron ulang state privasi saat kembali ke halaman ────────
    override fun onResume() {
        super.onResume()
        nominalVisible = VisibilityPrefs.isNominalVisible(requireContext())
        syncIkonMata()
        renderNominal()   // tampilkan ulang dengan state terbaru
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    private fun setupAdapter() {
        adapter = TabunganAdapter(
            onTabungClick = { tabungan ->
                DetailTabunganBottomSheet(
                    tabungan  = tabungan,
                    onUpdated = { viewModel.update(it) },
                    onDeleted = { viewModel.delete(it) }
                ).show(parentFragmentManager, "DetailTabungan")
            },
            onItemClick = { tabungan ->
                DetailTabunganBottomSheet(
                    tabungan  = tabungan,
                    onUpdated = { viewModel.update(it) },
                    onDeleted = { viewModel.delete(it) }
                ).show(parentFragmentManager, "DetailTabungan")
            }
        )
        binding.rvTabungan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTabungan.adapter = adapter
    }

    // ── Tab Listener (TabLayout, identik dengan HutangFragment) ──────────────
    private fun setupTabListener() {
        binding.tabLayoutTabungan.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tabAktif = tab?.position ?: 0
                viewModel.tabunganList.value?.let { refreshList(it) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ── Empty State ───────────────────────────────────────────────────────────
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.rvTabungan.visibility          = if (isEmpty) View.GONE    else View.VISIBLE
        binding.layoutEmptyTabungan.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun refreshList(list: List<com.example.appmoneego.data.entity.Tabungan>) {
        val berjalan = list.filter { it.terkumpul < it.targetNominal }
        val selesai  = list.filter { it.terkumpul >= it.targetNominal }
        val tampil   = if (tabAktif == 0) berjalan else selesai
        adapter.submitList(tampil)
        updateEmptyState(tampil.isEmpty())
    }

    // ── Observers ─────────────────────────────────────────────────────────────
    private fun setupObservers() {
        viewModel.tabunganList.observe(viewLifecycleOwner) { list ->
            // Simpan nilai untuk keperluan mask/unmask
            totalTerkumpul = list.sumOf { it.terkumpul }

            val tercapai = list.count { it.terkumpul >= it.targetNominal }
            val berjalan = list.count { it.terkumpul < it.targetNominal }

            // ── Nominal: tampil atau sembunyikan sesuai state ─────────────────
            renderNominal()

            // ── Subtitle: "Total Terkumpul dari X Impian" ─────────────────────
            binding.tvJumlahImpian.text = "Total Terkumpul dari ${list.size} Impian"

            // ── Stat: angka saja, tanpa kata "Target" ─────────────────────────
            binding.tvTercapai.text = tercapai.toString()
            binding.tvBerjalan.text = berjalan.toString()

            // ── Banner motivasi ───────────────────────────────────────────────
            val closest = list
                .filter { it.terkumpul < it.targetNominal }
                .minByOrNull { it.targetNominal - it.terkumpul }
            binding.tvInsightTabungan.text = if (closest != null) {
                val sisa = closest.targetNominal - closest.terkumpul
                "Target '${closest.nama}' sisa ${CurrencyFormatter.format(sisa)} lagi! Selesaikan yuk?"
            } else {
                "Semangat menabung untuk mencapai impianmu!"
            }

            refreshList(list)
        }
    }

    // ── Hutang Banner ─────────────────────────────────────────────────────────
    private fun setupHutangBanner() {
        hutangViewModel.hutangList.observe(viewLifecycleOwner) { list ->
            val aktif = list.filter { !it.selesai }
            if (aktif.isEmpty()) {
                binding.cardInfoHutang.visibility = View.GONE
            } else {
                val totalHutang = aktif.sumOf { it.sisaHutang.toDouble() }
                binding.tvInfoHutang.text =
                    "${aktif.size} hutang · Total ${CurrencyFormatter.format(totalHutang)}"
                binding.cardInfoHutang.visibility = View.VISIBLE
            }
        }
    }

    // ── Tombol Mata (logic identik DompetFragment.setupTombolMata) ────────────
    private fun setupTombolMata() {
        binding.ivToggleSaldoTabungan.setOnClickListener {
            nominalVisible = !nominalVisible
            // Simpan ke SharedPreferences (shared state, sama seperti Dompet)
            VisibilityPrefs.setNominalVisible(requireContext(), nominalVisible)
            syncIkonMata()
            renderNominal()
        }
    }

    // Sinkron ikon mata (ic_eye / ic_eye_off) — identik dengan DompetFragment
    private fun syncIkonMata() {
        binding.ivToggleSaldoTabungan.setImageResource(
            if (nominalVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
        )
    }

    // Render nominal: tampil angka asli atau "Rp ***" sesuai state
    private fun renderNominal() {
        binding.tvTotalTerkumpul.text = if (nominalVisible)
            CurrencyFormatter.format(totalTerkumpul)
        else
            "Rp ***"
    }

    // ── Click Listeners ───────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.tvLihatHutang.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.fabTambahTabungan.setOnClickListener {
            TambahTabunganDialog { tabungan ->
                viewModel.insert(tabungan)
            }.show(parentFragmentManager, "TambahTabungan")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
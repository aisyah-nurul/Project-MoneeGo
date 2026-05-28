package com.example.appmoneego.ui.tabungan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoneego.adapter.TabunganAdapter
import com.example.appmoneego.databinding.FragmentTabunganBinding
import com.example.appmoneego.ui.hutang.HutangViewModel
import com.example.appmoneego.ui.hutang.sisaHutang
import com.example.appmoneego.utils.CurrencyFormatter

class TabunganFragment : Fragment() {

    private var _binding: FragmentTabunganBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TabunganViewModel
    private lateinit var hutangViewModel: HutangViewModel
    private lateinit var adapter: TabunganAdapter
    private var tabAktif = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabunganBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel       = ViewModelProvider(this)[TabunganViewModel::class.java]
        hutangViewModel = ViewModelProvider(this)[HutangViewModel::class.java]
        setupAdapter()
        setupObservers()
        setupHutangBanner()
        setupClickListeners()
    }

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

    private fun refreshList(list: List<com.example.appmoneego.data.entity.Tabungan>) {
        val berjalan = list.filter { it.terkumpul < it.targetNominal }
        val selesai  = list.filter { it.terkumpul >= it.targetNominal }
        adapter.submitList(if (tabAktif == 0) berjalan else selesai)
    }

    private fun setupObservers() {
        viewModel.tabunganList.observe(viewLifecycleOwner) { list ->
            val totalTerkumpul = list.sumOf { it.terkumpul }
            val tercapai = list.count { it.terkumpul >= it.targetNominal }
            val berjalan = list.count { it.terkumpul < it.targetNominal }

            binding.tvTotalTerkumpul.text = CurrencyFormatter.format(totalTerkumpul)
            binding.tvJumlahImpian.text   = "Total Terkumpul dari ${list.size} Impian"
            binding.tvTercapai.text       = "${tercapai.toString().padStart(2, '0')} Target"
            binding.tvBerjalan.text       = "${berjalan.toString().padStart(2, '0')} Target"

            val closest = list
                .filter { it.terkumpul < it.targetNominal }
                .minByOrNull { it.targetNominal - it.terkumpul }
            binding.tvInsightTabungan.text = if (closest != null) {
                val sisa = closest.targetNominal - closest.terkumpul
                "Target '${closest.nama}' sisa ${CurrencyFormatter.format(sisa)} lagi! Selesaikan yuk?"
            } else "Semangat menabung!"

            refreshList(list)
        }
    }

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

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.tvLihatHutang.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnTabBerjalan.setOnClickListener {
            tabAktif = 0
            binding.btnTabBerjalan.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2C3E6B"))
            binding.btnTabBerjalan.setTextColor(android.graphics.Color.WHITE)
            binding.btnTabSelesai.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            binding.btnTabSelesai.setTextColor(android.graphics.Color.parseColor("#4A6FA5"))
            viewModel.tabunganList.value?.let { refreshList(it) }
        }

        binding.btnTabSelesai.setOnClickListener {
            tabAktif = 1
            binding.btnTabSelesai.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2C3E6B"))
            binding.btnTabSelesai.setTextColor(android.graphics.Color.WHITE)
            binding.btnTabBerjalan.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            binding.btnTabBerjalan.setTextColor(android.graphics.Color.parseColor("#4A6FA5"))
            viewModel.tabunganList.value?.let { refreshList(it) }
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
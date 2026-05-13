package com.example.appmoneego.ui.tabungan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Tabungan
import com.example.appmoneego.databinding.FragmentTabunganBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class TabunganFragment : Fragment() {

    private var _binding: FragmentTabunganBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TabunganViewModel
    private lateinit var adapter: TabunganAdapter
    private var tabAktif = 0 // 0 = Berjalan, 1 = Selesai

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabunganBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sembunyikan bottom nav
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            ?.visibility = View.GONE

        viewModel = ViewModelProvider(this)[TabunganViewModel::class.java]
        setupAdapter()
        setupObservers()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Tampilkan kembali bottom nav saat keluar
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            ?.visibility = View.VISIBLE
        _binding = null
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

    private fun refreshList(list: List<Tabungan>) {
        val berjalan = list.filter { it.terkumpul < it.targetNominal }
        val selesai  = list.filter { it.terkumpul >= it.targetNominal }
        val tampil   = if (tabAktif == 0) berjalan else selesai

        adapter.submitList(tampil)
        binding.layoutEmpty.visibility = if (tampil.isEmpty()) View.VISIBLE else View.GONE
        binding.rvTabungan.visibility  = if (tampil.isEmpty()) View.GONE   else View.VISIBLE
    }

    private fun setupObservers() {
        viewModel.tabunganList.observe(viewLifecycleOwner) { list ->
            val totalTerkumpul = list.sumOf { it.terkumpul }
            val tercapai = list.count { it.terkumpul >= it.targetNominal }
            val berjalan = list.count { it.terkumpul < it.targetNominal }

            binding.tvTotalTerkumpul.text = formatRupiah(totalTerkumpul)
            binding.tvJumlahImpian.text   = "Total Terkumpul dari ${list.size} Impian"
            binding.tvTercapai.text       = "${tercapai.toString().padStart(2, '0')} Target"
            binding.tvBerjalan.text       = "${berjalan.toString().padStart(2, '0')} Target"

            val closestTarget = list
                .filter { it.terkumpul < it.targetNominal }
                .minByOrNull { it.targetNominal - it.terkumpul }

            binding.tvInsightTabungan.text = if (closestTarget != null) {
                val sisa = closestTarget.targetNominal - closestTarget.terkumpul
                "Target '${closestTarget.nama}' sisa ${formatRupiah(sisa)} lagi! Selesaikan yuk?"
            } else {
                "Semangat menabung! 🎯"
            }

            refreshList(list)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnTabBerjalan.setOnClickListener {
            tabAktif = 0
            setTabStyle(0)
            viewModel.tabunganList.value?.let { refreshList(it) }
        }

        binding.btnTabSelesai.setOnClickListener {
            tabAktif = 1
            setTabStyle(1)
            viewModel.tabunganList.value?.let { refreshList(it) }
        }

        binding.fabTambahTabungan.setOnClickListener {
            TambahTabunganDialog { tabungan ->
                viewModel.insert(tabungan)
            }.show(parentFragmentManager, "TambahTabungan")
        }
    }

    private fun setTabStyle(aktif: Int) {
        val warnaPrimer    = android.graphics.Color.parseColor("#2C3E6B")
        val warnaSecondary = android.graphics.Color.parseColor("#4A6FA5")
        val warnaWhite     = android.graphics.Color.WHITE

        if (aktif == 0) {
            binding.btnTabBerjalan.backgroundTintList =
                android.content.res.ColorStateList.valueOf(warnaPrimer)
            binding.btnTabBerjalan.setTextColor(warnaWhite)
            binding.btnTabSelesai.backgroundTintList =
                android.content.res.ColorStateList.valueOf(warnaWhite)
            binding.btnTabSelesai.setTextColor(warnaSecondary)
        } else {
            binding.btnTabSelesai.backgroundTintList =
                android.content.res.ColorStateList.valueOf(warnaPrimer)
            binding.btnTabSelesai.setTextColor(warnaWhite)
            binding.btnTabBerjalan.backgroundTintList =
                android.content.res.ColorStateList.valueOf(warnaWhite)
            binding.btnTabBerjalan.setTextColor(warnaSecondary)
        }
    }

    private fun formatRupiah(value: Double): String =
        "Rp${String.format("%,.0f", value).replace(",", ".")}"
}
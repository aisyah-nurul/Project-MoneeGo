package com.example.appmoneego.ui.hutang

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoneego.databinding.FragmentHutangBinding
import com.example.appmoneego.hutang.CicilanBottomSheetFragment
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.R
import com.google.android.material.tabs.TabLayout

class HutangFragment : Fragment() {

    private var _binding: FragmentHutangBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HutangViewModel
    private lateinit var adapter: HutangAdapter
    private var isNominalVisible = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHutangBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HutangViewModel::class.java]
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = HutangAdapter(
            lifecycleScope = viewLifecycleOwner.lifecycleScope
        ) { hutang ->
            // Buka bottom sheet cicilan saat kartu diklik
            val sheet = CicilanBottomSheetFragment.newInstance(hutang)
            sheet.setOnCicilanSavedListener { updated ->
                viewModel.update(updated)
            }
            sheet.show(parentFragmentManager, "CicilanSheet")
        }
        binding.rvHutang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHutang.adapter = adapter
    }

    private fun refreshList() {
        val list     = viewModel.hutangList.value ?: return
        val aktif    = list.filter { !it.selesai }
        val lunas    = list.filter { it.selesai }
        val tabIndex = binding.tabLayoutHutang.selectedTabPosition
        val tampil   = if (tabIndex == 0) aktif else lunas
        adapter.submitList(tampil)
        binding.layoutEmpty.visibility = if (tampil.isEmpty()) View.VISIBLE else View.GONE
        binding.rvHutang.visibility    = if (tampil.isEmpty()) View.GONE   else View.VISIBLE
    }

    private fun setupObservers() {
        viewModel.hutangList.observe(viewLifecycleOwner) { list ->
            val aktif = list.filter { !it.selesai }
            val lunas = list.filter { it.selesai }
            val total = aktif.sumOf { it.sisaHutang }

            binding.tvTotalHutang.text  = CurrencyFormatter.format(total.toDouble())
            binding.tvStatBerjalan.text = aktif.size.toString()
            binding.tvStatLunas.text    = lunas.size.toString()

            refreshList()
        }

        binding.tabLayoutHutang.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { refreshList() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnTambahHutang.setOnClickListener {
            TambahHutangDialog { hutang ->
                viewModel.insert(hutang)
            }.show(parentFragmentManager, "TambahHutangDialog")
        }
        binding.ibBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.ivEye.setOnClickListener {
            isNominalVisible = !isNominalVisible
            if (isNominalVisible) {
                binding.ivEye.setImageResource(R.drawable.ic_eye)
                val total = viewModel.hutangList.value
                    ?.filter { !it.selesai }
                    ?.sumOf { it.sisaHutang } ?: 0L
                binding.tvTotalHutang.text = CurrencyFormatter.format(total.toDouble())
            } else {
                binding.ivEye.setImageResource(R.drawable.ic_eye_off)
                binding.tvTotalHutang.text = "Rp ••••••"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
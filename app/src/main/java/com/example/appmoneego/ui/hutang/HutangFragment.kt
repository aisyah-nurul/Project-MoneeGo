package com.example.appmoneego.ui.hutang

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoneego.R
import com.example.appmoneego.databinding.FragmentHutangBinding
import com.example.appmoneego.model.Hutang
import java.text.NumberFormat
import java.util.*

class HutangFragment : Fragment() {

    private var _binding: FragmentHutangBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HutangAdapter
    private val hutangList = mutableListOf<Hutang>()

    // Tab: true = BERJALAN, false = SELESAI
    private var tabBerjalan = true

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
        setupRecyclerView()
        setupClickListeners()
        updateSummary()
        refreshList()
    }

    private fun setupRecyclerView() {
        adapter = HutangAdapter(
            items = mutableListOf(),
            onItemClick = { _ -> },
            onLunasChanged = { hutang, isLunas ->
                val index = hutangList.indexOfFirst { it.id == hutang.id }
                if (index != -1) {
                    hutangList[index] = hutang.copy(lunas = isLunas)
                    updateSummary()
                    refreshList()
                }
            }
        )
        binding.rvHutang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHutang.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnTambah.setOnClickListener {
            TambahHutangDialog { hutang ->
                hutangList.add(0, hutang)
                updateSummary()
                // Tambah hutang baru → otomatis ke tab berjalan
                if (!tabBerjalan) switchTab(true)
                else refreshList()
            }.show(parentFragmentManager, "TambahHutangDialog")
        }

        binding.tabBerjalan.setOnClickListener { switchTab(true) }
        binding.tabSelesai.setOnClickListener { switchTab(false) }

        binding.tvLunasBulanIni.setOnClickListener {
            switchTab(false)
        }
    }

    private fun switchTab(toBerjalan: Boolean) {
        tabBerjalan = toBerjalan

        if (toBerjalan) {
            binding.tabBerjalan.setBackgroundResource(R.drawable.bg_jenis_selected)
            binding.tabBerjalan.setTextColor(android.graphics.Color.WHITE)
            binding.tabSelesai.setBackgroundResource(R.drawable.bg_card_white_rounded)
            binding.tabSelesai.setTextColor(android.graphics.Color.parseColor("#888888"))
        } else {
            binding.tabSelesai.setBackgroundResource(R.drawable.bg_jenis_selected)
            binding.tabSelesai.setTextColor(android.graphics.Color.WHITE)
            binding.tabBerjalan.setBackgroundResource(R.drawable.bg_card_white_rounded)
            binding.tabBerjalan.setTextColor(android.graphics.Color.parseColor("#888888"))
        }

        refreshList()
    }

    private fun refreshList() {
        val filtered = if (tabBerjalan)
            hutangList.filter { !it.lunas }.toMutableList()
        else
            hutangList.filter { it.lunas }.toMutableList()

        // Update adapter items
        val adapterItems = (binding.rvHutang.adapter as HutangAdapter)
            .let {
                // rebuild adapter dengan list baru
                adapter = HutangAdapter(
                    items = filtered,
                    onItemClick = { _ -> },
                    onLunasChanged = { hutang, isLunas ->
                        val index = hutangList.indexOfFirst { it.id == hutang.id }
                        if (index != -1) {
                            hutangList[index] = hutang.copy(lunas = isLunas)
                            updateSummary()
                            refreshList()
                        }
                    }
                )
                binding.rvHutang.adapter = adapter
            }

        if (filtered.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvHutang.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvHutang.visibility = View.VISIBLE
        }
    }

    private fun updateSummary() {
        val aktif = hutangList.filter { !it.lunas }
        val total = aktif.sumOf { it.jumlah }
        val lunas = hutangList.count { it.lunas }

        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.tvTotalHutang.text = fmt.format(total).replace("IDR", "Rp").replace(",00", "")
        binding.tvHutangAktif.text = "${aktif.size} Hutang Aktif"
        binding.tvLunasBulanIni.text = lunas.toString()

        val jatuhTempoBaru = aktif
            .filter { it.isJatuhTempoBaru }
            .minByOrNull { it.jatuhTempo?.time ?: Long.MAX_VALUE }

        if (jatuhTempoBaru?.jatuhTempo != null) {
            val cal = Calendar.getInstance()
            cal.time = jatuhTempoBaru.jatuhTempo
            val bulan = arrayOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
            binding.tvJatuhTempo.text = "${cal.get(Calendar.DAY_OF_MONTH)} ${bulan[cal.get(Calendar.MONTH)]}"
        } else {
            binding.tvJatuhTempo.text = "-"
        }

        binding.tvDompetAktif.text = "5"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
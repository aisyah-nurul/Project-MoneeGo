package com.example.appmoneego.ui.riwayattransaksi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoneego.adapter.TransaksiAdapter
import com.example.appmoneego.databinding.FragmentRiwayatTransaksiBinding
import com.example.appmoneego.viewmodel.TransaksiViewModel
import java.text.SimpleDateFormat
import java.util.*

class RiwayatTransaksiFragment : Fragment() {

    private var _binding: FragmentRiwayatTransaksiBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransaksiViewModel by viewModels()
    private lateinit var adapter: TransaksiAdapter
    private var kalenderVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup adapter
        adapter = TransaksiAdapter()
        binding.rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransaksi.adapter = adapter

        // tanggal hari ini di header
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id"))
        binding.tvTanggalHeader.text = sdf.format(Date())

        // observe semua transaksi
        viewModel.allTransaksi.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)

            // hitung ratio pemasukan vs pengeluaran
            val totalMasuk = list.filter { it.jenis == "PEMASUKAN" }.sumOf { it.nominal }
            val totalKeluar = list.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
            val total = totalMasuk + totalKeluar

            if (total > 0) {
                val persenMasuk = ((totalMasuk / total) * 100).toInt()
                val persenKeluar = 100 - persenMasuk
                binding.progressRatio.progress = persenMasuk
                binding.tvPersenMasuk.text = " Pemasukan $persenMasuk%"
                binding.tvPersenKeluar.text = " Pengeluaran $persenKeluar%"

                binding.tvJenisFilter.text = if (totalKeluar >= totalMasuk)
                    "Pengeluaran" else "Pemasukan"
            }
        }

        // toggle kalender
        binding.btnToggleKalender.setOnClickListener {
            kalenderVisible = !kalenderVisible
            binding.cardKalender.visibility =
                if (kalenderVisible) View.VISIBLE else View.GONE
        }

        // filter transaksi berdasarkan tanggal yang dipilih di kalender
        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day, 0, 0, 0)
            val startOfDay = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val endOfDay = cal.timeInMillis

            viewModel.getByDateRange(startOfDay, endOfDay)
                .observe(viewLifecycleOwner) { list ->
                    adapter.submitList(list)
                }

            // update tanggal di header
            val sdfHeader = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id"))
            binding.tvTanggalHeader.text = sdfHeader.format(Date(startOfDay))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
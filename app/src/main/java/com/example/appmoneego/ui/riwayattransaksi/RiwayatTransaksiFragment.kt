package com.example.appmoneego.ui.riwayattransaksi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoneego.R
import com.example.appmoneego.adapter.TransaksiAdapter
import com.example.appmoneego.databinding.FragmentRiwayatTransaksiBinding
import com.example.appmoneego.utils.DateUtils
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.TransaksiViewModel
import java.util.*

class RiwayatTransaksiFragment : Fragment() {

    private var _binding: FragmentRiwayatTransaksiBinding? = null
    private val binding get() = _binding!!

    private val viewModel:       TransaksiViewModel by viewModels()
    private val dompetViewModel: DompetViewModel    by viewModels()

    private lateinit var adapter: TransaksiAdapter
    private var kalenderVisible     = false
    private var selectedDateMillis: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupKalender()
        observeDompet()
        loadTransaksiByTanggal(selectedDateMillis)
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = TransaksiAdapter(
            onEditClick = { transaksi ->
                val bundle = Bundle().apply {
                    putInt("edit_id",          transaksi.id)
                    putDouble("edit_nominal",  transaksi.nominal)
                    putString("edit_jenis",    transaksi.jenis)
                    putString("edit_kategori", transaksi.kategori)
                    putString("edit_catatan",  transaksi.catatan)
                    putLong("edit_tanggal",    transaksi.tanggal)
                    putInt("edit_dompet_id",   transaksi.dompetId)
                }
                findNavController().navigate(R.id.action_riwayat_to_tambahTransaksi, bundle)
            },
            onDeleteClick = { transaksi ->
                // Dialog konfirmasi hapus
                AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Transaksi")
                    .setMessage("Yakin ingin menghapus \"${transaksi.catatan.ifEmpty { transaksi.kategori }}\"?")
                    .setPositiveButton("Hapus") { _, _ -> viewModel.delete(transaksi) }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )
        binding.rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransaksi.adapter       = adapter
    }

    private fun observeDompet() {
        dompetViewModel.allDompet.observe(viewLifecycleOwner) { dompetList ->
            adapter.submitDompet(dompetList)
        }
    }

    // ── Load transaksi ────────────────────────────────────────────────────────

    private fun loadTransaksiByTanggal(timeMillis: Long) {
        val start = DateUtils.getStartOfDay(timeMillis)
        val end   = DateUtils.getEndOfDay(timeMillis)

        binding.tvTanggalHeader.text = DateUtils.formatTanggal(timeMillis)

        viewModel.getByDateRange(start, end).observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)

            val totalMasuk  = list.filter { it.jenis == "PEMASUKAN"   }.sumOf { it.nominal }
            val totalKeluar = list.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
            val total       = totalMasuk + totalKeluar

            if (total > 0) {
                val persenMasuk  = ((totalMasuk / total) * 100).toInt()
                val persenKeluar = 100 - persenMasuk
                binding.progressRatio.progress = persenMasuk
                binding.tvPersenMasuk.text      = " Pemasukan $persenMasuk%"
                binding.tvPersenKeluar.text     = " Pengeluaran $persenKeluar%"
                binding.tvJenisFilter.text      =
                    if (totalKeluar >= totalMasuk) "Pengeluaran" else "Pemasukan"
            } else {
                binding.progressRatio.progress = 50
                binding.tvPersenMasuk.text      = " Pemasukan 0%"
                binding.tvPersenKeluar.text     = " Pengeluaran 0%"
                binding.tvJenisFilter.text      = "Pengeluaran"
            }
        }
    }

    // ── Kalender ──────────────────────────────────────────────────────────────

    private fun setupKalender() {
        binding.btnToggleKalender.setOnClickListener {
            kalenderVisible = !kalenderVisible
            binding.cardKalender.visibility =
                if (kalenderVisible) View.VISIBLE else View.GONE
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            selectedDateMillis = cal.timeInMillis
            kalenderVisible    = false
            binding.cardKalender.visibility = View.GONE
            loadTransaksiByTanggal(selectedDateMillis)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
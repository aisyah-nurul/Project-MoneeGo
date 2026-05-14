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
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.DateUtils
import com.example.appmoneego.utils.VisibilityPrefs
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.TransaksiViewModel
import java.util.*

class RiwayatTransaksiFragment : Fragment() {

    private var _binding: FragmentRiwayatTransaksiBinding? = null
    private val binding get() = _binding!!

    private val viewModel:       TransaksiViewModel by viewModels()
    private val dompetViewModel: DompetViewModel    by viewModels()

    private lateinit var adapter: TransaksiAdapter

    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var kalenderVisible = false

    private var nominalVisible: Boolean = true

    private var lastTotalMasuk  = 0.0
    private var lastTotalKeluar = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nominalVisible = VisibilityPrefs.isNominalVisible(requireContext())

        setupAdapter()
        setupKalender()
        setupNavigasiTanggal()
        setupTombolMata()
        observeDompet()
        loadTransaksiByTanggal(selectedDateMillis)

        syncIkonMata()
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = TransaksiAdapter(
            nominalVisibleInit = nominalVisible,

            // ✅ BARU: tap card → buka popup detail
            onItemClick = { transaksi ->
                val dialog = DetailTransaksiDialog(
                    transaksi    = transaksi,
                    daftarDompet = adapter.getDaftarDompet(),
                    onEditClick  = { t ->
                        val isTransfer = t.kategori == "Transfer"
                        val jenisEdit  = if (isTransfer) "TRANSFER" else t.jenis

                        val bundle = Bundle().apply {
                            putInt("edit_id",              t.id)
                            putDouble("edit_nominal",      t.nominal)
                            putString("edit_jenis",        jenisEdit)
                            putString("edit_kategori",     t.kategori)
                            putString("edit_catatan",      t.catatan)
                            putLong("edit_tanggal",        t.tanggal)
                            putInt("edit_dompet_id",       t.dompetId)
                            putBoolean("edit_is_transfer", isTransfer)
                        }
                        findNavController()
                            .navigate(R.id.action_riwayat_to_tambahTransaksi, bundle)
                    },
                    onDeleteClick = { t ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Hapus Transaksi")
                            .setMessage(
                                "Yakin ingin menghapus " +
                                        "\"${t.catatan.ifEmpty { t.kategori }}\"?"
                            )
                            .setPositiveButton("Hapus") { _, _ -> viewModel.delete(t) }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                )
                dialog.show(parentFragmentManager, DetailTransaksiDialog.TAG)
            },

            // onEditClick & onDeleteClick di sini tidak dipakai lagi
            // (sudah dipindah ke dalam DetailTransaksiDialog),
            // tapi tetap ada agar signature tidak berubah
            onEditClick   = {},
            onDeleteClick = {}
        )

        binding.rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransaksi.adapter       = adapter
        binding.rvTransaksi.setHasFixedSize(false)
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

            lastTotalMasuk  = list.filter { it.jenis == "PEMASUKAN"   }.sumOf { it.nominal }
            lastTotalKeluar = list.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
            val total = lastTotalMasuk + lastTotalKeluar

            if (total > 0) {
                val persenMasuk  = ((lastTotalMasuk / total) * 100).toInt()
                val persenKeluar = 100 - persenMasuk
                binding.progressRatio.progress = persenMasuk
                binding.tvPersenMasuk.text      = " Pemasukan $persenMasuk%"
                binding.tvPersenKeluar.text     = " Pengeluaran $persenKeluar%"
            } else {
                binding.progressRatio.progress = 50
                binding.tvPersenMasuk.text      = " Pemasukan 0%"
                binding.tvPersenKeluar.text     = " Pengeluaran 0%"
            }

            updateTotalHarian()
        }
    }

    // ── Total harian ──────────────────────────────────────────────────────────

    private fun updateTotalHarian() {
        if (nominalVisible) {
            binding.tvTotalMasuk.text  = CurrencyFormatter.format(lastTotalMasuk)
            binding.tvTotalKeluar.text = CurrencyFormatter.format(lastTotalKeluar)
        } else {
            binding.tvTotalMasuk.text  = "Rp ***"
            binding.tvTotalKeluar.text = "Rp ***"
        }
    }

    // ── Tombol Mata ───────────────────────────────────────────────────────────

    private fun setupTombolMata() {
        binding.btnToggleMata.setOnClickListener {
            nominalVisible = !nominalVisible
            VisibilityPrefs.setNominalVisible(requireContext(), nominalVisible)
            syncIkonMata()
            adapter.nominalVisible = nominalVisible
            updateTotalHarian()
        }
    }

    private fun syncIkonMata() {
        val iconRes = if (nominalVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
        binding.btnToggleMata.setImageResource(iconRes)
    }

    // ── Navigasi tanggal ──────────────────────────────────────────────────────

    private fun setupNavigasiTanggal() {
        binding.btnPrevTanggal.setOnClickListener {
            selectedDateMillis -= 86_400_000L
            tutupKalender()
            loadTransaksiByTanggal(selectedDateMillis)
        }
        binding.btnNextTanggal.setOnClickListener {
            selectedDateMillis += 86_400_000L
            tutupKalender()
            loadTransaksiByTanggal(selectedDateMillis)
        }
    }

    // ── Kalender ──────────────────────────────────────────────────────────────

    private fun setupKalender() {
        binding.btnToggleKalender.setOnClickListener {
            if (kalenderVisible) tutupKalender() else bukaKalender()
        }
        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            selectedDateMillis = cal.timeInMillis
            tutupKalender()
            loadTransaksiByTanggal(selectedDateMillis)
        }
    }

    private fun bukaKalender() {
        kalenderVisible = true
        binding.cardKalender.visibility = View.VISIBLE
    }

    private fun tutupKalender() {
        kalenderVisible = false
        binding.cardKalender.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
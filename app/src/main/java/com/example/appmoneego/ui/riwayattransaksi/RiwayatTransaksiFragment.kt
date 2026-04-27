package com.example.appmoneego.ui.riwayattransaksi

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.adapter.TransaksiAdapter
import com.example.appmoneego.databinding.FragmentRiwayatTransaksiBinding
import com.example.appmoneego.utils.DateUtils
import com.example.appmoneego.viewmodel.TransaksiViewModel
import java.text.SimpleDateFormat
import java.util.*

class RiwayatTransaksiFragment : Fragment() {

    private var _binding: FragmentRiwayatTransaksiBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransaksiViewModel by viewModels()
    private lateinit var adapter: TransaksiAdapter
    private var kalenderVisible = false

    // State tanggal yang dipilih (default hari ini)
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
        setupSwipeToDelete()
        setupKalender()

        // Load transaksi hari ini saat pertama buka
        loadTransaksiByTanggal(selectedDateMillis)
    }

    // ─── Setup Adapter ────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = TransaksiAdapter()
        binding.rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransaksi.adapter = adapter
    }

    // ─── Load transaksi berdasarkan tanggal ───────────────────────────────────

    private fun loadTransaksiByTanggal(timeMillis: Long) {
        val start = DateUtils.getStartOfDay(timeMillis)
        val end   = DateUtils.getEndOfDay(timeMillis)

        // Update label tanggal di header
        binding.tvTanggalHeader.text = DateUtils.formatTanggal(timeMillis)

        viewModel.getByDateRange(start, end).observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)

            val totalMasuk  = list.filter { it.jenis == "PEMASUKAN" }.sumOf { it.nominal }
            val totalKeluar = list.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
            val total       = totalMasuk + totalKeluar

            if (total > 0) {
                val persenMasuk  = ((totalMasuk / total) * 100).toInt()
                val persenKeluar = 100 - persenMasuk
                binding.progressRatio.progress = persenMasuk
                binding.tvPersenMasuk.text     = " Pemasukan $persenMasuk%"
                binding.tvPersenKeluar.text    = " Pengeluaran $persenKeluar%"
                binding.tvJenisFilter.text     = if (totalKeluar >= totalMasuk)
                    "Pengeluaran" else "Pemasukan"
            } else {
                binding.progressRatio.progress = 50
                binding.tvPersenMasuk.text     = " Pemasukan 0%"
                binding.tvPersenKeluar.text    = " Pengeluaran 0%"
                binding.tvJenisFilter.text     = "Pengeluaran"
            }
        }
    }

    // ─── Swipe to Delete ──────────────────────────────────────────────────────

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItemAt(position)

                if (item == null) {
                    // Header di-swipe → kembalikan
                    adapter.notifyItemChanged(position)
                    return
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Transaksi")
                    .setMessage("Yakin ingin menghapus transaksi \"${item.catatan.ifEmpty { item.kategori }}\"?")
                    .setPositiveButton("Hapus") { _, _ ->
                        viewModel.delete(item)
                    }
                    .setNegativeButton("Batal") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            // Background merah + teks "Hapus" saat swipe
            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint    = Paint()

                paint.color = Color.parseColor("#E53935")
                val background = RectF(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRoundRect(background, 16f, 16f, paint)

                paint.color     = Color.WHITE
                paint.textSize  = 40f
                paint.textAlign = Paint.Align.CENTER
                val textX = itemView.right - 80f
                val textY = (itemView.top + itemView.bottom) / 2f + 14f
                c.drawText("Hapus", textX, textY, paint)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            // Header tidak bisa di-swipe
            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder is TransaksiAdapter.HeaderViewHolder) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvTransaksi)
    }

    // ─── Kalender ─────────────────────────────────────────────────────────────

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

            // Sembunyikan kalender setelah pilih
            kalenderVisible = false
            binding.cardKalender.visibility = View.GONE

            loadTransaksiByTanggal(selectedDateMillis)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
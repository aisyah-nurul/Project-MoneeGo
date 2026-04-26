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

        setupAdapter()
        setupObserver()
        setupSwipeToDelete()
        setupKalender()

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id"))
        binding.tvTanggalHeader.text = sdf.format(Date())
    }

    // ─── Setup Adapter ────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = TransaksiAdapter()
        binding.rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransaksi.adapter = adapter
    }

    // ─── Observer ─────────────────────────────────────────────────────────────

    private fun setupObserver() {
        viewModel.allTransaksi.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)

            val totalMasuk  = list.filter { it.jenis == "PEMASUKAN" }.sumOf { it.nominal }
            val totalKeluar = list.filter { it.jenis == "PENGELUARAN" }.sumOf { it.nominal }
            val total = totalMasuk + totalKeluar

            if (total > 0) {
                val persenMasuk  = ((totalMasuk / total) * 100).toInt()
                val persenKeluar = 100 - persenMasuk
                binding.progressRatio.progress = persenMasuk
                binding.tvPersenMasuk.text     = " Pemasukan $persenMasuk%"
                binding.tvPersenKeluar.text    = " Pengeluaran $persenKeluar%"
                binding.tvJenisFilter.text     = if (totalKeluar >= totalMasuk)
                    "Pengeluaran" else "Pemasukan"
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

                // Cek apakah item yang di-swipe adalah TransaksiItem.Item (bukan Header)
                val item = adapter.getItemAt(position)

                if (item == null) {
                    // Kalau header yang ke-swipe, kembalikan saja
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

            // Background merah + tulisan Hapus saat swipe
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

                super.onChildDraw(
                    c, recyclerView, viewHolder,
                    dX, dY, actionState, isCurrentlyActive
                )
            }

            // Disable swipe untuk Header — hanya Item yang bisa di-swipe
            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // Kalau ViewHolder-nya HeaderViewHolder, tidak bisa di-swipe
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
            val startOfDay = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val endOfDay = cal.timeInMillis

            viewModel.getByDateRange(startOfDay, endOfDay)
                .observe(viewLifecycleOwner) { list ->
                    adapter.submitList(list)
                }

            val sdfHeader = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id"))
            binding.tvTanggalHeader.text = sdfHeader.format(Date(startOfDay))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
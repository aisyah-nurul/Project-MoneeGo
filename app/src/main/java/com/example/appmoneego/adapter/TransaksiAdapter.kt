package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.databinding.ItemHeaderTransaksiBinding
import com.example.appmoneego.databinding.ItemTransaksiBinding
import com.example.appmoneego.utils.CurrencyFormatter

class TransaksiAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // sealed class untuk tipe item di list
    sealed class TransaksiItem {
        data class Header(val label: String, val jenis: String) : TransaksiItem()
        data class Item(val transaksi: Transaksi) : TransaksiItem()
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    private var semuaData: List<Transaksi> = emptyList()
    private var displayList: MutableList<TransaksiItem> = mutableListOf()

    // lacak mana header yang di-collapse
    private val collapsedHeaders = mutableSetOf<String>()

    fun submitList(list: List<Transaksi>) {
        semuaData = list
        rebuildDisplayList()
    }

    private fun rebuildDisplayList() {
        displayList.clear()

        val pemasukan = semuaData.filter { it.jenis == "PEMASUKAN" }
        val pengeluaran = semuaData.filter { it.jenis == "PENGELUARAN" }

        // tambah header Pemasukan
        if (pemasukan.isNotEmpty()) {
            displayList.add(TransaksiItem.Header("Pemasukan", "PEMASUKAN"))
            // tambah item hanya kalau header tidak di-collapse
            if (!collapsedHeaders.contains("PEMASUKAN")) {
                pemasukan.forEach { displayList.add(TransaksiItem.Item(it)) }
            }
        }

        // tambah header Pengeluaran
        if (pengeluaran.isNotEmpty()) {
            displayList.add(TransaksiItem.Header("Pengeluaran", "PENGELUARAN"))
            if (!collapsedHeaders.contains("PENGELUARAN")) {
                pengeluaran.forEach { displayList.add(TransaksiItem.Item(it)) }
            }
        }

        notifyDataSetChanged()
    }

    // --- ViewHolder Header ---
    inner class HeaderViewHolder(private val binding: ItemHeaderTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TransaksiItem.Header) {
            binding.tvHeaderLabel.text = header.label

            // ubah icon sesuai collapsed/expanded
            val isCollapsed = collapsedHeaders.contains(header.jenis)
            binding.ivCollapse.rotation = if (isCollapsed) -90f else 0f

            // klik header → toggle collapse
            binding.root.setOnClickListener {
                if (isCollapsed) {
                    collapsedHeaders.remove(header.jenis)
                } else {
                    collapsedHeaders.add(header.jenis)
                }
                rebuildDisplayList()
            }
        }
    }

    // --- ViewHolder Item ---
    inner class ItemViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaksi: Transaksi) {
            binding.tvKategori.text = transaksi.kategori
            binding.tvCatatan.text = transaksi.catatan.ifEmpty { "-" }

            val formatted = CurrencyFormatter.format(transaksi.nominal)

            if (transaksi.jenis == "PEMASUKAN") {
                binding.tvNominal.text = "+ Rp. $formatted"
                binding.tvNominal.setTextColor(
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                )
                binding.tvNominalDetail.text = "+ $formatted"
                binding.tvNominalDetail.setTextColor(
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                )
            } else {
                binding.tvNominal.text = "- Rp. $formatted"
                binding.tvNominal.setTextColor(
                    binding.root.context.getColor(android.R.color.holo_red_dark)
                )
                binding.tvNominalDetail.text = "- $formatted"
                binding.tvNominalDetail.setTextColor(
                    binding.root.context.getColor(android.R.color.holo_red_dark)
                )
            }

            // sumber dana dari dompetId (sementara tampilkan sebagai "Bank")
            binding.tvSumberDana.text = "Bank"
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayList[position]) {
            is TransaksiItem.Header -> TYPE_HEADER
            is TransaksiItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemHeaderTransaksiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemTransaksiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ItemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is TransaksiItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TransaksiItem.Item -> (holder as ItemViewHolder).bind(item.transaksi)
        }
    }

    override fun getItemCount() = displayList.size
}
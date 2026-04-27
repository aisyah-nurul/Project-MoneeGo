package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.databinding.ItemHeaderTransaksiBinding
import com.example.appmoneego.databinding.ItemTransaksiBinding
import com.example.appmoneego.utils.CurrencyFormatter

class TransaksiAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ── Tipe item ──────────────────────────────────────────────────────────────
    sealed class TransaksiItem {
        data class Header(val label: String, val jenis: String) : TransaksiItem()
        data class Item(val transaksi: Transaksi) : TransaksiItem()
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM   = 1
    }

    private var semuaData: List<Transaksi> = emptyList()
    private var displayList: MutableList<TransaksiItem> = mutableListOf()

    // Header yang di-collapse
    private val collapsedHeaders = mutableSetOf<String>()

    // ── Submit data dengan DiffUtil agar efisien ───────────────────────────────
    fun submitList(list: List<Transaksi>) {
        semuaData = list
        rebuildDisplayList()
    }

    // Ambil Transaksi dari posisi tertentu (null jika Header)
    fun getItemAt(position: Int): Transaksi? {
        return when (val item = displayList.getOrNull(position)) {
            is TransaksiItem.Item   -> item.transaksi
            is TransaksiItem.Header -> null
            null                    -> null
        }
    }

    private fun rebuildDisplayList() {
        val newList = mutableListOf<TransaksiItem>()

        val pemasukan   = semuaData.filter { it.jenis == "PEMASUKAN" }
        val pengeluaran = semuaData.filter { it.jenis == "PENGELUARAN" }

        // Header Pemasukan
        if (pemasukan.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Pemasukan", "PEMASUKAN"))
            if (!collapsedHeaders.contains("PEMASUKAN")) {
                pemasukan.forEach { newList.add(TransaksiItem.Item(it)) }
            }
        }

        // Header Pengeluaran
        if (pengeluaran.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Pengeluaran", "PENGELUARAN"))
            if (!collapsedHeaders.contains("PENGELUARAN")) {
                pengeluaran.forEach { newList.add(TransaksiItem.Item(it)) }
            }
        }

        // DiffUtil agar animasi update lebih smooth
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = displayList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = displayList[oldPos]; val new = newList[newPos]
                if (old is TransaksiItem.Header && new is TransaksiItem.Header)
                    return old.jenis == new.jenis
                if (old is TransaksiItem.Item && new is TransaksiItem.Item)
                    return old.transaksi.id == new.transaksi.id
                return false
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                displayList[oldPos] == newList[newPos]
        })

        displayList = newList
        diff.dispatchUpdatesTo(this)
    }

    // ── ViewHolder Header ──────────────────────────────────────────────────────
    inner class HeaderViewHolder(private val binding: ItemHeaderTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TransaksiItem.Header) {
            binding.tvHeaderLabel.text = header.label

            val isCollapsed = collapsedHeaders.contains(header.jenis)
            binding.ivCollapse.rotation = if (isCollapsed) -90f else 0f

            binding.root.setOnClickListener {
                if (isCollapsed) collapsedHeaders.remove(header.jenis)
                else collapsedHeaders.add(header.jenis)
                rebuildDisplayList()
            }
        }
    }

    // ── ViewHolder Item ────────────────────────────────────────────────────────
    inner class ItemViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaksi: Transaksi) {
            binding.tvKategori.text = transaksi.kategori
            binding.tvCatatan.text  = transaksi.catatan.ifEmpty { "-" }

            val formatted = CurrencyFormatter.format(transaksi.nominal)

            if (transaksi.jenis == "PEMASUKAN") {
                binding.tvNominal.text = "+ $formatted"
                binding.tvNominal.setTextColor(0xFF4CAF50.toInt())      // hijau
                binding.tvNominalDetail.text = "+ $formatted"
                binding.tvNominalDetail.setTextColor(0xFF4CAF50.toInt())
            } else {
                binding.tvNominal.text = "- $formatted"
                binding.tvNominal.setTextColor(0xFFF44336.toInt())      // merah
                binding.tvNominalDetail.text = "- $formatted"
                binding.tvNominalDetail.setTextColor(0xFFF44336.toInt())
            }

            // Sumber dana berdasarkan dompetId
            binding.tvSumberDana.text = when (transaksi.dompetId) {
                2    -> "E-Wallet"
                3    -> "Bank"
                else -> "Tunai"
            }
        }
    }

    // ── Override wajib ─────────────────────────────────────────────────────────
    override fun getItemViewType(position: Int) = when (displayList[position]) {
        is TransaksiItem.Header -> TYPE_HEADER
        is TransaksiItem.Item   -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemHeaderTransaksiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            else -> ItemViewHolder(
                ItemTransaksiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is TransaksiItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TransaksiItem.Item   -> (holder as ItemViewHolder).bind(item.transaksi)
        }
    }

    override fun getItemCount() = displayList.size
}
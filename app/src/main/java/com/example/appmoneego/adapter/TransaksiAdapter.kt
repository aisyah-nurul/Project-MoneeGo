package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.databinding.ItemHeaderTransaksiBinding
import com.example.appmoneego.databinding.ItemTransaksiBinding
import com.example.appmoneego.utils.CurrencyFormatter

class TransaksiAdapter(
    nominalVisibleInit: Boolean = true,
    private val onEditClick:   (Transaksi) -> Unit = {},
    private val onDeleteClick: (Transaksi) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var nominalVisible: Boolean = nominalVisibleInit
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    sealed class TransaksiItem {
        data class Header(val label: String, val jenis: String) : TransaksiItem()
        data class Item(val transaksi: Transaksi) : TransaksiItem()
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM   = 1

        private val KATEGORI_ICON = mapOf(
            "Makanan"           to R.drawable.ic_makanan,
            "Fashion"           to R.drawable.ic_fashion,
            "Transportasi"      to R.drawable.ic_transportasi,
            "Pendidikan"        to R.drawable.ic_pendidikan,
            "Sosial"            to R.drawable.ic_sosial,
            "Kesehatan"         to R.drawable.ic_kesehatan,
            "Rumah Tangga"      to R.drawable.ic_rumahtangga,
            "Kebutuhan Pribadi" to R.drawable.ic_kebutuhanpribadi,
            "Gaji"              to R.drawable.ic_gaji,
            "Bonus"             to R.drawable.ic_bonus,
            "Freelance"         to R.drawable.ic_freelance,
            "Investasi"         to R.drawable.ic_investasi,
            "Hadiah"            to R.drawable.ic_hadiah,
            "Penjualan"         to R.drawable.ic_penjualan,
            "Saldo Awal"        to R.drawable.ic_wallet,
            "Transfer"          to R.drawable.ic_wallet
        )
    }

    private var semuaData:    List<Transaksi> = emptyList()
    private var daftarDompet: List<Dompet>    = emptyList()
    private var displayList:  MutableList<TransaksiItem> = mutableListOf()

    // Set ini menyimpan jenis header yang sedang di-collapse
    private val collapsedHeaders = mutableSetOf<String>()

    fun submitDompet(list: List<Dompet>) {
        daftarDompet = list
        rebuildDisplayList()
    }

    fun submitList(list: List<Transaksi>) {
        semuaData = list
        rebuildDisplayList()
    }

    private fun rebuildDisplayList() {
        val newList     = mutableListOf<TransaksiItem>()
        val transfer    = semuaData.filter { it.kategori == "Transfer" }
        val pemasukan   = semuaData.filter { it.jenis == "PEMASUKAN"   && it.kategori != "Transfer" }
        val pengeluaran = semuaData.filter { it.jenis == "PENGELUARAN" && it.kategori != "Transfer" }

        if (transfer.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Transfer", "TRANSFER"))
            if (!collapsedHeaders.contains("TRANSFER"))
                transfer.forEach { newList.add(TransaksiItem.Item(it)) }
        }
        if (pemasukan.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Pemasukan", "PEMASUKAN"))
            if (!collapsedHeaders.contains("PEMASUKAN"))
                pemasukan.forEach { newList.add(TransaksiItem.Item(it)) }
        }
        if (pengeluaran.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Pengeluaran", "PENGELUARAN"))
            if (!collapsedHeaders.contains("PENGELUARAN"))
                pengeluaran.forEach { newList.add(TransaksiItem.Item(it)) }
        }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = displayList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = displayList[oldPos]
                val new = newList[newPos]
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

    // ── HeaderViewHolder ──────────────────────────────────────────────────────

    inner class HeaderViewHolder(private val binding: ItemHeaderTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TransaksiItem.Header) {
            binding.tvHeaderLabel.text = header.label

            // ✅ FIX: Set rotasi awal berdasarkan state SAAT INI
            val isCollapsedNow = collapsedHeaders.contains(header.jenis)
            binding.ivCollapse.rotation = if (isCollapsedNow) -90f else 0f

            // ✅ FIX UTAMA: Baca state dari collapsedHeaders LANGSUNG di dalam listener,
            // bukan dari variabel yang di-capture saat bind() dipanggil.
            // Ini mencegah stale closure yang menyebabkan toggle tidak konsisten.
            binding.root.setOnClickListener {
                val isCurrentlyCollapsed = collapsedHeaders.contains(header.jenis)

                // Animasi chevron
                val fromDeg = if (isCurrentlyCollapsed) -90f else 0f
                val toDeg   = if (isCurrentlyCollapsed) 0f   else -90f
                android.animation.ObjectAnimator
                    .ofFloat(binding.ivCollapse, "rotation", fromDeg, toDeg)
                    .apply {
                        duration     = 200
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }

                // Toggle state
                if (isCurrentlyCollapsed) collapsedHeaders.remove(header.jenis)
                else                      collapsedHeaders.add(header.jenis)

                rebuildDisplayList()
            }
        }
    }

    // ── ItemViewHolder ────────────────────────────────────────────────────────

    inner class ItemViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // State expand/collapse detail per item (reset setiap bind karena RecyclerView recycles view)
        private var isDetailExpanded = true

        fun bind(transaksi: Transaksi) {
            val iconRes = KATEGORI_ICON[transaksi.kategori] ?: R.drawable.ic_wallet
            binding.ivKategoriIcon.setImageResource(iconRes)
            binding.tvKategori.text = transaksi.kategori
            binding.tvCatatan.text  = transaksi.catatan.ifEmpty { "-" }

            val isIncome = transaksi.jenis == "PEMASUKAN"
            val warna    = if (isIncome) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            val prefix   = if (isIncome) "+ " else "- "

            // Tampilkan atau sembunyikan nominal sesuai state mata
            if (nominalVisible) {
                binding.tvNominal.text = "$prefix${CurrencyFormatter.format(transaksi.nominal)}"
            } else {
                binding.tvNominal.text = "${prefix}Rp ***"
            }
            binding.tvNominal.setTextColor(warna)

            // Nama dompet dari lookup
            binding.tvSumberDana.text =
                daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"

            // Reset state detail ke expanded setiap kali item di-bind ulang
            // (karena RecyclerView recycle ViewHolder)
            isDetailExpanded = true
            binding.cardDetail.visibility = View.VISIBLE
            binding.ivArrowCollapse.rotation = 0f

            // Klik header item → toggle detail card
            binding.clHeaderInteraktif.setOnClickListener {
                isDetailExpanded = !isDetailExpanded
                val rotation = if (isDetailExpanded) 0f else -180f
                binding.ivArrowCollapse.animate()
                    .rotation(rotation)
                    .setDuration(200)
                    .start()
                binding.cardDetail.visibility =
                    if (isDetailExpanded) View.VISIBLE else View.GONE
            }

            // Klik card utama → dialog Edit / Hapus
            binding.clCardUtama.setOnClickListener {
                val context = binding.root.context
                val pilihan = arrayOf("✏️  Edit", "🗑️  Hapus")
                android.app.AlertDialog.Builder(context)
                    .setTitle(transaksi.kategori)
                    .setItems(pilihan) { _, which ->
                        if (which == 0) onEditClick(transaksi)
                        else            onDeleteClick(transaksi)
                    }
                    .show()
            }
        }
    }

    // ── Boilerplate ───────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) =
        if (displayList[position] is TransaksiItem.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER)
            HeaderViewHolder(ItemHeaderTransaksiBinding.inflate(inflater, parent, false))
        else
            ItemViewHolder(ItemTransaksiBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is TransaksiItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TransaksiItem.Item   -> (holder as ItemViewHolder).bind(item.transaksi)
        }
    }

    override fun getItemCount() = displayList.size
}
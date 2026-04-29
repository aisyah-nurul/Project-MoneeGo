package com.example.appmoneego.adapter

import android.view.LayoutInflater
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
    private val onEditClick:   (Transaksi) -> Unit = {},
    private val onDeleteClick: (Transaksi) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class TransaksiItem {
        data class Header(val label: String, val jenis: String) : TransaksiItem()
        data class Item(val transaksi: Transaksi) : TransaksiItem()
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM   = 1

        // Peta kategori → icon drawable
        // Sesuaikan nama drawable dengan yang ada di project kamu
        private val KATEGORI_ICON = mapOf(
            // Pengeluaran
            "Makanan"           to R.drawable.ic_makanan,
            "Fashion"           to R.drawable.ic_fashion,
            "Transportasi"      to R.drawable.ic_transportasi,
            "Pendidikan"        to R.drawable.ic_pendidikan,
            "Sosial"            to R.drawable.ic_sosial,
            "Kesehatan"         to R.drawable.ic_kesehatan,
            "Rumah Tangga"      to R.drawable.ic_rumahtangga,
            "Kebutuhan Pribadi" to R.drawable.ic_kebutuhanpribadi,
            // Pemasukan
            "Gaji"              to R.drawable.ic_gaji,
            "Bonus"             to R.drawable.ic_bonus,
            "Freelance"         to R.drawable.ic_freelance,
            "Investasi"         to R.drawable.ic_investasi,
            "Hadiah"            to R.drawable.ic_hadiah,
            "Penjualan"         to R.drawable.ic_penjualan,
            // Transfer
            "Transfer"          to R.drawable.ic_wallet
        )
    }

    private var semuaData:    List<Transaksi> = emptyList()
    private var daftarDompet: List<Dompet>    = emptyList()
    private var displayList:  MutableList<TransaksiItem> = mutableListOf()
    private val collapsedHeaders = mutableSetOf<String>()

    fun submitDompet(list: List<Dompet>) {
        daftarDompet = list
        rebuildDisplayList()
    }

    fun submitList(list: List<Transaksi>) {
        semuaData = list
        rebuildDisplayList()
    }

    fun getItemAt(position: Int): Transaksi? = when (val item = displayList.getOrNull(position)) {
        is TransaksiItem.Item   -> item.transaksi
        is TransaksiItem.Header -> null
        null                    -> null
    }

    private fun rebuildDisplayList() {
        val newList     = mutableListOf<TransaksiItem>()
        val pemasukan   = semuaData.filter { it.jenis == "PEMASUKAN" }
        val pengeluaran = semuaData.filter { it.jenis == "PENGELUARAN" }

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

            // Set rotasi awal chevron sesuai state collapsed/expanded
            val isCollapsed = collapsedHeaders.contains(header.jenis)
            binding.ivCollapse.rotation = if (isCollapsed) -90f else 0f

            // Klik header → toggle collapse dengan animasi chevron
            binding.root.setOnClickListener {
                val collapsed = collapsedHeaders.contains(header.jenis)
                val fromDeg   = if (collapsed) -90f else 0f
                val toDeg     = if (collapsed) 0f   else -90f

                // Animasi chevron
                android.animation.ObjectAnimator
                    .ofFloat(binding.ivCollapse, "rotation", fromDeg, toDeg)
                    .apply {
                        duration     = 200
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }

                if (collapsed) collapsedHeaders.remove(header.jenis)
                else           collapsedHeaders.add(header.jenis)

                rebuildDisplayList()
            }
        }
    }

    // ── ViewHolder Item ────────────────────────────────────────────────────────
    inner class ItemViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaksi: Transaksi) {
            // Icon kategori
            val iconRes = KATEGORI_ICON[transaksi.kategori] ?: R.drawable.ic_wallet
            binding.ivKategoriIcon.setImageResource(iconRes)

            binding.tvKategori.text = transaksi.kategori
            binding.tvCatatan.text  = transaksi.catatan.ifEmpty { "-" }

            val formatted = CurrencyFormatter.format(transaksi.nominal)
            val isIncome  = transaksi.jenis == "PEMASUKAN"
            val warna     = if (isIncome) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            val prefix    = if (isIncome) "+ " else "- "

            binding.tvNominal.text = "$prefix$formatted"
            binding.tvNominal.setTextColor(warna)
            binding.tvNominalDetail.text = "$prefix$formatted"
            binding.tvNominalDetail.setTextColor(warna)

            // Nama dompet dari lookup, bukan hardcode
            val namaDompet = daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"
            binding.tvSumberDana.text = namaDompet

            // TAP item → tampilkan dialog pilihan Edit atau Hapus
            binding.cardItemTransaksi.setOnClickListener {
                val context = binding.root.context
                val pilihan = arrayOf("✏️  Edit Transaksi", "🗑️  Hapus Transaksi")
                android.app.AlertDialog.Builder(context)
                    .setTitle(transaksi.kategori)
                    .setItems(pilihan) { _, which ->
                        when (which) {
                            0 -> onEditClick(transaksi)
                            1 -> onDeleteClick(transaksi)
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }

    override fun getItemViewType(position: Int) = when (displayList[position]) {
        is TransaksiItem.Header -> TYPE_HEADER
        is TransaksiItem.Item   -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemHeaderTransaksiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false)
            )
            else -> ItemViewHolder(
                ItemTransaksiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false)
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
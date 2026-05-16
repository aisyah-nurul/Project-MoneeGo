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
    private val onItemClick:   (Transaksi) -> Unit = {},
    private val onEditClick:   (Transaksi) -> Unit = {},
    private val onDeleteClick: (Transaksi) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var nominalVisible: Boolean = nominalVisibleInit
        set(value) { field = value; notifyDataSetChanged() }

    // Data class khusus untuk merepresentasikan satu pasang transfer
    data class TransferPair(
        val keluarTransaksi: Transaksi,   // sisi PENGELUARAN (dompet asal)
        val masukTransaksi: Transaksi,    // sisi PEMASUKAN (dompet tujuan)
        val nominal: Double,
        val tanggal: Long,
        val catatan: String,
        val transferId: String
    )

    sealed class TransaksiItem {
        data class Header(val label: String, val jenis: String) : TransaksiItem()
        data class Item(val transaksi: Transaksi) : TransaksiItem()
        data class TransferItem(val pair: TransferPair) : TransaksiItem()
    }

    companion object {
        const val TYPE_HEADER   = 0
        const val TYPE_ITEM     = 1
        const val TYPE_TRANSFER = 2

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
    private val collapsedHeaders = mutableSetOf<String>()

    fun submitDompet(list: List<Dompet>) { daftarDompet = list; rebuildDisplayList() }
    fun submitList(list: List<Transaksi>) { semuaData = list; rebuildDisplayList() }
    fun getDaftarDompet(): List<Dompet> = daftarDompet

    private fun rebuildDisplayList() {
        val newList = mutableListOf<TransaksiItem>()

        // ── Kelompokkan transfer berdasarkan transferId ────────────────────
        val transferData    = semuaData.filter { it.kategori == "Transfer" }
        val transferPairs   = mutableListOf<TransferPair>()
        val sudahDiproses   = mutableSetOf<Int>()

        transferData.forEach { t ->
            if (t.id in sudahDiproses) return@forEach

            val pasangan = if (t.transferId != null) {
                // Cari pasangan berdasarkan transferId
                transferData.find { other ->
                    other.id != t.id &&
                            other.transferId == t.transferId
                }
            } else {
                // Fallback untuk data lama (tanpa transferId): pasangkan manual
                transferData.find { other ->
                    other.id != t.id &&
                            other.nominal == t.nominal &&
                            other.tanggal == t.tanggal &&
                            other.id !in sudahDiproses
                }
            }

            if (pasangan != null) {
                // Tentukan mana asal (PENGELUARAN) dan tujuan (PEMASUKAN)
                val asal   = if (t.jenis == "PENGELUARAN") t else pasangan
                val tujuan = if (t.jenis == "PEMASUKAN")   t else pasangan

                transferPairs.add(TransferPair(
                    keluarTransaksi = asal,
                    masukTransaksi  = tujuan,
                    nominal         = t.nominal,
                    tanggal         = t.tanggal,
                    catatan         = t.catatan,
                    transferId      = t.transferId ?: ""
                ))
                sudahDiproses.add(t.id)
                sudahDiproses.add(pasangan.id)
            } else {
                // Transfer tunggal (tidak punya pasangan) — tampilkan normal
                transferPairs.add(TransferPair(
                    keluarTransaksi = t,
                    masukTransaksi  = t,
                    nominal         = t.nominal,
                    tanggal         = t.tanggal,
                    catatan         = t.catatan,
                    transferId      = t.transferId ?: ""
                ))
                sudahDiproses.add(t.id)
            }
        }

        val pemasukan   = semuaData.filter { it.jenis == "PEMASUKAN"   && it.kategori != "Transfer" }
        val pengeluaran = semuaData.filter { it.jenis == "PENGELUARAN" && it.kategori != "Transfer" }

        // ── Bangun display list ────────────────────────────────────────────
        if (transferPairs.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Transfer", "TRANSFER"))
            if (!collapsedHeaders.contains("TRANSFER")) {
                transferPairs.forEach { newList.add(TransaksiItem.TransferItem(it)) }
            }
        }
        if (pemasukan.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Pemasukan", "PEMASUKAN"))
            if (!collapsedHeaders.contains("PEMASUKAN")) {
                pemasukan.forEach { newList.add(TransaksiItem.Item(it)) }
            }
        }
        if (pengeluaran.isNotEmpty()) {
            newList.add(TransaksiItem.Header("Pengeluaran", "PENGELUARAN"))
            if (!collapsedHeaders.contains("PENGELUARAN")) {
                pengeluaran.forEach { newList.add(TransaksiItem.Item(it)) }
            }
        }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = displayList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = displayList[oldPos]; val new = newList[newPos]
                if (old is TransaksiItem.Header      && new is TransaksiItem.Header)      return old.jenis == new.jenis
                if (old is TransaksiItem.Item        && new is TransaksiItem.Item)        return old.transaksi.id == new.transaksi.id
                if (old is TransaksiItem.TransferItem && new is TransaksiItem.TransferItem) return old.pair.transferId == new.pair.transferId
                return false
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = displayList[oldPos] == newList[newPos]
        })
        displayList = newList
        diff.dispatchUpdatesTo(this)
    }

    // ── HeaderViewHolder ──────────────────────────────────────────────────────

    inner class HeaderViewHolder(private val binding: ItemHeaderTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TransaksiItem.Header) {
            binding.tvHeaderLabel.text = header.label
            val isCollapsedNow = collapsedHeaders.contains(header.jenis)
            binding.ivCollapse.rotation = if (isCollapsedNow) -90f else 0f
            binding.root.setOnClickListener {
                val isCurrentlyCollapsed = collapsedHeaders.contains(header.jenis)
                val fromDeg = if (isCurrentlyCollapsed) -90f else 0f
                val toDeg   = if (isCurrentlyCollapsed) 0f   else -90f
                android.animation.ObjectAnimator
                    .ofFloat(binding.ivCollapse, "rotation", fromDeg, toDeg)
                    .apply { duration = 200; interpolator = AccelerateDecelerateInterpolator(); start() }
                if (isCurrentlyCollapsed) collapsedHeaders.remove(header.jenis)
                else collapsedHeaders.add(header.jenis)
                rebuildDisplayList()
            }
        }
    }

    // ── TransferViewHolder — satu card untuk satu transfer ────────────────────

    inner class TransferViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isDetailExpanded = true

        fun bind(pair: TransferPair) {
            binding.ivKategoriIcon.setImageResource(R.drawable.ic_wallet)
            binding.tvKategori.text = "Transfer"

            // Format: "BCA → GOPAY"
            val namaAsal   = daftarDompet.find { it.id == pair.keluarTransaksi.dompetId }?.nama ?: "?"
            val namaTujuan = daftarDompet.find { it.id == pair.masukTransaksi.dompetId  }?.nama ?: "?"

            val subLabel = if (namaAsal == namaTujuan) {
                // Transfer tunggal tanpa pasangan
                pair.catatan.ifEmpty { "Transfer" }
            } else {
                "$namaAsal → $namaTujuan"
            }
            binding.tvCatatan.text    = subLabel
            binding.tvSumberDana.text = ""

            // Nominal transfer: warna netral
            if (nominalVisible) {
                binding.tvNominal.text = CurrencyFormatter.format(pair.nominal)
            } else {
                binding.tvNominal.text = "Rp ***"
            }
            binding.tvNominal.setTextColor(0xFF1A2B34.toInt())

            isDetailExpanded = true
            binding.cardDetail.visibility    = View.VISIBLE
            binding.ivArrowCollapse.rotation = 0f

            binding.clHeaderInteraktif.setOnClickListener {
                isDetailExpanded = !isDetailExpanded
                binding.ivArrowCollapse.animate()
                    .rotation(if (isDetailExpanded) 0f else -180f)
                    .setDuration(200).start()
                binding.cardDetail.visibility =
                    if (isDetailExpanded) View.VISIBLE else View.GONE
            }

            // Klik card → buka detail menggunakan transaksi sisi PENGELUARAN
            binding.clCardUtama.setOnClickListener {
                onItemClick(pair.keluarTransaksi)
            }
        }
    }

    // ── ItemViewHolder ────────────────────────────────────────────────────────

    inner class ItemViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isDetailExpanded = true

        fun bind(transaksi: Transaksi) {
            val iconRes = KATEGORI_ICON[transaksi.kategori] ?: R.drawable.ic_wallet
            binding.ivKategoriIcon.setImageResource(iconRes)
            binding.tvKategori.text = transaksi.kategori
            binding.tvCatatan.text  = transaksi.catatan.ifEmpty { "-" }

            val isIncome = transaksi.jenis == "PEMASUKAN"
            val warna  = if (isIncome) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            val prefix = if (isIncome) "+ " else "- "

            binding.tvNominal.text = if (nominalVisible)
                "$prefix${CurrencyFormatter.format(transaksi.nominal)}"
            else "${prefix}Rp ***"
            binding.tvNominal.setTextColor(warna)

            binding.tvSumberDana.text =
                daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"

            isDetailExpanded = true
            binding.cardDetail.visibility    = View.VISIBLE
            binding.ivArrowCollapse.rotation = 0f

            binding.clHeaderInteraktif.setOnClickListener {
                isDetailExpanded = !isDetailExpanded
                binding.ivArrowCollapse.animate()
                    .rotation(if (isDetailExpanded) 0f else -180f)
                    .setDuration(200).start()
                binding.cardDetail.visibility =
                    if (isDetailExpanded) View.VISIBLE else View.GONE
            }

            binding.clCardUtama.setOnClickListener { onItemClick(transaksi) }
        }
    }

    // ── Boilerplate ───────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) = when (displayList[position]) {
        is TransaksiItem.Header       -> TYPE_HEADER
        is TransaksiItem.TransferItem -> TYPE_TRANSFER
        is TransaksiItem.Item         -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER   -> HeaderViewHolder(ItemHeaderTransaksiBinding.inflate(inflater, parent, false))
            TYPE_TRANSFER -> TransferViewHolder(ItemTransaksiBinding.inflate(inflater, parent, false))
            else          -> ItemViewHolder(ItemTransaksiBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is TransaksiItem.Header       -> (holder as HeaderViewHolder).bind(item)
            is TransaksiItem.TransferItem -> (holder as TransferViewHolder).bind(item.pair)
            is TransaksiItem.Item         -> (holder as ItemViewHolder).bind(item.transaksi)
        }
    }

    override fun getItemCount() = displayList.size
}
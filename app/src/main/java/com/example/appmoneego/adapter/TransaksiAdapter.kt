package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Transaksi
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

    data class TransferPair(
        val keluarTransaksi: Transaksi,
        val masukTransaksi:  Transaksi,
        val nominal:    Double,
        val tanggal:    Long,
        val catatan:    String,
        val transferId: String
    )

    sealed class TransaksiItem {
        data class Item(val transaksi: Transaksi)       : TransaksiItem()
        data class TransferItem(val pair: TransferPair) : TransaksiItem()

        fun getTanggal(): Long = when (this) {
            is Item         -> transaksi.tanggal
            is TransferItem -> pair.tanggal
        }
    }

    companion object {
        const val TYPE_ITEM     = 0
        const val TYPE_TRANSFER = 1

        // ── Kategori standar → icon ───────────────────────────────────────────
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

        // ── Jenis dompet → icon ───────────────────────────────────────────────
        private val JENIS_DOMPET_ICON = mapOf(
            "Rekening Bank"  to R.drawable.ic_wallet_bank,
            "Dompet Digital" to R.drawable.ic_wallet_digital,
            "Uang Tunai"     to R.drawable.ic_wallet_cash,
            "Investasi"      to R.drawable.ic_wallet_investasi,
            "Tabungan"       to R.drawable.ic_wallet_tabungan,
            "Lainnya"        to R.drawable.ic_wallet_lainnya
        )

        private val KATEGORI_STANDAR = KATEGORI_ICON.keys.toSet()

        // ══════════════════════════════════════════════════════════════════════
        // BUG 2 FIX: Daftar kategori yang iconnya harus menggunakan icon
        // DOMPET SUMBER DANA, bukan icon kategori.
        //
        // Kenapa "Hutang" masuk sini?
        //   Saat user membayar hutang, transaksi disimpan dengan:
        //     jenis     = "PENGELUARAN"
        //     kategori  = "Hutang"
        //     dompetId  = id dompet yang dipakai membayar (Gopay, BCA, dll)
        //
        //   Sebelum fix: TransaksiAdapter memakai ic_hutang untuk semua
        //   transaksi berkategori "Hutang" — icon tidak mencerminkan dompet
        //   mana yang dipakai.
        //
        //   Setelah fix: Jika kategori == "Hutang", resolveIcon() akan mencari
        //   dompet berdasarkan transaksi.dompetId, lalu mengembalikan icon
        //   sesuai jenis dompet tersebut (Gopay → ic_wallet_digital,
        //   BCA → ic_wallet_bank, dst).
        //
        //   Ini berlaku di CARD transaksi (ItemViewHolder) maupun
        //   POPUP detail (DetailTransaksiDialog juga sudah dikerjakan terpisah).
        // ══════════════════════════════════════════════════════════════════════
        private val KATEGORI_PAKAI_ICON_DOMPET = setOf("Hutang")
    }

    private var semuaData:    List<Transaksi>            = emptyList()
    private var daftarDompet: List<Dompet>               = emptyList()
    private var displayList:  MutableList<TransaksiItem> = mutableListOf()

    fun submitDompet(list: List<Dompet>)  { daftarDompet = list; rebuildDisplayList() }
    fun submitList(list: List<Transaksi>) { semuaData    = list; rebuildDisplayList() }
    fun getDaftarDompet(): List<Dompet>   = daftarDompet

    // ── Resolve icon untuk satu transaksi ────────────────────────────────────
    //
    // Priority logic:
    //   1. Jika kategori ada di KATEGORI_PAKAI_ICON_DOMPET (mis. "Hutang")
    //      → cari dompet by dompetId → kembalikan icon sesuai jenis dompet
    //   2. Jika kategori ada di KATEGORI_STANDAR → pakai icon kategori biasa
    //   3. Jika kategori TIDAK ada di KATEGORI_STANDAR (= nama dompet dari
    //      saldo awal otomatis) → baca field catatan yang berisi jenis dompet
    //      → pakai JENIS_DOMPET_ICON
    //   4. Fallback ke ic_wallet jika tidak ditemukan
    private fun resolveIcon(transaksi: Transaksi): Int {
        // Kasus 1: kategori yang harus pakai icon dompet sumber dana (mis. "Hutang")
        if (transaksi.kategori in KATEGORI_PAKAI_ICON_DOMPET) {
            val dompet = daftarDompet.find { it.id == transaksi.dompetId }
            return JENIS_DOMPET_ICON[dompet?.jenis] ?: R.drawable.ic_wallet
        }

        // Kasus 2: kategori standar dengan icon kategori
        if (transaksi.kategori in KATEGORI_STANDAR) {
            return KATEGORI_ICON[transaksi.kategori] ?: R.drawable.ic_wallet
        }

        // Kasus 3: bukan kategori standar = nama dompet dari saldo awal otomatis
        // catatan berisi jenis dompet (diset oleh DompetFragment)
        return JENIS_DOMPET_ICON[transaksi.catatan] ?: R.drawable.ic_wallet
    }

    private fun rebuildDisplayList() {
        val newList = mutableListOf<TransaksiItem>()

        // ── Bangun TransferPairs ──────────────────────────────────────────────
        val transferData  = semuaData.filter { it.kategori == "Transfer" }
        val transferPairs = mutableListOf<TransferPair>()
        val sudahDiproses = mutableSetOf<Int>()

        transferData.forEach { t ->
            if (t.id in sudahDiproses) return@forEach
            val pasangan = if (t.transferId != null) {
                transferData.find { other ->
                    other.id != t.id && other.transferId == t.transferId
                }
            } else {
                transferData.find { other ->
                    other.id != t.id &&
                            other.nominal == t.nominal &&
                            other.tanggal == t.tanggal &&
                            other.id !in sudahDiproses
                }
            }
            if (pasangan != null) {
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

        transferPairs.forEach { newList.add(TransaksiItem.TransferItem(it)) }
        pemasukan.forEach     { newList.add(TransaksiItem.Item(it)) }
        pengeluaran.forEach   { newList.add(TransaksiItem.Item(it)) }

        newList.sortByDescending { it.getTanggal() }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = displayList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = displayList[oldPos]; val new = newList[newPos]
                if (old is TransaksiItem.Item         && new is TransaksiItem.Item)
                    return old.transaksi.id == new.transaksi.id
                if (old is TransaksiItem.TransferItem && new is TransaksiItem.TransferItem)
                    return old.pair.transferId == new.pair.transferId
                return false
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                displayList[oldPos] == newList[newPos]
        })
        displayList = newList
        diff.dispatchUpdatesTo(this)
    }

    // ── TransferViewHolder ────────────────────────────────────────────────────

    inner class TransferViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: TransferPair) {
            binding.ivKategoriIcon.setImageResource(R.drawable.ic_wallet)
            binding.tvKategori.text = "Transfer"

            val namaAsal   = daftarDompet.find { it.id == pair.keluarTransaksi.dompetId }?.nama ?: "?"
            val namaTujuan = daftarDompet.find { it.id == pair.masukTransaksi.dompetId  }?.nama ?: "?"
            binding.tvCatatan.text = if (namaAsal == namaTujuan)
                pair.catatan.ifEmpty { "Transfer" }
            else "$namaAsal → $namaTujuan"
            binding.tvSumberDana.text = ""

            binding.tvNominal.text = if (nominalVisible)
                CurrencyFormatter.format(pair.nominal) else "Rp ***"
            binding.tvNominal.setTextColor(0xFF1A2B34.toInt())

            binding.clHeaderInteraktif.visibility = View.GONE
            binding.cardDetail.visibility         = View.VISIBLE

            binding.clCardUtama.setOnClickListener { onItemClick(pair.keluarTransaksi) }
        }
    }

    // ── ItemViewHolder ────────────────────────────────────────────────────────

    inner class ItemViewHolder(private val binding: ItemTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaksi: Transaksi) {
            // Gunakan resolveIcon() — sudah menangani hutang, saldo awal, dan kategori biasa
            binding.ivKategoriIcon.setImageResource(resolveIcon(transaksi))
            binding.tvKategori.text = transaksi.kategori

            val isSaldoAwal = transaksi.kategori !in KATEGORI_STANDAR &&
                    transaksi.kategori !in KATEGORI_PAKAI_ICON_DOMPET
            binding.tvCatatan.text = transaksi.catatan.ifEmpty { "-" }

            val isIncome = transaksi.jenis == "PEMASUKAN"
            val warna    = if (isIncome) 0xFF276F29.toInt() else 0xFFF44336.toInt()
            val prefix   = if (isIncome) "+ " else "- "

            binding.tvNominal.text = if (nominalVisible)
                "$prefix${CurrencyFormatter.format(transaksi.nominal)}"
            else "${prefix}Rp ***"
            binding.tvNominal.setTextColor(warna)

            binding.tvSumberDana.text =
                daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"

            binding.clHeaderInteraktif.visibility = View.GONE
            binding.cardDetail.visibility         = View.VISIBLE

            binding.clCardUtama.setOnClickListener { onItemClick(transaksi) }
        }
    }

    // ── Boilerplate ───────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) = when (displayList[position]) {
        is TransaksiItem.TransferItem -> TYPE_TRANSFER
        is TransaksiItem.Item         -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TRANSFER -> TransferViewHolder(ItemTransaksiBinding.inflate(inflater, parent, false))
            else          -> ItemViewHolder(ItemTransaksiBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is TransaksiItem.TransferItem -> (holder as TransferViewHolder).bind(item.pair)
            is TransaksiItem.Item         -> (holder as ItemViewHolder).bind(item.transaksi)
        }
    }

    override fun getItemCount() = displayList.size
}
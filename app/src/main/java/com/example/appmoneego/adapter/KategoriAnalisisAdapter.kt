package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.model.KategoriSummary

class KategoriAnalisisAdapter(
    private val onItemClick: (KategoriSummary) -> Unit = {}
) : ListAdapter<KategoriSummary, KategoriAnalisisAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView        = view.findViewById(R.id.ivKategoriIcon)
        val tvNama: TextView         = view.findViewById(R.id.tvKategoriNama)
        val tvNominal: TextView      = view.findViewById(R.id.tvKategoriNominal)
        val tvPersen: TextView       = view.findViewById(R.id.tvKategoriPersen)
        val progressBar: ProgressBar = view.findViewById(R.id.progressKategori)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kategori_analisis, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvNama.text    = item.nama
        holder.tvNominal.text = formatRupiah(item.jumlah)

        // Persentase dibulatkan — tampilkan tanpa desimal
        val persenBulat = item.persentase.toInt()
        holder.tvPersen.text  = "$persenBulat%"
        holder.progressBar.progress = persenBulat

        // Icon: coba match kategori dulu, lalu jenis dompet
        holder.ivIcon.setImageResource(getIcon(item.nama))

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    /**
     * Cari icon berdasarkan nama kategori transaksi (sesuai TambahTransaksiFragment)
     * atau nama dompet (sesuai jenis dompet yang diinput pengguna).
     * Kalau tidak cocok → pakai icon wallet/dompet umum.
     */
    private fun getIcon(nama: String): Int = when (nama.lowercase().trim()) {
        // Tambahkan di dalam when di fungsi getIcon():
        "food"              -> R.drawable.ic_makanan
        "transportation"    -> R.drawable.ic_transportasi
        "education"         -> R.drawable.ic_pendidikan
        "social"            -> R.drawable.ic_sosial
        "health"            -> R.drawable.ic_kesehatan
        "household"         -> R.drawable.ic_rumahtangga
        "personal needs"    -> R.drawable.ic_kebutuhanpribadi
        "salary"            -> R.drawable.ic_gaji
        "investment"        -> R.drawable.ic_investasi
        "gift"              -> R.drawable.ic_hadiah
        "sales"             -> R.drawable.ic_penjualan
        // ── Kategori Pengeluaran ──────────────────────────────────────────────
        "makanan"           -> R.drawable.ic_makanan
        "fashion"           -> R.drawable.ic_fashion
        "transportasi"      -> R.drawable.ic_transportasi
        "pendidikan"        -> R.drawable.ic_pendidikan
        "sosial"            -> R.drawable.ic_sosial
        "kesehatan"         -> R.drawable.ic_kesehatan
        "rumah tangga"      -> R.drawable.ic_rumahtangga
        "kebutuhan pribadi" -> R.drawable.ic_kebutuhanpribadi

        // ── Kategori Pemasukan ────────────────────────────────────────────────
        "gaji"      -> R.drawable.ic_gaji
        "bonus"     -> R.drawable.ic_bonus
        "freelance" -> R.drawable.ic_freelance
        "investasi" -> R.drawable.ic_investasi
        "hadiah"    -> R.drawable.ic_hadiah
        "penjualan" -> R.drawable.ic_penjualan

        // ── Kategori Transfer / Saldo ─────────────────────────────────────────
        "transfer", "saldo awal" -> R.drawable.ic_wallet

        // ── Jenis Dompet (untuk mode "Oleh Dompet") ───────────────────────────
        // Deteksi berdasarkan nama dompet yang diinput pengguna:
        // Kalau nama mengandung kata kunci bank terkenal → ic_wallet_bank
        // Kalau mengandung kata kunci digital → ic_wallet_digital
        // dst.
        else -> getIconByDompetKeyword(nama)
    }

    private fun getIconByDompetKeyword(nama: String): Int {
        val lower = nama.lowercase()
        return when {
            // Bank / Rekening
            lower.contains("bca")      ||
                    lower.contains("mandiri")  ||
                    lower.contains("bni")      ||
                    lower.contains("bri")      ||
                    lower.contains("cimb")     ||
                    lower.contains("danamon")  ||
                    lower.contains("bank")     ||
                    lower.contains("rekening") ||
                    lower.contains("tabungan") -> R.drawable.ic_wallet_bank

            // Dompet Digital
            lower.contains("gopay")    ||
                    lower.contains("ovo")      ||
                    lower.contains("dana")     ||
                    lower.contains("shopeepay")||
                    lower.contains("linkaja")  ||
                    lower.contains("digital")  ||
                    lower.contains("dompet")   -> R.drawable.ic_wallet_digital

            // Uang Tunai
            lower.contains("cash")     ||
                    lower.contains("tunai")    ||
                    lower.contains("pegangan") ||
                    lower.contains("wallet")   -> R.drawable.ic_wallet_cash

            // Investasi
            lower.contains("investasi")||
                    lower.contains("saham")    ||
                    lower.contains("reksadana")||
                    lower.contains("crypto")   -> R.drawable.ic_wallet_investasi

            // Tabungan khusus
            lower.contains("tabungan") -> R.drawable.ic_wallet_tabungan

            // Default
            else -> R.drawable.ic_wallet
        }
    }

    private fun formatRupiah(amount: Double): String =
        "Rp ${String.format("%,.0f", amount).replace(',', '.')}"

    class DiffCallback : DiffUtil.ItemCallback<KategoriSummary>() {
        override fun areItemsTheSame(old: KategoriSummary, new: KategoriSummary) =
            old.nama == new.nama
        override fun areContentsTheSame(old: KategoriSummary, new: KategoriSummary) =
            old == new
    }
}
package com.example.appmoneego.ui.hutang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.CicilanEntity
import com.example.appmoneego.data.entity.Dompet

class RiwayatCicilanAdapter(
    private val list: List<CicilanEntity>,
    private val daftarDompet: List<Dompet>,
    private val onHapus: (CicilanEntity) -> Unit
) : RecyclerView.Adapter<RiwayatCicilanAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivIcon    : ImageView   = v.findViewById(R.id.ivRiwayatIcon)
        val tvNominal : TextView    = v.findViewById(R.id.tvRiwayatNominal)
        val tvTanggal : TextView    = v.findViewById(R.id.tvRiwayatTanggal)
        val tvCatatan : TextView    = v.findViewById(R.id.tvRiwayatCatatan)
        val btnHapus  : ImageButton = v.findViewById(R.id.btnHapusCicilan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_cicilan, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c            = list[position]
        val nomorCicilan = position + 1
        val dompet       = daftarDompet.find { it.id == c.dompetId }
        val namaDompet   = dompet?.nama ?: ""

        // FIX BUG 1: icon mengikuti jenis dompet yang dipakai cicilan ini —
        // sama persis dengan mapping di DompetAdapter, bukan icon centang
        // generik untuk semua dompet.
        holder.ivIcon.setImageResource(getIconDompet(dompet?.jenis ?: "Lainnya"))

        holder.tvNominal.text = "+Rp${String.format("%,d", c.nominal).replace(",", ".")}"
        holder.tvTanggal.text = "Cicilan ke-$nomorCicilan • ${c.tanggalBayar}"

        holder.tvCatatan.text = when {
            namaDompet.isNotEmpty() && c.catatan.isNotEmpty() -> "$namaDompet • ${c.catatan}"
            namaDompet.isNotEmpty()                           -> namaDompet
            c.catatan.isNotEmpty()                            -> c.catatan
            else                                              -> "-"
        }

        holder.btnHapus.setOnClickListener { onHapus(c) }
    }

    override fun getItemCount() = list.size

    /**
     * Mapping icon berdasarkan jenis dompet — identik dengan
     * DompetAdapter.getIconRes(), supaya konsisten dengan icon yang
     * tampil di Dropdown Sumber Dana dan Riwayat Transaksi.
     */
    private fun getIconDompet(jenis: String): Int = when (jenis) {
        "Rekening Bank"  -> R.drawable.ic_wallet_bank
        "Dompet Digital" -> R.drawable.ic_wallet_digital
        "Uang Tunai"     -> R.drawable.ic_wallet_cash
        "Investasi"      -> R.drawable.ic_wallet_investasi
        "Tabungan"       -> R.drawable.ic_wallet_tabungan
        else             -> R.drawable.ic_wallet_lainnya
    }
}
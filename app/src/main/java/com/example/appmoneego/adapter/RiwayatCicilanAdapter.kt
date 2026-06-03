package com.example.appmoneego.ui.hutang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
        val namaDompet   = daftarDompet.find { it.id == c.dompetId }?.nama ?: ""

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
}
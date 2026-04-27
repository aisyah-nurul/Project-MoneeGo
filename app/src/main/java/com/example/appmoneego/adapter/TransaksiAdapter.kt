package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.DateUtils

class TransaksiAdapter : ListAdapter<Transaksi, TransaksiAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Transaksi>() {
            override fun areItemsTheSame(oldItem: Transaksi, newItem: Transaksi) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Transaksi, newItem: Transaksi) =
                oldItem == newItem
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvKategori: TextView   = itemView.findViewById(R.id.tv_kategori)
        val tvNominal: TextView    = itemView.findViewById(R.id.tv_nominal)
        val tvCatatan: TextView    = itemView.findViewById(R.id.tv_catatan)
        val tvSumberDana: TextView = itemView.findViewById(R.id.tv_sumber_dana)
        val tvNominalDetail: TextView = itemView.findViewById(R.id.tv_nominal_detail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaksi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaksi = getItem(position)

        holder.tvKategori.text = transaksi.kategori

        // Nominal utama dengan warna sesuai jenis
        val nominalFormatted = CurrencyFormatter.format(transaksi.nominal)
        if (transaksi.jenis == "PEMASUKAN") {
            holder.tvNominal.text = "+ $nominalFormatted"
            holder.tvNominal.setTextColor(0xFF4CAF50.toInt())  // hijau
            holder.tvNominalDetail.text = "+ $nominalFormatted"
            holder.tvNominalDetail.setTextColor(0xFF4CAF50.toInt())
        } else {
            holder.tvNominal.text = "- $nominalFormatted"
            holder.tvNominal.setTextColor(0xFFF44336.toInt())  // merah
            holder.tvNominalDetail.text = "- $nominalFormatted"
            holder.tvNominalDetail.setTextColor(0xFFF44336.toInt())
        }

        // Catatan
        holder.tvCatatan.text = if (transaksi.catatan.isBlank()) "-"
        else transaksi.catatan

        // Sumber Dana berdasarkan dompetId
        holder.tvSumberDana.text = when (transaksi.dompetId) {
            2    -> "E-Wallet"
            3    -> "Bank"
            else -> "Tunai"
        }
    }
}
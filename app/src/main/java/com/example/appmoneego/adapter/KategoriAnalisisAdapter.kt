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

class KategoriAnalisisAdapter :
    ListAdapter<KategoriSummary, KategoriAnalisisAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivKategoriIcon)
        val tvNama: TextView = view.findViewById(R.id.tvKategoriNama)
        val tvNominal: TextView = view.findViewById(R.id.tvKategoriNominal)
        val tvPersen: TextView = view.findViewById(R.id.tvKategoriPersen)
        val progressBar: ProgressBar = view.findViewById(R.id.progressKategori)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kategori_analisis, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvNama.text = item.nama
        holder.tvNominal.text = "Rp ${String.format("%,.0f", item.jumlah).replace(',', '.')}"
        holder.tvPersen.text = "${item.persentase.toInt()}%"
        holder.progressBar.progress = item.persentase.toInt()
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
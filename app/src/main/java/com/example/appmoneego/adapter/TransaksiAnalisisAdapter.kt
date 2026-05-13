package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.model.KategoriSummary

class TransaksiAnalisisAdapter :
    RecyclerView.Adapter<TransaksiAnalisisAdapter.ViewHolder>() {

    private val list = mutableListOf<KategoriSummary>()

    fun submitList(data: List<KategoriSummary>?) {
        list.clear()

        if (data != null) {
            list.addAll(data)
        }

        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvNama: TextView =
            view.findViewById(R.id.tvNamaTransaksi)

        val tvNominal: TextView =
            view.findViewById(R.id.tvNominalTransaksi)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaksi_analisis, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = list[position]

        holder.tvNama.text = item.nama

        holder.tvNominal.text =
            "Rp ${String.format("%,.0f", item.jumlah).replace(',', '.')}"
    }

    override fun getItemCount(): Int = list.size
}
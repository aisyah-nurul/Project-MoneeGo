package com.example.appmoneego.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Transaksi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransaksiAnalisisAdapter :
    ListAdapter<Transaksi, TransaksiAnalisisAdapter.ViewHolder>(DiffCallback()) {

    private val colorMap = mapOf(
        "makanan"           to "#2C3E6B",
        "fashion"           to "#4A6E8A",
        "transportasi"      to "#6B8FA3",
        "kendaraan"         to "#6B8FA3",
        "pendidikan"        to "#8FAEC0",
        "kesehatan"         to "#9ABFD0",
        "sosial"            to "#AECFDB",
        "rumah tangga"      to "#5B8DB8",
        "kebutuhan pribadi" to "#7BA7BC",
        "gaji"              to "#2C3E6B",
        "freelance"         to "#4A6E8A",
        "investasi"         to "#6B8FA3",
        "bonus"             to "#8FAEC0",
        "penjualan"         to "#9ABFD0",
        "hadiah"            to "#AECFDB"
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewDot: View       = view.findViewById(R.id.viewDotWarna)
        val tvNama: TextView    = view.findViewById(R.id.tvNamaTransaksi)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggalTransaksi)
        val tvNominal: TextView = view.findViewById(R.id.tvNominalTransaksi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaksi_analisis, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvNama.text    = item.catatan.ifEmpty { item.kategori }
        holder.tvTanggal.text = formatTanggal(item.tanggal)

        val isPemasukan = item.jenis == "PEMASUKAN"
        if (isPemasukan) {
            holder.tvNominal.text = "+ ${formatRupiah(item.nominal)}"
            holder.tvNominal.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            holder.tvNominal.text = "- ${formatRupiah(item.nominal)}"
            holder.tvNominal.setTextColor(Color.parseColor("#C62828"))
        }

        val colorHex = colorMap[item.kategori.lowercase()] ?: "#6B8FA3"
        holder.viewDot.background.setTint(Color.parseColor(colorHex))
    }

    private fun formatRupiah(amount: Double): String =
        "Rp ${String.format("%,.0f", amount).replace(',', '.')}"

    private fun formatTanggal(timestamp: Long): String = try {
        SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date(timestamp))
    } catch (e: Exception) { "-" }

    class DiffCallback : DiffUtil.ItemCallback<Transaksi>() {
        override fun areItemsTheSame(old: Transaksi, new: Transaksi) = old.id == new.id
        override fun areContentsTheSame(old: Transaksi, new: Transaksi) = old == new
    }
}
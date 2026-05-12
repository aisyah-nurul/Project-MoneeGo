package com.example.appmoneego.ui.hutang

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.databinding.ItemHutangBinding
import com.example.appmoneego.model.Hutang
import com.example.appmoneego.model.JenisHutang
import java.text.NumberFormat
import java.util.*

class HutangAdapter(
    private val items: MutableList<Hutang>,
    private val onItemClick: (Hutang) -> Unit,
    private val onLunasChanged: (Hutang, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<HutangAdapter.HutangViewHolder>() {

    inner class HutangViewHolder(val binding: ItemHutangBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HutangViewHolder {
        val binding = ItemHutangBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HutangViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HutangViewHolder, position: Int) {
        val hutang = items[position]
        val b = holder.binding
        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        // Nama & jenis
        b.tvNamaHutang.text = hutang.nama
        b.tvJenisHutang.text = hutang.jenisHutang.label

        // Warna teks jenis
        val warnaJenis = when (hutang.jenisHutang) {
            JenisHutang.KARTU_KREDIT       -> "#E53935"
            JenisHutang.PERSONAL           -> "#1E88E5"
            JenisHutang.CICILAN            -> "#FB8C00"
            JenisHutang.PINJAMAN_BANK      -> "#43A047"
            JenisHutang.PINJOL             -> "#8E24AA"
            JenisHutang.PINJAM_KE_KERABAT  -> "#F57C00"
            JenisHutang.LAINNYA            -> "#757575"
        }
        b.tvJenisHutang.setTextColor(Color.parseColor(warnaJenis))

        // Checkbox — set tanpa listener dulu biar tidak trigger saat bind
        b.cbLunas.setOnCheckedChangeListener(null)
        b.cbLunas.isChecked = hutang.lunas
        b.cbLunas.setOnCheckedChangeListener { _, isChecked ->
            onLunasChanged(hutang, isChecked)
        }

        // Label waktu
        val diffMs = System.currentTimeMillis() - hutang.tanggalDibuat.time
        val diffHours = diffMs / (1000 * 60 * 60)
        b.tvWaktu.text = when {
            diffHours < 1  -> "Baru ditambahkan"
            diffHours < 24 -> "${diffHours}j lalu"
            diffHours < 48 -> "Kemarin"
            else           -> "${diffHours / 24} hari lalu"
        }

        // Jumlah
        b.tvJumlahTercatat.text = rupiahFormat.format(hutang.jumlah)
            .replace("IDR", "Rp").replace(",00", "")

        // Progress & persentase
        if (hutang.limitKredit > 0) {
            b.progressHutang.progress = hutang.persentase
            b.tvPersentase.text = "${hutang.persentase}%"
            b.tvPersentaseTotal.text = "${hutang.persentase}% dari Limit"
        } else {
            b.progressHutang.progress = 0
            b.tvPersentase.text = ""
            b.tvPersentaseTotal.text = ""
        }

        // Jatuh tempo
        if (hutang.jatuhTempo != null) {
            val cal = Calendar.getInstance()
            cal.time = hutang.jatuhTempo
            val bulan = arrayOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
            b.tvJatuhTempo.text = "Jatuh Tempo ${cal.get(Calendar.DAY_OF_MONTH)} ${bulan[cal.get(Calendar.MONTH)]}"
            b.layoutJatuhTempo.visibility = View.VISIBLE
        } else {
            b.layoutJatuhTempo.visibility = View.GONE
        }

        b.ivIconHutang.setImageResource(R.drawable.ic_hutang)
        holder.itemView.setOnClickListener { onItemClick(hutang) }
    }

    override fun getItemCount() = items.size
}
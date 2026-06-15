package com.example.appmoneego.ui.hutang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Hutang
import com.example.appmoneego.databinding.ItemHutangBinding
import com.example.appmoneego.utils.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HutangAdapter(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onItemClick: (Hutang) -> Unit
) : ListAdapter<Hutang, HutangAdapter.HutangViewHolder>(DiffCallback()) {

    inner class HutangViewHolder(val binding: ItemHutangBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HutangViewHolder {
        val binding = ItemHutangBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HutangViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HutangViewHolder, position: Int) {
        val hutang = getItem(position)
        val b = holder.binding

        // Nama
        b.tvNamaHutang.text = hutang.nama

        // Badge: tampilkan tanggal jatuh tempo jika ada, fallback "Berjalan" / "Lunas"
        val ctx = holder.itemView.context
        b.tvBadgeTempo.text = when {
            hutang.selesai -> ctx.getString(com.example.appmoneego.R.string.label_selesai)
            hutang.tanggalJatuhTempo.isNotEmpty() -> hutang.tanggalJatuhTempo
            else -> ctx.getString(com.example.appmoneego.R.string.label_berjalan)
        }

        // Nominal
        b.tvTotalHutang.text  = CurrencyFormatter.format(hutang.totalHutang.toDouble())
        b.tvSudahDibayar.text = CurrencyFormatter.format(hutang.sudahDibayar.toDouble())
        b.tvSisaHutang.text   = CurrencyFormatter.format(hutang.sisaHutang.toDouble())

        // Progress
        val persen = if (hutang.totalHutang > 0)
            ((hutang.sudahDibayar.toDouble() / hutang.totalHutang) * 100).toInt()
        else 0
        b.progressBayar.progress = persen
        b.tvPersenLunas.text = "$persen%"

        // Footer meta
        val ctx2 = holder.itemView.context
        val jenis = hutang.catatan.ifEmpty { ctx2.getString(com.example.appmoneego.R.string.title_hutang) }
        b.tvFooterMeta.text = "$jenis • $persen% ${ctx2.getString(com.example.appmoneego.R.string.label_selesai).lowercase()}"

        // Tap hint
        val ctx3 = holder.itemView.context
        b.tvTapHint.text = if (hutang.selesai) ctx3.getString(com.example.appmoneego.R.string.label_selesai) else ctx3.getString(com.example.appmoneego.R.string.label_input_cicilan)

        // Reset cicilan terakhir dulu (penting untuk RecyclerView recycling)
        b.layoutCicilanTerakhir.visibility = View.GONE

        // Query cicilan terakhir dari Room secara async
        val dao = MoneeGoDatabase.getDatabase(holder.itemView.context).cicilanDao()
        lifecycleScope.launch {
            val cicilan = withContext(Dispatchers.IO) {
                dao.getCicilanByHutangId(hutang.id)
            }
            // Pastikan view masih untuk item yang sama (guard against recycling)
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION || getItem(pos).id != hutang.id) return@launch

            if (cicilan.isNotEmpty()) {
                val terakhir = cicilan.maxByOrNull { it.tanggalBayar } ?: cicilan.last()
                b.tvCicilanTerakhirNominal.text =
                    CurrencyFormatter.format(terakhir.nominal.toDouble())
                b.tvCicilanTerakhirTanggal.text = terakhir.tanggalBayar
                b.layoutCicilanTerakhir.visibility = View.VISIBLE
            }
        }

        holder.itemView.setOnClickListener { onItemClick(hutang) }
    }

    class DiffCallback : DiffUtil.ItemCallback<Hutang>() {
        override fun areItemsTheSame(old: Hutang, new: Hutang) = old.id == new.id
        override fun areContentsTheSame(old: Hutang, new: Hutang) = old == new
    }
}
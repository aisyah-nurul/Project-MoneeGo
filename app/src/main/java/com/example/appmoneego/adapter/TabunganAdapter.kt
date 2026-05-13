package com.example.appmoneego.ui.tabungan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.data.entity.Tabungan
import com.example.appmoneego.databinding.ItemTabunganBinding
import java.text.SimpleDateFormat
import java.util.*

class TabunganAdapter(
    private val onTabungClick: (Tabungan) -> Unit,
    private val onItemClick:   (Tabungan) -> Unit
) : ListAdapter<Tabungan, TabunganAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemTabunganBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTabunganBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)
        val b = holder.binding

        b.tvNamaTabungan.text = t.nama

        // Deadline
        if (t.deadline != null) {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            b.tvDeadline.text = "Deadline: ${sdf.format(Date(t.deadline))}"
        } else {
            b.tvDeadline.text = "Deadline: -"
        }

        // Nominal
        b.tvTerkumpul.text    = formatRupiah(t.terkumpul)
        b.tvTargetNominal.text = formatRupiah(t.targetNominal)

        // Progress
        val persen = if (t.targetNominal > 0)
            ((t.terkumpul / t.targetNominal) * 100).toInt().coerceAtMost(100)
        else 0
        b.progressTabungan.progress = persen
        b.tvPersentase.text = "$persen%"

        // Sisa
        val sisa = (t.targetNominal - t.terkumpul).coerceAtLeast(0.0)
        b.tvSisa.text = "⏰ Kurang ${formatRupiah(sisa)}"

        // Tombol tabung
        b.btnTabung.setOnClickListener { onTabungClick(t) }

        // Klik kartu buka detail
        holder.itemView.setOnClickListener { onItemClick(t) }
    }

    private fun formatRupiah(value: Double): String =
        "Rp${String.format("%,.0f", value).replace(",", ".")}"

    class DiffCallback : DiffUtil.ItemCallback<Tabungan>() {
        override fun areItemsTheSame(old: Tabungan, new: Tabungan) = old.id == new.id
        override fun areContentsTheSame(old: Tabungan, new: Tabungan) = old == new
    }
}
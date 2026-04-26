package com.example.appmoneego.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.utils.CurrencyFormatter

class DompetAdapter(
    private val onItemClick: (Dompet) -> Unit,
    private val onItemLongClick: (Dompet) -> Boolean,
    private val totalSaldo: () -> Double
) : ListAdapter<Dompet, DompetAdapter.DompetViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Dompet>() {
            override fun areItemsTheSame(old: Dompet, new: Dompet) = old.id == new.id
            override fun areContentsTheSame(old: Dompet, new: Dompet) = old == new
        }
    }

    inner class DompetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView         = itemView.findViewById(R.id.cardDompet)
        val ivIkon: ImageView      = itemView.findViewById(R.id.ivIkonDompet)
        val tvNama: TextView       = itemView.findViewById(R.id.tvNamaDompet)
        val tvJenis: TextView      = itemView.findViewById(R.id.tvTipeDompet)
        val tvSaldo: TextView      = itemView.findViewById(R.id.tvSaldoDompet)
        val tvTanggal: TextView    = itemView.findViewById(R.id.tvTanggalDompet)
        val viewStripe: View       = itemView.findViewById(R.id.viewJenisStripe)
        val viewIconBg: View       = itemView.findViewById(R.id.viewIconBg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DompetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dompet, parent, false)
        return DompetViewHolder(view)
    }

    override fun onBindViewHolder(holder: DompetViewHolder, position: Int) {
        val dompet = getItem(position)

        // Animasi fade-in
        val anim = AnimationUtils.loadAnimation(holder.itemView.context, android.R.anim.fade_in)
        anim.duration = 280
        holder.itemView.startAnimation(anim)

        holder.tvNama.text    = dompet.nama
        holder.tvJenis.text   = dompet.jenis
        holder.tvSaldo.text   = CurrencyFormatter.format(dompet.saldo)
        holder.tvTanggal.text = getRelativeDate(dompet.tanggalDibuat)

        // Warna stripe kiri + bg icon per jenis
        val style = getJenisStyle(dompet.jenis)
        try {
            holder.viewStripe.setBackgroundColor(Color.parseColor(style.stripeHex))
            holder.viewIconBg.setBackgroundColor(Color.parseColor(style.bgHex))
        } catch (e: Exception) {
            holder.viewStripe.setBackgroundColor(Color.parseColor("#37474F"))
            holder.viewIconBg.setBackgroundColor(Color.parseColor("#ECEFF1"))
        }

        // Icon per jenis
        holder.ivIkon.setImageResource(getIconRes(dompet.jenis))

        holder.card.setOnClickListener     { onItemClick(dompet) }
        holder.card.setOnLongClickListener { onItemLongClick(dompet) }
    }

    data class JenisStyle(val stripeHex: String, val bgHex: String)

    private fun getJenisStyle(jenis: String): JenisStyle = when (jenis) {
        "Rekening Bank"  -> JenisStyle("#1565C0", "#E3F2FD")
        "Dompet Digital" -> JenisStyle("#6A1B9A", "#F3E5F5")
        "Uang Tunai"     -> JenisStyle("#2E7D32", "#E8F5E9")
        "Investasi"      -> JenisStyle("#E65100", "#FFF3E0")
        "Tabungan"       -> JenisStyle("#00695C", "#E0F2F1")
        else             -> JenisStyle("#37474F", "#ECEFF1")
    }

    private fun getIconRes(jenis: String): Int = when (jenis) {
        "Rekening Bank"  -> R.drawable.ic_wallet_bank
        "Dompet Digital" -> R.drawable.ic_wallet_digital
        "Uang Tunai"     -> R.drawable.ic_wallet_cash
        "Investasi"      -> R.drawable.ic_wallet_investasi
        "Tabungan"       -> R.drawable.ic_wallet_tabungan
        else             -> R.drawable.ic_wallet_lainnya
    }

    private fun getRelativeDate(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val days = diff / (1000 * 60 * 60 * 24)
        return when {
            days == 0L -> "Baru ditambahkan"
            days == 1L -> "Kemarin"
            days < 7   -> "$days hari lalu"
            days < 30  -> "${days / 7} minggu lalu"
            else       -> "${days / 30} bulan lalu"
        }
    }
}
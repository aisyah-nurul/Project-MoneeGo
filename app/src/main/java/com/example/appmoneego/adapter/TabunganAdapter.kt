package com.example.appmoneego.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Tabungan
import com.example.appmoneego.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class TabunganAdapter(
    // MERGED: tambah parameter nominalVisible dengan default true (dari kode saya)
    nominalVisible: Boolean = true,
    private val onTabungClick: (Tabungan) -> Unit,
    private val onItemClick: (Tabungan) -> Unit
) : ListAdapter<Tabungan, TabunganAdapter.TabunganViewHolder>(DIFF_CALLBACK) {

    // MERGED: state privasi adapter (dari kode saya)
    private var isNominalVisible: Boolean = nominalVisible

    // MERGED: dipanggil dari TabunganFragment saat icon mata di-tap (dari kode saya)
    fun setNominalVisible(visible: Boolean) {
        isNominalVisible = visible
        notifyDataSetChanged()
    }

    // Mode diatur dari luar via setMode(), default BERJALAN
    private var mode: Mode = Mode.BERJALAN

    enum class Mode { BERJALAN, SELESAI }

    fun setMode(m: Mode) {
        mode = m
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabunganViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tabungan, parent, false)
        return TabunganViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabunganViewHolder, position: Int) {
        holder.bind(getItem(position), mode)
    }

    inner class TabunganViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvNama: TextView          = itemView.findViewById(R.id.tv_nama_tabungan)
        private val tvDeadline: TextView      = itemView.findViewById(R.id.tv_deadline)
        private val tvTargetLabel: View       = itemView.findViewById(R.id.tv_target_label)
        private val tvTargetNominal: TextView = itemView.findViewById(R.id.tv_target_nominal)
        private val tvLabelTerkumpul: TextView = itemView.findViewById(R.id.tv_label_terkumpul)
        private val tvTerkumpul: TextView     = itemView.findViewById(R.id.tv_terkumpul)
        private val tvPersentase: TextView    = itemView.findViewById(R.id.tv_persentase)
        private val progressBar: ProgressBar  = itemView.findViewById(R.id.progress_tabungan)
        private val tvSisa: TextView          = itemView.findViewById(R.id.tv_sisa)
        private val btnTabung: Button         = itemView.findViewById(R.id.btn_tabung)
        private val divider: View             = itemView.findViewById(R.id.divider)

        fun bind(item: Tabungan, mode: Mode) {
            val context = itemView.context
            tvNama.text = item.nama

            if (item.deadline != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val prefix = if (mode == Mode.SELESAI)
                    context.getString(R.string.label_selesai) + " "
                else
                    context.getString(R.string.label_deadline) + ": "
                tvDeadline.text = "$prefix${sdf.format(Date(item.deadline))}"
                tvDeadline.visibility = View.VISIBLE
            } else {
                tvDeadline.visibility = View.GONE
            }

            if (mode == Mode.BERJALAN) {
                tvTargetLabel.visibility      = View.VISIBLE
                tvTargetNominal.visibility    = View.VISIBLE
                tvLabelTerkumpul.visibility   = View.VISIBLE
                tvTerkumpul.visibility        = View.VISIBLE
                tvPersentase.visibility       = View.VISIBLE
                progressBar.visibility        = View.VISIBLE
                tvSisa.visibility             = View.VISIBLE
                btnTabung.visibility          = View.VISIBLE
                divider.visibility            = View.VISIBLE

                // MERGED: sembunyikan nominal saat mode privasi aktif (dari kode saya)
                // Yang disembunyikan: Target Nominal, Terkumpul, Sisa Target
                // Yang TETAP tampil:  Persentase (progress %) — sesuai spec
                if (isNominalVisible) {
                    tvTargetNominal.text = CurrencyFormatter.format(item.targetNominal)
                    tvTerkumpul.text     = CurrencyFormatter.format(item.terkumpul)
                    val sisa = item.targetNominal - item.terkumpul
                    // MERGED: pakai getString(R.string.label_kurang) dari kode teman
                    tvSisa.text = "⏰ ${context.getString(R.string.label_kurang)} ${CurrencyFormatter.format(sisa)}"
                } else {
                    tvTargetNominal.text = "Rp ***"
                    tvTerkumpul.text     = "Rp ***"
                    tvSisa.text          = "⏰ Kurang Rp ***"
                }

                // Progress persen SELALU tampil — sesuai spec pengecualian Bug 3
                val persen = if (item.targetNominal > 0)
                    ((item.terkumpul / item.targetNominal) * 100).toInt() else 0
                tvPersentase.text    = "$persen%"
                progressBar.progress = persen

                // MERGED: pakai getString(R.string.btn_tabung) dari kode teman
                btnTabung.text = context.getString(R.string.btn_tabung)
                btnTabung.setOnClickListener { onTabungClick(item) }

            } else {
                tvTargetLabel.visibility      = View.GONE
                tvTargetNominal.visibility    = View.GONE
                tvLabelTerkumpul.visibility   = View.GONE
                tvTerkumpul.visibility        = View.GONE
                tvPersentase.visibility       = View.GONE
                progressBar.visibility        = View.GONE
                tvSisa.visibility             = View.GONE
                btnTabung.visibility          = View.GONE
                divider.visibility            = View.GONE
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Tabungan>() {
            override fun areItemsTheSame(oldItem: Tabungan, newItem: Tabungan) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Tabungan, newItem: Tabungan) =
                oldItem == newItem
        }
    }
}
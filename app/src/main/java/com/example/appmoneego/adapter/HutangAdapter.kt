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
import java.text.SimpleDateFormat
import java.util.Locale

class HutangAdapter(
    private val lifecycleScope: LifecycleCoroutineScope,
    // BUG 3 FIX: tambah parameter nominalVisible untuk sinkronisasi privasi
    nominalVisible: Boolean = true,
    private val onItemClick: (Hutang) -> Unit
) : ListAdapter<Hutang, HutangAdapter.HutangViewHolder>(DiffCallback()) {

    // ══════════════════════════════════════════════════════════════════════════
    // BUG 3 FIX: state privasi adapter
    //
    // Saat isNominalVisible = false:
    //   - tvTotalHutang  → "Rp ***"
    //   - tvSudahDibayar → "Rp ***"
    //   - tvSisaHutang   → "Rp ***"
    //   - tvCicilanTerakhirNominal → "Rp ***"
    //
    // setNominalVisible() dipanggil dari HutangFragment setiap kali icon mata
    // di-tap, sehingga seluruh card di RecyclerView langsung ikut update.
    // ══════════════════════════════════════════════════════════════════════════
    private var isNominalVisible: Boolean = nominalVisible

    fun setNominalVisible(visible: Boolean) {
        isNominalVisible = visible
        notifyDataSetChanged()
    }

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

        // ── BUG 3 FIX: Nominal card hutang ────────────────────────────────────
        // Sembunyikan Total Hutang, Sudah Dibayar, Sisa Hutang saat mode privasi
        if (isNominalVisible) {
            b.tvTotalHutang.text  = CurrencyFormatter.format(hutang.totalHutang.toDouble())
            b.tvSudahDibayar.text = CurrencyFormatter.format(hutang.sudahDibayar.toDouble())
            b.tvSisaHutang.text   = CurrencyFormatter.format(hutang.sisaHutang.toDouble())
        } else {
            b.tvTotalHutang.text  = "Rp ***"
            b.tvSudahDibayar.text = "Rp ***"
            b.tvSisaHutang.text   = "Rp ***"
        }

        // Progress (progress persen TETAP tampil meski mode privasi aktif — sesuai spec)
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

        // ── BUG 4 FIX: Query cicilan terakhir ────────────────────────────────
        // Sebelumnya: cicilan.maxByOrNull { it.tanggalBayar }
        //   Ini sort string dengan format "dd/MM/yyyy" — string sort tidak
        //   akurat untuk format ini karena "14/06/2026" < "4/06/2026" secara
        //   leksikografis. Hasilnya: kadang cicilan PERTAMA yang tampil
        //   sebagai "cicilan terakhir".
        //
        // Sesudahnya: parse tanggal ke Date lalu ambil yang paling baru.
        //   Jika parse gagal (format tidak konsisten), fallback ke cicilan
        //   terakhir dalam list (yang sudah di-ORDER BY rowid ASC dari DAO).
        //   Ini menjamin "Cicilan terakhir" selalu = pembayaran paling baru.
        val dao = MoneeGoDatabase.getDatabase(holder.itemView.context).cicilanDao()
        lifecycleScope.launch {
            val cicilan = withContext(Dispatchers.IO) {
                dao.getCicilanByHutangId(hutang.id)
            }
            // Guard against RecyclerView recycling — pastikan view masih untuk item ini
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION || getItem(pos).id != hutang.id) return@launch

            if (cicilan.isNotEmpty()) {
                // Parse tanggal format "dd/MM/yyyy" → ambil yang paling baru
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                val terakhir = cicilan.maxByOrNull { c ->
                    // Coba parse; jika gagal kembalikan Long.MIN_VALUE agar
                    // cicilan dengan format aneh tidak terpilih sebagai "terakhir"
                    try { sdf.parse(c.tanggalBayar)?.time ?: Long.MIN_VALUE }
                    catch (e: Exception) { Long.MIN_VALUE }
                } ?: cicilan.last() // fallback ke elemen terakhir dari list (ORDER BY rowid ASC)

                // BUG 3 FIX: nominal cicilan terakhir juga disembunyikan
                b.tvCicilanTerakhirNominal.text = if (isNominalVisible)
                    CurrencyFormatter.format(terakhir.nominal.toDouble())
                else
                    "Rp ***"
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
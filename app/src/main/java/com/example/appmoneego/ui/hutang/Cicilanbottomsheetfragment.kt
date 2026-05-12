package com.example.appmoneego.hutang

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.CicilanEntity
import com.example.appmoneego.data.entity.Hutang
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CicilanBottomSheetFragment : BottomSheetDialogFragment() {

    private var hutang: Hutang? = null
    private var onSaved: ((Hutang) -> Unit)? = null

    companion object {
        private const val ARG_HUTANG = "hutang"

        fun newInstance(hutang: Hutang): CicilanBottomSheetFragment {
            val frag = CicilanBottomSheetFragment()
            frag.arguments = Bundle().apply { putSerializable(ARG_HUTANG, hutang) }
            return frag
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        hutang = arguments?.getSerializable(ARG_HUTANG) as? Hutang
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_cicilan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val h = hutang ?: return

        val tvNama         = view.findViewById<TextView>(R.id.tvSheetNama)
        val tvSisa         = view.findViewById<TextView>(R.id.tvSheetSisa)
        val tvSudahDibayar = view.findViewById<TextView>(R.id.tvSheetSudahDibayar)
        val tvPersen       = view.findViewById<TextView>(R.id.tvSheetPersen)
        val progressBar    = view.findViewById<ProgressBar>(R.id.progressSheet)
        val rvRiwayat      = view.findViewById<RecyclerView>(R.id.rvRiwayatCicilan)
        val etNominal      = view.findViewById<EditText>(R.id.etNominalCicilan)
        val etTanggal      = view.findViewById<EditText>(R.id.etTanggalCicilan)
        val etCatatan      = view.findViewById<EditText>(R.id.etCatatanCicilan)
        val btnSimpan      = view.findViewById<Button>(R.id.btnSimpanCicilan)
        val btnBatal       = view.findViewById<Button>(R.id.btnBatalCicilan)

        val db         = MoneeGoDatabase.getDatabase(requireContext())
        val cicilanDao = db.cicilanDao()
        val hutangDao  = db.hutangDao()
        val sdf        = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Tampilkan info hutang
        val sisaAwal  = (h.totalHutang - h.sudahDibayar).coerceAtLeast(0L)
        val persenAwal = if (h.totalHutang > 0)
            ((h.sudahDibayar.toDouble() / h.totalHutang) * 100).toInt() else 0

        tvNama?.text         = h.nama
        tvSisa?.text         = formatRupiah(sisaAwal)
        tvSudahDibayar?.text = formatRupiah(h.sudahDibayar)
        tvPersen?.text       = "$persenAwal%"
        progressBar?.progress = persenAwal

        // Tanggal default hari ini
        etTanggal?.setText(sdf.format(Date()))
        etTanggal?.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    cal.set(y, m, d)
                    etTanggal.setText(sdf.format(cal.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Load riwayat cicilan dari Room
        lifecycleScope.launch {
            val riwayat = withContext(Dispatchers.IO) {
                cicilanDao.getCicilanByHutangId(h.id)
            }
            rvRiwayat?.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = RiwayatCicilanAdapter(riwayat)
            }
        }

        // Simpan cicilan ke Room + update Hutang
        btnSimpan?.setOnClickListener {
            val nominalStr = etNominal?.text.toString().replace("[^0-9]".toRegex(), "")
            val nominal    = nominalStr.toLongOrNull() ?: 0L
            if (nominal <= 0L) {
                etNominal?.error = "Masukkan nominal cicilan"
                return@setOnClickListener
            }

            val tanggal = etTanggal?.text.toString().ifBlank { sdf.format(Date()) }
            val catatan = etCatatan?.text.toString()

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // 1. Simpan cicilan baru ke tabel cicilan
                    val cicilanBaru = CicilanEntity(
                        id           = UUID.randomUUID().toString(),
                        hutangId     = h.id,
                        nominal      = nominal,
                        tanggalBayar = tanggal,
                        catatan      = catatan
                    )
                    cicilanDao.insertCicilan(cicilanBaru)

                    // 2. Update sudahDibayar di tabel hutang
                    val newSudahDibayar = (h.sudahDibayar + nominal).coerceAtMost(h.totalHutang)
                    val updatedHutang   = h.copy(
                        sudahDibayar = newSudahDibayar,
                        selesai      = newSudahDibayar >= h.totalHutang
                    )
                    hutangDao.updateHutang(updatedHutang)
                }

                // 3. Notify fragment/activity agar refresh list
                val newSudahDibayar = (h.sudahDibayar + nominal).coerceAtMost(h.totalHutang)
                val updatedHutang   = h.copy(
                    sudahDibayar = newSudahDibayar,
                    selesai      = newSudahDibayar >= h.totalHutang
                )
                onSaved?.invoke(updatedHutang)

                if (updatedHutang.selesai) {
                    Toast.makeText(requireContext(), "🎉 Hutang ${h.nama} lunas!", Toast.LENGTH_SHORT).show()
                }
                dismiss()
            }
        }

        btnBatal?.setOnClickListener { dismiss() }
    }

    fun setOnCicilanSavedListener(listener: (Hutang) -> Unit) {
        onSaved = listener
    }

    private fun formatRupiah(value: Long): String =
        "Rp${String.format("%,d", value).replace(",", ".")}"
}

// ── Adapter riwayat cicilan ──────────────────────────────────────────────────

class RiwayatCicilanAdapter(
    private val list: List<com.example.appmoneego.data.entity.CicilanEntity>
) : RecyclerView.Adapter<RiwayatCicilanAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNominal: TextView = v.findViewById(R.id.tvRiwayatNominal)
        val tvTanggal: TextView = v.findViewById(R.id.tvRiwayatTanggal)
        val tvCatatan: TextView = v.findViewById(R.id.tvRiwayatCatatan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_cicilan, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c  = list[position]
        val ke = list.size - position
        holder.tvNominal.text = "+Rp${String.format("%,d", c.nominal).replace(",", ".")}"
        holder.tvTanggal.text = "Cicilan ke-$ke • ${c.tanggalBayar}"
        holder.tvCatatan.text = c.catatan.ifBlank { "-" }
    }

    override fun getItemCount() = list.size
}
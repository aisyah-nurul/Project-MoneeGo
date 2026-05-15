package com.example.appmoneego.ui.hutang

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private var onDeleted: ((Hutang) -> Unit)? = null

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
        val layoutForm     = view.findViewById<View>(R.id.layoutFormCicilan)
        val layoutLunas    = view.findViewById<View>(R.id.layoutLunas)
        val etNominal      = view.findViewById<EditText>(R.id.etNominalCicilan)
        val etTanggal      = view.findViewById<EditText>(R.id.etTanggalCicilan)
        val etCatatan      = view.findViewById<EditText>(R.id.etCatatanCicilan)
        val btnSimpan      = view.findViewById<Button>(R.id.btnSimpanCicilan)
        val btnBatal       = view.findViewById<Button>(R.id.btnBatalCicilan)
        val btnHapus       = view.findViewById<Button>(R.id.btnHapusHutang)

        val db         = MoneeGoDatabase.getDatabase(requireContext())
        val cicilanDao = db.cicilanDao()
        val hutangDao  = db.hutangDao()
        val sdf        = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun refreshInfo(current: Hutang) {
            val sisa   = (current.totalHutang - current.sudahDibayar).coerceAtLeast(0L)
            val persen = if (current.totalHutang > 0)
                ((current.sudahDibayar.toDouble() / current.totalHutang) * 100).toInt() else 0

            tvNama?.text          = current.nama
            tvSisa?.text          = formatRupiah(sisa)
            tvSudahDibayar?.text  = formatRupiah(current.sudahDibayar)
            tvPersen?.text        = "$persen%"
            progressBar?.progress = persen
        }

        refreshInfo(h)

        if (h.selesai) {
            layoutForm?.visibility  = View.GONE
            layoutLunas?.visibility = View.VISIBLE
        } else {
            layoutForm?.visibility  = View.VISIBLE
            layoutLunas?.visibility = View.GONE
        }

        etTanggal?.setText(sdf.format(Date()))
        etTanggal?.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
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

        fun loadRiwayat() {
            lifecycleScope.launch {
                val riwayat = withContext(Dispatchers.IO) {
                    cicilanDao.getCicilanByHutangId(h.id)
                }
                rvRiwayat?.layoutManager = LinearLayoutManager(requireContext())
                rvRiwayat?.adapter = RiwayatCicilanAdapter(
                    list = riwayat,
                    onHapus = { cicilan ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Hapus Cicilan?")
                            .setMessage("Cicilan sebesar ${formatRupiah(cicilan.nominal)} akan dihapus dan sisa hutang akan disesuaikan.")
                            .setPositiveButton("Hapus") { _, _ ->
                                lifecycleScope.launch {
                                    val updatedHutang = withContext(Dispatchers.IO) {
                                        cicilanDao.deleteCicilanById(cicilan.id)
                                        val currentHutang = hutangDao.getHutangById(h.id)
                                            ?: return@withContext null
                                        val newSudahDibayar = (currentHutang.sudahDibayar - cicilan.nominal)
                                            .coerceAtLeast(0L)
                                        val updated = currentHutang.copy(
                                            sudahDibayar = newSudahDibayar,
                                            selesai = newSudahDibayar >= currentHutang.totalHutang
                                        )
                                        hutangDao.updateHutang(updated)
                                        updated
                                    }
                                    if (updatedHutang != null) {
                                        refreshInfo(updatedHutang)
                                        loadRiwayat()
                                        onSaved?.invoke(updatedHutang)
                                    }
                                }
                            }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                )
            }
        }

        loadRiwayat()

        btnHapus?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Hutang?")
                .setMessage("Hutang \"${h.nama}\" dan semua riwayat cicilannya akan dihapus permanen.")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            cicilanDao.deleteCicilanByHutangId(h.id)
                            hutangDao.deleteHutang(h)
                        }
                        onDeleted?.invoke(h)
                        Toast.makeText(
                            requireContext(),
                            "Hutang ${h.nama} dihapus",
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

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
                val updatedHutang = withContext(Dispatchers.IO) {
                    val cicilanBaru = CicilanEntity(
                        id           = UUID.randomUUID().toString(),
                        hutangId     = h.id,
                        nominal      = nominal,
                        tanggalBayar = tanggal,
                        catatan      = catatan
                    )
                    cicilanDao.insertCicilan(cicilanBaru)

                    val newSudahDibayar = (h.sudahDibayar + nominal).coerceAtMost(h.totalHutang)
                    val updated = h.copy(
                        sudahDibayar = newSudahDibayar,
                        selesai      = newSudahDibayar >= h.totalHutang
                    )
                    hutangDao.updateHutang(updated)
                    updated
                }

                onSaved?.invoke(updatedHutang)

                if (updatedHutang.selesai) {
                    Toast.makeText(
                        requireContext(),
                        "🎉 Hutang ${h.nama} lunas!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dismiss()
            }
        }

        btnBatal?.setOnClickListener { dismiss() }
    }

    fun setOnCicilanSavedListener(listener: (Hutang) -> Unit) { onSaved = listener }
    fun setOnHutangDeletedListener(listener: (Hutang) -> Unit) { onDeleted = listener }

    private fun formatRupiah(value: Long): String =
        "Rp${String.format("%,d", value).replace(",", ".")}"
}


// ── RiwayatCicilanAdapter ────────────────────────────────────────────────────

class RiwayatCicilanAdapter(
    private val list: List<CicilanEntity>,
    private val onHapus: (CicilanEntity) -> Unit
) : RecyclerView.Adapter<RiwayatCicilanAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNominal : TextView    = v.findViewById(R.id.tvRiwayatNominal)
        val tvTanggal : TextView    = v.findViewById(R.id.tvRiwayatTanggal)
        val tvCatatan : TextView    = v.findViewById(R.id.tvRiwayatCatatan)
        val btnHapus  : ImageButton = v.findViewById(R.id.btnHapusCicilan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_cicilan, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = list[position]
        val nomorCicilan = position + 1
        holder.tvNominal.text = "+Rp${String.format("%,d", c.nominal).replace(",", ".")}"
        holder.tvTanggal.text = "Cicilan ke-$nomorCicilan • ${c.tanggalBayar}"
        holder.tvCatatan.text = c.catatan.ifBlank { "-" }
        holder.btnHapus.setOnClickListener { onHapus(c) }
    }

    override fun getItemCount() = list.size
}
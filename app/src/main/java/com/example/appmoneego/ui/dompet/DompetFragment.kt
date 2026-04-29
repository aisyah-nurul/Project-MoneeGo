package com.example.appmoneego.ui.dompet

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoneego.R
import com.example.appmoneego.adapter.DompetAdapter
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.VisibilityPrefs
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.HutangViewModel
import com.example.appmoneego.viewmodel.TransaksiViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DompetFragment : Fragment() {

    private lateinit var dompetViewModel:   DompetViewModel
    private lateinit var hutangViewModel:   HutangViewModel
    private lateinit var transaksiViewModel: TransaksiViewModel
    private lateinit var adapter:           DompetAdapter

    private lateinit var tvTotalSaldo:     TextView
    private lateinit var tvDompetTerbesar: TextView
    private lateinit var tvDompetAktif:    TextView
    private lateinit var btnTambah:        Button
    private lateinit var rvDompet:         RecyclerView
    private lateinit var layoutEmptyDompet: LinearLayout
    private lateinit var cardInfoHutang:   CardView
    private lateinit var tvInfoHutang:     TextView
    private lateinit var tvLihatHutang:    TextView

    // Tombol mata di halaman dompet
    private lateinit var ivToggleSaldo: ImageView

    private var totalSaldo     = 0.0
    // Baca state dari SharedPreferences agar konsisten
    private var nominalVisible = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dompet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Baca state mata
        nominalVisible = VisibilityPrefs.isNominalVisible(requireContext())

        initViews(view)
        setupDompetViewModel()
        setupHutangViewModel()
        setupTransaksiViewModel()
        setupRecyclerView()
        setupClickListeners()
        setupTombolMata()
        syncIkonMata()
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    private fun initViews(view: View) {
        tvTotalSaldo      = view.findViewById(R.id.tv_total_saldo)
        tvDompetTerbesar  = view.findViewById(R.id.tv_dompet_terbesar)
        tvDompetAktif     = view.findViewById(R.id.tv_dompet_aktif)
        btnTambah         = view.findViewById(R.id.btn_tambah_dompet)
        rvDompet          = view.findViewById(R.id.rv_dompet)
        layoutEmptyDompet = view.findViewById(R.id.layout_empty_dompet)
        cardInfoHutang    = view.findViewById(R.id.card_info_hutang)
        tvInfoHutang      = view.findViewById(R.id.tv_info_hutang)
        tvLihatHutang     = view.findViewById(R.id.tv_lihat_hutang)
        // Tombol mata — pastikan id ini ada di fragment_dompet.xml
        ivToggleSaldo     = view.findViewById(R.id.iv_toggle_saldo_dompet)
    }

    // ── ViewModels ───────────────────────────────────────────────────────────

    private fun setupDompetViewModel() {
        dompetViewModel = ViewModelProvider(this)[DompetViewModel::class.java]

        dompetViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            totalSaldo = total ?: 0.0
            // Tampilkan atau sembunyikan sesuai state
            tvTotalSaldo.text = if (nominalVisible)
                CurrencyFormatter.format(totalSaldo) else "Rp ***"
            adapter.notifyDataSetChanged()
        }

        dompetViewModel.jumlahDompet.observe(viewLifecycleOwner) { jumlah ->
            tvDompetAktif.text = (jumlah ?: 0).toString()
        }

        dompetViewModel.dompetTerbesar.observe(viewLifecycleOwner) { dompet ->
            tvDompetTerbesar.text = dompet?.nama ?: "-"
        }

        dompetViewModel.allDompet.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState(list.isEmpty())
        }
    }

    private fun setupTransaksiViewModel() {
        transaksiViewModel = ViewModelProvider(this)[TransaksiViewModel::class.java]
    }

    private fun setupHutangViewModel() {
        hutangViewModel = ViewModelProvider(this)[HutangViewModel::class.java]
        hutangViewModel.allHutang.observe(viewLifecycleOwner) { list ->
            val aktif = list.filter { !it.sudahLunas }
            if (aktif.isEmpty()) {
                cardInfoHutang.visibility = View.GONE
            } else {
                val totalHutang = aktif.sumOf { it.nominal }
                tvInfoHutang.text = "${aktif.size} hutang · Total ${CurrencyFormatter.format(totalHutang)}"
                cardInfoHutang.visibility = View.VISIBLE
            }
        }
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = DompetAdapter(
            onItemClick     = { dompet -> showEditDeleteDialog(dompet) },
            onItemLongClick = { dompet -> showEditDeleteDialog(dompet); true },
            totalSaldo      = { totalSaldo },
            // Teruskan state mata ke DompetAdapter agar saldo per item ikut tersembunyi
            nominalVisible  = { nominalVisible }
        )
        rvDompet.layoutManager = LinearLayoutManager(requireContext())
        rvDompet.adapter = adapter
    }

    // ── Tombol Mata ──────────────────────────────────────────────────────────

    private fun setupTombolMata() {
        ivToggleSaldo.setOnClickListener {
            nominalVisible = !nominalVisible
            VisibilityPrefs.setNominalVisible(requireContext(), nominalVisible)
            syncIkonMata()

            // Refresh total saldo header
            tvTotalSaldo.text = if (nominalVisible)
                CurrencyFormatter.format(totalSaldo) else "Rp ***"

            // Refresh list item dompet
            adapter.notifyDataSetChanged()
        }
    }

    private fun syncIkonMata() {
        ivToggleSaldo.setImageResource(
            if (nominalVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
        )
    }

    // ── Click Listeners ──────────────────────────────────────────────────────

    private fun setupClickListeners() {
        btnTambah.setOnClickListener { showTambahDompetDialog() }
        view?.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        tvLihatHutang.setOnClickListener {
            findNavController().navigate(R.id.action_dompetFragment_to_hutangFragment)
        }
    }

    // ── BottomSheet Tambah / Edit Dompet ────────────────────────────────────

    private fun showTambahDompetDialog(dompetEdit: Dompet? = null) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.dialog_tambah_dompet, null)
        dialog.setContentView(v)
        dialog.behavior.apply { state = BottomSheetBehavior.STATE_EXPANDED; skipCollapsed = true }

        val etNama          = v.findViewById<TextInputEditText>(R.id.etNamaDompet)
        val etSaldo         = v.findViewById<TextInputEditText>(R.id.etSaldoAwal)
        val tilNama         = v.findViewById<TextInputLayout>(R.id.tilNamaDompet)
        val btnSimpan       = v.findViewById<Button>(R.id.btnSimpanDompet)
        val btnPilihTanggal = v.findViewById<LinearLayout>(R.id.btnPilihTanggal)
        val tvTanggal       = v.findViewById<TextView>(R.id.tvTanggalDipilih)

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        var tanggalDipilih: Long = System.currentTimeMillis()
        tvTanggal.text = sdf.format(tanggalDipilih)
        tvTanggal.setTextColor(resources.getColor(android.R.color.black, null))

        dompetEdit?.let {
            etNama.setText(it.nama)
            etSaldo.setText(it.saldo.toLong().toString())
            btnSimpan.text = "Simpan Perubahan"
            tanggalDipilih = it.tanggalDibuat
            tvTanggal.text = sdf.format(tanggalDipilih)
            tvTanggal.setTextColor(resources.getColor(android.R.color.black, null))
        }

        btnPilihTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = tanggalDipilih
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                tanggalDipilih = cal.timeInMillis
                tvTanggal.text = sdf.format(tanggalDipilih)
                tvTanggal.setTextColor(resources.getColor(android.R.color.black, null))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        var jenisTerpilih = dompetEdit?.jenis ?: "Lainnya"
        val jenisMap = mapOf(
            v.findViewById<LinearLayout>(R.id.btnJenisBank)      to "Rekening Bank",
            v.findViewById<LinearLayout>(R.id.btnJenisDigital)   to "Dompet Digital",
            v.findViewById<LinearLayout>(R.id.btnJenisTunai)     to "Uang Tunai",
            v.findViewById<LinearLayout>(R.id.btnJenisInvestasi) to "Investasi",
            v.findViewById<LinearLayout>(R.id.btnJenisTabungan)  to "Tabungan",
            v.findViewById<LinearLayout>(R.id.btnJenisLainnya)   to "Lainnya"
        )

        fun highlight(selected: LinearLayout) {
            jenisMap.keys.forEach { btn ->
                btn.isSelected = (btn == selected)
                btn.alpha = if (btn == selected) 1f else 0.6f
            }
        }

        jenisMap.entries.find { it.value == jenisTerpilih }
            ?.let { highlight(it.key) }
            ?: highlight(v.findViewById(R.id.btnJenisLainnya))

        jenisMap.forEach { (btn, jenis) ->
            btn.setOnClickListener { jenisTerpilih = jenis; highlight(btn) }
        }

        btnSimpan.setOnClickListener {
            val nama     = etNama.text.toString().trim()
            val saldoStr = etSaldo.text.toString().trim()
            if (nama.isEmpty()) { tilNama.error = "Nama dompet tidak boleh kosong"; return@setOnClickListener }
            tilNama.error = null
            val saldo = if (saldoStr.isEmpty()) 0.0 else CurrencyFormatter.parse(saldoStr)

            if (dompetEdit == null) {
                val dompetBaru = Dompet(nama = nama, jenis = jenisTerpilih, saldo = saldo,
                    ikon = getIkonByJenis(jenisTerpilih), tanggalDibuat = tanggalDipilih)
                dompetViewModel.insert(dompetBaru)
                if (saldo > 0) {
                    dompetViewModel.allDompet.observe(viewLifecycleOwner) { listDompet ->
                        val dompetBaik = listDompet.firstOrNull { it.nama == nama && it.jenis == jenisTerpilih }
                        dompetBaik?.let { d ->
                            transaksiViewModel.insertTanpaUpdateSaldo(Transaksi(
                                nominal = saldo, jenis = "PEMASUKAN", kategori = "Saldo Awal",
                                catatan = "Saldo awal dompet ${d.nama}",
                                tanggal = tanggalDipilih, dompetId = d.id))
                        }
                    }
                }
                Toast.makeText(requireContext(), "Dompet \"$nama\" ditambahkan!", Toast.LENGTH_SHORT).show()
            } else {
                dompetViewModel.update(dompetEdit.copy(nama = nama, jenis = jenisTerpilih,
                    saldo = saldo, ikon = getIkonByJenis(jenisTerpilih), tanggalDibuat = tanggalDipilih))
                Toast.makeText(requireContext(), "Dompet diperbarui!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    // ── Edit / Hapus ─────────────────────────────────────────────────────────

    private fun showEditDeleteDialog(dompet: Dompet) {
        AlertDialog.Builder(requireContext())
            .setTitle(dompet.nama)
            .setItems(arrayOf("✏️  Edit Dompet", "🗑️  Hapus Dompet")) { _, which ->
                when (which) {
                    0 -> showTambahDompetDialog(dompetEdit = dompet)
                    1 -> konfirmasiHapus(dompet)
                }
            }.show()
    }

    private fun konfirmasiHapus(dompet: Dompet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Dompet")
            .setMessage("Yakin ingin menghapus \"${dompet.nama}\"?\nSaldo ${CurrencyFormatter.format(dompet.saldo)} akan hilang.")
            .setPositiveButton("Hapus") { _, _ ->
                dompetViewModel.delete(dompet)
                Toast.makeText(requireContext(), "Dompet dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null).show()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateEmptyState(isEmpty: Boolean) {
        rvDompet.visibility          = if (isEmpty) View.GONE else View.VISIBLE
        layoutEmptyDompet.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun getIkonByJenis(jenis: String): String = when (jenis) {
        "Rekening Bank"  -> "ic_wallet_bank"
        "Dompet Digital" -> "ic_wallet_digital"
        "Uang Tunai"     -> "ic_wallet_cash"
        "Investasi"      -> "ic_wallet_investasi"
        "Tabungan"       -> "ic_wallet_tabungan"
        else             -> "ic_wallet_lainnya"
    }
}
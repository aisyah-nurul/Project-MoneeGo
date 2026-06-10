package com.example.appmoneego.ui.dompet

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
import com.example.appmoneego.ui.hutang.HutangViewModel
import com.example.appmoneego.ui.hutang.sisaHutang
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.VisibilityPrefs
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.TransaksiViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DompetFragment : Fragment() {

    private lateinit var dompetViewModel:    DompetViewModel
    private lateinit var hutangViewModel:    HutangViewModel
    private lateinit var transaksiViewModel: TransaksiViewModel
    private lateinit var adapter:            DompetAdapter

    private lateinit var tvTotalSaldo:      TextView
    private lateinit var tvDompetTerbesar:  TextView
    private lateinit var tvDompetAktif:     TextView
    private lateinit var btnTambah:         Button
    private lateinit var rvDompet:          RecyclerView
    private lateinit var layoutEmptyDompet: LinearLayout
    private lateinit var cardInfoHutang:    CardView
    private lateinit var tvInfoHutang:      TextView
    private lateinit var tvLihatHutang:     TextView
    private lateinit var ivToggleSaldo:     ImageView

    private var totalSaldo     = 0.0
    private var nominalVisible = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dompet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    override fun onResume() {
        super.onResume()
        nominalVisible = VisibilityPrefs.isNominalVisible(requireContext())
        syncIkonMata()
        tvTotalSaldo.text = if (nominalVisible)
            CurrencyFormatter.format(totalSaldo) else "Rp ***"
        adapter.notifyDataSetChanged()
    }

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
        ivToggleSaldo     = view.findViewById(R.id.iv_toggle_saldo_dompet)
    }

    private fun setupDompetViewModel() {
        dompetViewModel = ViewModelProvider(this)[DompetViewModel::class.java]

        dompetViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            totalSaldo = total ?: 0.0
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

        hutangViewModel.hutangList.observe(viewLifecycleOwner) { list ->
            val aktif = list.filter { !it.selesai }
            if (aktif.isEmpty()) {
                cardInfoHutang.visibility = View.GONE
            } else {
                val totalHutang = aktif.sumOf { it.sisaHutang.toDouble() }
                tvInfoHutang.text =
                    "${aktif.size} hutang · Total ${CurrencyFormatter.format(totalHutang)}"
                cardInfoHutang.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DompetAdapter(
            onItemClick     = { dompet -> showEditDeleteDialog(dompet) },
            onItemLongClick = { dompet -> showEditDeleteDialog(dompet); true },
            totalSaldo      = { totalSaldo },
            nominalVisible  = { nominalVisible }
        )
        rvDompet.layoutManager = LinearLayoutManager(requireContext())
        rvDompet.adapter = adapter
    }

    private fun setupTombolMata() {
        ivToggleSaldo.setOnClickListener {
            nominalVisible = !nominalVisible
            VisibilityPrefs.setNominalVisible(requireContext(), nominalVisible)
            syncIkonMata()
            tvTotalSaldo.text = if (nominalVisible)
                CurrencyFormatter.format(totalSaldo) else "Rp ***"
            adapter.notifyDataSetChanged()
        }
    }

    private fun syncIkonMata() {
        ivToggleSaldo.setImageResource(
            if (nominalVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
        )
    }

    private fun setupClickListeners() {
        btnTambah.setOnClickListener { showTambahDompetDialog() }
        view?.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        tvLihatHutang.setOnClickListener {
            findNavController().navigate(R.id.action_dompetFragment_to_hutangFragment)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        rvDompet.visibility          = if (isEmpty) View.GONE    else View.VISIBLE
        layoutEmptyDompet.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showTambahDompetDialog(dompetEdit: Dompet? = null) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.dialog_tambah_dompet, null)
        dialog.setContentView(v)
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        val etNama    = v.findViewById<TextInputEditText>(R.id.etNamaDompet)
        val etSaldo   = v.findViewById<TextInputEditText>(R.id.etSaldoAwal)
        val tilNama   = v.findViewById<TextInputLayout>(R.id.tilNamaDompet)
        val btnSimpan = v.findViewById<Button>(R.id.btnSimpanDompet)

        var tanggalDipilih: Long = System.currentTimeMillis()

        dompetEdit?.let {
            etNama.setText(it.nama)
            etSaldo.setText(it.saldo.toLong().toString())
            btnSimpan.text = getString(R.string.btn_simpan_perubahan)
            tanggalDipilih = it.tanggalDibuat
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
                btn.alpha = if (btn == selected) 1f else 0.9f
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
            if (nama.isEmpty()) {
                tilNama.error = getString(R.string.error_nama_dompet_kosong)
                return@setOnClickListener
            }
            tilNama.error = null
            val saldo = if (saldoStr.isEmpty()) 0.0 else CurrencyFormatter.parse(saldoStr)

            if (dompetEdit == null) {
                val dompetBaru = Dompet(
                    nama          = nama,
                    jenis         = jenisTerpilih,
                    saldo         = saldo,
                    ikon          = getIkonByJenis(jenisTerpilih),
                    tanggalDibuat = tanggalDipilih
                )
                dompetViewModel.insert(dompetBaru)

                if (saldo > 0) {
                    // ── PERUBAHAN: observer sekali pakai agar tidak insert berulang ──
                    // Gunakan Observer manual yang di-remove setelah insert pertama
                    val observer = object : androidx.lifecycle.Observer<List<Dompet>> {
                        override fun onChanged(listDompet: List<Dompet>) {
                            val dompetBaik = listDompet.firstOrNull {
                                it.nama == nama && it.jenis == jenisTerpilih
                            }
                            if (dompetBaik != null) {
                                // ── PERUBAHAN UTAMA ─────────────────────────────────
                                // kategori = nama dompet  → icon di riwayat ikut jenis
                                // catatan  = jenis dompet → adapter baca untuk icon
                                transaksiViewModel.insertTanpaUpdateSaldo(
                                    Transaksi(
                                        nominal  = saldo,
                                        jenis    = "PEMASUKAN",
                                        kategori = dompetBaik.nama,          // ← nama dompet
                                        catatan  = dompetBaik.jenis,         // ← jenis dompet
                                        tanggal  = tanggalDipilih,
                                        dompetId = dompetBaik.id
                                    )
                                )
                                // Remove observer agar tidak insert lagi saat list update
                                dompetViewModel.allDompet.removeObserver(this)
                            }
                        }
                    }
                    dompetViewModel.allDompet.observe(viewLifecycleOwner, observer)
                }

                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_dompet_ditambahkan, nama),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                dompetViewModel.update(
                    dompetEdit.copy(
                        nama          = nama,
                        jenis         = jenisTerpilih,
                        saldo         = saldo,
                        ikon          = getIkonByJenis(jenisTerpilih),
                        tanggalDibuat = tanggalDipilih
                    )
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_dompet_diperbarui),
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showEditDeleteDialog(dompet: Dompet) {
        AlertDialog.Builder(requireContext())
            .setTitle(dompet.nama)
            .setItems(
                arrayOf(
                    getString(R.string.menu_edit_dompet),
                    getString(R.string.menu_hapus_dompet)
                )
            ) { _, which ->
                when (which) {
                    0 -> showTambahDompetDialog(dompetEdit = dompet)
                    1 -> konfirmasiHapus(dompet)
                }
            }.show()
    }

    private fun konfirmasiHapus(dompet: Dompet) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_hapus_dompet_title))
            .setMessage(
                getString(
                    R.string.dialog_hapus_dompet_pesan,
                    dompet.nama,
                    CurrencyFormatter.format(dompet.saldo)
                )
            )
            .setPositiveButton(getString(R.string.dialog_hapus_transaksi_konfirmasi)) { _, _ ->
                dompetViewModel.delete(dompet)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_dompet_dihapus),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(getString(R.string.btn_batal), null)
            .show()
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
package com.example.appmoneego.ui.dompet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.viewmodel.DompetViewModel
import com.example.appmoneego.viewmodel.HutangViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DompetFragment : Fragment() {

    private lateinit var dompetViewModel: DompetViewModel
    private lateinit var hutangViewModel: HutangViewModel
    private lateinit var adapter: DompetAdapter

    private lateinit var tvTotalSaldo: TextView
    private lateinit var tvDompetTerbesar: TextView
    private lateinit var tvDompetAktif: TextView
    private lateinit var btnTambah: Button
    private lateinit var rvDompet: RecyclerView
    private lateinit var layoutEmptyDompet: LinearLayout
    private lateinit var cardInfoHutang: CardView
    private lateinit var tvInfoHutang: TextView
    private lateinit var tvLihatHutang: TextView

    private var totalSaldo = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dompet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupDompetViewModel()
        setupHutangViewModel()
        setupRecyclerView()
        setupClickListeners()
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

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
    }

    // ─── Dompet ViewModel ────────────────────────────────────────────────────

    private fun setupDompetViewModel() {
        dompetViewModel = ViewModelProvider(this)[DompetViewModel::class.java]

        dompetViewModel.totalSaldo.observe(viewLifecycleOwner) { total ->
            totalSaldo = total ?: 0.0
            tvTotalSaldo.text = CurrencyFormatter.format(totalSaldo)
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

    // ─── Hutang ViewModel — hanya untuk banner info ───────────────────────────

    private fun setupHutangViewModel() {
        hutangViewModel = ViewModelProvider(this)[HutangViewModel::class.java]

        hutangViewModel.allHutang.observe(viewLifecycleOwner) { list ->
            val aktif = list.filter { !it.sudahLunas }
            if (aktif.isEmpty()) {
                cardInfoHutang.visibility = View.GONE
            } else {
                val totalHutang = aktif.sumOf { it.nominal }
                val jumlah = aktif.size
                tvInfoHutang.text = "$jumlah hutang · Total ${CurrencyFormatter.format(totalHutang)}"
                cardInfoHutang.visibility = View.VISIBLE
            }
        }
    }

    // ─── RecyclerView ────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = DompetAdapter(
            onItemClick     = { dompet -> showEditDeleteDialog(dompet) },
            onItemLongClick = { dompet -> showEditDeleteDialog(dompet); true },
            totalSaldo      = { totalSaldo }
        )
        rvDompet.layoutManager = LinearLayoutManager(requireContext())
        rvDompet.adapter = adapter
    }

    // ─── Click Listeners ─────────────────────────────────────────────────────

    private fun setupClickListeners() {
        btnTambah.setOnClickListener { showTambahDompetDialog() }

        view?.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        tvLihatHutang.setOnClickListener {
            findNavController().navigate(R.id.action_dompetFragment_to_hutangFragment)
        }
    }

    // ─── BottomSheet Tambah / Edit Dompet ────────────────────────────────────

    private fun showTambahDompetDialog(dompetEdit: Dompet? = null) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.dialog_tambah_dompet, null)
        dialog.setContentView(v)

        // Biar konten naik saat keyboard muncul dan tombol Simpan tidak tertutup
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        val etNama    = v.findViewById<TextInputEditText>(R.id.etNamaDompet)
        val etSaldo   = v.findViewById<TextInputEditText>(R.id.etSaldoAwal)
        val tilNama   = v.findViewById<TextInputLayout>(R.id.tilNamaDompet)
        val btnSimpan = v.findViewById<Button>(R.id.btnSimpanDompet)

        dompetEdit?.let {
            etNama.setText(it.nama)
            etSaldo.setText(it.saldo.toLong().toString())
            btnSimpan.text = "Simpan Perubahan"
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

        // Set highlight default
        jenisMap.entries.find { it.value == jenisTerpilih }
            ?.let { highlight(it.key) }
            ?: highlight(v.findViewById(R.id.btnJenisLainnya))

        jenisMap.forEach { (btn, jenis) ->
            btn.setOnClickListener {
                jenisTerpilih = jenis
                highlight(btn)
            }
        }

        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val saldoStr = etSaldo.text.toString().trim()

            if (nama.isEmpty()) {
                tilNama.error = "Nama dompet tidak boleh kosong"
                return@setOnClickListener
            }
            tilNama.error = null

            val saldo = if (saldoStr.isEmpty()) 0.0
            else CurrencyFormatter.parse(saldoStr)

            if (dompetEdit == null) {
                dompetViewModel.insert(
                    Dompet(
                        nama  = nama,
                        jenis = jenisTerpilih,
                        saldo = saldo,
                        ikon  = getIkonByJenis(jenisTerpilih)
                    )
                )
                Toast.makeText(requireContext(), "Dompet \"$nama\" ditambahkan!", Toast.LENGTH_SHORT).show()
            } else {
                dompetViewModel.update(
                    dompetEdit.copy(
                        nama  = nama,
                        jenis = jenisTerpilih,
                        saldo = saldo,
                        ikon  = getIkonByJenis(jenisTerpilih)
                    )
                )
                Toast.makeText(requireContext(), "Dompet diperbarui!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    // ─── Edit / Hapus ────────────────────────────────────────────────────────

    private fun showEditDeleteDialog(dompet: Dompet) {
        AlertDialog.Builder(requireContext())
            .setTitle(dompet.nama)
            .setItems(arrayOf("✏️  Edit Dompet", "🗑️  Hapus Dompet")) { _, which ->
                when (which) {
                    0 -> showTambahDompetDialog(dompetEdit = dompet)
                    1 -> konfirmasiHapus(dompet)
                }
            }
            .show()
    }

    private fun konfirmasiHapus(dompet: Dompet) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Dompet")
            .setMessage(
                "Yakin ingin menghapus \"${dompet.nama}\"?\n" +
                        "Saldo ${CurrencyFormatter.format(dompet.saldo)} akan hilang."
            )
            .setPositiveButton("Hapus") { _, _ ->
                dompetViewModel.delete(dompet)
                Toast.makeText(requireContext(), "Dompet dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
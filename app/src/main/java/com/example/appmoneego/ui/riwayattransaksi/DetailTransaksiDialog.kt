package com.example.appmoneego.ui.riwayattransaksi

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.databinding.DialogDetailTransaksiBinding
import com.example.appmoneego.utils.CurrencyFormatter
import com.example.appmoneego.utils.DateUtils

class DetailTransaksiDialog(
    private val transaksi: Transaksi,
    private val daftarDompet: List<Dompet>,
    private val onEditClick: (Transaksi) -> Unit,
    private val onDeleteClick: (Transaksi) -> Unit
) : DialogFragment() {

    private var _binding: DialogDetailTransaksiBinding? = null
    private val binding get() = _binding!!

    // Map ikon kategori standar
    private val kategoriIcon = mapOf(
        "Makanan"           to R.drawable.ic_makanan,
        "Fashion"           to R.drawable.ic_fashion,
        "Transportasi"      to R.drawable.ic_transportasi,
        "Pendidikan"        to R.drawable.ic_pendidikan,
        "Sosial"            to R.drawable.ic_sosial,
        "Kesehatan"         to R.drawable.ic_kesehatan,
        "Rumah Tangga"      to R.drawable.ic_rumahtangga,
        "Kebutuhan Pribadi" to R.drawable.ic_kebutuhanpribadi,
        "Gaji"              to R.drawable.ic_gaji,
        "Bonus"             to R.drawable.ic_bonus,
        "Freelance"         to R.drawable.ic_freelance,
        "Investasi"         to R.drawable.ic_investasi,
        "Hadiah"            to R.drawable.ic_hadiah,
        "Penjualan"         to R.drawable.ic_penjualan,
        "Saldo Awal"        to R.drawable.ic_wallet,
        "Transfer"          to R.drawable.ic_wallet
    )

    // ══════════════════════════════════════════════════════════════════════════
    // BUG 2 FIX: Daftar kategori yang harus memakai icon DOMPET SUMBER DANA.
    //
    // "Hutang" masuk daftar ini karena:
    //   - Saat bayar hutang, transaksi disimpan dengan kategori = "Hutang"
    //     dan dompetId = id dompet yang dipakai (Gopay, BCA, dll.)
    //   - Popup detail harus menampilkan icon dompet tersebut, bukan ic_hutang
    //   - Ini konsisten dengan perubahan di TransaksiAdapter.resolveIcon()
    // ══════════════════════════════════════════════════════════════════════════
    private val kategoriPakaiIconDompet = setOf("Hutang")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDetailTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        isiData()
        setupTombol()
    }

    private fun isiData() {
        val isTransfer  = transaksi.kategori == "Transfer"
        val isPemasukan = transaksi.jenis == "PEMASUKAN"

        // ── Resolve icon ────────────────────────────────────────────────────
        //
        // Priority:
        //   1. Jika kategori ada di kategoriPakaiIconDompet (mis. "Hutang")
        //      → ambil icon dari jenis dompet sumber dana
        //   2. Jika PEMASUKAN non-transfer → icon dompet (konsisten dengan
        //      tampilan card di Riwayat Transaksi)
        //   3. Sisanya → icon kategori standar
        val iconRes = when {
            transaksi.kategori in kategoriPakaiIconDompet -> {
                // BUG 2 FIX: Bayar hutang → icon dompet sumber dana
                val dompetAsal = daftarDompet.find { it.id == transaksi.dompetId }
                getIconDompet(dompetAsal?.jenis ?: "Lainnya")
            }
            !isTransfer && isPemasukan -> {
                // Pemasukan non-transfer → icon dompet (sudah benar sebelumnya)
                val dompetAsal = daftarDompet.find { it.id == transaksi.dompetId }
                getIconDompet(dompetAsal?.jenis ?: "Lainnya")
            }
            else -> {
                // Pengeluaran / Transfer biasa → icon kategori
                kategoriIcon[transaksi.kategori] ?: R.drawable.ic_wallet
            }
        }
        binding.ivDialogIcon.setImageResource(iconRes)

        // Nama kategori
        binding.tvDialogKategori.text = getKategoriText(transaksi.kategori)

        // Nominal + warna
        // MERGED: pakai R.color.text_primary (dari kode saya) untuk transfer,
        // bukan hardcode hex 0xFF1A2B34, agar mengikuti tema aplikasi.
        when {
            isTransfer -> {
                binding.tvDialogNominal.text = CurrencyFormatter.format(transaksi.nominal)
                binding.tvDialogNominal.setTextColor(requireContext().getColor(R.color.text_primary))
            }
            isPemasukan -> {
                binding.tvDialogNominal.text = "+${CurrencyFormatter.format(transaksi.nominal)}"
                binding.tvDialogNominal.setTextColor(0xFF2E7D32.toInt())
            }
            else        -> {
                binding.tvDialogNominal.text = "-${CurrencyFormatter.format(transaksi.nominal)}"
                binding.tvDialogNominal.setTextColor(0xFFC62828.toInt())
            }
        }

        // Tanggal
        binding.tvDialogTanggal.text = DateUtils.formatTanggalPendek(transaksi.tanggal)

        if (isTransfer) {
            binding.rowDompet.visibility     = View.GONE
            binding.rowCatatan.visibility    = View.GONE
            binding.rowDariDompet.visibility = View.VISIBLE
            binding.rowKeDompet.visibility   = View.VISIBLE

            val namaDompetAsal = daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"
            binding.tvDialogDariDompet.text = namaDompetAsal

            val namaDompetTujuan = parseDompetTujuan(transaksi.catatan)
            binding.tvDialogKeDompet.text = namaDompetTujuan

        } else {
            binding.rowDompet.visibility     = View.VISIBLE
            binding.rowCatatan.visibility    = View.VISIBLE
            binding.rowDariDompet.visibility = View.GONE
            binding.rowKeDompet.visibility   = View.GONE

            val namaDompet = daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"

            binding.tvDialogDompet.text = when (namaDompet) {
                "Uang Tunai" -> getString(R.string.jenis_uang_tunai)
                "Rekening Bank" -> getString(R.string.jenis_rekening_bank)
                "Dompet Digital" -> getString(R.string.jenis_dompet_digital)
                "Investasi" -> getString(R.string.jenis_investasi)
                "Tabungan" -> getString(R.string.jenis_tabungan)
                else -> namaDompet
            }

            binding.tvDialogCatatan.text = transaksi.catatan.ifEmpty { "-" }
        }
    }

    private fun getKategoriText(kategori: String): String {
        return when (kategori) {
            "Makanan"           -> getString(R.string.kat_makanan)
            "Fashion"           -> getString(R.string.kat_fashion)
            "Transportasi"      -> getString(R.string.kat_transportasi)
            "Pendidikan"        -> getString(R.string.kat_pendidikan)
            "Sosial"            -> getString(R.string.kat_sosial)
            "Kesehatan"         -> getString(R.string.kat_kesehatan)
            "Rumah Tangga"      -> getString(R.string.kat_rumah_tangga)
            "Kebutuhan Pribadi" -> getString(R.string.kat_kebutuhan_pribadi)
            "Gaji"              -> getString(R.string.kat_gaji)
            "Bonus"             -> getString(R.string.kat_bonus)
            "Freelance"         -> getString(R.string.kat_freelance)
            "Investasi"         -> getString(R.string.kat_investasi)
            "Hadiah"            -> getString(R.string.kat_hadiah)
            "Penjualan"         -> getString(R.string.kat_penjualan)
            else                -> kategori
        }
    }

    private fun getIconDompet(jenis: String): Int = when (jenis) {
        "Rekening Bank"  -> R.drawable.ic_wallet_bank
        "Dompet Digital" -> R.drawable.ic_wallet_digital
        "Uang Tunai"     -> R.drawable.ic_wallet_cash
        "Investasi"      -> R.drawable.ic_wallet_investasi
        "Tabungan"       -> R.drawable.ic_wallet_tabungan
        else             -> R.drawable.ic_wallet_lainnya
    }

    private fun parseDompetTujuan(catatan: String): String {
        return if (catatan.startsWith("transfer_to:")) {
            val idTujuan = catatan.removePrefix("transfer_to:").trim().toIntOrNull()
            daftarDompet.find { it.id == idTujuan }?.nama ?: catatan
        } else {
            catatan.ifEmpty { "-" }
        }
    }

    private fun setupTombol() {
        // Tombol X: tutup dialog
        binding.btnClose.setOnClickListener { dismiss() }

        // Tombol Edit
        binding.btnEdit.setOnClickListener {
            onEditClick(transaksi)
            dismiss()
        }

        // MERGED: Tombol Hapus dengan konfirmasi AlertDialog terlebih dahulu
        // (dari kode saya) agar pengguna tidak tidak sengaja menghapus transaksi.
        binding.btnDelete.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_transaction))
                .setMessage(
                    getString(
                        R.string.delete_transaction_message,
                        transaksi.catatan.ifEmpty { transaksi.kategori }
                    )
                )
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    onDeleteClick(transaksi)
                    dismiss()
                }
                .setNegativeButton(getString(R.string.btn_batal), null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DetailTransaksiDialog"
    }
}
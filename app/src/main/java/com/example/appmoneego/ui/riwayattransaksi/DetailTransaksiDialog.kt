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

    // Map ikon kategori — dipakai untuk PENGELUARAN dan TRANSFER
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Hapus title bar bawaan dialog
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

        // Latar belakang dialog transparan agar corner radius card terlihat
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Lebar dialog = 90% lebar layar
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        isiData()
        setupTombol()
    }

    // ── Isi semua data ke view ─────────────────────────────────────────────

    private fun isiData() {
        val isTransfer    = transaksi.kategori == "Transfer"
        val isPemasukan   = transaksi.jenis == "PEMASUKAN"

        // ── FIX BUG 1: Icon untuk PEMASUKAN non-transfer ────────────────────
        // Gunakan icon berdasarkan JENIS DOMPET asal transaksi (sama seperti
        // mapping di DompetAdapter), bukan icon kategori — karena kategori
        // PEMASUKAN tidak punya mapping icon yang relevan dengan dompet.
        // PENGELUARAN dan TRANSFER tetap pakai kategoriIcon seperti semula.
        val iconRes = if (!isTransfer && isPemasukan) {
            val dompetAsal = daftarDompet.find { it.id == transaksi.dompetId }
            getIconDompet(dompetAsal?.jenis ?: "Lainnya")
        } else {
            kategoriIcon[transaksi.kategori] ?: R.drawable.ic_wallet
        }
        binding.ivDialogIcon.setImageResource(iconRes)

        // Nama kategori
        binding.tvDialogKategori.text = transaksi.kategori

        // Nominal + warna
        when {
            isTransfer  -> {
                // Transfer: warna hitam, tanpa prefix +/-
                binding.tvDialogNominal.text      = CurrencyFormatter.format(transaksi.nominal)
                binding.tvDialogNominal.setTextColor(0xFF1A2B34.toInt()) // text_primary
            }
            isPemasukan -> {
                // Pemasukan: warna hijau, prefix +
                binding.tvDialogNominal.text      = "+${CurrencyFormatter.format(transaksi.nominal)}"
                binding.tvDialogNominal.setTextColor(0xFF2E7D32.toInt()) // income_green
            }
            else        -> {
                // Pengeluaran: warna merah, prefix -
                binding.tvDialogNominal.text      = "-${CurrencyFormatter.format(transaksi.nominal)}"
                binding.tvDialogNominal.setTextColor(0xFFC62828.toInt()) // expense_red
            }
        }

        // Tanggal
        binding.tvDialogTanggal.text = DateUtils.formatTanggalPendek(transaksi.tanggal)

        if (isTransfer) {
            // ── Mode Transfer ──────────────────────────────────────────────
            // Sembunyikan row dompet tunggal & catatan
            binding.rowDompet.visibility  = View.GONE
            binding.rowCatatan.visibility = View.GONE

            // Tampilkan row dari/ke dompet
            binding.rowDariDompet.visibility = View.VISIBLE
            binding.rowKeDompet.visibility   = View.VISIBLE

            // Catatan transfer dipakai untuk menyimpan "idDompetTujuan"
            // Format catatan transfer: "transfer_to:<dompetTujuanId>"
            // Ambil nama dompet asal dari dompetId
            val namaDompetAsal = daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"
            binding.tvDialogDariDompet.text = namaDompetAsal

            // Ambil nama dompet tujuan dari catatan
            val namaDompetTujuan = parseDompetTujuan(transaksi.catatan)
            binding.tvDialogKeDompet.text = namaDompetTujuan

        } else {
            // ── Mode Pemasukan / Pengeluaran ───────────────────────────────
            binding.rowDompet.visibility     = View.VISIBLE
            binding.rowCatatan.visibility    = View.VISIBLE
            binding.rowDariDompet.visibility = View.GONE
            binding.rowKeDompet.visibility   = View.GONE

            // Nama dompet
            val namaDompet = daftarDompet.find { it.id == transaksi.dompetId }?.nama ?: "-"
            binding.tvDialogDompet.text = namaDompet

            // Catatan
            binding.tvDialogCatatan.text = transaksi.catatan.ifEmpty { "-" }
        }
    }

    /**
     * Mapping icon berdasarkan jenis dompet — identik dengan
     * DompetAdapter.getIconRes(), dipakai khusus untuk transaksi PEMASUKAN
     * agar icon di popup detail konsisten dengan icon dompet yang tampil
     * di halaman Riwayat Transaksi maupun halaman Dompet.
     */
    private fun getIconDompet(jenis: String): Int = when (jenis) {
        "Rekening Bank"  -> R.drawable.ic_wallet_bank
        "Dompet Digital" -> R.drawable.ic_wallet_digital
        "Uang Tunai"     -> R.drawable.ic_wallet_cash
        "Investasi"      -> R.drawable.ic_wallet_investasi
        "Tabungan"       -> R.drawable.ic_wallet_tabungan
        else             -> R.drawable.ic_wallet_lainnya
    }

    /**
     * Parse nama dompet tujuan dari field catatan.
     *
     * Konvensi yang dipakai TambahTransaksiFragment saat simpan transfer:
     *   catatan = "transfer_to:<idDompetTujuan>"
     *
     * Jika format berbeda / tidak ditemukan, tampilkan catatan apa adanya
     * sehingga tetap ada informasi yang muncul.
     */
    private fun parseDompetTujuan(catatan: String): String {
        return if (catatan.startsWith("transfer_to:")) {
            val idTujuan = catatan.removePrefix("transfer_to:").trim().toIntOrNull()
            daftarDompet.find { it.id == idTujuan }?.nama ?: catatan
        } else {
            // Fallback: tampilkan isi catatan sebagai nama tujuan
            catatan.ifEmpty { "-" }
        }
    }

    // ── Setup tombol ────────────────────────────────────────────────────────

    private fun setupTombol() {
        // Tombol X: tutup dialog
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Tombol Edit
        binding.btnEdit.setOnClickListener {
            onEditClick(transaksi)
            dismiss()
        }

        // ── FIX BUG 2: Hapus tanpa dialog konfirmasi di sini ────────────────
        // Konfirmasi penghapusan sudah ditangani oleh RiwayatTransaksiFragment
        // di onDeleteClick. Kalau ditampilkan lagi di sini, user harus
        // konfirmasi dua kali. Jadi cukup teruskan event dan tutup dialog.
        binding.btnDelete.setOnClickListener {
            onDeleteClick(transaksi)
            dismiss()
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
package com.example.appmoneego.utils

import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.model.InsightItem

/**
 * InsightGenerator — seluruh logika perhitungan insight keuangan.
 *
 * Dipanggil dari DashboardViewModel dengan data yang sudah tersedia.
 * Tidak ada dependency ke Android context, murni logika Kotlin.
 */
object InsightGenerator {

    // ── 20 Tips MoneeGo ───────────────────────────────────────────────────────

    private val daftarTips = listOf(
        "Catat setiap pengeluaran kecil sekalipun. Pengeluaran kecil yang diabaikan bisa menumpuk jadi besar.",
        "Terapkan aturan 50/30/20: 50% kebutuhan, 30% keinginan, 20% tabungan.",
        "Buat anggaran bulanan sebelum bulan dimulai, bukan setelah uang habis.",
        "Hindari belanja impulsif. Tunggu 24 jam sebelum membeli barang yang tidak direncanakan.",
        "Bayar diri sendiri dulu — sisihkan tabungan di awal bulan, bukan dari sisa.",
        "Manfaatkan promo dan diskon dengan bijak, bukan sebagai alasan untuk belanja lebih banyak.",
        "Cek langganan digital yang jarang dipakai dan pertimbangkan untuk dibatalkan.",
        "Masak di rumah lebih sering. Biaya makan di luar bisa 3-5x lebih mahal.",
        "Bawa bekal ke tempat kerja atau kampus untuk menghemat pengeluaran harian.",
        "Gunakan transportasi umum jika memungkinkan untuk menghemat bensin dan parkir.",
        "Dana darurat idealnya setara 3-6 bulan pengeluaran bulanan.",
        "Investasi bukan hanya untuk orang kaya. Mulai dari nominal kecil yang konsisten.",
        "Bandingkan harga sebelum membeli. Selisih kecil di tiap transaksi bisa besar di akhir bulan.",
        "Lunasi hutang berbunga tinggi terlebih dahulu sebelum menabung di instrumen lain.",
        "Pisahkan rekening untuk kebutuhan sehari-hari, tabungan, dan dana darurat.",
        "Jangan meminjam uang untuk barang konsumtif yang nilainya langsung turun.",
        "Review kondisi keuangan Anda setiap minggu, bukan hanya saat uang menipis.",
        "Naikkan target tabungan setiap kali gaji naik, jangan hanya naikkan gaya hidup.",
        "Belanja kebutuhan pokok dalam jumlah besar bisa lebih hemat daripada beli eceran.",
        "Keuangan sehat bukan soal berapa banyak yang kamu hasilkan, tapi seberapa bijak kamu mengelolanya."
    )

    /**
     * Ambil satu tips secara random setiap dipanggil.
     */
    fun getTipsRandom(): InsightItem {
        val pesan = daftarTips.random()
        return InsightItem(pesan = pesan, tipe = "INFO")
    }

    // ── Logika Ringkasan (6 Prioritas) ───────────────────────────────────────

    /**
     * Hitung insight ringkasan berdasarkan data yang tersedia.
     *
     * @param totalJumlahTransaksi  jumlah seluruh transaksi di database
     * @param tanggalTerakhir       timestamp transaksi paling baru (null = belum ada)
     * @param pengeluaranBulanIni   list transaksi pengeluaran non-transfer bulan ini
     * @param pengeluaranBulanLalu  list transaksi pengeluaran non-transfer bulan lalu
     * @param jumlahTransaksiBulanIni jumlah transaksi bulan berjalan
     */
    fun hitungInsight(
        totalJumlahTransaksi: Int,
        tanggalTerakhir: Long?,
        pengeluaranBulanIni: List<Transaksi>,
        pengeluaranBulanLalu: List<Transaksi>,
        jumlahTransaksiBulanIni: Int
    ): InsightItem {

        // ── Prioritas 1: Belum ada transaksi sama sekali ──────────────────────
        if (totalJumlahTransaksi == 0 || tanggalTerakhir == null) {
            val pesanWelcome = listOf(
                "Selamat datang di MoneeGo! Mulailah dengan mencatat pengeluaran pertama Anda hari ini.",
                "Belum ada data keuangan. Yuk mulai catat pemasukan dan pengeluaran pertama!"
            )
            return InsightItem(pesan = pesanWelcome.random(), tipe = "INFO")
        }

        // ── Prioritas 2: Tidak mencatat lebih dari 7 hari ────────────────────
        val sekarang  = System.currentTimeMillis()
        val selisihMs = sekarang - tanggalTerakhir
        val selisihHari = selisihMs / (1000L * 60 * 60 * 24)

        if (selisihHari > 7) {
            val pesanTidakAktif = listOf(
                "Sudah lama belum mencatat transaksi 😢",
                "MoneeGo merindukan catatan keuanganmu ✨",
                "Yuk mulai lagi kebiasaan baikmu 📖",
                "Ayo cek kondisi keuanganmu hari ini 👀",
                "Catatan keuanganmu belum diperbarui 😭",
                "Sedikit catatan hari ini bisa sangat membantu 💰",
                "Jangan lupa pantau pengeluaran harianmu 👛",
                "Mulai lagi yuk, pelan-pelan aja 🌱"
            )
            return InsightItem(pesan = pesanTidakAktif.random(), tipe = "WARNING")
        }

        // ── Prioritas 3 & 4: Analisis kategori pengeluaran bulan ini ─────────
        if (pengeluaranBulanIni.isNotEmpty()) {
            val totalBulanIni = pengeluaranBulanIni.sumOf { it.nominal }

            // Grouping per kategori
            val ringkasanKategori = pengeluaranBulanIni
                .groupBy { it.kategori }
                .map { (nama, items) ->
                    val jumlah = items.sumOf { it.nominal }
                    Pair(nama, jumlah)
                }
                .sortedByDescending { it.second }

            val kategoriTerbesar = ringkasanKategori.firstOrNull()

            if (kategoriTerbesar != null) {
                val persentase = if (totalBulanIni > 0)
                    (kategoriTerbesar.second / totalBulanIni) * 100
                else 0.0

                // Prioritas 3: kategori terbesar > 50%
                if (persentase > 50.0) {
                    return InsightItem(
                        pesan = "Lebih dari setengah pengeluaran Anda digunakan untuk kategori ${kategoriTerbesar.first}.",
                        tipe  = "WARNING"
                    )
                }

                // Prioritas 4: ada kategori terbesar
                return InsightItem(
                    pesan = "Pengeluaran terbesar bulan ini berada pada kategori ${kategoriTerbesar.first}.",
                    tipe  = "INFO"
                )
            }
        }

        // ── Prioritas 5: Bandingkan dengan bulan lalu ─────────────────────────
        val totalBulanIni  = pengeluaranBulanIni.sumOf { it.nominal }
        val totalBulanLalu = pengeluaranBulanLalu.sumOf { it.nominal }

        if (totalBulanLalu > 0) {
            return if (totalBulanIni > totalBulanLalu) {
                InsightItem(
                    pesan = "Pengeluaran bulan ini meningkat dibanding bulan sebelumnya.",
                    tipe  = "WARNING"
                )
            } else {
                InsightItem(
                    pesan = "Pengeluaran bulan ini lebih rendah dibanding bulan sebelumnya.",
                    tipe  = "SUCCESS"
                )
            }
        }

        // ── Prioritas 6: Fallback — hitung transaksi bulan ini ────────────────
        return InsightItem(
            pesan = "Anda telah mencatat $jumlahTransaksiBulanIni transaksi bulan ini.",
            tipe  = "INFO"
        )
    }
}
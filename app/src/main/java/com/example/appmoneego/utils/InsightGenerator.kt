package com.example.appmoneego.utils

import android.content.Context
import com.example.appmoneego.R
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.model.InsightItem

object InsightGenerator {

    fun getTipsRandom(context: Context): InsightItem {
        val daftarTips = listOf(
            context.getString(R.string.tips_01),
            context.getString(R.string.tips_02),
            context.getString(R.string.tips_03),
            context.getString(R.string.tips_04),
            context.getString(R.string.tips_05),
            context.getString(R.string.tips_06),
            context.getString(R.string.tips_07),
            context.getString(R.string.tips_08),
            context.getString(R.string.tips_09),
            context.getString(R.string.tips_10)
        )
        return InsightItem(pesan = daftarTips.random(), tipe = "INFO")
    }

    fun hitungInsight(
        context: Context,
        totalJumlahTransaksi: Int,
        tanggalTerakhir: Long?,
        pengeluaranBulanIni: List<Transaksi>,
        pengeluaranBulanLalu: List<Transaksi>,
        jumlahTransaksiBulanIni: Int
    ): InsightItem {

        if (totalJumlahTransaksi == 0 || tanggalTerakhir == null) {
            val pesanWelcome = listOf(
                context.getString(R.string.insight_welcome_1),
                context.getString(R.string.insight_welcome_2)
            )
            return InsightItem(pesan = pesanWelcome.random(), tipe = "INFO")
        }

        val selisihHari = (System.currentTimeMillis() - tanggalTerakhir) / (1000L * 60 * 60 * 24)

        if (selisihHari > 7) {
            val pesanTidakAktif = listOf(
                context.getString(R.string.insight_tidak_aktif_1),
                context.getString(R.string.insight_tidak_aktif_2),
                context.getString(R.string.insight_tidak_aktif_3),
                context.getString(R.string.insight_tidak_aktif_4)
            )
            return InsightItem(pesan = pesanTidakAktif.random(), tipe = "WARNING")
        }

        if (pengeluaranBulanIni.isNotEmpty()) {
            val totalBulanIni = pengeluaranBulanIni.sumOf { it.nominal }
            val kategoriTerbesar = pengeluaranBulanIni
                .groupBy { it.kategori }
                .map { (nama, items) -> Pair(nama, items.sumOf { it.nominal }) }
                .sortedByDescending { it.second }
                .firstOrNull()

            if (kategoriTerbesar != null) {
                val persentase = if (totalBulanIni > 0)
                    (kategoriTerbesar.second / totalBulanIni) * 100 else 0.0

                if (persentase > 50.0) {
                    return InsightItem(
                        pesan = context.getString(R.string.insight_kategori_dominan, kategoriTerbesar.first),
                        tipe  = "WARNING"
                    )
                }
                return InsightItem(
                    pesan = context.getString(R.string.insight_kategori_terbesar, kategoriTerbesar.first),
                    tipe  = "INFO"
                )
            }
        }

        val totalBulanIni  = pengeluaranBulanIni.sumOf { it.nominal }
        val totalBulanLalu = pengeluaranBulanLalu.sumOf { it.nominal }

        if (totalBulanLalu > 0) {
            return if (totalBulanIni > totalBulanLalu) {
                InsightItem(pesan = context.getString(R.string.insight_naik), tipe = "WARNING")
            } else {
                InsightItem(pesan = context.getString(R.string.insight_turun), tipe = "SUCCESS")
            }
        }

        return InsightItem(
            pesan = context.getString(R.string.insight_jumlah_transaksi, jumlahTransaksiBulanIni),
            tipe  = "INFO"
        )
    }
}
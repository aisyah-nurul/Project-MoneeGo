package com.example.appmoneego.model

import java.util.Date

enum class JenisHutang(val label: String) {
    KARTU_KREDIT("Kartu Kredit"),
    PERSONAL("Personal"),
    CICILAN("Cicilan"),
    PINJAMAN_BANK("Pinjaman Bank"),
    PINJOL("Pinjaman Online"),
    PINJAM_KE_KERABAT("Pinjam ke Kerabat"),
    LAINNYA("Lainnya")
}

data class Hutang(
    val id: Long = System.currentTimeMillis(),
    val nama: String,
    val jenisHutang: JenisHutang,
    val jumlah: Long,
    val limitKredit: Long = 0L,
    val jatuhTempo: Date? = null,
    val tanggalDibuat: Date = Date(),
    val lunas: Boolean = false
) {
    val persentase: Int
        get() = if (limitKredit > 0) {
            ((jumlah.toDouble() / limitKredit) * 100).toInt().coerceAtMost(100)
        } else 0

    val isJatuhTempoBaru: Boolean
        get() {
            if (jatuhTempo == null) return false
            val diff = jatuhTempo.time - System.currentTimeMillis()
            val days = diff / (1000 * 60 * 60 * 24)
            return days in 0..7
        }
}
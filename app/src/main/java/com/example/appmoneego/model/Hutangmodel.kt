package com.example.appmoneego.hutang

import java.io.Serializable

data class HutangModel(
    val id: String = "",
    val nama: String = "",
    val totalHutang: Long = 0L,
    val sudahDibayar: Long = 0L,
    val tanggalJatuhTempo: String = "",
    val catatan: String = "",
    val dompetId: String = "",
    val riwayatCicilan: List<CicilanModel> = emptyList(),
    val selesai: Boolean = false
) : Serializable {
    val sisaHutang: Long get() = totalHutang - sudahDibayar
    val persenLunas: Int
        get() = if (totalHutang == 0L) 0
        else ((sudahDibayar.toDouble() / totalHutang) * 100).toInt()
    val hariSisaTempo: Int
        get() {
            return try {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val jt = sdf.parse(tanggalJatuhTempo) ?: return 0
                val diff = jt.time - System.currentTimeMillis()
                (diff / (1000 * 60 * 60 * 24)).toInt()
            } catch (e: Exception) { 0 }
        }
}

data class CicilanModel(
    val id: String = "",
    val hutangId: String = "",
    val nominal: Long = 0L,
    val tanggalBayar: String = "",
    val catatan: String = ""
) : Serializable
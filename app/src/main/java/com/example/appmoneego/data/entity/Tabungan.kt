package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabungan")
data class Tabungan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val targetNominal: Double,
    val terkumpul: Double = 0.0,
    val deadline: Long? = null,
    val isPriority: Boolean = false,

    // ── Field baru ────────────────────────────────────────────────────────────
    // Menandai bahwa user sudah menekan "Saya Sudah Membeli Impian Ini".
    // Efek:
    //   - Tabungan tetap tampil di tab SELESAI
    //   - Riwayat cicilan tetap ada
    //   - TIDAK dihitung ke "Total Terkumpul dari X Impian" di header TabunganFragment
    //   - TIDAK bisa menerima setoran baru (form input disembunyikan di detail)
    val sudahDigunakan: Boolean = false
)
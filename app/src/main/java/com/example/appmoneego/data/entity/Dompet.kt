package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dompet")
data class Dompet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val jenis: String = "Lainnya",      // "Rekening Bank", "Dompet Digital", "Uang Tunai", "Investasi", "Tabungan", "Lainnya"
    val saldo: Double = 0.0,
    val ikon: String = "ic_wallet",     // nama drawable, kamu isi sendiri per jenis
    val warna: String = "#37474F",      // hex warna aksen kartu (opsional override)
    val tanggalDibuat: Long = System.currentTimeMillis()
)

package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dompet")
data class Dompet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val jenis: String = "Lainnya",
    val saldo: Double = 0.0,
    val ikon: String = "ic_wallet",
    val warna: String = "#37474F",
    val tanggalDibuat: Long = 0L  // diisi dari DatePicker, bukan System.currentTimeMillis()
)
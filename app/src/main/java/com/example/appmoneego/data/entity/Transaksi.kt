package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaksi")
data class Transaksi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nominal: Double,
    val jenis: String,        // PEMASUKAN / PENGELUARAN
    val kategori: String,
    val catatan: String,
    val tanggal: Long,
    val dompetId: Int
)
package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaksi")
data class Transaksi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nominal: Double,
    val jenis: String,        // "PEMASUKAN" atau "PENGELUARAN"
    val kategori: String,
    val catatan: String,
    val tanggal: Long,        // simpan sebagai timestamp
    val dompetId: Int
)

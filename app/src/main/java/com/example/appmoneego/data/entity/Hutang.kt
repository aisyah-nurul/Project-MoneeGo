package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hutang")
data class Hutang(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,           // nama orang/pihak
    val nominal: Double,
    val catatan: String = "",
    val tanggal: Long = System.currentTimeMillis(),
    val jenis: String,          // "SAYA_BERHUTANG" atau "DIPINJAMI"
    val sudahLunas: Boolean = false
)

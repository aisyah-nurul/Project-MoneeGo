package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "hutang")
data class Hutang(
    @PrimaryKey
    val id: String,
    val nama: String = "",
    val totalHutang: Long = 0L,
    val sudahDibayar: Long = 0L,
    val tanggalJatuhTempo: String = "",
    val catatan: String = "",
    val selesai: Boolean = false
) : Serializable
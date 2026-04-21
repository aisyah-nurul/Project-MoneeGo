package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dompet")
data class Dompet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val saldo: Double = 0.0,
    val ikon: String = "ic_wallet"
)

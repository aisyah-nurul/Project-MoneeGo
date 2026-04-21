package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabungan")
data class Tabungan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val targetNominal: Double,
    val terkumpul: Double = 0.0,
    val deadline: Long? = null
)

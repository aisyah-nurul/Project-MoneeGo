package com.example.appmoneego.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cicilan")
data class CicilanEntity(
    @PrimaryKey
    val id: String,
    val hutangId: String = "",
    val nominal: Long = 0L,
    val tanggalBayar: String = "",
    val catatan: String = ""
)
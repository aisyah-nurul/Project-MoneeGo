package com.example.appmoneego.repository

import com.example.appmoneego.data.dao.TabunganDao
import com.example.appmoneego.data.entity.Tabungan

class TabunganRepository(private val dao: TabunganDao) {
    val allTabungan = dao.getAllTabungan()

    suspend fun insert(tabungan: Tabungan) = dao.insert(tabungan)
    suspend fun update(tabungan: Tabungan) = dao.update(tabungan)
    suspend fun delete(tabungan: Tabungan) = dao.delete(tabungan)
    suspend fun getById(id: Int) = dao.getById(id)
    fun tambahTerkumpul(id: Int, jumlah: Double) {}
}
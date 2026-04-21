package com.example.appmoneego.repository

import com.example.appmoneego.data.dao.TransaksiDao
import com.example.appmoneego.data.entity.Transaksi

class TransaksiRepository (private val dao: TransaksiDao) {
    val allTransaksi = dao.getAllTransaksi()
    val totalPemasukan = dao.getTotalPemasukan()
    val totalPengeluaran = dao.getTotalPengeluaran()

    suspend fun insert(transaksi: Transaksi) = dao.insert(transaksi)
    suspend fun update(transaksi: Transaksi) = dao.update(transaksi)
    suspend fun delete(transaksi: Transaksi) = dao.delete(transaksi)
    fun getByDateRange(start: Long, end: Long) = dao.getByDateRange(start, end)
    fun getByKategori(kategori: String) = dao.getByKategori(kategori)
}
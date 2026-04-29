package com.example.appmoneego.repository

import androidx.lifecycle.LiveData
import com.example.appmoneego.data.dao.TransaksiDao
import com.example.appmoneego.data.entity.Transaksi

class TransaksiRepository(private val dao: TransaksiDao) {

    val allTransaksi: LiveData<List<Transaksi>> = dao.getAllTransaksi()
    val totalPemasukan: LiveData<Double?> = dao.getTotalPemasukan()
    val totalPengeluaran: LiveData<Double?> = dao.getTotalPengeluaran()

    suspend fun insert(transaksi: Transaksi) = dao.insert(transaksi)
    suspend fun update(transaksi: Transaksi) = dao.update(transaksi)
    suspend fun delete(transaksi: Transaksi) = dao.delete(transaksi)

    // Hapus semua transaksi milik dompet tertentu
    suspend fun deleteByDompetId(dompetId: Int) = dao.deleteByDompetId(dompetId)

    fun getByDateRange(start: Long, end: Long) = dao.getByDateRange(start, end)
    fun getByKategori(kategori: String) = dao.getByKategori(kategori)

    fun getPemasukanBulanIni(start: Long, end: Long): LiveData<Double?> =
        dao.getPemasukanBulanIni(start, end)

    fun getPengeluaranBulanIni(start: Long, end: Long): LiveData<Double?> =
        dao.getPengeluaranBulanIni(start, end)
}
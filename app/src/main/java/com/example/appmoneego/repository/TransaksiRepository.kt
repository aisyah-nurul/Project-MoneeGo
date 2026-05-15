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
    suspend fun deleteByDompetId(dompetId: Int) = dao.deleteByDompetId(dompetId)

    fun getByDateRange(start: Long, end: Long) = dao.getByDateRange(start, end)
    fun getByKategori(kategori: String) = dao.getByKategori(kategori)

    fun getPemasukanBulanIni(start: Long, end: Long): LiveData<Double?> =
        dao.getPemasukanBulanIni(start, end)

    fun getPengeluaranBulanIni(start: Long, end: Long): LiveData<Double?> =
        dao.getPengeluaranBulanIni(start, end)

    fun getByJenisAndBulan(jenis: String, start: Long, end: Long): LiveData<List<Transaksi>> =
        dao.getByJenisAndBulan(jenis, start, end)

    fun getRecentByBulan(start: Long, end: Long, limit: Int = 5): LiveData<List<Transaksi>> =
        dao.getRecentByBulan(start, end, limit)

    fun getByKategoriAndBulan(kategori: String, start: Long, end: Long): LiveData<List<Transaksi>> =
        dao.getByKategoriAndBulan(kategori, start, end)

    fun getByDompetAndBulan(dompetId: Int, start: Long, end: Long): LiveData<List<Transaksi>> =
        dao.getByDompetAndBulan(dompetId, start, end)
}
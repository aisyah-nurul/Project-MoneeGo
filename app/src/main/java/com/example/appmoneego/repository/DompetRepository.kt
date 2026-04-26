package com.example.appmoneego.repository

import androidx.lifecycle.LiveData
import com.example.appmoneego.data.dao.DompetDao
import com.example.appmoneego.data.entity.Dompet

class DompetRepository(private val dompetDao: DompetDao) {

    val allDompet: LiveData<List<Dompet>>   = dompetDao.getAllDompet()
    val totalSaldo: LiveData<Double?>        = dompetDao.getTotalSaldo()
    val jumlahDompet: LiveData<Int>          = dompetDao.getJumlahDompet()
    val dompetTerbesar: LiveData<Dompet?>    = dompetDao.getDompetTerbesar()

    suspend fun insert(dompet: Dompet)  = dompetDao.insert(dompet)
    suspend fun update(dompet: Dompet)  = dompetDao.update(dompet)
    suspend fun delete(dompet: Dompet)  = dompetDao.delete(dompet)

    // Dipakai oleh TransaksiViewModel untuk ambil dompet sebelum update saldo
    suspend fun getById(id: Int): Dompet? = dompetDao.getDompetById(id)

    suspend fun tambahSaldo(id: Int, jumlah: Double)   = dompetDao.tambahSaldo(id, jumlah)
    suspend fun kurangiSaldo(id: Int, jumlah: Double)  = dompetDao.kurangiSaldo(id, jumlah)
}
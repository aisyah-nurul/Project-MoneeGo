package com.example.appmoneego.repository

import com.example.appmoneego.data.dao.DompetDao
import com.example.appmoneego.data.entity.Dompet

class DompetRepository(private val dao: DompetDao) {
    val allDompet = dao.getAllDompet()
    val totalSaldo = dao.getTotalSaldo()

    suspend fun insert(dompet: Dompet) = dao.insert(dompet)
    suspend fun update(dompet: Dompet) = dao.update(dompet)
    suspend fun delete(dompet: Dompet) = dao.delete(dompet)
    suspend fun getById(id: Int) = dao.getById(id)
}
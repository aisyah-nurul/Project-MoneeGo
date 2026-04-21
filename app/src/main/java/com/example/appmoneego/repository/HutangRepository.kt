package com.example.appmoneego.repository

import com.example.appmoneego.data.dao.HutangDao
import com.example.appmoneego.data.entity.Hutang

class HutangRepository(private val dao: HutangDao) {
    val allHutang = dao.getAllHutang()
    val hutangAktif = dao.getHutangAktif()

    suspend fun insert(hutang: Hutang) = dao.insert(hutang)
    suspend fun update(hutang: Hutang) = dao.update(hutang)
    suspend fun delete(hutang: Hutang) = dao.delete(hutang)
    suspend fun lunaskan(id: Int) = dao.lunaskan(id)
    fun updateStatus(id: Int, status: Boolean) {}
}
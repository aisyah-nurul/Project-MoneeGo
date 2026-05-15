package com.example.appmoneego.repository

import androidx.lifecycle.LiveData
import com.example.appmoneego.data.dao.HutangDao
import com.example.appmoneego.data.entity.CicilanEntity
import com.example.appmoneego.data.entity.Hutang

class HutangRepository(private val dao: HutangDao) {

    val allHutang: LiveData<List<Hutang>> = dao.getAllHutang()

    suspend fun insert(hutang: Hutang) = dao.insertHutang(hutang)
    suspend fun update(hutang: Hutang) = dao.updateHutang(hutang)
    suspend fun delete(hutang: Hutang) = dao.deleteHutang(hutang)

    suspend fun getCicilan(hutangId: String): List<CicilanEntity> =
        dao.getCicilanByHutang(hutangId)

    suspend fun insertCicilan(cicilan: CicilanEntity) = dao.insertCicilan(cicilan)
}
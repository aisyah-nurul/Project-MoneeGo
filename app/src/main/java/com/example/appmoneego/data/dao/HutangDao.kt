package com.example.appmoneego.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmoneego.data.entity.Hutang
import com.example.appmoneego.data.entity.CicilanEntity

@Dao
interface HutangDao {

    @Query("SELECT * FROM hutang ORDER BY rowid DESC")
    fun getAllHutang(): LiveData<List<Hutang>>

    @Query("SELECT * FROM hutang WHERE id = :hutangId LIMIT 1")
    suspend fun getHutangById(hutangId: String): Hutang?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHutang(hutang: Hutang)

    @Update
    suspend fun updateHutang(hutang: Hutang)

    @Delete
    suspend fun deleteHutang(hutang: Hutang)

    @Query("SELECT * FROM cicilan WHERE hutangId = :hutangId ORDER BY rowid ASC")
    suspend fun getCicilanByHutang(hutangId: String): List<CicilanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCicilan(cicilan: CicilanEntity)
}
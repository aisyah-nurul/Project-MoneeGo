package com.example.appmoneego.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appmoneego.data.entity.CicilanEntity

@Dao
interface CicilanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCicilan(cicilan: CicilanEntity)

    @Query("SELECT * FROM cicilan WHERE hutangId = :hutangId ORDER BY tanggalBayar ASC")
    suspend fun getCicilanByHutangId(hutangId: String): List<CicilanEntity>

    @Query("SELECT * FROM cicilan WHERE hutangId = :hutangId ORDER BY tanggalBayar DESC LIMIT 1")
    suspend fun getCicilanTerakhir(hutangId: String): CicilanEntity?

    @Query("DELETE FROM cicilan WHERE hutangId = :hutangId")
    suspend fun deleteCicilanByHutangId(hutangId: String)

    @Query("DELETE FROM cicilan WHERE id = :cicilanId")
    suspend fun deleteCicilanById(cicilanId: String)
}
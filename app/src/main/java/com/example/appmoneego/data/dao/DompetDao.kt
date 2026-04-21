package com.example.appmoneego.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmoneego.data.entity.Dompet

@Dao
interface DompetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dompet: Dompet)

    @Update
    suspend fun update(dompet: Dompet)

    @Delete
    suspend fun delete(dompet: Dompet)

    @Query("SELECT * FROM dompet")
    fun getAllDompet(): LiveData<List<Dompet>>

    @Query("SELECT * FROM dompet WHERE id = :id")
    suspend fun getById(id: Int): Dompet?

    @Query("SELECT SUM(saldo) FROM dompet")
    fun getTotalSaldo(): LiveData<Double?>
}
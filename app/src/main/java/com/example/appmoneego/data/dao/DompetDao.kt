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

    @Query("SELECT * FROM dompet ORDER BY tanggalDibuat DESC")
    fun getAllDompet(): LiveData<List<Dompet>>

    @Query("SELECT * FROM dompet WHERE id = :id")
    suspend fun getDompetById(id: Int): Dompet?

    @Query("SELECT SUM(saldo) FROM dompet")
    fun getTotalSaldo(): LiveData<Double?>

    @Query("SELECT COUNT(*) FROM dompet")
    fun getJumlahDompet(): LiveData<Int>

    @Query("SELECT * FROM dompet ORDER BY saldo DESC LIMIT 1")
    fun getDompetTerbesar(): LiveData<Dompet?>

    @Query("UPDATE dompet SET saldo = saldo + :jumlah WHERE id = :id")
    suspend fun tambahSaldo(id: Int, jumlah: Double)

    @Query("UPDATE dompet SET saldo = saldo - :jumlah WHERE id = :id")
    suspend fun kurangiSaldo(id: Int, jumlah: Double)
}
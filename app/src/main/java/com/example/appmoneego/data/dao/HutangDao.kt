package com.example.appmoneego.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmoneego.data.entity.Hutang

@Dao
interface HutangDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hutang: Hutang)

    @Update
    suspend fun update(hutang: Hutang)

    @Delete
    suspend fun delete(hutang: Hutang)

    @Query("SELECT * FROM hutang ORDER BY tanggal DESC")
    fun getAllHutang(): LiveData<List<Hutang>>

    @Query("SELECT * FROM hutang WHERE sudahLunas = 0 ORDER BY tanggal DESC")
    fun getHutangAktif(): LiveData<List<Hutang>>

    @Query("UPDATE hutang SET sudahLunas = 1 WHERE id = :id")
    suspend fun lunaskan(id: Int)
}
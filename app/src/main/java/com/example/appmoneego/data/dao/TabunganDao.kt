package com.example.appmoneego.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmoneego.data.entity.Tabungan

@Dao
interface TabunganDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tabungan: Tabungan)

    @Update
    suspend fun update(tabungan: Tabungan)

    @Delete
    suspend fun delete(tabungan: Tabungan)

    @Query("SELECT * FROM tabungan")
    fun getAllTabungan(): LiveData<List<Tabungan>>

    @Query("SELECT * FROM tabungan WHERE id = :id")
    suspend fun getById(id: Int): Tabungan?
}
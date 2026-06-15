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

    // ── Target Tabungan Prioritas ─────────────────────────────────────────────

    @Query("SELECT * FROM tabungan WHERE isPriority = 1 LIMIT 1")
    fun getTabunganPrioritas(): LiveData<Tabungan?>

    @Query("UPDATE tabungan SET isPriority = 0")
    suspend fun clearAllPriority()

    @Query("UPDATE tabungan SET isPriority = :isPriority WHERE id = :id")
    suspend fun setPriority(id: Int, isPriority: Boolean)

    // ── Tandai sudah digunakan ────────────────────────────────────────────────
    // Dipanggil saat user menekan "Saya Sudah Membeli Impian Ini" dan memilih "Sudah".
    // Tabungan tetap di tab SELESAI, tapi tidak lagi dihitung ke Total Terkumpul.
    @Query("UPDATE tabungan SET sudahDigunakan = 1 WHERE id = :id")
    suspend fun tandaiSudahDigunakan(id: Int)
}
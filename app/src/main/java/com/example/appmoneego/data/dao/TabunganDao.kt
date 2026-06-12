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

    // ── Untuk Target Tabungan Prioritas di Dashboard ──────────────────────────

    /**
     * Mengambil satu-satunya tabungan yang sedang dijadikan prioritas.
     * Mengembalikan null jika belum ada tabungan yang isPriority = true.
     * Digunakan oleh Dashboard — tidak ada fallback otomatis.
     */
    @Query("SELECT * FROM tabungan WHERE isPriority = 1 LIMIT 1")
    fun getTabunganPrioritas(): LiveData<Tabungan?>

    /**
     * Mematikan status prioritas pada SEMUA tabungan.
     * Dipanggil sebelum mengaktifkan prioritas baru, agar hanya
     * satu tabungan yang isPriority = true pada satu waktu.
     */
    @Query("UPDATE tabungan SET isPriority = 0")
    suspend fun clearAllPriority()

    /**
     * Set status prioritas untuk satu tabungan tertentu berdasarkan id.
     */
    @Query("UPDATE tabungan SET isPriority = :isPriority WHERE id = :id")
    suspend fun setPriority(id: Int, isPriority: Boolean)
}
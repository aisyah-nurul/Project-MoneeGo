package com.example.appmoneego.repository

import androidx.lifecycle.LiveData
import com.example.appmoneego.data.dao.TabunganDao
import com.example.appmoneego.data.entity.Tabungan

class TabunganRepository(private val dao: TabunganDao) {
    val allTabungan = dao.getAllTabungan()

    suspend fun insert(tabungan: Tabungan) = dao.insert(tabungan)
    suspend fun update(tabungan: Tabungan) = dao.update(tabungan)
    suspend fun delete(tabungan: Tabungan) = dao.delete(tabungan)
    suspend fun getById(id: Int) = dao.getById(id)
    fun tambahTerkumpul(id: Int, jumlah: Double) {}

    // ── Target Tabungan Prioritas ─────────────────────────────────────────────

    /**
     * LiveData tabungan prioritas — null jika belum ada yang diaktifkan user.
     * Dashboard mengamati ini secara langsung, tanpa fallback otomatis.
     */
    val tabunganPrioritas: LiveData<Tabungan?> = dao.getTabunganPrioritas()

    /**
     * Mengaktifkan atau mematikan status prioritas pada satu tabungan.
     *
     * - Jika isPriority = true  → matikan dulu prioritas pada SEMUA tabungan
     *                              (memastikan hanya 1 yang aktif), lalu
     *                              aktifkan untuk tabungan ini.
     * - Jika isPriority = false → cukup matikan untuk tabungan ini saja.
     */
    suspend fun setPrioritas(id: Int, isPriority: Boolean) {
        if (isPriority) {
            dao.clearAllPriority()
        }
        dao.setPriority(id, isPriority)
    }
}
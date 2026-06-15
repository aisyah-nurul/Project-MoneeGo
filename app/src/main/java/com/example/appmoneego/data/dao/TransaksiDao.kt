package com.example.appmoneego.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmoneego.data.entity.Transaksi

@Dao
interface TransaksiDao {

    // FIX BUG 2: insert sekarang mengembalikan row id (Long) hasil insert —
    // dibutuhkan agar CicilanEntity bisa menyimpan transaksiId yang terkait.
    // Perubahan ini aman untuk pemanggil lama yang tidak memakai return value.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaksi: Transaksi): Long

    @Update
    suspend fun update(transaksi: Transaksi)

    @Delete
    suspend fun delete(transaksi: Transaksi)

    @Query("DELETE FROM transaksi WHERE dompetId = :dompetId")
    suspend fun deleteByDompetId(dompetId: Int)

    // FIX BUG 2: ambil transaksi berdasarkan id — dipakai saat cicilan dihapus
    // dari Detail Hutang, untuk menghapus transaksi terkait di Riwayat Transaksi.
    @Query("SELECT * FROM transaksi WHERE id = :id LIMIT 1")
    suspend fun getTransaksiById(id: Int): Transaksi?

    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    fun getAllTransaksi(): LiveData<List<Transaksi>>

    @Query("SELECT * FROM transaksi WHERE dompetId = :dompetId ORDER BY tanggal DESC")
    fun getByDompet(dompetId: Int): LiveData<List<Transaksi>>

    @Query("SELECT * FROM transaksi WHERE tanggal BETWEEN :start AND :end ORDER BY tanggal DESC")
    fun getByDateRange(start: Long, end: Long): LiveData<List<Transaksi>>

    @Query("SELECT SUM(nominal) FROM transaksi WHERE jenis = 'PEMASUKAN'")
    fun getTotalPemasukan(): LiveData<Double?>

    @Query("SELECT SUM(nominal) FROM transaksi WHERE jenis = 'PENGELUARAN'")
    fun getTotalPengeluaran(): LiveData<Double?>

    @Query("SELECT * FROM transaksi WHERE kategori = :kategori ORDER BY tanggal DESC")
    fun getByKategori(kategori: String): LiveData<List<Transaksi>>

    @Query("SELECT SUM(nominal) FROM transaksi WHERE jenis = 'PEMASUKAN' AND tanggal BETWEEN :start AND :end")
    fun getPemasukanBulanIni(start: Long, end: Long): LiveData<Double?>

    @Query("SELECT SUM(nominal) FROM transaksi WHERE jenis = 'PENGELUARAN' AND tanggal BETWEEN :start AND :end")
    fun getPengeluaranBulanIni(start: Long, end: Long): LiveData<Double?>

    // ── Analisis ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM transaksi WHERE jenis = :jenis AND tanggal BETWEEN :start AND :end ORDER BY tanggal DESC")
    fun getByJenisAndBulan(jenis: String, start: Long, end: Long): LiveData<List<Transaksi>>

    @Query("SELECT * FROM transaksi WHERE tanggal BETWEEN :start AND :end ORDER BY tanggal DESC LIMIT :limit")
    fun getRecentByBulan(start: Long, end: Long, limit: Int = 5): LiveData<List<Transaksi>>

    @Query("SELECT * FROM transaksi WHERE kategori = :kategori AND tanggal BETWEEN :start AND :end ORDER BY tanggal DESC")
    fun getByKategoriAndBulan(kategori: String, start: Long, end: Long): LiveData<List<Transaksi>>

    @Query("SELECT * FROM transaksi WHERE dompetId = :dompetId AND tanggal BETWEEN :start AND :end ORDER BY tanggal DESC")
    fun getByDompetAndBulan(dompetId: Int, start: Long, end: Long): LiveData<List<Transaksi>>

    // ── Insight — query tambahan ───────────────────────────────────────────────

    @Query("""
        SELECT * FROM transaksi
        WHERE jenis = 'PENGELUARAN'
          AND kategori != 'Transfer'
          AND tanggal BETWEEN :start AND :end
        ORDER BY tanggal DESC
    """)
    fun getPengeluaranNonTransferBulanIni(
        start: Long,
        end: Long
    ): LiveData<List<Transaksi>>

    @Query("""
        SELECT * FROM transaksi
        WHERE jenis = 'PENGELUARAN'
          AND kategori != 'Transfer'
          AND tanggal BETWEEN :start AND :end
        ORDER BY tanggal DESC
    """)
    fun getPengeluaranNonTransferBulanLalu(
        start: Long,
        end: Long
    ): LiveData<List<Transaksi>>

    @Query("SELECT COUNT(*) FROM transaksi")
    fun getTotalJumlahTransaksi(): LiveData<Int>

    @Query("SELECT MAX(tanggal) FROM transaksi")
    fun getTanggalTransaksiTerakhir(): LiveData<Long?>

    @Query("""
        SELECT COUNT(*) FROM transaksi
        WHERE tanggal BETWEEN :start AND :end
    """)
    fun getJumlahTransaksiBulanIni(start: Long, end: Long): LiveData<Int>

    // ── Ekspor CSV ────────────────────────────────────────────────────────────
    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    suspend fun getAllTransaksiOnce(): List<Transaksi>
}
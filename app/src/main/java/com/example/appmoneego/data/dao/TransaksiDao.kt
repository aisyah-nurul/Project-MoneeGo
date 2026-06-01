package com.example.appmoneego.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmoneego.data.entity.Transaksi

@Dao
interface TransaksiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaksi: Transaksi)

    @Update
    suspend fun update(transaksi: Transaksi)

    @Delete
    suspend fun delete(transaksi: Transaksi)

    @Query("DELETE FROM transaksi WHERE dompetId = :dompetId")
    suspend fun deleteByDompetId(dompetId: Int)

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

    /**
     * Ambil semua transaksi pengeluaran bulan ini (non-transfer) untuk
     * keperluan hitung kategori terbesar di insight.
     */
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

    /**
     * Ambil semua transaksi pengeluaran bulan lalu (non-transfer) untuk
     * perbandingan bulan ke bulan di insight prioritas 5.
     */
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

    /**
     * Hitung jumlah seluruh transaksi — dipakai untuk cek apakah belum ada
     * transaksi sama sekali (prioritas 1).
     */
    @Query("SELECT COUNT(*) FROM transaksi")
    fun getTotalJumlahTransaksi(): LiveData<Int>

    /**
     * Ambil tanggal transaksi terakhir — dipakai untuk cek apakah sudah
     * lebih dari 7 hari tidak mencatat (prioritas 2).
     */
    @Query("SELECT MAX(tanggal) FROM transaksi")
    fun getTanggalTransaksiTerakhir(): LiveData<Long?>

    /**
     * Hitung jumlah transaksi bulan ini — dipakai untuk insight prioritas 6.
     */
    @Query("""
        SELECT COUNT(*) FROM transaksi
        WHERE tanggal BETWEEN :start AND :end
    """)
    fun getJumlahTransaksiBulanIni(start: Long, end: Long): LiveData<Int>
}
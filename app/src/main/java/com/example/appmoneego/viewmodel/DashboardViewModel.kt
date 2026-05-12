package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.repository.DompetRepository
import com.example.appmoneego.repository.TabunganRepository
import com.example.appmoneego.repository.TransaksiRepository
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transaksiRepo: TransaksiRepository
    private val dompetRepo: DompetRepository
    private val tabunganRepo: TabunganRepository

    // ── Saldo total semua dompet (tidak berubah saat ganti bulan) ─────────────
    val totalSaldo: LiveData<Double?>
    val allTransaksi: LiveData<List<Transaksi>>
    val saldoBersih = MediatorLiveData<Double>()

    // ── Pemasukan & Pengeluaran — berubah saat ganti bulan ────────────────────
    private val _totalPemasukan = MediatorLiveData<Double?>()
    val totalPemasukan: LiveData<Double?> get() = _totalPemasukan

    private val _totalPengeluaran = MediatorLiveData<Double?>()
    val totalPengeluaran: LiveData<Double?> get() = _totalPengeluaran

    // Sumber LiveData aktif saat ini (disimpan agar bisa di-remove sebelum ganti)
    private var sourcePemasukan:   LiveData<Double?>? = null
    private var sourcePengeluaran: LiveData<Double?>? = null

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        transaksiRepo = TransaksiRepository(db.transaksiDao())
        dompetRepo    = DompetRepository(db.dompetDao())
        tabunganRepo  = TabunganRepository(db.tabunganDao())

        totalSaldo   = dompetRepo.totalSaldo
        allTransaksi = transaksiRepo.allTransaksi

        saldoBersih.addSource(totalSaldo) { nilai ->
            saldoBersih.value = nilai ?: 0.0
        }

        // Load bulan saat ini saat pertama kali dibuat
        loadBulan(Calendar.getInstance())
    }

    /**
     * Dipanggil dari Fragment setiap kali user menekan tombol ◀ atau ▶.
     * Mengganti sumber LiveData pemasukan & pengeluaran ke range bulan baru.
     */
    fun loadBulan(cal: Calendar) {
        val (start, end) = getRangeFromCalendar(cal)

        // Hapus sumber lama sebelum menambahkan yang baru
        // agar tidak ada observer ganda
        sourcePemasukan?.let   { _totalPemasukan.removeSource(it) }
        sourcePengeluaran?.let { _totalPengeluaran.removeSource(it) }

        val newPemasukan   = transaksiRepo.getPemasukanBulanIni(start, end)
        val newPengeluaran = transaksiRepo.getPengeluaranBulanIni(start, end)

        sourcePemasukan   = newPemasukan
        sourcePengeluaran = newPengeluaran

        _totalPemasukan.addSource(newPemasukan)   { v -> _totalPemasukan.value   = v }
        _totalPengeluaran.addSource(newPengeluaran) { v -> _totalPengeluaran.value = v }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun getRangeFromCalendar(cal: Calendar): Pair<Long, Long> {
        val c = cal.clone() as Calendar

        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis

        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        val end = c.timeInMillis

        return Pair(start, end)
    }
}
package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.repository.TransaksiRepository
import com.example.appmoneego.repository.DompetRepository
import com.example.appmoneego.repository.TabunganRepository
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transaksiRepo: TransaksiRepository
    private val dompetRepo: DompetRepository
    private val tabunganRepo: TabunganRepository

    val totalSaldo: LiveData<Double?>
    val totalPemasukan: LiveData<Double?>
    val totalPengeluaran: LiveData<Double?>
    val allTransaksi: LiveData<List<Transaksi>>
    val saldoBersih = MediatorLiveData<Double>()

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        transaksiRepo = TransaksiRepository(db.transaksiDao())
        dompetRepo    = DompetRepository(db.dompetDao())
        tabunganRepo  = TabunganRepository(db.tabunganDao())

        totalSaldo   = dompetRepo.totalSaldo
        allTransaksi = transaksiRepo.allTransaksi

        val (startOfMonth, endOfMonth) = getBulanIniRange()

        totalPemasukan   = transaksiRepo.getPemasukanBulanIni(startOfMonth, endOfMonth)
        totalPengeluaran = transaksiRepo.getPengeluaranBulanIni(startOfMonth, endOfMonth)

        saldoBersih.addSource(totalSaldo) { nilai ->
            saldoBersih.value = nilai ?: 0.0
        }
    }

    private fun getBulanIniRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()

        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}
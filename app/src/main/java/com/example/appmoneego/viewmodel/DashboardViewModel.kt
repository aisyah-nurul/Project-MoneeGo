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

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transaksiRepo: TransaksiRepository
    private val dompetRepo: DompetRepository
    private val tabunganRepo: TabunganRepository

    val totalSaldo: LiveData<Double?>
    val totalPemasukan: LiveData<Double?>
    val totalPengeluaran: LiveData<Double?>
    val allTransaksi: LiveData<List<Transaksi>>

    // saldo bersih = totalSaldo semua dompet
    val saldoBersih = MediatorLiveData<Double>()

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        transaksiRepo = TransaksiRepository(db.transaksiDao())
        dompetRepo = DompetRepository(db.dompetDao())
        tabunganRepo = TabunganRepository(db.tabunganDao())

        totalSaldo = dompetRepo.totalSaldo
        totalPemasukan = transaksiRepo.totalPemasukan
        totalPengeluaran = transaksiRepo.totalPengeluaran
        allTransaksi = transaksiRepo.allTransaksi

        saldoBersih.addSource(totalSaldo) { nilai ->
            saldoBersih.value = nilai ?: 0.0
        }
    }
}
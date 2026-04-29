package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.repository.DompetRepository
import com.example.appmoneego.repository.TransaksiRepository
import kotlinx.coroutines.launch

class DompetViewModel(application: Application) : AndroidViewModel(application) {

    private val dompetRepo: DompetRepository
    private val transaksiRepo: TransaksiRepository

    val allDompet: LiveData<List<Dompet>>
    val totalSaldo: LiveData<Double?>
    val jumlahDompet: LiveData<Int>
    val dompetTerbesar: LiveData<Dompet?>

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        dompetRepo    = DompetRepository(db.dompetDao())
        transaksiRepo = TransaksiRepository(db.transaksiDao())

        allDompet     = dompetRepo.allDompet
        totalSaldo    = dompetRepo.totalSaldo
        jumlahDompet  = dompetRepo.jumlahDompet
        dompetTerbesar = dompetRepo.dompetTerbesar
    }

    fun insert(dompet: Dompet) = viewModelScope.launch {
        dompetRepo.insert(dompet)
    }

    fun update(dompet: Dompet) = viewModelScope.launch {
        dompetRepo.update(dompet)
    }

    // Hapus dompet + semua transaksi yang terkait dompet ini
    fun delete(dompet: Dompet) = viewModelScope.launch {
        transaksiRepo.deleteByDompetId(dompet.id)  // hapus transaksi dulu
        dompetRepo.delete(dompet)                   // baru hapus dompet
    }
}
package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.repository.TransaksiRepository
import com.example.appmoneego.repository.DompetRepository
import kotlinx.coroutines.launch

class TransaksiViewModel(application: Application) : AndroidViewModel(application) {

    private val transaksiRepo: TransaksiRepository
    private val dompetRepo: DompetRepository

    val allTransaksi: LiveData<List<Transaksi>>

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        transaksiRepo = TransaksiRepository(db.transaksiDao())
        dompetRepo    = DompetRepository(db.dompetDao())
        allTransaksi  = transaksiRepo.allTransaksi
    }

    // Insert transaksi biasa — otomatis update saldo dompet
    fun insert(transaksi: Transaksi) = viewModelScope.launch {
        transaksiRepo.insert(transaksi)
        val dompet = dompetRepo.getById(transaksi.dompetId)
        dompet?.let {
            val saldoBaru = if (transaksi.jenis == "PEMASUKAN") {
                it.saldo + transaksi.nominal
            } else {
                it.saldo - transaksi.nominal
            }
            dompetRepo.update(it.copy(saldo = saldoBaru))
        }
    }

    // Insert transaksi TANPA update saldo dompet
    // Dipakai saat tambah dompet baru — saldo sudah di-set langsung di entity Dompet
    fun insertTanpaUpdateSaldo(transaksi: Transaksi) = viewModelScope.launch {
        transaksiRepo.insert(transaksi)
    }

    fun update(transaksi: Transaksi, nominalLama: Double, jenisLama: String) =
        viewModelScope.launch {
            val dompet = dompetRepo.getById(transaksi.dompetId)
            dompet?.let {
                val saldoKembalikan = if (jenisLama == "PEMASUKAN") {
                    it.saldo - nominalLama
                } else {
                    it.saldo + nominalLama
                }
                val saldoBaru = if (transaksi.jenis == "PEMASUKAN") {
                    saldoKembalikan + transaksi.nominal
                } else {
                    saldoKembalikan - transaksi.nominal
                }
                dompetRepo.update(it.copy(saldo = saldoBaru))
            }
            transaksiRepo.update(transaksi)
        }

    fun delete(transaksi: Transaksi) = viewModelScope.launch {
        val dompet = dompetRepo.getById(transaksi.dompetId)
        dompet?.let {
            val saldoBaru = if (transaksi.jenis == "PEMASUKAN") {
                it.saldo - transaksi.nominal
            } else {
                it.saldo + transaksi.nominal
            }
            dompetRepo.update(it.copy(saldo = saldoBaru))
        }
        transaksiRepo.delete(transaksi)
    }

    fun getByDateRange(start: Long, end: Long) = transaksiRepo.getByDateRange(start, end)
    fun getByKategori(kategori: String)         = transaksiRepo.getByKategori(kategori)
}
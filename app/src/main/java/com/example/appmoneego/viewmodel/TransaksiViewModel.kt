package com.example.appmoneego.viewmodel

import android.app.Application
import android.util.Log
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

    fun insertTanpaUpdateSaldo(transaksi: Transaksi) = viewModelScope.launch {
        transaksiRepo.insert(transaksi)
    }

    fun update(
        transaksi: Transaksi,
        nominalLama: Double,
        jenisLama: String,
        dompetIdLama: Int = transaksi.dompetId
    ) = viewModelScope.launch {

        Log.d("EditTransaksi", "ViewModel.update() — id=${transaksi.id} nominal=${transaksi.nominal} dompetId=${transaksi.dompetId} dompetIdLama=$dompetIdLama")

        try {
            if (dompetIdLama != transaksi.dompetId) {
                // ── Dompet BERUBAH ──────────────────────────────────────────
                Log.d("EditTransaksi", "Dompet berubah: $dompetIdLama → ${transaksi.dompetId}")

                val dompetLama = dompetRepo.getById(dompetIdLama)
                Log.d("EditTransaksi", "dompetLama=$dompetLama")
                dompetLama?.let {
                    val saldoKembalikan = if (jenisLama == "PEMASUKAN")
                        it.saldo - nominalLama
                    else
                        it.saldo + nominalLama
                    dompetRepo.update(it.copy(saldo = saldoKembalikan))
                    Log.d("EditTransaksi", "saldo lama: ${it.saldo} → $saldoKembalikan")
                }

                val dompetBaru = dompetRepo.getById(transaksi.dompetId)
                Log.d("EditTransaksi", "dompetBaru=$dompetBaru")
                dompetBaru?.let {
                    val saldoBaru = if (transaksi.jenis == "PEMASUKAN")
                        it.saldo + transaksi.nominal
                    else
                        it.saldo - transaksi.nominal
                    dompetRepo.update(it.copy(saldo = saldoBaru))
                    Log.d("EditTransaksi", "saldo baru: ${it.saldo} → $saldoBaru")
                }

            } else {
                // ── Dompet SAMA ─────────────────────────────────────────────
                Log.d("EditTransaksi", "Dompet sama: $dompetIdLama")
                val dompet = dompetRepo.getById(transaksi.dompetId)
                Log.d("EditTransaksi", "dompet=$dompet")

                dompet?.let {
                    val saldoKembalikan = if (jenisLama == "PEMASUKAN")
                        it.saldo - nominalLama
                    else
                        it.saldo + nominalLama
                    val saldoBaru = if (transaksi.jenis == "PEMASUKAN")
                        saldoKembalikan + transaksi.nominal
                    else
                        saldoKembalikan - transaksi.nominal
                    dompetRepo.update(it.copy(saldo = saldoBaru))
                    Log.d("EditTransaksi", "saldo: ${it.saldo} → $saldoBaru")
                }
            }

            // ── Update transaksi — SELALU dijalankan, bahkan kalau dompet null ──
            transaksiRepo.update(transaksi)
            Log.d("EditTransaksi", "transaksiRepo.update() SELESAI ✓ id=${transaksi.id}")

        } catch (e: Exception) {
            // Tangkap exception yang sebelumnya tertelan diam-diam
            Log.e("EditTransaksi", "ERROR di update(): ${e.message}", e)
            // Tetap coba update transaksi walau ada error saldo
            try {
                transaksiRepo.update(transaksi)
                Log.d("EditTransaksi", "transaksiRepo.update() FALLBACK SELESAI ✓")
            } catch (e2: Exception) {
                Log.e("EditTransaksi", "FALLBACK juga gagal: ${e2.message}", e2)
            }
        }
    }

    fun delete(transaksi: Transaksi) = viewModelScope.launch {
        val dompet = dompetRepo.getById(transaksi.dompetId)
        dompet?.let {
            val saldoBaru = if (transaksi.jenis == "PEMASUKAN")
                it.saldo - transaksi.nominal
            else
                it.saldo + transaksi.nominal
            dompetRepo.update(it.copy(saldo = saldoBaru))
        }
        transaksiRepo.delete(transaksi)
    }

    fun getByDateRange(start: Long, end: Long) = transaksiRepo.getByDateRange(start, end)
    fun getByKategori(kategori: String)         = transaksiRepo.getByKategori(kategori)
}
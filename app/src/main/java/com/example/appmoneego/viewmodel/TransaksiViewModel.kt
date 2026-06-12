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

            transaksiRepo.update(transaksi)
            Log.d("EditTransaksi", "transaksiRepo.update() SELESAI ✓ id=${transaksi.id}")

        } catch (e: Exception) {
            Log.e("EditTransaksi", "ERROR di update(): ${e.message}", e)
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

    // ── FUNGSI BARU: update transaksi saldo awal saat dompet diedit ──────────
    //
    // Dipanggil oleh DompetFragment saat user edit dompet.
    // Hanya update: nominal, kategori, catatan.
    // TIDAK menyentuh saldo dompet (karena saldo sudah dihandle di DompetViewModel.update).
    //
    // Parameter:
    //   transaksi    — transaksi saldo awal yang akan diupdate
    //   nominalBaru  — saldo dompet terbaru (= nominal transaksi baru)
    //   kategoriBaru — nama dompet terbaru  (tampil sebagai judul di riwayat)
    //   catatanBaru  — jenis dompet terbaru (dibaca adapter untuk menentukan icon)
    fun updateSaldoAwalDompet(
        transaksi:    Transaksi,
        nominalBaru:  Double,
        kategoriBaru: String,
        catatanBaru:  String
    ) = viewModelScope.launch {
        val transaksiDiupdate = transaksi.copy(
            nominal  = nominalBaru,
            kategori = kategoriBaru,
            catatan  = catatanBaru
        )
        transaksiRepo.update(transaksiDiupdate)
        Log.d("SinkronDompet", "updateSaldoAwalDompet ✓ id=${transaksi.id} nominal=$nominalBaru kategori=$kategoriBaru catatan=$catatanBaru")
    }

    fun getByDateRange(start: Long, end: Long) = transaksiRepo.getByDateRange(start, end)
    fun getByKategori(kategori: String)         = transaksiRepo.getByKategori(kategori)
}
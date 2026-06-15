package com.example.appmoneego.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.dao.CicilanDao
import com.example.appmoneego.data.dao.HutangDao
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.repository.TransaksiRepository
import com.example.appmoneego.repository.DompetRepository
import kotlinx.coroutines.launch

class TransaksiViewModel(application: Application) : AndroidViewModel(application) {

    private val transaksiRepo: TransaksiRepository
    private val dompetRepo: DompetRepository

    // FIX BUG 2 & 3: akses langsung ke cicilan & hutang — dibutuhkan saat
    // transaksi cicilan hutang dihapus dari Riwayat Transaksi, agar riwayat
    // cicilan terkait ikut terhapus dan sudahDibayar/selesai dihitung ulang.
    private val cicilanDao: CicilanDao
    private val hutangDao: HutangDao

    val allTransaksi: LiveData<List<Transaksi>>

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        transaksiRepo = TransaksiRepository(db.transaksiDao())
        dompetRepo    = DompetRepository(db.dompetDao())
        cicilanDao    = db.cicilanDao()
        hutangDao     = db.hutangDao()
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
        // Kembalikan saldo dompet seperti semula
        val dompet = dompetRepo.getById(transaksi.dompetId)
        dompet?.let {
            val saldoBaru = if (transaksi.jenis == "PEMASUKAN")
                it.saldo - transaksi.nominal
            else
                it.saldo + transaksi.nominal
            dompetRepo.update(it.copy(saldo = saldoBaru))
        }

        // ── FIX BUG 2 & 3 ────────────────────────────────────────────────────
        // Jika transaksi ini adalah cicilan hutang (terhubung lewat
        // transaksiId di CicilanEntity), hapus juga riwayat cicilannya
        // di Detail Hutang dan hitung ulang sudahDibayar/selesai dari
        // data cicilan yang masih tersisa — bukan pakai nilai cache lama.
        val cicilanTerkait = cicilanDao.getCicilanByTransaksiId(transaksi.id)
        if (cicilanTerkait != null) {
            cicilanDao.deleteCicilanById(cicilanTerkait.id)

            val hutang = hutangDao.getHutangById(cicilanTerkait.hutangId)
            if (hutang != null) {
                val totalSudahDibayar = cicilanDao
                    .getCicilanByHutangId(hutang.id)
                    .sumOf { it.nominal }

                hutangDao.updateHutang(
                    hutang.copy(
                        sudahDibayar = totalSudahDibayar,
                        selesai = hutang.totalHutang > 0 &&
                                totalSudahDibayar >= hutang.totalHutang
                    )
                )
                Log.d("HapusTransaksi", "Cicilan terkait dihapus, hutang ${hutang.nama} di-recalc: sudahDibayar=$totalSudahDibayar")
            }
        }

        transaksiRepo.delete(transaksi)
    }

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
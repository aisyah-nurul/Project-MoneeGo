package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.repository.TransaksiRepository
import com.example.appmoneego.model.KategoriSummary

class AnalisisViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: TransaksiRepository
    val allTransaksi: LiveData<List<Transaksi>>

    // ringkasan per kategori untuk pie chart
    val kategoriSummary = MediatorLiveData<List<KategoriSummary>>()

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        repo = TransaksiRepository(db.transaksiDao())
        allTransaksi = repo.allTransaksi

        kategoriSummary.addSource(allTransaksi) { list ->
            val pengeluaran = list.filter { it.jenis == "PENGELUARAN" }
            val total = pengeluaran.sumOf { it.nominal }

            val grouped = pengeluaran
                .groupBy { it.kategori }
                .map { (kategori, items) ->
                    val jumlah = items.sumOf { it.nominal }
                    val persentase = if (total > 0) (jumlah / total * 100).toFloat() else 0f
                    KategoriSummary(kategori, jumlah, persentase)
                }
                .sortedByDescending { it.jumlah }

            kategoriSummary.value = grouped
        }
    }
}
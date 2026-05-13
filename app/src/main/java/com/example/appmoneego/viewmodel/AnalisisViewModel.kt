package com.example.appmoneego.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoneego.model.KategoriSummary
import java.text.SimpleDateFormat
import java.util.*

class AnalisisViewModel : ViewModel() {

    // ================= TAB =================
    private val _activeTab = MutableLiveData("PENGELUARAN")
    val activeTab: LiveData<String> = _activeTab

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    private val _subTab = MutableLiveData("kategori")
    val subTab: LiveData<String> = _subTab

    fun setSubTab(tab: String) {
        _subTab.value = tab
    }

    // ================= BULAN =================
    private val _currentCal = MutableLiveData(Calendar.getInstance())
    val currentCal: LiveData<Calendar> = _currentCal

    fun prevBulan() {
        val cal = _currentCal.value ?: Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        _currentCal.value = cal
    }

    fun nextBulan() {
        val cal = _currentCal.value ?: Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        _currentCal.value = cal
    }

    fun getLabelBulan(): String {
        val cal = _currentCal.value ?: Calendar.getInstance()
        val format = SimpleDateFormat("MMMM yyyy", Locale("id"))
        return format.format(cal.time)
    }

    // ================= DATA =================
    private val _kategoriSummaryPengeluaran = MutableLiveData<List<KategoriSummary>>()
    val kategoriSummaryPengeluaran: LiveData<List<KategoriSummary>> = _kategoriSummaryPengeluaran

    private val _kategoriSummaryPemasukan = MutableLiveData<List<KategoriSummary>>()
    val kategoriSummaryPemasukan: LiveData<List<KategoriSummary>> = _kategoriSummaryPemasukan

    private val _totalPengeluaran = MutableLiveData<Double>()
    val totalPengeluaran: LiveData<Double> = _totalPengeluaran

    private val _totalPemasukan = MutableLiveData<Double>()
    val totalPemasukan: LiveData<Double> = _totalPemasukan

    private val _transaksiTerbaru = MutableLiveData<List<KategoriSummary>>()
    val transaksiTerbaru: LiveData<List<KategoriSummary>> = _transaksiTerbaru

    init {
        loadDummyData()
    }

    private fun loadDummyData() {

        val pengeluaran = listOf(
            KategoriSummary("Makanan", 1500000.0, 40.0),
            KategoriSummary("Transportasi", 800000.0, 25.0),
            KategoriSummary("Belanja", 700000.0, 20.0),
            KategoriSummary("Lainnya", 500000.0, 15.0)
        )

        val pemasukan = listOf(
            KategoriSummary("Gaji", 5000000.0, 70.0),
            KategoriSummary("Bonus", 1500000.0, 20.0),
            KategoriSummary("Freelance", 500000.0, 10.0)
        )

        _kategoriSummaryPengeluaran.value = pengeluaran
        _kategoriSummaryPemasukan.value = pemasukan

        _totalPengeluaran.value = pengeluaran.sumOf { it.jumlah }
        _totalPemasukan.value = pemasukan.sumOf { it.jumlah }

        _transaksiTerbaru.value = pengeluaran
    }
}
package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.model.KategoriSummary
import com.example.appmoneego.repository.DompetRepository
import com.example.appmoneego.repository.TransaksiRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalisisViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: TransaksiRepository
    private val dompetRepo: DompetRepository

    // ── Tab & SubTab ──────────────────────────────────────────────────────────
    private val _activeTab = MutableLiveData("PENGELUARAN")
    val activeTab: LiveData<String> = _activeTab

    private val _subTab = MutableLiveData("kategori")
    val subTab: LiveData<String> = _subTab

    fun setTab(tab: String) { _activeTab.value = tab }
    fun setSubTab(tab: String) { _subTab.value = tab }

    // ── Kalender ──────────────────────────────────────────────────────────────
    private val _currentCal = MutableLiveData(Calendar.getInstance())
    val currentCal: LiveData<Calendar> = _currentCal

    private val _rangeStart = MutableLiveData<Long>()
    private val _rangeEnd   = MutableLiveData<Long>()

    // Dompet list untuk lookup nama
    val allDompet: LiveData<List<Dompet>>

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        repo       = TransaksiRepository(db.transaksiDao())
        dompetRepo = DompetRepository(db.dompetDao())
        allDompet  = dompetRepo.allDompet
        updateRange()
    }

    private fun updateRange() {
        val cal = _currentCal.value ?: Calendar.getInstance()
        val start = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        _rangeStart.value = start.timeInMillis
        _rangeEnd.value   = end.timeInMillis
    }

    fun prevBulan() {
        val cal = _currentCal.value ?: Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        _currentCal.value = cal
        updateRange()
    }

    fun nextBulan() {
        val cal = _currentCal.value ?: Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        _currentCal.value = cal
        updateRange()
    }

    fun getLabelBulan(): String {
        val cal = _currentCal.value ?: Calendar.getInstance()
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // ── Data Room ─────────────────────────────────────────────────────────────

    val transaksiPengeluaran: LiveData<List<Transaksi>> = _rangeStart.switchMap { start ->
        _rangeEnd.switchMap { end -> repo.getByJenisAndBulan("PENGELUARAN", start, end) }
    }

    val transaksiPemasukan: LiveData<List<Transaksi>> = _rangeStart.switchMap { start ->
        _rangeEnd.switchMap { end -> repo.getByJenisAndBulan("PEMASUKAN", start, end) }
    }

    // Untuk bottom sheet detail per kategori
    private val _selectedKategori = MutableLiveData<String?>()
    val selectedKategori: LiveData<String?> = _selectedKategori

    fun selectKategori(nama: String?) { _selectedKategori.value = nama }

    val transaksiByKategori: LiveData<List<Transaksi>> = _selectedKategori.switchMap { nama ->
        if (nama == null) MutableLiveData(emptyList())
        else _rangeStart.switchMap { start ->
            _rangeEnd.switchMap { end ->
                repo.getByKategoriAndBulan(nama, start, end)
            }
        }
    }

    // ── Summary Kategori ──────────────────────────────────────────────────────

    val kategoriSummaryPengeluaran = MediatorLiveData<List<KategoriSummary>>().apply {
        addSource(transaksiPengeluaran) { value = buildKategoriSummary(it, kategoriResolver) }
    }

    val kategoriSummaryPemasukan = MediatorLiveData<List<KategoriSummary>>().apply {
        addSource(transaksiPemasukan) { value = buildKategoriSummary(it, kategoriResolver) }
    }


    // ── Summary Dompet (dengan nama asli dari lookup) ─────────────────────────

    val dompetSummaryPengeluaran = MediatorLiveData<List<KategoriSummary>>()
    val dompetSummaryPemasukan   = MediatorLiveData<List<KategoriSummary>>()

    // Dipanggil dari Fragment setelah allDompet tersedia
    fun rebuildDompetSummary(dompetList: List<Dompet>) {
        val mapDompet = dompetList.associate { it.id to it.nama }
        transaksiPengeluaran.value?.let {
            dompetSummaryPengeluaran.value = buildDompetSummary(it, mapDompet)
        }
        transaksiPemasukan.value?.let {
            dompetSummaryPemasukan.value = buildDompetSummary(it, mapDompet)
        }
    }

    // ── Total ─────────────────────────────────────────────────────────────────

    val totalPengeluaran = MediatorLiveData<Double>().apply {
        addSource(transaksiPengeluaran) { value = it.sumOf { t -> t.nominal } }
    }

    val totalPemasukan = MediatorLiveData<Double>().apply {
        addSource(transaksiPemasukan) { value = it.sumOf { t -> t.nominal } }
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun buildKategoriSummary(
        list: List<Transaksi>,
        namaResolver: ((String) -> String)? = null
    ): List<KategoriSummary> {
        val total = list.sumOf { it.nominal }
        return list.groupBy { it.kategori }
            .map { (nama, items) ->
                val jumlah = items.sumOf { it.nominal }
                val persen = if (total > 0) Math.round(jumlah / total * 100).toFloat() else 0f
                val namaDisplay = namaResolver?.invoke(nama) ?: nama
                KategoriSummary(namaDisplay, jumlah, persen)
            }
            .sortedByDescending { it.jumlah }
    }

    private var kategoriResolver: ((String) -> String)? = null

    fun setKategoriResolver(resolver: (String) -> String) {
        kategoriResolver = resolver
        // Rebuild ulang dengan resolver baru
        transaksiPengeluaran.value?.let {
            kategoriSummaryPengeluaran.value = buildKategoriSummary(it, kategoriResolver)
        }
        transaksiPemasukan.value?.let {
            kategoriSummaryPemasukan.value = buildKategoriSummary(it, kategoriResolver)
        }
    }
    private fun buildDompetSummary(
        list: List<Transaksi>,
        dompetMap: Map<Int, String>
    ): List<KategoriSummary> {
        val total = list.sumOf { it.nominal }
        return list.groupBy { it.dompetId }
            .map { (dompetId, items) ->
                val jumlah = items.sumOf { it.nominal }
                val persen = if (total > 0) Math.round(jumlah / total * 100).toFloat() else 0f
                // Pakai nama dompet asli dari input pengguna
                val nama = dompetMap[dompetId] ?: "Dompet $dompetId"
                KategoriSummary(nama, jumlah, persen)
            }
            .sortedByDescending { it.jumlah }
    }
}
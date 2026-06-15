package com.example.appmoneego.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Transaksi
import com.example.appmoneego.model.InsightItem
import com.example.appmoneego.repository.DompetRepository
import com.example.appmoneego.repository.TabunganRepository
import com.example.appmoneego.repository.TransaksiRepository
import com.example.appmoneego.utils.InsightGenerator
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private var insightContext: Context = application.applicationContext

    fun updateContext(context: Context) {
        insightContext = context
        // Re-generate tips dan insight dengan locale baru
        _insightTips.value = InsightGenerator.getTipsRandom(insightContext)
        recalculateInsight()
    }

    private val transaksiRepo: TransaksiRepository
    private val dompetRepo: DompetRepository
    private val tabunganRepo: TabunganRepository

    // ── Saldo total semua dompet (tidak berubah saat ganti bulan) ─────────────
    val totalSaldo: LiveData<Double?>
    val allTransaksi: LiveData<List<Transaksi>>
    val saldoBersih = MediatorLiveData<Double>()

    // ── Pemasukan & Pengeluaran — berubah saat ganti bulan ────────────────────
    private val _totalPemasukan = MediatorLiveData<Double?>()
    val totalPemasukan: LiveData<Double?> get() = _totalPemasukan

    private val _totalPengeluaran = MediatorLiveData<Double?>()
    val totalPengeluaran: LiveData<Double?> get() = _totalPengeluaran

    // Sumber LiveData aktif saat ini (disimpan agar bisa di-remove sebelum ganti)
    private var sourcePemasukan:   LiveData<Double?>? = null
    private var sourcePengeluaran: LiveData<Double?>? = null

    // ── Insight ───────────────────────────────────────────────────────────────

    // Data mentah untuk insight — sumber dari Room, tidak berubah saat ganti bulan UI
    // karena insight selalu menampilkan bulan BERJALAN (Calendar.getInstance())
    private val _insightRingkasan = MediatorLiveData<InsightItem>()
    val insightRingkasan: LiveData<InsightItem> get() = _insightRingkasan

    // Tips random — di-generate satu kali saat ViewModel dibuat
    // Tidak perlu LiveData karena tidak bergantung database
    private val _insightTips = MutableLiveData<InsightItem>()
    fun refreshInsight() {
        _insightTips.value = InsightGenerator.getTipsRandom(insightContext)
        setupInsightSources()
    }

    val insightTips: LiveData<InsightItem> get() = _insightTips

    // Sumber-sumber data mentah untuk insight — disimpan agar bisa di-remove
    private var srcTotalJumlah:        LiveData<Int>?           = null
    private var srcTanggalTerakhir:    LiveData<Long?>?         = null
    private var srcPengeluaranIni:     LiveData<List<Transaksi>>? = null
    private var srcPengeluaranLalu:    LiveData<List<Transaksi>>? = null
    private var srcJumlahBulanIni:     LiveData<Int>?           = null

    // Nilai cache — dipakai untuk recalculate setiap kali salah satu sumber berubah
    private var cacheTotalJumlah:         Int                  = 0
    private var cacheTanggalTerakhir:     Long?                = null
    private var cachePengeluaranIni:      List<Transaksi>      = emptyList()
    private var cachePengeluaranLalu:     List<Transaksi>      = emptyList()
    private var cacheJumlahBulanIni:      Int                  = 0

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        transaksiRepo = TransaksiRepository(db.transaksiDao())
        dompetRepo    = DompetRepository(db.dompetDao())
        tabunganRepo  = TabunganRepository(db.tabunganDao())

        totalSaldo   = dompetRepo.totalSaldo
        allTransaksi = transaksiRepo.allTransaksi

        saldoBersih.addSource(totalSaldo) { nilai ->
            saldoBersih.value = nilai ?: 0.0
        }

        // Load bulan saat ini saat pertama kali dibuat
        loadBulan(Calendar.getInstance())

        // Tips random — di-generate sekali saat ViewModel pertama dibuat
        _insightTips.value = InsightGenerator.getTipsRandom(getApplication())

        // Setup sumber-sumber data untuk insight
        setupInsightSources()
    }

    // ── Setup sumber data insight ─────────────────────────────────────────────

    /**
     * Insight selalu mengacu pada bulan BERJALAN (bukan bulan yang ditampilkan
     * di card saldo yang bisa digeser user), sehingga range-nya dihitung
     * dari Calendar.getInstance() bukan dari calBulanAktif.
     */
    private fun setupInsightSources() {
        val sekarang = Calendar.getInstance()
        val (startIni, endIni) = getRangeFromCalendar(sekarang)

        val bulanLalu = (sekarang.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val (startLalu, endLalu) = getRangeFromCalendar(bulanLalu)

        // Hapus sumber lama jika ada
        srcTotalJumlah?.let     { _insightRingkasan.removeSource(it) }
        srcTanggalTerakhir?.let { _insightRingkasan.removeSource(it) }
        srcPengeluaranIni?.let  { _insightRingkasan.removeSource(it) }
        srcPengeluaranLalu?.let { _insightRingkasan.removeSource(it) }
        srcJumlahBulanIni?.let  { _insightRingkasan.removeSource(it) }

        // Buat sumber baru
        val newTotalJumlah     = transaksiRepo.totalJumlahTransaksi
        val newTanggalTerakhir = transaksiRepo.tanggalTransaksiTerakhir
        val newPengeluaranIni  = transaksiRepo.getPengeluaranNonTransferBulanIni(startIni, endIni)
        val newPengeluaranLalu = transaksiRepo.getPengeluaranNonTransferBulanLalu(startLalu, endLalu)
        val newJumlahBulanIni  = transaksiRepo.getJumlahTransaksiBulanIni(startIni, endIni)

        srcTotalJumlah     = newTotalJumlah
        srcTanggalTerakhir = newTanggalTerakhir
        srcPengeluaranIni  = newPengeluaranIni
        srcPengeluaranLalu = newPengeluaranLalu
        srcJumlahBulanIni  = newJumlahBulanIni

        // Tambahkan ke MediatorLiveData — setiap kali salah satu berubah,
        // recalculate insight dengan data terbaru
        _insightRingkasan.addSource(newTotalJumlah) { v ->
            cacheTotalJumlah = v ?: 0
            recalculateInsight()
        }
        _insightRingkasan.addSource(newTanggalTerakhir) { v ->
            cacheTanggalTerakhir = v
            recalculateInsight()
        }
        _insightRingkasan.addSource(newPengeluaranIni) { v ->
            cachePengeluaranIni = v ?: emptyList()
            recalculateInsight()
        }
        _insightRingkasan.addSource(newPengeluaranLalu) { v ->
            cachePengeluaranLalu = v ?: emptyList()
            recalculateInsight()
        }
        _insightRingkasan.addSource(newJumlahBulanIni) { v ->
            cacheJumlahBulanIni = v ?: 0
            recalculateInsight()
        }
    }

    private fun recalculateInsight() {
        _insightRingkasan.value = InsightGenerator.hitungInsight(
            context                = insightContext,
            totalJumlahTransaksi   = cacheTotalJumlah,
            tanggalTerakhir        = cacheTanggalTerakhir,
            pengeluaranBulanIni    = cachePengeluaranIni,
            pengeluaranBulanLalu   = cachePengeluaranLalu,
            jumlahTransaksiBulanIni = cacheJumlahBulanIni
        )
    }

    // ── Load Bulan (untuk card saldo) ─────────────────────────────────────────

    /**
     * Dipanggil dari Fragment setiap kali user menekan tombol ◀ atau ▶.
     * Mengganti sumber LiveData pemasukan & pengeluaran ke range bulan baru.
     * Insight TIDAK ikut berubah karena insight selalu bulan berjalan.
     */
    fun loadBulan(cal: Calendar) {
        val (start, end) = getRangeFromCalendar(cal)

        sourcePemasukan?.let   { _totalPemasukan.removeSource(it) }
        sourcePengeluaran?.let { _totalPengeluaran.removeSource(it) }

        val newPemasukan   = transaksiRepo.getPemasukanBulanIni(start, end)
        val newPengeluaran = transaksiRepo.getPengeluaranBulanIni(start, end)

        sourcePemasukan   = newPemasukan
        sourcePengeluaran = newPengeluaran

        _totalPemasukan.addSource(newPemasukan)     { v -> _totalPemasukan.value   = v }
        _totalPengeluaran.addSource(newPengeluaran) { v -> _totalPengeluaran.value = v }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun getRangeFromCalendar(cal: Calendar): Pair<Long, Long> {
        val c = cal.clone() as Calendar

        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis

        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        val end = c.timeInMillis

        return Pair(start, end)
    }
}
package com.example.appmoneego.ui.tabungan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Tabungan
import kotlinx.coroutines.launch

class TabunganViewModel(application: Application) : AndroidViewModel(application) {

    private val tabunganDao = MoneeGoDatabase.getDatabase(application).tabunganDao()
    private val dompetDao   = MoneeGoDatabase.getDatabase(application).dompetDao()

    val tabunganList = tabunganDao.getAllTabungan()

    fun insert(tabungan: Tabungan) = viewModelScope.launch {
        tabunganDao.insert(tabungan)
    }

    fun update(tabungan: Tabungan) = viewModelScope.launch {
        tabunganDao.update(tabungan)
    }

    fun delete(tabungan: Tabungan) = viewModelScope.launch {
        tabunganDao.delete(tabungan)
    }

    fun tambahTabungan(tabungan: Tabungan, nominal: Double, dompetId: Int) =
        viewModelScope.launch {
            val newTerkumpul = (tabungan.terkumpul + nominal)
                .coerceAtMost(tabungan.targetNominal)
            tabunganDao.update(tabungan.copy(terkumpul = newTerkumpul))
            dompetDao.kurangiSaldo(dompetId, nominal)
        }
}
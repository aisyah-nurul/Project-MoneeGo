package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Tabungan
import com.example.appmoneego.repository.TabunganRepository
import kotlinx.coroutines.launch

class TabunganViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: TabunganRepository

    val allTabungan: LiveData<List<Tabungan>>

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        repo = TabunganRepository(db.tabunganDao())
        allTabungan = repo.allTabungan
    }

    fun insert(tabungan: Tabungan) = viewModelScope.launch {
        repo.insert(tabungan)
    }

    fun update(tabungan: Tabungan) = viewModelScope.launch {
        repo.update(tabungan)
    }

    fun delete(tabungan: Tabungan) = viewModelScope.launch {
        repo.delete(tabungan)
    }

    fun tambahTerkumpul(tabungan: Tabungan, jumlah: Double) = viewModelScope.launch {
        val updated = tabungan.copy(terkumpul = tabungan.terkumpul + jumlah)
        repo.update(updated)
    }

    // hitung persentase progress tabungan
    fun getProgress(tabungan: Tabungan): Int {
        if (tabungan.targetNominal == 0.0) return 0
        return ((tabungan.terkumpul / tabungan.targetNominal) * 100).toInt().coerceIn(0, 100)
    }
}
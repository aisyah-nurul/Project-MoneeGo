package com.example.appmoneego.ui.tabungan

import androidx.lifecycle.*
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Tabungan
import com.example.appmoneego.repository.TabunganRepository
import kotlinx.coroutines.launch
import android.app.Application

class TabunganViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TabunganRepository by lazy {
        val db = MoneeGoDatabase.getDatabase(application)
        TabunganRepository(db.tabunganDao())
    }

    val tabunganList: LiveData<List<Tabungan>> = repository.allTabungan

    fun insert(tabungan: Tabungan) = viewModelScope.launch {
        repository.insert(tabungan)
    }

    fun update(tabungan: Tabungan) = viewModelScope.launch {
        repository.update(tabungan)
    }

    fun delete(tabungan: Tabungan) = viewModelScope.launch {
        repository.delete(tabungan)
    }

    fun tambahTerkumpul(tabungan: Tabungan, jumlah: Double) = viewModelScope.launch {
        repository.update(tabungan.copy(terkumpul = tabungan.terkumpul + jumlah))
    }
}
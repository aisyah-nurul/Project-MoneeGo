package com.example.appmoneego.ui.hutang

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Hutang
import kotlinx.coroutines.launch

class HutangViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MoneeGoDatabase.getDatabase(application).hutangDao()

    val hutangList: LiveData<List<Hutang>> = dao.getAllHutang()

    fun insert(hutang: Hutang) = viewModelScope.launch {
        dao.insertHutang(hutang)
    }

    fun update(hutang: Hutang) = viewModelScope.launch {
        dao.updateHutang(hutang)
    }

    fun delete(hutang: Hutang) = viewModelScope.launch {
        dao.deleteHutang(hutang)
    }

    fun tandaiSelesai(hutang: Hutang) = viewModelScope.launch {
        dao.updateHutang(hutang.copy(selesai = true))
    }
}

// Extension property: sisa = totalHutang - sudahDibayar
val Hutang.sisaHutang: Long
    get() = (totalHutang - sudahDibayar).coerceAtLeast(0L)
package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Hutang
import com.example.appmoneego.repository.HutangRepository
import kotlinx.coroutines.launch

class HutangViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: HutangRepository

    val allHutang: LiveData<List<Hutang>>
    val hutangAktif: LiveData<List<Hutang>>

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        repo = HutangRepository(db.hutangDao())
        allHutang = repo.allHutang
        hutangAktif = repo.hutangAktif
    }

    fun insert(hutang: Hutang) = viewModelScope.launch {
        repo.insert(hutang)
    }

    fun update(hutang: Hutang) = viewModelScope.launch {
        repo.update(hutang)
    }

    fun delete(hutang: Hutang) = viewModelScope.launch {
        repo.delete(hutang)
    }

    fun lunaskan(id: Int) = viewModelScope.launch {
        repo.lunaskan(id)
    }
}
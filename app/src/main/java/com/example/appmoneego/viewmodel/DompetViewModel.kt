package com.example.appmoneego.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoneego.data.database.MoneeGoDatabase
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.repository.DompetRepository
import kotlinx.coroutines.launch

class DompetViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: DompetRepository

    val allDompet: LiveData<List<Dompet>>
    val totalSaldo: LiveData<Double?>

    init {
        val db = MoneeGoDatabase.getDatabase(application)
        repo = DompetRepository(db.dompetDao())
        allDompet = repo.allDompet
        totalSaldo = repo.totalSaldo
    }

    fun insert(dompet: Dompet) = viewModelScope.launch {
        repo.insert(dompet)
    }

    fun update(dompet: Dompet) = viewModelScope.launch {
        repo.update(dompet)
    }

    fun delete(dompet: Dompet) = viewModelScope.launch {
        repo.delete(dompet)
    }
}
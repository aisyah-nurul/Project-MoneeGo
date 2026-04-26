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

    private val repository: DompetRepository
    val allDompet: LiveData<List<Dompet>>
    val totalSaldo: LiveData<Double?>
    val jumlahDompet: LiveData<Int>
    val dompetTerbesar: LiveData<Dompet?>

    init {
        val dao = MoneeGoDatabase.getDatabase(application).dompetDao()
        repository = DompetRepository(dao)
        allDompet = repository.allDompet
        totalSaldo = repository.totalSaldo
        jumlahDompet = repository.jumlahDompet
        dompetTerbesar = repository.dompetTerbesar
    }

    fun insert(dompet: Dompet) = viewModelScope.launch {
        repository.insert(dompet)
    }

    fun update(dompet: Dompet) = viewModelScope.launch {
        repository.update(dompet)
    }

    fun delete(dompet: Dompet) = viewModelScope.launch {
        repository.delete(dompet)
    }
}
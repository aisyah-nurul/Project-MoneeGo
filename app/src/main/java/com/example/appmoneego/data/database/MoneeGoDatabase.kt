package com.example.appmoneego.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appmoneego.data.dao.*
import com.example.appmoneego.data.entity.*

@Database(
    entities = [Transaksi::class, Dompet::class, Tabungan::class, Hutang::class],
    version = 1,
    exportSchema = false
)

abstract class MoneeGoDatabase : RoomDatabase() {

    abstract fun transaksiDao(): TransaksiDao
    abstract fun dompetDao(): DompetDao
    abstract fun tabunganDao(): TabunganDao
    abstract fun hutangDao(): HutangDao

    companion object {
        @Volatile
        private var INSTANCE: MoneeGoDatabase? = null

        fun getDatabase(context: Context): MoneeGoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneeGoDatabase::class.java,
                    "moneego_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
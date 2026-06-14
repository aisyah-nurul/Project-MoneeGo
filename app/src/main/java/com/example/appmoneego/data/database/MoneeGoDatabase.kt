package com.example.appmoneego.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appmoneego.data.dao.*
import com.example.appmoneego.data.entity.*

@Database(
    entities = [
        Transaksi::class,
        Dompet::class,
        Tabungan::class,
        Hutang::class,
        CicilanEntity::class
    ],
    // FIX BUG 2: version dinaikkan 3 → 4 karena CicilanEntity menambah
    // kolom baru "transaksiId". fallbackToDestructiveMigration akan
    // membuat ulang database (data lama hilang) — wajar untuk tahap dev.
    version = 4,
    exportSchema = false
)
abstract class MoneeGoDatabase : RoomDatabase() {

    abstract fun transaksiDao(): TransaksiDao
    abstract fun dompetDao(): DompetDao
    abstract fun tabunganDao(): TabunganDao
    abstract fun hutangDao(): HutangDao
    abstract fun cicilanDao(): CicilanDao

    companion object {
        @Volatile
        private var INSTANCE: MoneeGoDatabase? = null

        fun getDatabase(context: Context): MoneeGoDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneeGoDatabase::class.java,
                    "moneego_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
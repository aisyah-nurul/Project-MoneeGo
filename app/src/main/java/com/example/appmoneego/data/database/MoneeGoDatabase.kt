package com.example.appmoneego.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
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

        /**
         * Migration versi 4 -> 5
         * Menambahkan kolom sudahDigunakan pada tabel tabungan
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE tabungan 
                    ADD COLUMN sudahDigunakan INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): MoneeGoDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneeGoDatabase::class.java,
                    "moneego_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
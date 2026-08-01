package com.elprompter.promptvault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Database lokal 100% offline untuk ActivityLog & MoveHistory.
 *
 * Rules dan Settings SENGAJA tidak dipindah ke sini -- keduanya tetap di
 * DataStore Preferences karena ukurannya kecil dan tidak butuh query relasional.
 * Hanya data yang berpotensi tumbuh besar (log & riwayat pemindahan) yang
 * dipindah ke SQLite lewat Room, supaya UI tidak perlu decode ulang JSON
 * ratusan/ribuan baris setiap kali ada perubahan kecil.
 */
@Database(
    entities = [ActivityLogEntity::class, MoveHistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityLogDao(): ActivityLogDao
    abstract fun moveHistoryDao(): MoveHistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prompt_vault.db"
                )
                    // Tabel ini hanya berisi log/riwayat non-kritis (bukan rules
                    // pengguna). Kalau skema berubah tanpa migrasi resmi di masa
                    // depan, lebih aman reset tabel log daripada app crash total.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

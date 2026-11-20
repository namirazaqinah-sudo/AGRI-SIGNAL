package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FarmerAccountEntity::class, ScanRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AgriDatabase : RoomDatabase() {
    abstract fun farmerDao(): FarmerDao
    abstract fun scanRecordDao(): ScanRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AgriDatabase? = null

        fun getInstance(context: Context): AgriDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgriDatabase::class.java,
                    "agri_signal.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

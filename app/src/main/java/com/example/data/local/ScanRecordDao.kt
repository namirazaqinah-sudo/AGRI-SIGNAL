package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records WHERE userPhone = :userPhone ORDER BY createdAtMillis DESC")
    fun observeRecordsByUser(userPhone: String): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): ScanRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScanRecordEntity): Long

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}

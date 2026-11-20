package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FarmerDao {
    @Query("SELECT * FROM farmer_accounts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getFarmerByPhone(phone: String): FarmerAccountEntity?

    @Query("SELECT * FROM farmer_accounts ORDER BY createdAtMillis DESC")
    suspend fun getAllFarmers(): List<FarmerAccountEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFarmer(farmer: FarmerAccountEntity)
}

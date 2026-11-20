package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_accounts")
data class FarmerAccountEntity(
    @PrimaryKey val phoneNumber: String,
    val fullName: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

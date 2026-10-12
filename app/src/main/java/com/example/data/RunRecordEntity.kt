package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_records")
data class RunRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val distanceMeters: Int,
    val coinsCollected: Int,
    val characterId: String,
    val biomeName: String,
    val timestamp: Long = System.currentTimeMillis()
)

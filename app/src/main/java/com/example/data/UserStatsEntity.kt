package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 150,
    val highScore: Int = 0,
    val bestDistance: Int = 0,
    val totalRuns: Int = 0,
    val selectedCharacterId: String = "nova",
    val unlockedCharacters: String = "nova", // comma separated
    val magnetLevel: Int = 1,
    val jetpackLevel: Int = 1,
    val shieldLevel: Int = 1,
    val multiplierLevel: Int = 1
)

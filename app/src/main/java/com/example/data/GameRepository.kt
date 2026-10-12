package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val dao: GameDao) {

    val userStats: Flow<UserStatsEntity> = dao.getUserStatsFlow().map { stats ->
        stats ?: UserStatsEntity()
    }

    val topRuns: Flow<List<RunRecordEntity>> = dao.getTopRuns()
    val recentRuns: Flow<List<RunRecordEntity>> = dao.getRecentRuns()

    suspend fun getStats(): UserStatsEntity {
        return dao.getUserStats() ?: UserStatsEntity().also {
            dao.insertOrUpdateUserStats(it)
        }
    }

    suspend fun saveRun(
        score: Int,
        distanceMeters: Int,
        coinsCollected: Int,
        characterId: String,
        biomeName: String
    ): Boolean {
        val current = getStats()
        val isNewHighScore = score > current.highScore

        val updated = current.copy(
            coins = current.coins + coinsCollected,
            highScore = maxOf(current.highScore, score),
            bestDistance = maxOf(current.bestDistance, distanceMeters),
            totalRuns = current.totalRuns + 1
        )
        dao.insertOrUpdateUserStats(updated)

        dao.insertRunRecord(
            RunRecordEntity(
                score = score,
                distanceMeters = distanceMeters,
                coinsCollected = coinsCollected,
                characterId = characterId,
                biomeName = biomeName
            )
        )

        return isNewHighScore
    }

    suspend fun selectCharacter(characterId: String) {
        val current = getStats()
        if (current.unlockedCharacters.split(",").contains(characterId)) {
            dao.insertOrUpdateUserStats(current.copy(selectedCharacterId = characterId))
        }
    }

    suspend fun unlockCharacter(characterId: String, cost: Int): Boolean {
        val current = getStats()
        if (current.coins >= cost) {
            val list = current.unlockedCharacters.split(",").toMutableList()
            if (!list.contains(characterId)) {
                list.add(characterId)
                dao.insertOrUpdateUserStats(
                    current.copy(
                        coins = current.coins - cost,
                        unlockedCharacters = list.joinToString(","),
                        selectedCharacterId = characterId
                    )
                )
                return true
            }
        }
        return false
    }

    suspend fun upgradeTech(techType: String, cost: Int): Boolean {
        val current = getStats()
        if (current.coins < cost) return false

        val updated = when (techType) {
            "magnet" -> if (current.magnetLevel < 5) current.copy(coins = current.coins - cost, magnetLevel = current.magnetLevel + 1) else return false
            "jetpack" -> if (current.jetpackLevel < 5) current.copy(coins = current.coins - cost, jetpackLevel = current.jetpackLevel + 1) else return false
            "shield" -> if (current.shieldLevel < 5) current.copy(coins = current.coins - cost, shieldLevel = current.shieldLevel + 1) else return false
            "multiplier" -> if (current.multiplierLevel < 5) current.copy(coins = current.coins - cost, multiplierLevel = current.multiplierLevel + 1) else return false
            else -> return false
        }
        dao.insertOrUpdateUserStats(updated)
        return true
    }
}

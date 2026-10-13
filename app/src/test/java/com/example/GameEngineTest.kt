package com.example

import com.example.data.UserStatsEntity
import com.example.game.ALL_CHARACTERS
import com.example.game.Biome
import com.example.game.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun testCharacterList() {
        assertTrue(ALL_CHARACTERS.isNotEmpty())
        assertEquals("nova", ALL_CHARACTERS.first().id)
    }

    @Test
    fun testBiomes() {
        assertEquals(4, Biome.values().size)
    }

    @Test
    fun testUserStatsDefaults() {
        val stats = UserStatsEntity()
        assertEquals(150, stats.coins)
        assertEquals("nova", stats.selectedCharacterId)
        assertEquals(1, stats.magnetLevel)
    }
}

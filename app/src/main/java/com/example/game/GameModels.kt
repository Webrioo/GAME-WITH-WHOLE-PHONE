package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.R

enum class ObstacleType {
    LOW_ROCK,        // Jump over
    HIGH_LASER,      // Slide under
    TALL_CONTAINER,  // Change lane
    HOVER_DRONE,     // Flying robot, change lane
    WARP_GATE        // Hyperspace Stargate to travel to next planet!
}

data class Obstacle(
    val id: Long,
    val lane: Int, // -1, 0, 1
    var z: Float,  // Distance ahead of camera
    val type: ObstacleType
)

enum class CollectibleType {
    COIN,
    JETPACK,
    MAGNET,
    SHIELD,
    MULTIPLIER_2X,
    HYPER_RING,    // Launch into super-jump orbit!
    NITRO_CELL     // Collect to charge hyperdrive dash
}

data class Collectible(
    val id: Long,
    var lane: Int,
    var z: Float,
    var yOffset: Float = 0f, // 0 for ground level, >0 for jump arc or jetpack altitude
    val type: CollectibleType,
    var isCollected: Boolean = false
)

enum class Biome(
    val title: String,
    val bgDrawableRes: Int,
    val gridColor: Color,
    val railColor: Color,
    val glowColor: Color,
    val targetScore: Int,
    val nextBiomeName: String
) {
    MOON_BASE(
        title = "Moon Base Alpha",
        bgDrawableRes = R.drawable.bg_moon,
        gridColor = Color(0xFF00E5FF),
        railColor = Color(0xFF38BDF8),
        glowColor = Color(0x3300E5FF),
        targetScore = 1000,
        nextBiomeName = "Mars Olympus Rift"
    ),
    MARS_CANYONS(
        title = "Mars Olympus Rift",
        bgDrawableRes = R.drawable.bg_mars,
        gridColor = Color(0xFFFF6D00),
        railColor = Color(0xFFFB923C),
        glowColor = Color(0x33FF6D00),
        targetScore = 2500,
        nextBiomeName = "Cosmic Nebula"
    ),
    DEEP_GALAXY(
        title = "Cosmic Nebula",
        bgDrawableRes = R.drawable.bg_galaxy,
        gridColor = Color(0xFFD946EF),
        railColor = Color(0xFFA855F7),
        glowColor = Color(0x33D946EF),
        targetScore = 5000,
        nextBiomeName = "Saturn Rings Station"
    ),
    SATURN_RINGS(
        title = "Saturn Rings Station",
        bgDrawableRes = R.drawable.bg_saturn,
        gridColor = Color(0xFFFFD700),
        railColor = Color(0xFFFBBF24),
        glowColor = Color(0x33FFD700),
        targetScore = 10000,
        nextBiomeName = "Moon Base Alpha"
    )
}

data class CharacterInfo(
    val id: String,
    val name: String,
    val title: String,
    val drawableRes: Int,
    val suitColor: Color,
    val visorColor: Color,
    val boardColor: Color,
    val jetFlameColor: Color,
    val cost: Int
)

val ALL_CHARACTERS = listOf(
    CharacterInfo(
        id = "nova",
        name = "Astro Nova",
        title = "Galactic Pathfinder",
        drawableRes = R.drawable.avatar_nova,
        suitColor = Color(0xFFE2E8F0),
        visorColor = Color(0xFF00F0FF),
        boardColor = Color(0xFF0284C7),
        jetFlameColor = Color(0xFF38BDF8),
        cost = 0
    ),
    CharacterInfo(
        id = "cosmo",
        name = "Cosmo 9000",
        title = "Cybernetic Android",
        drawableRes = R.drawable.avatar_cosmo,
        suitColor = Color(0xFF475569),
        visorColor = Color(0xFFE879F9),
        boardColor = Color(0xFFA855F7),
        jetFlameColor = Color(0xFFF43F5E),
        cost = 250
    ),
    CharacterInfo(
        id = "zyx",
        name = "Zyx Starling",
        title = "Extraterrestrial Prodigy",
        drawableRes = R.drawable.avatar_zyx,
        suitColor = Color(0xFF0D9488),
        visorColor = Color(0xFF34D399),
        boardColor = Color(0xFF10B981),
        jetFlameColor = Color(0xFF22D3EE),
        cost = 600
    )
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var alpha: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float
)

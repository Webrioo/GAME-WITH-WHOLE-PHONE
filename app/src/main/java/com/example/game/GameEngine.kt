package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.HapticsManager
import com.example.audio.SoundSynthesizer
import com.example.data.UserStatsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    private val soundSynthesizer: SoundSynthesizer,
    private val hapticsManager: HapticsManager
) {
    companion object {
        const val LANE_LEFT = -1
        const val LANE_CENTER = 0
        const val LANE_RIGHT = 1
        const val Z_MAX = 1200f
        const val Z_PLAYER = 60f
        const val BASE_SPEED = 240f // units per second
        const val MAX_SPEED = 560f
        const val GRAVITY = 1100f
        const val JUMP_FORCE = 520f
        const val SLIDE_TIME = 0.85f // seconds
        const val JETPACK_ALTITUDE = 160f
    }

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var nextObjectId = 1L
    private var spawnTimer = 0f
    private var powerupTimer = 0f
    private var coinStreak = 0
    private var coinStreakResetTimer = 0f

    fun startNewGame(userStats: UserStatsEntity) {
        val selectedChar = ALL_CHARACTERS.find { it.id == userStats.selectedCharacterId } ?: ALL_CHARACTERS.first()

        // Calculate power-up durations based on tech upgrades
        val magnetDuration = 10f + (userStats.magnetLevel - 1) * 2.5f
        val jetpackDuration = 8f + (userStats.jetpackLevel - 1) * 2.0f
        val shieldDuration = 15f + (userStats.shieldLevel - 1) * 3.0f
        val multiplierBonus = 1 + (userStats.multiplierLevel - 1) * 0.25f

        _gameState.value = GameState(
            isPlaying = true,
            isGameOver = false,
            isPaused = false,
            character = selectedChar,
            magnetMaxDuration = magnetDuration,
            jetpackMaxDuration = jetpackDuration,
            shieldMaxDuration = shieldDuration,
            baseMultiplierBonus = multiplierBonus
        )

        spawnTimer = 0f
        powerupTimer = 0f
        coinStreak = 0
        soundSynthesizer.startMusic()
    }

    fun pauseGame() {
        val state = _gameState.value
        if (state.isPlaying && !state.isGameOver) {
            _gameState.value = state.copy(isPaused = true)
        }
    }

    fun resumeGame() {
        val state = _gameState.value
        if (state.isPlaying && !state.isGameOver) {
            _gameState.value = state.copy(isPaused = false)
        }
    }

    fun moveLeft() {
        val state = _gameState.value
        if (!state.isPlaying || state.isGameOver || state.isPaused) return
        if (state.targetLane > LANE_LEFT) {
            _gameState.value = state.copy(targetLane = state.targetLane - 1)
            hapticsManager.vibrateTap()
        }
    }

    fun moveRight() {
        val state = _gameState.value
        if (!state.isPlaying || state.isGameOver || state.isPaused) return
        if (state.targetLane < LANE_RIGHT) {
            _gameState.value = state.copy(targetLane = state.targetLane + 1)
            hapticsManager.vibrateTap()
        }
    }

    fun jump() {
        val state = _gameState.value
        if (!state.isPlaying || state.isGameOver || state.isPaused) return
        if (state.jetpackActiveTime > 0f) return // Already in sky

        if (state.playerJumpY <= 1f) {
            _gameState.value = state.copy(
                isJumping = true,
                isSliding = false,
                slideTimeRemaining = 0f,
                jumpVelocity = JUMP_FORCE
            )
            soundSynthesizer.playJumpSound()
            hapticsManager.vibrateJump()
        }
    }

    fun slide() {
        val state = _gameState.value
        if (!state.isPlaying || state.isGameOver || state.isPaused) return
        if (state.jetpackActiveTime > 0f) return

        // Fast fall if currently in the air
        val newJumpY = if (state.playerJumpY > 0f) 0f else state.playerJumpY
        val newVel = if (state.playerJumpY > 0f) -400f else 0f

        _gameState.value = state.copy(
            isSliding = true,
            isJumping = false,
            slideTimeRemaining = SLIDE_TIME,
            playerJumpY = newJumpY,
            jumpVelocity = newVel
        )
        soundSynthesizer.playSlideSound()
        hapticsManager.vibrateSlide()
    }

    fun update(deltaSeconds: Float) {
        val state = _gameState.value
        if (!state.isPlaying || state.isGameOver || state.isPaused) return

        val dt = deltaSeconds.coerceIn(0f, 0.05f)

        // Calculate current forward speed
        val currentSpeed = (BASE_SPEED + (state.distanceRunMeters * 0.16f)).coerceAtMost(MAX_SPEED)
        val distanceDelta = (currentSpeed * dt) / 10f // meters
        val newDistance = state.distanceRunMeters + distanceDelta

        // Dynamic Biome based on distance
        val currentBiome = when {
            newDistance % 2400f < 800f -> Biome.MOON_BASE
            newDistance % 2400f < 1600f -> Biome.MARS_CANYONS
            else -> Biome.DEEP_GALAXY
        }

        // Active power-up timers
        val newMagnetTime = (state.magnetActiveTime - dt).coerceAtLeast(0f)
        val newJetpackTime = (state.jetpackActiveTime - dt).coerceAtLeast(0f)
        val newShieldTime = (state.shieldActiveTime - dt).coerceAtLeast(0f)
        val newMultiplierTime = (state.multiplierActiveTime - dt).coerceAtLeast(0f)

        // Score Multiplier
        val activeMultiplier = (if (newMultiplierTime > 0f) 2f else 1f) * state.baseMultiplierBonus
        val scoreGain = (distanceDelta * 2.2f * activeMultiplier).toInt()
        val newScore = state.score + scoreGain

        // Smooth horizontal lane transition
        val currentLaneX = state.playerLaneX + (state.targetLane - state.playerLaneX) * (14f * dt).coerceAtMost(1f)

        // Jump & Gravity physics
        var playerY = state.playerJumpY
        var jumpVel = state.jumpVelocity
        var isJumping = state.isJumping

        if (newJetpackTime > 0f) {
            // Jetpack smoothly hovers in the stratosphere
            playerY += (JETPACK_ALTITUDE - playerY) * (8f * dt)
            jumpVel = 0f
            isJumping = false
        } else {
            // Normal gravity
            if (playerY > 0f || jumpVel > 0f) {
                playerY += jumpVel * dt
                jumpVel -= GRAVITY * dt
                if (playerY <= 0f) {
                    playerY = 0f
                    jumpVel = 0f
                    isJumping = false
                }
            }
        }

        // Slide / Crouch timer
        var slideTime = state.slideTimeRemaining
        var isSliding = state.isSliding
        if (slideTime > 0f) {
            slideTime -= dt
            if (slideTime <= 0f) {
                slideTime = 0f
                isSliding = false
            }
        }

        // Update coin streak combo timer
        if (coinStreakResetTimer > 0f) {
            coinStreakResetTimer -= dt
            if (coinStreakResetTimer <= 0f) {
                coinStreak = 0
            }
        }

        // Advance obstacles
        val updatedObstacles = mutableListOf<Obstacle>()
        var hitObstacle = false
        var hitObstacleType: ObstacleType? = null

        val isJetpackFlying = newJetpackTime > 0f

        for (obs in state.obstacles) {
            val prevZ = obs.z
            val newZ = prevZ - (currentSpeed * dt)

            // Collision check: player is around Z_PLAYER (depth buffer ~ 40f to 80f)
            val inCollisionZ = newZ in (Z_PLAYER - 25f)..(Z_PLAYER + 25f)
            val inPlayerLane = kotlin.math.abs(obs.lane - currentLaneX) < 0.55f

            if (!isJetpackFlying && inCollisionZ && inPlayerLane && !state.isInvulnerable) {
                var collisionAvoided = false
                when (obs.type) {
                    ObstacleType.LOW_ROCK -> {
                        // Avoided if jumping high enough
                        if (playerY > 38f) collisionAvoided = true
                    }
                    ObstacleType.HIGH_LASER -> {
                        // Avoided if sliding/crouching
                        if (isSliding && playerY < 15f) collisionAvoided = true
                    }
                    ObstacleType.TALL_CONTAINER, ObstacleType.HOVER_DRONE -> {
                        // Must be in different lane!
                        collisionAvoided = false
                    }
                }

                if (!collisionAvoided) {
                    hitObstacle = true
                    hitObstacleType = obs.type
                }
            }

            if (newZ > -40f && !hitObstacle) {
                updatedObstacles.add(obs.copy(z = newZ))
            }
        }

        // Handle collision with obstacle
        var finalShieldTime = newShieldTime
        var isGameOver = false
        var invulnerableTimer = (state.invulnerableTimer - dt).coerceAtLeast(0f)

        if (hitObstacle) {
            if (finalShieldTime > 0f) {
                // Shield absorbs blow!
                finalShieldTime = 0f
                invulnerableTimer = 1.8f
                soundSynthesizer.playShieldDeflectSound()
                hapticsManager.vibrateCrash()
                // Clear immediate obstacles in front
                updatedObstacles.removeAll { it.z < 250f }
            } else {
                // Game Over!
                isGameOver = true
                soundSynthesizer.playCrashSound()
                soundSynthesizer.stopMusic()
                hapticsManager.vibrateCrash()
            }
        }

        // Advance collectibles & handle pickups
        val updatedCollectibles = mutableListOf<Collectible>()
        var coinsGathered = 0
        var activatedJetpack = false
        var activatedMagnet = false
        var activatedShield = false
        var activatedMultiplier = false

        val magnetActive = newMagnetTime > 0f

        for (col in state.collectibles) {
            var colLane = col.lane
            var colZ = col.z - (currentSpeed * dt)
            var colY = col.yOffset

            // Magnet attraction
            if (magnetActive && col.type == CollectibleType.COIN && colZ in 0f..500f) {
                // Pull towards current player lane and Y
                val targetLaneVal = currentLaneX.toInt()
                if (colLane < targetLaneVal) colLane += 1
                if (colLane > targetLaneVal) colLane -= 1
                colY += (playerY - colY) * (10f * dt)
                colZ -= (currentSpeed * 0.4f * dt) // pull forward
            }

            // Pickup check
            val inPickupZ = colZ in (Z_PLAYER - 30f)..(Z_PLAYER + 30f)
            val inPickupLane = kotlin.math.abs(colLane - currentLaneX) < 0.65f
            val inPickupY = kotlin.math.abs(colY - playerY) < 55f

            if (inPickupZ && inPickupLane && inPickupY && !col.isCollected) {
                col.isCollected = true
                when (col.type) {
                    CollectibleType.COIN -> {
                        coinsGathered += 1
                        coinStreak++
                        coinStreakResetTimer = 2.0f
                        soundSynthesizer.playCoinSound(coinStreak)
                        hapticsManager.vibrateCoin()
                    }
                    CollectibleType.JETPACK -> {
                        activatedJetpack = true
                        soundSynthesizer.playPowerupSound()
                    }
                    CollectibleType.MAGNET -> {
                        activatedMagnet = true
                        soundSynthesizer.playPowerupSound()
                    }
                    CollectibleType.SHIELD -> {
                        activatedShield = true
                        soundSynthesizer.playPowerupSound()
                    }
                    CollectibleType.MULTIPLIER_2X -> {
                        activatedMultiplier = true
                        soundSynthesizer.playPowerupSound()
                    }
                }
            } else if (colZ > -30f && !col.isCollected) {
                updatedCollectibles.add(col.copy(lane = colLane, z = colZ, yOffset = colY))
            }
        }

        // Spawning new items
        spawnTimer += dt
        powerupTimer += dt

        if (spawnTimer >= (1.1f - (currentSpeed / MAX_SPEED) * 0.45f).coerceAtLeast(0.48f)) {
            spawnTimer = 0f
            spawnPattern(updatedObstacles, updatedCollectibles, isJetpackFlying)
        }

        // Particle updates
        val particles = updateParticles(state.particles, dt, currentLaneX, playerY, state.character.jetFlameColor)

        _gameState.value = state.copy(
            score = newScore,
            coinsCollected = state.coinsCollected + coinsGathered,
            distanceRunMeters = newDistance,
            currentBiome = currentBiome,
            playerLaneX = currentLaneX,
            playerJumpY = playerY,
            jumpVelocity = jumpVel,
            isJumping = isJumping,
            isSliding = isSliding,
            slideTimeRemaining = slideTime,
            obstacles = updatedObstacles,
            collectibles = updatedCollectibles,
            particles = particles,
            magnetActiveTime = if (activatedMagnet) state.magnetMaxDuration else newMagnetTime,
            jetpackActiveTime = if (activatedJetpack) state.jetpackMaxDuration else newJetpackTime,
            shieldActiveTime = if (activatedShield) state.shieldMaxDuration else finalShieldTime,
            multiplierActiveTime = if (activatedMultiplier) 12f else newMultiplierTime,
            invulnerableTimer = invulnerableTimer,
            isGameOver = isGameOver
        )
    }

    private fun spawnPattern(
        obstacles: MutableList<Obstacle>,
        collectibles: MutableList<Collectible>,
        isJetpackMode: Boolean
    ) {
        val spawnZ = Z_MAX
        if (isJetpackMode) {
            // In jetpack mode, spawn rich golden coin clusters in the sky!
            val lane = Random.nextInt(3) - 1
            for (i in 0..4) {
                collectibles.add(
                    Collectible(
                        id = nextObjectId++,
                        lane = lane,
                        z = spawnZ + (i * 50f),
                        yOffset = JETPACK_ALTITUDE,
                        type = CollectibleType.COIN
                    )
                )
            }
            return
        }

        // Decide pattern type:
        // 1. Single obstacle + coins on other lanes
        // 2. Jump obstacle with coin arc above it
        // 3. High laser barrier with coins on ground to slide through
        // 4. Double obstacle (leaving 1 escape lane)
        // 5. Power-up spawn
        val patternType = Random.nextInt(6)

        when (patternType) {
            0 -> {
                // Jumpable rock with coin arc
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = lane, z = spawnZ, type = ObstacleType.LOW_ROCK)
                )
                // Coin arc above
                for (step in -1..1) {
                    collectibles.add(
                        Collectible(
                            id = nextObjectId++,
                            lane = lane,
                            z = spawnZ + (step * 45f),
                            yOffset = 50f + (if (step == 0) 25f else 0f),
                            type = CollectibleType.COIN
                        )
                    )
                }
            }
            1 -> {
                // Slideable high laser barrier
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = lane, z = spawnZ, type = ObstacleType.HIGH_LASER)
                )
                // Coins under it to encourage sliding!
                collectibles.add(
                    Collectible(id = nextObjectId++, lane = lane, z = spawnZ, yOffset = 0f, type = CollectibleType.COIN)
                )
            }
            2 -> {
                // Tall obstacle in one lane, coin line in adjacent lane
                val blockedLane = Random.nextInt(3) - 1
                val type = if (Random.nextBoolean()) ObstacleType.TALL_CONTAINER else ObstacleType.HOVER_DRONE
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = blockedLane, z = spawnZ, type = type)
                )

                // Place coins in a clear lane
                val coinLane = if (blockedLane == 0) 1 else 0
                for (i in 0..2) {
                    collectibles.add(
                        Collectible(
                            id = nextObjectId++,
                            lane = coinLane,
                            z = spawnZ + (i * 45f),
                            yOffset = 0f,
                            type = CollectibleType.COIN
                        )
                    )
                }
            }
            3 -> {
                // Double obstacles (1 escape lane!)
                val openLane = Random.nextInt(3) - 1
                for (lane in -1..1) {
                    if (lane != openLane) {
                        val obsType = if (Random.nextBoolean()) ObstacleType.TALL_CONTAINER else ObstacleType.LOW_ROCK
                        obstacles.add(
                            Obstacle(id = nextObjectId++, lane = lane, z = spawnZ, type = obsType)
                        )
                    }
                }
                // Place rewarding coins in open lane
                collectibles.add(
                    Collectible(id = nextObjectId++, lane = openLane, z = spawnZ, yOffset = 0f, type = CollectibleType.COIN)
                )
            }
            4 -> {
                // Coin trail across lane
                val lane = Random.nextInt(3) - 1
                for (i in 0..4) {
                    collectibles.add(
                        Collectible(
                            id = nextObjectId++,
                            lane = lane,
                            z = spawnZ + (i * 40f),
                            yOffset = 0f,
                            type = CollectibleType.COIN
                        )
                    )
                }
            }
            5 -> {
                // Power-up chance
                val powerupLane = Random.nextInt(3) - 1
                val pType = when (Random.nextInt(4)) {
                    0 -> CollectibleType.JETPACK
                    1 -> CollectibleType.MAGNET
                    2 -> CollectibleType.SHIELD
                    else -> CollectibleType.MULTIPLIER_2X
                }
                collectibles.add(
                    Collectible(
                        id = nextObjectId++,
                        lane = powerupLane,
                        z = spawnZ,
                        yOffset = 15f,
                        type = pType
                    )
                )
            }
        }
    }

    private fun updateParticles(
        existing: List<Particle>,
        dt: Float,
        playerLaneX: Float,
        playerY: Float,
        flameColor: Color
    ): List<Particle> {
        val list = mutableListOf<Particle>()

        // Update existing
        for (p in existing) {
            p.life -= dt
            if (p.life > 0f) {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                list.add(p)
            }
        }

        // Spawn hoverboard thruster exhaust flame particles
        if (list.size < 40) {
            val spreadX = (Random.nextFloat() - 0.5f) * 12f
            val speedY = 50f + Random.nextFloat() * 80f
            val particleColor = if (Random.nextBoolean()) flameColor else Color(0xFFFFD600)
            list.add(
                Particle(
                    x = playerLaneX * 80f + spreadX,
                    y = playerY - 5f,
                    vx = (Random.nextFloat() - 0.5f) * 30f,
                    vy = -speedY,
                    color = particleColor,
                    alpha = 0.9f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    size = 5f + Random.nextFloat() * 6f
                )
            )
        }

        return list
    }
}

data class GameState(
    val isPlaying: Boolean = false,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val score: Int = 0,
    val coinsCollected: Int = 0,
    val distanceRunMeters: Float = 0f,
    val currentBiome: Biome = Biome.MOON_BASE,
    val character: CharacterInfo = ALL_CHARACTERS.first(),
    val targetLane: Int = 0,
    val playerLaneX: Float = 0f,
    val playerJumpY: Float = 0f,
    val jumpVelocity: Float = 0f,
    val isJumping: Boolean = false,
    val isSliding: Boolean = false,
    val slideTimeRemaining: Float = 0f,
    val obstacles: List<Obstacle> = emptyList(),
    val collectibles: List<Collectible> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val magnetActiveTime: Float = 0f,
    val jetpackActiveTime: Float = 0f,
    val shieldActiveTime: Float = 0f,
    val multiplierActiveTime: Float = 0f,
    val magnetMaxDuration: Float = 10f,
    val jetpackMaxDuration: Float = 8f,
    val shieldMaxDuration: Float = 15f,
    val baseMultiplierBonus: Float = 1f,
    val invulnerableTimer: Float = 0f
) {
    val isInvulnerable: Boolean
        get() = invulnerableTimer > 0f || jetpackActiveTime > 0f
}

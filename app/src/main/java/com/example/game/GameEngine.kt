package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.HapticsManager
import com.example.audio.SoundSynthesizer
import com.example.data.UserStatsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class GameEngine(
    private val soundSynthesizer: SoundSynthesizer,
    private val hapticsManager: HapticsManager
) {
    companion object {
        const val LANE_LEFT = -1
        const val LANE_CENTER = 0
        const val LANE_RIGHT = 1
        const val Z_MAX = 1400f
        const val Z_PLAYER = 60f
        const val BASE_SPEED = 280f
        const val MAX_SPEED = 640f
        const val GRAVITY = 1150f
        const val JUMP_FORCE = 540f
        const val SUPER_JUMP_FORCE = 740f
        const val SLIDE_TIME = 0.85f
        const val JETPACK_ALTITUDE = 180f
    }

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var nextObjectId = 1L
    private var spawnTimer = 0f
    private var coinStreak = 0
    private var coinStreakResetTimer = 0f
    private var warpGateSpawnedForScore = 0

    fun startNewGame(userStats: UserStatsEntity) {
        val selectedChar = ALL_CHARACTERS.find { it.id == userStats.selectedCharacterId } ?: ALL_CHARACTERS.first()

        val magnetDuration = 10f + (userStats.magnetLevel - 1) * 2.5f
        val jetpackDuration = 8f + (userStats.jetpackLevel - 1) * 2.0f
        val shieldDuration = 15f + (userStats.shieldLevel - 1) * 3.0f
        val multiplierBonus = 1 + (userStats.multiplierLevel - 1) * 0.25f

        _gameState.value = GameState(
            isPlaying = true,
            isGameOver = false,
            isPaused = false,
            character = selectedChar,
            currentBiome = Biome.MOON_BASE,
            magnetMaxDuration = magnetDuration,
            jetpackMaxDuration = jetpackDuration,
            shieldMaxDuration = shieldDuration,
            baseMultiplierBonus = multiplierBonus
        )

        spawnTimer = 0f
        coinStreak = 0
        warpGateSpawnedForScore = 0
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
        if (state.jetpackActiveTime > 0f) return

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

        val newJumpY = if (state.playerJumpY > 0f) 0f else state.playerJumpY
        val newVel = if (state.playerJumpY > 0f) -500f else 0f

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

    fun activateNitro() {
        val state = _gameState.value
        if (!state.isPlaying || state.isGameOver || state.isPaused) return
        if (state.nitroGauge >= 0.95f && state.nitroBoostTime <= 0f) {
            _gameState.value = state.copy(
                nitroBoostTime = 4.0f,
                nitroGauge = 0f
            )
            soundSynthesizer.playHyperRingSound()
            hapticsManager.vibrateWarp()
        }
    }

    fun update(deltaSeconds: Float) {
        val state = _gameState.value
        if (!state.isPlaying || state.isGameOver || state.isPaused) return

        val dt = deltaSeconds.coerceIn(0f, 0.05f)

        // Speed calculation (boosted during Nitro)
        val isNitro = state.nitroBoostTime > 0f
        val nitroSpeedBoost = if (isNitro) 1.55f else 1.0f
        val baseSpeed = (BASE_SPEED + (state.distanceRunMeters * 0.18f)).coerceAtMost(MAX_SPEED)
        val currentSpeed = baseSpeed * nitroSpeedBoost

        val distanceDelta = (currentSpeed * dt) / 10f
        val newDistance = state.distanceRunMeters + distanceDelta

        // Active timers
        val newMagnetTime = (state.magnetActiveTime - dt).coerceAtLeast(0f)
        val newJetpackTime = (state.jetpackActiveTime - dt).coerceAtLeast(0f)
        val newShieldTime = (state.shieldActiveTime - dt).coerceAtLeast(0f)
        val newMultiplierTime = (state.multiplierActiveTime - dt).coerceAtLeast(0f)
        val newNitroTime = (state.nitroBoostTime - dt).coerceAtLeast(0f)
        val newWarpTimer = (state.warpEffectTimer - dt).coerceAtLeast(0f)

        val activeMultiplier = (if (newMultiplierTime > 0f) 2f else 1f) * state.baseMultiplierBonus
        val scoreGain = (distanceDelta * 2.4f * activeMultiplier).toInt()
        val newScore = state.score + scoreGain

        // Score Target Biome Progression
        var currentBiome = state.currentBiome
        var warpAnnouncement = if (newWarpTimer > 0f) state.warpAnnouncement else null
        var triggeredWarpEffect = false

        // Check if player reached the score milestone for next biome
        val targetScore = currentBiome.targetScore
        if (newScore >= targetScore && warpGateSpawnedForScore < targetScore) {
            warpGateSpawnedForScore = targetScore
            // Switch to next biome!
            currentBiome = when (currentBiome) {
                Biome.MOON_BASE -> Biome.MARS_CANYONS
                Biome.MARS_CANYONS -> Biome.DEEP_GALAXY
                Biome.DEEP_GALAXY -> Biome.SATURN_RINGS
                Biome.SATURN_RINGS -> Biome.MOON_BASE
            }
            triggeredWarpEffect = true
            warpAnnouncement = "WARP JUMP! ENTERING ${currentBiome.title.uppercase()}"
            soundSynthesizer.playWarpSound()
            hapticsManager.vibrateWarp()
        }

        // Horizontal lane transition
        val currentLaneX = state.playerLaneX + (state.targetLane - state.playerLaneX) * (15f * dt).coerceAtMost(1f)

        // Jump / Jetpack / Gravity
        var playerY = state.playerJumpY
        var jumpVel = state.jumpVelocity
        var isJumping = state.isJumping

        if (newJetpackTime > 0f) {
            playerY += (JETPACK_ALTITUDE - playerY) * (8f * dt)
            jumpVel = 0f
            isJumping = false
        } else {
            if (playerY > 0f || jumpVel != 0f) {
                playerY += jumpVel * dt
                jumpVel -= GRAVITY * dt
                if (playerY <= 0f) {
                    playerY = 0f
                    jumpVel = 0f
                    isJumping = false
                }
            }
        }

        // Slide timer
        var slideTime = state.slideTimeRemaining
        var isSliding = state.isSliding
        if (slideTime > 0f) {
            slideTime -= dt
            if (slideTime <= 0f) {
                slideTime = 0f
                isSliding = false
            }
        }

        if (coinStreakResetTimer > 0f) {
            coinStreakResetTimer -= dt
            if (coinStreakResetTimer <= 0f) {
                coinStreak = 0
            }
        }

        // Obstacles collision & progress
        val updatedObstacles = mutableListOf<Obstacle>()
        var hitObstacle = false
        var hitObstacleType: ObstacleType? = null
        val isInvincible = state.isInvulnerable || isNitro

        for (obs in state.obstacles) {
            val newZ = obs.z - (currentSpeed * dt)

            // Warp gate trigger
            if (obs.type == ObstacleType.WARP_GATE && newZ in (Z_PLAYER - 30f)..(Z_PLAYER + 30f)) {
                // Surfed through Stargate!
                triggeredWarpEffect = true
                currentBiome = when (currentBiome) {
                    Biome.MOON_BASE -> Biome.MARS_CANYONS
                    Biome.MARS_CANYONS -> Biome.DEEP_GALAXY
                    Biome.DEEP_GALAXY -> Biome.SATURN_RINGS
                    Biome.SATURN_RINGS -> Biome.MOON_BASE
                }
                warpAnnouncement = "WARP JUMP! ENTERING ${currentBiome.title.uppercase()}"
                soundSynthesizer.playWarpSound()
                hapticsManager.vibrateWarp()
            }

            // Normal obstacle collision
            val inCollisionZ = newZ in (Z_PLAYER - 25f)..(Z_PLAYER + 25f)
            val inPlayerLane = kotlin.math.abs(obs.lane - currentLaneX) < 0.65f

            if (newJetpackTime <= 0f && inCollisionZ && inPlayerLane && !isInvincible && obs.type != ObstacleType.WARP_GATE) {
                var collisionAvoided = false
                when (obs.type) {
                    ObstacleType.LOW_ROCK -> {
                        if (playerY > 35f) collisionAvoided = true
                    }
                    ObstacleType.HIGH_LASER -> {
                        if (isSliding && playerY < 18f) collisionAvoided = true
                    }
                    ObstacleType.TALL_CONTAINER, ObstacleType.HOVER_DRONE -> {
                        collisionAvoided = false
                    }
                    ObstacleType.WARP_GATE -> collisionAvoided = true
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

        // Handle collision damage
        var finalShieldTime = newShieldTime
        var isGameOver = false
        var invulnerableTimer = (state.invulnerableTimer - dt).coerceAtLeast(0f)

        if (hitObstacle) {
            if (finalShieldTime > 0f) {
                finalShieldTime = 0f
                invulnerableTimer = 2.0f
                soundSynthesizer.playShieldDeflectSound()
                hapticsManager.vibrateCrash()
                updatedObstacles.removeAll { it.z < 260f }
            } else {
                isGameOver = true
                soundSynthesizer.playCrashSound()
                soundSynthesizer.stopMusic()
                hapticsManager.vibrateCrash()
            }
        }

        // Collectibles & Pickups
        val updatedCollectibles = mutableListOf<Collectible>()
        var coinsGathered = 0
        var activatedJetpack = false
        var activatedMagnet = false
        var activatedShield = false
        var activatedMultiplier = false
        var nitroGain = 0f

        val magnetActive = newMagnetTime > 0f || isNitro

        for (col in state.collectibles) {
            var colLane = col.lane
            var colZ = col.z - (currentSpeed * dt)
            var colY = col.yOffset

            // Magnet pull across lanes
            if (magnetActive && col.type == CollectibleType.COIN && colZ in 0f..550f) {
                val targetLaneVal = currentLaneX.toInt()
                if (colLane < targetLaneVal) colLane += 1
                if (colLane > targetLaneVal) colLane -= 1
                colY += (playerY - colY) * (12f * dt)
                colZ -= (currentSpeed * 0.45f * dt)
            }

            val inPickupZ = colZ in (Z_PLAYER - 35f)..(Z_PLAYER + 35f)
            val inPickupLane = kotlin.math.abs(colLane - currentLaneX) < 0.72f
            val inPickupY = kotlin.math.abs(colY - playerY) < 65f

            if (inPickupZ && inPickupLane && inPickupY && !col.isCollected) {
                col.isCollected = true
                when (col.type) {
                    CollectibleType.COIN -> {
                        coinsGathered += 1
                        coinStreak++
                        coinStreakResetTimer = 2.2f
                        nitroGain += 0.08f
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
                    CollectibleType.HYPER_RING -> {
                        // Launch super jump!
                        playerY = 10f
                        jumpVel = SUPER_JUMP_FORCE
                        isJumping = true
                        soundSynthesizer.playHyperRingSound()
                        hapticsManager.vibrateWarp()
                    }
                    CollectibleType.NITRO_CELL -> {
                        nitroGain += 0.5f
                        soundSynthesizer.playPowerupSound()
                    }
                }
            } else if (colZ > -30f && !col.isCollected) {
                updatedCollectibles.add(col.copy(lane = colLane, z = colZ, yOffset = colY))
            }
        }

        // Update nitro gauge
        val updatedNitroGauge = (state.nitroGauge + nitroGain).coerceIn(0f, 1f)

        // Spawning patterns
        spawnTimer += dt
        val spawnInterval = (1.15f - (currentSpeed / MAX_SPEED) * 0.45f).coerceAtLeast(0.45f)
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnPattern(updatedObstacles, updatedCollectibles, newJetpackTime > 0f)
        }

        // Particle updates
        val particles = updateParticles(
            state.particles,
            dt,
            currentLaneX,
            playerY,
            state.character.jetFlameColor,
            isNitro,
            newWarpTimer > 0f
        )

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
            nitroBoostTime = newNitroTime,
            nitroGauge = updatedNitroGauge,
            warpEffectTimer = if (triggeredWarpEffect) 1.5f else newWarpTimer,
            warpAnnouncement = warpAnnouncement,
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
            val lane = Random.nextInt(3) - 1
            for (i in 0..4) {
                collectibles.add(
                    Collectible(
                        id = nextObjectId++,
                        lane = lane,
                        z = spawnZ + (i * 55f),
                        yOffset = JETPACK_ALTITUDE,
                        type = CollectibleType.COIN
                    )
                )
            }
            return
        }

        // Pattern selection:
        // 0: Low rock with jump coin arc
        // 1: High laser with ground coins to slide under
        // 2: Tall container in one lane, hyper jump ring in open lane
        // 3: Double obstacles with open escape lane
        // 4: Hyper jump ring launching over two obstacles with coin arc
        // 5: Power-up or nitro cell
        val pattern = Random.nextInt(7)

        when (pattern) {
            0 -> {
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = lane, z = spawnZ, type = ObstacleType.LOW_ROCK)
                )
                // Coin arc
                for (step in -1..1) {
                    collectibles.add(
                        Collectible(
                            id = nextObjectId++,
                            lane = lane,
                            z = spawnZ + (step * 50f),
                            yOffset = 60f + (if (step == 0) 30f else 0f),
                            type = CollectibleType.COIN
                        )
                    )
                }
            }
            1 -> {
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = lane, z = spawnZ, type = ObstacleType.HIGH_LASER)
                )
                for (i in 0..2) {
                    collectibles.add(
                        Collectible(
                            id = nextObjectId++,
                            lane = lane,
                            z = spawnZ + (i * 45f),
                            yOffset = 0f,
                            type = CollectibleType.COIN
                        )
                    )
                }
            }
            2 -> {
                val blockedLane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = blockedLane, z = spawnZ, type = ObstacleType.TALL_CONTAINER)
                )
                val openLane = if (blockedLane == 0) 1 else 0
                // Spawn a Hyper Jump Ring in open lane!
                collectibles.add(
                    Collectible(
                        id = nextObjectId++,
                        lane = openLane,
                        z = spawnZ,
                        yOffset = 15f,
                        type = CollectibleType.HYPER_RING
                    )
                )
            }
            3 -> {
                // Double obstacles (1 escape lane)
                val openLane = Random.nextInt(3) - 1
                for (lane in -1..1) {
                    if (lane != openLane) {
                        val type = if (Random.nextBoolean()) ObstacleType.TALL_CONTAINER else ObstacleType.LOW_ROCK
                        obstacles.add(
                            Obstacle(id = nextObjectId++, lane = lane, z = spawnZ, type = type)
                        )
                    }
                }
                for (i in 0..2) {
                    collectibles.add(
                        Collectible(
                            id = nextObjectId++,
                            lane = openLane,
                            z = spawnZ + (i * 45f),
                            yOffset = 0f,
                            type = CollectibleType.COIN
                        )
                    )
                }
            }
            4 -> {
                // Hyper Ring in middle launching over rocks with high coins
                collectibles.add(
                    Collectible(
                        id = nextObjectId++,
                        lane = 0,
                        z = spawnZ,
                        yOffset = 15f,
                        type = CollectibleType.HYPER_RING
                    )
                )
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = 0, z = spawnZ + 120f, type = ObstacleType.LOW_ROCK)
                )
                for (i in 1..4) {
                    collectibles.add(
                        Collectible(
                            id = nextObjectId++,
                            lane = 0,
                            z = spawnZ + (i * 60f),
                            yOffset = 120f,
                            type = CollectibleType.COIN
                        )
                    )
                }
            }
            5 -> {
                // Flying drone obstacle
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(id = nextObjectId++, lane = lane, z = spawnZ, type = ObstacleType.HOVER_DRONE)
                )
                val altLane = if (lane == 0) -1 else 0
                collectibles.add(
                    Collectible(
                        id = nextObjectId++,
                        lane = altLane,
                        z = spawnZ,
                        yOffset = 0f,
                        type = CollectibleType.NITRO_CELL
                    )
                )
            }
            else -> {
                // Power-up
                val lane = Random.nextInt(3) - 1
                val pType = when (Random.nextInt(4)) {
                    0 -> CollectibleType.JETPACK
                    1 -> CollectibleType.MAGNET
                    2 -> CollectibleType.SHIELD
                    else -> CollectibleType.MULTIPLIER_2X
                }
                collectibles.add(
                    Collectible(
                        id = nextObjectId++,
                        lane = lane,
                        z = spawnZ,
                        yOffset = 20f,
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
        flameColor: Color,
        isNitro: Boolean,
        isWarp: Boolean
    ): List<Particle> {
        val list = mutableListOf<Particle>()

        for (p in existing) {
            p.life -= dt
            if (p.life > 0f) {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                list.add(p)
            }
        }

        // Hoverboard flame exhaust particles
        val maxParticles = if (isWarp || isNitro) 80 else 40
        if (list.size < maxParticles) {
            val color = if (isNitro) Color(0xFF00F0FF) else if (Random.nextBoolean()) flameColor else Color(0xFFFFD600)
            list.add(
                Particle(
                    x = playerLaneX * 120f + (Random.nextFloat() - 0.5f) * 20f,
                    y = playerY - 8f,
                    vx = (Random.nextFloat() - 0.5f) * 50f,
                    vy = -(80f + Random.nextFloat() * 100f),
                    color = color,
                    alpha = 0.95f,
                    life = 0.4f,
                    maxLife = 0.4f,
                    size = if (isNitro) 10f else 6f
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
    val nitroBoostTime: Float = 0f,
    val nitroGauge: Float = 0f,
    val warpEffectTimer: Float = 0f,
    val warpAnnouncement: String? = null,
    val magnetMaxDuration: Float = 10f,
    val jetpackMaxDuration: Float = 8f,
    val shieldMaxDuration: Float = 15f,
    val baseMultiplierBonus: Float = 1f,
    val invulnerableTimer: Float = 0f
) {
    val isInvulnerable: Boolean
        get() = invulnerableTimer > 0f || jetpackActiveTime > 0f || nitroBoostTime > 0f
}

package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.game.Biome
import com.example.game.Collectible
import com.example.game.CollectibleType
import com.example.game.GameEngine
import com.example.game.GameState
import com.example.game.Obstacle
import com.example.game.ObstacleType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    gameState: GameState,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Cache the 4 planetary biome wallpapers
    val moonBitmap = remember {
        BitmapFactory.decodeResource(context.resources, Biome.MOON_BASE.bgDrawableRes)
    }
    val marsBitmap = remember {
        BitmapFactory.decodeResource(context.resources, Biome.MARS_CANYONS.bgDrawableRes)
    }
    val galaxyBitmap = remember {
        BitmapFactory.decodeResource(context.resources, Biome.DEEP_GALAXY.bgDrawableRes)
    }
    val saturnBitmap = remember {
        BitmapFactory.decodeResource(context.resources, Biome.SATURN_RINGS.bgDrawableRes)
    }

    var totalDragX = remember { 0f }
    var totalDragY = remember { 0f }
    var dragTriggered = remember { false }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gameState.isPlaying) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                        dragTriggered = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!dragTriggered) {
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            val threshold = 30f
                            if (abs(totalDragX) > threshold || abs(totalDragY) > threshold) {
                                dragTriggered = true
                                if (abs(totalDragX) > abs(totalDragY)) {
                                    if (totalDragX > 0) onSwipeRight() else onSwipeLeft()
                                } else {
                                    if (totalDragY < 0) onSwipeUp() else onSwipeDown()
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        dragTriggered = false
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Draw Space Environment Background FITTING THE ENTIRE SCREEN
        val currentBg = when (gameState.currentBiome) {
            Biome.MOON_BASE -> moonBitmap
            Biome.MARS_CANYONS -> marsBitmap
            Biome.DEEP_GALAXY -> galaxyBitmap
            Biome.SATURN_RINGS -> saturnBitmap
        }

        drawFullScreenBiomeBackground(currentBg, width, height)

        // 2. Camera perspective FOV parameters
        val horizonY = height * 0.28f
        val groundY = height * 0.88f
        val cameraZ = 300f
        val laneSpacing = width * 0.32f // 3 broad, comfortable lanes

        fun project(laneX: Float, z: Float, yElev: Float): Triple<Float, Float, Float> {
            val depth = (z + cameraZ).coerceAtLeast(10f)
            // Scale is ~1.0 at player position, scaling down gracefully to 0.16 at horizon
            val scale = (cameraZ / depth).coerceIn(0.08f, 2.2f)
            val px = (width / 2f) + (laneX * laneSpacing * scale)
            val py = horizonY + (groundY - horizonY) * scale - (yElev * scale * 2.4f)
            return Triple(px, py, scale)
        }

        // 3. Draw Elevated Holographic Space Highway
        drawElevatedSpaceHighway(
            width = width,
            height = height,
            horizonY = horizonY,
            groundY = groundY,
            distance = gameState.distanceRunMeters,
            biome = gameState.currentBiome
        )

        // 4. Collectibles & Obstacles sorted back-to-front
        val sceneElements = mutableListOf<Renderable>()
        for (col in gameState.collectibles) {
            sceneElements.add(Renderable.CollectibleItem(col))
        }
        for (obs in gameState.obstacles) {
            sceneElements.add(Renderable.ObstacleItem(obs))
        }
        sceneElements.sortByDescending { it.z }

        for (elem in sceneElements) {
            when (elem) {
                is Renderable.CollectibleItem -> {
                    if (elem.col.z in -30f..GameEngine.Z_MAX) {
                        val (px, py, scale) = project(elem.col.lane.toFloat(), elem.col.z, elem.col.yOffset)
                        drawCollectible(elem.col, px, py, scale, width, height, gameState.distanceRunMeters)
                    }
                }
                is Renderable.ObstacleItem -> {
                    if (elem.obs.z in -30f..GameEngine.Z_MAX) {
                        val (px, py, scale) = project(elem.obs.lane.toFloat(), elem.obs.z, 0f)
                        drawObstacle(elem.obs, px, py, scale, width, height, gameState.currentBiome)
                    }
                }
            }
        }

        // 5. Draw Particles
        for (p in gameState.particles) {
            val (px, py, _) = project(p.x / 120f, GameEngine.Z_PLAYER + 5f, p.y)
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(px, py)
            )
        }

        // 6. Draw Player Surfer & Cosmic Hoverboard with CLOSE-UP BOLD FOV
        val (surferX, surferY, surferScale) = project(
            laneX = gameState.playerLaneX,
            z = GameEngine.Z_PLAYER,
            yElev = gameState.playerJumpY
        )

        drawPlayerSurfer(
            x = surferX,
            y = surferY,
            scale = surferScale,
            width = width,
            height = height,
            gameState = gameState
        )

        // 7. Draw Hyperspace Warp Jump Effect (if transitioning biomes)
        if (gameState.warpEffectTimer > 0f) {
            drawWarpTunnelEffect(width, height, horizonY, gameState.warpEffectTimer)
        }
    }
}

private sealed class Renderable {
    abstract val z: Float
    data class CollectibleItem(val col: Collectible) : Renderable() {
        override val z: Float = col.z
    }
    data class ObstacleItem(val obs: Obstacle) : Renderable() {
        override val z: Float = obs.z
    }
}

/**
 * Fills the ENTIRE SCREEN with the planetary landscape artwork (aspect fill / center crop),
 * so there are ZERO black voids at the bottom or sides!
 */
private fun DrawScope.drawFullScreenBiomeBackground(
    bitmap: Bitmap?,
    width: Float,
    height: Float
) {
    if (bitmap != null) {
        val bmpWidth = bitmap.width.toFloat()
        val bmpHeight = bitmap.height.toFloat()

        val scale = maxOf(width / bmpWidth, height / bmpHeight)
        val scaledWidth = bmpWidth * scale
        val scaledHeight = bmpHeight * scale

        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f

        val dstRect = Rect(left.toInt(), top.toInt(), (left + scaledWidth).toInt(), (top + scaledHeight).toInt())
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)

        drawContext.canvas.nativeCanvas.drawBitmap(bitmap, srcRect, dstRect, null)
    }

    // Gentle atmospheric cosmic vignette
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x33050B1E),
                Color(0x11050B1E),
                Color(0x44050B1E),
                Color(0x88050B1E)
            ),
            startY = 0f,
            endY = height
        ),
        topLeft = Offset.Zero,
        size = Size(width, height)
    )
}

/**
 * Draws the elevated sci-fi maglev space highway suspended over the planet surface.
 * The deck has a translucent tinted glass surface so the stunning terrain below is fully visible!
 */
private fun DrawScope.drawElevatedSpaceHighway(
    width: Float,
    height: Float,
    horizonY: Float,
    groundY: Float,
    distance: Float,
    biome: Biome
) {
    val centerX = width / 2f
    val topTrackWidth = width * 0.16f
    val bottomTrackWidth = width * 0.98f

    // 1. Elevated track translucent surface (shows planet scenery underneath)
    val trackPath = Path().apply {
        moveTo(centerX - topTrackWidth / 2f, horizonY)
        lineTo(centerX + topTrackWidth / 2f, horizonY)
        lineTo(centerX + bottomTrackWidth / 2f, groundY + 140f)
        lineTo(centerX - bottomTrackWidth / 2f, groundY + 140f)
        close()
    }

    // Translucent deck with subtle sci-fi grid
    drawPath(
        path = trackPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xAA0B132B),
                Color(0xCC0E1A38),
                Color(0xDD0A1124)
            ),
            startY = horizonY,
            endY = groundY + 140f
        )
    )

    // 2. 4 Neon Guide Rails (forming 3 wide lanes)
    val railRatios = listOf(-1.5f, -0.5f, 0.5f, 1.5f)
    for (ratio in railRatios) {
        val topX = centerX + (ratio / 3f) * topTrackWidth
        val botX = centerX + (ratio / 3f) * bottomTrackWidth
        val isOuter = abs(ratio) > 1.0f

        drawLine(
            color = if (isOuter) biome.railColor else biome.gridColor.copy(alpha = 0.75f),
            start = Offset(topX, horizonY),
            end = Offset(botX, groundY + 140f),
            strokeWidth = if (isOuter) 7f else 3.5f
        )

        // Outer rail glow
        if (isOuter) {
            drawLine(
                color = biome.railColor.copy(alpha = 0.35f),
                start = Offset(topX, horizonY),
                end = Offset(botX, groundY + 140f),
                strokeWidth = 16f
            )
        }
    }

    // 3. Perspective Moving Energy Crossbars (creates intense forward speed)
    val speedOffset = (distance * 7f) % 90f
    for (i in 0..16) {
        val z = ((i * 70f) - speedOffset + 1120f) % 1120f
        if (z > 20f) {
            val scale = 300f / (z + 300f)
            val lineY = horizonY + (groundY - horizonY) * scale
            val leftX = centerX - (bottomTrackWidth / 2f) * scale
            val rightX = centerX + (bottomTrackWidth / 2f) * scale
            val alpha = (scale * 1.3f).coerceIn(0.08f, 0.9f)

            drawLine(
                color = biome.gridColor.copy(alpha = alpha),
                start = Offset(leftX, lineY),
                end = Offset(rightX, lineY),
                strokeWidth = (scale * 6f).coerceIn(1.5f, 5f)
            )
        }
    }
}

/**
 * Draws the player surfer and hoverboard with BOLD, prominent proportions (takes ~18% screen height).
 */
private fun DrawScope.drawPlayerSurfer(
    x: Float,
    y: Float,
    scale: Float,
    width: Float,
    height: Float,
    gameState: GameState
) {
    val char = gameState.character
    val isSliding = gameState.isSliding
    val isJetpack = gameState.jetpackActiveTime > 0f
    val isShieldActive = gameState.shieldActiveTime > 0f
    val isNitro = gameState.nitroBoostTime > 0f

    // Bold screen-relative dimensions
    val baseSurferHeight = height * 0.17f * scale
    val baseSurferWidth = width * 0.22f * scale
    val boardWidth = width * 0.28f * scale
    val boardHeight = height * 0.032f * scale

    val boardY = y - 6f * scale

    // 1. Ground Shadow (if jumping)
    if (gameState.playerJumpY > 0f && !isJetpack) {
        val groundShadowScale = (1f - (gameState.playerJumpY / 400f)).coerceIn(0.3f, 1f)
        val shadowW = boardWidth * groundShadowScale
        drawOval(
            color = Color(0x88000000),
            topLeft = Offset(x - shadowW / 2f, y + (gameState.playerJumpY * scale * 2.4f) - 10f),
            size = Size(shadowW, 16f * scale * groundShadowScale)
        )
    }

    // 2. Cosmic Hoverboard
    val boardPath = Path().apply {
        moveTo(x - boardWidth / 2f, boardY)
        lineTo(x - boardWidth * 0.38f, boardY - boardHeight)
        lineTo(x + boardWidth * 0.38f, boardY - boardHeight)
        lineTo(x + boardWidth / 2f, boardY)
        close()
    }

    // Hoverboard Body
    drawPath(
        path = boardPath,
        color = if (isNitro) Color(0xFF00F0FF) else char.boardColor
    )
    drawPath(
        path = boardPath,
        color = Color.White,
        style = Stroke(width = 3.5f * scale)
    )

    // Glowing Neon Thruster Nozzles & Exhaust Plumes
    val flameColor = if (isNitro) Color(0xFF00F0FF) else char.jetFlameColor
    val flameLength = (if (isNitro) 60f else 32f) * scale

    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(flameColor, Color(0xFFFFD600), Color.Transparent),
            startY = boardY,
            endY = boardY + flameLength
        ),
        topLeft = Offset(x - 24f * scale, boardY),
        size = Size(18f * scale, flameLength)
    )
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(flameColor, Color(0xFFFFD600), Color.Transparent),
            startY = boardY,
            endY = boardY + flameLength
        ),
        topLeft = Offset(x + 6f * scale, boardY),
        size = Size(18f * scale, flameLength)
    )

    // 3. Astronaut Character Figure
    val bodyHeight = if (isSliding) baseSurferHeight * 0.52f else baseSurferHeight
    val bodyTop = boardY - boardHeight - bodyHeight

    // Boots (if standing)
    if (!isSliding) {
        val bootW = baseSurferWidth * 0.28f
        val bootH = baseSurferHeight * 0.22f
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(x - baseSurferWidth * 0.34f, boardY - boardHeight - bootH),
            size = Size(bootW, bootH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale)
        )
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(x + baseSurferWidth * 0.06f, boardY - boardHeight - bootH),
            size = Size(bootW, bootH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale)
        )
    }

    // High-tech Torso Armor
    val torsoW = baseSurferWidth * 0.76f
    val torsoH = bodyHeight * 0.55f
    drawRoundRect(
        color = char.suitColor,
        topLeft = Offset(x - torsoW / 2f, bodyTop + bodyHeight * 0.32f),
        size = Size(torsoW, torsoH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
    )

    // Life support oxygen pack
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(x - torsoW * 0.42f, bodyTop + bodyHeight * 0.36f),
        size = Size(torsoW * 0.84f, torsoH * 0.85f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
    )

    // Helmet & Glowing Holographic Visor
    val helmetRadius = (baseSurferWidth * (if (isSliding) 0.34f else 0.42f))
    val helmetCenter = Offset(x, bodyTop + helmetRadius + 4f * scale)

    // Outer helmet
    drawCircle(
        color = char.suitColor,
        radius = helmetRadius,
        center = helmetCenter
    )
    // Glowing Visor
    drawCircle(
        color = char.visorColor,
        radius = helmetRadius * 0.74f,
        center = helmetCenter
    )
    // Visor specular reflection
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = helmetRadius * 0.30f,
        center = Offset(helmetCenter.x - 4f * scale, helmetCenter.y - 4f * scale)
    )

    // Jetpack Stratosphere Thrusters
    if (isJetpack) {
        val skyFlameH = 75f * scale
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFFFFD600), Color.Transparent),
                startY = boardY,
                endY = boardY + skyFlameH
            ),
            topLeft = Offset(x - 20f * scale, boardY),
            size = Size(40f * scale, skyFlameH)
        )
    }

    // Shield Forcefield Bubble
    if (isShieldActive) {
        val shieldR = baseSurferHeight * 0.68f
        drawCircle(
            color = Color(0x3300F0FF),
            radius = shieldR,
            center = Offset(x, bodyTop + bodyHeight * 0.5f)
        )
        drawCircle(
            color = Color(0xFF00F0FF),
            radius = shieldR,
            center = Offset(x, bodyTop + bodyHeight * 0.5f),
            style = Stroke(width = 4f * scale)
        )
    }
}

/**
 * Draws large, imposing, high-contrast obstacles that are easily readable and fun to dodge!
 */
private fun DrawScope.drawObstacle(
    obstacle: Obstacle,
    px: Float,
    py: Float,
    scale: Float,
    width: Float,
    height: Float,
    biome: Biome
) {
    when (obstacle.type) {
        ObstacleType.LOW_ROCK -> {
            // Chunky crystalline space boulder (Jumpable)
            val rockW = width * 0.28f * scale
            val rockH = height * 0.085f * scale

            val rockPath = Path().apply {
                moveTo(px - rockW / 2f, py)
                lineTo(px - rockW * 0.38f, py - rockH * 0.85f)
                lineTo(px - rockW * 0.08f, py - rockH)
                lineTo(px + rockW * 0.32f, py - rockH * 0.9f)
                lineTo(px + rockW / 2f, py)
                close()
            }
            drawPath(
                path = rockPath,
                brush = Brush.verticalGradient(
                    colors = listOf(biome.railColor, Color(0xFF1E293B), Color(0xFF0F172A)),
                    startY = py - rockH,
                    endY = py
                )
            )
            drawPath(
                path = rockPath,
                color = biome.gridColor,
                style = Stroke(width = 4f * scale)
            )
            // Danger flashing beacon
            drawCircle(
                color = Color(0xFFFFD600),
                radius = 6f * scale,
                center = Offset(px, py - rockH * 0.65f)
            )
        }

        ObstacleType.HIGH_LASER -> {
            // High Laser Barrier Pylon (Slideable)
            val gateW = width * 0.32f * scale
            val gateH = height * 0.19f * scale
            val beamY = py - gateH * 0.72f
            val postW = 14f * scale

            // Left Post
            drawRect(
                color = Color(0xFF334155),
                topLeft = Offset(px - gateW / 2f, py - gateH),
                size = Size(postW, gateH)
            )
            // Right Post
            drawRect(
                color = Color(0xFF334155),
                topLeft = Offset(px + gateW / 2f - postW, py - gateH),
                size = Size(postW, gateH)
            )

            // High Plasma Beam
            drawLine(
                color = Color(0xFFFF0055),
                start = Offset(px - gateW / 2f, beamY),
                end = Offset(px + gateW / 2f, beamY),
                strokeWidth = 16f * scale
            )
            drawLine(
                color = Color.White,
                start = Offset(px - gateW / 2f, beamY),
                end = Offset(px + gateW / 2f, beamY),
                strokeWidth = 5f * scale
            )
            // Downward Chevron indicator
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(px - 10f * scale, beamY + 16f * scale),
                end = Offset(px, beamY + 28f * scale),
                strokeWidth = 4f * scale
            )
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(px + 10f * scale, beamY + 16f * scale),
                end = Offset(px, beamY + 28f * scale),
                strokeWidth = 4f * scale
            )
        }

        ObstacleType.TALL_CONTAINER -> {
            // Towering Freight Container Pod (Must change lanes)
            val boxW = width * 0.28f * scale
            val boxH = height * 0.21f * scale

            val boxPath = Path().apply {
                moveTo(px - boxW / 2f, py)
                lineTo(px - boxW / 2f, py - boxH)
                lineTo(px + boxW / 2f, py - boxH)
                lineTo(px + boxW / 2f, py)
                close()
            }
            drawPath(
                path = boxPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                    startY = py - boxH,
                    endY = py
                )
            )
            drawPath(
                path = boxPath,
                color = Color(0xFF00F0FF),
                style = Stroke(width = 4.5f * scale)
            )
            // Hazard Chevrons
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFFFB800), Color(0xFF0F172A), Color(0xFFFFB800))
                ),
                topLeft = Offset(px - boxW / 2f + 6f * scale, py - boxH * 0.55f),
                size = Size(boxW - 12f * scale, 22f * scale)
            )
        }

        ObstacleType.HOVER_DRONE -> {
            // Menacing Alien Security Drone (Must change lanes)
            val droneW = width * 0.26f * scale
            val droneH = height * 0.08f * scale
            val floatY = py - height * 0.09f * scale

            // Drone Hull
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(px - droneW / 2f, floatY - droneH / 2f),
                size = Size(droneW, droneH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * scale)
            )
            drawRoundRect(
                color = Color(0xFFEF4444),
                topLeft = Offset(px - droneW / 2f, floatY - droneH / 2f),
                size = Size(droneW, droneH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * scale),
                style = Stroke(width = 3.5f * scale)
            )
            // Scanning Eye
            drawCircle(
                color = Color(0xFFFF0033),
                radius = 12f * scale,
                center = Offset(px, floatY)
            )
        }

        ObstacleType.WARP_GATE -> {
            // Massive Stargate at horizon
            val gateRadius = width * 0.42f * scale
            drawCircle(
                color = Color(0x6600F0FF),
                radius = gateRadius,
                center = Offset(px, py - gateRadius)
            )
            drawCircle(
                color = Color(0xFF00F0FF),
                radius = gateRadius,
                center = Offset(px, py - gateRadius),
                style = Stroke(width = 8f * scale)
            )
        }
    }
}

/**
 * Draws large, juicy, spinning golden Stardust Coins & Power-Up Pods!
 */
private fun DrawScope.drawCollectible(
    collectible: Collectible,
    px: Float,
    py: Float,
    scale: Float,
    width: Float,
    height: Float,
    distance: Float
) {
    when (collectible.type) {
        CollectibleType.COIN -> {
            // Large golden spinning coin
            val spin = cos((distance * 0.16f) + collectible.id.toFloat())
            val coinRadius = width * 0.045f * scale
            val coinW = (coinRadius * abs(spin)).coerceAtLeast(5f * scale)
            val coinH = coinRadius
            val centerY = py - 20f * scale

            // Outer Aura
            drawCircle(
                color = Color(0x66FFD700),
                radius = coinRadius * 1.5f,
                center = Offset(px, centerY)
            )
            // Coin Disc
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFEA00), Color(0xFFFFB300), Color(0xFFFF8F00))
                ),
                topLeft = Offset(px - coinW, centerY - coinH),
                size = Size(coinW * 2f, coinH * 2f)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(px - coinW, centerY - coinH),
                size = Size(coinW * 2f, coinH * 2f),
                style = Stroke(width = 2.5f * scale)
            )
        }

        CollectibleType.HYPER_RING -> {
            // Quantum Launch Pad Ring
            val ringRadius = width * 0.09f * scale
            drawCircle(
                color = Color(0x5500F0FF),
                radius = ringRadius,
                center = Offset(px, py - 20f * scale)
            )
            drawCircle(
                color = Color(0xFF00F0FF),
                radius = ringRadius,
                center = Offset(px, py - 20f * scale),
                style = Stroke(width = 6f * scale)
            )
            // Inner boost chevron
            drawLine(
                color = Color.White,
                start = Offset(px - 14f * scale, py - 10f * scale),
                end = Offset(px, py - 28f * scale),
                strokeWidth = 4f * scale
            )
            drawLine(
                color = Color.White,
                start = Offset(px + 14f * scale, py - 10f * scale),
                end = Offset(px, py - 28f * scale),
                strokeWidth = 4f * scale
            )
        }

        CollectibleType.JETPACK -> {
            val size = width * 0.085f * scale
            drawRoundRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(px - size / 2f, py - size - 10f * scale),
                size = Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = size * 0.3f,
                center = Offset(px, py - size / 2f - 10f * scale)
            )
        }

        CollectibleType.MAGNET -> {
            val size = width * 0.085f * scale
            drawRoundRect(
                color = Color(0xFF9333EA),
                topLeft = Offset(px - size / 2f, py - size - 10f * scale),
                size = Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
            )
            drawArc(
                color = Color(0xFFF43F5E),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(px - size * 0.3f, py - size * 0.75f - 10f * scale),
                size = Size(size * 0.6f, size * 0.6f),
                style = Stroke(width = 5f * scale)
            )
        }

        CollectibleType.SHIELD -> {
            val size = width * 0.085f * scale
            drawRoundRect(
                color = Color(0xFF059669),
                topLeft = Offset(px - size / 2f, py - size - 10f * scale),
                size = Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
            )
            drawCircle(
                color = Color(0xFF34D399),
                radius = size * 0.32f,
                center = Offset(px, py - size / 2f - 10f * scale)
            )
        }

        CollectibleType.MULTIPLIER_2X -> {
            val size = width * 0.085f * scale
            drawRoundRect(
                color = Color(0xFFEA580C),
                topLeft = Offset(px - size / 2f, py - size - 10f * scale),
                size = Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
            )
            drawCircle(
                color = Color(0xFFFFD600),
                radius = size * 0.32f,
                center = Offset(px, py - size / 2f - 10f * scale)
            )
        }

        CollectibleType.NITRO_CELL -> {
            val size = width * 0.085f * scale
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(px - size / 2f, py - size - 10f * scale),
                size = Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
            )
            drawCircle(
                color = Color.White,
                radius = size * 0.3f,
                center = Offset(px, py - size / 2f - 10f * scale)
            )
        }
    }
}

/**
 * Draws cinematic Hyperspace Warp Jump streaks & radial energy burst when transitioning worlds!
 */
private fun DrawScope.drawWarpTunnelEffect(
    width: Float,
    height: Float,
    horizonY: Float,
    timer: Float
) {
    val centerX = width / 2f
    val centerY = horizonY

    // Radial warp speed lines
    for (i in 0..28) {
        val angle = (i * (360f / 28f)) * (Math.PI / 180.0)
        val innerR = 40f
        val outerR = width * 1.2f
        val startX = centerX + (cos(angle) * innerR).toFloat()
        val startY = centerY + (sin(angle) * innerR).toFloat()
        val endX = centerX + (cos(angle) * outerR).toFloat()
        val endY = centerY + (sin(angle) * outerR).toFloat()

        drawLine(
            color = if (i % 2 == 0) Color(0xFF00F0FF) else Color(0xFFEC4899),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = (4f + (i % 4) * 2f)
        )
    }

    // Flashing hyperspace core
    drawCircle(
        color = Color(0x66FFFFFF),
        radius = width * 0.35f,
        center = Offset(centerX, centerY)
    )
}

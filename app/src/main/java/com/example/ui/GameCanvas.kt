package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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

    // Cache background bitmaps
    val moonBitmap = remember {
        BitmapFactory.decodeResource(context.resources, Biome.MOON_BASE.bgDrawableRes)
    }
    val marsBitmap = remember {
        BitmapFactory.decodeResource(context.resources, Biome.MARS_CANYONS.bgDrawableRes)
    }
    val galaxyBitmap = remember {
        BitmapFactory.decodeResource(context.resources, Biome.DEEP_GALAXY.bgDrawableRes)
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

                            val threshold = 35f
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
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Draw Space Environment Background
        val currentBg = when (gameState.currentBiome) {
            Biome.MOON_BASE -> moonBitmap
            Biome.MARS_CANYONS -> marsBitmap
            Biome.DEEP_GALAXY -> galaxyBitmap
        }

        val horizonY = canvasHeight * 0.36f
        val groundY = canvasHeight * 0.88f

        drawBiomeBackground(currentBg, canvasWidth, canvasHeight, horizonY, gameState.currentBiome)

        // 2. Draw 3D Perspective Track
        drawSpaceHighway(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            horizonY = horizonY,
            groundY = groundY,
            distance = gameState.distanceRunMeters,
            biome = gameState.currentBiome
        )

        // 3. Draw Collectibles & Obstacles sorted back-to-front (depth sort)
        val cameraZ = 240f
        val laneSpacing = canvasWidth * 0.34f

        fun project(laneX: Float, z: Float, yElev: Float): Triple<Float, Float, Float> {
            val depth = (z + cameraZ).coerceAtLeast(10f)
            val scale = (cameraZ / depth).coerceIn(0.01f, 2.5f)
            val px = (canvasWidth / 2f) + (laneX * laneSpacing * scale)
            val py = horizonY + (groundY - horizonY) * scale - (yElev * scale * 2.2f)
            return Triple(px, py, scale)
        }

        // Merge and sort all dynamic scene elements by Z descending
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
                    val (px, py, scale) = project(elem.col.lane.toFloat(), elem.col.z, elem.col.yOffset)
                    if (elem.col.z in -20f..GameEngine.Z_MAX) {
                        drawCollectible(elem.col, px, py, scale, gameState.distanceRunMeters)
                    }
                }
                is Renderable.ObstacleItem -> {
                    val (px, py, scale) = project(elem.obs.lane.toFloat(), elem.obs.z, 0f)
                    if (elem.obs.z in -20f..GameEngine.Z_MAX) {
                        drawObstacle(elem.obs, px, py, scale, gameState.currentBiome)
                    }
                }
            }
        }

        // 4. Draw Particles (exhaust flame, stars, sparks)
        for (p in gameState.particles) {
            val (px, py, _) = project(p.x / 80f, GameEngine.Z_PLAYER + 5f, p.y)
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(px, py)
            )
        }

        // 5. Draw Player Surfer & Hoverboard
        val (surferX, surferY, surferScale) = project(
            laneX = gameState.playerLaneX,
            z = GameEngine.Z_PLAYER,
            yElev = gameState.playerJumpY
        )

        drawSurfer(
            x = surferX,
            y = surferY,
            scale = surferScale,
            gameState = gameState
        )
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

private fun DrawScope.drawBiomeBackground(
    bitmap: Bitmap?,
    width: Float,
    height: Float,
    horizonY: Float,
    biome: Biome
) {
    if (bitmap != null) {
        val srcRect = android.graphics.Rect(0, 0, bitmap.width, (bitmap.height * 0.65f).toInt())
        val dstRect = android.graphics.Rect(0, 0, width.toInt(), (horizonY * 1.3f).toInt())
        drawContext.canvas.nativeCanvas.drawBitmap(bitmap, srcRect, dstRect, null)
    }

    // Atmospheric space fog gradient above and below horizon
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                biome.fogColor.copy(alpha = 0.5f),
                biome.fogColor.copy(alpha = 0.95f),
                biome.fogColor
            ),
            startY = horizonY * 0.5f,
            endY = horizonY * 1.15f
        ),
        topLeft = Offset(0f, horizonY * 0.5f),
        size = Size(width, horizonY * 0.65f)
    )
}

private fun DrawScope.drawSpaceHighway(
    canvasWidth: Float,
    canvasHeight: Float,
    horizonY: Float,
    groundY: Float,
    distance: Float,
    biome: Biome
) {
    val centerX = canvasWidth / 2f
    val topTrackWidth = canvasWidth * 0.12f
    val bottomTrackWidth = canvasWidth * 0.98f

    // 1. Dark glowing road surface
    val trackPath = Path().apply {
        moveTo(centerX - topTrackWidth / 2f, horizonY)
        lineTo(centerX + topTrackWidth / 2f, horizonY)
        lineTo(centerX + bottomTrackWidth / 2f, groundY + 120f)
        lineTo(centerX - bottomTrackWidth / 2f, groundY + 120f)
        close()
    }

    drawPath(
        path = trackPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                biome.fogColor.copy(alpha = 0.9f),
                Color(0xFF0F172A),
                Color(0xFF090D16)
            ),
            startY = horizonY,
            endY = groundY
        )
    )

    // 2. Neon Lane Dividing Rails (4 rails creating 3 lanes)
    val railRatios = listOf(-1.5f, -0.5f, 0.5f, 1.5f)
    for (ratio in railRatios) {
        val topX = centerX + (ratio / 3f) * topTrackWidth
        val botX = centerX + (ratio / 3f) * bottomTrackWidth
        val isOuter = abs(ratio) > 1.0f

        drawLine(
            color = if (isOuter) biome.railColor else biome.gridColor.copy(alpha = 0.6f),
            start = Offset(topX, horizonY),
            end = Offset(botX, groundY + 120f),
            strokeWidth = if (isOuter) 5.5f else 2.5f
        )
    }

    // 3. Perspective moving grid crossbars (creates thrilling sense of forward velocity)
    val speedOffset = (distance * 6f) % 100f
    for (i in 0..16) {
        val z = ((i * 65f) - speedOffset + 1040f) % 1040f
        if (z > 20f) {
            val scale = 240f / (z + 240f)
            val lineY = horizonY + (groundY - horizonY) * scale
            val leftX = centerX - (bottomTrackWidth / 2f) * scale
            val rightX = centerX + (bottomTrackWidth / 2f) * scale
            val alpha = (scale * 1.2f).coerceIn(0.05f, 0.85f)

            drawLine(
                color = biome.gridColor.copy(alpha = alpha),
                start = Offset(leftX, lineY),
                end = Offset(rightX, lineY),
                strokeWidth = (scale * 6f).coerceIn(1.2f, 4f)
            )
        }
    }
}

private fun DrawScope.drawObstacle(
    obstacle: Obstacle,
    px: Float,
    py: Float,
    scale: Float,
    biome: Biome
) {
    when (obstacle.type) {
        ObstacleType.LOW_ROCK -> {
            // Jumpable crystalline space boulder
            val rockW = 85f * scale
            val rockH = 45f * scale
            val path = Path().apply {
                moveTo(px - rockW / 2f, py)
                lineTo(px - rockW * 0.35f, py - rockH * 0.9f)
                lineTo(px + rockW * 0.1f, py - rockH)
                lineTo(px + rockW * 0.45f, py - rockH * 0.7f)
                lineTo(px + rockW / 2f, py)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(biome.railColor, Color(0xFF1E293B)),
                    startY = py - rockH,
                    endY = py
                )
            )
            drawPath(
                path = path,
                color = biome.gridColor,
                style = Stroke(width = 2.5f * scale)
            )
            // Warning neon glow marker
            drawCircle(
                color = Color(0xFFFFD600),
                radius = 4f * scale,
                center = Offset(px, py - rockH * 0.6f)
            )
        }
        ObstacleType.HIGH_LASER -> {
            // Slideable High Plasma Barrier (open underneath)
            val gateW = 92f * scale
            val gateH = 110f * scale
            val beamY = py - gateH * 0.75f
            val postW = 8f * scale

            // Left Post
            drawRect(
                color = Color(0xFF475569),
                topLeft = Offset(px - gateW / 2f, py - gateH),
                size = Size(postW, gateH)
            )
            // Right Post
            drawRect(
                color = Color(0xFF475569),
                topLeft = Offset(px + gateW / 2f - postW, py - gateH),
                size = Size(postW, gateH)
            )
            // High Pulsing Laser Beam
            drawLine(
                color = Color(0xFFFF0055),
                start = Offset(px - gateW / 2f, beamY),
                end = Offset(px + gateW / 2f, beamY),
                strokeWidth = 10f * scale
            )
            drawLine(
                color = Color(0xFFFFAACC),
                start = Offset(px - gateW / 2f, beamY),
                end = Offset(px + gateW / 2f, beamY),
                strokeWidth = 3f * scale
            )
            // Down arrow sign showing "SLIDE UNDER"
            drawLine(
                color = Color(0xFFFFCC00),
                start = Offset(px, beamY + 12f * scale),
                end = Offset(px, beamY + 28f * scale),
                strokeWidth = 2.5f * scale
            )
        }
        ObstacleType.TALL_CONTAINER -> {
            // Full-height freight pod (must change lane)
            val boxW = 82f * scale
            val boxH = 120f * scale

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
                style = Stroke(width = 3f * scale)
            )
            // Hazard warning stripes
            val stripeH = 14f * scale
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFFB800), Color(0xFF1E293B), Color(0xFFFFB800))
                ),
                topLeft = Offset(px - boxW / 2f + 4f * scale, py - boxH * 0.55f),
                size = Size(boxW - 8f * scale, stripeH)
            )
        }
        ObstacleType.HOVER_DRONE -> {
            // Flying robotic drone
            val droneW = 75f * scale
            val droneH = 45f * scale
            val floatY = py - 60f * scale

            // Drone body
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(px - droneW / 2f, floatY - droneH / 2f),
                size = Size(droneW, droneH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
            )
            drawRoundRect(
                color = Color(0xFFEF4444),
                topLeft = Offset(px - droneW / 2f, floatY - droneH / 2f),
                size = Size(droneW, droneH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale),
                style = Stroke(width = 2.5f * scale)
            )
            // Red glowing scanner eye
            drawCircle(
                color = Color(0xFFFF0033),
                radius = 8f * scale,
                center = Offset(px, floatY)
            )
        }
    }
}

private fun DrawScope.drawCollectible(
    collectible: Collectible,
    px: Float,
    py: Float,
    scale: Float,
    distance: Float
) {
    when (collectible.type) {
        CollectibleType.COIN -> {
            // Rotating gold stardust coin
            val spin = cos((distance * 0.15f) + collectible.id.toFloat())
            val coinRadius = 18f * scale
            val coinW = (coinRadius * abs(spin)).coerceAtLeast(3f * scale)
            val coinH = coinRadius

            // Outer golden glow
            drawCircle(
                color = Color(0x66FFD700),
                radius = 24f * scale,
                center = Offset(px, py - 15f * scale)
            )
            // Coin ellipse
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFE57F), Color(0xFFFFC107), Color(0xFFFF8F00))
                ),
                topLeft = Offset(px - coinW, py - 15f * scale - coinH),
                size = Size(coinW * 2f, coinH * 2f)
            )
            drawOval(
                color = Color(0xFFFFF8E1),
                topLeft = Offset(px - coinW, py - 15f * scale - coinH),
                size = Size(coinW * 2f, coinH * 2f),
                style = Stroke(width = 2f * scale)
            )
        }
        CollectibleType.JETPACK -> {
            val boxSize = 36f * scale
            drawRoundRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(px - boxSize / 2f, py - 30f * scale),
                size = Size(boxSize, boxSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
            )
            // Jet thruster flames icon
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 9f * scale,
                center = Offset(px, py - 12f * scale)
            )
        }
        CollectibleType.MAGNET -> {
            val boxSize = 36f * scale
            drawRoundRect(
                color = Color(0xFF9333EA),
                topLeft = Offset(px - boxSize / 2f, py - 30f * scale),
                size = Size(boxSize, boxSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
            )
            // U-shape magnet symbol
            drawArc(
                color = Color(0xFFF43F5E),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(px - 10f * scale, py - 26f * scale),
                size = Size(20f * scale, 20f * scale),
                style = Stroke(width = 4f * scale)
            )
        }
        CollectibleType.SHIELD -> {
            val boxSize = 36f * scale
            drawRoundRect(
                color = Color(0xFF059669),
                topLeft = Offset(px - boxSize / 2f, py - 30f * scale),
                size = Size(boxSize, boxSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
            )
            // Shield emblem
            drawCircle(
                color = Color(0xFF34D399),
                radius = 10f * scale,
                center = Offset(px, py - 12f * scale)
            )
        }
        CollectibleType.MULTIPLIER_2X -> {
            val boxSize = 36f * scale
            drawRoundRect(
                color = Color(0xFFEA580C),
                topLeft = Offset(px - boxSize / 2f, py - 30f * scale),
                size = Size(boxSize, boxSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
            )
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 10f * scale,
                center = Offset(px, py - 12f * scale)
            )
        }
    }
}

private fun DrawScope.drawSurfer(
    x: Float,
    y: Float,
    scale: Float,
    gameState: GameState
) {
    val char = gameState.character
    val isSliding = gameState.isSliding
    val isJetpack = gameState.jetpackActiveTime > 0f
    val isShieldActive = gameState.shieldActiveTime > 0f

    // Surfer board dimensions
    val boardW = 68f * scale
    val boardH = 16f * scale

    // 1. Board Shadow on highway
    if (gameState.playerJumpY > 0f && !isJetpack) {
        val groundShadowScale = scale * (1f - (gameState.playerJumpY / 300f).coerceIn(0f, 0.6f))
        drawOval(
            color = Color(0x77000000),
            topLeft = Offset(x - (boardW * groundShadowScale / scale) / 2f, y + (gameState.playerJumpY * scale * 2.2f) - 6f),
            size = Size(boardW * groundShadowScale / scale, 12f * groundShadowScale)
        )
    }

    // 2. Cosmic Hoverboard
    val boardY = y - 4f * scale
    val boardPath = Path().apply {
        moveTo(x - boardW / 2f, boardY)
        lineTo(x - boardW * 0.35f, boardY - boardH)
        lineTo(x + boardW * 0.35f, boardY - boardH)
        lineTo(x + boardW / 2f, boardY)
        close()
    }
    drawPath(
        path = boardPath,
        color = char.boardColor
    )
    drawPath(
        path = boardPath,
        color = Color(0xFF00F0FF),
        style = Stroke(width = 2.5f * scale)
    )

    // Thruster exhaust glow under hoverboard
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(char.jetFlameColor, Color.Transparent),
            center = Offset(x, boardY + 6f * scale),
            radius = 26f * scale
        ),
        radius = 26f * scale,
        center = Offset(x, boardY + 6f * scale)
    )

    // 3. Surfer Character Body & Armor
    val bodyHeight = if (isSliding) 36f * scale else 72f * scale
    val bodyWidth = 38f * scale
    val bodyTop = boardY - boardH - bodyHeight

    // Legs / Crouch
    if (!isSliding) {
        // Left & Right boots
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(x - 14f * scale, boardY - boardH - 18f * scale),
            size = Size(10f * scale, 18f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
        )
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(x + 4f * scale, boardY - boardH - 18f * scale),
            size = Size(10f * scale, 18f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
        )
    }

    // Torso / Space Suit
    drawRoundRect(
        color = char.suitColor,
        topLeft = Offset(x - bodyWidth / 2f, bodyTop + 24f * scale),
        size = Size(bodyWidth, bodyHeight - 24f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
    )

    // Oxygen backpack / jet thruster harness
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(x - 16f * scale, bodyTop + 28f * scale),
        size = Size(32f * scale, (bodyHeight - 32f * scale).coerceAtLeast(10f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale)
    )

    // Helmet & Glowing Visor
    val helmetRadius = if (isSliding) 14f * scale else 19f * scale
    val helmetCenter = Offset(x, bodyTop + helmetRadius + 4f * scale)

    // Helmet shell
    drawCircle(
        color = char.suitColor,
        radius = helmetRadius,
        center = helmetCenter
    )
    // Visor reflective neon arc
    drawCircle(
        color = char.visorColor,
        radius = helmetRadius * 0.72f,
        center = helmetCenter
    )
    // Visor highlight reflection
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = helmetRadius * 0.28f,
        center = Offset(helmetCenter.x - 3f * scale, helmetCenter.y - 3f * scale)
    )

    // Jetpack Plumes if active
    if (isJetpack) {
        val plumeH = 45f * scale
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFFFFD600), Color.Transparent),
                startY = boardY,
                endY = boardY + plumeH
            ),
            topLeft = Offset(x - 12f * scale, boardY),
            size = Size(24f * scale, plumeH)
        )
    }

    // 4. Cosmic Energy Shield Bubble (if active)
    if (isShieldActive) {
        val shieldRadius = 55f * scale
        drawCircle(
            color = Color(0x3300F0FF),
            radius = shieldRadius,
            center = Offset(x, bodyTop + bodyHeight / 2f)
        )
        drawCircle(
            color = Color(0xCC00F0FF),
            radius = shieldRadius,
            center = Offset(x, bodyTop + bodyHeight / 2f),
            style = Stroke(width = 3f * scale)
        )
    }
}

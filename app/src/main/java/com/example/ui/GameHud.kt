package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameState

@Composable
fun GameHud(
    gameState: GameState,
    onPauseClick: () -> Unit,
    onNitroClick: () -> Unit,
    showOnScreenControls: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onJumpClick: () -> Unit,
    onSlideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // TOP HUD BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Biome & Distance Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier.border(1.dp, gameState.currentBiome.railColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Distance",
                            tint = gameState.currentBiome.gridColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${gameState.distanceRunMeters.toInt()} m",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Coins Gathered Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier.border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${gameState.coinsCollected}",
                            color = Color(0xFFFFE082),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Pause Button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xCC0F172A), CircleShape)
                        .border(1.dp, Color(0xFF475569), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score Banner & Score Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SCORE",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = String.format("%,d", gameState.score),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Score Target Progress Widget
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NEXT WARP: ${gameState.currentBiome.targetScore} PTS",
                        color = gameState.currentBiome.gridColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "To ${gameState.currentBiome.nextBiomeName}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val progressToWarp = (gameState.score.toFloat() / gameState.currentBiome.targetScore.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressToWarp },
                        modifier = Modifier
                            .width(130.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = gameState.currentBiome.gridColor,
                        trackColor = Color(0x661E293B)
                    )
                }
            }

            // Power-up active timers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (gameState.jetpackActiveTime > 0f) {
                    PowerupIndicator(
                        icon = Icons.Default.RocketLaunch,
                        title = "JETPACK",
                        progress = gameState.jetpackActiveTime / gameState.jetpackMaxDuration,
                        accentColor = Color(0xFF38BDF8)
                    )
                }
                if (gameState.magnetActiveTime > 0f) {
                    PowerupIndicator(
                        icon = Icons.Default.ElectricBolt,
                        title = "MAGNET",
                        progress = gameState.magnetActiveTime / gameState.magnetMaxDuration,
                        accentColor = Color(0xFFA855F7)
                    )
                }
                if (gameState.shieldActiveTime > 0f) {
                    PowerupIndicator(
                        icon = Icons.Default.Shield,
                        title = "SHIELD",
                        progress = gameState.shieldActiveTime / gameState.shieldMaxDuration,
                        accentColor = Color(0xFF34D399)
                    )
                }
                if (gameState.multiplierActiveTime > 0f) {
                    PowerupIndicator(
                        icon = Icons.Default.ElectricBolt,
                        title = "2X BOOST",
                        progress = gameState.multiplierActiveTime / 12f,
                        accentColor = Color(0xFFFF9800)
                    )
                }
                if (gameState.nitroBoostTime > 0f) {
                    PowerupIndicator(
                        icon = Icons.Default.Bolt,
                        title = "NITRO",
                        progress = gameState.nitroBoostTime / 4f,
                        accentColor = Color(0xFF00F0FF)
                    )
                }
            }
        }

        // WARP JUMP ANNOUNCEMENT BANNER
        AnimatedVisibility(
            visible = gameState.warpAnnouncement != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xEE0B132B),
                modifier = Modifier
                    .border(
                        2.dp,
                        Brush.horizontalGradient(listOf(Color(0xFF00F0FF), Color(0xFFEC4899), Color(0xFFFFD700))),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "★ HYPERSPACE JUMP ★",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = gameState.warpAnnouncement ?: "",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // NITRO BOOST ACTIVATOR BUTTON (Right edge)
        val isNitroReady = gameState.nitroGauge >= 0.95f || gameState.nitroBoostTime > 0f
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            NitroDriveButton(
                gauge = gameState.nitroGauge,
                isActive = gameState.nitroBoostTime > 0f,
                isReady = isNitroReady,
                onClick = onNitroClick
            )
        }

        // OPTIONAL ON-SCREEN CONTROLS
        AnimatedVisibility(
            visible = showOnScreenControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left & Right Lane Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ArcadeTouchButton(
                        icon = Icons.Default.KeyboardArrowLeft,
                        description = "Move Left",
                        onClick = onLeftClick,
                        tag = "btn_lane_left"
                    )
                    ArcadeTouchButton(
                        icon = Icons.Default.KeyboardArrowRight,
                        description = "Move Right",
                        onClick = onRightClick,
                        tag = "btn_lane_right"
                    )
                }

                // Jump & Slide Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ArcadeTouchButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        description = "Slide Crouch",
                        onClick = onSlideClick,
                        tag = "btn_slide",
                        accentColor = Color(0xFFEC4899)
                    )
                    ArcadeTouchButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        description = "Jump",
                        onClick = onJumpClick,
                        tag = "btn_jump",
                        accentColor = Color(0xFF00F0FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun NitroDriveButton(
    gauge: Float,
    isActive: Boolean,
    isReady: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isActive) Color(0xFF00F0FF) else if (isReady) Color(0xEE0F172A) else Color(0xAA0F172A),
        modifier = Modifier
            .size(54.dp)
            .border(
                width = if (isReady) 2.5.dp else 1.5.dp,
                color = if (isReady) Color(0xFF00F0FF) else Color(0xFF475569),
                shape = CircleShape
            )
            .clickable(enabled = isReady, onClick = onClick)
            .testTag("nitro_button")
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Nitro Boost",
                tint = if (isActive) Color(0xFF0F172A) else if (isReady) Color(0xFF00F0FF) else Color(0xFF64748B),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isActive) "WARP" else "${(gauge * 100).toInt()}%",
                color = if (isActive) Color(0xFF0F172A) else if (isReady) Color(0xFF00F0FF) else Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PowerupIndicator(
    icon: ImageVector,
    title: String,
    progress: Float,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xD90F172A),
        modifier = Modifier
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .width(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = Color(0xFF334155)
            )
        }
    }
}

@Composable
private fun ArcadeTouchButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String,
    accentColor: Color = Color(0xFF38BDF8)
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xD91E293B), Color(0xF20F172A))
                ),
                CircleShape
            )
            .border(2.dp, accentColor.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = accentColor,
            modifier = Modifier.size(32.dp)
        )
    }
}

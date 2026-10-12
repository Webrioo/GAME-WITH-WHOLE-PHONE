package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.UserStatsEntity
import com.example.game.ALL_CHARACTERS

@Composable
fun MainMenuScreen(
    userStats: UserStatsEntity,
    onStartGame: () -> Unit,
    onOpenHangar: () -> Unit,
    onOpenTechLab: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentChar = ALL_CHARACTERS.find { it.id == userStats.selectedCharacterId } ?: ALL_CHARACTERS.first()
    var showHowToPlay by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Space Nebula Background
        Image(
            painter = painterResource(id = R.drawable.bg_galaxy),
            contentDescription = "Cosmic Galaxy",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x990B0F2B),
                            Color(0x550B0F2B),
                            Color(0xE60B0F2B),
                            Color(0xFF0B0F2B)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Stardust coins, How To Play & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stardust Coin Wallet
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier.border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${userStats.coins}",
                            color = Color(0xFFFFE082),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showHowToPlay = true },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xCC0F172A), CircleShape)
                            .border(1.dp, Color(0xFF38BDF8), CircleShape)
                            .testTag("help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Controls",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xCC0F172A), CircleShape)
                            .border(1.dp, Color(0xFF475569), CircleShape)
                            .testTag("menu_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // CENTER LOGO & CHARACTER CARD
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Game Title
                Text(
                    text = "COSMIC",
                    color = Color(0xFF00F0FF),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "SURFER",
                    color = Color(0xFFEC4899),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "3D ENDLESS SPACE ODYSSEY",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Character Hologram Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0x99131B38),
                    modifier = Modifier
                        .size(width = 200.dp, height = 230.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                listOf(currentChar.visorColor, Color(0xFF00F0FF))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, currentChar.visorColor, RoundedCornerShape(18.dp))
                        ) {
                            Image(
                                painter = painterResource(id = currentChar.drawableRes),
                                contentDescription = currentChar.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentChar.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentChar.title,
                                color = currentChar.visorColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High score badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x800F172A),
                    modifier = Modifier.border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Record",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BEST RECORD: ${String.format("%,d", userStats.highScore)}",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BOTTOM ACTIONS & LAUNCH BUTTON
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Launch Run Button
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("play_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F0FF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Launch",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAUNCH MISSION",
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom feature tabs: Hangar, Tech Lab, Hall of Fame
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuNavButton(
                        title = "Surfers",
                        icon = Icons.Default.RocketLaunch,
                        color = Color(0xFF38BDF8),
                        onClick = onOpenHangar,
                        tag = "menu_hangar_button",
                        modifier = Modifier.weight(1f)
                    )
                    MenuNavButton(
                        title = "Tech Lab",
                        icon = Icons.Default.Science,
                        color = Color(0xFFA855F7),
                        onClick = onOpenTechLab,
                        tag = "menu_tech_button",
                        modifier = Modifier.weight(1f)
                    )
                    MenuNavButton(
                        title = "Records",
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFFFD700),
                        onClick = onOpenLeaderboard,
                        tag = "menu_leaderboard_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // How to play dialog
        if (showHowToPlay) {
            HowToPlayDialog(onDismiss = { showHowToPlay = false })
        }
    }
}

@Composable
private fun MenuNavButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .testTag(tag),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF20F172A),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF00F0FF), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "FLIGHT MANUAL",
                    color = Color(0xFF00F0FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                InstructionItem(
                    action = "Swipe Left / Right",
                    description = "Change track lanes to avoid obstacles and gather coins."
                )
                InstructionItem(
                    action = "Swipe Up",
                    description = "Jump over moon boulders and laser tripwires."
                )
                InstructionItem(
                    action = "Swipe Down",
                    description = "Slide and crouch under high plasma barrier beams."
                )
                InstructionItem(
                    action = "Power-Ups",
                    description = "Jetpack (soar in sky), Magnet (attract coins), Shield (survive 1 crash), 2X Boost."
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dismiss_help_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "READY FOR TAKEOFF",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionItem(action: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = action,
            color = Color(0xFFFFD700),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )
    }
}

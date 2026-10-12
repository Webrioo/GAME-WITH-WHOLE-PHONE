package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.GameState

@Composable
fun GameOverDialog(
    gameState: GameState,
    isNewHighScore: Boolean,
    onRestart: () -> Unit,
    onOpenHangar: () -> Unit,
    onOpenTechLab: () -> Unit
) {
    Dialog(onDismissRequest = { /* Force explicit button click */ }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF20F172A),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFEC4899), Color(0xFF00F0FF))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "MISSION TERMINATED",
                    color = Color(0xFFFF5252),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                if (isNewHighScore) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0x33FFD700),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "New Record",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEW ALL-TIME RECORD!",
                                color = Color(0xFFFFE082),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score Display
                Text(
                    text = "FINAL SCORE",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format("%,d", gameState.score),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox(
                        icon = Icons.Default.Navigation,
                        label = "Distance",
                        value = "${gameState.distanceRunMeters.toInt()}m",
                        color = gameState.currentBiome.gridColor
                    )
                    StatBox(
                        icon = Icons.Default.MonetizationOn,
                        label = "Stardust",
                        value = "+${gameState.coinsCollected}",
                        color = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Respawn / Run Again Button
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("restart_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F0FF)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Respawn",
                        tint = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESPAWN MISSION",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation row to Hangar or Tech Lab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenHangar,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hangar_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Hangar",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Surfers",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenTechLab,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tech_lab_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Tech Lab",
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tech Lab",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x801E293B),
        modifier = Modifier
            .width(120.dp)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}

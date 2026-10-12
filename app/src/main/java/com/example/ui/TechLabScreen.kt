package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserStatsEntity

@Composable
fun TechLabScreen(
    userStats: UserStatsEntity,
    onUpgrade: (techType: String, cost: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0B0F2B),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                            .testTag("tech_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "TECH LAB",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Balance
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
                            text = "${userStats.coins}",
                            color = Color(0xFFFFE082),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Upgrade your space survival tech. Increases power-up duration and boosts your cosmic scoring capabilities.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            item {
                TechUpgradeCard(
                    title = "Cosmic Jetpack",
                    description = "Ascend above all hazards into stratosphere golden coin lanes.",
                    level = userStats.jetpackLevel,
                    icon = Icons.Default.RocketLaunch,
                    accentColor = Color(0xFF38BDF8),
                    coins = userStats.coins,
                    tag = "jetpack",
                    onUpgrade = { cost -> onUpgrade("jetpack", cost) }
                )
            }

            item {
                TechUpgradeCard(
                    title = "Gravity Magnet",
                    description = "Draws all surrounding stardust coins into your path across lanes.",
                    level = userStats.magnetLevel,
                    icon = Icons.Default.ElectricBolt,
                    accentColor = Color(0xFFA855F7),
                    coins = userStats.coins,
                    tag = "magnet",
                    onUpgrade = { cost -> onUpgrade("magnet", cost) }
                )
            }

            item {
                TechUpgradeCard(
                    title = "Plasma Shield",
                    description = "Provides impact deflecting forcefield that absorbs collision shockwaves.",
                    level = userStats.shieldLevel,
                    icon = Icons.Default.Shield,
                    accentColor = Color(0xFF34D399),
                    coins = userStats.coins,
                    tag = "shield",
                    onUpgrade = { cost -> onUpgrade("shield", cost) }
                )
            }

            item {
                TechUpgradeCard(
                    title = "Score Amplifier",
                    description = "Enhances baseline run score multiplier throughout every mission.",
                    level = userStats.multiplierLevel,
                    icon = Icons.Default.Star,
                    accentColor = Color(0xFFFF9800),
                    coins = userStats.coins,
                    tag = "multiplier",
                    onUpgrade = { cost -> onUpgrade("multiplier", cost) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TechUpgradeCard(
    title: String,
    description: String,
    level: Int,
    icon: ImageVector,
    accentColor: Color,
    coins: Int,
    tag: String,
    onUpgrade: (Int) -> Unit
) {
    val isMax = level >= 5
    val cost = level * 100

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x80131B38),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x33475569), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMax) "MAX TIER" else "Tier $level of 5",
                            color = if (isMax) Color(0xFF00F0FF) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Level Pips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(width = 12.dp, height = 8.dp)
                                .background(
                                    if (i <= level) accentColor else Color(0xFF334155),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isMax) {
                Surface(
                    color = Color(0x3300F0FF),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "MAXIMUM POWER REACHED",
                        color = Color(0xFF00F0FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                Button(
                    onClick = { onUpgrade(cost) },
                    enabled = coins >= cost,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("upgrade_$tag")
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Cost",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPGRADE FOR $cost COINS",
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

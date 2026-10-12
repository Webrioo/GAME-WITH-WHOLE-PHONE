package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserStatsEntity
import com.example.game.ALL_CHARACTERS
import com.example.game.CharacterInfo

@Composable
fun HangarScreen(
    userStats: UserStatsEntity,
    onSelectCharacter: (String) -> Unit,
    onUnlockCharacter: (String, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val unlockedList = userStats.unlockedCharacters.split(",")

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
                            .testTag("hangar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SPACE HANGAR",
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
                    text = "Select your cosmic surfer. Each hero comes equipped with custom rocket hoverboards & tailored thruster trails.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(ALL_CHARACTERS, key = { it.id }) { character ->
                val isUnlocked = unlockedList.contains(character.id)
                val isSelected = userStats.selectedCharacterId == character.id

                CharacterCard(
                    character = character,
                    isUnlocked = isUnlocked,
                    isSelected = isSelected,
                    canAfford = userStats.coins >= character.cost,
                    onSelect = { onSelectCharacter(character.id) },
                    onUnlock = { onUnlockCharacter(character.id, character.cost) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CharacterCard(
    character: CharacterInfo,
    isUnlocked: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x80131B38),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00F0FF) else Color(0x33475569),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Character Avatar
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, character.visorColor, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = character.drawableRes),
                    contentDescription = character.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .background(Color(0xFF00F0FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details & Action
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = character.title,
                    color = character.visorColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isSelected) {
                    Surface(
                        color = Color(0x3300F0FF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "EQUIPPED",
                            color = Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else if (isUnlocked) {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp).testTag("select_${character.id}")
                    ) {
                        Text(
                            text = "EQUIP",
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onUnlock,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp).testTag("unlock_${character.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Cost",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${character.cost}",
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

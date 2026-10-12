package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuitToMenu: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF20F172A),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MISSION PAUSED",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME RUN",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_settings_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SETTINGS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onQuitToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Menu",
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ABORT TO HANGAR",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

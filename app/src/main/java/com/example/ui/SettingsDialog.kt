package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
fun SettingsDialog(
    isMusicEnabled: Boolean,
    isSfxEnabled: Boolean,
    isHapticsEnabled: Boolean,
    showOnScreenControls: Boolean,
    onToggleMusic: (Boolean) -> Unit,
    onToggleSfx: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleOnScreenControls: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(24.dp)
            ) {
                Text(
                    text = "SETTINGS & CONTROLS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                SettingsToggleRow(
                    title = "Cosmic Music Synthesizer",
                    subtitle = "Dynamic sci-fi synthwave arpeggio",
                    checked = isMusicEnabled,
                    onCheckedChange = onToggleMusic,
                    tag = "switch_music"
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "Sound Effects (SFX)",
                    subtitle = "Jumps, slides, coin chimes & crashes",
                    checked = isSfxEnabled,
                    onCheckedChange = onToggleSfx,
                    tag = "switch_sfx"
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "Haptic Vibration",
                    subtitle = "Tactile impact & boost vibrations",
                    checked = isHapticsEnabled,
                    onCheckedChange = onToggleHaptics,
                    tag = "switch_haptics"
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "On-Screen Arcade Buttons",
                    subtitle = "Touch buttons alongside swipe gestures",
                    checked = showOnScreenControls,
                    onCheckedChange = onToggleOnScreenControls,
                    tag = "switch_controls"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("close_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "DONE",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0F172A),
                checkedTrackColor = Color(0xFF00F0FF),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CosmicSurferApp(
    viewModel: GameViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val topRuns by viewModel.topRuns.collectAsStateWithLifecycle()

    val isMusicEnabled by viewModel.isMusicEnabled.collectAsStateWithLifecycle()
    val isSfxEnabled by viewModel.isSfxEnabled.collectAsStateWithLifecycle()
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsStateWithLifecycle()
    val showOnScreenControls by viewModel.showOnScreenControls.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val isNewHighScore by viewModel.isNewHighScoreAchieved.collectAsStateWithLifecycle()

    when (currentScreen) {
        ScreenState.MENU -> {
            MainMenuScreen(
                userStats = userStats,
                onStartGame = { viewModel.startGame() },
                onOpenHangar = { viewModel.navigateTo(ScreenState.HANGAR) },
                onOpenTechLab = { viewModel.navigateTo(ScreenState.TECH_LAB) },
                onOpenLeaderboard = { viewModel.navigateTo(ScreenState.LEADERBOARD) },
                onOpenSettings = { viewModel.openSettings() }
            )
        }
        ScreenState.GAME -> {
            BackHandler {
                if (gameState.isGameOver || gameState.isPaused) {
                    viewModel.navigateTo(ScreenState.MENU)
                } else {
                    viewModel.pauseGame()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                GameCanvas(
                    gameState = gameState,
                    onSwipeLeft = { viewModel.moveLeft() },
                    onSwipeRight = { viewModel.moveRight() },
                    onSwipeUp = { viewModel.jump() },
                    onSwipeDown = { viewModel.slide() }
                )

                GameHud(
                    gameState = gameState,
                    onPauseClick = { viewModel.pauseGame() },
                    showOnScreenControls = showOnScreenControls,
                    onLeftClick = { viewModel.moveLeft() },
                    onRightClick = { viewModel.moveRight() },
                    onJumpClick = { viewModel.jump() },
                    onSlideClick = { viewModel.slide() }
                )

                if (gameState.isPaused && !gameState.isGameOver) {
                    PauseDialog(
                        onResume = { viewModel.resumeGame() },
                        onOpenSettings = { viewModel.openSettings() },
                        onQuitToMenu = { viewModel.navigateTo(ScreenState.MENU) }
                    )
                }

                if (gameState.isGameOver) {
                    GameOverDialog(
                        gameState = gameState,
                        isNewHighScore = isNewHighScore,
                        onRestart = { viewModel.restartGame() },
                        onOpenHangar = { viewModel.navigateTo(ScreenState.HANGAR) },
                        onOpenTechLab = { viewModel.navigateTo(ScreenState.TECH_LAB) }
                    )
                }
            }
        }
        ScreenState.HANGAR -> {
            HangarScreen(
                userStats = userStats,
                onSelectCharacter = { viewModel.selectCharacter(it) },
                onUnlockCharacter = { id, cost -> viewModel.unlockCharacter(id, cost) },
                onBack = { viewModel.navigateTo(ScreenState.MENU) }
            )
        }
        ScreenState.TECH_LAB -> {
            TechLabScreen(
                userStats = userStats,
                onUpgrade = { type, cost -> viewModel.upgradeTech(type, cost) },
                onBack = { viewModel.navigateTo(ScreenState.MENU) }
            )
        }
        ScreenState.LEADERBOARD -> {
            LeaderboardScreen(
                records = topRuns,
                onBack = { viewModel.navigateTo(ScreenState.MENU) }
            )
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            isMusicEnabled = isMusicEnabled,
            isSfxEnabled = isSfxEnabled,
            isHapticsEnabled = isHapticsEnabled,
            showOnScreenControls = showOnScreenControls,
            onToggleMusic = { viewModel.toggleMusic(it) },
            onToggleSfx = { viewModel.toggleSfx(it) },
            onToggleHaptics = { viewModel.toggleHaptics(it) },
            onToggleOnScreenControls = { viewModel.toggleOnScreenControls(it) },
            onDismiss = { viewModel.closeSettings() }
        )
    }
}

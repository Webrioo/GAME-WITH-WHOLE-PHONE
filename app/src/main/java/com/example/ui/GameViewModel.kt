package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticsManager
import com.example.audio.SoundSynthesizer
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.RunRecordEntity
import com.example.data.UserStatsEntity
import com.example.game.GameEngine
import com.example.game.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenState {
    MENU,
    GAME,
    HANGAR,
    TECH_LAB,
    LEADERBOARD
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val soundSynthesizer = SoundSynthesizer()
    val hapticsManager = HapticsManager(application)
    val gameEngine = GameEngine(soundSynthesizer, hapticsManager)

    private val repository: GameRepository

    val userStats: StateFlow<UserStatsEntity>
    val topRuns: StateFlow<List<RunRecordEntity>>

    private val _currentScreen = MutableStateFlow(ScreenState.MENU)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _isMusicEnabled = MutableStateFlow(true)
    val isMusicEnabled: StateFlow<Boolean> = _isMusicEnabled.asStateFlow()

    private val _isSfxEnabled = MutableStateFlow(true)
    val isSfxEnabled: StateFlow<Boolean> = _isSfxEnabled.asStateFlow()

    private val _isHapticsEnabled = MutableStateFlow(true)
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val _showOnScreenControls = MutableStateFlow(false)
    val showOnScreenControls: StateFlow<Boolean> = _showOnScreenControls.asStateFlow()

    private val _isNewHighScoreAchieved = MutableStateFlow(false)
    val isNewHighScoreAchieved: StateFlow<Boolean> = _isNewHighScoreAchieved.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    val gameState: StateFlow<GameState> = gameEngine.gameState

    private var gameLoopJob: Job? = null
    private var hasSavedCurrentRun = false

    init {
        val db = GameDatabase.getInstance(application)
        repository = GameRepository(db.gameDao())

        userStats = repository.userStats.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStatsEntity()
        )

        topRuns = repository.topRuns.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun navigateTo(screen: ScreenState) {
        if (_currentScreen.value == ScreenState.GAME && screen != ScreenState.GAME) {
            stopGameLoop()
            soundSynthesizer.stopMusic()
        }
        _currentScreen.value = screen
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun toggleMusic(enabled: Boolean) {
        _isMusicEnabled.value = enabled
        soundSynthesizer.setMusicEnabled(enabled)
    }

    fun toggleSfx(enabled: Boolean) {
        _isSfxEnabled.value = enabled
        soundSynthesizer.setSfxEnabled(enabled)
    }

    fun toggleHaptics(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
        hapticsManager.setEnabled(enabled)
    }

    fun toggleOnScreenControls(enabled: Boolean) {
        _showOnScreenControls.value = enabled
    }

    fun startGame() {
        viewModelScope.launch {
            val stats = repository.getStats()
            gameEngine.startNewGame(stats)
            _isNewHighScoreAchieved.value = false
            hasSavedCurrentRun = false
            _currentScreen.value = ScreenState.GAME
            startGameLoop()
        }
    }

    fun restartGame() {
        startGame()
    }

    fun pauseGame() {
        gameEngine.pauseGame()
    }

    fun resumeGame() {
        gameEngine.resumeGame()
    }

    fun moveLeft() = gameEngine.moveLeft()
    fun moveRight() = gameEngine.moveRight()
    fun jump() = gameEngine.jump()
    fun slide() = gameEngine.slide()

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTimeNanos = System.nanoTime()
            while (isActive) {
                val nowNanos = System.nanoTime()
                val deltaSec = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTimeNanos = nowNanos

                gameEngine.update(deltaSec)

                val state = gameEngine.gameState.value
                if (state.isGameOver && !hasSavedCurrentRun) {
                    hasSavedCurrentRun = true
                    handleGameOver(state)
                }

                delay(16) // ~60 fps
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun handleGameOver(state: GameState) {
        viewModelScope.launch {
            val isRecord = repository.saveRun(
                score = state.score,
                distanceMeters = state.distanceRunMeters.toInt(),
                coinsCollected = state.coinsCollected,
                characterId = state.character.id,
                biomeName = state.currentBiome.title
            )
            _isNewHighScoreAchieved.value = isRecord
        }
    }

    fun selectCharacter(charId: String) {
        viewModelScope.launch {
            repository.selectCharacter(charId)
        }
    }

    fun unlockCharacter(charId: String, cost: Int) {
        viewModelScope.launch {
            val success = repository.unlockCharacter(charId, cost)
            if (success) {
                soundSynthesizer.playPowerupSound()
                hapticsManager.vibrateTap()
            }
        }
    }

    fun upgradeTech(techType: String, cost: Int) {
        viewModelScope.launch {
            val success = repository.upgradeTech(techType, cost)
            if (success) {
                soundSynthesizer.playPowerupSound()
                hapticsManager.vibrateTap()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        soundSynthesizer.stopMusic()
    }
}

package hu.gyerekbillentyuzet.feature.keypad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.gyerekbillentyuzet.app.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class KeypadUiState(
    val mode: String = "inner",
    val soundEnabled: Boolean = true,
    val character: String = "",
    val characterId: Long = 0,
    val parentMenu: Boolean = false,
    val exited: Boolean = false
)

class KeypadViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val _state = MutableStateFlow(KeypadUiState())
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()
    init {
        viewModelScope.launch {
            settings.mode.combine(settings.sound) { mode, sound -> mode to sound }
                .collect { (mode, sound) -> _state.update { it.copy(mode=mode, soundEnabled=sound) } }
        }
    }
    fun press(ch: String) { if (ch.isNotBlank()) _state.update { it.copy(character=ch.takeLast(1).uppercase(), characterId=it.characterId+1) } }
    fun setMode(mode: String) { viewModelScope.launch { settings.setMode(mode) }; _state.update { it.copy(mode=mode) } }
    fun setSound(enabled: Boolean) { viewModelScope.launch { settings.setSound(enabled) }; _state.update { it.copy(soundEnabled=enabled) } }
    fun showMenu() { _state.update { it.copy(parentMenu=true) } }
    fun closeMenu() { _state.update { it.copy(parentMenu=false) } }
    fun exit() { _state.update { it.copy(parentMenu=false, exited=true) } }
    fun restart() { _state.update { it.copy(exited=false) } }
}

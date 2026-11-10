package hu.gyerekbillentyuzet.feature.keypad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.gyerekbillentyuzet.app.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A képernyő közepén látszó betű (a webes `Shown`). */
data class Shown(val id: Long, val ch: String, val colorIdx: Int, val anim: Int)

/** Egy konfetti / csavarodó darab (a webes `Bit`). Távolságok dp-ben. */
data class Bit(val dx: Float, val dy: Float, val r: Float, val colorIdx: Int, val curl: Boolean)

data class KeypadUiState(
    val loaded: Boolean = false,
    val mode: String? = null, // "inner" | "native" | null (választó látszik)
    val soundEnabled: Boolean = true,
    val shown: Shown? = null,
    val bits: List<Bit> = emptyList(),
    val parentMenu: Boolean = false,
    val exited: Boolean = false,
)

class KeypadViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val _state = MutableStateFlow(KeypadUiState())
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()
    private var uid = 0L

    init {
        viewModelScope.launch {
            settings.mode.combine(settings.sound) { mode, sound -> mode to sound }
                .collect { (mode, sound) -> _state.update { it.copy(loaded = true, mode = mode, soundEnabled = sound) } }
        }
    }

    /** Ugyanaz a logika, mint a webes `press`: szín, animáció és konfetti a betű kódjából. */
    fun press(ch: String) {
        if (ch.isEmpty() || ch == " ") return
        val seed = ch.first().code
        val bits = List(22) { i ->
            val ang = Random.nextDouble() * PI * 2
            val dist = 120 + Random.nextDouble() * 260
            Bit(
                dx = (cos(ang) * dist).toFloat(),
                dy = (sin(ang) * dist).toFloat(),
                r = (Random.nextDouble() * 720 - 360).toFloat(),
                colorIdx = (i + seed) % 6,
                curl = i % 5 == 0,
            )
        }
        _state.update {
            uid += 1
            it.copy(shown = Shown(uid, ch.uppercase(), seed % 6, seed % 4), bits = bits)
        }
    }

    fun choose(mode: String) {
        viewModelScope.launch { settings.setMode(mode) }
        _state.update { it.copy(mode = mode) }
    }

    fun toggleMode() = choose(if (_state.value.mode == "inner") "native" else "inner")

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { settings.setSound(enabled) }
        _state.update { it.copy(soundEnabled = enabled) }
    }

    fun showMenu() { _state.update { it.copy(parentMenu = true) } }
    fun closeMenu() { _state.update { it.copy(parentMenu = false) } }
    fun exit() { _state.update { it.copy(parentMenu = false, exited = true) } }
    fun restart() { _state.update { it.copy(exited = false) } }
}

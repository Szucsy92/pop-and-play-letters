package hu.gyerekbillentyuzet.feature.keypad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.gyerekbillentyuzet.app.SettingsRepository
import hu.gyerekbillentyuzet.core.audio.NativeSoundEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Egy felugró nagy karakter a soron. */
data class Typed(val id: Long, val ch: String, val colorIdx: Int)

/** Konfetti-robbanás. kind: 0 = kis pukkanás, 1 = nagy (az 5. karakternél), 2 = ünneplés a szövegbe repüléskor. */
data class Burst(val id: Long, val kind: Int, val slot: Int, val total: Int, val seed: Int)

data class KeypadUiState(
    val loaded: Boolean = false,
    val mode: String? = null, // "inner" | "native" | null (választó látszik)
    val soundEnabled: Boolean = true,
    val pending: List<Typed> = emptyList(), // a soron felsorakozó (max 5) karakter
    val history: String = "",               // a már "begépelt", megtartott szöveg
    val flying: List<Typed> = emptyList(),  // épp a szövegbe repülő sor
    val flyId: Long = 0,
    val bump: Long = 0,
    val bursts: List<Burst> = emptyList(),
    val parentMenu: Boolean = false,
    val exited: Boolean = false,
)

class KeypadViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val sound = NativeSoundEngine()
    private val _state = MutableStateFlow(KeypadUiState())
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()
    private var uid = 0L
    private var flushJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = settings.history.first()
            _state.update { it.copy(history = saved) }
            settings.mode.combine(settings.sound) { mode, snd -> mode to snd }
                .collect { (mode, snd) -> _state.update { it.copy(loaded = true, mode = mode, soundEnabled = snd) } }
        }
    }

    /** A sor tele (5) -> a szövegbe repül. Ez a tiszta állapotátmenet. */
    private fun doFlush(s: KeypadUiState): KeypadUiState {
        if (s.pending.isEmpty()) return s
        val hist = (s.history + s.pending.joinToString("") { it.ch }).takeLast(MAX_HISTORY)
        uid += 1
        val burst = Burst(uid, 2, 0, 0, s.pending.first().colorIdx)
        return s.copy(
            history = hist,
            flying = s.pending,
            flyId = s.flyId + 1,
            bump = s.bump + 1,
            pending = emptyList(),
            bursts = (s.bursts + burst).takeLast(6),
        )
    }

    private fun persist() {
        val h = _state.value.history
        viewModelScope.launch { settings.setHistory(h) }
    }

    fun press(ch: String) {
        if (ch.isEmpty() || ch == " ") return
        val seed = ch.first().code
        var flushed = false
        _state.update { s ->
            var cur = s
            if (cur.pending.size >= ROW) {
                cur = doFlush(cur)
                flushed = true
            }
            uid += 1
            val typed = Typed(uid, ch.uppercase(), seed % 6)
            val pend = cur.pending + typed
            uid += 1
            val burst = Burst(uid, if (pend.size == ROW) 1 else 0, pend.size - 1, pend.size, seed)
            cur.copy(pending = pend, bursts = (cur.bursts + burst).takeLast(6))
        }
        if (_state.value.soundEnabled) {
            sound.play(seed)
            if (flushed) sound.chime()
        }
        if (flushed) persist()
        // Ha a sor megtelt, hamar a szövegbe repül; ha a gyerek megáll, később akkor is.
        flushJob?.cancel()
        val wait = if (_state.value.pending.size >= ROW) 900L else 3200L
        flushJob = viewModelScope.launch {
            delay(wait)
            flushNow()
        }
    }

    private fun flushNow() {
        if (_state.value.pending.isEmpty()) return
        _state.update { doFlush(it) }
        if (_state.value.soundEnabled) sound.chime()
        persist()
    }

    fun clearText() {
        flushJob?.cancel()
        _state.update { it.copy(history = "", pending = emptyList(), flying = emptyList()) }
        persist()
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

    override fun onCleared() {
        sound.shutdown()
        super.onCleared()
    }

    private companion object {
        const val ROW = 5
        const val MAX_HISTORY = 200
    }
}

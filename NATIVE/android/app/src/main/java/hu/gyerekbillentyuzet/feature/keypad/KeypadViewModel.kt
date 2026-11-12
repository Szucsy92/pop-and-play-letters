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
            settings.clearLegacyHistory() // nem emlékezünk: a szöveg csak a memóriában él
            settings.mode.combine(settings.sound) { mode, snd -> mode to snd }
                .collect { (mode, snd) -> _state.update { it.copy(loaded = true, mode = mode, soundEnabled = snd) } }
        }
    }

    /** A sor tele (5) -> a szövegbe repül. Ez a tiszta állapotátmenet. */
    private fun doFlush(s: KeypadUiState): KeypadUiState {
        if (s.pending.isEmpty()) return s
        val hist = trimHistory(s.history + s.pending.joinToString("") { it.ch })
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

    private fun trimHistory(h: String): String {
        if (h.length <= MAX_HISTORY) return h
        val cut = h.takeLast(MAX_HISTORY)
        return if (cut.isNotEmpty() && Character.isLowSurrogate(cut.first())) cut.drop(1) else cut
    }

    fun press(ch: String) {
        if (ch.isEmpty()) return
        if (ch == " ") { pressSpace(); return }
        val seed = ch.codePointAt(0)
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
        // Ha a sor megtelt (5), hamar a szövegbe repül; ha a gyerek 1, 2, 3 vagy 4 karakternél megáll, az is gyorsan követi.
        flushJob?.cancel()
        val wait = if (_state.value.pending.size >= ROW) FULL_FLUSH_MS else IDLE_FLUSH_MS
        flushJob = viewModelScope.launch {
            delay(wait)
            flushNow()
        }
    }

    private fun flushNow() {
        if (_state.value.pending.isEmpty()) return
        _state.update { doFlush(it) }
        if (_state.value.soundEnabled) sound.chime()
    }

    /** Azonnali "elküldés" (rakéta gomb): a soron lévő karakterek rögtön a szövegbe repülnek. */
    fun sendNow() {
        flushJob?.cancel()
        flushNow()
    }

    /** Szóköz: a sor beviszi magát, majd a szövegbe kerül egy szóköz is (nincs dupla / kezdő szóköz). */
    fun pressSpace() {
        flushJob?.cancel()
        var hadPending = false
        _state.update { s ->
            hadPending = s.pending.isNotEmpty()
            val cur = if (hadPending) doFlush(s) else s
            val h = cur.history
            val next = if (h.isEmpty() || h.last() == ' ') h else trimHistory("$h ")
            cur.copy(history = next, bump = cur.bump + 1)
        }
        if (_state.value.soundEnabled) { if (hadPending) sound.chime() else sound.play(3) }
    }

    /** A puffer törlése: nem marad semmi a memóriában sem (kilépéskor és a háttérbe kerüléskor). */
    fun wipe() {
        flushJob?.cancel()
        _state.update { it.copy(history = "", pending = emptyList(), flying = emptyList(), bursts = emptyList()) }
    }

    fun clearText() = wipe()

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
    fun exit() { wipe(); _state.update { it.copy(parentMenu = false, exited = true) } }
    fun restart() { _state.update { it.copy(exited = false) } }

    override fun onCleared() {
        sound.shutdown()
        super.onCleared()
    }

    private companion object {
        const val ROW = 5
        const val MAX_HISTORY = 4000 // görgethető, de a memóriában marad
        const val IDLE_FLUSH_MS = 800L // 1-4 karakternél megállva ennyi után bekerül
        const val FULL_FLUSH_MS = 600L // az 5. karakter után ennyivel
    }
}

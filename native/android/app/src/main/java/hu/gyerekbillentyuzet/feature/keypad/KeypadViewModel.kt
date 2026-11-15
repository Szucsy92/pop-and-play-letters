package hu.gyerekbillentyuzet.feature.keypad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.gyerekbillentyuzet.GlyphStyle
import hu.gyerekbillentyuzet.app.SettingsRepository
import hu.gyerekbillentyuzet.app.UpdateInfo
import hu.gyerekbillentyuzet.app.UpdateManager
import java.io.File
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

/** Konfetti-robbanás. kind: 0 = kis pukkanás, 1 = nagy (a sor utolsó karakterénél), 2 = ünneplés a szövegbe repüléskor. */
data class Burst(val id: Long, val kind: Int, val slot: Int, val total: Int, val seed: Int)

data class KeypadUiState(
    val loaded: Boolean = false,
    val mode: String? = null, // "inner" | "native" | null (választó látszik)
    val soundEnabled: Boolean = true,
    val glyphStyle: GlyphStyle = GlyphStyle.DEFAULT,
    val pending: List<Typed> = emptyList(), // a soron felsorakozó (max 10) karakter
    val history: String = "",               // a már "begépelt", megtartott szöveg
    val flying: List<Typed> = emptyList(),  // épp a szövegbe repülő sor
    val flyId: Long = 0,
    val bump: Long = 0,
    val bursts: List<Burst> = emptyList(),
    val parentMenu: Boolean = false,
    val update: UpdateInfo? = null,     // elérhető frissítés (null = nincs / elhalasztva)
    val updateStage: Int = 0,           // 0 = felajánlva, 1 = letöltés, 2 = telepítés indítva, 3 = hiba
    val updateProgress: Int = 0,
    val exited: Boolean = false,
)

class KeypadViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val sound = NativeSoundEngine()
    private val _state = MutableStateFlow(KeypadUiState())
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()
    private var uid = 0L
    private var flushJob: Job? = null
    private var apkFile: File? = null

    init {
        viewModelScope.launch {
            settings.clearLegacyHistory() // nem emlékezünk: a szöveg csak a memóriában él
            combine(settings.mode, settings.sound, settings.glyphStyle) { mode, snd, glyph -> Triple(mode, snd, GlyphStyle.from(glyph)) }
                .collect { (mode, snd, glyph) -> _state.update { it.copy(loaded = true, mode = mode, soundEnabled = snd, glyphStyle = glyph) } }
        }
    }

    init {
        // Indulás után egyszer megnézzük, van-e újabb kiadás a GitHubon (hiba esetén csendben kihagyjuk).
        viewModelScope.launch {
            delay(1500)
            UpdateManager.check(getApplication())?.let { info ->
                _state.update { it.copy(update = info, updateStage = 0, updateProgress = 0) }
            }
        }
    }

    /** "Később": a mostani indításra elrejti az ablakot. */
    fun dismissUpdate() { _state.update { it.copy(update = null) } }

    /** "Letöltés": letölti az APK-t, majd átadja a telepítésnek (onReady). Letöltött fájlnál rögtön újra telepít. */
    fun startUpdate(onReady: (File) -> Unit) {
        val info = _state.value.update ?: return
        if (_state.value.updateStage == 1) return
        apkFile?.takeIf { _state.value.updateStage == 2 && it.exists() }?.let { onReady(it); return }
        _state.update { it.copy(updateStage = 1, updateProgress = 0) }
        viewModelScope.launch {
            try {
                val f = UpdateManager.download(getApplication(), info) { p -> _state.update { s -> s.copy(updateProgress = p) } }
                apkFile = f
                _state.update { it.copy(updateStage = 2) }
                onReady(f)
            } catch (_: Exception) {
                _state.update { it.copy(updateStage = 3) }
            }
        }
    }

    /** A sor tele (10) -> a szövegbe repül. Ez a tiszta állapotátmenet. */
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
        // Ha a sor megtelt (10), hamar a szövegbe repül; ha a gyerek 10-nél kevesebb karakternél megáll, az is gyorsan követi.
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

    fun setGlyphStyle(style: GlyphStyle) {
        viewModelScope.launch { settings.setGlyphStyle(style.id) }
        _state.update { it.copy(glyphStyle = style) }
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
        const val ROW = 10
        const val MAX_HISTORY = 4000 // görgethető, de a memóriában marad
        const val IDLE_FLUSH_MS = 800L // 1-4 karakternél megállva ennyi után bekerül
        const val FULL_FLUSH_MS = 600L // az utolsó karakter után ennyivel
    }
}

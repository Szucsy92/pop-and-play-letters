package hu.gyerekbillentyuzet.feature.lock

class ChildLockController(private val onUnlock: () -> Unit) {
    private var taps = mutableListOf<Long>()
    fun tap(now: Long = android.os.SystemClock.elapsedRealtime()) {
        taps = taps.filter { now - it <= 2_000 }.toMutableList()
        taps.add(now)
        if (taps.size >= 5) { taps.clear(); onUnlock() }
    }
}

package lei.greg.utils

class Debouncer {
    private var last = 0L
    private val DEDUPE_WINDOW_MS = 500L

    fun canFire(): Boolean {
        val now = System.currentTimeMillis()
        if (now - last <= DEDUPE_WINDOW_MS) return false
        last = now
        return true
    }


}
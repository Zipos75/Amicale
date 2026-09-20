package com.example.timer

data class SubstitutionTimerState(
    val intervalMinutes: Int = 3, // Between 1 and 20 min
    val remainingSeconds: Int = 3 * 60,
    val isRunning: Boolean = false,
    val substitutionsCount: Int = 0,
    val syncWithMatchTimer: Boolean = true,
    val showBannerAlert: Boolean = false
) {
    val totalSeconds: Int
        get() = intervalMinutes * 60

    val progress: Float
        get() = if (totalSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedRemaining: String
        get() {
            val m = remainingSeconds / 60
            val s = remainingSeconds % 60
            return String.format("%02d:%02d", m, s)
        }
}

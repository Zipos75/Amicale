package com.example.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.model.MatchEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveTimerState(
    val matchId: Long = -1L,
    val isRunning: Boolean = false,
    val startedAtMs: Long = 0L,
    val elapsedBeforeMs: Long = 0L,
    val totalDurationMs: Long = 25 * 60 * 1000L,
    val currentPeriod: Int = 1,
    val isCountDown: Boolean = true,
    val timeoutRemainingSec: Int? = null // For 30s timeout
) {
    /**
     * Compute instantaneous elapsed time using System clock (never accumulating ticks!)
     */
    fun currentElapsedMs(): Long {
        return if (isRunning) {
            val now = System.currentTimeMillis()
            (elapsedBeforeMs + (now - startedAtMs)).coerceIn(0L, totalDurationMs)
        } else {
            elapsedBeforeMs.coerceIn(0L, totalDurationMs)
        }
    }

    fun displayTimeMs(): Long {
        val elapsed = currentElapsedMs()
        return if (isCountDown) {
            (totalDurationMs - elapsed).coerceAtLeast(0L)
        } else {
            elapsed
        }
    }

    val isFinished: Boolean
        get() = currentElapsedMs() >= totalDurationMs
}

object MatchTimerManager {

    private const val TAG = "MatchTimerManager"
    private const val ALARM_REQUEST_CODE = 9090

    private val _timerState = MutableStateFlow(LiveTimerState())
    val timerState = _timerState.asStateFlow()

    private val managerScope = CoroutineScope(Dispatchers.IO)

    /**
     * Call when a match is opened in the UI to sync timer state from DB.
     * Computes real time elapsed if it was left running while app was closed!
     */
    fun syncWithMatch(context: Context, match: MatchEntity) {
        val totalMs = match.periodDurationMinutes * 60 * 1000L
        val isRunning = match.isTimerRunning

        if (isRunning && match.startedAtMs != null) {
            val now = System.currentTimeMillis()
            val totalElapsed = match.elapsedBeforeMs + (now - match.startedAtMs)
            if (totalElapsed >= totalMs) {
                // Period finished while app was closed or in background
                _timerState.value = LiveTimerState(
                    matchId = match.id,
                    isRunning = false,
                    startedAtMs = 0L,
                    elapsedBeforeMs = totalMs,
                    totalDurationMs = totalMs,
                    currentPeriod = match.currentPeriod,
                    isCountDown = match.isCountDown
                )
                // Persist finished in DB
                managerScope.launch {
                    val db = AppDatabase.getDatabase(context)
                    db.matchDao().update(
                        match.copy(
                            isTimerRunning = false,
                            startedAtMs = null,
                            elapsedBeforeMs = totalMs
                        )
                    )
                }
                cancelAlarm(context)
                stopForegroundService(context)
                return
            }
        }

        _timerState.value = LiveTimerState(
            matchId = match.id,
            isRunning = isRunning,
            startedAtMs = match.startedAtMs ?: 0L,
            elapsedBeforeMs = match.elapsedBeforeMs,
            totalDurationMs = totalMs,
            currentPeriod = match.currentPeriod,
            isCountDown = match.isCountDown
        )
    }

    /**
     * Starts the chronometer.
     * Calculates startedAt = System.currentTimeMillis()
     * Schedules exact alarm via AlarmManager.setExactAndAllowWhileIdle
     * Starts Foreground Service with persistent notification.
     */
    fun startTimer(context: Context, match: MatchEntity) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val totalMs = match.periodDurationMinutes * 60 * 1000L
        val currentElapsed = match.elapsedBeforeMs.coerceIn(0L, totalMs)
        val remainingMs = totalMs - currentElapsed

        if (remainingMs <= 0) return

        val triggerAtMillis = now + remainingMs

        // 1. Schedule exact alarm with AlarmManager
        scheduleExactAlarm(appContext, match.id, match.currentPeriod, triggerAtMillis)

        // 2. Update In-Memory State
        _timerState.value = _timerState.value.copy(
            matchId = match.id,
            isRunning = true,
            startedAtMs = now,
            elapsedBeforeMs = currentElapsed,
            totalDurationMs = totalMs,
            currentPeriod = match.currentPeriod,
            isCountDown = match.isCountDown,
            timeoutRemainingSec = null
        )

        // 3. Persist in DB
        managerScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            db.matchDao().update(
                match.copy(
                    isTimerRunning = true,
                    startedAtMs = now,
                    elapsedBeforeMs = currentElapsed
                )
            )
        }

        // 4. Start Foreground Service
        val title = "${match.homeTeam} vs ${match.awayTeam}"
        val score = "${match.homeScore} - ${match.awayScore}"
        val serviceIntent = Intent(appContext, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START
            putExtra(TimerForegroundService.EXTRA_MATCH_ID, match.id)
            putExtra(TimerForegroundService.EXTRA_MATCH_TITLE, title)
            putExtra(TimerForegroundService.EXTRA_STARTED_AT, now)
            putExtra(TimerForegroundService.EXTRA_ELAPSED_BEFORE, currentElapsed)
            putExtra(TimerForegroundService.EXTRA_DURATION_MS, totalMs)
            putExtra(TimerForegroundService.EXTRA_IS_COUNTDOWN, match.isCountDown)
            putExtra(TimerForegroundService.EXTRA_PERIOD, match.currentPeriod)
            putExtra(TimerForegroundService.EXTRA_SCORE, score)
        }
        ContextCompat.startForegroundService(appContext, serviceIntent)
    }

    /**
     * Pauses chronometer.
     * Computes exact elapsedBefore = elapsedBefore + (now - startedAt).
     * Cancels scheduled AlarmManager alarm!
     */
    fun pauseTimer(context: Context, match: MatchEntity) {
        val appContext = context.applicationContext
        cancelAlarm(appContext)

        val now = System.currentTimeMillis()
        val totalMs = match.periodDurationMinutes * 60 * 1000L
        val additional = if (match.startedAtMs != null) now - match.startedAtMs else 0L
        val newElapsed = (match.elapsedBeforeMs + additional).coerceIn(0L, totalMs)

        _timerState.value = _timerState.value.copy(
            isRunning = false,
            startedAtMs = 0L,
            elapsedBeforeMs = newElapsed,
            totalDurationMs = totalMs
        )

        managerScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            db.matchDao().update(
                match.copy(
                    isTimerRunning = false,
                    startedAtMs = null,
                    elapsedBeforeMs = newElapsed
                )
            )
        }

        val serviceIntent = Intent(appContext, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_PAUSE
            putExtra(TimerForegroundService.EXTRA_ELAPSED_BEFORE, newElapsed)
        }
        appContext.startService(serviceIntent)
    }

    /**
     * Resets chronometer for current period.
     * Cancels scheduled alarm!
     */
    fun resetTimer(context: Context, match: MatchEntity) {
        val appContext = context.applicationContext
        cancelAlarm(appContext)

        val totalMs = match.periodDurationMinutes * 60 * 1000L
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            startedAtMs = 0L,
            elapsedBeforeMs = 0L,
            totalDurationMs = totalMs,
            timeoutRemainingSec = null
        )

        managerScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            db.matchDao().update(
                match.copy(
                    isTimerRunning = false,
                    startedAtMs = null,
                    elapsedBeforeMs = 0L
                )
            )
        }

        val serviceIntent = Intent(appContext, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_PAUSE
            putExtra(TimerForegroundService.EXTRA_ELAPSED_BEFORE, 0L)
        }
        appContext.startService(serviceIntent)
    }

    /**
     * Switches to 2nd half (deuxième mi-temps).
     * Resets timer to 0, cancels alarm, sets period = 2.
     */
    fun switchToSecondPeriod(context: Context, match: MatchEntity) {
        val appContext = context.applicationContext
        cancelAlarm(appContext)

        val totalMs = match.periodDurationMinutes * 60 * 1000L
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            startedAtMs = 0L,
            elapsedBeforeMs = 0L,
            totalDurationMs = totalMs,
            currentPeriod = 2,
            timeoutRemainingSec = null
        )

        managerScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            db.matchDao().update(
                match.copy(
                    currentPeriod = 2,
                    isTimerRunning = false,
                    startedAtMs = null,
                    elapsedBeforeMs = 0L
                )
            )
        }

        val serviceIntent = Intent(appContext, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_PAUSE
            putExtra(TimerForegroundService.EXTRA_PERIOD, 2)
            putExtra(TimerForegroundService.EXTRA_ELAPSED_BEFORE, 0L)
        }
        appContext.startService(serviceIntent)
    }

    /**
     * Category change resets chrono to 0 and cancels alarm.
     */
    fun onCategoryChanged(context: Context, match: MatchEntity, newCategory: String) {
        val appContext = context.applicationContext
        cancelAlarm(appContext)

        val newDurationMinutes = MatchEntity.defaultDurationForCategory(newCategory)
        val newTotalMs = newDurationMinutes * 60 * 1000L

        _timerState.value = _timerState.value.copy(
            isRunning = false,
            startedAtMs = 0L,
            elapsedBeforeMs = 0L,
            totalDurationMs = newTotalMs,
            timeoutRemainingSec = null
        )

        managerScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            db.matchDao().update(
                match.copy(
                    category = newCategory,
                    periodDurationMinutes = newDurationMinutes,
                    isTimerRunning = false,
                    startedAtMs = null,
                    elapsedBeforeMs = 0L
                )
            )
        }

        stopForegroundService(appContext)
    }

    /**
     * Match finish ends timer, cancels alarm and stops foreground service.
     */
    fun finishMatch(context: Context, match: MatchEntity) {
        val appContext = context.applicationContext
        cancelAlarm(appContext)

        _timerState.value = _timerState.value.copy(
            isRunning = false,
            startedAtMs = 0L,
            timeoutRemainingSec = null
        )

        managerScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            db.matchDao().update(
                match.copy(
                    isFinished = true,
                    isScoreEncoded = true,
                    isTimerRunning = false,
                    startedAtMs = null
                )
            )
        }

        stopForegroundService(appContext)
    }

    /**
     * Toggles countdown vs count-up.
     */
    fun toggleCountDown(context: Context, match: MatchEntity) {
        val appContext = context.applicationContext
        val newCountDown = !match.isCountDown
        _timerState.value = _timerState.value.copy(isCountDown = newCountDown)

        managerScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            db.matchDao().update(match.copy(isCountDown = newCountDown))
        }

        val serviceIntent = Intent(appContext, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_UPDATE
            putExtra(TimerForegroundService.EXTRA_IS_COUNTDOWN, newCountDown)
        }
        appContext.startService(serviceIntent)
    }

    /**
     * 30-second coach timeout:
     * Pauses the match chronometer immediately, cancels alarm, and starts 30s timeout countdown.
     */
    fun triggerCoachTimeout(context: Context, match: MatchEntity) {
        pauseTimer(context, match)
        _timerState.value = _timerState.value.copy(timeoutRemainingSec = 30)
    }

    fun updateTimeoutSec(sec: Int?) {
        _timerState.value = _timerState.value.copy(timeoutRemainingSec = sec)
    }

    fun onPeriodAlarmFired(context: Context, matchId: Long, period: Int) {
        val totalMs = _timerState.value.totalDurationMs
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            startedAtMs = 0L,
            elapsedBeforeMs = totalMs
        )

        managerScope.launch {
            val db = AppDatabase.getDatabase(context)
            val match = db.matchDao().getMatchById(matchId)
            if (match != null) {
                db.matchDao().update(
                    match.copy(
                        isTimerRunning = false,
                        startedAtMs = null,
                        elapsedBeforeMs = totalMs
                    )
                )
            }
        }
    }

    private fun scheduleExactAlarm(context: Context, matchId: Long, period: Int, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MATCH_PERIOD_END
            putExtra(AlarmReceiver.EXTRA_MATCH_ID, matchId)
            putExtra(AlarmReceiver.EXTRA_PERIOD, period)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.i(TAG, "Exact alarm scheduled for $triggerAtMillis (in ${(triggerAtMillis - System.currentTimeMillis()) / 1000}s)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for exact alarm, falling back to setAndAllowWhileIdle", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MATCH_PERIOD_END
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Scheduled alarm successfully canceled")
        }
    }

    private fun stopForegroundService(context: Context) {
        val serviceIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }
}

package com.example.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerForegroundService : Service {

    constructor() : super()

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var tickerJob: Job? = null

    private var currentMatchId: Long = -1L
    private var matchTitle: String = "Feuille de match"
    private var startedAtMs: Long = 0L
    private var elapsedBeforeMs: Long = 0L
    private var totalDurationMs: Long = 25 * 60 * 1000L
    private var isRunning: Boolean = false
    private var isCountDown: Boolean = true
    private var currentPeriod: Int = 1
    private var scoreText: String = "0 - 0"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentMatchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)
                matchTitle = intent.getStringExtra(EXTRA_MATCH_TITLE) ?: "Match en cours"
                startedAtMs = intent.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis())
                elapsedBeforeMs = intent.getLongExtra(EXTRA_ELAPSED_BEFORE, 0L)
                totalDurationMs = intent.getLongExtra(EXTRA_DURATION_MS, 25 * 60 * 1000L)
                isCountDown = intent.getBooleanExtra(EXTRA_IS_COUNTDOWN, true)
                currentPeriod = intent.getIntExtra(EXTRA_PERIOD, 1)
                scoreText = intent.getStringExtra(EXTRA_SCORE) ?: "0 - 0"
                isRunning = true

                startAsForeground()
                startTicker()
            }
            ACTION_UPDATE -> {
                matchTitle = intent.getStringExtra(EXTRA_MATCH_TITLE) ?: matchTitle
                scoreText = intent.getStringExtra(EXTRA_SCORE) ?: scoreText
                isCountDown = intent.getBooleanExtra(EXTRA_IS_COUNTDOWN, isCountDown)
                updateNotification()
            }
            ACTION_PAUSE -> {
                elapsedBeforeMs = intent.getLongExtra(EXTRA_ELAPSED_BEFORE, elapsedBeforeMs)
                isRunning = false
                tickerJob?.cancel()
                updateNotification()
            }
            ACTION_STOP -> {
                isRunning = false
                tickerJob?.cancel()
                stopForegroundCompat()
                stopSelf()
            }
            ACTION_PERIOD_ENDED -> {
                isRunning = false
                tickerJob?.cancel()
                showPeriodEndedNotification()
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && isRunning) {
                updateNotification()
                val currentElapsed = elapsedBeforeMs + (System.currentTimeMillis() - startedAtMs)
                if (currentElapsed >= totalDurationMs) {
                    isRunning = false
                    showPeriodEndedNotification()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showPeriodEndedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏱ Fin de la MT $currentPeriod ! ($scoreText)")
            .setContentText("Coup de sifflet final de la période — $matchTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsed = if (isRunning) {
            elapsedBeforeMs + (System.currentTimeMillis() - startedAtMs)
        } else {
            elapsedBeforeMs
        }.coerceIn(0L, totalDurationMs)

        val displayMs = if (isCountDown) {
            (totalDurationMs - elapsed).coerceAtLeast(0L)
        } else {
            elapsed
        }

        val totalSec = displayMs / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        val formattedTime = String.format("%02d:%02d", mins, secs)

        val periodLabel = "MT $currentPeriod"
        val statusLabel = if (isRunning) "En cours" else "En pause"
        val countMode = if (isCountDown) "restantes" else "écoulées"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("[$periodLabel] $scoreText • $formattedTime ($statusLabel)")
            .setContentText("$matchTitle — $formattedTime $countMode")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chronomètre de Match de Hockey",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification persistante du temps de match restant"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
    }

    companion object {
        const val CHANNEL_ID = "hockey_timer_channel"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START = "ACTION_START"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PERIOD_ENDED = "ACTION_PERIOD_ENDED"

        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_MATCH_TITLE = "extra_match_title"
        const val EXTRA_STARTED_AT = "extra_started_at"
        const val EXTRA_ELAPSED_BEFORE = "extra_elapsed_before"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_IS_COUNTDOWN = "extra_is_countdown"
        const val EXTRA_PERIOD = "extra_period"
        const val EXTRA_SCORE = "extra_score"
    }
}

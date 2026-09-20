package com.example.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val matchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)
        val period = intent.getIntExtra(EXTRA_PERIOD, 1)
        Log.i("AlarmReceiver", "Received period end alarm for match $matchId, period $period")

        // 1. Play buzzer/referee whistle on USAGE_ALARM stream with vibration
        AlarmSoundHelper.playPeriodEndAlarm(context)

        // 2. Notify Foreground Service and Timer Manager
        val updateIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_PERIOD_ENDED
            putExtra(EXTRA_MATCH_ID, matchId)
            putExtra(EXTRA_PERIOD, period)
        }
        context.startService(updateIntent)

        // 3. Inform TimerManager to update DB state
        MatchTimerManager.onPeriodAlarmFired(context, matchId, period)
    }

    companion object {
        const val ACTION_MATCH_PERIOD_END = "com.example.ACTION_MATCH_PERIOD_END"
        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_PERIOD = "extra_period"
    }
}

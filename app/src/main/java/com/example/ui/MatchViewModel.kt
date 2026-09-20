package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.CalendarImporter
import com.example.calendar.DeviceCalendarInfo
import com.example.calendar.ImportResult
import com.example.data.AppDatabase
import com.example.data.model.GoalEvent
import com.example.data.model.MatchEntity
import com.example.data.repository.AppSettings
import com.example.data.repository.MatchRepository
import com.example.data.repository.SettingsRepository
import com.example.timer.AlarmSoundHelper
import com.example.timer.LiveTimerState
import com.example.timer.MatchTimerManager
import com.example.timer.SubstitutionTimerState
import com.example.ui.theme.HockeyAccentDefault
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val matchRepo = MatchRepository(db)
    private val settingsRepo = SettingsRepository(application)

    val matches: StateFlow<List<MatchEntity>> = matchRepo.allMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _selectedMatchId = MutableStateFlow<Long?>(null)
    val selectedMatchId: StateFlow<Long?> = _selectedMatchId.asStateFlow()

    private val _selectedMatch = MutableStateFlow<MatchEntity?>(null)
    val selectedMatch: StateFlow<MatchEntity?> = _selectedMatch.asStateFlow()

    private val _matchGoals = MutableStateFlow<List<GoalEvent>>(emptyList())
    val matchGoals: StateFlow<List<GoalEvent>> = _matchGoals.asStateFlow()

    private val _opponentColorHex = MutableStateFlow<String?>(null)
    val opponentColorHex: StateFlow<String?> = _opponentColorHex.asStateFlow()

    val liveTimerState: StateFlow<LiveTimerState> = MatchTimerManager.timerState

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _distinctTeams = MutableStateFlow<List<String>>(emptyList())
    val distinctTeams: StateFlow<List<String>> = _distinctTeams.asStateFlow()

    private val _substitutionTimer = MutableStateFlow(SubstitutionTimerState())
    val substitutionTimer: StateFlow<SubstitutionTimerState> = _substitutionTimer.asStateFlow()

    private val _deviceCalendars = MutableStateFlow<List<DeviceCalendarInfo>>(emptyList())
    val deviceCalendars: StateFlow<List<DeviceCalendarInfo>> = _deviceCalendars.asStateFlow()

    private var goalsCollectionJob: Job? = null
    private var colorCollectionJob: Job? = null
    private var matchObservingJob: Job? = null
    private var timeoutTickerJob: Job? = null

    init {
        // Collect matches to auto-select latest active or upcoming match if none selected
        viewModelScope.launch {
            matches.collect { list ->
                if (list.isNotEmpty() && _selectedMatchId.value == null) {
                    val upcoming = list.firstOrNull { !it.isFinished } ?: list.last()
                    selectMatch(upcoming.id)
                }

                // Extract all distinct team names for settings picker
                val teams = mutableSetOf<String>()
                list.forEach {
                    teams.add(it.homeTeam)
                    teams.add(it.awayTeam)
                }
                _distinctTeams.value = teams.sorted()
            }
        }

        // Ticker for UI to refresh timer state every 200ms when running
        viewModelScope.launch {
            while (true) {
                delay(200)
                if (MatchTimerManager.timerState.value.isRunning) {
                    // Triggers state refresh
                    val current = MatchTimerManager.timerState.value
                    if (current.isFinished) {
                        // End of period reached
                        MatchTimerManager.onPeriodAlarmFired(
                            getApplication(),
                            current.matchId,
                            current.currentPeriod
                        )
                    }
                }
            }
        }

        // 1-second interval ticker for Player Substitutions Timer
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val subState = _substitutionTimer.value
                val isMatchRunning = MatchTimerManager.timerState.value.isRunning
                val isTicking = subState.isRunning || (subState.syncWithMatchTimer && isMatchRunning)

                if (isTicking && subState.remainingSeconds > 0) {
                    val nextSec = subState.remainingSeconds - 1
                    if (nextSec <= 0) {
                        // Sound substitution beep & vibrate!
                        AlarmSoundHelper.playSubstitutionBeep(getApplication())
                        // Do NOT rearm automatically: stop and wait for user to confirm substitution
                        _substitutionTimer.value = subState.copy(
                            remainingSeconds = 0,
                            isRunning = false,
                            showBannerAlert = true
                        )
                    } else {
                        _substitutionTimer.value = subState.copy(remainingSeconds = nextSec)
                    }
                }
            }
        }
    }

    fun selectMatch(matchId: Long) {
        _selectedMatchId.value = matchId

        matchObservingJob?.cancel()
        matchObservingJob = viewModelScope.launch {
            matchRepo.getMatchFlow(matchId).collect { match ->
                _selectedMatch.value = match
                if (match != null) {
                    MatchTimerManager.syncWithMatch(getApplication(), match)
                    observeOpponentColor(match.opponentTeam)
                }
            }
        }

        goalsCollectionJob?.cancel()
        goalsCollectionJob = viewModelScope.launch {
            matchRepo.getGoalsFlow(matchId).collect { goals ->
                _matchGoals.value = goals
            }
        }
    }

    private fun observeOpponentColor(opponentName: String) {
        colorCollectionJob?.cancel()
        colorCollectionJob = viewModelScope.launch {
            matchRepo.getOpponentColorHex(opponentName).collect { hex ->
                _opponentColorHex.value = hex
            }
        }
    }

    fun addManualMatch(
        dateStr: String,
        timeStr: String,
        myTeam: String,
        opponent: String,
        isHome: Boolean,
        category: String,
        location: String?
    ) {
        viewModelScope.launch {
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val timestamp = try {
                dateTimeFormat.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val homeTeam = if (isHome) myTeam else opponent
            val awayTeam = if (isHome) opponent else myTeam
            val durationMin = MatchEntity.defaultDurationForCategory(category)

            val match = MatchEntity(
                uid = "manual_${UUID.randomUUID()}",
                isFromCalendar = false,
                timestamp = timestamp,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                isHome = isHome,
                myTeam = myTeam,
                opponentTeam = opponent,
                category = category,
                location = location?.ifBlank { null },
                periodDurationMinutes = durationMin
            )

            val id = matchRepo.insertMatch(match)
            selectMatch(id)
        }
    }

    fun deleteManualMatch(matchId: Long) {
        viewModelScope.launch {
            matchRepo.deleteManualMatch(matchId)
            if (_selectedMatchId.value == matchId) {
                _selectedMatchId.value = null
                _selectedMatch.value = null
            }
        }
    }

    fun addGoal(isMyTeam: Boolean) {
        val match = _selectedMatch.value ?: return
        val currentPeriod = match.currentPeriod
        val elapsedSec = MatchTimerManager.timerState.value.currentElapsedMs() / 1000
        val minute = (elapsedSec / 60).toInt() + 1
        val teamName = if (isMyTeam) match.myTeam else match.opponentTeam

        viewModelScope.launch {
            matchRepo.addGoal(
                matchId = match.id,
                isMyTeam = isMyTeam,
                period = currentPeriod,
                minute = minute,
                teamName = teamName
            )
        }
    }

    fun removeGoal(isMyTeam: Boolean) {
        val match = _selectedMatch.value ?: return
        viewModelScope.launch {
            matchRepo.removeLastGoal(match.id, isMyTeam)
        }
    }

    fun changePenaltyCorners(isMyTeam: Boolean, delta: Int) {
        val match = _selectedMatch.value ?: return
        viewModelScope.launch {
            matchRepo.changePenaltyCorner(match.id, isMyTeam, delta)
        }
    }

    fun setOpponentColor(colorHex: String) {
        _opponentColorHex.value = colorHex
        val match = _selectedMatch.value ?: return
        viewModelScope.launch {
            matchRepo.setOpponentColor(match.opponentTeam, colorHex)
        }
    }

    fun changeMatchCategory(newCategory: String) {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.onCategoryChanged(getApplication(), match, newCategory)
    }

    fun startTimer() {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.startTimer(getApplication(), match)
    }

    fun pauseTimer() {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.pauseTimer(getApplication(), match)
    }

    fun resetTimer() {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.resetTimer(getApplication(), match)
    }

    fun switchToSecondPeriod() {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.switchToSecondPeriod(getApplication(), match)
    }

    fun toggleCountDown() {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.toggleCountDown(getApplication(), match)
    }

    fun triggerCoachTimeout() {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.triggerCoachTimeout(getApplication(), match)

        // Run 30s timeout ticker
        timeoutTickerJob?.cancel()
        timeoutTickerJob = viewModelScope.launch {
            for (sec in 30 downTo 0) {
                MatchTimerManager.updateTimeoutSec(sec)
                delay(1000)
            }
            MatchTimerManager.updateTimeoutSec(null)
        }
    }

    fun finishMatch() {
        val match = _selectedMatch.value ?: return
        MatchTimerManager.finishMatch(getApplication(), match)
    }

    // Settings actions
    fun importFromICal(url: String) {
        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "Téléchargement du calendrier en cours..."

            val childTeam = settings.value.childTeamName
            when (val result = CalendarImporter.importFromUrl(url, childTeam)) {
                is ImportResult.Success -> {
                    val count = matchRepo.mergeImportedMatches(result.matches)
                    settingsRepo.updateCalendarConfig(url, "ICAL_URL")
                    settingsRepo.recordImportSuccess(count)

                    if (result.suggestedChildTeam != null && childTeam.isBlank()) {
                        settingsRepo.updateChildTeamName(result.suggestedChildTeam)
                    }

                    _importStatusMessage.value = "✓ Import réussi : $count match(s) synchronisé(s)."
                }
                is ImportResult.Error -> {
                    _importStatusMessage.value = "✕ ${result.message}"
                }
            }
            _isImporting.value = false
        }
    }

    fun loadDeviceCalendars() {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            _deviceCalendars.value = CalendarImporter.getDeviceCalendars(resolver)
        }
    }

    fun importFromDeviceCalendar(calendarId: Long? = null) {
        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "Lecture de l'agenda Android..."

            val childTeam = settings.value.childTeamName
            val resolver = getApplication<Application>().contentResolver
            when (val result = CalendarImporter.importFromDeviceCalendar(resolver, calendarId, childTeam)) {
                is ImportResult.Success -> {
                    val count = matchRepo.mergeImportedMatches(result.matches)
                    settingsRepo.updateCalendarConfig("", "PHONE_CALENDAR")
                    settingsRepo.recordImportSuccess(count)

                    if (result.suggestedChildTeam != null && childTeam.isBlank()) {
                        settingsRepo.updateChildTeamName(result.suggestedChildTeam)
                    }

                    _importStatusMessage.value = "✓ $count match(s) importé(s) depuis l'agenda sélectionné."
                }
                is ImportResult.Error -> {
                    _importStatusMessage.value = "✕ ${result.message}"
                }
            }
            _isImporting.value = false
        }
    }

    fun deleteImportedMatches() {
        viewModelScope.launch {
            val count = matchRepo.deleteImportedMatches()
            _importStatusMessage.value = "✓ $count match(s) importé(s) effacé(s)."
            settingsRepo.recordImportSuccess(0)
        }
    }

    // Substitution / Rotation Timer Actions
    fun setSubstitutionInterval(minutes: Int) {
        val clamped = minutes.coerceIn(1, 20)
        val cur = _substitutionTimer.value
        _substitutionTimer.value = cur.copy(
            intervalMinutes = clamped,
            remainingSeconds = clamped * 60
        )
    }

    fun toggleSubstitutionTimer() {
        val cur = _substitutionTimer.value
        _substitutionTimer.value = cur.copy(isRunning = !cur.isRunning)
    }

    fun resetSubstitutionTimer() {
        val cur = _substitutionTimer.value
        _substitutionTimer.value = cur.copy(
            remainingSeconds = cur.intervalMinutes * 60,
            isRunning = false,
            showBannerAlert = false
        )
    }

    fun toggleSyncWithMatchTimer() {
        val cur = _substitutionTimer.value
        _substitutionTimer.value = cur.copy(syncWithMatchTimer = !cur.syncWithMatchTimer)
    }

    fun dismissSubstitutionAlert() {
        val cur = _substitutionTimer.value
        val isMatchRunning = MatchTimerManager.timerState.value.isRunning
        val shouldRun = if (cur.syncWithMatchTimer) isMatchRunning else true
        _substitutionTimer.value = cur.copy(
            showBannerAlert = false,
            remainingSeconds = cur.intervalMinutes * 60,
            substitutionsCount = cur.substitutionsCount + 1,
            isRunning = shouldRun
        )
    }

    fun setChildTeamName(name: String) {
        viewModelScope.launch {
            settingsRepo.updateChildTeamName(name)
        }
    }

    fun setClubHeader(name: String, subtitle: String) {
        viewModelScope.launch {
            settingsRepo.updateClubHeader(name, subtitle)
        }
    }

    fun setClubColor(colorHex: String) {
        viewModelScope.launch {
            settingsRepo.updateClubColor(colorHex)
        }
    }

    fun clearImportStatus() {
        _importStatusMessage.value = null
    }
}

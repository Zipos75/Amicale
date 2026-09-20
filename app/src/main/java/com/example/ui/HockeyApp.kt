package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AddManualMatchDialog
import com.example.ui.screens.BilanScreen
import com.example.ui.screens.MatchDetailScreen
import com.example.ui.screens.MatchListScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.HockeyAccentDefault
import com.example.ui.theme.HockeyBorder
import com.example.ui.theme.HockeyDarkBg
import com.example.ui.theme.HockeyDefeat
import com.example.ui.theme.HockeySurface
import com.example.ui.theme.HockeySurfaceVariant
import com.example.ui.theme.HockeyTextPrimary
import com.example.ui.theme.HockeyTextSecondary
import com.example.ui.theme.HockeyTheme
import com.example.ui.theme.HockeyWin

enum class HockeyAppTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    MATCHS("Matchs", Icons.Default.SportsHockey, "tab_matchs"),
    MATCH("Match", Icons.Default.Scoreboard, "tab_match"),
    BILAN("Bilan", Icons.Default.EmojiEvents, "tab_bilan"),
    REGLES("Règles", Icons.Default.MenuBook, "tab_rules"),
    REGLAGES("Réglages", Icons.Default.Settings, "tab_settings")
}

@Composable
fun HockeyApp(
    viewModel: MatchViewModel = viewModel()
) {
    val matches by viewModel.matches.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedMatch by viewModel.selectedMatch.collectAsState()
    val goals by viewModel.matchGoals.collectAsState()
    val opponentColorHex by viewModel.opponentColorHex.collectAsState()
    val timerState by viewModel.liveTimerState.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importStatusMessage by viewModel.importStatusMessage.collectAsState()
    val distinctTeams by viewModel.distinctTeams.collectAsState()
    val deviceCalendars by viewModel.deviceCalendars.collectAsState()
    val substitutionTimer by viewModel.substitutionTimer.collectAsState()

    var currentTab by remember { mutableStateOf(HockeyAppTab.MATCHS) }
    var showAddManualDialog by remember { mutableStateOf(false) }

    // Parse club custom accent color
    val clubAccentColor = remember(settings.clubColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.clubColorHex))
        } catch (_: Exception) {
            HockeyAccentDefault
        }
    }

    HockeyTheme(customAccentColor = clubAccentColor) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(HockeyDarkBg),
            topBar = {
                HockeyTopHeader(
                    clubName = settings.clubName,
                    clubSubtitle = settings.clubSubtitle,
                    clubColor = clubAccentColor,
                    timerRunning = timerState.isRunning,
                    currentTab = currentTab,
                    onReturnToMatch = {
                        if (selectedMatch != null) {
                            currentTab = HockeyAppTab.MATCH
                        }
                    }
                )
            },
            bottomBar = {
                HockeyBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        if (tab == HockeyAppTab.MATCH && selectedMatch == null && matches.isNotEmpty()) {
                            // Automatically select next match if none selected
                            val next = matches.firstOrNull { !it.isFinished } ?: matches.first()
                            viewModel.selectMatch(next.id)
                        }
                        currentTab = tab
                    },
                    hasActiveTimer = timerState.isRunning
                )
            },
            containerColor = HockeyDarkBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    HockeyAppTab.MATCHS -> {
                        MatchListScreen(
                            matches = matches,
                            onMatchClick = { matchId ->
                                viewModel.selectMatch(matchId)
                                currentTab = HockeyAppTab.MATCH
                            },
                            onNavigateToSettings = { currentTab = HockeyAppTab.REGLAGES },
                            onAddMatchClick = { showAddManualDialog = true },
                            onDeleteMatch = { viewModel.deleteManualMatch(it) }
                        )
                    }

                    HockeyAppTab.MATCH -> {
                        if (selectedMatch != null) {
                            MatchDetailScreen(
                                match = selectedMatch!!,
                                goals = goals,
                                timerState = timerState,
                                substitutionTimer = substitutionTimer,
                                opponentColorHex = opponentColorHex,
                                onBackClick = { currentTab = HockeyAppTab.MATCHS },
                                onAddGoal = { isMyTeam -> viewModel.addGoal(isMyTeam) },
                                onRemoveGoal = { isMyTeam -> viewModel.removeGoal(isMyTeam) },
                                onPenaltyCornerDelta = { isMyTeam, delta ->
                                    viewModel.changePenaltyCorners(isMyTeam, delta)
                                },
                                onStartTimer = { viewModel.startTimer() },
                                onPauseTimer = { viewModel.pauseTimer() },
                                onResetTimer = { viewModel.resetTimer() },
                                onSwitchToSecondPeriod = { viewModel.switchToSecondPeriod() },
                                onToggleCountDown = { viewModel.toggleCountDown() },
                                onTriggerTimeout = { viewModel.triggerCoachTimeout() },
                                onChangeCategory = { viewModel.changeMatchCategory(it) },
                                onSetOpponentColor = { viewModel.setOpponentColor(it) },
                                onFinishMatch = { viewModel.finishMatch() },
                                onSetSubstitutionInterval = { viewModel.setSubstitutionInterval(it) },
                                onToggleSubstitutionTimer = { viewModel.toggleSubstitutionTimer() },
                                onResetSubstitutionTimer = { viewModel.resetSubstitutionTimer() },
                                onToggleSyncSubstitution = { viewModel.toggleSyncWithMatchTimer() },
                                onDismissSubstitutionAlert = { viewModel.dismissSubstitutionAlert() }
                            )
                        } else {
                            // If no match selected, guide user
                            NoMatchSelectedView(
                                onNavigateToMatchs = { currentTab = HockeyAppTab.MATCHS },
                                onAddMatchClick = { showAddManualDialog = true }
                            )
                        }
                    }

                    HockeyAppTab.BILAN -> {
                        BilanScreen(
                            matches = matches,
                            onMatchClick = { matchId ->
                                viewModel.selectMatch(matchId)
                                currentTab = HockeyAppTab.MATCH
                            }
                        )
                    }

                    HockeyAppTab.REGLES -> {
                        RulesScreen()
                    }

                    HockeyAppTab.REGLAGES -> {
                        SettingsScreen(
                            settings = settings,
                            distinctTeams = distinctTeams,
                            isImporting = isImporting,
                            importStatusMessage = importStatusMessage,
                            deviceCalendars = deviceCalendars,
                            onImportIcal = { viewModel.importFromICal(it) },
                            onImportPhoneCalendar = { calendarId -> viewModel.importFromDeviceCalendar(calendarId) },
                            onLoadDeviceCalendars = { viewModel.loadDeviceCalendars() },
                            onDeleteImportedMatches = { viewModel.deleteImportedMatches() },
                            onSetChildTeam = { viewModel.setChildTeamName(it) },
                            onSetClubHeader = { name, subtitle -> viewModel.setClubHeader(name, subtitle) },
                            onSetClubColor = { viewModel.setClubColor(it) },
                            onClearImportStatus = { viewModel.clearImportStatus() }
                        )
                    }
                }
            }
        }

        // Add Manual Match Dialog
        if (showAddManualDialog) {
            AddManualMatchDialog(
                defaultMyTeam = settings.childTeamName,
                onDismiss = { showAddManualDialog = false },
                onAddMatch = { date, time, myTeam, opp, isHome, cat, loc ->
                    viewModel.addManualMatch(date, time, myTeam, opp, isHome, cat, loc)
                    showAddManualDialog = false
                    currentTab = HockeyAppTab.MATCH
                }
            )
        }
    }
}

@Composable
fun HockeyTopHeader(
    clubName: String,
    clubSubtitle: String,
    clubColor: Color,
    timerRunning: Boolean,
    currentTab: HockeyAppTab,
    onReturnToMatch: () -> Unit
) {
    Surface(
        color = HockeySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(com.example.R.drawable.ic_amicale_logo),
                        contentDescription = "Logo Amicale Anderlecht Avia",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, clubColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = clubName.ifBlank { "Mon Club" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = HockeyTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = clubSubtitle.ifBlank { "Hockey Club" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = clubColor,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }

                // If timer is running and parent is browsing another tab, show quick return pill!
                if (timerRunning && currentTab != HockeyAppTab.MATCH) {
                    Surface(
                        onClick = onReturnToMatch,
                        shape = RoundedCornerShape(16.dp),
                        color = HockeyWin.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyWin),
                        modifier = Modifier.testTag("running_timer_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(HockeyWin)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chrono actif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HockeyWin
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HockeyBottomNavigation(
    currentTab: HockeyAppTab,
    onTabSelected: (HockeyAppTab) -> Unit,
    hasActiveTimer: Boolean
) {
    NavigationBar(
        containerColor = HockeySurface,
        contentColor = HockeyTextPrimary,
        tonalElevation = 8.dp
    ) {
        HockeyAppTab.values().forEach { tab ->
            val isSelected = currentTab == tab

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == HockeyAppTab.MATCH && hasActiveTimer) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = HockeyWin,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        ) {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HockeyDarkBg,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = HockeyTextSecondary,
                    unselectedTextColor = HockeyTextSecondary
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

@Composable
fun NoMatchSelectedView(
    onNavigateToMatchs: () -> Unit,
    onAddMatchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = HockeySurfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Scoreboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aucun match sélectionné",
            style = MaterialTheme.typography.titleLarge,
            color = HockeyTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choisissez un match dans la liste ou créez-en un pour ouvrir la feuille de match en direct.",
            style = MaterialTheme.typography.bodyMedium,
            color = HockeyTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            onClick = onNavigateToMatchs,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Voir la liste des matchs",
                    color = HockeyDarkBg,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            onClick = onAddMatchClick,
            shape = RoundedCornerShape(12.dp),
            color = HockeySurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Créer un match hors calendrier",
                    color = HockeyTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

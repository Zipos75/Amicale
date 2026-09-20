package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalEvent
import com.example.data.model.MatchEntity
import com.example.timer.LiveTimerState
import com.example.timer.SubstitutionTimerState
import com.example.ui.theme.ClubColorPresets
import com.example.ui.theme.HockeyBorder
import com.example.ui.theme.HockeyDarkBg
import com.example.ui.theme.HockeyDefeat
import com.example.ui.theme.HockeyDraw
import com.example.ui.theme.HockeyPenaltyYellow
import com.example.ui.theme.HockeySurface
import com.example.ui.theme.HockeySurfaceVariant
import com.example.ui.theme.HockeyTextPrimary
import com.example.ui.theme.HockeyTextSecondary
import com.example.ui.theme.HockeyTurfGreen
import com.example.ui.theme.HockeyWin
import com.example.ui.theme.TabularScoreStyle
import com.example.ui.theme.TabularTimerStyle
import com.example.ui.theme.colorFromHex
import com.example.ui.theme.getContrastingTextColor
import com.example.ui.theme.toHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    match: MatchEntity,
    goals: List<GoalEvent>,
    timerState: LiveTimerState,
    substitutionTimer: SubstitutionTimerState = SubstitutionTimerState(),
    opponentColorHex: String?,
    onBackClick: () -> Unit,
    onAddGoal: (isMyTeam: Boolean) -> Unit,
    onRemoveGoal: (isMyTeam: Boolean) -> Unit,
    onPenaltyCornerDelta: (isMyTeam: Boolean, delta: Int) -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSwitchToSecondPeriod: () -> Unit,
    onToggleCountDown: () -> Unit,
    onTriggerTimeout: () -> Unit,
    onChangeCategory: (String) -> Unit,
    onSetOpponentColor: (String) -> Unit,
    onFinishMatch: () -> Unit,
    onSetSubstitutionInterval: (Int) -> Unit = {},
    onToggleSubstitutionTimer: () -> Unit = {},
    onResetSubstitutionTimer: () -> Unit = {},
    onToggleSyncSubstitution: () -> Unit = {},
    onDismissSubstitutionAlert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Keep screen on when chronometer is running and app in foreground!
    DisposableEffect(timerState.isRunning) {
        val window = (context as? Activity)?.window
        if (timerState.isRunning) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Dialog states
    var showColorPicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // Parse opponent custom color
    val opponentColor = remember(opponentColorHex) {
        colorFromHex(opponentColorHex, Color(0xFFD90429))
    }

    val onOpponentTextColor = remember(opponentColor) {
        getContrastingTextColor(opponentColor)
    }

    val myTeamColor = MaterialTheme.colorScheme.primary
    val onMyTeamTextColor = MaterialTheme.colorScheme.onPrimary

    // Time calculations
    val displayMs = timerState.displayTimeMs()
    val totalSec = displayMs / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HockeyDarkBg)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar: Back + Category selector + Opponent Color + Finish
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retour aux matchs",
                        tint = HockeyTextPrimary
                    )
                }

                // Category selector button
                Surface(
                    onClick = { showCategoryDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = HockeySurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder),
                    modifier = Modifier.testTag("category_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Catégorie : ${match.category}",
                            color = HockeyTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Opponent Color Picker Button
                IconButton(
                    onClick = { showColorPicker = true },
                    modifier = Modifier.testTag("opponent_color_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(opponentColor)
                            .border(2.dp, HockeyTextPrimary, CircleShape)
                    )
                }

                // Finish match button
                if (!match.isFinished) {
                    OutlinedButton(
                        onClick = { showFinishDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HockeyWin),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyWin),
                        modifier = Modifier.testTag("finish_match_button")
                    ) {
                        Text("Fin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Surface(
                        color = HockeyWin.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyWin)
                    ) {
                        Text(
                            text = "TERMINÉ",
                            color = HockeyWin,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Section 1: BIG SCORE & TEAMS with ONE-HAND ACCESSIBILITY
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Team 1: My Team (always on top or prominent)
                    TeamScoreRow(
                        teamName = match.myTeam,
                        score = if (match.isHome) match.homeScore else match.awayScore,
                        isHome = match.isHome,
                        tagLabel = if (match.isHome) "DOMICILE" else "EXTÉRIEUR",
                        teamColor = myTeamColor,
                        onTextColor = onMyTeamTextColor,
                        onAddGoal = { onAddGoal(true) },
                        onRemoveGoal = { onRemoveGoal(true) },
                        isMyTeam = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = HockeyBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Team 2: Opponent (tinted with persistent opponent color!)
                    TeamScoreRow(
                        teamName = match.opponentTeam,
                        score = if (match.isHome) match.awayScore else match.homeScore,
                        isHome = !match.isHome,
                        tagLabel = if (!match.isHome) "DOMICILE" else "EXTÉRIEUR",
                        teamColor = opponentColor,
                        onTextColor = onOpponentTextColor,
                        onAddGoal = { onAddGoal(false) },
                        onRemoveGoal = { onRemoveGoal(false) },
                        isMyTeam = false
                    )
                }
            }
        }

        // Section 2: CHRONOMÈTRE OFFICIEL (M3, High contrast, one hand touch targets)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(
                    if (timerState.isRunning) 2.dp else 1.dp,
                    if (timerState.isRunning) MaterialTheme.colorScheme.primary else HockeyBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Period Header & Countdown mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = if (timerState.isRunning) MaterialTheme.colorScheme.primary else HockeySurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "MI-TEMPS ${timerState.currentPeriod} / 2",
                                color = if (timerState.isRunning) HockeyDarkBg else HockeyTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Toggle Décompte / Croissant
                        Surface(
                            onClick = onToggleCountDown,
                            shape = RoundedCornerShape(8.dp),
                            color = HockeySurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder),
                            modifier = Modifier.testTag("toggle_countdown_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (timerState.isCountDown) Icons.Default.HourglassBottom else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = HockeyTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (timerState.isCountDown) "Décompte" else "Croissant",
                                    fontSize = 11.sp,
                                    color = HockeyTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Huge Tabular Chrono Digits
                    Text(
                        text = formattedTime,
                        style = TabularTimerStyle,
                        color = when {
                            timerState.isFinished -> HockeyDefeat
                            timerState.isRunning -> HockeyTextPrimary
                            else -> HockeyTextSecondary
                        },
                        modifier = Modifier.testTag("timer_display_text")
                    )

                    Text(
                        text = if (timerState.isFinished) {
                            "FIN DE MI-TEMPS ATTEINTE"
                        } else if (timerState.isRunning) {
                            "CHRONO EN COURS (Alarme et notification actives)"
                        } else {
                            "EN PAUSE"
                        },
                        fontSize = 11.sp,
                        color = if (timerState.isRunning) HockeyWin else HockeyTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    // 30s Timeout Banner if active
                    AnimatedVisibility(visible = timerState.timeoutRemainingSec != null) {
                        Surface(
                            color = HockeyPenaltyYellow.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HockeyPenaltyYellow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "⏸ TEMPS MORT COACH : ${timerState.timeoutRemainingSec ?: 0}s",
                                    color = HockeyPenaltyYellow,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Main Play/Pause Button (Minimum 56dp height for gloves)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (timerState.isRunning) onPauseTimer() else onStartTimer()
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp)
                                .testTag("play_pause_timer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (timerState.isRunning) HockeyDraw else MaterialTheme.colorScheme.primary,
                                contentColor = HockeyDarkBg
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (timerState.isRunning) "PAUSE" else "DÉMARRER",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        // 30s Timeout Button
                        OutlinedButton(
                            onClick = onTriggerTimeout,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("coach_timeout_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HockeyPenaltyYellow),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HockeyPenaltyYellow)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TEMPS MORT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "30 sec",
                                    fontSize = 10.sp,
                                    color = HockeyTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary controls: Switch to 2nd half & Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Switch to second period
                        if (timerState.currentPeriod == 1) {
                            Button(
                                onClick = onSwitchToSecondPeriod,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("switch_to_period_2_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HockeySurfaceVariant,
                                    contentColor = HockeyTextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("2e mi-temps", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Reset Chrono
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reset_timer_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HockeyTextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remise à zéro", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 2B: CHANGEMENTS DE JOUEURS / ROTATIONS
        item {
            SubstitutionTimerCard(
                state = substitutionTimer,
                isMatchTimerRunning = timerState.isRunning,
                onSetInterval = onSetSubstitutionInterval,
                onToggleRunning = onToggleSubstitutionTimer,
                onReset = onResetSubstitutionTimer,
                onToggleSync = onToggleSyncSubstitution,
                onDismissAlert = onDismissSubstitutionAlert
            )
        }

        // Section 3: PENALTY CORNERS (Displayed ONLY for U10, U11/U12; completely hidden for U7/U8 and U9!)
        if (match.hasPenaltyCorners) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HockeySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "PENALTY CORNERS (PC)",
                            style = MaterialTheme.typography.labelLarge,
                            color = HockeyPenaltyYellow,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // My Team PC
                            PenaltyCornerCounter(
                                teamName = match.myTeam,
                                count = if (match.isHome) match.homePenaltyCorners else match.awayPenaltyCorners,
                                teamColor = myTeamColor,
                                onIncrement = { onPenaltyCornerDelta(true, 1) },
                                onDecrement = { onPenaltyCornerDelta(true, -1) },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Opponent PC
                            PenaltyCornerCounter(
                                teamName = match.opponentTeam,
                                count = if (match.isHome) match.awayPenaltyCorners else match.homePenaltyCorners,
                                teamColor = opponentColor,
                                onIncrement = { onPenaltyCornerDelta(false, 1) },
                                onDecrement = { onPenaltyCornerDelta(false, -1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Section 4: DÉROULÉ DU MATCH (Timestamped Goal Events)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DÉROULÉ DES BUTS",
                            style = MaterialTheme.typography.labelLarge,
                            color = HockeyTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${goals.size} but(s)",
                            color = HockeyTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (goals.isEmpty()) {
                        Text(
                            text = "Aucun but inscrit pour le moment.",
                            color = HockeyTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        goals.reversed().forEach { goal ->
                            GoalEventItem(
                                goal = goal,
                                isMyTeam = goal.isMyTeam,
                                teamColor = if (goal.isMyTeam) myTeamColor else opponentColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Category Change Confirmation Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Changer la catégorie") },
            text = {
                Column {
                    Text(
                        "Attention : Changer la catégorie remet le chronomètre à zéro et adapte la durée des périodes (20 min en U7/U8, 25 min en U9/U10/U11/U12).",
                        fontSize = 13.sp,
                        color = HockeyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("U7/U8", "U9", "U10", "U11/U12").forEach { cat ->
                        Surface(
                            onClick = {
                                onChangeCategory(cat)
                                showCategoryDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (match.category == cat) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else HockeySurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = cat,
                                fontWeight = if (match.category == cat) FontWeight.Bold else FontWeight.Normal,
                                color = if (match.category == cat) MaterialTheme.colorScheme.primary else HockeyTextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Annuler")
                }
            },
            containerColor = HockeySurface,
            titleContentColor = HockeyTextPrimary,
            textContentColor = HockeyTextSecondary
        )
    }

    // Opponent Color Dialog
    if (showColorPicker) {
        OpponentColorPickerDialog(
            opponentTeamName = match.opponentTeam,
            currentColor = opponentColor,
            onColorSelected = { colorHex ->
                onSetOpponentColor(colorHex)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // Reset Chrono Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Remise à zéro du chrono ?") },
            text = { Text("Le temps de la mi-temps en cours sera remis à zéro et l'alarme planifiée sera annulée.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTimer()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HockeyDefeat)
                ) {
                    Text("Remettre à zéro")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Annuler")
                }
            },
            containerColor = HockeySurface
        )
    }

    // Finish Match Confirmation Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Terminer la rencontre ?") },
            text = { Text("Le match sera marqué comme terminé et archivé dans votre bilan. Le score actuel sera validé.") },
            confirmButton = {
                Button(
                    onClick = {
                        onFinishMatch()
                        showFinishDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HockeyWin)
                ) {
                    Text("Valider la fin du match", color = HockeyDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continuer")
                }
            },
            containerColor = HockeySurface
        )
    }
}

@Composable
fun TeamScoreRow(
    teamName: String,
    score: Int,
    isHome: Boolean,
    tagLabel: String,
    teamColor: Color,
    onTextColor: Color,
    onAddGoal: () -> Unit,
    onRemoveGoal: () -> Unit,
    isMyTeam: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Team Name & Tag
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = tagLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = teamColor,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = teamName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = HockeyTextPrimary,
                maxLines = 2,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Small "Retirer" button to correct mistake
            if (score > 0) {
                OutlinedButton(
                    onClick = onRemoveGoal,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HockeyTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag(if (isMyTeam) "remove_my_goal_button" else "remove_opp_goal_button")
                ) {
                    Text("− Retirer 1 but", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Giant Score display
        Text(
            text = score.toString(),
            style = TabularScoreStyle,
            color = HockeyTextPrimary,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .testTag(if (isMyTeam) "my_team_score_text" else "opponent_score_text")
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Large +1 Button (min 64dp for gloves / cold weather!)
        Button(
            onClick = onAddGoal,
            modifier = Modifier
                .size(width = 82.dp, height = 72.dp)
                .testTag(if (isMyTeam) "add_my_goal_button" else "add_opp_goal_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = teamColor,
                contentColor = onTextColor
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "+1",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun PenaltyCornerCounter(
    teamName: String,
    count: Int,
    teamColor: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HockeySurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = teamName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = HockeyTextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = teamColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (count > 0) {
                    OutlinedButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pc_decrement"),
                        shape = CircleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                    ) {
                        Text("−", fontSize = 16.sp, color = HockeyTextSecondary)
                    }
                }
                Button(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("pc_increment"),
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HockeyPenaltyYellow,
                        contentColor = HockeyDarkBg
                    )
                ) {
                    Text("+1", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun GoalEventItem(
    goal: GoalEvent,
    isMyTeam: Boolean,
    teamColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = HockeySurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(teamColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "MT ${goal.period} • ${goal.minute}'",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = HockeyTextSecondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "But : ${goal.teamName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HockeyTextPrimary
            )
        }
    }
}

@Composable
fun OpponentColorPickerDialog(
    opponentTeamName: String,
    currentColor: Color,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Couleur de l'adversaire", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = opponentTeamName,
                    fontSize = 13.sp,
                    color = HockeyTextSecondary
                )
            }
        },
        text = {
            Column {
                // Live preview of team badge with selected color
                Surface(
                    color = currentColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = opponentTeamName,
                        color = getContrastingTextColor(currentColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Choisissez la teinte du club adverse. Elle s'appliquera immédiatement au match et sera mémorisée pour toute la saison.",
                    fontSize = 12.sp,
                    color = HockeyTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Grid of 12 preset hockey club shades
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ClubColorPresets.chunked(4).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowColors.forEach { color ->
                                val hexString = color.toHex()
                                val isSelected = hexString.equals(currentColor.toHex(), ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.5.dp else 1.dp,
                                            color = if (isSelected) Color.White else HockeyBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { onColorSelected(hexString) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = getContrastingTextColor(color),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = HockeySurface,
        titleContentColor = HockeyTextPrimary
    )
}

@Composable
fun SubstitutionTimerCard(
    state: SubstitutionTimerState,
    isMatchTimerRunning: Boolean,
    onSetInterval: (Int) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onToggleSync: () -> Unit,
    onDismissAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HockeySurface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (state.showBannerAlert) 2.dp else 1.dp,
            color = if (state.showBannerAlert) HockeyPenaltyYellow else HockeyBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHANGEMENTS DE JOUEURS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = if (state.isRunning || (state.syncWithMatchTimer && isMatchTimerRunning)) HockeyWin.copy(alpha = 0.2f) else HockeySurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.isRunning || (state.syncWithMatchTimer && isMatchTimerRunning)) HockeyWin else HockeyBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (state.isRunning || (state.syncWithMatchTimer && isMatchTimerRunning)) HockeyWin else HockeyTextSecondary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isRunning || (state.syncWithMatchTimer && isMatchTimerRunning)) "Actif" else "En pause",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isRunning || (state.syncWithMatchTimer && isMatchTimerRunning)) HockeyWin else HockeyTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Minuteur d'alerte pour effectuer les rotations régulières (bip sonore + vibration)",
                fontSize = 11.sp,
                color = HockeyTextSecondary
            )

            // Alert banner if rotation just triggered
            if (state.showBannerAlert) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = HockeyPenaltyYellow.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, HockeyPenaltyYellow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = HockeyPenaltyYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CHANGEMENT DE JOUEUR !",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = HockeyPenaltyYellow
                                )
                                Text(
                                    text = "Rotation ${state.substitutionsCount + 1} • Minuteur en attente",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HockeyTextPrimary
                                )
                                Text(
                                    text = "Cliquez sur OK pour relancer ${state.intervalMinutes} min",
                                    fontSize = 11.sp,
                                    color = HockeyTextSecondary
                                )
                            }
                        }
                        Button(
                            onClick = onDismissAlert,
                            colors = ButtonDefaults.buttonColors(containerColor = HockeyPenaltyYellow, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("OK", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Digital Countdown + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = state.formattedRemaining,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.remainingSeconds <= 30 && (state.isRunning || (state.syncWithMatchTimer && isMatchTimerRunning))) HockeyPenaltyYellow else HockeyTextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "sur ${state.intervalMinutes} min • ${state.substitutionsCount} changement(s) fait(s)",
                        fontSize = 11.sp,
                        color = HockeyTextSecondary
                    )
                }

                // Quick Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isTicking = state.isRunning || (state.syncWithMatchTimer && isMatchTimerRunning)
                    IconButton(
                        onClick = onToggleRunning,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isTicking) HockeyPenaltyYellow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .border(1.dp, if (isTicking) HockeyPenaltyYellow else MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("toggle_substitution_timer_button")
                    ) {
                        Icon(
                            imageVector = if (isTicking) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTicking) "Pause" else "Démarrer",
                            tint = if (isTicking) HockeyPenaltyYellow else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(HockeySurfaceVariant)
                            .border(1.dp, HockeyBorder, CircleShape)
                            .testTag("reset_substitution_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Réinitialiser",
                            tint = HockeyTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = HockeySurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interval Configuration (1 to 20 minutes)
            Surface(
                color = HockeySurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Temps entre chaque rotation :",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HockeyTextPrimary
                        )

                        // Stepper: [-] 3 min [+] (configurable between 1 and 20 min)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSetInterval(state.intervalMinutes - 1) },
                                enabled = state.intervalMinutes > 1,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HockeyTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuer", modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = "${state.intervalMinutes} min",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(55.dp),
                                textAlign = TextAlign.Center
                            )

                            OutlinedButton(
                                onClick = { onSetInterval(state.intervalMinutes + 1) },
                                enabled = state.intervalMinutes < 20,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HockeyTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Augmenter", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fast Presets (2, 3, 4, 5, 10 min)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(2, 3, 4, 5, 10).forEach { preset ->
                            val isSelected = state.intervalMinutes == preset
                            Surface(
                                onClick = { onSetInterval(preset) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else HockeySurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else HockeyBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${preset}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else HockeyTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sync with match timer switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSync() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Synchroniser avec le chrono du match",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HockeyTextPrimary
                    )
                    Text(
                        text = "Démarre et s'arrête automatiquement avec la mi-temps",
                        fontSize = 10.sp,
                        color = HockeyTextSecondary
                    )
                }
                Switch(
                    checked = state.syncWithMatchTimer,
                    onCheckedChange = { onToggleSync() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchEntity
import com.example.ui.theme.HockeyBorder
import com.example.ui.theme.HockeyDarkBg
import com.example.ui.theme.HockeyDefeat
import com.example.ui.theme.HockeyDraw
import com.example.ui.theme.HockeySurface
import com.example.ui.theme.HockeySurfaceVariant
import com.example.ui.theme.HockeyTextPrimary
import com.example.ui.theme.HockeyTextSecondary
import com.example.ui.theme.HockeyTurfGreen
import com.example.ui.theme.HockeyWin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchListScreen(
    matches: List<MatchEntity>,
    onMatchClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddMatchClick: () -> Unit,
    onDeleteMatch: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()

    // Separate matches into upcoming/in-progress/to-encode and finished
    val activeAndUpcoming = matches.filter { !it.isFinished }
    val finishedMatches = matches.filter { it.isFinished }

    // Next match highlighted
    val nextMatchId = activeAndUpcoming.minByOrNull { it.timestamp }?.id

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HockeyDarkBg)
    ) {
        if (matches.isEmpty()) {
            EmptyMatchesView(
                onNavigateToSettings = onNavigateToSettings,
                onAddMatchClick = onAddMatchClick
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Matchs à venir / En cours / À encoder
                if (activeAndUpcoming.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "MATCHS À VENIR & EN COURS",
                            count = activeAndUpcoming.size,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(activeAndUpcoming, key = { it.id }) { match ->
                        val isNextMatch = match.id == nextMatchId
                        val isPastDate = match.timestamp < now
                        val needsEncoding = isPastDate && !match.isScoreEncoded

                        MatchCard(
                            match = match,
                            isNextMatch = isNextMatch,
                            needsEncoding = needsEncoding,
                            isFinished = false,
                            onClick = { onMatchClick(match.id) },
                            onDelete = if (!match.isFromCalendar) {
                                { onDeleteMatch(match.id) }
                            } else null
                        )
                    }
                }

                // Section 2: Matchs terminés
                if (finishedMatches.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(
                            title = "MATCHS TERMINÉS",
                            count = finishedMatches.size,
                            accentColor = HockeyWin
                        )
                    }

                    items(finishedMatches, key = { it.id }) { match ->
                        MatchCard(
                            match = match,
                            isNextMatch = false,
                            needsEncoding = false,
                            isFinished = true,
                            onClick = { onMatchClick(match.id) },
                            onDelete = if (!match.isFromCalendar) {
                                { onDeleteMatch(match.id) }
                            } else null
                        )
                    }
                }
            }
        }

        // Floating Action Button to add non-calendar match
        ExtendedFloatingActionButton(
            onClick = onAddMatchClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_manual_match_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = HockeyDarkBg
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter un match")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Match hors calendrier",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = HockeyTextSecondary,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = HockeySurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = count.toString(),
                color = HockeyTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun MatchCard(
    match: MatchEntity,
    isNextMatch: Boolean,
    needsEncoding: Boolean,
    isFinished: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val dateFormat = remember { SimpleDateFormat("EEE dd MMM", Locale.FRENCH) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.FRENCH) }
    val dateText = dateFormat.format(Date(match.timestamp)).replaceFirstChar { it.uppercase() }
    val timeText = timeFormat.format(Date(match.timestamp))

    val borderColor = when {
        isNextMatch -> MaterialTheme.colorScheme.primary
        isFinished -> when (match.matchResult) {
            "WIN" -> HockeyWin
            "DRAW" -> HockeyDraw
            else -> HockeyDefeat
        }
        else -> HockeyBorder
    }

    val borderWidth = if (isNextMatch) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isNextMatch) HockeySurfaceVariant else HockeySurface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Category Badge + Date/Time + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag (e.g. U9, U10)
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = match.category,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = HockeyTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$dateText • $timeText",
                    color = HockeyTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                // Status Badges
                if (isNextMatch && !isFinished) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PROCHAIN MATCH",
                            color = HockeyDarkBg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (needsEncoding) {
                    Surface(
                        color = HockeyDraw.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyDraw)
                    ) {
                        Text(
                            text = "À ENCODER",
                            color = HockeyDraw,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (isFinished) {
                    val (resultLabel, resultColor) = when (match.matchResult) {
                        "WIN" -> "VICTOIRE" to HockeyWin
                        "DRAW" -> "NUL" to HockeyDraw
                        else -> "DÉFAITE" to HockeyDefeat
                    }
                    Surface(
                        color = resultColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, resultColor)
                    ) {
                        Text(
                            text = resultLabel,
                            color = resultColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Delete action for non-calendar matches
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_match_${match.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer ce match",
                            tint = HockeyTextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.homeTeam,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (match.isHome) MaterialTheme.colorScheme.primary else HockeyTextPrimary,
                        fontWeight = if (match.isHome) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.awayTeam,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (!match.isHome) MaterialTheme.colorScheme.primary else HockeyTextPrimary,
                        fontWeight = if (!match.isHome) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Score or Open arrow
                if (match.isScoreEncoded || isFinished) {
                    Surface(
                        color = HockeyDarkBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${match.homeScore} - ${match.awayScore}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    isFinished -> when (match.matchResult) {
                                        "WIN" -> HockeyWin
                                        "DRAW" -> HockeyDraw
                                        else -> HockeyDefeat
                                    }
                                    else -> HockeyTextPrimary
                                }
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Ouvrir feuille de match",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Location if present
            if (!match.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = HockeyTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = match.location,
                        color = HockeyTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyMatchesView(
    onNavigateToSettings: () -> Unit,
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
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SportsHockey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aucun match au calendrier",
            style = MaterialTheme.typography.titleLarge,
            color = HockeyTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Importez le calendrier officiel de votre club (iCal ou Google Agenda) ou créez un match manuellement.",
            style = MaterialTheme.typography.bodyMedium,
            color = HockeyTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(52.dp)
                .testTag("empty_state_settings_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = HockeyDarkBg
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configurer & importer le calendrier", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onAddMatchClick,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("empty_state_add_manual_button"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = HockeyTextPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Créer un match hors calendrier")
        }
    }
}

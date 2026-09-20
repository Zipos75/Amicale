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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.HockeyWin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BilanScreen(
    matches: List<MatchEntity>,
    onMatchClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only encoded or finished matches are part of the bilan
    val playedMatches = remember(matches) {
        matches.filter { it.isScoreEncoded || it.isFinished }
    }

    var wins = 0
    var draws = 0
    var defeats = 0
    var goalsFor = 0
    var goalsAgainst = 0

    for (m in playedMatches) {
        val myScore = if (m.isHome) m.homeScore else m.awayScore
        val oppScore = if (m.isHome) m.awayScore else m.homeScore
        goalsFor += myScore
        goalsAgainst += oppScore
        when {
            myScore > oppScore -> wins++
            myScore == oppScore -> draws++
            else -> defeats++
        }
    }

    val goalDiff = goalsFor - goalsAgainst
    val totalPlayed = playedMatches.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HockeyDarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "BILAN DE LA SAISON",
                style = MaterialTheme.typography.titleLarge,
                color = HockeyTextPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "$totalPlayed match(s) joué(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = HockeyTextSecondary
            )
        }

        // Section 1: Wins, Draws, Losses summary cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "VICTOIRES",
                    value = wins.toString(),
                    color = HockeyWin,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "NULS",
                    value = draws.toString(),
                    color = HockeyDraw,
                    icon = Icons.Default.TrendingFlat,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "DÉFAITES",
                    value = defeats.toString(),
                    color = HockeyDefeat,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section 2: Goals (BP, BC, Diff)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STATISTIQUES DE BUTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = HockeyTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        GoalStatItem(
                            label = "Marqués (BP)",
                            value = goalsFor.toString(),
                            color = HockeyWin
                        )
                        GoalStatItem(
                            label = "Encaissés (BC)",
                            value = goalsAgainst.toString(),
                            color = HockeyDefeat
                        )
                        GoalStatItem(
                            label = "Différence",
                            value = (if (goalDiff > 0) "+$goalDiff" else "$goalDiff"),
                            color = if (goalDiff > 0) HockeyWin else if (goalDiff == 0) HockeyDraw else HockeyDefeat
                        )
                    }
                }
            }
        }

        // Section 3: Chronological History
        item {
            Text(
                text = "HISTORIQUE CHRONOLOGIQUE",
                style = MaterialTheme.typography.labelLarge,
                color = HockeyTextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (playedMatches.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HockeySurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucun match joué ou encodé pour l'instant.\nEncodez vos matchs pour voir vos statistiques !",
                        color = HockeyTextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(playedMatches.sortedByDescending { it.timestamp }, key = { it.id }) { match ->
                BilanMatchRow(match = match, onClick = { onMatchClick(match.id) })
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HockeySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HockeyTextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun GoalStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = HockeyTextSecondary
        )
    }
}

@Composable
fun BilanMatchRow(match: MatchEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH) }
    val dateText = dateFormat.format(Date(match.timestamp))

    val (resultColor, resultBadge) = when (match.matchResult) {
        "WIN" -> HockeyWin to "V"
        "DRAW" -> HockeyDraw to "N"
        else -> HockeyDefeat to "D"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, HockeyBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("bilan_match_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = HockeySurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Result Badge (V / N / D)
            Surface(
                color = resultColor,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = resultBadge,
                        color = HockeyDarkBg,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Teams & Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${match.homeTeam} vs ${match.awayTeam}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HockeyTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateText • ${match.category}",
                    fontSize = 11.sp,
                    color = HockeyTextSecondary
                )
            }

            // Final Score
            Surface(
                color = HockeyDarkBg,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, resultColor)
            ) {
                Text(
                    text = "${match.homeScore} - ${match.awayScore}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = resultColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

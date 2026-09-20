package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HockeyRulesRepository
import com.example.ui.theme.HockeyBorder
import com.example.ui.theme.HockeyDarkBg
import com.example.ui.theme.HockeyPenaltyYellow
import com.example.ui.theme.HockeySurface
import com.example.ui.theme.HockeySurfaceVariant
import com.example.ui.theme.HockeyTextPrimary
import com.example.ui.theme.HockeyTextSecondary

@Composable
fun RulesScreen(
    modifier: Modifier = Modifier
) {
    val categories = remember { HockeyRulesRepository.categories }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val currentCategory = categories[selectedCategoryIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HockeyDarkBg)
    ) {
        // Top Bar Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RÈGLEMENTS OFFICIELS",
                    style = MaterialTheme.typography.titleLarge,
                    color = HockeyTextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Fédérations belges de Hockey (ARBH / VHL / LFH)",
                style = MaterialTheme.typography.bodySmall,
                color = HockeyTextSecondary
            )
        }

        // Category Tabs (U7/U8, U9, U10, U11/U12)
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = HockeySurface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = { Divider(color = HockeyBorder) }
        ) {
            categories.forEachIndexed { index, cat ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    modifier = Modifier.testTag("rules_tab_${cat.category}"),
                    text = {
                        Text(
                            text = cat.category,
                            fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HockeySurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Catégorie ${currentCategory.category}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = currentCategory.durationText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentCategory.subtitle,
                            fontSize = 13.sp,
                            color = HockeyTextSecondary
                        )
                    }
                }
            }

            // Common rules box (applicable à partir de U7)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HockeyPenaltyYellow.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HockeyPenaltyYellow.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sports,
                                contentDescription = null,
                                tint = HockeyPenaltyYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DISPOSITIONS COMMUNES (DÈS U7)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HockeyPenaltyYellow,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HockeyRulesRepository.commonRules.forEach { rule ->
                            Text(
                                text = "• $rule",
                                fontSize = 12.sp,
                                color = HockeyTextPrimary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Verbatim rules items for selected category
            items(currentCategory.items) { item ->
                RuleRowCard(label = item.label, description = item.description)
            }

            // Verbatim disclaimer at the bottom (Required by prompt)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HockeySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = HockeyTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = HockeyRulesRepository.disclaimerText,
                            fontSize = 11.sp,
                            color = HockeyTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleRowCard(label: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HockeySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = HockeyTextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

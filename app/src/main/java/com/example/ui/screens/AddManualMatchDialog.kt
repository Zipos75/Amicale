package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HockeyDarkBg
import com.example.ui.theme.HockeySurface
import com.example.ui.theme.HockeyTextPrimary
import com.example.ui.theme.HockeyTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualMatchDialog(
    defaultMyTeam: String,
    onDismiss: () -> Unit,
    onAddMatch: (
        date: String,
        time: String,
        myTeam: String,
        opponent: String,
        isHome: Boolean,
        category: String,
        location: String?
    ) -> Unit
) {
    val currentDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val currentTime = remember { "10:00" }

    var dateInput by remember { mutableStateOf(currentDate) }
    var timeInput by remember { mutableStateOf(currentTime) }
    var myTeamInput by remember { mutableStateOf(defaultMyTeam.ifBlank { "Mon Équipe" }) }
    var opponentInput by remember { mutableStateOf("") }
    var isHome by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("U10") }
    var locationInput by remember { mutableStateOf("") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("U7/U8", "U9", "U10", "U11/U12")

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nouveau match hors calendrier",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HockeyTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                // Date & Heure
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (JJ/MM/AAAA)") },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("match_date_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = { timeInput = it },
                        label = { Text("Heure") },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("match_time_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Mon équipe
                OutlinedTextField(
                    value = myTeamInput,
                    onValueChange = { myTeamInput = it },
                    label = { Text("Mon équipe") },
                    placeholder = { Text("Ex: Waterloo Ducks U10B-1") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_team_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Adversaire
                OutlinedTextField(
                    value = opponentInput,
                    onValueChange = { opponentInput = it },
                    label = { Text("Équipe adverse") },
                    placeholder = { Text("Ex: Léopold U10B-2") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opponent_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Domicile / Extérieur Toggle
                Column {
                    Text(
                        text = "Lieu de la rencontre :",
                        fontSize = 12.sp,
                        color = HockeyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = isHome,
                            onClick = { isHome = true },
                            label = { Text("Domicile") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = HockeyDarkBg
                            )
                        )
                        FilterChip(
                            selected = !isHome,
                            onClick = { isHome = false },
                            label = { Text("Extérieur") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = HockeyDarkBg
                            )
                        )
                    }
                }

                // Catégorie
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Lieu facultatif
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Terrain / Club (facultatif)") },
                    placeholder = { Text("Ex: Terrain 2, Drève d'Argenteuil") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (myTeamInput.isBlank()) {
                        errorMsg = "Veuillez indiquer le nom de votre équipe."
                        return@Button
                    }
                    if (opponentInput.isBlank()) {
                        errorMsg = "Veuillez indiquer l'adversaire."
                        return@Button
                    }
                    onAddMatch(
                        dateInput.trim(),
                        timeInput.trim(),
                        myTeamInput.trim(),
                        opponentInput.trim(),
                        isHome,
                        selectedCategory,
                        locationInput.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = HockeyDarkBg
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Ajouter", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        containerColor = HockeySurface,
        titleContentColor = HockeyTextPrimary
    )
}

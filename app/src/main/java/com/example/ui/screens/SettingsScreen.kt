package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calendar.DeviceCalendarInfo
import com.example.data.repository.AppSettings
import com.example.ui.theme.ClubColorPresets
import com.example.ui.theme.HockeyBorder
import com.example.ui.theme.HockeyDarkBg
import com.example.ui.theme.HockeySurface
import com.example.ui.theme.HockeySurfaceVariant
import com.example.ui.theme.HockeyTextPrimary
import com.example.ui.theme.HockeyTextSecondary
import com.example.ui.theme.HockeyWin
import com.example.ui.theme.colorFromHex
import com.example.ui.theme.getContrastingTextColor
import com.example.ui.theme.toHex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    distinctTeams: List<String>,
    isImporting: Boolean,
    importStatusMessage: String?,
    deviceCalendars: List<DeviceCalendarInfo> = emptyList(),
    onImportIcal: (String) -> Unit,
    onLoadDeviceCalendars: () -> Unit = {},
    onImportPhoneCalendar: (calendarId: Long?) -> Unit,
    onDeleteImportedMatches: () -> Unit = {},
    onSetChildTeam: (String) -> Unit,
    onSetClubHeader: (name: String, subtitle: String) -> Unit,
    onSetClubColor: (String) -> Unit,
    onClearImportStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Local inputs for smooth editing
    var clubNameInput by remember(settings.clubName) { mutableStateOf(settings.clubName) }
    var clubSubtitleInput by remember(settings.clubSubtitle) { mutableStateOf(settings.clubSubtitle) }
    var icalUrlInput by remember(settings.calendarUrl) { mutableStateOf(settings.calendarUrl) }
    var customHexInput by remember(settings.clubColorHex) { mutableStateOf(settings.clubColorHex) }

    var expandedTeamDropdown by remember { mutableStateOf(false) }
    var showCalendarPickerDialog by remember { mutableStateOf(false) }
    var selectedCalendarId by remember { mutableStateOf<Long?>(null) }
    var showDeleteImportedConfirmDialog by remember { mutableStateOf(false) }

    // Calendar Permission launcher
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onLoadDeviceCalendars()
            showCalendarPickerDialog = true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HockeyDarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Page Title
        item {
            Text(
                text = "RÉGLAGES",
                style = MaterialTheme.typography.titleLarge,
                color = HockeyTextPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Personnalisation du club, calendrier et équipe",
                style = MaterialTheme.typography.bodySmall,
                color = HockeyTextSecondary
            )
        }

        // Status banner if import finished
        if (!importStatusMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (importStatusMessage.startsWith("✓")) HockeyWin.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (importStatusMessage.startsWith("✓")) HockeyWin else MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = importStatusMessage,
                            color = HockeyTextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearImportStatus, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = HockeyTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Section 1: Mon Club (Header display)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MON CLUB (EN-TÊTE)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clubNameInput,
                        onValueChange = {
                            clubNameInput = it
                            onSetClubHeader(it, clubSubtitleInput)
                        },
                        label = { Text("Nom du club") },
                        placeholder = { Text("Ex: Amicale Anderlecht") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("club_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = clubSubtitleInput,
                        onValueChange = {
                            clubSubtitleInput = it
                            onSetClubHeader(clubNameInput, it)
                        },
                        label = { Text("Sous-titre") },
                        placeholder = { Text("Ex: Hockey Club") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("club_subtitle_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Section 2: Équipe de l'enfant
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ÉQUIPE DE L'ENFANT",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cette équipe est identifiée comme « Mon équipe » (à domicile ou à l'extérieur).",
                        fontSize = 12.sp,
                        color = HockeyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (distinctTeams.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedTeamDropdown,
                            onExpandedChange = { expandedTeamDropdown = !expandedTeamDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = settings.childTeamName,
                                onValueChange = { onSetChildTeam(it) },
                                label = { Text("Nom d'équipe sélectionné") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTeamDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("child_team_dropdown"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTeamDropdown,
                                onDismissRequest = { expandedTeamDropdown = false }
                            ) {
                                distinctTeams.forEach { team ->
                                    DropdownMenuItem(
                                        text = { Text(team) },
                                        onClick = {
                                            onSetChildTeam(team)
                                            expandedTeamDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = settings.childTeamName,
                            onValueChange = { onSetChildTeam(it) },
                            label = { Text("Équipe de l'enfant") },
                            placeholder = { Text("Ex: Amicale Anderlecht U9B-1") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("child_team_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Section 3: Calendrier & Synchronisation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IMPORT DU CALENDRIER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Source 1 : Lien iCal (.ics) Sportlink ou Google Agenda\nSource 2 : Agenda présent sur votre téléphone",
                        fontSize = 12.sp,
                        color = HockeyTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // iCal URL Input
                    OutlinedTextField(
                        value = icalUrlInput,
                        onValueChange = { icalUrlInput = it },
                        label = { Text("Lien iCal (.ics)") },
                        placeholder = { Text("https://.../calendar.ics") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ical_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onImportIcal(icalUrlInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("import_ical_button"),
                        enabled = !isImporting && icalUrlInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = HockeyDarkBg
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HockeyDarkBg, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Téléchargement...")
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Télécharger & synchroniser le lien iCal", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Phone Calendar Button
                    OutlinedButton(
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CALENDAR
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                onLoadDeviceCalendars()
                                showCalendarPickerDialog = true
                            } else {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("import_phone_calendar_button"),
                        enabled = !isImporting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HockeyTextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importer depuis l'agenda du téléphone")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete imported matches button
                    OutlinedButton(
                        onClick = { showDeleteImportedConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_imported_matches_button"),
                        enabled = !isImporting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Effacer les matchs importés", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }

                    // Last import information
                    if (settings.lastImportTime > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val lastDateStr = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH).format(Date(settings.lastImportTime))
                        Surface(
                            color = HockeySurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = HockeyTextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dernier import : $lastDateStr (${settings.lastImportCount} matchs)",
                                    fontSize = 11.sp,
                                    color = HockeyTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Club Accent Color
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COULEUR D'ACCENTUATION DU CLUB",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Teinte les boutons principaux, l'onglet actif et les bordures de cartes.",
                        fontSize = 12.sp,
                        color = HockeyTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Presets Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClubColorPresets.chunked(6).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowColors.forEach { color ->
                                    val hex = color.toHex()
                                    val isSelected = settings.clubColorHex.equals(hex, ignoreCase = true)

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) HockeyTextPrimary else HockeyBorder,
                                                shape = CircleShape
                                            )
                                            .clickable { onSetClubColor(hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = getContrastingTextColor(color),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Custom hex code input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customHexInput,
                            onValueChange = { customHexInput = it },
                            label = { Text("Code Hexadécimal") },
                            placeholder = { Text("#60B6E7") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_color_hex_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onSetClubColor(customHexInput.trim()) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Appliquer")
                        }
                    }
                }
            }
        }

        // Section 5: À propos (sobre, conforme aux instructions)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HockeySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HockeyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "À PROPOS",
                        style = MaterialTheme.typography.labelLarge,
                        color = HockeyTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Application développée par Tanguy Lauwers pour aider, en tant que parent, les arbitres et coachs de l'Amicale d'Anderlecht. Soyez toujours fair-play. Si vous voyez une erreur ou une amélioration possible, envoyez-moi un message WhatsApp au 0474/21.76.80.",
                        fontSize = 13.sp,
                        color = HockeyTextPrimary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/32474217680"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0474217680"))
                                try { context.startActivity(dialIntent) } catch (_: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "💬 Message WhatsApp (0474/21.76.80)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // Calendar Selection Dialog
    if (showCalendarPickerDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarPickerDialog = false },
            containerColor = HockeySurface,
            title = {
                Text(
                    text = "Choisir le calendrier à importer",
                    fontWeight = FontWeight.Bold,
                    color = HockeyTextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sélectionnez le calendrier contenant vos matchs pour n'importer que les événements concernés :",
                        color = HockeyTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: All calendars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCalendarId = null }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedCalendarId == null),
                            onClick = { selectedCalendarId = null },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Tous les calendriers",
                                fontWeight = FontWeight.Bold,
                                color = HockeyTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Recherche les matchs dans tous les agendas",
                                color = HockeyTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Individual device calendars
                    deviceCalendars.forEach { cal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCalendarId = cal.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedCalendarId == cal.id),
                                onClick = { selectedCalendarId = cal.id },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(cal.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = cal.displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = HockeyTextPrimary,
                                    fontSize = 14.sp
                                )
                                cal.accountName?.let { acc ->
                                    Text(
                                        text = acc,
                                        color = HockeyTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCalendarPickerDialog = false
                        onImportPhoneCalendar(selectedCalendarId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Importer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarPickerDialog = false }) {
                    Text("Annuler", color = HockeyTextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteImportedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteImportedConfirmDialog = false },
            containerColor = HockeySurface,
            title = {
                Text(
                    text = "Effacer les matchs importés ?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Tous les matchs synchronisés depuis un calendrier ou un lien iCal seront supprimés. Vos matchs créés manuellement seront conservés.",
                    color = HockeyTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteImportedConfirmDialog = false
                        onDeleteImportedMatches()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Effacer", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteImportedConfirmDialog = false }) {
                    Text("Annuler", color = HockeyTextSecondary)
                }
            }
        )
    }
}

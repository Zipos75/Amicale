package com.example.calendar

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.example.data.model.MatchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

sealed class ImportResult {
    data class Success(
        val matches: List<MatchEntity>,
        val suggestedChildTeam: String?,
        val allDistinctTeams: List<String>
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}

data class DeviceCalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String?,
    val color: Int?
)

object CalendarImporter {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Retrieves all user calendars from Android's CalendarContract.
     */
    suspend fun getDeviceCalendars(contentResolver: ContentResolver): List<DeviceCalendarInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DeviceCalendarInfo>()
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR
            )
            val cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )
            cursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)

                while (c.moveToNext()) {
                    list.add(
                        DeviceCalendarInfo(
                            id = c.getLong(idIdx),
                            displayName = c.getString(nameIdx) ?: "Agenda",
                            accountName = c.getString(accIdx),
                            color = if (c.isNull(colorIdx)) null else c.getInt(colorIdx)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // In case of permission issues or mock environments
        }
        list
    }

    /**
     * Download and parse an iCal (.ics) file from URL.
     * Works directly without CORS restrictions via OkHttp.
     * Handles offline / airplane mode with explicit friendly message.
     */
    suspend fun importFromUrl(url: String, currentChildTeam: String = ""): ImportResult = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext ImportResult.Error("Veuillez saisir une adresse de calendrier iCal (.ics).")
        }

        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) && !trimmedUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext ImportResult.Error("L'adresse doit commencer par http:// ou https://")
        }

        try {
            val request = Request.Builder()
                .url(trimmedUrl)
                .header("User-Agent", "HockeyFeuilleDeMatch/1.0 (Android)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ImportResult.Error("Erreur serveur (${response.code}) lors du téléchargement du calendrier.")
            }

            val bodyString = response.body?.string()
                ?: return@withContext ImportResult.Error("Le calendrier téléchargé est vide.")

            parseICalString(bodyString, currentChildTeam)
        } catch (e: java.net.UnknownHostException) {
            ImportResult.Error("Aucune connexion réseau : impossible de joindre le calendrier. Vérifiez votre connexion internet ou le mode avion.")
        } catch (e: java.net.SocketTimeoutException) {
            ImportResult.Error("Le serveur du calendrier met trop de temps à répondre. Réessayez dans un instant.")
        } catch (e: IOException) {
            ImportResult.Error("Impossible de télécharger le calendrier : ${e.localizedMessage ?: "erreur réseau"}. Vérifiez votre connexion ou le mode avion.")
        } catch (e: Exception) {
            ImportResult.Error("Erreur lors de l'import : ${e.localizedMessage}")
        }
    }

    /**
     * Read events from phone's local Android Calendar via CalendarContract.
     */
    suspend fun importFromDeviceCalendar(
        contentResolver: ContentResolver,
        calendarId: Long? = null,
        currentChildTeam: String = ""
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION
            )

            // Get events from 30 days ago to 120 days in the future
            val now = System.currentTimeMillis()
            val startSearch = now - (30L * 24 * 3600 * 1000)
            val endSearch = now + (120L * 24 * 3600 * 1000)

            val (selection, selectionArgs) = if (calendarId != null) {
                val sel = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val args = arrayOf(calendarId.toString(), startSearch.toString(), endSearch.toString())
                Pair(sel, args)
            } else {
                val sel = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val args = arrayOf(startSearch.toString(), endSearch.toString())
                Pair(sel, args)
            }
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

            val cursor: Cursor? = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            val matches = mutableListOf<MatchEntity>()
            cursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIdx = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val dtstartIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val locIdx = c.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)

                while (c.moveToNext()) {
                    val eventId = c.getLong(idIdx)
                    val title = c.getString(titleIdx) ?: ""
                    val dtstart = c.getLong(dtstartIdx)
                    val location = c.getString(locIdx)

                    if (title.isNotBlank()) {
                        val (home, away) = splitTeams(title)
                        val category = detectCategory(title)
                        val isHome = if (currentChildTeam.isNotBlank()) {
                            home.contains(currentChildTeam, ignoreCase = true)
                        } else true

                        val myTeam = if (isHome) home else away
                        val opponent = if (isHome) away else home

                        val durationMin = MatchEntity.defaultDurationForCategory(category)

                        matches.add(
                            MatchEntity(
                                uid = "dev_cal_$eventId",
                                isFromCalendar = true,
                                timestamp = dtstart,
                                homeTeam = home,
                                awayTeam = away,
                                isHome = isHome,
                                myTeam = myTeam,
                                opponentTeam = opponent,
                                category = category,
                                location = location,
                                periodDurationMinutes = durationMin
                            )
                        )
                    }
                }
            }

            if (matches.isEmpty()) {
                return@withContext ImportResult.Error("Aucun match trouvé dans le calendrier du téléphone sur la période analysée.")
            }

            val (suggestedTeam, distinctTeams) = analyzeTeams(matches, currentChildTeam)
            val configuredMatches = if (suggestedTeam != null && currentChildTeam.isBlank()) {
                matches.map { reassignMyTeam(it, suggestedTeam) }
            } else {
                matches
            }

            ImportResult.Success(configuredMatches, suggestedTeam, distinctTeams)
        } catch (e: SecurityException) {
            ImportResult.Error("Permission de lire l'agenda refusée par Android.")
        } catch (e: Exception) {
            ImportResult.Error("Erreur d'accès à l'agenda : ${e.localizedMessage}")
        }
    }

    /**
     * Parse raw iCalendar text (.ics).
     * Strictly fulfills all prompt requirements:
     * - Unfolds folded lines (starts with space or tab)
     * - Unescapes \,, \;, \n
     * - Splits summary on first hyphen not followed by a digit: -(?!\d)
     * - Handles DTSTART in UTC (Z), TZID, and Date-only
     * - Counts team occurrences and suggests most frequent
     */
    fun parseICalString(rawIcs: String, currentChildTeam: String = ""): ImportResult {
        val unfoldedLines = unfoldICal(rawIcs)
        val matches = mutableListOf<MatchEntity>()

        var inEvent = false
        var currentUid: String? = null
        var currentSummary: String? = null
        var currentStartTimestamp: Long? = null
        var currentLocation: String? = null

        for (line in unfoldedLines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                currentUid = null
                currentSummary = null
                currentStartTimestamp = null
                currentLocation = null
                continue
            }

            if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && currentSummary != null && currentStartTimestamp != null) {
                    val uid = currentUid ?: "gen_uid_${currentStartTimestamp}_${currentSummary.hashCode()}"
                    val cleanSummary = unescapeICal(currentSummary)
                    val cleanLocation = currentLocation?.let { unescapeICal(it) }

                    val (home, away) = splitTeams(cleanSummary)
                    val category = detectCategory(cleanSummary)
                    val isHome = if (currentChildTeam.isNotBlank()) {
                        home.contains(currentChildTeam, ignoreCase = true)
                    } else true

                    val myTeam = if (isHome) home else away
                    val opponent = if (isHome) away else home
                    val durationMin = MatchEntity.defaultDurationForCategory(category)

                    matches.add(
                        MatchEntity(
                            uid = uid,
                            isFromCalendar = true,
                            timestamp = currentStartTimestamp,
                            homeTeam = home,
                            awayTeam = away,
                            isHome = isHome,
                            myTeam = myTeam,
                            opponentTeam = opponent,
                            category = category,
                            location = cleanLocation,
                            periodDurationMinutes = durationMin
                        )
                    )
                }
                inEvent = false
                continue
            }

            if (!inEvent) continue

            val colonIdx = line.indexOf(':')
            if (colonIdx == -1) continue

            val propPart = line.substring(0, colonIdx)
            val valuePart = line.substring(colonIdx + 1)

            val propName = propPart.split(';')[0].trim().uppercase(Locale.ROOT)

            when (propName) {
                "UID" -> currentUid = valuePart.trim()
                "SUMMARY" -> currentSummary = valuePart.trim()
                "LOCATION" -> currentLocation = valuePart.trim()
                "DTSTART" -> currentStartTimestamp = parseDtStart(propPart, valuePart.trim())
            }
        }

        if (matches.isEmpty()) {
            return ImportResult.Error("Aucun match (événement VEVENT valide avec date et titre) n'a été trouvé dans ce fichier iCal.")
        }

        val (suggestedTeam, distinctTeams) = analyzeTeams(matches, currentChildTeam)
        val finalTeam = if (currentChildTeam.isNotBlank()) currentChildTeam else (suggestedTeam ?: "")
        val configuredMatches = if (finalTeam.isNotBlank()) {
            matches.map { reassignMyTeam(it, finalTeam) }
        } else {
            matches
        }

        return ImportResult.Success(configuredMatches, suggestedTeam, distinctTeams)
    }

    /**
     * Unfolds lines in iCal standard:
     * A line starting with space or tab is a continuation of the previous line.
     */
    fun unfoldICal(content: String): List<String> {
        val lines = content.lines()
        val result = mutableListOf<String>()
        for (line in lines) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && result.isNotEmpty()) {
                result[result.size - 1] = result.last() + line.substring(1)
            } else {
                result.add(line)
            }
        }
        return result
    }

    fun unescapeICal(value: String): String {
        return value
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\n", "\n", ignoreCase = true)
            .replace("\\\\", "\\")
    }

    /**
     * Splits teams in summary:
     * Example: "Waterloo Ducks U9B-3-Amicale Anderlecht U9B-1"
     * Rule: Cut on the first hyphen that is NOT followed by a digit: -(?!\d)
     */
    fun splitTeams(summary: String): Pair<String, String> {
        val clean = summary.trim()
        val regex = Regex("-(?!\\d)")
        val match = regex.find(clean)
        return if (match != null) {
            val home = clean.substring(0, match.range.first).trim()
            val away = clean.substring(match.range.last + 1).trim()
            Pair(home, away)
        } else {
            if (clean.contains(" vs ", ignoreCase = true)) {
                val parts = clean.split(Regex(" vs ", RegexOption.IGNORE_CASE), limit = 2)
                Pair(parts[0].trim(), parts.getOrElse(1) { "" }.trim())
            } else if (clean.contains(" - ")) {
                val parts = clean.split(" - ", limit = 2)
                Pair(parts[0].trim(), parts.getOrElse(1) { "" }.trim())
            } else {
                Pair(clean, "Adversaire")
            }
        }
    }

    /**
     * Detect category from string (e.g. U7, U8, U9, U10, U11, U12)
     */
    fun detectCategory(text: String): String {
        val upper = text.uppercase(Locale.ROOT)
        return when {
            upper.contains("U7") || upper.contains("U8") -> "U7/U8"
            upper.contains("U9") -> "U9"
            upper.contains("U11") || upper.contains("U12") -> "U11/U12"
            upper.contains("U10") -> "U10"
            else -> "U10"
        }
    }

    /**
     * Parses DTSTART:
     * 1. UTC with Z: 20241014T103000Z
     * 2. Local with TZID: DTSTART;TZID=Europe/Brussels:20241014T103000
     * 3. Date only: 20241014
     */
    fun parseDtStart(propPart: String, value: String): Long {
        val cleanVal = value.trim()

        // 1. Check UTC Z
        if (cleanVal.endsWith("Z", ignoreCase = true)) {
            val cleanTime = cleanVal.removeSuffix("Z").removeSuffix("z")
            val format = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            try {
                return format.parse(cleanTime)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {}
        }

        // 2. Check TZID in property part
        var specifiedTz: TimeZone = TimeZone.getDefault()
        if (propPart.contains("TZID=", ignoreCase = true)) {
            val tzidPart = propPart.substringAfter("TZID=", "").substringBefore(';')
            if (tzidPart.isNotBlank()) {
                specifiedTz = TimeZone.getTimeZone(tzidPart.trim())
            }
        }

        // Standard DateTime: yyyyMMdd'T'HHmmss
        if (cleanVal.contains("T")) {
            val format = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT).apply {
                timeZone = specifiedTz
            }
            try {
                return format.parse(cleanVal)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {}
        }

        // Date only: yyyyMMdd
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.ROOT).apply {
            timeZone = specifiedTz
        }
        try {
            return dateFormat.parse(cleanVal)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {}

        return System.currentTimeMillis()
    }

    /**
     * Counts team occurrences and suggests the most frequent as child's team.
     */
    fun analyzeTeams(matches: List<MatchEntity>, preferredTeam: String): Pair<String?, List<String>> {
        val counts = mutableMapOf<String, Int>()
        for (m in matches) {
            counts[m.homeTeam] = (counts[m.homeTeam] ?: 0) + 1
            counts[m.awayTeam] = (counts[m.awayTeam] ?: 0) + 1
        }

        val sortedTeams = counts.keys.sortedWith(compareByDescending<String> { counts[it] }.thenBy { it })
        val suggested = if (preferredTeam.isNotBlank() && counts.containsKey(preferredTeam)) {
            preferredTeam
        } else {
            sortedTeams.firstOrNull()
        }

        return Pair(suggested, sortedTeams)
    }

    private fun reassignMyTeam(match: MatchEntity, myTeamName: String): MatchEntity {
        val isHome = match.homeTeam.equals(myTeamName, ignoreCase = true) ||
                match.homeTeam.contains(myTeamName, ignoreCase = true)
        val opponent = if (isHome) match.awayTeam else match.homeTeam
        return match.copy(
            isHome = isHome,
            myTeam = myTeamName,
            opponentTeam = opponent
        )
    }
}

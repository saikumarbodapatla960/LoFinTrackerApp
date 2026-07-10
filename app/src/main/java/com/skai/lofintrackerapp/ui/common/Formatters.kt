package com.skai.lofintrackerapp.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Our standard format for the database (for sorting)
private val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
// The new format you requested for display
private val displayFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

/**
 * Converts a YYYY-MM-DD string from the database to a DD-MM-YYYY string for the UI.
 */
fun formatDateForDisplay(dbDate: String): String {
    return try {
        val date = dbFormatter.parse(dbDate)
        displayFormatter.format(date ?: Date())
    } catch (e: Exception) {
        dbDate // Fallback if parsing fails
    }
}

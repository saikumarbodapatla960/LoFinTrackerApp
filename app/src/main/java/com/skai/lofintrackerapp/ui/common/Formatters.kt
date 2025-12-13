// In ...ui.common/Formatters.kt
package com.skai.lofintrackerapp.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.NumberFormat
import java.util.Currency

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

fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        // Fallback if code is invalid
        "₹$amount"
    }
}
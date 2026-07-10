package com.skai.lofintrackerapp.ui.common

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun getCurrencyDisplaySymbol(currencyCode: String, forSelection: Boolean = false): String {
    return when (currencyCode) {
        "USD" -> if (forSelection) "USD ($)" else "$"
        "EUR" -> if (forSelection) "EUR (€)" else "€"
        "GBP" -> if (forSelection) "GBP (£)" else "£"
        "JPY" -> if (forSelection) "JPY (¥)" else "¥"
        "INR" -> if (forSelection) "INR (₹)" else "₹"
        else -> currencyCode
    }
}

fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance() as DecimalFormat
        // For USD, explicitly set the symbol to '$' to avoid "US$"
        if (currencyCode == "USD") {
            val symbols = format.decimalFormatSymbols
            symbols.currencySymbol = "$"
            format.decimalFormatSymbols = symbols
        } else {
            // For other currencies, use the standard symbol
            format.currency = Currency.getInstance(currencyCode)
        }
        format.format(amount)
    } catch (e: Exception) {
        // Fallback if currency code is invalid
        "${getCurrencyDisplaySymbol(currencyCode)} ${String.format("%.2f", amount)}"
    }
}

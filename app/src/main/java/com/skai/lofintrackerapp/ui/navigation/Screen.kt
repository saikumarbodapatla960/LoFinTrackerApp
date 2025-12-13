// In ...ui.navigation/Screen.kt
package com.skai.lofintrackerapp.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Income : Screen("income", "Income")
    data object Expenses : Screen("expenses", "Expenses")
    data object Balance : Screen("balance", "Balance & Accounts")
    data object Loans : Screen("loans", "Loans")
    data object CreditCards : Screen("credit_cards", "Credit Cards")
    // --- ADD THIS ---
    data object Recurring : Screen("recurring", "Scheduled Payments")
    // ----------------
    data object Charts : Screen("charts", "Income vs. Expense")
    data object Settings : Screen("settings", "Settings")
}
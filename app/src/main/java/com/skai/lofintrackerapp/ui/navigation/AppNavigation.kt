// In ...ui.navigation/AppNavigation.kt
package com.skai.lofintrackerapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.skai.lofintrackerapp.ui.screens.*
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Income.route) { IncomeScreen(viewModel) }
        composable(Screen.Expenses.route) { ExpensesScreen(viewModel) }
        composable(Screen.Balance.route) { BalanceScreen(viewModel) }
        composable(Screen.Loans.route) { LoansScreen(viewModel) }
        composable(Screen.CreditCards.route) { CreditCardScreen(viewModel) }
        // --- ADD THIS LINE ---
        composable(Screen.Recurring.route) { RecurringPaymentsScreen(viewModel) }
        // --------------------
        composable(Screen.Charts.route) { ChartsScreen(viewModel) }
        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
    }
}
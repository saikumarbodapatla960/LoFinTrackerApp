package com.skai.lofintrackerapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.skai.lofintrackerapp.ui.AppShell
import com.skai.lofintrackerapp.ui.screens.*
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    // Get User Name
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    // Title Logic
    val title = when (currentRoute) {
        Screen.Dashboard.route -> "Dashboard" // Title is just "Dashboard" now
        Screen.Income.route -> "Income"
        Screen.Expenses.route -> "Expenses"
        Screen.Balance.route -> "Balance"
        Screen.Loans.route -> "Loans"
        Screen.CreditCards.route -> "Credit Cards"
        Screen.Recurring.route -> "Recurring Payments"
        Screen.Settings.route -> "Settings"
        Screen.Charts.route -> "Charts"
        else -> "LoFin Tracker"
    }

    AppShell(
        title = title,
        userName = userName, // <-- PASSING NAME HERE
        viewModel = viewModel,
        navController = navController,
        drawerState = drawerState,
        currentRoute = currentRoute
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel, navController) }
            composable(Screen.Income.route) { IncomeScreen(viewModel) }
            composable(Screen.Expenses.route) { ExpensesScreen(viewModel) }
            composable(Screen.Balance.route) { BalanceScreen(viewModel) }
            composable(Screen.Loans.route) { LoansScreen(viewModel) }
            composable(Screen.CreditCards.route) { CreditCardScreen(viewModel) }
            composable(Screen.Recurring.route) { RecurringPaymentsScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
            composable(Screen.Charts.route) { ChartsScreen(viewModel) }
        }
    }
}
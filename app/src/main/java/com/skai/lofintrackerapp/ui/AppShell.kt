// In ...ui/AppShell.kt
package com.skai.lofintrackerapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skai.lofintrackerapp.ui.navigation.AppNavigation
import com.skai.lofintrackerapp.ui.navigation.Screen
import com.skai.lofintrackerapp.ui.screens.TransactionFormDialog
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

val drawerItems = listOf(
    Screen.Dashboard,
    Screen.Income,
    Screen.Expenses,
    Screen.Balance,
    Screen.Loans,
    Screen.CreditCards,
    Screen.Recurring,
    Screen.Charts,
    Screen.Settings
)

fun getIconForScreen(screen: Screen): ImageVector {
    return when (screen) {
        Screen.Dashboard -> Icons.Default.Dashboard
        Screen.Income -> Icons.Default.ArrowUpward
        Screen.Expenses -> Icons.Default.ArrowDownward
        Screen.Balance -> Icons.Default.AccountBalance
        Screen.Loans -> Icons.Default.Savings
        Screen.CreditCards -> Icons.Default.CreditCard
        Screen.Recurring -> Icons.Default.EventRepeat
        Screen.Charts -> Icons.Default.BarChart
        Screen.Settings -> Icons.Default.Settings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    viewModel: MainViewModel,
    userName: String
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = drawerItems.find { it.route == currentRoute } ?: Screen.Dashboard

    var showTransactionDialog by remember { mutableStateOf(false) }

    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // --- LOGO REMOVED FROM HERE ---
                Spacer(modifier = Modifier.height(24.dp)) // Just some spacing

                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(getIconForScreen(screen), contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(currentScreen.title)
                            if (currentScreen == Screen.Dashboard) {
                                Text(
                                    text = "Welcome, $userName!",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTransactionDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Transaction",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            AppNavigation(
                viewModel = viewModel,
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }

        if (showTransactionDialog) {
            TransactionFormDialog(
                accounts = accounts,
                loans = loans,
                creditCards = creditCards,
                onDismiss = { showTransactionDialog = false },
                onConfirm = { newTransaction ->
                    viewModel.insertTransaction(newTransaction)
                }
            )
        }
    }
}
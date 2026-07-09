package com.skai.lofintrackerapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.skai.lofintrackerapp.ui.navigation.Screen
import com.skai.lofintrackerapp.ui.screens.TransactionFormDialog
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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

val drawerItems = listOf(
    Screen.Dashboard, Screen.Income, Screen.Expenses, Screen.Balance,
    Screen.Loans, Screen.CreditCards, Screen.Recurring, Screen.Charts, Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    title: String,
    userName: String? = null,
    viewModel: MainViewModel,
    navController: NavController,
    drawerState: DrawerState,
    currentRoute: String?,
    content: @Composable (PaddingValues) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showTransactionDialog by remember { mutableStateOf(false) }

    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(getIconForScreen(screen), contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = true
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
                            Text(text = title, style = MaterialTheme.typography.titleLarge)
                            if (currentRoute == Screen.Dashboard.route && !userName.isNullOrEmpty()) {
                                Text(
                                    text = "Welcome, $userName !",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTransactionDialog = true }) {
                            Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                )
            },
            content = content
        )

        if (showTransactionDialog) {
            TransactionFormDialog(
                accounts = accounts,
                loans = loans,
                creditCards = creditCards,
                onDismiss = { showTransactionDialog = false },
                onConfirm = { viewModel.insertTransaction(it) },
                onAddScheduled = { viewModel.insertScheduledTransaction(it) } // <-- ADDED
            )
        }
    }
}
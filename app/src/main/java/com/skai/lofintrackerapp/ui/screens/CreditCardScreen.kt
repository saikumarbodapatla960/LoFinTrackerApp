// In ...ui/screens/CreditCardScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.db.CreditCard
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ConfirmationDialog
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CreditCardScreen(viewModel: MainViewModel) {

    // --- Data Collection ---
    val cards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val totalDebt by viewModel.totalCreditCardDebt.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle() // For transaction list names

    // --- Filter Data ---
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedCreditCardIds by viewModel.selectedCreditCardIds.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredCreditCardTransactions.collectAsStateWithLifecycle()

    // --- Local UI State ---
    // Card Editing
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CreditCard?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<CreditCard?>(null) }

    // Transaction Editing
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    // Needed for Transaction Form
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()

    // Filter Items
    val filterItems = remember(cards) {
        cards.map { FilterSelectionItem(id = it.id, name = it.name) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    cardToEdit = null
                    showAddOrEditDialog = true
                },
                containerColor = FabGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Credit Card")
            }
        },
        // --- FIX: REMOVE EXTRA TOP PADDING ---
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->

        // --- LAYOUT FIX ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp) // Only horizontal padding
        ) {

            // 1. Total Debt Card
            ReusableTotalCard(
                title = "Total Credit Card Debt",
                totalAmount = totalDebt,
                cardColor = Color(0xFFFDECEA), // Light Red
                textColor = Color(0xFFF44336), // Dashboard Red
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Your Cards", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Cards List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.forEach { card ->
                    CreditCardRow(
                        card = card,
                        onEdit = {
                            cardToEdit = card
                            showEditConfirmation = true
                        },
                        onDelete = {
                            showDeleteConfirmation = card
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Filters
            FilterControls(
                startDate = startDate,
                endDate = endDate,
                onStartDateSelected = { viewModel.onStartDateChange(it) },
                onEndDateSelected = { viewModel.onEndDateChange(it) },
                viewModel = viewModel,
                filterTitle = "Credit Cards",
                allItems = filterItems,
                selectedIds = selectedCreditCardIds,
                onItemToggle = { viewModel.toggleCreditCardId(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Transaction List
            Text(text = "Recent Card Transactions", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            TransactionList(
                transactions = filteredTransactions,
                accounts = accounts,
                onEdit = { transaction ->
                    transactionToEdit = transaction
                    showTransactionDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- DIALOGS ---

        // Card Add/Edit
        if (showAddOrEditDialog) {
            CreditCardFormDialog(
                cardToEdit = cardToEdit,
                onDismiss = {
                    showAddOrEditDialog = false
                    cardToEdit = null
                },
                onConfirm = { card ->
                    if (cardToEdit == null) {
                        viewModel.insertCreditCard(card)
                    } else {
                        viewModel.updateCreditCard(card)
                    }
                    showAddOrEditDialog = false
                    cardToEdit = null
                }
            )
        }

        // Edit Confirm
        if (showEditConfirmation && cardToEdit != null) {
            ConfirmationDialog(
                title = "Edit Credit Card",
                message = "Are you sure you want to edit '${cardToEdit!!.name}'?",
                icon = Icons.Default.Edit,
                confirmText = "Edit",
                onDismiss = {
                    showEditConfirmation = false
                    cardToEdit = null
                },
                onConfirm = {
                    showAddOrEditDialog = true
                    showEditConfirmation = false
                }
            )
        }

        // Delete Confirm
        if (showDeleteConfirmation != null) {
            ConfirmationDialog(
                title = "Delete Credit Card",
                message = "Are you sure you want to delete '${showDeleteConfirmation!!.name}'? This cannot be undone.",
                icon = Icons.Default.Delete,
                confirmText = "Delete",
                onDismiss = { showDeleteConfirmation = null },
                onConfirm = {
                    viewModel.deleteCreditCard(showDeleteConfirmation!!)
                    showDeleteConfirmation = null
                }
            )
        }

        // Transaction Edit
        if (showTransactionDialog) {
            TransactionFormDialog(
                transactionToEdit = transactionToEdit,
                accounts = accounts,
                loans = loans,
                creditCards = cards, // Pass the cards list
                onDismiss = {
                    showTransactionDialog = false
                    transactionToEdit = null
                },
                onConfirm = { transaction ->
                    if (transactionToEdit == null) {
                        viewModel.insertTransaction(transaction)
                    } else {
                        viewModel.updateTransaction(transactionToEdit!!, transaction)
                    }
                },
                onDelete = { transaction ->
                    viewModel.deleteTransaction(transaction)
                }
            )
        }
    }
}

@Composable
private fun CreditCardRow(
    card: CreditCard,
    onEdit: (CreditCard) -> Unit,
    onDelete: (CreditCard) -> Unit
) {
    val formattedOwed = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(card.amountOwed)
    val formattedLimit = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(card.limit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = card.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Limit: $formattedLimit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Due: ${card.dueDate}th | Stmt: ${card.statementDate}th",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formattedOwed,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (card.amountOwed > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { onEdit(card) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onDelete(card) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
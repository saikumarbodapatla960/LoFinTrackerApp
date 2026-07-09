package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.skai.lofintrackerapp.ui.common.formatCurrency
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun CreditCardScreen(viewModel: MainViewModel) {
    val cards by viewModel.allCreditCards.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalDebt by viewModel.totalCreditCardDebt.collectAsStateWithLifecycle(initialValue = 0.0)
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = "INR")
    val isSortDesc by viewModel.sortDescending.collectAsStateWithLifecycle(initialValue = true)

    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedCreditCardIds by viewModel.selectedCreditCardIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val filteredTransactions by viewModel.filteredCreditCardTransactions.collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CreditCard?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<CreditCard?>(null) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    val loans by viewModel.allLoans.collectAsStateWithLifecycle(initialValue = emptyList())

    val filterItems = remember(cards) { cards.map { FilterSelectionItem(id = it.id, name = it.name) } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { cardToEdit = null; showAddOrEditDialog = true }, containerColor = FabGreen) {
                Icon(Icons.Default.Add, "Add Credit Card")
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // FIX: Remove double padding
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            ReusableTotalCard("Total Credit Card Debt", totalDebt, currency, Color(0xFFFDECEA), Color(0xFFF44336))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Cards", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.forEach { card ->
                    CreditCardRow(card, currency, { cardToEdit = card; showEditConfirmation = true }, { showDeleteConfirmation = card })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Card Transactions", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { viewModel.toggleSortOrder() }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = if (isSortDesc) "Newest added first" else "Oldest added first",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            TransactionList(
                transactions = filteredTransactions, 
                accounts = accounts, 
                creditCards = cards, 
                currencyCode = currency, 
                onEdit = { transactionToEdit = it; showTransactionDialog = true }
            )
            Spacer(modifier = Modifier.height(64.dp))
        }

        if (showAddOrEditDialog) { CreditCardFormDialog(cardToEdit, { showAddOrEditDialog = false; cardToEdit = null }, { if (cardToEdit == null) viewModel.insertCreditCard(it) else viewModel.updateCreditCard(it); showAddOrEditDialog = false; cardToEdit = null }) }
        if (showEditConfirmation && cardToEdit != null) { ConfirmationDialog("Edit Credit Card", "Edit '${cardToEdit!!.name}'?", Icons.Default.Edit, "Edit", { showAddOrEditDialog = true; showEditConfirmation = false }, { showEditConfirmation = false; cardToEdit = null }) }
        if (showDeleteConfirmation != null) { ConfirmationDialog("Delete Credit Card", "Delete '${showDeleteConfirmation!!.name}'?", Icons.Default.Delete, "Delete", { viewModel.deleteCreditCard(showDeleteConfirmation!!); showDeleteConfirmation = null }, { showDeleteConfirmation = null }) }
        if (showTransactionDialog) { 
            TransactionFormDialog(
                transactionToEdit = transactionToEdit, 
                accounts = accounts, 
                loans = loans, 
                creditCards = cards, 
                onDismiss = { showTransactionDialog = false; transactionToEdit = null }, 
                onConfirm = { if (transactionToEdit == null) viewModel.insertTransaction(it) else viewModel.updateTransaction(transactionToEdit!!, it) }, 
                onDelete = { viewModel.deleteTransaction(it) },
                onAddScheduled = { viewModel.insertScheduledTransaction(it) }
            ) 
        }
    }
}

@Composable
private fun CreditCardRow(card: CreditCard, currency: String, onEdit: (CreditCard) -> Unit, onDelete: (CreditCard) -> Unit) {
    val formattedOwed = formatCurrency(card.amountOwed, currency)
    val formattedLimit = formatCurrency(card.limit, currency)
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(card.name, style = MaterialTheme.typography.titleMedium)
                Text("Limit: $formattedLimit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Statement Day: ${card.statementDate}${getDaySuffix(card.statementDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Due Day: ${card.dueDate}${getDaySuffix(card.dueDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formattedOwed, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (card.amountOwed > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = { onEdit(card) }) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { onDelete(card) }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
        }
    }
}

private fun getDaySuffix(day: Int): String {
    return if (day in 11..13) "th"
    else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

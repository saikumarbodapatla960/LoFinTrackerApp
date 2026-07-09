package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.skai.lofintrackerapp.data.db.ScheduledTransaction
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.data.db.TransactionType
import com.skai.lofintrackerapp.ui.common.formatCurrency
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RecurringPaymentsScreen(viewModel: MainViewModel) {
    val scheduledItems by viewModel.allScheduledTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val loans by viewModel.allLoans.collectAsStateWithLifecycle(initialValue = emptyList())
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle(initialValue = emptyList())
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = "INR")

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ScheduledTransaction?>(null) }

    var showPayDialog by remember { mutableStateOf(false) }
    var transactionToPreFill by remember { mutableStateOf<Transaction?>(null) }
    var itemBeingPaid by remember { mutableStateOf<ScheduledTransaction?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                itemToEdit = null
                showAddDialog = true 
            }, containerColor = FabGreen) {
                Icon(Icons.Default.Add, "Add Scheduled")
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // FIX: Remove double padding
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Scheduled Payments", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (scheduledItems.isEmpty()) {
                Text("No recurring payments set up. Click '+' to add one!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scheduledItems) { item ->
                    ScheduledItemRow(
                        item = item,
                        accountName = accounts.find { it.id == item.accountId }?.name,
                        cardName = creditCards.find { it.id == item.creditCardId }?.name,
                        currencyCode = currency,
                        onEdit = { itemToEdit = item; showAddDialog = true },
                        onDelete = { viewModel.deleteScheduledTransaction(item) },
                        onPay = {
                            val preFilled = Transaction(
                                type = item.type,
                                amount = item.amount,
                                category = item.category,
                                accountId = item.accountId,
                                creditCardId = item.creditCardId,
                                loanId = item.loanId,
                                paymentMode = item.paymentMode,
                                date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                description = item.description
                            )
                            transactionToPreFill = preFilled
                            itemBeingPaid = item
                            showPayDialog = true
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddDialog) {
        RecurringTransactionFormDialog(
            itemToEdit = itemToEdit,
            accounts = accounts,
            loans = loans,
            creditCards = creditCards,
            onDismiss = { showAddDialog = false; itemToEdit = null },
            onConfirm = {
                if (itemToEdit == null) viewModel.insertScheduledTransaction(it)
                else viewModel.updateScheduledTransaction(it)
            }
        )
    }

    if (showPayDialog && transactionToPreFill != null) {
        TransactionFormDialog(
            transactionToEdit = transactionToPreFill,
            accounts = accounts,
            loans = loans,
            creditCards = creditCards,
            onDismiss = { showPayDialog = false },
            onConfirm = { tx ->
                val error = viewModel.insertTransaction(tx)
                if (error == null) {
                    val nextDate = try {
                        val currentDue = LocalDate.parse(itemBeingPaid!!.nextDueDate)
                        when (itemBeingPaid!!.frequency) {
                            "Daily" -> currentDue.plusDays(1).toString()
                            "Weekly" -> currentDue.plusWeeks(1).toString()
                            "Yearly" -> currentDue.plusYears(1).toString()
                            else -> currentDue.plusMonths(1).toString()
                        }
                    } catch (e: Exception) {
                        LocalDate.now().plusMonths(1).toString()
                    }
                    viewModel.updateScheduledTransaction(itemBeingPaid!!.copy(nextDueDate = nextDate))
                    showPayDialog = false
                }
                error
            },
            onAddScheduled = { viewModel.insertScheduledTransaction(it) }
        )
    }
}

@Composable
fun ScheduledItemRow(
    item: ScheduledTransaction,
    accountName: String?,
    cardName: String?,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPay: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val source = accountName ?: cardName ?: "No Source"
                Text("Source: $source | ${item.frequency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Next Due: ${item.nextDueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (item.amount > 0.0) formatCurrency(item.amount, currencyCode) else "Variable Amount",
                    color = if(item.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row {
                IconButton(onClick = onPay) { Icon(Icons.Default.CheckCircle, "Pay Now", tint = Color(0xFF4CAF50)) }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
            }
        }
    }
}

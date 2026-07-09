package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skai.lofintrackerapp.data.db.Account
import com.skai.lofintrackerapp.data.db.CreditCard
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.data.db.TransactionType
import com.skai.lofintrackerapp.ui.common.formatDateForDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.skai.lofintrackerapp.ui.common.formatCurrency

@Composable
fun TransactionList(
    transactions: List<Transaction>,
    accounts: List<Account>,
    creditCards: List<CreditCard>,
    currencyCode: String,
    onEdit: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val accountMap = remember(accounts) {
        accounts.associateBy({ acc: Account -> acc.id }, { acc: Account -> acc.name })
    }
    val cardMap = remember(creditCards) {
        creditCards.associateBy({ card: CreditCard -> card.id }, { card: CreditCard -> card.name })
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        transactions.forEach { transaction ->
            val sourceName = when {
                transaction.category == "Credit Card Payment" && transaction.accountId != null && transaction.creditCardId != null -> {
                    "${accountMap[transaction.accountId] ?: "Unknown Account"} -> ${cardMap[transaction.creditCardId] ?: "Unknown Card"}"
                }
                transaction.creditCardId != null -> cardMap[transaction.creditCardId] ?: "Unknown Card"
                transaction.accountId != null -> accountMap[transaction.accountId] ?: "Unknown Account"
                else -> "Unknown"
            }
            TransactionRow(
                transaction = transaction,
                sourceName = sourceName,
                currencyCode = currencyCode,
                onEdit = { onEdit(transaction) }
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, sourceName: String, currencyCode: String, onEdit: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val sign = if (isIncome) "+" else "-"
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val formattedAmount = formatCurrency(transaction.amount, currencyCode)

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = amountColor, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "$sourceName | ${formatDateForDisplay(transaction.date)} | ${formatTransactionTime(transaction.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.description.isNotBlank()) Text(text = transaction.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "$sign$formattedAmount", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = amountColor, modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun formatTransactionTime(createdAt: Long): String {
    if (createdAt <= 0L) return "time not recorded"
    return SimpleDateFormat("hh:mm a", Locale.US).format(Date(createdAt))
}

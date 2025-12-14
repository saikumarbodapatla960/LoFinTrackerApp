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
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.data.db.TransactionType
import com.skai.lofintrackerapp.ui.common.formatDateForDisplay
import com.skai.lofintrackerapp.ui.common.formatCurrency

@Composable
fun TransactionList(
    transactions: List<Transaction>,
    accounts: List<Account>,
    currencyCode: String, // <-- Added
    onEdit: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val accountMap = remember(accounts) { accounts.associateBy({ it.id }, { it.name }) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        transactions.forEach { transaction ->
            TransactionRow(
                transaction = transaction,
                accountName = accountMap[transaction.accountId] ?: "Unknown",
                currencyCode = currencyCode,
                onEdit = { onEdit(transaction) }
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, accountName: String, currencyCode: String, onEdit: () -> Unit) {
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
                Text(text = "$accountName | ${formatDateForDisplay(transaction.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.description.isNotBlank()) Text(text = transaction.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "$sign$formattedAmount", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = amountColor, modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
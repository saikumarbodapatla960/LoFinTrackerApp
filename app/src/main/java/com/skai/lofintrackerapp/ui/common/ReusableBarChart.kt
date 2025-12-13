// In ...ui.common/ReusableBarChart.kt
package com.skai.lofintrackerapp.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight // <-- ADDED THIS
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.background // <-- THIS WAS THE MISSING IMPORT

@Composable
fun ReusableBarChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)

    // Find the larger value to set the scale
    val maxAmount = maxOf(income, expense, 1.0) // Avoid dividing by zero

    Row(
        modifier = modifier.height(250.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // --- Income Bar ---
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(income),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(((income / maxAmount) * 200).dp.coerceAtLeast(4.dp))
                    .background(incomeColor) // <-- This will now work
            ) {}
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Income",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        // --- Expense Bar ---
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(expense),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(((expense / maxAmount) * 200).dp.coerceAtLeast(4.dp))
                    .background(expenseColor) // <-- This will now work
            ) {}
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Expense",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
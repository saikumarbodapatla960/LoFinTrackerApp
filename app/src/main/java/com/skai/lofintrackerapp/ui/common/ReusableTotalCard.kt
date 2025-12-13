// In ...ui/common/ReusableTotalCard.kt
package com.skai.lofintrackerapp.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier // <-- This was missing before
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReusableTotalCard(
    title: String,
    totalAmount: Double,
    cardColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier // <-- Added this parameter
) {
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalAmount)

    Card(
        // Apply the passed modifier here (this handles the top padding)
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}
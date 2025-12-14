// In ...ui/screens/SettingsScreen.kt
package com.skai.lofintrackerapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.ui.common.FormDropdown
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val currentCurrency by viewModel.currency.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // List of supported currencies
    val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // Theme Selector
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Theme", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentTheme == "light",
                        onClick = { viewModel.saveAppTheme("light") },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = currentTheme == "dark",
                        onClick = { viewModel.saveAppTheme("dark") },
                        label = { Text("Dark") }
                    )
                    FilterChip(
                        selected = currentTheme == "system",
                        onClick = { viewModel.saveAppTheme("system") },
                        label = { Text("System") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Preferences", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // Currency Selector
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Currency", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                // Simple dropdown reusing your component
                FormDropdown(
                    label = "Select Currency",
                    options = currencies,
                    onOptionSelected = { index -> viewModel.saveCurrency(currencies[index]) },
                    selectedTextValue = currentCurrency
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Support", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // Feedback Button
        SettingsItem(
            icon = Icons.Default.Feedback,
            title = "Send Feedback",
            onClick = {
                // REPLACE THIS LINK WITH YOUR GOOGLE FORM LINK
                val formUrl = "https://forms.gle/RkVjy2FaMH4XGBPv9"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formUrl))
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
package com.skai.lofintrackerapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- PROFILE SECTION ---
        Text("Profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Person,
            title = "Name",
            subtitle = userName ?: "Not Set",
            onClick = { newName = userName ?: ""; showNameDialog = true }
        )

        // --- GENERAL SECTION ---
        Spacer(modifier = Modifier.height(24.dp))
        Text("General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Palette,
            title = "App Theme",
            subtitle = currentTheme.replaceFirstChar { it.uppercase() },
            onClick = { showThemeDialog = true }
        )

        SettingsItem(
            icon = Icons.Default.AttachMoney,
            title = "Currency",
            subtitle = currency,
            onClick = { showCurrencyDialog = true }
        )

        // --- SECURITY SECTION ---
        Spacer(modifier = Modifier.height(24.dp))
        Text("Security & Data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // App Lock Switch
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Fingerprint, "Security", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("App Lock", style = MaterialTheme.typography.bodyLarge)
                    Text("Require fingerprint/PIN on open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(
                checked = isAppLockEnabled,
                onCheckedChange = { viewModel.saveAppLockEnabled(it) }
            )
        }
        Divider()

        // CSV Export
        SettingsItem(
            icon = Icons.Default.FileDownload,
            title = "Export Data (CSV)",
            subtitle = "Backup your transactions to a file",
            onClick = { exportData(context, allTransactions) }
        )

        // --- SUPPORT SECTION (NEW) ---
        Spacer(modifier = Modifier.height(24.dp))
        Text("Support", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Star,
            title = "Rate & Feedback",
            subtitle = "Rate us on Play Store",
            onClick = { openPlayStoreListing(context) }
        )
    }

    // --- DIALOGS ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Name") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") }) },
            confirmButton = { TextButton(onClick = { viewModel.saveUserName(newName); showNameDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancel") } }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    listOf("system", "light", "dark").forEach { theme ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.saveAppTheme(theme); showThemeDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentTheme == theme, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(theme.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") } }
        )
    }

    // Currency Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    listOf("INR", "USD", "EUR", "GBP", "JPY").forEach { curr ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.saveCurrency(curr); showCurrencyDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currency == curr, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(curr)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider()
}

// --- HELPER FUNCTIONS ---

fun openPlayStoreListing(context: Context) {
    val packageName = context.packageName
    try {
        // Try opening the Play Store app directly
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: Exception) {
        // If Play Store app is missing, open in Browser
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}

fun exportData(context: Context, transactions: List<Transaction>) {
    try {
        val csvHeader = "Date,Amount,Type,Category,Description\n"
        val sb = StringBuilder()
        sb.append(csvHeader)
        transactions.forEach {
            val safeDesc = it.description.replace(",", " ")
            sb.append("${it.date},${it.amount},${it.type},${it.category},$safeDesc\n")
        }

        val filename = "lofin_backup_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, filename)
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share CSV Backup"))

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
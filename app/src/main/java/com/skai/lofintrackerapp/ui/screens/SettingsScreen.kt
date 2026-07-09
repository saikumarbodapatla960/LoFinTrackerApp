package com.skai.lofintrackerapp.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.FileOutputStream

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val reminderDays by viewModel.reminderDays.collectAsStateWithLifecycle()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val totalDebt by viewModel.totalDebt.collectAsStateWithLifecycle()
    
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) {
                viewModel.restoreFromJson(content)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- PROFILE ---
        Text("Profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Person,
            title = "Personal Name",
            subtitle = if (userName.isBlank()) "Tap to set name" else userName,
            onClick = { newName = userName; showNameDialog = true }
        )

        SettingsItem(
            icon = Icons.Default.Notifications,
            title = "Payment Reminders",
            subtitle = if (reminderDays == 0) "Notifying on the due day" else "Notifying $reminderDays days before",
            onClick = { showReminderDialog = true }
        )

        // --- BACKUP & TRANSFER ---
        Spacer(modifier = Modifier.height(24.dp))
        Text("Backup & Transfer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Non-readable system files for device migration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.CloudUpload,
            title = "Export Backup (JSON)",
            subtitle = "Save a system file to move your data",
            onClick = { exportBackup(context, viewModel.getBackupJson()) }
        )

        SettingsItem(
            icon = Icons.Default.CloudDownload,
            title = "Import & Restore (JSON)",
            subtitle = "Restore data from a previously saved backup",
            onClick = { importLauncher.launch("application/json") }
        )

        // --- FINANCIAL DOCUMENTS ---
        Spacer(modifier = Modifier.height(24.dp))
        Text("Financial Documents", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Human-readable files for accounting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.TableView,
            title = "Export to Excel (CSV)",
            subtitle = "Open your transactions in Google Sheets or Excel",
            onClick = { exportFinancialCSV(context, allTransactions) }
        )

        SettingsItem(
            icon = Icons.Default.PictureAsPdf,
            title = "Generate Statement (PDF)",
            subtitle = "Printable summary of accounts, debt, and history",
            onClick = { generateFinancialPdf(context, userName, allAccounts.sumOf { it.balance }, totalDebt, currency, allTransactions) }
        )

        // --- APPEARANCE & SECURITY ---
        Spacer(modifier = Modifier.height(24.dp))
        Text("App Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Palette,
            title = "App Theme",
            subtitle = currentTheme.replaceFirstChar { it.uppercase() },
            onClick = { showThemeDialog = true }
        )

        SettingsItem(
            icon = Icons.Default.AttachMoney,
            title = "Primary Currency",
            subtitle = currency,
            onClick = { showCurrencyDialog = true }
        )

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
                    Text("Require fingerprint or PIN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = isAppLockEnabled, onCheckedChange = { viewModel.saveAppLockEnabled(it) })
        }
    }

    // --- DIALOGS ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Set Name") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Full Name") }) },
            confirmButton = { TextButton(onClick = { viewModel.saveUserName(newName); showNameDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancel") } }
        )
    }

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

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Reminder Interval") },
            text = {
                Column {
                    listOf(0, 1, 2, 3, 5, 7).forEach { days ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.saveReminderDays(days); showReminderDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = reminderDays == days, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (days == 0) "On the actual due day" else "$days days before due date")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReminderDialog = false }) { Text("Cancel") } }
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
    HorizontalDivider()
}

// --- HELPER FUNCTIONS ---

fun exportBackup(context: Context, json: String) {
    try {
        val filename = "lofin_system_backup_${System.currentTimeMillis()}.json"
        val file = File(context.cacheDir, filename)
        file.writeText(json)
        shareFile(context, file, "application/json", "System Backup")
    } catch (e: Exception) { e.printStackTrace() }
}

fun exportFinancialCSV(context: Context, transactions: List<Transaction>) {
    try {
        val csvHeader = "Date,Amount,Type,Category,Description,InterestPaid\n"
        val sb = StringBuilder().append(csvHeader)
        transactions.forEach {
            sb.append("${it.date},${it.amount},${it.type},${it.category},${it.description.replace(",", " ")},${it.interestAmount}\n")
        }
        val file = File(context.cacheDir, "lofin_excel_report_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString())
        shareFile(context, file, "text/csv", "Financial Excel Report")
    } catch (e: Exception) { e.printStackTrace() }
}

fun generateFinancialPdf(context: Context, name: String, balance: Double, debt: Double, currency: String, txs: List<Transaction>) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        paint.textSize = 24f
        canvas.drawText("LoFin Tracker: Financial Statement", 50f, 60f, paint)
        
        paint.textSize = 14f
        canvas.drawText("Prepared for: $name", 50f, 100f, paint)
        canvas.drawText("Total Available Balance: $currency $balance", 50f, 130f, paint)
        canvas.drawText("Total Outstanding Debt: $currency $debt", 50f, 150f, paint)
        
        paint.textSize = 16f
        canvas.drawText("Recent Activity Summary", 50f, 200f, paint)
        
        var y = 230f
        paint.textSize = 10f
        txs.take(25).forEach {
            val amountStr = if (it.type.name == "INCOME") "+${it.amount}" else "-${it.amount}"
            canvas.drawText("${it.date} | ${it.category}: $currency $amountStr", 70f, y, paint)
            y += 20f
            if (y > 800) return@forEach // Basic overflow check
        }

        document.finishPage(page)
        val file = File(context.cacheDir, "lofin_financial_statement_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        shareFile(context, file, "application/pdf", "Financial PDF Statement")
    } catch (e: Exception) { e.printStackTrace() }
}

private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.skai.lofintrackerapp.R

@Composable
fun WelcomeScreen(onNameSubmitted: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isPolicyChecked by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }

    // Hardcoded Policy Text (Offline Friendly)
    val privacyPolicyText = """
This privacy policy governs your use of the "LoFin Tracker" mobile application ("Application") created by SKai Team.

Your Privacy is Our Priority

This Application is a 100% offline finance tracker. We believe that your personal financial data is private and should belong only to you.

Data We Do Not Collect

We do not collect, transmit, share, or store any of your personal information. All data you enter into the Application is stored securely and exclusively on your device's local database.

This includes:

Your name

Your accounts and balances

Your transactions

Your loans

Any other financial data you enter

Since your data is never sent to our servers or any third-party service, we have no access to it.

Data Storage

All information is stored locally on your device. If you uninstall the Application, all your data will be permanently deleted from the device (unless you use your device's own backup features).

Changes to This Privacy Policy

We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy in this location.

Contact Us

If you have any questions about this Privacy Policy, please feel free to contact us at skaiteamapps@gmail.com.
    """.trimIndent()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "LoFin Tracker",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Manage your finances offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Enter your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- CHECKBOX WITH DIALOG TRIGGER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = isPolicyChecked, onCheckedChange = { isPolicyChecked = it })
                Spacer(modifier = Modifier.width(8.dp))

                val annotatedString = buildAnnotatedString {
                    append("I agree to the ")
                    pushStringAnnotation(tag = "policy", annotation = "show")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append("Privacy Policy")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "policy", start = offset, end = offset)
                            .firstOrNull()?.let { showPolicyDialog = true }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onNameSubmitted(name) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = name.isNotBlank() && isPolicyChecked
            ) {
                Text("Get Started")
            }
        }
    }

    // --- THE IN-APP DIALOG ---
    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(privacyPolicyText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    isPolicyChecked = true // Auto-check on read (optional nice touch)
                    showPolicyDialog = false
                }) {
                    Text("Close & Agree")
                }
            }
        )
    }
}
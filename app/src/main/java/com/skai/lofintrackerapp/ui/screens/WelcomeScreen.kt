// In ...ui/screens/WelcomeScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.Image // <-- For your Logo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource // <-- For your Logo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skai.lofintrackerapp.R // <-- Make sure this matches your package

@Composable
fun WelcomeScreen(onNameSubmitted: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- LOGO ADDED HERE ---
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp) // Bigger size looks better
            )
            // -----------------------

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "LoFin Tracker",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your personal finance manager.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Please enter your name to get started:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { if (name.isNotBlank()) onNameSubmitted(name) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Start Tracking")
            }
        }
    }
}
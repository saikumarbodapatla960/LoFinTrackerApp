// In ...ui.screens/SettingsScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    var nameText by remember(userName) { mutableStateOf(userName ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Profile Section ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Profile", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        // Save button inside the text field
                        if (nameText != userName) {
                            Button(
                                onClick = { viewModel.saveUserName(nameText) },
                                modifier = Modifier.padding(end = 8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                )
            }
        }

        // --- Appearance Section ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThemeButton(
                        label = "Light",
                        icon = Icons.Default.LightMode,
                        selected = currentTheme == "light",
                        onClick = { viewModel.saveAppTheme("light") }
                    )
                    ThemeButton(
                        label = "Dark",
                        icon = Icons.Default.DarkMode,
                        selected = currentTheme == "dark",
                        onClick = { viewModel.saveAppTheme("dark") }
                    )
                    ThemeButton(
                        label = "System",
                        icon = Icons.Default.SettingsSystemDaydream,
                        selected = currentTheme == "system",
                        onClick = { viewModel.saveAppTheme("system") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
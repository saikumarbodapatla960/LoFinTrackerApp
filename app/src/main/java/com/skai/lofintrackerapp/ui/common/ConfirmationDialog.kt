// In ...ui.common/ConfirmationDialog.kt
package com.skai.lofintrackerapp.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    icon: ImageVector,
    confirmText: String, // <-- Make the button text a parameter
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = title) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm() // <-- Let the parent handle all logic
            }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
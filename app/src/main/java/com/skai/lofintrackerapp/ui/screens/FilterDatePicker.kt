// In ...ui.screens/FilterDatePicker.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// Helper to get today's date in millis (at UTC midnight)
private fun getTodayDateMillis(): Long {
    return LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

// Helper to convert LocalDate to millis
private fun LocalDate.toMillis(): Long {
    return this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

// Helper to convert millis to LocalDate
private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    selectableDates: SelectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return utcTimeMillis <= getTodayDateMillis()
        }
    }
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toMillis(),
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis ?: getTodayDateMillis()
                    onConfirm(selectedMillis.toLocalDate())
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            // --- THIS IS THE FIX ---
            // It now correctly calls the onDismiss() function
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
            // --- END OF FIX ---
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
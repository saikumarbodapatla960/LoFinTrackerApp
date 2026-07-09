package com.skai.lofintrackerapp.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDateRangePicker(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    viewModel: MainViewModel
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        initialSelectedEndDateMillis = initialEndDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    if (startMillis != null) {
                        val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                        val end = if (endMillis != null) {
                            Instant.ofEpochMilli(endMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                        } else {
                            start
                        }
                        onConfirm(start, end)
                    }
                }
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            // 1. Fix the "Select dates" title padding
            title = {
                Text(
                    text = "Select dates",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            // 2. Fix the huge text wrapping by defining a custom headline
            headline = {
                val startMillis = dateRangePickerState.selectedStartDateMillis
                val endMillis = dateRangePickerState.selectedEndDateMillis

                val displayText = if (startMillis != null && endMillis != null) {
                    val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                    val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                    "${start.format(formatter)} - ${end.format(formatter)}"
                } else if (startMillis != null) {
                    val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                    start.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                } else {
                    "Start - End"
                }

                Text(
                    text = displayText,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.headlineSmall, // Smaller font prevents wrapping
                    maxLines = 1
                )
            },
            showModeToggle = false
        )
    }
}
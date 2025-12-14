// In ...ui/screens/FilterControls.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skai.lofintrackerapp.ui.common.FilterDateRangePicker
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- CRITICAL DATA CLASS (Fixes errors in Loans/CreditCard screens) ---
data class FilterSelectionItem(val id: Long, val name: String)

// --- FIX: Added ExperimentalLayoutApi to suppress the FlowRow error ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterControls(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    viewModel: MainViewModel,

    // Mode 1: Text Categories (Income/Expense)
    allCategories: List<String>? = null,
    selectedCategories: Set<String>? = null,
    onCategoryToggle: ((String) -> Unit)? = null,

    // Mode 2: ID Items (Accounts/Loans/Cards)
    filterTitle: String? = null,
    allItems: List<FilterSelectionItem>? = null,
    selectedIds: Set<Long>? = null,
    onItemToggle: ((Long) -> Unit)? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (allCategories != null || allItems != null) {
            FilledTonalButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val count = (selectedCategories?.size ?: 0) + (selectedIds?.size ?: 0)
                Text(if (count > 0) "Filter ($count)" else "Filter")
            }
        }
    }

    if (showDatePicker) {
        FilterDateRangePicker(
            initialStartDate = startDate,
            initialEndDate = endDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                onStartDateSelected(start)
                onEndDateSelected(end)
                showDatePicker = false
            },
            viewModel = viewModel
        )
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = filterTitle ?: "Filter Categories",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Mode 1: String Categories
                if (allCategories != null && selectedCategories != null && onCategoryToggle != null) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allCategories.forEach { category ->
                            FilterChip(
                                selected = selectedCategories.contains(category),
                                onClick = { onCategoryToggle(category) },
                                label = { Text(category) }
                            )
                        }
                    }
                }

                // Mode 2: ID Items
                if (allItems != null && selectedIds != null && onItemToggle != null) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allItems.forEach { item ->
                            FilterChip(
                                selected = selectedIds.contains(item.id),
                                onClick = { onItemToggle(item.id) },
                                label = { Text(item.name) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
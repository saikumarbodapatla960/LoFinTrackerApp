// In ...ui.screens/FilterControls.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// A simple data class for our new filter
data class FilterSelectionItem(
    val id: Long,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterControls(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    viewModel: MainViewModel,

    // For Category filters
    allCategories: List<String>? = null,
    selectedCategories: Set<String>? = null,
    onCategoryToggle: ((String) -> Unit)? = null,

    // For Account/Loan filters
    filterTitle: String? = null,
    allItems: List<FilterSelectionItem>? = null,
    selectedIds: Set<Long>? = null,
    onItemToggle: ((Long) -> Unit)? = null
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showItemDropdown by remember { mutableStateOf(false) } // For Accounts/Loans

    val quickFilters = mapOf(
        "Today" to { viewModel.setFilterToToday() },
        "This Month" to { viewModel.setFilterToThisMonth() },
        "Last 30 Days" to { viewModel.setFilterToLast30Days() },
        "This Year" to { viewModel.setFilterToThisYear() }
    )

    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Filter Transactions", style = MaterialTheme.typography.titleLarge)

            // Date Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // "From" Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("From:", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(startDate.format(formatter))
                    }
                }

                // "To" Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("To:", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(endDate.format(formatter))
                    }
                }
            }

            // Quick Select Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickFilters.toList()) { (title, onClick) ->
                    FilterChip(
                        selected = false,
                        onClick = onClick,
                        label = { Text(title) }
                    )
                }
            }

            // Category Filter (Optional)
            if (allCategories != null && selectedCategories != null && onCategoryToggle != null) {
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedButton(
                        onClick = { showCategoryDropdown = true },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    ) {
                        val text = if (selectedCategories.isEmpty()) "Categories (All)"
                        else "Categories (${selectedCategories.size}/${allCategories.size})"
                        Text(text = text, modifier = Modifier.weight(1f))
                        Icon(if (showCategoryDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, "Toggle")
                    }

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        allCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedCategories.isEmpty() || selectedCategories.contains(category),
                                            onCheckedChange = null
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(category)
                                    }
                                },
                                onClick = { onCategoryToggle(category) }
                            )
                        }
                    }
                }
            }

            // Account/Loan Filter (Optional)
            if (filterTitle != null && allItems != null && selectedIds != null && onItemToggle != null) {
                ExposedDropdownMenuBox(
                    expanded = showItemDropdown,
                    onExpandedChange = { showItemDropdown = !showItemDropdown }
                ) {
                    OutlinedButton(
                        onClick = { showItemDropdown = true },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    ) {
                        val text = if (selectedIds.isEmpty()) "$filterTitle (All)"
                        else "$filterTitle (${selectedIds.size}/${allItems.size})"
                        Text(text = text, modifier = Modifier.weight(1f))
                        Icon(if (showItemDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, "Toggle")
                    }

                    ExposedDropdownMenu(
                        expanded = showItemDropdown,
                        onDismissRequest = { showItemDropdown = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        allItems.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedIds.isEmpty() || selectedIds.contains(item.id),
                                            onCheckedChange = null
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(item.name)
                                    }
                                },
                                onClick = { onItemToggle(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show Date Pickers
    if (showStartDatePicker) {
        FilterDatePicker(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { newDate ->
                onStartDateSelected(newDate)
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        FilterDatePicker(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { newDate ->
                onEndDateSelected(newDate)
                showEndDatePicker = false
            }
        )
    }
}
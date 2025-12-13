// In ...ui.viewmodel/ViewModelFactory.kt
package com.skai.lofintrackerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skai.lofintrackerapp.data.UserPreferences // <-- ADDED
import com.skai.lofintrackerapp.data.repository.AppRepository
import java.lang.IllegalArgumentException

class MainViewModelFactory(
    private val repository: AppRepository,
    private val userPreferences: UserPreferences // <-- ADDED PARAMETER
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, userPreferences) as T // <-- Pass it here
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
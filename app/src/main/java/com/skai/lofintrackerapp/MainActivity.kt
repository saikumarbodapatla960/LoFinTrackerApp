// In ...MainActivity.kt
package com.skai.lofintrackerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue // <-- THIS IS THE CRITICAL MISSING IMPORT
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.UserPreferences
import com.skai.lofintrackerapp.ui.AppShell
import com.skai.lofintrackerapp.ui.navigation.Screen
import com.skai.lofintrackerapp.ui.screens.TutorialScreen
import com.skai.lofintrackerapp.ui.screens.WelcomeScreen
import com.skai.lofintrackerapp.ui.theme.LoFinTrackerAppTheme
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val userPreferences by lazy { UserPreferences(this) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as LoFinApp).repository, userPreferences)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handle permission result if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        setContent {
            // We explicitly tell the compiler the type to avoid inference errors
            val userName: String? by viewModel.userName.collectAsStateWithLifecycle()
            val appTheme: String by viewModel.appTheme.collectAsStateWithLifecycle()
            val hasSeenTutorial: Boolean by viewModel.hasSeenTutorial.collectAsStateWithLifecycle()

            val startDestination = if (intent?.getStringExtra("navigate_to") == "recurring") {
                Screen.Recurring.route
            } else {
                Screen.Dashboard.route
            }

            val darkTheme = when (appTheme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            LoFinTrackerAppTheme(darkTheme = darkTheme) {
                when {
                    userName == null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    userName == "" -> {
                        WelcomeScreen(onNameSubmitted = { name -> viewModel.saveUserName(name) })
                    }
                    !hasSeenTutorial -> {
                        TutorialScreen(onGetStarted = { viewModel.saveHasSeenTutorial() })
                    }
                    else -> {
                        AppShell(viewModel = viewModel, userName = userName!!)
                    }
                }
            }
        }
    }
}
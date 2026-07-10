package com.skai.lofintrackerapp

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.skai.lofintrackerapp.ui.navigation.AppNavigation
import com.skai.lofintrackerapp.ui.screens.TutorialScreen
import com.skai.lofintrackerapp.ui.screens.WelcomeScreen
import com.skai.lofintrackerapp.ui.theme.LoFinTrackerAppTheme
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_LoFinTrackerApp)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkForAppUpdates()
        requestNotificationPermission()

        setContent {
            val app = application as LoFinApp
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(app.repository, app.userPreferences))
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle("system")

            LoFinTrackerAppTheme(darkTheme = when (appTheme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }) {
                MainApp(viewModel)
            }
        }
    }

    private fun checkForAppUpdates() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, 1001)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Composable
    private fun MainApp(viewModel: MainViewModel) {
        val navController = rememberNavController()
        val userName by viewModel.userName.collectAsStateWithLifecycle()
        val hasSeenTutorial by viewModel.hasSeenTutorial.collectAsStateWithLifecycle()
        val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()

        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            // Wait until the flows have emitted their first real value.
            combine(
                viewModel.userName,
                viewModel.hasSeenTutorial,
                viewModel.isAppLockEnabled
            ) { _, _, _ -> false }.first { !it } // This will emit `false` as soon as all flows have an item, then complete.
            isLoading = false
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (isLoading) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                var isUnlocked by remember(isAppLockEnabled) { mutableStateOf(!isAppLockEnabled) }

                if (isUnlocked) {
                    when {
                        userName.isBlank() -> WelcomeScreen { viewModel.saveUserName(it) }
                        !hasSeenTutorial -> TutorialScreen(viewModel) { viewModel.saveHasSeenTutorial() }
                        else -> AppNavigation(viewModel, navController)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Locked", style = MaterialTheme.typography.headlineMedium)
                    }
                    LaunchedEffect(Unit) {
                        authenticateUser { success ->
                            if (success) isUnlocked = true else finish()
                        }
                    }
                }
            }
        }
    }

    private fun authenticateUser(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        onResult(false)
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("LoFin Tracker Locked")
            .setSubtitle("Use fingerprint or PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

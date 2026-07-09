package com.skai.lofintrackerapp

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.skai.lofintrackerapp.data.UserPreferences
import com.skai.lofintrackerapp.ui.navigation.AppNavigation
import com.skai.lofintrackerapp.ui.screens.TutorialScreen
import com.skai.lofintrackerapp.ui.screens.WelcomeScreen
import com.skai.lofintrackerapp.ui.theme.LoFinTrackerAppTheme
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModelFactory
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, 1001)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val userPreferences = UserPreferences(this)
        var isAppLockEnabled = false
        runBlocking { isAppLockEnabled = userPreferences.isAppLockEnabled.first() }

        setContent {
            val app = application as LoFinApp
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(app.repository, userPreferences))
            val navController = rememberNavController()
            
            // Collect all states here for consistency
            val userName by viewModel.userName.collectAsStateWithLifecycle()
            val hasSeenTutorial by viewModel.hasSeenTutorial.collectAsStateWithLifecycle()
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()

            var isUnlocked by remember { mutableStateOf(!isAppLockEnabled) }

            LoFinTrackerAppTheme(darkTheme = when(appTheme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (isUnlocked) {
                        when {
                            userName.isBlank() && !hasSeenTutorial -> WelcomeScreen {
                                viewModel.saveUserName(it)
                                viewModel.saveHasSeenTutorial()
                            }
                            !hasSeenTutorial -> TutorialScreen(viewModel) { viewModel.saveHasSeenTutorial() }
                            else -> AppNavigation(viewModel, navController)
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Locked", style = MaterialTheme.typography.headlineMedium)
                        }

                        LaunchedEffect(Unit) {
                            authenticateUser { success ->
                                if (success) {
                                    isUnlocked = true
                                } else {
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun authenticateUser(onResult: (Boolean) -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuthenticate = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(true)
            return
        }

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
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("LoFin Tracker Locked")
            .setSubtitle("Use fingerprint or PIN")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

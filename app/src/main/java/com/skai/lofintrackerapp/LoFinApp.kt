package com.skai.lofintrackerapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skai.lofintrackerapp.data.UserPreferences
import com.skai.lofintrackerapp.data.db.AppDatabase
import com.skai.lofintrackerapp.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class LoFinApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database, applicationContext) }
    val userPreferences by lazy { UserPreferences(this) }

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        setupWorker()
        recordAppInstallTimestamp()
    }

    private fun setupWorker() {
        val workRequest = PeriodicWorkRequestBuilder<PaymentReminderWorker>(
            8, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PaymentReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun recordAppInstallTimestamp() {
        applicationScope.launch {
            val timestamp = userPreferences.appInstallTimestamp.first()
            if (timestamp == null) {
                userPreferences.saveAppInstallTimestamp(System.currentTimeMillis())
            }
        }
    }
}

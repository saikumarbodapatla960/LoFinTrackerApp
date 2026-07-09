// In .../LoFinApp.kt
package com.skai.lofintrackerapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skai.lofintrackerapp.data.db.AppDatabase
import com.skai.lofintrackerapp.data.repository.AppRepository
import java.util.concurrent.TimeUnit

class LoFinApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.appDao(), applicationContext) }

    override fun onCreate() {
        super.onCreate()
        setupWorker()
    }

    private fun setupWorker() {
        // --- UPDATED INTERVAL: 8 HOURS ---
        val workRequest = PeriodicWorkRequestBuilder<PaymentReminderWorker>(
            8, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PaymentReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE, // Changed to UPDATE to apply the new 8-hour time immediately
            workRequest
        )
    }
}

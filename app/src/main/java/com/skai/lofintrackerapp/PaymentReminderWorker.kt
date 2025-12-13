// In .../PaymentReminderWorker.kt
package com.skai.lofintrackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skai.lofintrackerapp.data.db.AppDatabase
import com.skai.lofintrackerapp.data.db.ScheduledTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PaymentReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.appDao()

        val scheduledItems = dao.getAllScheduledTransactions().first()
        val today = LocalDate.now()

        // --- NEW LOGIC: CHECK FOR PAST AND PRESENT ---
        val dueItems = scheduledItems.filter {
            try {
                val dueDate = LocalDate.parse(it.nextDueDate) // Assumes YYYY-MM-DD
                // If date is today OR in the past (overdue), notify!
                !dueDate.isAfter(today)
            } catch (e: Exception) {
                false
            }
        }

        // --- NEW LOGIC: SEPARATE NOTIFICATIONS ---
        if (dueItems.isNotEmpty()) {
            createNotificationChannel() // Ensure channel exists
            dueItems.forEach { item ->
                sendNotification(item)
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scheduled Payments"
            val descriptionText = "Reminders for upcoming and overdue payments"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("scheduled_payments_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(item: ScheduledTransaction) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open the specific screen
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "recurring")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            item.id.toInt(), // Unique Request Code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Customize message based on if it's overdue or due today
        val dueDate = LocalDate.parse(item.nextDueDate)
        val today = LocalDate.now()
        val title = if (dueDate.isBefore(today)) "Overdue Payment!" else "Payment Due Today"
        val amountText = if (item.amount > 0) "Amount: ₹${item.amount}" else "Variable Amount"

        val notification = NotificationCompat.Builder(applicationContext, "scheduled_payments_channel")
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText("${item.description} - $amountText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use the Item ID as the Notification ID so they don't overwrite each other
        notificationManager.notify(item.id.toInt(), notification)
    }
}
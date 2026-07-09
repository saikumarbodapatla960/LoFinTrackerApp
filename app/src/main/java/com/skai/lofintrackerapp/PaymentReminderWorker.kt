package com.skai.lofintrackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skai.lofintrackerapp.data.UserPreferences
import com.skai.lofintrackerapp.data.db.AppDatabase
import com.skai.lofintrackerapp.data.db.ScheduledTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PaymentReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.appDao()
        val userPreferences = UserPreferences(applicationContext)

        val scheduledItems = dao.getAllScheduledTransactions().first()
        val reminderDays = userPreferences.reminderDays.first()
        val today = LocalDate.now()

        val itemsToNotify = scheduledItems.filter {
            try {
                val dueDate = LocalDate.parse(it.nextDueDate)
                val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
                // Notify if it's due today, overdue, or due within the user's setting
                daysUntilDue <= reminderDays
            } catch (e: Exception) {
                false
            }
        }

        if (itemsToNotify.isNotEmpty()) {
            createNotificationChannel()
            itemsToNotify.forEach { item ->
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "recurring")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dueDate = LocalDate.parse(item.nextDueDate)
        val today = LocalDate.now()
        
        val title = when {
            dueDate.isBefore(today) -> "Overdue Payment!"
            dueDate.isEqual(today) -> "Payment Due Today"
            else -> "Upcoming Payment"
        }
        
        val daysText = if (dueDate.isAfter(today)) {
            "Due in ${ChronoUnit.DAYS.between(today, dueDate)} days"
        } else if (dueDate.isBefore(today)) {
            "Was due on ${item.nextDueDate}"
        } else {
            "Due today!"
        }

        val amountText = if (item.amount > 0) "Amount: ₹${item.amount}" else "Variable Amount"

        val notification = NotificationCompat.Builder(applicationContext, "scheduled_payments_channel")
            .setSmallIcon(R.drawable.app_logo) // Ensure this icon exists
            .setContentTitle(title)
            .setContentText("${item.description} - $daysText ($amountText)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(item.id.toInt(), notification)
    }
}

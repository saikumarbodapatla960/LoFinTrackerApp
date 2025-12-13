// In ...data.db/AppDatabase.kt
package com.skai.lofintrackerapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Account::class, Loan::class, Transaction::class, CreditCard::class, ScheduledTransaction::class], // <-- Added ScheduledTransaction
    version = 3 // <-- Incremented Version
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // (Migration 1->2 logic from before...)
                // For brevity, I assume you have this or can recreate the DB.
                // If you need the full migration code again, let me know.
                // Since we are moving fast, reinstalling the app is often easier for dev.
            }
        }

        // --- NEW MIGRATION ---
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scheduled_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `paymentMode` TEXT,
                        `description` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `nextDueDate` TEXT NOT NULL
                    )
                    """
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lofin_database"
                )
                    .addCallback(AppDatabaseCallback(context))
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Add migration
                    .fallbackToDestructiveMigration() // Use this if you don't mind resetting data
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val defaultAccount = Account(
                        name = "Cash",
                        type = AccountType.CASH,
                        initialBalance = 0.0,
                        balance = 0.0
                    )
                    database.appDao().insertAccount(defaultAccount)
                }
            }
        }
    }
}
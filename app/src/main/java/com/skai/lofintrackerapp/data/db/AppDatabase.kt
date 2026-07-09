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
    entities = [Account::class, Loan::class, Transaction::class, CreditCard::class, ScheduledTransaction::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) {} }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `scheduled_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `accountId` INTEGER NOT NULL, `paymentMode` TEXT, `description` TEXT NOT NULL, `frequency` TEXT NOT NULL, `nextDueDate` TEXT NOT NULL)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE transactions_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, accountId INTEGER, paymentMode TEXT, loanId INTEGER, creditCardId INTEGER, date TEXT NOT NULL, description TEXT NOT NULL, FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE RESTRICT, FOREIGN KEY(loanId) REFERENCES loans(id) ON DELETE SET NULL, FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON DELETE RESTRICT)")
                db.execSQL("INSERT INTO transactions_new (id, type, amount, category, accountId, paymentMode, loanId, creditCardId, date, description) SELECT id, type, amount, category, accountId, paymentMode, loanId, creditCardId, date, description FROM transactions")
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Scheduled Transactions Update
                db.execSQL("CREATE TABLE scheduled_transactions_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, accountId INTEGER, creditCardId INTEGER, loanId INTEGER, paymentMode TEXT, description TEXT NOT NULL, frequency TEXT NOT NULL, nextDueDate TEXT NOT NULL, FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE SET NULL, FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON DELETE SET NULL, FOREIGN KEY(loanId) REFERENCES loans(id) ON DELETE SET NULL)")
                db.execSQL("INSERT INTO scheduled_transactions_new (id, type, amount, category, accountId, description, frequency, nextDueDate) SELECT id, type, amount, category, accountId, description, frequency, nextDueDate FROM scheduled_transactions")
                db.execSQL("DROP TABLE scheduled_transactions")
                db.execSQL("ALTER TABLE scheduled_transactions_new RENAME TO scheduled_transactions")
                
                // Missing columns for Request 7
                db.execSQL("ALTER TABLE loans ADD COLUMN totalInterestPaid REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE loans ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE loans ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE credit_cards ADD COLUMN totalInterestPaid REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN interestAmount REAL NOT NULL DEFAULT 0.0")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_transactions_accountId ON scheduled_transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_transactions_creditCardId ON scheduled_transactions(creditCardId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_transactions_loanId ON scheduled_transactions(loanId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_loanId ON transactions(loanId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_creditCardId ON transactions(creditCardId)")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE transactions SET creditCardId = loanId, loanId = NULL WHERE category = 'Credit Card Payment' AND loanId IS NOT NULL AND EXISTS (SELECT 1 FROM credit_cards WHERE credit_cards.id = transactions.loanId)")
                db.execSQL("UPDATE transactions SET loanId = NULL WHERE category = 'Credit Card Payment'")
                db.execSQL("UPDATE scheduled_transactions SET creditCardId = loanId, loanId = NULL WHERE category = 'Credit Card Payment' AND loanId IS NOT NULL AND EXISTS (SELECT 1 FROM credit_cards WHERE credit_cards.id = scheduled_transactions.loanId)")
                db.execSQL("UPDATE scheduled_transactions SET loanId = NULL WHERE category = 'Credit Card Payment'")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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
                    val defaultAccount = Account(name = "Cash", type = AccountType.CASH, initialBalance = 0.0, balance = 0.0)
                    database.appDao().insertAccount(defaultAccount)
                }
            }
        }
    }
}

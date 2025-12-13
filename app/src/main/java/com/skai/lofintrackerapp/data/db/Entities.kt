// In ...data.db/Entities.kt
package com.skai.lofintrackerapp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Double,
    var balance: Double
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lender: String,
    val initialAmount: Double,
    var remainingAmount: Double,
    val date: String
)

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val limit: Double = 0.0,
    var amountOwed: Double = 0.0,
    val statementDate: Int = 1,
    val dueDate: Int = 1
)

// --- NEW ENTITY ---
@Entity(tableName = "scheduled_transactions")
data class ScheduledTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double, // 0.0 if variable
    val category: String,
    val accountId: Long, // Default account
    val paymentMode: String?,
    val description: String,
    val frequency: String, // "Monthly", "Yearly", "Weekly"
    val nextDueDate: String // YYYY-MM-DD
)
// ------------------

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val accountId: Long,
    val paymentMode: String?,
    val loanId: Long?,
    val creditCardId: Long?,
    val date: String,
    val description: String
)
package com.skai.lofintrackerapp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class AccountType { BANK, CASH, SAVINGS, CURRENT, INVESTMENT, OTHER }

@Serializable
enum class TransactionType { EXPENSE, INCOME }

@Entity(tableName = "accounts")
@Serializable
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Double,
    var balance: Double
)

@Entity(tableName = "loans")
@Serializable
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lender: String,
    val initialAmount: Double,
    var remainingAmount: Double,
    val date: String,
    val interestRate: Double = 0.0,
    val isClosed: Boolean = false,
    val totalInterestPaid: Double = 0.0
)

@Entity(tableName = "credit_cards")
@Serializable
data class CreditCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val limit: Double = 0.0,
    var amountOwed: Double = 0.0,
    val statementDate: Int = 1,
    val dueDate: Int = 1,
    val totalInterestPaid: Double = 0.0
)

@Entity(
    tableName = "scheduled_transactions",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CreditCard::class, parentColumns = ["id"], childColumns = ["creditCardId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Loan::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.SET_NULL)
    ]
)
@Serializable
data class ScheduledTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double, 
    val category: String,
    val accountId: Long?,
    val creditCardId: Long?,
    val loanId: Long?,
    val paymentMode: String?,
    val description: String,
    val frequency: String, 
    val nextDueDate: String 
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Loan::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CreditCard::class, parentColumns = ["id"], childColumns = ["creditCardId"], onDelete = ForeignKey.RESTRICT)
    ]
)
@Serializable
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val accountId: Long?,
    val paymentMode: String?,
    val loanId: Long?,
    val creditCardId: Long?,
    val date: String,
    val description: String,
    val interestAmount: Double = 0.0
)
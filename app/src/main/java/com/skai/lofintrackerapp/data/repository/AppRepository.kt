package com.skai.lofintrackerapp.data.repository

import android.content.Context
import androidx.room.Transaction as RoomTransaction
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.skai.lofintrackerapp.PaymentReminderWorker
import com.skai.lofintrackerapp.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class AppRepository(
    private val appDao: AppDao,
    private val appContext: Context? = null
) {

    val allAccounts: Flow<List<Account>> = appDao.getAllAccounts()
    val allLoans: Flow<List<Loan>> = appDao.getAllLoans()
    val allTransactions: Flow<List<Transaction>> = appDao.getAllTransactions()
    val allCreditCards: Flow<List<CreditCard>> = appDao.getAllCreditCards()
    val allScheduledTransactions: Flow<List<ScheduledTransaction>> = appDao.getAllScheduledTransactions()

    suspend fun insertAccount(account: Account) { appDao.insertAccount(account) }
    suspend fun updateAccount(account: Account) { appDao.updateAccount(account) }
    suspend fun deleteAccount(account: Account) { appDao.deleteAccount(account) }
    suspend fun insertLoan(loan: Loan): Long = appDao.insertLoan(loan)
    suspend fun updateLoan(loan: Loan) { appDao.updateLoan(loan) }
    suspend fun deleteLoan(loan: Loan) { appDao.deleteLoan(loan) }
    suspend fun insertScheduledTransaction(tx: ScheduledTransaction) {
        appDao.insertScheduledTransaction(tx)
        enqueueImmediateReminderCheck()
    }
    suspend fun updateScheduledTransaction(tx: ScheduledTransaction) {
        appDao.updateScheduledTransaction(tx)
        enqueueImmediateReminderCheck()
    }
    suspend fun deleteScheduledTransaction(tx: ScheduledTransaction) { appDao.deleteScheduledTransaction(tx) }

    @RoomTransaction
    suspend fun insertLoanAndPayout(loan: Loan, payoutAccountId: Long) {
        val newLoanId = appDao.insertLoan(loan)
        val payoutTransaction = Transaction(
            type = TransactionType.INCOME,
            amount = loan.initialAmount,
            category = "Loan",
            accountId = payoutAccountId,
            paymentMode = null,
            loanId = newLoanId,
            creditCardId = null,
            date = loan.date,
            description = "Loan payout from ${loan.lender}",
            createdAt = System.currentTimeMillis()
        )
        insertTransactionAndUpdateBalances(payoutTransaction)
    }

    suspend fun insertCreditCard(card: CreditCard) {
        val cardId = appDao.insertCreditCard(card)
        upsertCreditCardSchedule(card.copy(id = cardId))
    }
    suspend fun updateCreditCard(card: CreditCard) {
        appDao.updateCreditCard(card)
        upsertCreditCardSchedule(card)
    }
    suspend fun deleteCreditCard(card: CreditCard) { appDao.deleteCreditCard(card) }

    @RoomTransaction
    suspend fun insertTransactionAndUpdateBalances(transaction: Transaction): String? {
        if (transaction.amount <= 0) return "Amount must be greater than zero."

        // Helper to validate a transaction without applying changes
        val validationError = validateTransaction(transaction)
        if (validationError != null) return validationError

        // Process interest calculation for debt payments (Request 7)
        val finalTransaction = transaction.withCalculatedInterest()
            .copy(createdAt = if (transaction.createdAt == 0L) System.currentTimeMillis() else transaction.createdAt)

        appDao.insertTransaction(finalTransaction)
        applyBalanceUpdates(finalTransaction)
        return null
    }

    private suspend fun validateTransaction(transaction: Transaction, ignoredTransactionId: Long? = null): String? {
        if (transaction.amount <= 0) return "Amount must be greater than zero."

        val txDate = LocalDate.parse(transaction.date)

        if (transaction.type == TransactionType.EXPENSE) {
            when {
                transaction.category == "Credit Card Payment" -> {
                    val account = transaction.accountId?.let { appDao.getAccountById(it) } ?: return "Invalid bank account."
                    val card = transaction.creditCardId?.let { appDao.getCreditCardById(it) } ?: return "Invalid credit card."

                    if (card.amountOwed <= 0) return "Cannot pay bill for a card with no debt."
                    
                    val balanceAtDate = calculateBalanceAtDate(account.id, txDate, ignoredTransactionId)
                    if (balanceAtDate < transaction.amount) {
                        return "Insufficient funds in '${account.name}' on ${transaction.date}. Balance was ₹$balanceAtDate."
                    }
                }
                transaction.paymentMode == "Credit Card" -> {
                    val card = transaction.creditCardId?.let { appDao.getCreditCardById(it) } ?: return "Invalid Credit Card."
                    if (card.amountOwed + transaction.amount > card.limit) {
                        return "Transaction failed: Exceeds credit limit."
                    }
                }
                else -> {
                    val account = transaction.accountId?.let { appDao.getAccountById(it) } ?: return "Invalid account."
                    
                    val balanceAtDate = calculateBalanceAtDate(account.id, txDate, ignoredTransactionId)
                    if (balanceAtDate < transaction.amount && account.type == AccountType.CASH) {
                        return "Insufficient funds in 'Cash' on ${transaction.date}. Balance was ₹$balanceAtDate."
                    } else if (balanceAtDate < transaction.amount) {
                        return "Insufficient funds in '${account.name}' on ${transaction.date}. Balance was ₹$balanceAtDate."
                    }
                }
            }

            if (transaction.category == "Loan Repayment" && transaction.loanId != null) {
                val loan = appDao.getLoanById(transaction.loanId) ?: return "Invalid loan."
                if (loan.isClosed) return "This loan is already closed."
            }
        } else { // INCOME
            if (transaction.accountId == null) return "Income transaction must have an account"
        }
        return null
    }

    private suspend fun calculateBalanceAtDate(accountId: Long, date: LocalDate, ignoredTransactionId: Long? = null): Double {
        val account = appDao.getAccountById(accountId) ?: return 0.0
        val transactions = appDao.getTransactionsDirect().filter { it.accountId == accountId && it.id != ignoredTransactionId }
        
        var runningBalance = account.initialBalance
        transactions.filter { LocalDate.parse(it.date).isBefore(date) || LocalDate.parse(it.date).isEqual(date) }
            .sortedBy { it.date }
            .forEach {
                if (it.type == TransactionType.INCOME) runningBalance += it.amount
                else runningBalance -= it.amount
            }
        return runningBalance
    }

    private suspend fun applyBalanceUpdates(transaction: Transaction) {
        if (transaction.type == TransactionType.EXPENSE) {
            when {
                transaction.category == "Credit Card Payment" -> {
                    transaction.accountId?.let { id ->
                        appDao.getAccountById(id)?.let { acc ->
                            appDao.updateAccount(acc.copy(balance = acc.balance - transaction.amount))
                        }
                    }
                    transaction.creditCardId?.let { id ->
                        appDao.getCreditCardById(id)?.let { card ->
                            val principalPaid = minOf(transaction.amount, card.amountOwed)
                            val interestPaid = (transaction.amount - principalPaid).coerceAtLeast(0.0)
                            updateCreditCardAndSchedule(card.copy(
                                amountOwed = (card.amountOwed - principalPaid).coerceAtLeast(0.0),
                                totalInterestPaid = card.totalInterestPaid + interestPaid
                            ))
                        }
                    }
                }
                transaction.paymentMode == "Credit Card" -> {
                    transaction.creditCardId?.let { id ->
                        appDao.getCreditCardById(id)?.let { card ->
                            updateCreditCardAndSchedule(card.copy(amountOwed = card.amountOwed + transaction.amount))
                        }
                    }
                }
                else -> {
                    transaction.accountId?.let { id ->
                        appDao.getAccountById(id)?.let { acc ->
                            appDao.updateAccount(acc.copy(balance = acc.balance - transaction.amount))
                        }
                    }
                }
            }

            if (transaction.category == "Loan Repayment" && transaction.loanId != null) {
                appDao.getLoanById(transaction.loanId)?.let { loan ->
                    val principalPaid = minOf(transaction.amount, loan.remainingAmount)
                    val interestPaid = (transaction.amount - principalPaid).coerceAtLeast(0.0)
                    val newRemaining = (loan.remainingAmount - principalPaid).coerceAtLeast(0.0)
                    appDao.updateLoan(loan.copy(
                        remainingAmount = newRemaining,
                        totalInterestPaid = loan.totalInterestPaid + interestPaid,
                        isClosed = newRemaining <= 0
                    ))
                }
            }
        } else { // INCOME
            transaction.accountId?.let { id ->
                appDao.getAccountById(id)?.let { acc ->
                    appDao.updateAccount(acc.copy(balance = acc.balance + transaction.amount))
                }
            }
        }
    }

    @RoomTransaction
    suspend fun updateTransactionAndUpdateBalances(oldTx: Transaction, newTx: Transaction): String? {
        // Revert the effects of the old transaction
        revertBalanceUpdates(oldTx)

        val preparedNewTx = newTx.withCalculatedInterest().copy(
            id = oldTx.id,
            createdAt = System.currentTimeMillis()
        )
        val error = validateTransaction(preparedNewTx, ignoredTransactionId = oldTx.id)
        if (error != null) {
            // If the new transaction fails, re-apply the old transaction's updates to restore state
            applyBalanceUpdates(oldTx) 
            return error
        }

        applyBalanceUpdates(preparedNewTx)
        appDao.updateTransaction(preparedNewTx)
        return null
    }

    private suspend fun revertBalanceUpdates(transaction: Transaction) {
        if (transaction.type == TransactionType.EXPENSE) {
            when {
                transaction.category == "Credit Card Payment" -> {
                    transaction.accountId?.let { id ->
                        appDao.getAccountById(id)?.let { acc ->
                            appDao.updateAccount(acc.copy(balance = acc.balance + transaction.amount))
                        }
                    }
                    transaction.creditCardId?.let { id ->
                        appDao.getCreditCardById(id)?.let { card ->
                            val principalReverted = (transaction.amount - transaction.interestAmount).coerceAtLeast(0.0)
                            updateCreditCardAndSchedule(card.copy(
                                amountOwed = card.amountOwed + principalReverted,
                                totalInterestPaid = (card.totalInterestPaid - transaction.interestAmount).coerceAtLeast(0.0)
                            ))
                        }
                    }
                }
                transaction.paymentMode == "Credit Card" -> {
                    transaction.creditCardId?.let { id ->
                        appDao.getCreditCardById(id)?.let { card ->
                            updateCreditCardAndSchedule(card.copy(amountOwed = (card.amountOwed - transaction.amount).coerceAtLeast(0.0)))
                        }
                    }
                }
                else -> {
                    transaction.accountId?.let { id ->
                        appDao.getAccountById(id)?.let { acc ->
                            appDao.updateAccount(acc.copy(balance = acc.balance + transaction.amount))
                        }
                    }
                }
            }

            if (transaction.category == "Loan Repayment" && transaction.loanId != null) {
                appDao.getLoanById(transaction.loanId)?.let { loan ->
                    val principalReverted = (transaction.amount - transaction.interestAmount).coerceAtLeast(0.0)
                    val newRemaining = loan.remainingAmount + principalReverted
                    appDao.updateLoan(loan.copy(
                        remainingAmount = newRemaining,
                        totalInterestPaid = (loan.totalInterestPaid - transaction.interestAmount).coerceAtLeast(0.0),
                        isClosed = newRemaining <= 0.0
                    ))
                }
            }
        } else { // INCOME
            transaction.accountId?.let { id ->
                appDao.getAccountById(id)?.let { acc ->
                    appDao.updateAccount(acc.copy(balance = acc.balance - transaction.amount))
                }
            }
        }
    }

    suspend fun deleteTransactionAndUpdateBalances(transaction: Transaction) {
        revertBalanceUpdates(transaction)
        appDao.deleteTransaction(transaction)
    }

    private suspend fun Transaction.withCalculatedInterest(): Transaction {
        return when {
            category == "Loan Repayment" && loanId != null -> {
                val loan = appDao.getLoanById(loanId)
                val interest = if (loan != null && amount > loan.remainingAmount) amount - loan.remainingAmount else interestAmount
                copy(interestAmount = interest.coerceAtLeast(0.0))
            }
            category == "Credit Card Payment" && creditCardId != null -> {
                val card = appDao.getCreditCardById(creditCardId)
                val interest = if (card != null && amount > card.amountOwed) amount - card.amountOwed else interestAmount
                copy(interestAmount = interest.coerceAtLeast(0.0))
            }
            else -> copy(interestAmount = interestAmount.coerceAtLeast(0.0))
        }
    }

    private suspend fun upsertCreditCardSchedule(card: CreditCard) {
        val nextDueDate = nextDueDateForDay(card.dueDate)
        val existing = appDao.getCreditCardSchedule(card.id)
        val schedule = ScheduledTransaction(
            id = existing?.id ?: 0,
            type = TransactionType.EXPENSE,
            amount = card.amountOwed.coerceAtLeast(0.0),
            category = "Credit Card Payment",
            accountId = existing?.accountId,
            creditCardId = card.id,
            loanId = null,
            paymentMode = existing?.paymentMode ?: "Transfer",
            description = "${card.name} credit card bill",
            frequency = "Monthly",
            nextDueDate = nextDueDate
        )
        if (existing == null) appDao.insertScheduledTransaction(schedule) else appDao.updateScheduledTransaction(schedule)
    }

    private fun nextDueDateForDay(day: Int): String {
        val today = LocalDate.now()
        val safeDay = day.coerceIn(1, 31)
        fun LocalDate.withSafeDay(): LocalDate = withDayOfMonth(safeDay.coerceAtMost(lengthOfMonth()))
        val candidate = today.withSafeDay()
        return (if (candidate.isBefore(today) || candidate.isEqual(today)) today.plusMonths(1).withSafeDay() else candidate).toString()
    }

    private suspend fun updateCreditCardAndSchedule(card: CreditCard) {
        appDao.updateCreditCard(card)
        upsertCreditCardSchedule(card)
    }

    fun enqueueImmediateReminderCheck() {
        appContext?.let { context ->
            val workRequest = OneTimeWorkRequestBuilder<PaymentReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

}

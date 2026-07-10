package com.skai.lofintrackerapp.data.repository

import android.content.Context
import androidx.room.withTransaction
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.skai.lofintrackerapp.PaymentReminderWorker
import com.skai.lofintrackerapp.data.db.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AppRepository(
    private val database: AppDatabase,
    private val appContext: Context? = null
) {
    private val appDao = database.appDao()

    val allAccounts: Flow<List<Account>> = appDao.getAllAccounts()
    val allLoans: Flow<List<Loan>> = appDao.getAllLoans()
    val allTransactions: Flow<List<Transaction>> = appDao.getAllTransactions()
    val allCreditCards: Flow<List<CreditCard>> = appDao.getAllCreditCards()
    val allScheduledTransactions: Flow<List<ScheduledTransaction>> = appDao.getAllScheduledTransactions()

    suspend fun insertAccount(account: Account): String? {
        if (account.initialBalance < 0) return "Initial balance cannot be negative."
        appDao.insertAccount(account)
        return null
    }
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

    suspend fun insertLoanAndPayout(loan: Loan, payoutAccountId: Long) {
        database.withTransaction {
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
    }

    suspend fun insertCreditCard(card: CreditCard) {
        database.withTransaction {
            val cardId = appDao.insertCreditCard(card)
            upsertCreditCardSchedule(card.copy(id = cardId))
        }
    }
    suspend fun updateCreditCard(card: CreditCard) {
        database.withTransaction {
            appDao.updateCreditCard(card)
            upsertCreditCardSchedule(card)
        }
    }
    suspend fun deleteCreditCard(card: CreditCard) { appDao.deleteCreditCard(card) }

    suspend fun insertTransactionAndUpdateBalances(transaction: Transaction): String? {
        if (transaction.amount <= 0) return "Amount must be greater than zero."
        return database.withTransaction {
            val validationError = validateTransaction(transaction)
            if (validationError != null) return@withTransaction validationError
            val finalTransaction = transaction.withCalculatedInterest()
                .copy(createdAt = if (transaction.createdAt == 0L) System.currentTimeMillis() else transaction.createdAt)
            appDao.insertTransaction(finalTransaction)
            applyBalanceUpdates(finalTransaction)
            null
        }
    }

    private suspend fun validateTransaction(transaction: Transaction, ignoredTransactionId: Long? = null): String? {
        val txDate = try { LocalDate.parse(transaction.date) } catch (e: Exception) { return "Invalid date format." }
        if (transaction.type == TransactionType.EXPENSE) {
            when {
                transaction.category == "Credit Card Payment" -> {
                    val account = transaction.accountId?.let { appDao.getAccountById(it) } ?: return "Invalid bank account."
                    val card = transaction.creditCardId?.let { appDao.getCreditCardById(it) } ?: return "Invalid credit card."
                    if (card.amountOwed <= 0) return "Cannot pay bill for a card with no debt."
                    val balanceAtDate = calculateBalanceAtDate(account.id, txDate, ignoredTransactionId)
                    if (balanceAtDate < transaction.amount) return "Insufficient funds in '${account.name}' on ${transaction.date}."
                }
                transaction.paymentMode == "Credit Card" -> {
                    val card = transaction.creditCardId?.let { appDao.getCreditCardById(it) } ?: return "Invalid Credit Card."
                    if (card.amountOwed + transaction.amount > card.limit) return "Transaction failed: Exceeds credit limit."
                }
                else -> {
                    val account = transaction.accountId?.let { appDao.getAccountById(it) } ?: return "Invalid account."
                    val balanceAtDate = calculateBalanceAtDate(account.id, txDate, ignoredTransactionId)
                    if (balanceAtDate < transaction.amount) return "Insufficient funds in '${account.name}' on ${transaction.date}."
                }
            }
        }
        return null
    }

    private suspend fun calculateBalanceAtDate(accountId: Long, date: LocalDate, ignoredTransactionId: Long? = null): Double {
        val account = appDao.getAccountById(accountId) ?: return 0.0
        val transactions = appDao.getTransactionsDirect().filter { it.accountId == accountId && it.id != ignoredTransactionId }
        var runningBalance = account.initialBalance
        transactions.filter { try { !LocalDate.parse(it.date).isAfter(date) } catch(e: Exception) { false } }
            .sortedBy { it.date }
            .forEach { if (it.type == TransactionType.INCOME) runningBalance += it.amount else runningBalance -= it.amount }
        return runningBalance
    }

    private suspend fun applyBalanceUpdates(transaction: Transaction) {
        if (transaction.type == TransactionType.EXPENSE) {
            when {
                transaction.category == "Credit Card Payment" -> {
                    transaction.accountId?.let { id -> appDao.getAccountById(id)?.let { acc -> appDao.updateAccount(acc.copy(balance = acc.balance - transaction.amount)) } }
                    transaction.creditCardId?.let { id -> appDao.getCreditCardById(id)?.let { card ->
                        val principalPaid = minOf(transaction.amount, card.amountOwed)
                        updateCreditCardAndSchedule(card.copy(amountOwed = (card.amountOwed - principalPaid).coerceAtLeast(0.0), totalInterestPaid = card.totalInterestPaid + (transaction.amount - principalPaid).coerceAtLeast(0.0)))
                    } }
                }
                transaction.paymentMode == "Credit Card" -> {
                    transaction.creditCardId?.let { id -> appDao.getCreditCardById(id)?.let { card -> updateCreditCardAndSchedule(card.copy(amountOwed = card.amountOwed + transaction.amount)) } }
                }
                else -> {
                    transaction.accountId?.let { id -> appDao.getAccountById(id)?.let { acc -> appDao.updateAccount(acc.copy(balance = acc.balance - transaction.amount)) } }
                }
            }
            if (transaction.category == "Loan Repayment" && transaction.loanId != null) {
                appDao.getLoanById(transaction.loanId)?.let { loan ->
                    val principalPaid = minOf(transaction.amount, loan.remainingAmount)
                    val newRemaining = (loan.remainingAmount - principalPaid).coerceAtLeast(0.0)
                    appDao.updateLoan(loan.copy(remainingAmount = newRemaining, totalInterestPaid = loan.totalInterestPaid + (transaction.amount - principalPaid).coerceAtLeast(0.0), isClosed = newRemaining <= 0))
                }
            }
        } else {
            transaction.accountId?.let { id -> appDao.getAccountById(id)?.let { acc -> appDao.updateAccount(acc.copy(balance = acc.balance + transaction.amount)) } }
        }
    }

    suspend fun updateTransactionAndUpdateBalances(oldTx: Transaction, newTx: Transaction): String? {
        return database.withTransaction {
            revertBalanceUpdates(oldTx)
            val preparedNewTx = newTx.withCalculatedInterest().copy(id = oldTx.id, createdAt = System.currentTimeMillis())
            val error = validateTransaction(preparedNewTx, ignoredTransactionId = oldTx.id)
            if (error != null) { applyBalanceUpdates(oldTx); return@withTransaction error }
            applyBalanceUpdates(preparedNewTx)
            appDao.updateTransaction(preparedNewTx)
            null
        }
    }

    private suspend fun revertBalanceUpdates(transaction: Transaction) {
        if (transaction.type == TransactionType.EXPENSE) {
            when {
                transaction.category == "Credit Card Payment" -> {
                    transaction.accountId?.let { id -> appDao.getAccountById(id)?.let { acc -> appDao.updateAccount(acc.copy(balance = acc.balance + transaction.amount)) } }
                    transaction.creditCardId?.let { id -> appDao.getCreditCardById(id)?.let { card ->
                        val principalReverted = (transaction.amount - transaction.interestAmount).coerceAtLeast(0.0)
                        updateCreditCardAndSchedule(card.copy(amountOwed = card.amountOwed + principalReverted, totalInterestPaid = (card.totalInterestPaid - transaction.interestAmount).coerceAtLeast(0.0)))
                    } }
                }
                transaction.paymentMode == "Credit Card" -> {
                    transaction.creditCardId?.let { id -> appDao.getCreditCardById(id)?.let { card -> updateCreditCardAndSchedule(card.copy(amountOwed = (card.amountOwed - transaction.amount).coerceAtLeast(0.0))) } }
                }
                else -> {
                    transaction.accountId?.let { id -> appDao.getAccountById(id)?.let { acc -> appDao.updateAccount(acc.copy(balance = acc.balance + transaction.amount)) } }
                }
            }
            if (transaction.category == "Loan Repayment" && transaction.loanId != null) {
                appDao.getLoanById(transaction.loanId)?.let { loan ->
                    val principalReverted = (transaction.amount - transaction.interestAmount).coerceAtLeast(0.0)
                    val newRemaining = loan.remainingAmount + principalReverted
                    appDao.updateLoan(loan.copy(remainingAmount = newRemaining, totalInterestPaid = (loan.totalInterestPaid - transaction.interestAmount).coerceAtLeast(0.0), isClosed = newRemaining <= 0.0))
                }
            }
        } else {
            transaction.accountId?.let { id -> appDao.getAccountById(id)?.let { acc -> appDao.updateAccount(acc.copy(balance = acc.balance - transaction.amount)) } }
        }
    }

    suspend fun deleteTransactionAndUpdateBalances(transaction: Transaction) {
        database.withTransaction {
            revertBalanceUpdates(transaction)
            appDao.deleteTransaction(transaction)
        }
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

    suspend fun restoreAllData(
        accounts: List<Account>,
        loans: List<Loan>,
        transactions: List<Transaction>,
        creditCards: List<CreditCard>,
        scheduledTransactions: List<ScheduledTransaction>
    ) {
        database.withTransaction {
            // Order matters due to Foreign Key RESTRICT constraints!
            appDao.clearTransactions()
            appDao.clearScheduledTransactions()
            appDao.clearLoans()
            appDao.clearCreditCards()
            appDao.clearAccounts()

            val accountIdMap = mutableMapOf<Long, Long>()
            accounts.forEach { oldAcc ->
                val newId = appDao.insertAccount(oldAcc.copy(id = 0))
                accountIdMap[oldAcc.id] = newId
            }

            val loanIdMap = mutableMapOf<Long, Long>()
            loans.forEach { oldLoan ->
                val newId = appDao.insertLoan(oldLoan.copy(id = 0))
                loanIdMap[oldLoan.id] = newId
            }

            val cardIdMap = mutableMapOf<Long, Long>()
            creditCards.forEach { oldCard ->
                val newId = appDao.insertCreditCard(oldCard.copy(id = 0))
                cardIdMap[oldCard.id] = newId
            }

            transactions.forEach { oldTx ->
                val restoredCreditCardId = when {
                    oldTx.creditCardId != null -> cardIdMap[oldTx.creditCardId]
                    oldTx.category == "Credit Card Payment" && oldTx.loanId != null -> cardIdMap[oldTx.loanId]
                    else -> null
                }
                val mappedTx = oldTx.copy(
                    id = 0,
                    accountId = oldTx.accountId?.let { accountIdMap[it] },
                    loanId = if (oldTx.category == "Credit Card Payment") null else oldTx.loanId?.let { loanIdMap[it] },
                    creditCardId = restoredCreditCardId
                )
                appDao.insertTransaction(mappedTx)
            }

            scheduledTransactions.forEach { oldSt ->
                val restoredCreditCardId = when {
                    oldSt.creditCardId != null -> cardIdMap[oldSt.creditCardId]
                    oldSt.category == "Credit Card Payment" && oldSt.loanId != null -> cardIdMap[oldSt.loanId]
                    else -> null
                }
                val mappedSt = oldSt.copy(
                    id = 0,
                    accountId = oldSt.accountId?.let { accountIdMap[it] },
                    loanId = if (oldSt.category == "Credit Card Payment") null else oldSt.loanId?.let { loanIdMap[it] },
                    creditCardId = restoredCreditCardId
                )
                appDao.insertScheduledTransaction(mappedSt)
            }

            cardIdMap.values.forEach { newCardId ->
                appDao.getCreditCardById(newCardId)?.let { card ->
                    if (appDao.getCreditCardSchedule(newCardId) == null) {
                        upsertCreditCardSchedule(card)
                    }
                }
            }

            if (accounts.isEmpty()) {
                appDao.insertAccount(Account(name = "Cash", type = AccountType.CASH, initialBalance = 0.0, balance = 0.0))
            }
        }
    }
}

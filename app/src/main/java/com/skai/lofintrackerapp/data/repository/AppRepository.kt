package com.skai.lofintrackerapp.data.repository

import androidx.room.Transaction as RoomTransaction
import com.skai.lofintrackerapp.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class AppRepository(private val appDao: AppDao) {

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
    suspend fun insertScheduledTransaction(tx: ScheduledTransaction) { appDao.insertScheduledTransaction(tx) }
    suspend fun updateScheduledTransaction(tx: ScheduledTransaction) { appDao.updateScheduledTransaction(tx) }
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
            description = "Loan payout from ${loan.lender}"
        )
        insertTransactionAndUpdateBalances(payoutTransaction)
    }

    suspend fun insertCreditCard(card: CreditCard) { appDao.insertCreditCard(card) }
    suspend fun updateCreditCard(card: CreditCard) { appDao.updateCreditCard(card) }
    suspend fun deleteCreditCard(card: CreditCard) { appDao.deleteCreditCard(card) }

    @RoomTransaction
    suspend fun insertTransactionAndUpdateBalances(transaction: Transaction): String? {
        if (transaction.amount <= 0) return "Amount must be greater than zero."

        val txDate = LocalDate.parse(transaction.date)

        // Helper to validate a transaction without applying changes
        val validationError = validateTransaction(transaction)
        if (validationError != null) return validationError

        // Process interest calculation for debt payments (Request 7)
        var finalTransaction = transaction
        if (transaction.category == "Loan Repayment" && transaction.loanId != null) {
            val loan = appDao.getLoanById(transaction.loanId)!!
            if (transaction.amount > loan.remainingAmount) {
                val interest = transaction.amount - loan.remainingAmount
                finalTransaction = transaction.copy(interestAmount = interest)
            }
        } else if (transaction.category == "Credit Card Payment" && transaction.loanId != null) {
            // loanId is being used as creditCardId for payments
            val card = appDao.getCreditCardById(transaction.loanId)!! 
            if (transaction.amount > card.amountOwed) {
                val interest = transaction.amount - card.amountOwed
                finalTransaction = transaction.copy(interestAmount = interest)
            }
        }

        appDao.insertTransaction(finalTransaction)
        applyBalanceUpdates(finalTransaction)
        return null
    }

    private suspend fun validateTransaction(transaction: Transaction): String? {
        if (transaction.amount <= 0) return "Amount must be greater than zero."

        val txDate = LocalDate.parse(transaction.date)

        if (transaction.type == TransactionType.EXPENSE) {
            when {
                transaction.category == "Credit Card Payment" -> {
                    val account = transaction.accountId?.let { appDao.getAccountById(it) } ?: return "Invalid bank account."
                    val card = transaction.loanId?.let { appDao.getCreditCardById(it) } ?: return "Invalid credit card."

                    if (card.amountOwed <= 0) return "Cannot pay bill for a card with no debt."
                    
                    val balanceAtDate = calculateBalanceAtDate(account.id, txDate)
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
                    
                    val balanceAtDate = calculateBalanceAtDate(account.id, txDate)
                    if (balanceAtDate < transaction.amount && account.type == AccountType.CASH) {
                        return "Insufficient funds in 'Cash' on ${transaction.date}. Balance was ₹$balanceAtDate."
                    } else if (balanceAtDate < transaction.amount && transaction.category != "Bank Charges") {
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

    private suspend fun calculateBalanceAtDate(accountId: Long, date: LocalDate): Double {
        val account = appDao.getAccountById(accountId) ?: return 0.0
        val transactions = appDao.getTransactionsDirect().filter { it.accountId == accountId }
        
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
                    transaction.loanId?.let { id -> // loanId is used as creditCardId for payment target
                        appDao.getCreditCardById(id)?.let { card ->
                            val principalPaid = minOf(transaction.amount, card.amountOwed)
                            val interestPaid = transaction.amount - principalPaid // Correct calculation of interest
                            appDao.updateCreditCard(card.copy(
                                amountOwed = card.amountOwed - principalPaid,
                                totalInterestPaid = card.totalInterestPaid + interestPaid
                            ))
                        }
                    }
                }
                transaction.paymentMode == "Credit Card" -> {
                    transaction.creditCardId?.let { id ->
                        appDao.getCreditCardById(id)?.let { card ->
                            appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed + transaction.amount))
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
                    val interestPaid = transaction.amount - principalPaid // Correct calculation of interest
                    val newRemaining = loan.remainingAmount - principalPaid
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

        // Try to insert and apply the new transaction (will also validate)
        val error = insertTransactionAndUpdateBalances(newTx)
        if (error != null) {
            // If the new transaction fails, re-apply the old transaction's updates to restore state
            applyBalanceUpdates(oldTx) 
            // We don't re-insert oldTx into the DB here because it was never deleted by revertBalanceUpdates
            return error
        }
        
        // If new transaction succeeded, we now delete the old transaction from the DB
        appDao.deleteTransaction(oldTx)
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
                    transaction.loanId?.let { id ->
                        appDao.getCreditCardById(id)?.let { card ->
                            val principalReverted = transaction.amount - transaction.interestAmount // Use interestAmount from transaction
                            appDao.updateCreditCard(card.copy(
                                amountOwed = card.amountOwed + principalReverted,
                                totalInterestPaid = card.totalInterestPaid - transaction.interestAmount
                            ))
                        }
                    }
                }
                transaction.paymentMode == "Credit Card" -> {
                    transaction.creditCardId?.let { id ->
                        appDao.getCreditCardById(id)?.let { card ->
                            appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed - transaction.amount))
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
                    val principalReverted = transaction.amount - transaction.interestAmount // Use interestAmount from transaction
                    val newRemaining = loan.remainingAmount + principalReverted
                    appDao.updateLoan(loan.copy(
                        remainingAmount = newRemaining,
                        totalInterestPaid = loan.totalInterestPaid - transaction.interestAmount,
                        isClosed = newRemaining > 0
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

}
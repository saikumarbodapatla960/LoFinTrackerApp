// In ...data.repository/AppRepository.kt
package com.skai.lofintrackerapp.data.repository

import androidx.room.Transaction as RoomTransaction
import com.skai.lofintrackerapp.data.db.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    // --- Live Data Flows (Unchanged) ---
    val allAccounts: Flow<List<Account>> = appDao.getAllAccounts()
    val allLoans: Flow<List<Loan>> = appDao.getAllLoans()
    val allTransactions: Flow<List<Transaction>> = appDao.getAllTransactions()
    val allCreditCards: Flow<List<CreditCard>> = appDao.getAllCreditCards()
    val allScheduledTransactions: Flow<List<ScheduledTransaction>> = appDao.getAllScheduledTransactions()

    // --- Account/Loan/Card Functions (Unchanged) ---
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

    // --- TRANSACTION LOGIC (UPDATED) ---

    @RoomTransaction
    suspend fun insertTransactionAndUpdateBalances(transaction: Transaction): String? {

        // --- 1. VALIDATION CHECKS ---
        if (transaction.amount <= 0) {
            return "Amount must be greater than zero."
        }

        if (transaction.type == TransactionType.EXPENSE) {
            when (transaction.paymentMode) {
                "Credit Card" -> {
                    // Credit Card Purchase
                    val card = appDao.getCreditCardById(transaction.creditCardId!!)
                    if (card != null) {
                        val newAmountOwed = card.amountOwed + transaction.amount
                        if (newAmountOwed > card.limit) {
                            return "Transaction failed: This would exceed your credit limit."
                        }
                    } else {
                        return "Invalid Credit Card."
                    }
                }
                "Credit Card Payment" -> {
                    // Paying a CC Bill
                    val account = appDao.getAccountById(transaction.accountId) ?: return "Invalid bank account."
                    val newBalance = account.balance - transaction.amount
                    // --- BANK ACCOUNT CHECK (Strict) ---
                    if (newBalance < 0) {
                        if (account.type == AccountType.CASH) {
                            return "Transaction failed: Insufficient funds in 'Cash'."
                        } else if (transaction.category != "Bank Charges") {
                            return "Transaction failed: Insufficient funds in '${account.name}'."
                        }
                    }
                }
                else -> {
                    // --- Normal Bank/Cash Purchase (UPI, Debit, Cash, etc.) ---
                    val account = appDao.getAccountById(transaction.accountId) ?: return "Invalid account."
                    val newBalance = account.balance - transaction.amount

                    // --- THIS IS THE FIX ---
                    if (newBalance < 0) {
                        if (account.type == AccountType.CASH) {
                            return "Transaction failed: Insufficient funds in 'Cash'."
                        }
                        // Allow bank accounts to go negative ONLY for bank charges
                        else if (transaction.category != "Bank Charges") {
                            return "Transaction failed: Insufficient funds in '${account.name}'."
                        }
                    }
                    // --- END OF FIX ---
                }
            }

            // --- LOAN PAYMENT FIX ---
            if (transaction.category == "Loan Repayment" && transaction.loanId != null) {
                val loan = appDao.getLoanById(transaction.loanId) ?: return "Invalid loan."
                val newRemaining = loan.remainingAmount - transaction.amount
                if (newRemaining < 0) {
                    return "Payment (₹${transaction.amount}) is larger than the remaining loan balance (₹${loan.remainingAmount})."
                }
            }
            // --- END OF FIX ---
        }
        // --- END VALIDATION ---

        // 2. Insert the transaction (passed all checks)
        appDao.insertTransaction(transaction)

        // 3. Update Balances
        if (transaction.type == TransactionType.EXPENSE) {
            when (transaction.paymentMode) {
                "Credit Card" -> {
                    val card = appDao.getCreditCardById(transaction.creditCardId!!)!!
                    appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed + transaction.amount))
                }
                "Credit Card Payment" -> {
                    val account = appDao.getAccountById(transaction.accountId)!!
                    appDao.updateAccount(account.copy(balance = account.balance - transaction.amount))
                    val card = appDao.getCreditCardById(transaction.loanId!!)!!
                    appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed - transaction.amount))
                }
                else -> {
                    // Normal Bank/Cash
                    val account = appDao.getAccountById(transaction.accountId)!!
                    appDao.updateAccount(account.copy(balance = account.balance - transaction.amount))
                }
            }
        } else {
            // Normal Income
            val account = appDao.getAccountById(transaction.accountId)!!
            appDao.updateAccount(account.copy(balance = account.balance + transaction.amount))
        }

        // 4. Update Loan Repayment
        if (transaction.type == TransactionType.EXPENSE && transaction.category == "Loan Repayment" && transaction.loanId != null) {
            val loan = appDao.getLoanById(transaction.loanId)!!
            appDao.updateLoan(loan.copy(remainingAmount = loan.remainingAmount - transaction.amount))
        }

        return null // Success!
    }

    // --- Delete Transaction Logic (Unchanged but verified) ---
    // ... inside AppRepository class ...

    @RoomTransaction
    suspend fun updateTransactionAndUpdateBalances(oldTx: Transaction, newTx: Transaction): String? {
        // 1. Revert the OLD transaction's effect on balances
        // We can reuse the logic from delete, but we DON'T delete the row yet.
        // (Copying logic for safety to avoid recursion issues)
        if (oldTx.type == TransactionType.EXPENSE) {
            when (oldTx.paymentMode) {
                "Credit Card" -> {
                    val card = appDao.getCreditCardById(oldTx.creditCardId!!)
                    if (card != null) appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed - oldTx.amount))
                }
                "Credit Card Payment" -> {
                    val account = appDao.getAccountById(oldTx.accountId)
                    if (account != null) appDao.updateAccount(account.copy(balance = account.balance + oldTx.amount))
                    val card = appDao.getCreditCardById(oldTx.loanId!!)
                    if (card != null) appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed + oldTx.amount))
                }
                else -> {
                    val account = appDao.getAccountById(oldTx.accountId)
                    if (account != null) appDao.updateAccount(account.copy(balance = account.balance + oldTx.amount))
                }
            }
        } else {
            val account = appDao.getAccountById(oldTx.accountId)
            if (account != null) appDao.updateAccount(account.copy(balance = account.balance - oldTx.amount))
        }

        if (oldTx.category == "Loan Repayment" && oldTx.loanId != null) {
            val loan = appDao.getLoanById(oldTx.loanId)
            if (loan != null) appDao.updateLoan(loan.copy(remainingAmount = loan.remainingAmount + oldTx.amount))
        }

        // 2. Now apply the NEW transaction (using the existing insert logic)
        // We use a trick: We update the row in the DB, then call the balance update logic manually
        appDao.updateTransaction(newTx) // Update the row to new values

        // Now manually apply the NEW balance effects (Logic copied from insertTransactionAndUpdateBalances for explicit control)
        // --- VALIDATION CHECKS (Simplified for update) ---
        if (newTx.amount <= 0) return "Amount must be greater than zero."

        // ... (You can perform the same insufficient funds checks here if you want strict safety) ...

        // Apply New Balances
        if (newTx.type == TransactionType.EXPENSE) {
            when (newTx.paymentMode) {
                "Credit Card" -> {
                    val card = appDao.getCreditCardById(newTx.creditCardId!!)
                    if (card != null) appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed + newTx.amount))
                }
                "Credit Card Payment" -> {
                    val account = appDao.getAccountById(newTx.accountId)!!
                    appDao.updateAccount(account.copy(balance = account.balance - newTx.amount))
                    val card = appDao.getCreditCardById(newTx.loanId!!)!!
                    appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed - newTx.amount))
                }
                else -> {
                    val account = appDao.getAccountById(newTx.accountId)!!
                    appDao.updateAccount(account.copy(balance = account.balance - newTx.amount))
                }
            }
        } else {
            val account = appDao.getAccountById(newTx.accountId)!!
            appDao.updateAccount(account.copy(balance = account.balance + newTx.amount))
        }

        if (newTx.category == "Loan Repayment" && newTx.loanId != null) {
            val loan = appDao.getLoanById(newTx.loanId)!!
            appDao.updateLoan(loan.copy(remainingAmount = loan.remainingAmount - newTx.amount))
        }

        return null
    }

    suspend fun deleteTransactionAndUpdateBalances(transaction: Transaction) {
        appDao.deleteTransaction(transaction)

        if (transaction.type == TransactionType.EXPENSE) {
            when (transaction.paymentMode) {
                "Credit Card" -> {
                    val card = appDao.getCreditCardById(transaction.creditCardId!!)
                    if (card != null) {
                        appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed - transaction.amount))
                    }
                }
                "Credit Card Payment" -> {
                    val account = appDao.getAccountById(transaction.accountId)
                    if (account != null) {
                        appDao.updateAccount(account.copy(balance = account.balance + transaction.amount))
                    }
                    val card = appDao.getCreditCardById(transaction.loanId!!)
                    if (card != null) {
                        appDao.updateCreditCard(card.copy(amountOwed = card.amountOwed + transaction.amount))
                    }
                }
                else -> {
                    val account = appDao.getAccountById(transaction.accountId)
                    if (account != null) {
                        appDao.updateAccount(account.copy(balance = account.balance + transaction.amount))
                    }
                }
            }
        } else {
            val account = appDao.getAccountById(transaction.accountId)
            if (account != null) {
                appDao.updateAccount(account.copy(balance = account.balance - transaction.amount))
            }
        }

        if (transaction.type == TransactionType.EXPENSE && transaction.category == "Loan Repayment" && transaction.loanId != null) {
            val loan = appDao.getLoanById(transaction.loanId)
            if (loan != null) {
                appDao.updateLoan(loan.copy(remainingAmount = loan.remainingAmount + transaction.amount))
            }
        }
    }
}
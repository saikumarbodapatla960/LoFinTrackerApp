// In ...data.db/AppDao.kt
package com.skai.lofintrackerapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Account Functions ---
    @Insert
    suspend fun insertAccount(account: Account): Long
    @Update
    suspend fun updateAccount(account: Account)
    @Delete
    suspend fun deleteAccount(account: Account)
    @Query("SELECT * FROM accounts ORDER BY balance DESC, name COLLATE NOCASE ASC")
    fun getAllAccounts(): Flow<List<Account>>
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    // --- Loan Functions ---
    @Insert
    suspend fun insertLoan(loan: Loan): Long
    @Update
    suspend fun updateLoan(loan: Loan)
    @Delete
    suspend fun deleteLoan(loan: Loan)
    @Query("SELECT * FROM loans ORDER BY name ASC")
    fun getAllLoans(): Flow<List<Loan>>
    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): Loan?

    // --- Credit Card Functions ---
    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCreditCardById(id: Long): CreditCard?
    @Query("SELECT * FROM credit_cards ORDER BY name ASC")
    fun getAllCreditCards(): Flow<List<CreditCard>>
    @Insert
    suspend fun insertCreditCard(card: CreditCard): Long
    @Update
    suspend fun updateCreditCard(card: CreditCard)
    @Delete
    suspend fun deleteCreditCard(card: CreditCard)

    // --- Transaction Functions ---
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions")
    suspend fun getTransactionsDirect(): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM scheduled_transactions WHERE category = 'Credit Card Payment' AND creditCardId = :cardId LIMIT 1")
    suspend fun getCreditCardSchedule(cardId: Long): ScheduledTransaction?

    // --- SCHEDULED TRANSACTION FUNCTIONS ---
    @Query("SELECT * FROM scheduled_transactions ORDER BY nextDueDate ASC")
    fun getAllScheduledTransactions(): Flow<List<ScheduledTransaction>>

    @Insert
    suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long

    @Update
    suspend fun updateScheduledTransaction(transaction: ScheduledTransaction)

    @Delete
    suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction)

    // --- Backup & Restore ---
    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()
    @Query("DELETE FROM loans")
    suspend fun clearLoans()
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()
    @Query("DELETE FROM credit_cards")
    suspend fun clearCreditCards()
    @Query("DELETE FROM scheduled_transactions")
    suspend fun clearScheduledTransactions()
}

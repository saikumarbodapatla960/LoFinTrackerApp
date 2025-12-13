// In ...ui.viewmodel/MainViewModel.kt
package com.skai.lofintrackerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.lofintrackerapp.data.DEFAULT_EXPENSE_CATEGORIES
import com.skai.lofintrackerapp.data.DEFAULT_INCOME_CATEGORIES
import com.skai.lofintrackerapp.data.UserPreferences
import com.skai.lofintrackerapp.data.db.*
import com.skai.lofintrackerapp.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(
    private val repository: AppRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // --- USER PREFERENCES ---
    val userName: StateFlow<String?> = userPreferences.userName
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appTheme: StateFlow<String> = userPreferences.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val hasSeenTutorial: StateFlow<Boolean> = userPreferences.hasSeenTutorial
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun saveUserName(name: String) = viewModelScope.launch { userPreferences.saveUserName(name) }
    fun saveAppTheme(theme: String) = viewModelScope.launch { userPreferences.saveAppTheme(theme) }
    fun saveHasSeenTutorial() = viewModelScope.launch { userPreferences.saveHasSeenTutorial(true) }

    // --- STATE FLOWS ---
    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLoans: StateFlow<List<Loan>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditCards: StateFlow<List<CreditCard>> = repository.allCreditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- THIS WAS MISSING ---
    val allScheduledTransactions: StateFlow<List<ScheduledTransaction>> = repository.allScheduledTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // ------------------------

    val totalCreditCardDebt: StateFlow<Double> = allCreditCards.map { cards ->
        cards.sumOf { it.amountOwed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = allAccounts.map { accounts ->
        accounts.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLoans: StateFlow<Double> = allLoans.map { loans ->
        loans.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- FILTERS ---
    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val startDate = _startDate.asStateFlow()
    private val _endDate = MutableStateFlow(LocalDate.now())
    val endDate = _endDate.asStateFlow()

    private val _selectedIncomeCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedIncomeCategories = _selectedIncomeCategories.asStateFlow()
    private val _selectedExpenseCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedExpenseCategories = _selectedExpenseCategories.asStateFlow()
    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAccountIds = _selectedAccountIds.asStateFlow()
    private val _selectedLoanIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedLoanIds = _selectedLoanIds.asStateFlow()
    private val _selectedCreditCardIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCreditCardIds = _selectedCreditCardIds.asStateFlow()

    // --- FILTERED LISTS ---
    val filteredIncomeTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, startDate, endDate, selectedIncomeCategories) { txs, start, end, cats ->
            txs.filter {
                val txDate = LocalDate.parse(it.date)
                it.type == TransactionType.INCOME && !txDate.isBefore(start) && !txDate.isAfter(end) && (cats.isEmpty() || cats.contains(it.category))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredExpenseTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, startDate, endDate, selectedExpenseCategories) { txs, start, end, cats ->
            txs.filter {
                val txDate = LocalDate.parse(it.date)
                it.type == TransactionType.EXPENSE && !txDate.isBefore(start) && !txDate.isAfter(end) && (cats.isEmpty() || cats.contains(it.category))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBalanceTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, startDate, endDate, selectedAccountIds) { txs, start, end, ids ->
            txs.filter {
                val txDate = LocalDate.parse(it.date)
                !txDate.isBefore(start) && !txDate.isAfter(end) && (ids.isEmpty() || ids.contains(it.accountId))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLoanTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, startDate, endDate, selectedLoanIds) { txs, start, end, ids ->
            txs.filter {
                val txDate = LocalDate.parse(it.date)
                it.loanId != null && !txDate.isBefore(start) && !txDate.isAfter(end) && (ids.isEmpty() || ids.contains(it.loanId))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCreditCardTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, startDate, endDate, selectedCreditCardIds) { txs, start, end, cardIds ->
            txs.filter {
                val txDate = LocalDate.parse(it.date)
                !txDate.isBefore(start) && !txDate.isAfter(end) &&
                        ((it.creditCardId != null && (cardIds.isEmpty() || cardIds.contains(it.creditCardId))) ||
                                (it.category == "Credit Card Payment" && (cardIds.isEmpty() || cardIds.contains(it.loanId))))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFilteredIncome: StateFlow<Double> = filteredIncomeTransactions.map { it.sumOf { tx -> tx.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalFilteredExpense: StateFlow<Double> = filteredExpenseTransactions.map { it.sumOf { tx -> tx.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currency: StateFlow<String> = userPreferences.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    fun saveCurrency(currency: String) = viewModelScope.launch {
        userPreferences.saveCurrency(currency)
    }

    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, startDate, endDate) { transactions, start, end ->
            transactions.filter {
                val txDate = LocalDate.parse(it.date)
                !txDate.isBefore(start) && !txDate.isAfter(end)
            }.sortedByDescending { it.date }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- EVENT FUNCTIONS ---
    fun onStartDateChange(newDate: LocalDate) { _startDate.value = newDate }
    fun onEndDateChange(newDate: LocalDate) { _endDate.value = newDate }
    fun setFilterToToday() { _startDate.value = LocalDate.now(); _endDate.value = LocalDate.now() }
    fun setFilterToThisMonth() { val today = LocalDate.now(); _startDate.value = today.withDayOfMonth(1); _endDate.value = today }
    fun setFilterToLast30Days() { val today = LocalDate.now(); _startDate.value = today.minusDays(30); _endDate.value = today }
    fun setFilterToThisYear() { val today = LocalDate.now(); _startDate.value = today.withDayOfYear(1); _endDate.value = today }

    fun toggleIncomeCategory(category: String) {
        val current = _selectedIncomeCategories.value.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        _selectedIncomeCategories.value = current.toSet()
    }
    fun toggleExpenseCategory(category: String) {
        val current = _selectedExpenseCategories.value.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        _selectedExpenseCategories.value = current.toSet()
    }
    fun toggleAccountId(id: Long) {
        val current = _selectedAccountIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedAccountIds.value = current.toSet()
    }
    fun toggleLoanId(id: Long) {
        val current = _selectedLoanIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedLoanIds.value = current.toSet()
    }
    fun toggleCreditCardId(id: Long) {
        val current = _selectedCreditCardIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedCreditCardIds.value = current.toSet()
    }

    fun insertAccount(account: Account) = viewModelScope.launch { repository.insertAccount(account) }
    fun updateAccount(account: Account) = viewModelScope.launch { repository.updateAccount(account) }
    fun deleteAccount(account: Account) = viewModelScope.launch { repository.deleteAccount(account) }

    fun updateLoan(loan: Loan) = viewModelScope.launch { repository.updateLoan(loan) }
    fun insertLoan(loan: Loan) = viewModelScope.launch { repository.insertLoan(loan) }
    fun insertLoanAndPayout(loan: Loan, accountId: Long) = viewModelScope.launch { repository.insertLoanAndPayout(loan, accountId) }
    fun deleteLoan(loan: Loan) = viewModelScope.launch { repository.deleteLoan(loan) }

    fun insertCreditCard(card: CreditCard) = viewModelScope.launch { repository.insertCreditCard(card) }
    fun updateCreditCard(card: CreditCard) = viewModelScope.launch { repository.updateCreditCard(card) }
    fun deleteCreditCard(card: CreditCard) = viewModelScope.launch { repository.deleteCreditCard(card) }

    // --- THESE WERE MISSING ---
    fun insertScheduledTransaction(tx: ScheduledTransaction) = viewModelScope.launch { repository.insertScheduledTransaction(tx) }
    fun updateScheduledTransaction(tx: ScheduledTransaction) = viewModelScope.launch { repository.updateScheduledTransaction(tx) }
    fun deleteScheduledTransaction(tx: ScheduledTransaction) = viewModelScope.launch { repository.deleteScheduledTransaction(tx) }
    // --------------------------

    suspend fun insertTransaction(transaction: Transaction): String? {
        return repository.insertTransactionAndUpdateBalances(transaction)
    }

    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction): String? {
        repository.deleteTransactionAndUpdateBalances(oldTx)
        return repository.insertTransactionAndUpdateBalances(newTx)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransactionAndUpdateBalances(transaction)
    }
}
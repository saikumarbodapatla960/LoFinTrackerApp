package com.skai.lofintrackerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // --- Preferences ---
    val userName = userPreferences.userName.map { it ?: "" }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val appTheme = userPreferences.appTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val hasSeenTutorial = userPreferences.hasSeenTutorial.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val currency = userPreferences.currency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    fun saveUserName(name: String) = viewModelScope.launch { userPreferences.saveUserName(name) }
    fun saveAppTheme(theme: String) = viewModelScope.launch { userPreferences.saveAppTheme(theme) }
    fun saveHasSeenTutorial() = viewModelScope.launch { userPreferences.saveHasSeenTutorial(true) }
    fun saveCurrency(curr: String) = viewModelScope.launch { userPreferences.saveCurrency(curr) }

    // --- Data Lists ---
    val allAccounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLoans = repository.allLoans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCreditCards = repository.allCreditCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allScheduledTransactions = repository.allScheduledTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Totals ---
    val totalCreditCardDebt = allCreditCards.map { it.sumOf { c -> c.amountOwed } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalBalance = allAccounts.map { it.sumOf { a -> a.balance } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalLoans = allLoans.map { it.sumOf { l -> l.remainingAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Filters ---
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

    // --- Filtered Logic ---
    val filteredIncomeTransactions = combine(allTransactions, startDate, endDate, selectedIncomeCategories) { txs, start, end, cats ->
        txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && it.type == TransactionType.INCOME && (cats.isEmpty() || cats.contains(it.category)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredExpenseTransactions = combine(allTransactions, startDate, endDate, selectedExpenseCategories) { txs, start, end, cats ->
        txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && it.type == TransactionType.EXPENSE && (cats.isEmpty() || cats.contains(it.category)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBalanceTransactions = combine(allTransactions, startDate, endDate, selectedAccountIds) { txs, start, end, ids ->
        txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && (ids.isEmpty() || ids.contains(it.accountId)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLoanTransactions = combine(allTransactions, startDate, endDate, selectedLoanIds) { txs, start, end, ids ->
        txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && it.loanId != null && (ids.isEmpty() || ids.contains(it.loanId)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCreditCardTransactions = combine(allTransactions, startDate, endDate, selectedCreditCardIds) { txs, start, end, ids ->
        txs.filter {
            val d = LocalDate.parse(it.date)
            !d.isBefore(start) && !d.isAfter(end) &&
                    ((it.creditCardId != null && (ids.isEmpty() || ids.contains(it.creditCardId))) ||
                            (it.category == "Credit Card Payment" && (ids.isEmpty() || ids.contains(it.loanId))))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFilteredIncome = filteredIncomeTransactions.map { it.sumOf { tx -> tx.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalFilteredExpense = filteredExpenseTransactions.map { it.sumOf { tx -> tx.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val filteredTransactions = combine(allTransactions, startDate, endDate) { txs, start, end ->
        txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Helper Functions ---
    fun onStartDateChange(d: LocalDate) { _startDate.value = d }
    fun onEndDateChange(d: LocalDate) { _endDate.value = d }
    fun toggleIncomeCategory(c: String) { _selectedIncomeCategories.value = _selectedIncomeCategories.value.toMutableSet().apply { if(contains(c)) remove(c) else add(c) } }
    fun toggleExpenseCategory(c: String) { _selectedExpenseCategories.value = _selectedExpenseCategories.value.toMutableSet().apply { if(contains(c)) remove(c) else add(c) } }
    fun toggleAccountId(id: Long) { _selectedAccountIds.value = _selectedAccountIds.value.toMutableSet().apply { if(contains(id)) remove(id) else add(id) } }
    fun toggleLoanId(id: Long) { _selectedLoanIds.value = _selectedLoanIds.value.toMutableSet().apply { if(contains(id)) remove(id) else add(id) } }
    fun toggleCreditCardId(id: Long) { _selectedCreditCardIds.value = _selectedCreditCardIds.value.toMutableSet().apply { if(contains(id)) remove(id) else add(id) } }

    // --- DB Operations ---
    fun insertAccount(a: Account) = viewModelScope.launch { repository.insertAccount(a) }
    fun updateAccount(a: Account) = viewModelScope.launch { repository.updateAccount(a) }
    fun deleteAccount(a: Account) = viewModelScope.launch { repository.deleteAccount(a) }
    fun updateLoan(l: Loan) = viewModelScope.launch { repository.updateLoan(l) }
    fun insertLoan(l: Loan) = viewModelScope.launch { repository.insertLoan(l) }
    fun insertLoanAndPayout(l: Loan, id: Long) = viewModelScope.launch { repository.insertLoanAndPayout(l, id) }
    fun deleteLoan(l: Loan) = viewModelScope.launch { repository.deleteLoan(l) }
    fun insertCreditCard(c: CreditCard) = viewModelScope.launch { repository.insertCreditCard(c) }
    fun updateCreditCard(c: CreditCard) = viewModelScope.launch { repository.updateCreditCard(c) }
    fun deleteCreditCard(c: CreditCard) = viewModelScope.launch { repository.deleteCreditCard(c) }

    // Scheduled Transactions
    fun insertScheduledTransaction(t: ScheduledTransaction) = viewModelScope.launch { repository.insertScheduledTransaction(t) }
    fun updateScheduledTransaction(t: ScheduledTransaction) = viewModelScope.launch { repository.updateScheduledTransaction(t) }
    fun deleteScheduledTransaction(t: ScheduledTransaction) = viewModelScope.launch { repository.deleteScheduledTransaction(t) }

    // Standard Transactions (Suspend for Dialogs)
    // MUST BE SUSPEND and RETURN STRING?
    suspend fun insertTransaction(transaction: Transaction): String? {
        return repository.insertTransactionAndUpdateBalances(transaction)
    }

    // MUST BE SUSPEND and RETURN STRING?
    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction): String? {
        repository.deleteTransactionAndUpdateBalances(oldTx)
        return repository.insertTransactionAndUpdateBalances(newTx)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransactionAndUpdateBalances(transaction)
    }

    // Add this val:
    val isAppLockEnabled = userPreferences.isAppLockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Add this fun:
    fun saveAppLockEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.saveAppLockEnabled(enabled) }
}
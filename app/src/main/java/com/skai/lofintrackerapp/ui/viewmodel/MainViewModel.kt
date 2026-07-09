package com.skai.lofintrackerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.lofintrackerapp.data.UserPreferences
import com.skai.lofintrackerapp.data.db.*
import com.skai.lofintrackerapp.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppBackupData(
    val accounts: List<Account>,
    val loans: List<Loan>,
    val transactions: List<Transaction>,
    val creditCards: List<CreditCard>,
    val scheduledTransactions: List<ScheduledTransaction>
)

class MainViewModel(
    private val repository: AppRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // --- Preferences ---
    val userName: StateFlow<String> = userPreferences.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val appTheme: StateFlow<String> = userPreferences.appTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val hasSeenTutorial: StateFlow<Boolean> = userPreferences.hasSeenTutorial.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val currency: StateFlow<String> = userPreferences.currency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")
    val reminderDays: StateFlow<Int> = userPreferences.reminderDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun saveUserName(name: String) = viewModelScope.launch { userPreferences.saveUserName(name) }
    fun saveAppTheme(theme: String) = viewModelScope.launch { userPreferences.saveAppTheme(theme) }
    fun saveHasSeenTutorial() = viewModelScope.launch { userPreferences.saveHasSeenTutorial(true) }
    fun saveCurrency(curr: String) = viewModelScope.launch { userPreferences.saveCurrency(curr) }
    fun saveReminderDays(days: Int) = viewModelScope.launch { userPreferences.saveReminderDays(days) }

    // --- Data Lists ---
    val allAccounts: StateFlow<List<Account>> = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLoans: StateFlow<List<Loan>> = repository.allLoans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCreditCards: StateFlow<List<CreditCard>> = repository.allCreditCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allScheduledTransactions: StateFlow<List<ScheduledTransaction>> = repository.allScheduledTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Totals ---
    val totalCreditCardDebt: StateFlow<Double> = allCreditCards.map { it.sumOf { c -> c.amountOwed } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalBalance: StateFlow<Double> = allAccounts.map { it.sumOf { a -> a.balance } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalLoans: StateFlow<Double> = allLoans.map { it.filter { !it.isClosed }.sumOf { l -> l.remainingAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalDebt: StateFlow<Double> = combine(totalLoans, totalCreditCardDebt) { loans, cards -> loans + cards }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Filters & Sorting ---
    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()
    private val _endDate = MutableStateFlow(LocalDate.now())
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _sortDescending = MutableStateFlow(true)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAccountIds: StateFlow<Set<Long>> = _selectedAccountIds.asStateFlow()
    private val _selectedLoanIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedLoanIds: StateFlow<Set<Long>> = _selectedLoanIds.asStateFlow()
    private val _selectedCreditCardIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCreditCardIds: StateFlow<Set<Long>> = _selectedCreditCardIds.asStateFlow()
    
    private val _selectedIncomeCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedIncomeCategories: StateFlow<Set<String>> = _selectedIncomeCategories.asStateFlow()
    private val _selectedExpenseCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedExpenseCategories: StateFlow<Set<String>> = _selectedExpenseCategories.asStateFlow()

    // --- Filtered Logic ---
    val filteredIncomeTransactions = combine(allTransactions, startDate, endDate, selectedIncomeCategories, sortDescending) { txs, start, end, cats, isDesc ->
        val filtered = txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && it.type == TransactionType.INCOME && (cats.isEmpty() || cats.contains(it.category)) }
        if (isDesc) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredExpenseTransactions = combine(allTransactions, startDate, endDate, selectedExpenseCategories, sortDescending) { txs, start, end, cats, isDesc ->
        val filtered = txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && it.type == TransactionType.EXPENSE && (cats.isEmpty() || cats.contains(it.category)) }
        if (isDesc) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBalanceTransactions = combine(allTransactions, startDate, endDate, selectedAccountIds, sortDescending) { txs, start, end, ids, isDesc ->
        val filtered = txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && (ids.isEmpty() || ids.contains(it.accountId)) }
        if (isDesc) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLoanTransactions = combine(allTransactions, startDate, endDate, selectedLoanIds, sortDescending) { txs, start, end, ids, isDesc ->
        val filtered = txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } && it.loanId != null && (ids.isEmpty() || ids.contains(it.loanId)) }
        if (isDesc) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCreditCardTransactions = combine(allTransactions, startDate, endDate, selectedCreditCardIds, sortDescending) { txs, start, end, ids, isDesc ->
        val filtered = txs.filter {
            val d = LocalDate.parse(it.date)
            !d.isBefore(start) && !d.isAfter(end) &&
                    ((it.creditCardId != null && (ids.isEmpty() || ids.contains(it.creditCardId))) ||
                            (it.category == "Credit Card Payment" && (ids.isEmpty() || ids.contains(it.loanId))))
        }
        if (isDesc) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFilteredIncome = filteredIncomeTransactions.map { it.sumOf { tx -> tx.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalFilteredExpense = filteredExpenseTransactions.map { it.sumOf { tx -> tx.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    val filteredTransactions = combine(allTransactions, startDate, endDate, sortDescending) { txs, start, end, isDesc ->
        val filtered = txs.filter { LocalDate.parse(it.date).let { d -> !d.isBefore(start) && !d.isAfter(end) } }
        if (isDesc) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Helper Functions ---
    fun onStartDateChange(d: LocalDate) { _startDate.value = d }
    fun onEndDateChange(d: LocalDate) { _endDate.value = d }
    fun toggleSortOrder() { _sortDescending.value = !_sortDescending.value }
    fun toggleAccountId(id: Long) { _selectedAccountIds.value = _selectedAccountIds.value.toMutableSet().apply { if(contains(id)) remove(id) else add(id) } }
    fun toggleLoanId(id: Long) { _selectedLoanIds.value = _selectedLoanIds.value.toMutableSet().apply { if(contains(id)) remove(id) else add(id) } }
    fun toggleCreditCardId(id: Long) { _selectedCreditCardIds.value = _selectedCreditCardIds.value.toMutableSet().apply { if(contains(id)) remove(id) else add(id) } }
    fun toggleIncomeCategory(c: String) { _selectedIncomeCategories.value = _selectedIncomeCategories.value.toMutableSet().apply { if(contains(c)) remove(c) else add(c) } }
    fun toggleExpenseCategory(c: String) { _selectedExpenseCategories.value = _selectedExpenseCategories.value.toMutableSet().apply { if(contains(c)) remove(c) else add(c) } }

    // --- DB Operations ---
    fun insertAccount(a: Account) = viewModelScope.launch { repository.insertAccount(a) }
    fun updateAccount(a: Account) = viewModelScope.launch { repository.updateAccount(a) }
    fun deleteAccount(a: Account) = viewModelScope.launch { repository.deleteAccount(a) }
    fun insertLoan(l: Loan) = viewModelScope.launch { repository.insertLoan(l) }
    fun updateLoan(l: Loan) = viewModelScope.launch { repository.updateLoan(l) }
    fun insertLoanAndPayout(l: Loan, payoutAccountId: Long) = viewModelScope.launch { repository.insertLoanAndPayout(l, payoutAccountId) }
    fun deleteLoan(l: Loan) = viewModelScope.launch { repository.deleteLoan(l) }
    fun insertCreditCard(c: CreditCard) = viewModelScope.launch { repository.insertCreditCard(c) }
    fun updateCreditCard(c: CreditCard) = viewModelScope.launch { repository.updateCreditCard(c) }
    fun deleteCreditCard(c: CreditCard) = viewModelScope.launch { repository.deleteCreditCard(c) }

    fun insertScheduledTransaction(t: ScheduledTransaction) = viewModelScope.launch { repository.insertScheduledTransaction(t) }
    fun updateScheduledTransaction(t: ScheduledTransaction) = viewModelScope.launch { repository.updateScheduledTransaction(t) }
    fun deleteScheduledTransaction(t: ScheduledTransaction) = viewModelScope.launch { repository.deleteScheduledTransaction(t) }

    suspend fun insertTransaction(transaction: Transaction): String? = repository.insertTransactionAndUpdateBalances(transaction)
    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction): String? = repository.updateTransactionAndUpdateBalances(oldTx, newTx)
    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch { repository.deleteTransactionAndUpdateBalances(transaction) }

    val isAppLockEnabled = userPreferences.isAppLockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun saveAppLockEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.saveAppLockEnabled(enabled) }

    // --- Backup & Restore ---
    fun getBackupJson(): String {
        val data = AppBackupData(allAccounts.value, allLoans.value, allTransactions.value, allCreditCards.value, allScheduledTransactions.value)
        return Json.encodeToString(data)
    }

    fun restoreFromJson(json: String): Boolean {
        return try {
            val data = Json.decodeFromString<AppBackupData>(json)
            viewModelScope.launch {
                data.accounts.forEach { repository.insertAccount(it) }
                data.loans.forEach { repository.insertLoan(it) }
                data.transactions.forEach { repository.insertTransactionAndUpdateBalances(it) }
                data.creditCards.forEach { repository.insertCreditCard(it) }
                data.scheduledTransactions.forEach { repository.insertScheduledTransaction(it) }
            }
            true
        } catch (e: Exception) { false }
    }
}
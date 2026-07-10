package com.skai.lofintrackerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.lofintrackerapp.data.UserPreferences
import com.skai.lofintrackerapp.data.db.*
import com.skai.lofintrackerapp.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class AppBackupData(
    val accounts: List<Account> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val scheduledTransactions: List<ScheduledTransaction> = emptyList(),
    // Include user preferences for a "full" restore
    val userName: String? = null,
    val appTheme: String? = null,
    val hasSeenTutorial: Boolean? = null,
    val currency: String? = null,
    val reminderDays: Int? = null,
    val isAppLockEnabled: Boolean? = null,
    val appInstallTimestamp: Long? = null
)

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject {
    put(name, value ?: JSONObject.NULL)
    return this
}

private fun JSONObject.optLongOrNull(name: String): Long? {
    return if (has(name) && !isNull(name)) optLong(name) else null
}

private fun JSONObject.optStringOrNull(name: String): String? {
    return if (has(name) && !isNull(name)) optString(name) else null
}

private fun JSONObject.optBooleanOrNull(name: String): Boolean? {
    return if (has(name) && !isNull(name)) optBoolean(name) else null
}

private fun JSONObject.optIntOrNull(name: String): Int? {
    return if (has(name) && !isNull(name)) optInt(name) else null
}

private fun parseAccountType(value: String?): AccountType {
    return runCatching { AccountType.valueOf(value ?: AccountType.OTHER.name) }.getOrDefault(AccountType.OTHER)
}

private fun parseTransactionType(value: String?): TransactionType {
    return runCatching { TransactionType.valueOf(value ?: TransactionType.EXPENSE.name) }.getOrDefault(TransactionType.EXPENSE)
}

private fun <T> Iterable<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray {
    val array = JSONArray()
    forEach { array.put(transform(it)) }
    return array
}

private fun JSONArray?.toJsonObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    val items = mutableListOf<JSONObject>()
    for (index in 0 until length()) {
        optJSONObject(index)?.let { items.add(it) }
    }
    return items
}

private fun Account.toJsonObject(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("type", type.name)
    .put("initialBalance", initialBalance)
    .put("balance", balance)

private fun JSONObject.toAccount(): Account = Account(
    id = optLong("id", 0L),
    name = optString("name", "Account"),
    type = parseAccountType(optStringOrNull("type")),
    initialBalance = optDouble("initialBalance", 0.0),
    balance = optDouble("balance", optDouble("initialBalance", 0.0))
)

private fun Loan.toJsonObject(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("lender", lender)
    .put("initialAmount", initialAmount)
    .put("remainingAmount", remainingAmount)
    .put("date", date)
    .put("interestRate", interestRate)
    .put("isClosed", isClosed)
    .put("totalInterestPaid", totalInterestPaid)

private fun JSONObject.toLoan(): Loan = Loan(
    id = optLong("id", 0L),
    name = optString("name", "Loan"),
    lender = optString("lender", ""),
    initialAmount = optDouble("initialAmount", 0.0),
    remainingAmount = optDouble("remainingAmount", optDouble("initialAmount", 0.0)),
    date = optString("date", LocalDate.now().toString()),
    interestRate = optDouble("interestRate", 0.0),
    isClosed = optBoolean("isClosed", false),
    totalInterestPaid = optDouble("totalInterestPaid", 0.0)
)

private fun CreditCard.toJsonObject(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("limit", limit)
    .put("amountOwed", amountOwed)
    .put("statementDate", statementDate)
    .put("dueDate", dueDate)
    .put("totalInterestPaid", totalInterestPaid)

private fun JSONObject.toCreditCard(): CreditCard = CreditCard(
    id = optLong("id", 0L),
    name = optString("name", "Credit Card"),
    limit = optDouble("limit", 0.0),
    amountOwed = optDouble("amountOwed", 0.0),
    statementDate = optInt("statementDate", 1),
    dueDate = optInt("dueDate", 1),
    totalInterestPaid = optDouble("totalInterestPaid", 0.0)
)

private fun Transaction.toJsonObject(): JSONObject = JSONObject()
    .put("id", id)
    .put("type", type.name)
    .put("amount", amount)
    .put("category", category)
    .putNullable("accountId", accountId)
    .putNullable("paymentMode", paymentMode)
    .putNullable("loanId", loanId)
    .putNullable("creditCardId", creditCardId)
    .put("date", date)
    .put("description", description)
    .put("interestAmount", interestAmount)
    .put("createdAt", createdAt)

private fun JSONObject.toTransaction(): Transaction = Transaction(
    id = optLong("id", 0L),
    type = parseTransactionType(optStringOrNull("type")),
    amount = optDouble("amount", 0.0),
    category = optString("category", ""),
    accountId = optLongOrNull("accountId"),
    paymentMode = optStringOrNull("paymentMode"),
    loanId = optLongOrNull("loanId"),
    creditCardId = optLongOrNull("creditCardId"),
    date = optString("date", LocalDate.now().toString()),
    description = optString("description", ""),
    interestAmount = optDouble("interestAmount", 0.0),
    createdAt = optLong("createdAt", System.currentTimeMillis())
)

private fun ScheduledTransaction.toJsonObject(): JSONObject = JSONObject()
    .put("id", id)
    .put("type", type.name)
    .put("amount", amount)
    .put("category", category)
    .putNullable("accountId", accountId)
    .putNullable("creditCardId", creditCardId)
    .putNullable("loanId", loanId)
    .putNullable("paymentMode", paymentMode)
    .put("description", description)
    .put("frequency", frequency)
    .put("nextDueDate", nextDueDate)

private fun JSONObject.toScheduledTransaction(): ScheduledTransaction = ScheduledTransaction(
    id = optLong("id", 0L),
    type = parseTransactionType(optStringOrNull("type")),
    amount = optDouble("amount", 0.0),
    category = optString("category", ""),
    accountId = optLongOrNull("accountId"),
    creditCardId = optLongOrNull("creditCardId"),
    loanId = optLongOrNull("loanId"),
    paymentMode = optStringOrNull("paymentMode"),
    description = optString("description", ""),
    frequency = optString("frequency", "Monthly"),
    nextDueDate = optString("nextDueDate", LocalDate.now().toString())
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
    val appInstallTimestamp: StateFlow<Long?> = userPreferences.appInstallTimestamp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveUserName(name: String) = viewModelScope.launch { userPreferences.saveUserName(name) }
    fun saveAppTheme(theme: String) = viewModelScope.launch { userPreferences.saveAppTheme(theme) }
    fun saveHasSeenTutorial() = viewModelScope.launch { userPreferences.saveHasSeenTutorial(true) }
    fun saveCurrency(curr: String) = viewModelScope.launch { userPreferences.saveCurrency(curr) }
    fun saveReminderDays(days: Int) = viewModelScope.launch {
        userPreferences.saveReminderDays(days)
        repository.enqueueImmediateReminderCheck()
    }

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

    fun onStartDateChange(date: LocalDate) { _startDate.value = date }
    fun onEndDateChange(date: LocalDate) { _endDate.value = date }
    fun toggleSortOrder() { _sortDescending.value = !_sortDescending.value }

    fun toggleAccountId(id: Long) {
        _selectedAccountIds.value = if (_selectedAccountIds.value.contains(id)) _selectedAccountIds.value - id else _selectedAccountIds.value + id
    }
    fun toggleLoanId(id: Long) {
        _selectedLoanIds.value = if (_selectedLoanIds.value.contains(id)) _selectedLoanIds.value - id else _selectedLoanIds.value + id
    }
    fun toggleCreditCardId(id: Long) {
        _selectedCreditCardIds.value = if (_selectedCreditCardIds.value.contains(id)) _selectedCreditCardIds.value - id else _selectedCreditCardIds.value + id
    }
    fun toggleIncomeCategory(cat: String) {
        _selectedIncomeCategories.value = if (_selectedIncomeCategories.value.contains(cat)) _selectedIncomeCategories.value - cat else _selectedIncomeCategories.value + cat
    }
    fun toggleExpenseCategory(cat: String) {
        _selectedExpenseCategories.value = if (_selectedExpenseCategories.value.contains(cat)) _selectedExpenseCategories.value - cat else _selectedExpenseCategories.value + cat
    }

    val filteredTransactions = combine(allTransactions, startDate, endDate, sortDescending) { txs, start, end, isDesc ->
        val filtered = txs.filter {
            val d = try { LocalDate.parse(it.date) } catch (e: Exception) { null }
            d != null && !d.isBefore(start) && !d.isAfter(end)
        }
        sortByAddedTime(filtered, isDesc)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBalanceTransactions = combine(filteredTransactions, selectedAccountIds) { txs, ids ->
        if (ids.isEmpty()) txs else txs.filter { it.accountId in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredIncomeTransactions = combine(filteredTransactions, selectedIncomeCategories) { txs, cats ->
        val incomeTxs = txs.filter { it.type == TransactionType.INCOME }
        if (cats.isEmpty()) incomeTxs else incomeTxs.filter { it.category in cats }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredExpenseTransactions = combine(filteredTransactions, selectedExpenseCategories) { txs, cats ->
        val expenseTxs = txs.filter { it.type == TransactionType.EXPENSE }
        if (cats.isEmpty()) expenseTxs else expenseTxs.filter { it.category in cats }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLoanTransactions = combine(filteredTransactions, selectedLoanIds) { txs, ids ->
        val loanTxs = txs.filter { it.type == TransactionType.LOAN_REPAYMENT || it.type == TransactionType.LOAN_DISBURSEMENT }
        if (ids.isEmpty()) loanTxs else loanTxs.filter { it.loanId in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCreditCardTransactions = combine(filteredTransactions, selectedCreditCardIds) { txs, ids ->
        val ccTxs = txs.filter { it.type == TransactionType.CREDIT_CARD_PAYMENT || it.type == TransactionType.CREDIT_CARD_SPEND }
        if (ids.isEmpty()) ccTxs else ccTxs.filter { it.creditCardId in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFilteredIncome = filteredIncomeTransactions.map { it.sumOf { t -> t.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalFilteredExpense = filteredExpenseTransactions.map { it.sumOf { t -> t.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val isCashAccountInitialBalanceEditable = allTransactions.map { txs ->
        txs.none { it.type != TransactionType.INITIAL_BALANCE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private fun sortByAddedTime(transactions: List<Transaction>, descending: Boolean): List<Transaction> {
        return if (descending) {
            transactions.sortedWith(compareByDescending<Transaction> { it.createdAt }.thenByDescending { it.id })
        } else {
            transactions.sortedWith(compareBy<Transaction> { it.createdAt }.thenBy { it.id })
        }
    }

    // --- DB Operations ---
    suspend fun insertAccount(a: Account): String? = repository.insertAccount(a)
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
        return JSONObject()
            .put("schemaVersion", 2)
            .put("accounts", allAccounts.value.toJsonArray { it.toJsonObject() })
            .put("loans", allLoans.value.toJsonArray { it.toJsonObject() })
            .put("transactions", allTransactions.value.toJsonArray { it.toJsonObject() })
            .put("creditCards", allCreditCards.value.toJsonArray { it.toJsonObject() })
            .put("scheduledTransactions", allScheduledTransactions.value.toJsonArray { it.toJsonObject() })
            .put("userName", userName.value)
            .put("appTheme", appTheme.value)
            .put("hasSeenTutorial", hasSeenTutorial.value)
            .put("currency", currency.value)
            .put("reminderDays", reminderDays.value)
            .put("isAppLockEnabled", isAppLockEnabled.value)
            .putNullable("appInstallTimestamp", appInstallTimestamp.value)
            .toString(2)
    }

    fun restoreFromJson(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(json)
                val data = AppBackupData(
                    accounts = root.optJSONArray("accounts").toJsonObjects().map { it.toAccount() },
                    loans = root.optJSONArray("loans").toJsonObjects().map { it.toLoan() },
                    transactions = root.optJSONArray("transactions").toJsonObjects().map { it.toTransaction() },
                    creditCards = root.optJSONArray("creditCards").toJsonObjects().map { it.toCreditCard() },
                    scheduledTransactions = root.optJSONArray("scheduledTransactions").toJsonObjects().map { it.toScheduledTransaction() },
                    userName = root.optStringOrNull("userName"),
                    appTheme = root.optStringOrNull("appTheme"),
                    hasSeenTutorial = root.optBooleanOrNull("hasSeenTutorial"),
                    currency = root.optStringOrNull("currency"),
                    reminderDays = root.optIntOrNull("reminderDays"),
                    isAppLockEnabled = root.optBooleanOrNull("isAppLockEnabled"),
                    appInstallTimestamp = root.optLongOrNull("appInstallTimestamp")
                )

                // Restore settings
                data.userName?.let { userPreferences.saveUserName(it) }
                data.appTheme?.let { userPreferences.saveAppTheme(it) }
                data.hasSeenTutorial?.let { userPreferences.saveHasSeenTutorial(it) }
                data.currency?.let { userPreferences.saveCurrency(it) }
                data.reminderDays?.let { userPreferences.saveReminderDays(it) }
                data.isAppLockEnabled?.let { userPreferences.saveAppLockEnabled(it) }
                data.appInstallTimestamp?.let { userPreferences.saveAppInstallTimestamp(it) }

                // Restore database records
                repository.restoreAllData(
                    accounts = data.accounts,
                    loans = data.loans,
                    transactions = data.transactions,
                    creditCards = data.creditCards,
                    scheduledTransactions = data.scheduledTransactions
                )
                repository.enqueueImmediateReminderCheck()
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }
}

package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PocketlyDatabase
import com.example.data.PocketlyRepository
import com.example.data.SettingsStore
import com.example.domain.Account
import com.example.domain.AccountKind
import com.example.domain.ActivityItem
import com.example.domain.Budget
import com.example.domain.Category
import com.example.domain.CategoryKind
import com.example.domain.CompoundingMode
import com.example.domain.DateUtils
import com.example.domain.FinanceEngine
import com.example.domain.Holding
import com.example.domain.HoldingContribution
import com.example.domain.HoldingMode
import com.example.domain.HoldingType
import com.example.domain.HoldingValuePoint
import com.example.domain.InvestRange
import com.example.domain.MoneyMath
import com.example.domain.MonthKey
import com.example.domain.RepeatFrequency
import com.example.domain.ScheduledItem
import com.example.domain.Transaction
import com.example.domain.TransactionKind
import com.example.domain.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab(val route: String, val label: String) {
    HOME("home", "Home"),
    ACCOUNTS("accounts", "Accounts"),
    BUDGETS("budgets", "Budgets"),
    INVEST("invest", "Invest"),
    PROFILE("profile", "Profile")
}

sealed interface SubScreen {
    data object None : SubScreen
    data object Search : SubScreen
    data object Scheduled : SubScreen
    data object Activity : SubScreen
    data class AccountDetail(val accountId: String) : SubScreen
    data class HoldingDetail(val holdingId: String) : SubScreen
    data object GrowthCalculator : SubScreen
    data object CategoriesManager : SubScreen
}

data class AddTransactionSheetState(
    val isOpen: Boolean = false,
    val editingTransaction: Transaction? = null,
    val initialCategoryId: String? = null,
    val initialAccountId: String? = null
)

data class GrowthCalculatorState(
    val startingCents: Long = 500_000L, // $5,000
    val monthlyContributionCents: Long = 10_000L, // $100
    val annualYieldPercent: Double = 4.5,
    val years: Int = 10,
    val compounding: CompoundingMode = CompoundingMode.MONTHLY
)

data class SnackbarNotice(
    val message: String,
    val undoTransaction: Transaction? = null
)

data class PocketlyUiState(
    val isInitialized: Boolean = false,
    val settings: UserSettings = UserSettings(),
    val isLocked: Boolean = false,
    val activeTab: MainTab = MainTab.HOME,
    val subScreen: SubScreen = SubScreen.None,
    val homeMode: Int = 0, // 0 = Cash flow, 1 = Overview
    val selectedMonth: MonthKey = DateUtils.today().toMonthKey(),
    val selectedHomeCategoryId: String? = null,
    val selectedOverviewCategoryId: String? = null,
    val selectedInvestRange: InvestRange = InvestRange.ONE_YEAR,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val scheduledItems: List<ScheduledItem> = emptyList(),
    val holdings: List<Holding> = emptyList(),
    val activityItems: List<ActivityItem> = emptyList(),
    val addTxSheet: AddTransactionSheetState = AddTransactionSheetState(),
    val calculator: GrowthCalculatorState = GrowthCalculatorState()
) {
    val activeAccounts: List<Account> get() = accounts.filter { it.archivedAt == null }
    val activeCategories: List<Category> get() = categories.filter { it.archivedAt == null }
    val activeHoldings: List<Holding> get() = holdings.filter { it.archivedAt == null }
    val unreadActivityCount: Int get() = activityItems.count { it.readAt == null }
    val accountsById: Map<String, Account> get() = accounts.associateBy { it.id }
    val categoriesById: Map<String, Category> get() = categories.associateBy { it.id }
}

private data class LocalNavState(
    val isInitialized: Boolean = false,
    val isLocked: Boolean = false,
    val activeTab: MainTab = MainTab.HOME,
    val subScreen: SubScreen = SubScreen.None,
    val homeMode: Int = 0,
    val selectedMonth: MonthKey = DateUtils.today().toMonthKey(),
    val selectedHomeCategoryId: String? = null,
    val selectedOverviewCategoryId: String? = null,
    val selectedInvestRange: InvestRange = InvestRange.ONE_YEAR,
    val addTxSheet: AddTransactionSheetState = AddTransactionSheetState(),
    val calculator: GrowthCalculatorState = GrowthCalculatorState()
)

class PocketlyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PocketlyRepository = PocketlyRepository(
        db = PocketlyDatabase.getInstance(application),
        settingsStore = SettingsStore(application)
    )

    private val navState = MutableStateFlow(LocalNavState())
    private val _notices = MutableSharedFlow<SnackbarNotice>(extraBufferCapacity = 4)
    val notices: SharedFlow<SnackbarNotice> = _notices.asSharedFlow()

    private var lastBackgroundEpochMs: Long = 0L

    private val dataFlow1 = combine(
        repository.settingsFlow,
        repository.accountsFlow,
        repository.categoriesFlow,
        repository.transactionsFlow
    ) { s, a, c, t -> Quad(s, a, c, t) }

    private val dataFlow2 = combine(
        repository.budgetsFlow,
        repository.scheduledItemsFlow,
        repository.holdingsFlow,
        repository.activityFlow
    ) { b, sch, h, act -> Quad(b, sch, h, act) }

    val uiState: StateFlow<PocketlyUiState> = combine(
        dataFlow1,
        dataFlow2,
        navState
    ) { d1, d2, nav ->
        PocketlyUiState(
            isInitialized = nav.isInitialized,
            settings = d1.first,
            isLocked = nav.isLocked,
            activeTab = nav.activeTab,
            subScreen = nav.subScreen,
            homeMode = nav.homeMode,
            selectedMonth = nav.selectedMonth,
            selectedHomeCategoryId = nav.selectedHomeCategoryId,
            selectedOverviewCategoryId = nav.selectedOverviewCategoryId,
            selectedInvestRange = nav.selectedInvestRange,
            accounts = d1.second,
            categories = d1.third,
            transactions = d1.fourth,
            budgets = d2.first,
            scheduledItems = d2.second,
            holdings = d2.third,
            activityItems = d2.fourth,
            addTxSheet = nav.addTxSheet,
            calculator = nav.calculator
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PocketlyUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultsInitialized()
            repository.runScheduledCatchUp()
            val settings = uiState.value.settings
            navState.update {
                it.copy(
                    isInitialized = true,
                    isLocked = settings.onboarded && settings.hasPin
                )
            }
        }
    }

    // ─── Lifecycle Lock Handling ───

    fun onAppBackgrounded() {
        lastBackgroundEpochMs = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val settings = uiState.value.settings
        if (settings.onboarded && settings.hasPin && lastBackgroundEpochMs > 0L) {
            val elapsed = System.currentTimeMillis() - lastBackgroundEpochMs
            if (elapsed >= settings.lockAfterMs) {
                navState.update { it.copy(isLocked = true) }
            }
        }
        viewModelScope.launch { repository.runScheduledCatchUp() }
    }

    fun unlockApp() {
        viewModelScope.launch { repository.resetPinFailures() }
        navState.update { it.copy(isLocked = false) }
    }

    suspend fun verifyPin(pin: String): Pair<Boolean, Long> {
        val result = repository.verifyPinAndUpdateLockout(pin)
        if (result.first) {
            navState.update { it.copy(isLocked = false) }
        }
        return result
    }

    fun completeOnboarding(currency: String, pin: String?, biometric: Boolean) {
        viewModelScope.launch {
            repository.updateCurrency(currency)
            if (!pin.isNullOrBlank()) {
                repository.setPin(pin)
                repository.updateBiometric(biometric)
            }
            repository.ensureDefaultsInitialized()
            repository.updateOnboarded(true)
            navState.update { it.copy(isLocked = false) }
        }
    }

    // ─── Navigation ───

    fun selectTab(tab: MainTab) {
        navState.update { it.copy(activeTab = tab, subScreen = SubScreen.None) }
    }

    fun openSubScreen(screen: SubScreen) {
        navState.update { it.copy(subScreen = screen) }
    }

    fun closeSubScreen() {
        navState.update { it.copy(subScreen = SubScreen.None) }
    }

    fun setHomeMode(mode: Int) {
        navState.update { it.copy(homeMode = mode) }
    }

    fun selectMonth(month: MonthKey) {
        navState.update { it.copy(selectedMonth = month, selectedOverviewCategoryId = null) }
    }

    fun toggleHomeCategoryFilter(categoryId: String?) {
        navState.update {
            it.copy(
                selectedHomeCategoryId = if (it.selectedHomeCategoryId == categoryId) null else categoryId
            )
        }
    }

    fun selectOverviewCategory(categoryId: String?) {
        navState.update { it.copy(selectedOverviewCategoryId = categoryId) }
    }

    fun selectInvestRange(range: InvestRange) {
        navState.update { it.copy(selectedInvestRange = range) }
    }

    // ─── Add / Edit Transaction Sheet ───

    fun openAddTransaction(categoryId: String? = null, accountId: String? = null) {
        navState.update {
            it.copy(
                addTxSheet = AddTransactionSheetState(
                    isOpen = true,
                    editingTransaction = null,
                    initialCategoryId = categoryId ?: it.selectedHomeCategoryId,
                    initialAccountId = accountId
                )
            )
        }
    }

    fun openEditTransaction(tx: Transaction) {
        navState.update {
            it.copy(
                addTxSheet = AddTransactionSheetState(
                    isOpen = true,
                    editingTransaction = tx,
                    initialCategoryId = tx.categoryId,
                    initialAccountId = tx.accountId
                )
            )
        }
    }

    fun closeTransactionSheet() {
        navState.update { it.copy(addTxSheet = AddTransactionSheetState(isOpen = false)) }
    }

    fun saveTransaction(
        existingId: String?,
        kind: TransactionKind,
        amountCents: Long,
        accountId: String,
        toAccountId: String?,
        categoryId: String?,
        merchant: String?,
        note: String?,
        dayKey: String,
        keepSheetOpen: Boolean = false
    ) {
        val nowIso = DateUtils.nowIso()
        val parsedDate = DateUtils.parseDayKey(dayKey) ?: DateUtils.today()
        val tx = Transaction(
            id = existingId ?: repository.newId("tx"),
            kind = kind,
            amountCents = amountCents.coerceIn(1L, MoneyMath.MAX_AMOUNT_CENTS),
            accountId = accountId,
            toAccountId = if (kind == TransactionKind.TRANSFER) toAccountId else null,
            categoryId = if (kind == TransactionKind.TRANSFER) null else categoryId,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            merchant = merchant?.trim()?.takeIf { it.isNotEmpty() },
            dateIso = parsedDate.toIsoStartOfDay(),
            dayKey = parsedDate.toDayKey(),
            scheduledId = null,
            createdAt = nowIso,
            updatedAt = nowIso,
            isDemo = false
        )
        viewModelScope.launch {
            repository.saveTransaction(tx)
            if (!keepSheetOpen) closeTransactionSheet()
            val s = uiState.value
            val catName = tx.categoryId?.let { s.categoriesById[it]?.name } ?: kind.label
            val signed = if (kind == TransactionKind.EXPENSE) -tx.amountCents else tx.amountCents
            val amtText = MoneyMath.formatCents(signed, s.settings.currency, showPlus = kind == TransactionKind.INCOME)
            _notices.emit(
                SnackbarNotice(
                    message = if (existingId == null) "Saved, $catName $amtText" else "Saved changes",
                    undoTransaction = if (existingId == null) tx else null
                )
            )
        }
    }

    fun deleteTransactionWithUndo(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx.id)
            closeTransactionSheet()
            _notices.emit(SnackbarNotice(message = "Deleted transaction", undoTransaction = tx))
        }
    }

    fun undoNoticeAction(notice: SnackbarNotice) {
        val tx = notice.undoTransaction ?: return
        viewModelScope.launch {
            if (notice.message.startsWith("Deleted")) {
                repository.saveTransaction(tx)
            } else {
                repository.deleteTransaction(tx.id)
            }
        }
    }

    // ─── Accounts ───

    fun saveAccount(
        existing: Account?,
        name: String,
        kind: AccountKind,
        icon: String,
        color: String,
        openingBalanceCents: Long
    ) {
        viewModelScope.launch {
            val s = uiState.value
            val account = Account(
                id = existing?.id ?: repository.newId("acc"),
                name = name.trim().ifEmpty { "Account" },
                kind = kind,
                icon = icon,
                color = color,
                openingBalanceCents = openingBalanceCents,
                currency = s.settings.currency,
                archivedAt = existing?.archivedAt,
                createdAt = existing?.createdAt ?: DateUtils.nowIso(),
                sortOrder = existing?.sortOrder ?: s.accounts.size,
                isDemo = false
            )
            repository.saveAccount(account)
            _notices.emit(SnackbarNotice(if (existing == null) "Added ${account.name}" else "Saved ${account.name}"))
        }
    }

    fun moveAccountOrder(accountId: String, delta: Int) {
        viewModelScope.launch {
            val list = uiState.value.activeAccounts.toMutableList()
            val idx = list.indexOfFirst { it.id == accountId }
            val target = (idx + delta).coerceIn(0, list.lastIndex)
            if (idx >= 0 && idx != target) {
                val moved = list.removeAt(idx)
                list.add(target, moved)
                repository.saveAccountsOrder(list)
            }
        }
    }

    fun archiveAccount(account: Account) {
        viewModelScope.launch {
            repository.archiveAccount(account)
            closeSubScreen()
            _notices.emit(SnackbarNotice("Archived ${account.name}"))
        }
    }

    // ─── Budgets ───

    fun saveBudget(categoryId: String, amountCents: Long) {
        viewModelScope.launch {
            repository.saveBudget(categoryId, amountCents)
            _notices.emit(SnackbarNotice(if (amountCents > 0L) "Budget saved" else "Budget removed"))
        }
    }

    // ─── Scheduled ───

    fun saveScheduledItem(
        existing: ScheduledItem?,
        name: String,
        kind: TransactionKind,
        amountCents: Long,
        accountId: String,
        categoryId: String?,
        repeat: RepeatFrequency,
        nextDueDayKey: String,
        paused: Boolean,
        autoPost: Boolean
    ) {
        viewModelScope.launch {
            val item = ScheduledItem(
                id = existing?.id ?: repository.newId("sch"),
                name = name.trim().ifEmpty { "Scheduled payment" },
                kind = if (kind == TransactionKind.INCOME) TransactionKind.INCOME else TransactionKind.EXPENSE,
                amountCents = amountCents.coerceIn(1L, MoneyMath.MAX_AMOUNT_CENTS),
                accountId = accountId,
                categoryId = categoryId,
                repeat = repeat,
                nextDueDayKey = nextDueDayKey,
                paused = paused,
                autoPost = autoPost,
                isDemo = false
            )
            repository.saveScheduledItem(item)
            repository.runScheduledCatchUp()
            _notices.emit(SnackbarNotice("Saved ${item.name}"))
        }
    }

    fun toggleScheduledPaused(item: ScheduledItem) {
        viewModelScope.launch {
            repository.saveScheduledItem(item.copy(paused = !item.paused))
        }
    }

    fun deleteScheduledItem(id: String) {
        viewModelScope.launch {
            repository.deleteScheduledItem(id)
            _notices.emit(SnackbarNotice("Deleted scheduled item"))
        }
    }

    // ─── Investments & Growth Calculator ───

    fun addHolding(
        name: String,
        type: HoldingType,
        mode: HoldingMode,
        principalCents: Long,
        apyPercent: Double?,
        startDayKey: String,
        maturityDayKey: String?,
        icon: String,
        color: String,
        openDetailAfter: Boolean = false
    ) {
        viewModelScope.launch {
            val id = repository.newId("hold")
            val validStart = DateUtils.parseDayKey(startDayKey)?.toDayKey() ?: DateUtils.today().toDayKey()
            val validMaturity = maturityDayKey?.takeIf { it.isNotBlank() }?.let { DateUtils.parseDayKey(it)?.toDayKey() }
            val contrib = if (principalCents > 0L) {
                listOf(HoldingContribution(repository.newId("hc"), id, validStart, principalCents))
            } else emptyList()
            val valPts = if (mode == HoldingMode.MANUAL && principalCents > 0L) {
                listOf(HoldingValuePoint(repository.newId("hv"), id, validStart, principalCents))
            } else emptyList()

            val holding = Holding(
                id = id,
                name = name.trim().ifEmpty { type.label },
                type = type,
                mode = mode,
                apy = if (mode == HoldingMode.YIELD) ((apyPercent ?: 0.0) / 100.0).coerceIn(0.0, 1.0) else null,
                startDayKey = validStart,
                maturityDayKey = validMaturity,
                contributions = contrib,
                valueHistory = valPts,
                icon = icon,
                color = color,
                archivedAt = null,
                isDemo = false
            )
            repository.saveHoldingWithInitialData(holding)
            _notices.emit(SnackbarNotice("Added ${holding.name}"))
            if (openDetailAfter) {
                navState.update { it.copy(activeTab = MainTab.INVEST, subScreen = SubScreen.HoldingDetail(id)) }
            }
        }
    }

    fun addHoldingDepositOrWithdraw(holdingId: String, signedAmountCents: Long, dayKey: String) {
        viewModelScope.launch {
            val validDay = DateUtils.parseDayKey(dayKey)?.toDayKey() ?: DateUtils.today().toDayKey()
            repository.addHoldingContribution(holdingId, signedAmountCents, validDay)
            _notices.emit(SnackbarNotice(if (signedAmountCents >= 0L) "Deposit added" else "Withdrawal logged"))
        }
    }

    fun updateHoldingRate(holding: Holding, newApyPercent: Double) {
        viewModelScope.launch {
            repository.updateHoldingMeta(holding.copy(apy = (newApyPercent / 100.0).coerceIn(0.0, 1.0)))
            _notices.emit(SnackbarNotice("Updated APY rate"))
        }
    }

    fun updateManualHoldingValue(holdingId: String, newValueCents: Long) {
        viewModelScope.launch {
            repository.addHoldingValuePoint(holdingId, newValueCents.coerceAtLeast(0L), DateUtils.today().toDayKey())
            _notices.emit(SnackbarNotice("Updated current value"))
        }
    }

    fun archiveHolding(holding: Holding) {
        viewModelScope.launch {
            repository.archiveHolding(holding)
            closeSubScreen()
            _notices.emit(SnackbarNotice("Archived ${holding.name}"))
        }
    }

    fun updateCalculator(transform: (GrowthCalculatorState) -> GrowthCalculatorState) {
        navState.update { it.copy(calculator = transform(it.calculator)) }
    }

    fun saveCalculatorAsInvestment() {
        val calc = uiState.value.calculator
        val name = "Savings plan (${MoneyMath.formatPercent(calc.annualYieldPercent, decimals = 1)})"
        addHolding(
            name = name,
            type = HoldingType.SAVINGS,
            mode = HoldingMode.YIELD,
            principalCents = calc.startingCents,
            apyPercent = calc.annualYieldPercent,
            startDayKey = DateUtils.today().toDayKey(),
            maturityDayKey = null,
            icon = "piggy_bank",
            color = "mint",
            openDetailAfter = true
        )
    }

    // ─── Activity ───

    fun markActivityReadAndNavigate(item: ActivityItem) {
        viewModelScope.launch { repository.markActivityRead(item.id) }
        when (item.kind) {
            com.example.domain.ActivityKind.BUDGET_90,
            com.example.domain.ActivityKind.BUDGET_100,
            com.example.domain.ActivityKind.MILESTONE -> selectTab(MainTab.BUDGETS)
            com.example.domain.ActivityKind.SCHEDULED_PAID,
            com.example.domain.ActivityKind.SCHEDULED_DUE -> openSubScreen(SubScreen.Scheduled)
        }
    }

    fun markAllActivityRead() {
        viewModelScope.launch { repository.markAllActivityRead() }
    }

    // ─── Categories Manager & Profile Settings ───

    fun saveCategory(existing: Category?, name: String, icon: String, color: String, kind: CategoryKind) {
        viewModelScope.launch {
            val cat = Category(
                id = existing?.id ?: repository.newId("cat"),
                name = name.trim().ifEmpty { "Category" },
                icon = icon,
                color = color,
                kind = kind,
                archivedAt = existing?.archivedAt,
                sortOrder = existing?.sortOrder ?: uiState.value.categories.size,
                isDemo = false
            )
            repository.saveCategory(cat)
            _notices.emit(SnackbarNotice("Saved ${cat.name}"))
        }
    }

    fun archiveCategory(category: Category) {
        viewModelScope.launch {
            repository.archiveCategory(category)
            _notices.emit(SnackbarNotice("Archived ${category.name}"))
        }
    }

    fun updateCurrency(code: String) = viewModelScope.launch {
        repository.updateCurrency(code)
        _notices.emit(SnackbarNotice("Currency set to $code"))
    }

    fun updateTheme(theme: String) = viewModelScope.launch { repository.updateTheme(theme) }
    fun updateReduceMotion(reduce: Boolean) = viewModelScope.launch { repository.updateReduceMotion(reduce) }
    fun updateSoundEffects(enabled: Boolean) = viewModelScope.launch { repository.updateSoundEffects(enabled) }
    fun updateBiometric(enabled: Boolean) = viewModelScope.launch { repository.updateBiometric(enabled) }
    fun updateLockAfter(lockAfter: String) = viewModelScope.launch { repository.updateLockAfter(lockAfter) }
    fun updateProfile(name: String, avatarColor: String) = viewModelScope.launch {
        repository.updateProfile(name, avatarColor)
        _notices.emit(SnackbarNotice("Profile updated"))
    }

    fun changePin(newPin: String) = viewModelScope.launch {
        repository.setPin(newPin)
        _notices.emit(SnackbarNotice("PIN updated"))
    }

    fun removePin() = viewModelScope.launch {
        repository.removePin()
        _notices.emit(SnackbarNotice("PIN removed"))
    }

    fun loadDemoData() = viewModelScope.launch {
        repository.loadDemoData()
        navState.update { it.copy(selectedMonth = DateUtils.today().toMonthKey(), selectedHomeCategoryId = null) }
        _notices.emit(SnackbarNotice("Demo data loaded"))
    }

    fun clearAllData() = viewModelScope.launch {
        repository.clearAllData()
        navState.update { it.copy(selectedHomeCategoryId = null, selectedOverviewCategoryId = null) }
        _notices.emit(SnackbarNotice("All local data cleared"))
    }

    suspend fun buildCsvExport(): String = repository.buildCsvExport()
    suspend fun buildJsonBackup(): String = repository.buildJsonBackup()

    fun importJsonBackup(rawJson: String) = viewModelScope.launch {
        val result = repository.importJsonBackup(rawJson)
        result.onSuccess { count ->
            _notices.emit(SnackbarNotice("Imported backup ($count transactions)"))
        }.onFailure { err ->
            _notices.emit(SnackbarNotice(err.message ?: "Couldn't read that backup file."))
        }
    }

    fun emitNotice(msg: String) = viewModelScope.launch {
        _notices.emit(SnackbarNotice(msg))
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

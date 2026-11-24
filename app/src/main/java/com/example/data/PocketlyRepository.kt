package com.example.data

import androidx.room.withTransaction
import com.example.domain.Account
import com.example.domain.ActivityItem
import com.example.domain.ActivityKind
import com.example.domain.Budget
import com.example.domain.Category
import com.example.domain.DateUtils
import com.example.domain.FinanceEngine
import com.example.domain.Holding
import com.example.domain.HoldingContribution
import com.example.domain.HoldingValuePoint
import com.example.domain.MoneyMath
import com.example.domain.PinSecurity
import com.example.domain.ScheduledItem
import com.example.domain.Transaction
import com.example.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class PocketlyRepository(
    private val db: PocketlyDatabase,
    private val settingsStore: SettingsStore
) {
    private val dao = db.dao()

    val settingsFlow: Flow<UserSettings> = settingsStore.settingsFlow

    val accountsFlow: Flow<List<Account>> = dao.observeAccounts().map { list -> list.map { it.toDomain() } }
    val categoriesFlow: Flow<List<Category>> = dao.observeCategories().map { list -> list.map { it.toDomain() } }
    val transactionsFlow: Flow<List<Transaction>> = dao.observeTransactions().map { list -> list.map { it.toDomain() } }
    val budgetsFlow: Flow<List<Budget>> = dao.observeBudgets().map { list -> list.map { it.toDomain() } }
    val scheduledItemsFlow: Flow<List<ScheduledItem>> = dao.observeScheduledItems().map { list -> list.map { it.toDomain() } }
    val activityFlow: Flow<List<ActivityItem>> = dao.observeActivity().map { list -> list.map { it.toDomain() } }

    val holdingsFlow: Flow<List<Holding>> = combine(
        dao.observeHoldings(),
        dao.observeContributions(),
        dao.observeValuePoints()
    ) { holdings, contribs, valuePoints ->
        val cMap = contribs.map { it.toDomain() }.groupBy { it.holdingId }
        val vMap = valuePoints.map { it.toDomain() }.groupBy { it.holdingId }
        holdings.map { h -> h.toDomain(cMap[h.id].orEmpty(), vMap[h.id].orEmpty()) }
    }

    fun newId(prefix: String = "id"): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    suspend fun ensureDefaultsInitialized() {
        val cats = dao.getCategoriesSnapshot()
        if (cats.isEmpty()) {
            dao.upsertCategories(DemoSeeder.defaultCategories().map { it.toEntity() })
        }
        val accounts = dao.getAccountsSnapshot()
        if (accounts.isEmpty()) {
            val currency = settingsStore.getSnapshot().currency
            dao.upsertAccount(DemoSeeder.defaultStarterAccount(currency).toEntity())
        }
    }

    suspend fun runScheduledCatchUp() {
        val settings = settingsStore.getSnapshot()
        val items = dao.getScheduledItemsSnapshot().map { it.toDomain() }
        if (items.isEmpty()) return
        val existingTx = dao.getTransactionsSnapshot()
        val existingKeys = existingTx
            .mapNotNull { tx -> tx.scheduledId?.let { "$it|${tx.dayKey}" } }
            .toSet()
        val accountsById = dao.getAccountsSnapshot().map { it.toDomain() }.associateBy { it.id }

        val outcome = FinanceEngine.processScheduledCatchUp(
            items = items,
            existingScheduledKeys = existingKeys,
            accountsById = accountsById,
            currencyCode = settings.currency,
            idGenerator = { newId("sch") }
        )

        if (outcome.transactionsToCreate.isNotEmpty() || outcome.updatedScheduledItems.isNotEmpty() || outcome.activityItemsToCreate.isNotEmpty()) {
            db.withTransaction {
                if (outcome.transactionsToCreate.isNotEmpty()) {
                    dao.insertTransactionsIgnoreConflict(outcome.transactionsToCreate.map { it.toEntity() })
                }
                if (outcome.updatedScheduledItems.isNotEmpty()) {
                    dao.upsertScheduledItems(outcome.updatedScheduledItems.map { it.toEntity() })
                }
                if (outcome.activityItemsToCreate.isNotEmpty()) {
                    dao.insertActivitiesIgnoreConflict(outcome.activityItemsToCreate.map { it.toEntity() })
                }
            }
            evaluateBudgetThresholds()
        }
    }

    // ─── Transactions ───

    suspend fun saveTransaction(tx: Transaction) {
        dao.upsertTransaction(tx.toEntity())
        settingsStore.updateLastUsed(tx.accountId, tx.categoryId)
        evaluateBudgetThresholds()
    }

    suspend fun deleteTransaction(id: String) {
        dao.deleteTransaction(id)
    }

    // ─── Accounts ───

    suspend fun saveAccount(account: Account) {
        dao.upsertAccount(account.toEntity())
    }

    suspend fun saveAccountsOrder(accounts: List<Account>) {
        val updated = accounts.mapIndexed { index, account -> account.copy(sortOrder = index).toEntity() }
        dao.upsertAccounts(updated)
    }

    suspend fun archiveAccount(account: Account) {
        dao.upsertAccount(account.copy(archivedAt = DateUtils.nowIso()).toEntity())
    }

    // ─── Categories ───

    suspend fun saveCategory(category: Category) {
        dao.upsertCategory(category.toEntity())
    }

    suspend fun saveCategoriesOrder(categories: List<Category>) {
        dao.upsertCategories(categories.mapIndexed { i, c -> c.copy(sortOrder = i).toEntity() })
    }

    suspend fun archiveCategory(category: Category) {
        dao.upsertCategory(category.copy(archivedAt = DateUtils.nowIso()).toEntity())
    }

    // ─── Budgets & Threshold Alerts ───

    suspend fun saveBudget(categoryId: String, amountCents: Long) {
        if (amountCents <= 0L) {
            dao.deleteBudgetByCategory(categoryId)
        } else {
            val existing = dao.getBudgetsSnapshot().firstOrNull { it.categoryId == categoryId }
            val budget = Budget(
                id = existing?.id ?: newId("bud"),
                categoryId = categoryId,
                amountCents = amountCents,
                period = "monthly",
                rollover = false,
                createdAt = existing?.createdAt ?: DateUtils.nowIso(),
                isDemo = false
            )
            dao.upsertBudget(budget.toEntity())
            evaluateBudgetThresholds()
        }
    }

    suspend fun evaluateBudgetThresholds() {
        val settings = settingsStore.getSnapshot()
        val today = DateUtils.today()
        val monthKey = today.toMonthKey()
        val budgets = dao.getBudgetsSnapshot().map { it.toDomain() }
        if (budgets.isEmpty()) return
        val transactions = dao.getTransactionsSnapshot().map { it.toDomain() }
        val catNames = dao.getCategoriesSnapshot().associate { it.id to it.name }
        val nowIso = DateUtils.nowIso()

        val alerts = mutableListOf<ActivityItem>()
        for (budget in budgets) {
            if (budget.amountCents <= 0L) continue
            val status = FinanceEngine.computeBudgetStatus(budget, transactions, monthKey, today)
            val label = if (budget.categoryId == "all") "Overall budget" else (catNames[budget.categoryId] ?: "Budget")
            if (status.rawRatio >= 1.0) {
                alerts.add(
                    ActivityItem(
                        id = newId("act"),
                        kind = ActivityKind.BUDGET_100,
                        title = "$label reached 100% of budget",
                        body = "You've spent ${MoneyMath.formatCents(status.spentCents, settings.currency)} of your ${MoneyMath.formatCents(budget.amountCents, settings.currency, dropWholeZeros = true)} budget.",
                        createdAt = nowIso,
                        refId = budget.categoryId,
                        dedupeKey = "bud100_${budget.categoryId}_${monthKey.toKey()}"
                    )
                )
            } else if (status.rawRatio >= 0.9) {
                alerts.add(
                    ActivityItem(
                        id = newId("act"),
                        kind = ActivityKind.BUDGET_90,
                        title = "$label is at 90% of budget",
                        body = "You've spent ${MoneyMath.formatCents(status.spentCents, settings.currency)} of your ${MoneyMath.formatCents(budget.amountCents, settings.currency, dropWholeZeros = true)} budget.",
                        createdAt = nowIso,
                        refId = budget.categoryId,
                        dedupeKey = "bud90_${budget.categoryId}_${monthKey.toKey()}"
                    )
                )
            }
        }
        if (alerts.isNotEmpty()) {
            dao.insertActivitiesIgnoreConflict(alerts.map { it.toEntity() })
        }
    }

    // ─── Scheduled ───

    suspend fun saveScheduledItem(item: ScheduledItem) {
        dao.upsertScheduledItem(item.toEntity())
    }

    suspend fun deleteScheduledItem(id: String) {
        dao.deleteScheduledItem(id)
    }

    // ─── Holdings ───

    suspend fun saveHoldingWithInitialData(holding: Holding) {
        db.withTransaction {
            dao.upsertHolding(holding.toEntity())
            if (holding.contributions.isNotEmpty()) {
                dao.upsertContributions(holding.contributions.map { it.toEntity() })
            }
            if (holding.valueHistory.isNotEmpty()) {
                dao.upsertValuePoints(holding.valueHistory.map { it.toEntity() })
            }
        }
    }

    suspend fun updateHoldingMeta(holding: Holding) {
        dao.upsertHolding(holding.toEntity())
    }

    suspend fun addHoldingContribution(holdingId: String, amountCents: Long, dayKey: String) {
        dao.upsertContribution(
            HoldingContribution(
                id = newId("hc"),
                holdingId = holdingId,
                dayKey = dayKey,
                amountCents = amountCents
            ).toEntity()
        )
    }

    suspend fun addHoldingValuePoint(holdingId: String, valueCents: Long, dayKey: String) {
        dao.upsertValuePoint(
            HoldingValuePoint(
                id = newId("hv"),
                holdingId = holdingId,
                dayKey = dayKey,
                valueCents = valueCents
            ).toEntity()
        )
    }

    suspend fun archiveHolding(holding: Holding) {
        dao.upsertHolding(holding.copy(archivedAt = DateUtils.nowIso()).toEntity())
    }

    // ─── Activity ───

    suspend fun markActivityRead(id: String) {
        dao.markActivityRead(id, DateUtils.nowIso())
    }

    suspend fun markAllActivityRead() {
        dao.markAllActivityRead(DateUtils.nowIso())
    }

    // ─── Settings & Security ───

    suspend fun updateCurrency(currency: String) = settingsStore.updateCurrency(currency)
    suspend fun updateTheme(theme: String) = settingsStore.updateTheme(theme)
    suspend fun updateReduceMotion(reduce: Boolean) = settingsStore.updateReduceMotion(reduce)
    suspend fun updateSoundEffects(enabled: Boolean) = settingsStore.updateSoundEffects(enabled)
    suspend fun updateBiometric(enabled: Boolean) = settingsStore.updateBiometric(enabled)
    suspend fun updateLockAfter(lockAfter: String) = settingsStore.updateLockAfter(lockAfter)
    suspend fun updateOnboarded(onboarded: Boolean) = settingsStore.updateOnboarded(onboarded)
    suspend fun updateProfile(name: String, avatarColor: String) = settingsStore.updateProfile(name, avatarColor)

    suspend fun setPin(pin: String) {
        val salt = PinSecurity.generateSaltHex()
        val hash = PinSecurity.hashPin(pin, salt)
        settingsStore.setPin(hash, salt)
    }

    suspend fun removePin() = settingsStore.clearPin()

    suspend fun verifyPinAndUpdateLockout(pin: String): Pair<Boolean, Long> {
        val s = settingsStore.getSnapshot()
        val now = System.currentTimeMillis()
        if (s.lockoutUntilEpochMs > now) {
            return false to (s.lockoutUntilEpochMs - now)
        }
        val hash = s.pinHash
        val salt = s.pinSalt
        if (hash.isNullOrBlank() || salt.isNullOrBlank()) return true to 0L
        val valid = PinSecurity.verifyPin(pin, salt, hash)
        if (valid) {
            settingsStore.updatePinFailures(0, 0L)
            return true to 0L
        }
        val nextFails = s.failedPinAttempts + 1
        val cooldown = PinSecurity.lockoutDurationMsForFailures(nextFails)
        val lockoutUntil = if (cooldown > 0L) now + cooldown else 0L
        settingsStore.updatePinFailures(nextFails, lockoutUntil)
        return false to cooldown
    }

    suspend fun resetPinFailures() {
        settingsStore.updatePinFailures(0, 0L)
    }

    // ─── Demo Data & Clear All & Backup ───

    suspend fun loadDemoData() {
        val settings = settingsStore.getSnapshot()
        val bundle = DemoSeeder.generateDemoBundle(currency = settings.currency)
        db.withTransaction {
            dao.clearTransactions()
            dao.clearBudgets()
            dao.clearScheduledItems()
            dao.clearContributions()
            dao.clearValuePoints()
            dao.clearHoldings()
            dao.clearActivity()
            dao.clearAccounts()
            dao.clearCategories()

            dao.upsertCategories(bundle.categories.map { it.toEntity() })
            dao.upsertAccounts(bundle.accounts.map { it.toEntity() })
            dao.upsertTransactions(bundle.transactions.map { it.toEntity() })
            dao.upsertBudgets(bundle.budgets.map { it.toEntity() })
            dao.upsertScheduledItems(bundle.scheduledItems.map { it.toEntity() })
            dao.upsertHoldings(bundle.holdings.map { it.toEntity() })
            dao.upsertContributions(bundle.holdings.flatMap { it.contributions }.map { it.toEntity() })
            dao.upsertValuePoints(bundle.holdings.flatMap { it.valueHistory }.map { it.toEntity() })
            dao.insertActivitiesIgnoreConflict(bundle.activityItems.map { it.toEntity() })
        }
        settingsStore.updateDemoLoaded(true)
        settingsStore.updateLastUsed(bundle.accounts.first().id, bundle.categories.first().id)
    }

    suspend fun clearAllData() {
        val currency = settingsStore.getSnapshot().currency
        db.withTransaction {
            dao.clearTransactions()
            dao.clearBudgets()
            dao.clearScheduledItems()
            dao.clearContributions()
            dao.clearValuePoints()
            dao.clearHoldings()
            dao.clearActivity()
            dao.clearAccounts()
            dao.clearCategories()

            dao.upsertCategories(DemoSeeder.defaultCategories().map { it.toEntity() })
            dao.upsertAccount(DemoSeeder.defaultStarterAccount(currency).toEntity())
        }
        settingsStore.updateDemoLoaded(false)
    }

    suspend fun buildCsvExport(): String {
        val settings = settingsStore.getSnapshot()
        val txs = dao.getTransactionsSnapshot().map { it.toDomain() }
        val accs = dao.getAccountsSnapshot().map { it.toDomain() }.associateBy { it.id }
        val cats = dao.getCategoriesSnapshot().map { it.toDomain() }.associateBy { it.id }
        return BackupCodec.exportTransactionsCsv(txs, accs, cats, settings.currency)
    }

    suspend fun buildJsonBackup(): String {
        val settings = settingsStore.getSnapshot()
        val accounts = dao.getAccountsSnapshot().map { it.toDomain() }
        val categories = dao.getCategoriesSnapshot().map { it.toDomain() }
        val txs = dao.getTransactionsSnapshot().map { it.toDomain() }
        val budgets = dao.getBudgetsSnapshot().map { it.toDomain() }
        val scheduled = dao.getScheduledItemsSnapshot().map { it.toDomain() }
        val contribs = dao.getContributionsSnapshot().map { it.toDomain() }.groupBy { it.holdingId }
        val values = dao.getValuePointsSnapshot().map { it.toDomain() }.groupBy { it.holdingId }
        val holdings = dao.getHoldingsSnapshot().map { it.toDomain(contribs[it.id].orEmpty(), values[it.id].orEmpty()) }
        val activities = dao.getActivitySnapshot().map { it.toDomain() }

        return BackupCodec.exportFullBackupJson(
            FullBackupSnapshot(
                currency = settings.currency,
                accounts = accounts,
                categories = categories,
                transactions = txs,
                budgets = budgets,
                scheduledItems = scheduled,
                holdings = holdings,
                activityItems = activities
            )
        )
    }

    suspend fun importJsonBackup(rawJson: String): Result<Int> {
        val parsed = BackupCodec.parseFullBackupJson(rawJson)
        val snapshot = parsed.getOrElse { return Result.failure(it) }
        db.withTransaction {
            dao.clearTransactions()
            dao.clearBudgets()
            dao.clearScheduledItems()
            dao.clearContributions()
            dao.clearValuePoints()
            dao.clearHoldings()
            dao.clearActivity()
            dao.clearAccounts()
            dao.clearCategories()

            dao.upsertCategories(snapshot.categories.map { it.toEntity() })
            dao.upsertAccounts(snapshot.accounts.map { it.toEntity() })
            dao.upsertTransactions(snapshot.transactions.map { it.toEntity() })
            dao.upsertBudgets(snapshot.budgets.map { it.toEntity() })
            dao.upsertScheduledItems(snapshot.scheduledItems.map { it.toEntity() })
            dao.upsertHoldings(snapshot.holdings.map { it.toEntity() })
            dao.upsertContributions(snapshot.holdings.flatMap { it.contributions }.map { it.toEntity() })
            dao.upsertValuePoints(snapshot.holdings.flatMap { it.valueHistory }.map { it.toEntity() })
        }
        settingsStore.updateCurrency(snapshot.currency)
        settingsStore.updateDemoLoaded(false)
        ensureDefaultsInitialized()
        return Result.success(snapshot.transactions.size)
    }
}

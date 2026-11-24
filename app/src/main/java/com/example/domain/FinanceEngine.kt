package com.example.domain

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

data class CashFlowSummary(
    val monthKey: MonthKey,
    val incomeCents: Long,
    val expenseCents: Long
) {
    val netCents: Long get() = incomeCents - expenseCents
}

data class CategorySpendSlice(
    val category: Category,
    val spentCents: Long,
    val sharePercent: Double,
    val budgetCents: Long?
)

data class BudgetStatus(
    val categoryId: String,
    val budgetCents: Long,
    val spentCents: Long,
    val idealPaceCents: Long,
    val isOverBudget: Boolean,
    val fillRatio: Float, // clamped 0f..1f so bars never overflow
    val savedCents: Long, // positive when under budget, negative when over
    val rawRatio: Double
) {
    fun gaugeHeadline(currencyCode: String): String =
        if (savedCents >= 0L) "Well done, you saved" else "Over budget this month"

    fun gaugePrimaryValue(currencyCode: String): String =
        MoneyMath.formatCents(abs(savedCents), currencyCode)

    fun gaugeSubtitle(currencyCode: String): String =
        if (savedCents >= 0L) {
            "of ${MoneyMath.formatCents(budgetCents, currencyCode, dropWholeZeros = true)} budget"
        } else {
            "You're ${MoneyMath.formatCents(abs(savedCents), currencyCode)} over budget"
        }
}

data class HoldingValuation(
    val holding: Holding,
    val currentValueCents: Long,
    val totalPutInCents: Long,
    val growthCents: Long,
    val growthPercent: Double?, // null if totalPutInCents <= 0
    val isMatured: Boolean,
    val estimatedYearlyYieldCents: Long,
    val lastUpdatedDayKey: String?
)

data class SeriesPoint(
    val dayKey: String,
    val valueCents: Long,
    val putInCents: Long = 0L
)

data class GrowthProjectionPoint(
    val monthIndex: Int,
    val yearNumber: Double,
    val valueCents: Long,
    val putInCents: Long
)

data class GrowthProjectionResult(
    val finalValueCents: Long,
    val totalPutInCents: Long,
    val totalEarnedCents: Long,
    val points: List<GrowthProjectionPoint>
)

data class ScheduledCatchUpOutcome(
    val transactionsToCreate: List<Transaction>,
    val updatedScheduledItems: List<ScheduledItem>,
    val activityItemsToCreate: List<ActivityItem>
)

object FinanceEngine {

    // ─────────────────────────── BALANCES ───────────────────────────

    /**
     * Account balance = openingBalance + income - expenses + transfers in - transfers out.
     * Credit accounts can go negative and display as negative.
     */
    fun computeAccountBalance(
        account: Account,
        transactions: List<Transaction>,
        upToDayKeyInclusive: String? = null
    ): Long {
        var balance = account.openingBalanceCents
        for (tx in transactions) {
            if (upToDayKeyInclusive != null && tx.dayKey > upToDayKeyInclusive) continue
            when (tx.kind) {
                TransactionKind.INCOME -> if (tx.accountId == account.id) balance += tx.amountCents
                TransactionKind.EXPENSE -> if (tx.accountId == account.id) balance -= tx.amountCents
                TransactionKind.TRANSFER -> {
                    if (tx.accountId == account.id) balance -= tx.amountCents
                    if (tx.toAccountId == account.id) balance += tx.amountCents
                }
            }
        }
        return balance
    }

    /**
     * Monthly delta for an account during a given month (transfers DO change an account's own balance).
     */
    fun computeAccountMonthChange(
        account: Account,
        transactions: List<Transaction>,
        monthKey: MonthKey
    ): Long {
        var delta = 0L
        for (tx in transactions) {
            if (!DateUtils.isInMonth(tx.dayKey, monthKey)) continue
            when (tx.kind) {
                TransactionKind.INCOME -> if (tx.accountId == account.id) delta += tx.amountCents
                TransactionKind.EXPENSE -> if (tx.accountId == account.id) delta -= tx.amountCents
                TransactionKind.TRANSFER -> {
                    if (tx.accountId == account.id) delta -= tx.amountCents
                    if (tx.toAccountId == account.id) delta += tx.amountCents
                }
            }
        }
        return delta
    }

    /**
     * Total wealth = sum of active account balances + sum of active investment valuations.
     */
    fun computeTotalWealthCents(
        accounts: List<Account>,
        transactions: List<Transaction>,
        holdings: List<Holding>,
        asOf: DayDate = DateUtils.today(),
        includeArchivedInHistory: Boolean = false
    ): Long {
        val asOfKey = asOf.toDayKey()
        val accountsSum = accounts
            .filter { includeArchivedInHistory || it.archivedAt == null }
            .sumOf { computeAccountBalance(it, transactions, upToDayKeyInclusive = asOfKey) }
        val investmentsSum = holdings
            .filter { includeArchivedInHistory || it.archivedAt == null }
            .sumOf { computeHoldingValuation(it, asOf).currentValueCents }
        return accountsSum + investmentsSum
    }

    // ─────────────────────────── CASH FLOW & PERIODS ───────────────────────────

    /**
     * Transfers are NEVER counted as income or expense in cash flow, overview, or budgets.
     */
    fun computeMonthCashFlow(
        transactions: List<Transaction>,
        monthKey: MonthKey,
        categoryFilterId: String? = null
    ): CashFlowSummary {
        var income = 0L
        var expense = 0L
        for (tx in transactions) {
            if (tx.kind == TransactionKind.TRANSFER) continue
            if (!DateUtils.isInMonth(tx.dayKey, monthKey)) continue
            if (categoryFilterId != null && tx.categoryId != categoryFilterId) continue
            when (tx.kind) {
                TransactionKind.INCOME -> income += tx.amountCents
                TransactionKind.EXPENSE -> expense += tx.amountCents
                TransactionKind.TRANSFER -> Unit
            }
        }
        return CashFlowSummary(monthKey, income, expense)
    }

    fun computeLastNMonthsCashFlow(
        transactions: List<Transaction>,
        anchorMonth: MonthKey,
        count: Int = 9,
        categoryFilterId: String? = null
    ): List<CashFlowSummary> {
        return (count - 1 downTo 0).map { offset ->
            computeMonthCashFlow(transactions, anchorMonth.plusMonths(-offset), categoryFilterId)
        }
    }

    fun computeExpenseBreakdown(
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        monthKey: MonthKey,
        accountIdFilter: String? = null
    ): List<CategorySpendSlice> {
        val spentByCategory = mutableMapOf<String, Long>()
        for (tx in transactions) {
            if (tx.kind != TransactionKind.EXPENSE) continue
            if (!DateUtils.isInMonth(tx.dayKey, monthKey)) continue
            if (accountIdFilter != null && tx.accountId != accountIdFilter) continue
            val catId = tx.categoryId ?: continue
            spentByCategory[catId] = (spentByCategory[catId] ?: 0L) + tx.amountCents
        }
        val total = spentByCategory.values.sum()
        if (total <= 0L) return emptyList()
        val budgetMap = budgets.associateBy { it.categoryId }
        val catMap = categories.associateBy { it.id }

        return spentByCategory.entries
            .mapNotNull { (catId, spent) ->
                val cat = catMap[catId] ?: return@mapNotNull null
                val pct = (spent.toDouble() / total.toDouble()) * 100.0
                CategorySpendSlice(
                    category = cat,
                    spentCents = spent,
                    sharePercent = pct,
                    budgetCents = budgetMap[catId]?.amountCents
                )
            }
            .sortedByDescending { it.spentCents }
    }

    // ─────────────────────────── BUDGETS ───────────────────────────

    fun computeBudgetStatus(
        budget: Budget,
        transactions: List<Transaction>,
        monthKey: MonthKey,
        today: DayDate = DateUtils.today()
    ): BudgetStatus {
        val spent = transactions
            .asSequence()
            .filter { it.kind == TransactionKind.EXPENSE }
            .filter { DateUtils.isInMonth(it.dayKey, monthKey) }
            .filter { budget.categoryId == "all" || it.categoryId == budget.categoryId }
            .sumOf { it.amountCents }

        val daysInPeriod = DateUtils.daysInMonth(monthKey.year, monthKey.month)
        val dayOfPeriod = when {
            today.toMonthKey() == monthKey -> today.day.coerceIn(1, daysInPeriod)
            today.toMonthKey() > monthKey -> daysInPeriod
            else -> 0
        }
        val idealPace = if (budget.amountCents > 0L && daysInPeriod > 0) {
            (budget.amountCents.toDouble() * dayOfPeriod.toDouble() / daysInPeriod.toDouble()).roundToLong()
        } else 0L

        val rawRatio = if (budget.amountCents > 0L) {
            spent.toDouble() / budget.amountCents.toDouble()
        } else 0.0
        val fillRatio = rawRatio.toFloat().coerceIn(0f, 1f)
        val saved = budget.amountCents - spent
        return BudgetStatus(
            categoryId = budget.categoryId,
            budgetCents = budget.amountCents,
            spentCents = spent,
            idealPaceCents = idealPace,
            isOverBudget = spent > budget.amountCents && budget.amountCents > 0L,
            fillRatio = fillRatio,
            savedCents = saved,
            rawRatio = rawRatio
        )
    }

    /**
     * Daily cumulative spend vs ideal linear budget pace for the selected month.
     */
    fun computeBudgetPaceSeries(
        allBudgetCents: Long,
        transactions: List<Transaction>,
        monthKey: MonthKey,
        today: DayDate = DateUtils.today()
    ): List<Pair<Long, Long>> {
        val daysInMonth = DateUtils.daysInMonth(monthKey.year, monthKey.month)
        val dailySpend = LongArray(daysInMonth + 1)
        for (tx in transactions) {
            if (tx.kind != TransactionKind.EXPENSE) continue
            if (!DateUtils.isInMonth(tx.dayKey, monthKey)) continue
            val d = DateUtils.parseDayKey(tx.dayKey)?.day ?: continue
            if (d in 1..daysInMonth) dailySpend[d] += tx.amountCents
        }
        val maxDayToPlot = when {
            today.toMonthKey() == monthKey -> today.day.coerceIn(1, daysInMonth)
            today.toMonthKey() > monthKey -> daysInMonth
            else -> 1
        }
        val result = ArrayList<Pair<Long, Long>>(daysInMonth)
        var running = 0L
        for (day in 1..daysInMonth) {
            if (day <= maxDayToPlot) running += dailySpend[day]
            val ideal = (allBudgetCents.toDouble() * day.toDouble() / daysInMonth.toDouble()).roundToLong()
            val spentVal = if (day <= maxDayToPlot) running else -1L
            result.add(spentVal to ideal)
        }
        return result
    }

    // ─────────────────────────── INVESTMENTS & YIELD ───────────────────────────

    /**
     * Yield mode: APY already includes compounding.
     * Value of deposit c made on date d at time t: c * (1 + apy) ^ ((t - d) / 365.0).
     * Stops accrual at maturityDate if passed.
     */
    fun computeHoldingValuation(
        holding: Holding,
        asOf: DayDate = DateUtils.today()
    ): HoldingValuation {
        val totalPutIn = holding.contributions
            .filter { DateUtils.parseDayKey(it.dayKey)?.let { d -> d <= asOf } ?: false }
            .sumOf { it.amountCents }

        if (holding.mode == HoldingMode.MANUAL) {
            val historyUpTo = holding.valueHistory
                .filter { DateUtils.parseDayKey(it.dayKey)?.let { d -> d <= asOf } ?: false }
                .sortedBy { it.dayKey }
            val latest = historyUpTo.lastOrNull()
            val currentVal = latest?.valueCents ?: totalPutIn.coerceAtLeast(0L)
            val growth = currentVal - totalPutIn
            val growthPct = if (totalPutIn > 0L) (growth.toDouble() / totalPutIn.toDouble()) * 100.0 else null
            return HoldingValuation(
                holding = holding,
                currentValueCents = currentVal,
                totalPutInCents = totalPutIn,
                growthCents = growth,
                growthPercent = growthPct,
                isMatured = false,
                estimatedYearlyYieldCents = 0L,
                lastUpdatedDayKey = latest?.dayKey
            )
        }

        val apy = (holding.apy ?: 0.0).coerceAtLeast(0.0)
        val maturityDate = holding.maturityDayKey?.let { DateUtils.parseDayKey(it) }
        val isMatured = maturityDate != null && asOf >= maturityDate
        val effectiveAsOf = if (maturityDate != null && asOf > maturityDate) maturityDate else asOf

        var valueDouble = 0.0
        for (c in holding.contributions) {
            val cDate = DateUtils.parseDayKey(c.dayKey) ?: continue
            if (cDate > asOf) continue
            val daysElapsed = DateUtils.daysBetween(cDate, effectiveAsOf).coerceAtLeast(0L)
            val years = daysElapsed.toDouble() / 365.0
            val factor = (1.0 + apy).pow(years)
            valueDouble += c.amountCents.toDouble() * factor
        }
        val currentVal = valueDouble.roundToLong().coerceAtLeast(0L)
        val growth = currentVal - totalPutIn
        val growthPct = if (totalPutIn > 0L) (growth.toDouble() / totalPutIn.toDouble()) * 100.0 else null
        val estYearly = if (isMatured || holding.archivedAt != null) {
            0L
        } else {
            (currentVal.toDouble() * apy).roundToLong()
        }

        return HoldingValuation(
            holding = holding,
            currentValueCents = currentVal,
            totalPutInCents = totalPutIn,
            growthCents = growth,
            growthPercent = growthPct,
            isMatured = isMatured,
            estimatedYearlyYieldCents = estYearly,
            lastUpdatedDayKey = asOf.toDayKey()
        )
    }

    fun computePortfolioSeries(
        holdings: List<Holding>,
        range: InvestRange,
        today: DayDate = DateUtils.today(),
        samplePoints: Int = 36
    ): List<SeriesPoint> {
        val active = holdings.filter { it.archivedAt == null }
        if (active.isEmpty()) return emptyList()

        val earliestDate = active
            .flatMap { h ->
                h.contributions.mapNotNull { DateUtils.parseDayKey(it.dayKey) } +
                    h.valueHistory.mapNotNull { DateUtils.parseDayKey(it.dayKey) } +
                    listOfNotNull(h.startDayKey?.let { DateUtils.parseDayKey(it) })
            }
            .minOrNull() ?: today.plusDays(-30)

        val startDate = when (val d = range.days) {
            null -> earliestDate
            else -> {
                val candidate = today.plusDays(-d)
                if (candidate < earliestDate) earliestDate else candidate
            }
        }
        val totalSpan = DateUtils.daysBetween(startDate, today).coerceAtLeast(1L)
        val steps = minOf(samplePoints, totalSpan.toInt()).coerceAtLeast(2)

        return (0..steps).map { idx ->
            val dayOffset = ((totalSpan.toDouble() * idx) / steps.toDouble()).roundToLong().toInt()
            val date = startDate.plusDays(dayOffset)
            var totalVal = 0L
            var totalPutIn = 0L
            for (h in active) {
                val v = computeHoldingValuation(h, date)
                totalVal += v.currentValueCents
                totalPutIn += v.totalPutInCents
            }
            SeriesPoint(dayKey = date.toDayKey(), valueCents = totalVal, putInCents = totalPutIn)
        }
    }

    // ─────────────────────────── GROWTH CALCULATOR ───────────────────────────

    /**
     * Exact Growth Calculator per Section 5:
     * - Monthly compounding: i = (1 + a)^(1/12) - 1; end-of-month contributions;
     *   FV = P(1+i)^m + PMT * ((1+i)^m - 1) / i (or P + PMT*m when i == 0).
     * - Yearly compounding: simulate month by month; simple interest (balance * a / 12)
     *   into pending pot, added to balance every 12th month; contributions enter at month end.
     *
     * Reference test: P=$5,000, PMT=$100, a=4.5%, Y=10, monthly -> ~$22,813 total, $17,000 put in, ~$5,813 earned.
     */
    fun calculateGrowth(
        startingCents: Long,
        monthlyContributionCents: Long,
        annualYieldRate: Double, // e.g. 0.045 for 4.5%
        years: Int,
        compounding: CompoundingMode
    ): GrowthProjectionResult {
        val safeP = startingCents.coerceAtLeast(0L).toDouble()
        val safePmt = monthlyContributionCents.coerceAtLeast(0L).toDouble()
        val safeA = annualYieldRate.coerceIn(0.0, 1.0)
        val safeYears = years.coerceIn(1, 50)
        val totalMonths = safeYears * 12

        val points = ArrayList<GrowthProjectionPoint>(totalMonths + 1)
        points.add(
            GrowthProjectionPoint(
                monthIndex = 0,
                yearNumber = 0.0,
                valueCents = startingCents.coerceAtLeast(0L),
                putInCents = startingCents.coerceAtLeast(0L)
            )
        )

        if (compounding == CompoundingMode.MONTHLY) {
            val monthlyRate = if (safeA == 0.0) 0.0 else (1.0 + safeA).pow(1.0 / 12.0) - 1.0
            for (m in 1..totalMonths) {
                val fv = if (monthlyRate == 0.0) {
                    safeP + safePmt * m
                } else {
                    val growthFactor = (1.0 + monthlyRate).pow(m)
                    safeP * growthFactor + safePmt * ((growthFactor - 1.0) / monthlyRate)
                }
                val putIn = (safeP + safePmt * m).roundToLong()
                points.add(
                    GrowthProjectionPoint(
                        monthIndex = m,
                        yearNumber = m / 12.0,
                        valueCents = fv.roundToLong(),
                        putInCents = putIn
                    )
                )
            }
        } else {
            var balance = safeP
            var pendingPot = 0.0
            for (m in 1..totalMonths) {
                pendingPot += balance * (safeA / 12.0)
                balance += safePmt
                if (m % 12 == 0) {
                    balance += pendingPot
                    pendingPot = 0.0
                }
                val putIn = (safeP + safePmt * m).roundToLong()
                points.add(
                    GrowthProjectionPoint(
                        monthIndex = m,
                        yearNumber = m / 12.0,
                        valueCents = (balance + pendingPot).roundToLong(),
                        putInCents = putIn
                    )
                )
            }
        }

        val last = points.last()
        val earned = (last.valueCents - last.putInCents).coerceAtLeast(0L)
        return GrowthProjectionResult(
            finalValueCents = last.valueCents,
            totalPutInCents = last.putInCents,
            totalEarnedCents = earned,
            points = points
        )
    }

    // ─────────────────────────── SCHEDULED ITEMS ───────────────────────────

    fun advanceScheduleDate(current: DayDate, repeat: RepeatFrequency): DayDate? = when (repeat) {
        RepeatFrequency.WEEKLY -> current.plusDays(7)
        RepeatFrequency.MONTHLY -> current.plusMonthsClamped(1)
        RepeatFrequency.YEARLY -> current.plusYearsClamped(1)
        RepeatFrequency.ONCE -> null
    }

    /**
     * Processes missed scheduled items up to today.
     * - autoPost items create transactions dated on their due date (never double-posted via existingKeys).
     * - non-autoPost items generate a single reminder ActivityItem if overdue/due soon.
     */
    fun processScheduledCatchUp(
        items: List<ScheduledItem>,
        existingScheduledKeys: Set<String>, // "$scheduledId|$dayKey"
        accountsById: Map<String, Account>,
        currencyCode: String,
        today: DayDate = DateUtils.today(),
        nowIso: String = DateUtils.nowIso(),
        idGenerator: () -> String
    ): ScheduledCatchUpOutcome {
        val createdTx = mutableListOf<Transaction>()
        val updatedItems = mutableListOf<ScheduledItem>()
        val createdActivity = mutableListOf<ActivityItem>()
        val seenKeys = existingScheduledKeys.toMutableSet()

        for (item in items) {
            if (item.paused) continue
            var dueDate = DateUtils.parseDayKey(item.nextDueDayKey) ?: continue

            if (!item.autoPost) {
                val diff = DateUtils.daysBetween(today, dueDate)
                if (diff <= 3L) {
                    val dedupe = "sched_due_${item.id}_${item.nextDueDayKey}"
                    val duePhrase = when {
                        diff < 0L -> "overdue since ${DateUtils.formatShortDate(item.nextDueDayKey)}"
                        diff == 0L -> "due today"
                        diff == 1L -> "due tomorrow"
                        else -> "due in $diff days"
                    }
                    createdActivity.add(
                        ActivityItem(
                            id = idGenerator(),
                            kind = ActivityKind.SCHEDULED_DUE,
                            title = "${item.name} is $duePhrase",
                            body = "${MoneyMath.formatCents(item.amountCents, currencyCode)} · ${DateUtils.formatShortDate(item.nextDueDayKey)}",
                            createdAt = nowIso,
                            refId = item.id,
                            dedupeKey = dedupe
                        )
                    )
                }
                continue
            }

            var currentItem = item
            var mutated = false
            var safetyCounter = 0
            while (dueDate <= today && safetyCounter < 120) {
                safetyCounter++
                val dueKey = dueDate.toDayKey()
                val uniqueKey = "${item.id}|$dueKey"
                if (seenKeys.add(uniqueKey)) {
                    createdTx.add(
                        Transaction(
                            id = idGenerator(),
                            kind = item.kind,
                            amountCents = item.amountCents,
                            accountId = item.accountId,
                            categoryId = item.categoryId,
                            merchant = item.name,
                            note = "Scheduled ${item.repeat.label}",
                            dateIso = dueDate.toIsoStartOfDay(),
                            dayKey = dueKey,
                            scheduledId = item.id,
                            createdAt = nowIso,
                            updatedAt = nowIso,
                            isDemo = item.isDemo
                        )
                    )
                    val accountName = accountsById[item.accountId]?.name ?: "account"
                    val signedCents = if (item.kind == TransactionKind.EXPENSE) -item.amountCents else item.amountCents
                    createdActivity.add(
                        ActivityItem(
                            id = idGenerator(),
                            kind = ActivityKind.SCHEDULED_PAID,
                            title = "${item.name} was paid from $accountName",
                            body = "${MoneyMath.formatCents(signedCents, currencyCode)} · ${DateUtils.formatShortDate(dueKey)}",
                            createdAt = nowIso,
                            refId = item.id,
                            dedupeKey = "sched_paid_${item.id}_$dueKey"
                        )
                    )
                }

                val next = advanceScheduleDate(dueDate, item.repeat)
                if (next == null) {
                    currentItem = currentItem.copy(paused = true)
                    mutated = true
                    break
                } else {
                    dueDate = next
                    currentItem = currentItem.copy(nextDueDayKey = dueDate.toDayKey())
                    mutated = true
                }
            }
            if (mutated) updatedItems.add(currentItem)
        }

        return ScheduledCatchUpOutcome(createdTx, updatedItems, createdActivity)
    }

    fun computeNext30DaysScheduledNetCents(
        items: List<ScheduledItem>,
        today: DayDate = DateUtils.today()
    ): Pair<Int, Long> {
        val horizon = today.plusDays(30)
        var count = 0
        var net = 0L
        for (item in items) {
            if (item.paused) continue
            var due = DateUtils.parseDayKey(item.nextDueDayKey) ?: continue
            var guard = 0
            while (due <= horizon && guard < 10) {
                guard++
                if (due >= today.plusDays(-7)) { // include recently overdue + next 30d
                    count++
                    net += if (item.kind == TransactionKind.EXPENSE) -item.amountCents else item.amountCents
                }
                due = advanceScheduleDate(due, item.repeat) ?: break
            }
        }
        return count to net
    }
}

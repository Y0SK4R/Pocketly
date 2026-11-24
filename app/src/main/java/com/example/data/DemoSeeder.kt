package com.example.data

import com.example.domain.Account
import com.example.domain.AccountKind
import com.example.domain.ActivityItem
import com.example.domain.ActivityKind
import com.example.domain.Budget
import com.example.domain.Category
import com.example.domain.CategoryKind
import com.example.domain.DateUtils
import com.example.domain.DayDate
import com.example.domain.Holding
import com.example.domain.HoldingContribution
import com.example.domain.HoldingMode
import com.example.domain.HoldingType
import com.example.domain.HoldingValuePoint
import com.example.domain.RepeatFrequency
import com.example.domain.ScheduledItem
import com.example.domain.Transaction
import com.example.domain.TransactionKind

data class SeedBundle(
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val scheduledItems: List<ScheduledItem>,
    val holdings: List<Holding>,
    val activityItems: List<ActivityItem>
)

object DemoSeeder {

    fun defaultCategories(nowIso: String = DateUtils.nowIso(), isDemo: Boolean = false): List<Category> = listOf(
        Category("cat_food", "Food & Drink", "utensils", "orange", CategoryKind.EXPENSE, null, 0, isDemo),
        Category("cat_shopping", "Shopping", "shopping_bag", "pink", CategoryKind.EXPENSE, null, 1, isDemo),
        Category("cat_dining", "Dining out", "dining", "sky", CategoryKind.EXPENSE, null, 2, isDemo),
        Category("cat_transport", "Transport", "car", "sky", CategoryKind.EXPENSE, null, 3, isDemo),
        Category("cat_home", "Home", "home", "lime", CategoryKind.EXPENSE, null, 4, isDemo),
        Category("cat_bills", "Bills", "zap", "slate", CategoryKind.EXPENSE, null, 5, isDemo),
        Category("cat_health", "Health", "heart_pulse", "purple", CategoryKind.EXPENSE, null, 6, isDemo),
        Category("cat_fun", "Fun", "ticket", "teal", CategoryKind.EXPENSE, null, 7, isDemo),
        Category("cat_travel", "Travel", "plane", "clay", CategoryKind.EXPENSE, null, 8, isDemo),
        Category("cat_other", "Other", "dots", "slate", CategoryKind.EXPENSE, null, 9, isDemo),
        Category("cat_salary", "Salary", "banknote", "mint", CategoryKind.INCOME, null, 10, isDemo),
        Category("cat_freelance", "Freelance", "trending_up", "teal", CategoryKind.INCOME, null, 11, isDemo),
        Category("cat_dividends", "Yield & Interest", "piggy_bank", "lime", CategoryKind.INCOME, null, 12, isDemo)
    )

    fun defaultStarterAccount(currency: String = "USD", nowIso: String = DateUtils.nowIso()): Account = Account(
        id = "acc_main",
        name = "Main Wallet",
        kind = AccountKind.WALLET,
        icon = "wallet",
        color = "mint",
        openingBalanceCents = 0L,
        currency = currency,
        createdAt = nowIso,
        sortOrder = 0,
        isDemo = false
    )

    fun generateDemoBundle(currency: String = "USD", today: DayDate = DateUtils.today()): SeedBundle {
        val nowIso = DateUtils.nowIso()
        val categories = defaultCategories(nowIso, isDemo = true)

        val accWallet = Account("demo_acc_wallet", "Main Wallet", AccountKind.WALLET, "wallet", "mint", 3_120_00L, currency, null, nowIso, 0, true)
        val accChecking = Account("demo_acc_checking", "Checking", AccountKind.CHECKING, "landmark", "sky", 6_500_00L, currency, null, nowIso, 1, true)
        val accCredit = Account("demo_acc_credit", "Credit Card", AccountKind.CREDIT, "credit_card", "pink", -180_00L, currency, null, nowIso, 2, true)
        val accSavings = Account("demo_acc_savings", "Savings", AccountKind.SAVINGS, "piggy_bank", "purple", 15_500_00L, currency, null, nowIso, 3, true)
        val accounts = listOf(accWallet, accChecking, accCredit, accSavings)

        val txs = mutableListOf<Transaction>()
        var txIdx = 1
        fun addTx(
            kind: TransactionKind,
            amountCents: Long,
            accountId: String,
            categoryId: String?,
            merchant: String,
            note: String? = null,
            date: DayDate,
            toAccountId: String? = null
        ) {
            txs.add(
                Transaction(
                    id = "demo_tx_${txIdx++}",
                    kind = kind,
                    amountCents = amountCents,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    note = note,
                    merchant = merchant,
                    dateIso = date.toIsoStartOfDay(),
                    dayKey = date.toDayKey(),
                    scheduledId = null,
                    createdAt = date.toIsoStartOfDay(),
                    updatedAt = date.toIsoStartOfDay(),
                    isDemo = true
                )
            )
        }

        // 8 previous months of realistic cash flow to power the 9-month grouped bar chart
        val historicalMonthlyIncome = listOf(420_000L, 485_000L, 440_000L, 510_000L, 490_000L, 560_000L, 530_000L, 580_000L)
        val historicalMonthlyFood = listOf(48_000L, 52_000L, 45_500L, 56_000L, 51_200L, 62_000L, 54_800L, 58_400L)
        val historicalMonthlyShop = listOf(31_000L, 42_000L, 28_000L, 49_000L, 35_000L, 54_000L, 39_500L, 44_000L)
        val historicalMonthlyHome = listOf(125_000L, 125_000L, 128_000L, 125_000L, 130_000L, 125_000L, 125_000L, 129_000L)
        val historicalMonthlyMisc = listOf(44_000L, 58_000L, 39_000L, 52_000L, 46_000L, 64_000L, 48_000L, 55_000L)

        val thisMonth = today.toMonthKey()
        for (i in 8 downTo 1) {
            val mk = thisMonth.plusMonths(-i)
            val idx = 8 - i
            addTx(TransactionKind.INCOME, historicalMonthlyIncome[idx], accChecking.id, "cat_salary", "Payroll deposit", "Monthly salary", DayDate(mk.year, mk.month, 1))
            addTx(TransactionKind.EXPENSE, historicalMonthlyHome[idx], accChecking.id, "cat_home", "Apartment rent", "Monthly rent", DayDate(mk.year, mk.month, 3))
            addTx(TransactionKind.EXPENSE, historicalMonthlyFood[idx], accWallet.id, "cat_food", "Trader Joe's", "Groceries", DayDate(mk.year, mk.month, 9))
            addTx(TransactionKind.EXPENSE, historicalMonthlyShop[idx], accCredit.id, "cat_shopping", "Target", "Home essentials", DayDate(mk.year, mk.month, 14))
            addTx(TransactionKind.EXPENSE, historicalMonthlyMisc[idx], accWallet.id, "cat_transport", "City transit & fuel", "Commute", DayDate(mk.year, mk.month, 19))
            addTx(TransactionKind.TRANSFER, 75_000L, accChecking.id, null, "Savings transfer", "Auto-save", DayDate(mk.year, mk.month, 22), toAccountId = accSavings.id)
        }

        // Current month transactions (including Today and Yesterday matching the mockups)
        val monthStart = thisMonth.startDate()
        addTx(TransactionKind.INCOME, 260_000L, accChecking.id, "cat_salary", "Payroll deposit", "Direct deposit", monthStart)
        if (today.day >= 2) {
            addTx(TransactionKind.EXPENSE, 24_500L, accWallet.id, "cat_food", "Whole Foods", "Weekly groceries", monthStart.plusDays(1))
        }
        if (today.day >= 4) {
            addTx(TransactionKind.EXPENSE, 17_700L, accCredit.id, "cat_shopping", "Uniqlo", "Autumn jacket", monthStart.plusDays(2))
        }
        if (today.day >= 5) {
            addTx(TransactionKind.EXPENSE, 12_800L, accChecking.id, "cat_home", "City Electric", "Utility bill", monthStart.plusDays(3))
        }
        if (today.day >= 6) {
            addTx(TransactionKind.EXPENSE, 7_800L, accWallet.id, "cat_dining", "Osteria", "Dinner with friends", monthStart.plusDays(4))
        }
        if (today.day >= 7) {
            addTx(TransactionKind.EXPENSE, 8_400L, accCredit.id, "cat_health", "Downtown Pharmacy", "Wellness & prescriptions", monthStart.plusDays(4))
        }
        if (today.day >= 8) {
            addTx(TransactionKind.EXPENSE, 9_600L, accWallet.id, "cat_transport", "Metro & Uber", "City rides", monthStart.plusDays(5))
        }
        addTx(TransactionKind.TRANSFER, 100_000L, accChecking.id, null, "Monthly savings", "To Savings", monthStart, toAccountId = accSavings.id)

        // Yesterday & Today vivid entries
        val yesterday = today.plusDays(-1)
        if (yesterday.toMonthKey() == thisMonth) {
            addTx(TransactionKind.EXPENSE, 36_400L, accWallet.id, "cat_food", "Green Market", "Fresh produce & pantry", yesterday)
            addTx(TransactionKind.EXPENSE, 4_250L, accCredit.id, "cat_fun", "Alamo Cinema", "Movie tickets", yesterday)
        }
        addTx(TransactionKind.EXPENSE, 1_850L, accWallet.id, "cat_food", "Blue Bottle", "Lunch", today)
        addTx(TransactionKind.EXPENSE, 6_488L, accCredit.id, "cat_shopping", "Bookshop", "Design & finance books", today)
        addTx(TransactionKind.EXPENSE, 2_850L, accWallet.id, "cat_transport", "Shell Station", "Fuel top-up", today)

        // Budgets matching the Budgets mockup
        val budgets = listOf(
            Budget("demo_b_all", "all", 560_000L, "monthly", false, nowIso, true),
            Budget("demo_b_food", "cat_food", 22_000L, "monthly", false, nowIso, true),
            Budget("demo_b_shop", "cat_shopping", 10_000L, "monthly", false, nowIso, true),
            Budget("demo_b_dining", "cat_dining", 9_000L, "monthly", false, nowIso, true)
        )

        // Scheduled items matching Scheduled mockup (Next 30 days = -$172.47)
        val scheduled = listOf(
            ScheduledItem("demo_sch_rent", "Rent", TransactionKind.EXPENSE, 12_000L, accChecking.id, "cat_home", RepeatFrequency.MONTHLY, today.plusDays(3).toDayKey(), false, true, true),
            ScheduledItem("demo_sch_phone", "Phone plan", TransactionKind.EXPENSE, 2_200L, accChecking.id, "cat_bills", RepeatFrequency.MONTHLY, today.plusDays(3).toDayKey(), false, false, true),
            ScheduledItem("demo_sch_stream", "Streaming", TransactionKind.EXPENSE, 699L, accCredit.id, "cat_fun", RepeatFrequency.MONTHLY, today.plusDays(7).toDayKey(), false, true, true),
            ScheduledItem("demo_sch_gym", "Gym", TransactionKind.EXPENSE, 2_348L, accWallet.id, "cat_health", RepeatFrequency.MONTHLY, today.plusDays(10).toDayKey(), false, false, true)
        )

        // Four Holdings matching the Investments mockup
        val oneYearAgo = today.plusDays(-340)
        val sixMonthsAgo = today.plusDays(-180)
        val threeMonthsAgo = today.plusDays(-90)

        val hSavings = Holding(
            id = "demo_h_hys",
            name = "High-yield savings",
            type = HoldingType.SAVINGS,
            mode = HoldingMode.YIELD,
            apy = 0.045,
            startDayKey = oneYearAgo.toDayKey(),
            maturityDayKey = null,
            contributions = listOf(
                HoldingContribution("demo_hc_1", "demo_h_hys", oneYearAgo.toDayKey(), 900_000L),
                HoldingContribution("demo_hc_2", "demo_h_hys", sixMonthsAgo.toDayKey(), 300_000L)
            ),
            valueHistory = emptyList(),
            icon = "piggy_bank",
            color = "orange",
            archivedAt = null,
            isDemo = true
        )

        val hTbill = Holding(
            id = "demo_h_tbill",
            name = "Treasury bills",
            type = HoldingType.TBILL,
            mode = HoldingMode.YIELD,
            apy = 0.041,
            startDayKey = sixMonthsAgo.toDayKey(),
            maturityDayKey = today.plusDays(120).toDayKey(),
            contributions = listOf(
                HoldingContribution("demo_hc_3", "demo_h_tbill", sixMonthsAgo.toDayKey(), 588_000L)
            ),
            valueHistory = emptyList(),
            icon = "document",
            color = "sky",
            archivedAt = null,
            isDemo = true
        )

        val hIndex = Holding(
            id = "demo_h_index",
            name = "Index fund",
            type = HoldingType.INDEX,
            mode = HoldingMode.MANUAL,
            apy = null,
            startDayKey = oneYearAgo.toDayKey(),
            maturityDayKey = null,
            contributions = listOf(
                HoldingContribution("demo_hc_4", "demo_h_index", oneYearAgo.toDayKey(), 310_000L),
                HoldingContribution("demo_hc_5", "demo_h_index", sixMonthsAgo.toDayKey(), 126_000L)
            ),
            valueHistory = listOf(
                HoldingValuePoint("demo_hv_1", "demo_h_index", oneYearAgo.toDayKey(), 310_000L),
                HoldingValuePoint("demo_hv_2", "demo_h_index", sixMonthsAgo.toDayKey(), 478_000L),
                HoldingValuePoint("demo_hv_3", "demo_h_index", threeMonthsAgo.toDayKey(), 542_000L),
                HoldingValuePoint("demo_hv_4", "demo_h_index", today.plusDays(-2).toDayKey(), 590_000L)
            ),
            icon = "trending_up",
            color = "pink",
            archivedAt = null,
            isDemo = true
        )

        val hCd = Holding(
            id = "demo_h_cd",
            name = "Certificate of deposit",
            type = HoldingType.CD,
            mode = HoldingMode.YIELD,
            apy = 0.050,
            startDayKey = today.plusDays(-500).toDayKey(),
            maturityDayKey = today.plusDays(60).toDayKey(),
            contributions = listOf(
                HoldingContribution("demo_hc_6", "demo_h_cd", today.plusDays(-500).toDayKey(), 238_000L)
            ),
            valueHistory = emptyList(),
            icon = "certificate",
            color = "purple",
            archivedAt = null,
            isDemo = true
        )

        val activities = listOf(
            ActivityItem(
                id = "demo_act_1",
                kind = ActivityKind.BUDGET_90,
                title = "Shopping is at 90% of budget",
                body = "You've spent most of your shopping budget this month.",
                createdAt = today.toIsoStartOfDay(),
                readAt = null,
                refId = "cat_shopping",
                dedupeKey = "demo_act_1"
            ),
            ActivityItem(
                id = "demo_act_2",
                kind = ActivityKind.SCHEDULED_PAID,
                title = "Rent was paid from Checking",
                body = "-$120.00 · ${DateUtils.formatShortDate(today.toDayKey())}",
                createdAt = today.toIsoStartOfDay(),
                readAt = null,
                refId = "demo_sch_rent",
                dedupeKey = "demo_act_2"
            ),
            ActivityItem(
                id = "demo_act_3",
                kind = ActivityKind.MILESTONE,
                title = "You stayed under your overall budget",
                body = "Your spending pace is well within your $5,600 monthly budget.",
                createdAt = today.plusDays(-4).toIsoStartOfDay(),
                readAt = today.plusDays(-3).toIsoStartOfDay(),
                refId = "all",
                dedupeKey = "demo_act_3"
            )
        )

        return SeedBundle(
            accounts = accounts,
            categories = categories,
            transactions = txs,
            budgets = budgets,
            scheduledItems = scheduled,
            holdings = listOf(hSavings, hTbill, hIndex, hCd),
            activityItems = activities
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryKind
import com.example.domain.DateUtils
import com.example.domain.FinanceEngine
import com.example.domain.MoneyMath
import com.example.domain.Transaction
import com.example.domain.TransactionKind
import com.example.ui.components.AnimatedCurrencyText
import com.example.ui.components.CategoryIconTile
import com.example.ui.components.CategoryProgressRing
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.GroupedCashFlowBarChart
import com.example.ui.components.MoneyPillChip
import com.example.ui.components.PocketlyCard
import com.example.ui.components.PocketlyEmptyState
import com.example.ui.components.SegmentedTabs
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import com.example.ui.viewmodel.PocketlyUiState
import kotlin.math.abs

@Composable
fun HomeOverviewScreen(
    state: PocketlyUiState,
    onOpenActivity: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenScheduled: () -> Unit,
    onSelectHomeMode: (Int) -> Unit,
    onSelectMonth: (com.example.domain.MonthKey) -> Unit,
    onToggleHomeCategory: (String?) -> Unit,
    onSelectOverviewCategory: (String?) -> Unit,
    onOpenAddTransaction: (categoryId: String?) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onLoadDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val today = remember { DateUtils.today() }
    val thisMonth = remember(today) { today.toMonthKey() }
    val selectedMonth = state.selectedMonth

    val monthCashFlow = remember(state.transactions, selectedMonth, state.selectedHomeCategoryId) {
        FinanceEngine.computeMonthCashFlow(state.transactions, selectedMonth, state.selectedHomeCategoryId)
    }
    val nineMonthBars = remember(state.transactions, selectedMonth, state.selectedHomeCategoryId) {
        FinanceEngine.computeLastNMonthsCashFlow(state.transactions, selectedMonth, count = 9, categoryFilterId = state.selectedHomeCategoryId)
    }
    val expenseSlices = remember(state.transactions, state.categories, state.budgets, selectedMonth) {
        FinanceEngine.computeExpenseBreakdown(state.transactions, state.categories, state.budgets, selectedMonth)
    }
    val prevMonthFlow = remember(state.transactions, selectedMonth) {
        FinanceEngine.computeMonthCashFlow(state.transactions, selectedMonth.plusMonths(-1))
    }
    val scheduledSummary = remember(state.scheduledItems, today) {
        FinanceEngine.computeNext30DaysScheduledNetCents(state.scheduledItems, today)
    }

    val activeFilterCat = if (state.homeMode == 1) state.selectedOverviewCategoryId else state.selectedHomeCategoryId

    // Day-grouped transactions for selected month + optional category filter
    val groupedByDay = remember(state.transactions, selectedMonth, activeFilterCat) {
        state.transactions
            .filter { DateUtils.isInMonth(it.dayKey, selectedMonth) }
            .filter { activeFilterCat == null || it.categoryId == activeFilterCat }
            .groupBy { it.dayKey }
            .toList()
            .sortedByDescending { it.first }
    }

    // Category rings (All + expense categories with spend or budget)
    val expenseCategories = remember(state.activeCategories, expenseSlices, state.budgets) {
        val sliceMap = expenseSlices.associateBy { it.category.id }
        state.activeCategories
            .filter { it.kind == CategoryKind.EXPENSE }
            .sortedByDescending { sliceMap[it.id]?.spentCents ?: 0L }
            .take(8)
    }

    val allBudget = remember(state.budgets) { state.budgets.firstOrNull { it.categoryId == "all" } }
    val totalMonthExpense = remember(state.transactions, selectedMonth) {
        FinanceEngine.computeMonthCashFlow(state.transactions, selectedMonth).expenseCents
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header: Pocketly left, Bell (with unread dot) + Search right
        item(key = "home_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pocketly",
                    style = PocketlyTextStyles.screenTitle,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = onOpenActivity,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_activity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = "Activity notifications",
                            tint = colors.ink
                        )
                    }
                    if (state.unreadActivityCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 11.dp, end = 11.dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(colors.mint)
                        )
                    }
                }
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search transactions",
                        tint = colors.ink
                    )
                }
            }
        }

        // Empty State when no transactions exist at all
        if (state.transactions.isEmpty()) {
            item(key = "home_empty") {
                PocketlyEmptyState(
                    title = "Nothing logged yet",
                    subtitle = "Add your first expense or income in 3 taps, or load sample data to explore every screen.",
                    primaryLabel = "Add your first transaction",
                    onPrimaryClick = { onOpenAddTransaction(null) },
                    secondaryLabel = "Load demo data",
                    onSecondaryClick = onLoadDemoData
                )
            }
            return@LazyColumn
        }

        // Row of Category Progress Rings ("All" first)
        item(key = "home_rings") {
            val budgetByCat = remember(state.budgets) { state.budgets.associateBy { it.categoryId } }
            val spentByCat = remember(expenseSlices) { expenseSlices.associate { it.category.id to it.spentCents } }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item(key = "ring_all") {
                    val allProgress = if (allBudget != null && allBudget.amountCents > 0L) {
                        (totalMonthExpense.toFloat() / allBudget.amountCents.toFloat()).coerceIn(0f, 1f)
                    } else if (totalMonthExpense > 0L) 0.65f else 0f
                    CategoryProgressRing(
                        label = "All",
                        iconKey = "wallet",
                        colorKey = "mint",
                        progress = allProgress,
                        selected = state.selectedHomeCategoryId == null,
                        isOverBudget = allBudget != null && totalMonthExpense > allBudget.amountCents,
                        onClick = { onToggleHomeCategory(null) },
                        modifier = Modifier.testTag("ring_all")
                    )
                }
                items(expenseCategories, key = { it.id }) { cat ->
                    val spent = spentByCat[cat.id] ?: 0L
                    val bud = budgetByCat[cat.id]
                    val ratio = when {
                        bud != null && bud.amountCents > 0L -> (spent.toFloat() / bud.amountCents.toFloat()).coerceIn(0f, 1f)
                        totalMonthExpense > 0L -> (spent.toFloat() / totalMonthExpense.toFloat()).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    val over = bud != null && bud.amountCents > 0L && spent > bud.amountCents
                    CategoryProgressRing(
                        label = cat.name.split(" ").first(),
                        iconKey = cat.icon,
                        colorKey = cat.color,
                        progress = ratio,
                        selected = state.selectedHomeCategoryId == cat.id,
                        isOverBudget = over,
                        onClick = { onToggleHomeCategory(cat.id) },
                        modifier = Modifier.testTag("ring_${cat.id}")
                    )
                }
            }
        }

        // Hero Card: Cash flow | Overview switch
        item(key = "home_hero_card") {
            PocketlyCard {
                SegmentedTabs(
                    options = listOf("Cash flow", "Overview"),
                    selectedIndex = state.homeMode,
                    onSelect = onSelectHomeMode,
                    tagPrefix = "home_mode"
                )
                Spacer(Modifier.height(16.dp))

                AnimatedContent(
                    targetState = state.homeMode,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "home_overview_switch"
                ) { mode ->
                    if (mode == 0) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (state.selectedHomeCategoryId != null) {
                                    "${state.categoriesById[state.selectedHomeCategoryId]?.name ?: "Category"} · ${selectedMonth.shortLabel()}"
                                } else if (selectedMonth == thisMonth) {
                                    "Cash flow this month"
                                } else {
                                    "Cash flow · ${selectedMonth.shortLabel()}"
                                },
                                style = PocketlyTextStyles.label,
                                color = colors.inkMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            AnimatedCurrencyText(
                                targetCents = monthCashFlow.netCents,
                                currencyCode = currency,
                                style = PocketlyTextStyles.heroNumber,
                                color = colors.ink
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MoneyPillChip(
                                    amountCents = monthCashFlow.incomeCents,
                                    currencyCode = currency,
                                    positive = true,
                                    dropWholeZeros = true
                                )
                                MoneyPillChip(
                                    amountCents = monthCashFlow.expenseCents,
                                    currencyCode = currency,
                                    positive = false,
                                    dropWholeZeros = true
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            GroupedCashFlowBarChart(
                                months = nineMonthBars,
                                currencyCode = currency
                            )
                        }
                    } else {
                        // Overview inside Home (matching Overview mockup)
                        OverviewContent(
                            state = state,
                            thisMonth = thisMonth,
                            selectedMonth = selectedMonth,
                            monthCashFlow = monthCashFlow,
                            prevMonthExpenseCents = prevMonthFlow.expenseCents,
                            expenseSlices = expenseSlices,
                            nineMonthBars = nineMonthBars,
                            onSelectMonth = onSelectMonth,
                            onSelectCategory = onSelectOverviewCategory
                        )
                    }
                }
            }
        }

        // Scheduled Summary Card (opens Scheduled)
        item(key = "home_scheduled_summary") {
            val (count, netCents) = scheduledSummary
            PocketlyCard(
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                onClick = onOpenScheduled,
                modifier = Modifier.testTag("home_scheduled_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.amberSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = colors.amber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scheduled", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                        val desc = if (count == 0) {
                            "No upcoming bills in 30 days"
                        } else {
                            "$count ${if (count == 1) "transaction" else "transactions"}, ${MoneyMath.formatCents(netCents, currency)}"
                        }
                        Text(desc, style = PocketlyTextStyles.label, color = colors.inkMuted)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Open scheduled", tint = colors.inkMuted)
                }
            }
        }

        // Active filter badge if category filtered
        if (activeFilterCat != null) {
            item(key = "active_filter_banner") {
                val catName = state.categoriesById[activeFilterCat]?.name ?: "Category"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.mintSoft)
                        .clickable {
                            if (state.homeMode == 1) onSelectOverviewCategory(null)
                            else onToggleHomeCategory(null)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtered by $catName",
                        style = PocketlyTextStyles.label,
                        color = colors.mint
                    )
                    Text(
                        text = "Clear",
                        style = PocketlyTextStyles.captionTabular,
                        color = colors.mint
                    )
                }
            }
        }

        // Day-grouped transaction list
        if (groupedByDay.isEmpty()) {
            item(key = "month_no_tx") {
                PocketlyEmptyState(
                    title = "No transactions in ${selectedMonth.shortLabel()}",
                    subtitle = "Tap + to add one or switch to another month.",
                    primaryLabel = "Add transaction",
                    onPrimaryClick = { onOpenAddTransaction(activeFilterCat) },
                    showMascot = false
                )
            }
        } else {
            groupedByDay.forEach { (dayKey, dayTxs) ->
                val dayNet = dayTxs.sumOf { tx ->
                    when (tx.kind) {
                        TransactionKind.EXPENSE -> -tx.amountCents
                        TransactionKind.INCOME -> tx.amountCents
                        TransactionKind.TRANSFER -> 0L
                    }
                }
                item(key = "day_header_$dayKey") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateUtils.formatDayHeader(dayKey, today),
                            style = PocketlyTextStyles.label,
                            color = colors.inkMuted
                        )
                        if (dayNet != 0L) {
                            Text(
                                text = MoneyMath.formatCents(dayNet, currency, showPlus = dayNet > 0L),
                                style = PocketlyTextStyles.labelTabular,
                                color = colors.inkMuted
                            )
                        }
                    }
                }
                item(key = "day_card_$dayKey") {
                    PocketlyCard(
                        cornerRadius = 24.dp,
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        dayTxs.forEachIndexed { idx, tx ->
                            SwipeableTransactionRow(
                                tx = tx,
                                state = state,
                                onEdit = { onEditTransaction(tx) },
                                onDelete = { onDeleteTransaction(tx) }
                            )
                            if (idx < dayTxs.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 72.dp, end = 16.dp)
                                        .height(1.dp)
                                        .background(colors.line)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewContent(
    state: PocketlyUiState,
    thisMonth: com.example.domain.MonthKey,
    selectedMonth: com.example.domain.MonthKey,
    monthCashFlow: com.example.domain.CashFlowSummary,
    prevMonthExpenseCents: Long,
    expenseSlices: List<com.example.domain.CategorySpendSlice>,
    nineMonthBars: List<com.example.domain.CashFlowSummary>,
    onSelectMonth: (com.example.domain.MonthKey) -> Unit,
    onSelectCategory: (String?) -> Unit
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency

    val tabMonths = remember(thisMonth) {
        listOf(thisMonth.plusMonths(-2), thisMonth.plusMonths(-1), thisMonth)
    }
    val selectedTabIdx = tabMonths.indexOf(selectedMonth).let { if (it < 0) 2 else it }

    Column(modifier = Modifier.fillMaxWidth()) {
        SegmentedTabs(
            options = listOf(
                tabMonths[0].shortLabel(),
                tabMonths[1].shortLabel(),
                "This month"
            ),
            selectedIndex = selectedTabIdx,
            onSelect = { idx -> onSelectMonth(tabMonths[idx]) },
            tagPrefix = "overview_period"
        )

        Spacer(Modifier.height(16.dp))

        // Expenses Header + delta vs previous period
        Text("Expenses", style = PocketlyTextStyles.rowTitle, color = colors.ink)
        Spacer(Modifier.height(2.dp))
        AnimatedCurrencyText(
            targetCents = -monthCashFlow.expenseCents,
            currencyCode = currency,
            style = PocketlyTextStyles.heroNumber,
            color = colors.ink
        )
        Spacer(Modifier.height(4.dp))
        val deltaPct = MoneyMath.percentDelta(monthCashFlow.expenseCents, prevMonthExpenseCents)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (deltaPct == null) {
                Text(
                    text = if (monthCashFlow.expenseCents == 0L) "No spending yet" else "New vs previous period",
                    style = PocketlyTextStyles.caption,
                    color = colors.inkMuted
                )
            } else {
                val lowerSpendIsGood = deltaPct <= 0.0
                val tint = if (lowerSpendIsGood) colors.mint else colors.coral
                Icon(
                    imageVector = if (deltaPct >= 0.0) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${MoneyMath.formatPercent(abs(deltaPct), decimals = 0)} vs previous period",
                    style = PocketlyTextStyles.captionTabular,
                    color = tint
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        ExpenseDonutChart(
            slices = expenseSlices,
            currencyCode = currency,
            selectedCategoryId = state.selectedOverviewCategoryId,
            onSelectCategory = onSelectCategory
        )

        Spacer(Modifier.height(18.dp))

        // Swipeable extra overview cards: Cash flow mini-bars, Top merchants, Weekly spend
        val pagerState = rememberPagerState(pageCount = { 3 })
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface2)
                    .padding(16.dp)
            ) {
                when (page) {
                    0 -> {
                        Text("Cash flow", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = MoneyMath.formatCents(monthCashFlow.netCents, currency),
                            style = PocketlyTextStyles.mediumNumber,
                            color = colors.ink
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MoneyPillChip(monthCashFlow.incomeCents, currency, positive = true, dropWholeZeros = true)
                            MoneyPillChip(monthCashFlow.expenseCents, currency, positive = false, dropWholeZeros = true)
                        }
                        Spacer(Modifier.height(12.dp))
                        GroupedCashFlowBarChart(
                            months = nineMonthBars.takeLast(6),
                            currencyCode = currency,
                            height = 112.dp
                        )
                    }
                    1 -> {
                        Text("Top merchants", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                        Spacer(Modifier.height(10.dp))
                        val topMerchants = remember(state.transactions, selectedMonth) {
                            state.transactions
                                .filter { it.kind == TransactionKind.EXPENSE && DateUtils.isInMonth(it.dayKey, selectedMonth) }
                                .groupBy { it.merchant ?: state.categoriesById[it.categoryId]?.name ?: "Purchase" }
                                .map { (name, list) -> name to list.sumOf { it.amountCents } }
                                .sortedByDescending { it.second }
                                .take(4)
                        }
                        if (topMerchants.isEmpty()) {
                            Text("No merchants logged this month.", style = PocketlyTextStyles.label, color = colors.inkMuted)
                        } else {
                            topMerchants.forEach { (name, cents) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, style = PocketlyTextStyles.label, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(MoneyMath.formatCents(-cents, currency), style = PocketlyTextStyles.labelTabular, color = colors.coral)
                                }
                            }
                        }
                    }
                    else -> {
                        Text("Weekly spend vs income", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                        Spacer(Modifier.height(10.dp))
                        val weekly = remember(state.transactions, selectedMonth) {
                            (1..4).map { week ->
                                val startDay = (week - 1) * 7 + 1
                                val endDay = if (week == 4) 31 else week * 7
                                val weekTxs = state.transactions.filter {
                                    DateUtils.isInMonth(it.dayKey, selectedMonth) &&
                                        (DateUtils.parseDayKey(it.dayKey)?.day ?: 0) in startDay..endDay
                                }
                                val inc = weekTxs.filter { it.kind == TransactionKind.INCOME }.sumOf { it.amountCents }
                                val exp = weekTxs.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.amountCents }
                                Triple("Week $week", inc, exp)
                            }
                        }
                        weekly.forEach { (label, inc, exp) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, style = PocketlyTextStyles.label, color = colors.inkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(MoneyMath.formatCents(inc, currency, showPlus = true, dropWholeZeros = true), style = PocketlyTextStyles.captionTabular, color = colors.mint)
                                    Text(MoneyMath.formatCents(-exp, currency, dropWholeZeros = true), style = PocketlyTextStyles.captionTabular, color = colors.coral)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { idx ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (pagerState.currentPage == idx) 16.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == idx) colors.mint else colors.line)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionRow(
    tx: Transaction,
    state: PocketlyUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val dir = dismissState.dismissDirection
            val bg = when (dir) {
                SwipeToDismissBoxValue.EndToStart -> colors.coral
                SwipeToDismissBoxValue.StartToEnd -> colors.mint
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (dir == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.White)
                } else if (dir == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = Color.White)
                }
            }
        },
        content = {
            TransactionRowContent(tx = tx, state = state, onClick = onEdit)
        }
    )
}

@Composable
fun TransactionRowContent(
    tx: Transaction,
    state: PocketlyUiState,
    onClick: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val category = tx.categoryId?.let { state.categoriesById[it] }
    val account = state.accountsById[tx.accountId]
    val toAccount = tx.toAccountId?.let { state.accountsById[it] }

    val title = when (tx.kind) {
        TransactionKind.TRANSFER -> "Transfer"
        else -> category?.name ?: tx.kind.label
    }
    val subtitle = when (tx.kind) {
        TransactionKind.TRANSFER -> "${account?.name ?: "Account"} to ${toAccount?.name ?: "Account"}"
        else -> listOfNotNull(account?.name, tx.merchant ?: tx.note).joinToString(", ")
    }
    val amountText = when (tx.kind) {
        TransactionKind.EXPENSE -> MoneyMath.formatCents(-tx.amountCents, currency)
        TransactionKind.INCOME -> MoneyMath.formatCents(tx.amountCents, currency, showPlus = true)
        TransactionKind.TRANSFER -> MoneyMath.formatCents(tx.amountCents, currency)
    }
    val amountColor = when (tx.kind) {
        TransactionKind.EXPENSE -> colors.coral
        TransactionKind.INCOME -> colors.mint
        TransactionKind.TRANSFER -> colors.ink
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("tx_row_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconTile(
            iconKey = if (tx.kind == TransactionKind.TRANSFER) "banknote" else (category?.icon ?: "dots"),
            colorKey = if (tx.kind == TransactionKind.TRANSFER) "slate" else (category?.color ?: "slate"),
            size = 44.dp,
            iconSize = 21.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PocketlyTextStyles.rowTitle,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = PocketlyTextStyles.caption,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = amountText,
            style = PocketlyTextStyles.rowAmount,
            color = amountColor,
            maxLines = 1
        )
    }
}

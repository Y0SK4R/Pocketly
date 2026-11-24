package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryKind
import com.example.domain.DateUtils
import com.example.domain.FinanceEngine
import com.example.domain.MoneyMath
import com.example.domain.MonthKey
import com.example.ui.components.BudgetPaceLineChart
import com.example.ui.components.CategoryIconTile
import com.example.ui.components.HeroAmountDisplay
import com.example.ui.components.LiquidBudgetGauge
import com.example.ui.components.MascotMood
import com.example.ui.components.MintRippleButton
import com.example.ui.components.PocketlyBottomSheet
import com.example.ui.components.PocketlyCard
import com.example.ui.components.PocketlyDeleteConfirmSheet
import com.example.ui.components.PocketlyMascot
import com.example.ui.components.SegmentedTabs
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import com.example.ui.viewmodel.PocketlyUiState

private data class BudgetEditTarget(
    val categoryId: String,
    val label: String,
    val currentAmountCents: Long
)

@Composable
fun BudgetsScreen(
    state: PocketlyUiState,
    onSelectMonth: (MonthKey) -> Unit,
    onSaveBudget: (categoryId: String, amountCents: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val today = remember { DateUtils.today() }
    val thisMonth = remember(today) { today.toMonthKey() }
    val selectedMonth = state.selectedMonth

    val tabMonths = remember(thisMonth) {
        listOf(thisMonth.plusMonths(-2), thisMonth.plusMonths(-1), thisMonth)
    }
    val selectedTabIdx = tabMonths.indexOf(selectedMonth).let { if (it < 0) 2 else it }

    val budgetsByCat = remember(state.budgets) { state.budgets.associateBy { it.categoryId } }
    val allBudget = budgetsByCat["all"]
    val allStatus = remember(allBudget, state.transactions, selectedMonth, today) {
        allBudget?.let { FinanceEngine.computeBudgetStatus(it, state.transactions, selectedMonth, today) }
    }
    val paceSeries = remember(allBudget, state.transactions, selectedMonth, today) {
        if (allBudget != null && allBudget.amountCents > 0L) {
            FinanceEngine.computeBudgetPaceSeries(allBudget.amountCents, state.transactions, selectedMonth, today)
        } else emptyList()
    }

    val expenseCats = remember(state.activeCategories) {
        state.activeCategories.filter { it.kind == CategoryKind.EXPENSE }
    }
    val budgetedCategories = remember(expenseCats, budgetsByCat) {
        expenseCats.filter { budgetsByCat.containsKey(it.id) }
    }
    val unbudgetedCategories = remember(expenseCats, budgetsByCat) {
        expenseCats.filter { !budgetsByCat.containsKey(it.id) }
    }

    var editTarget by remember { mutableStateOf<BudgetEditTarget?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "budgets_title") {
            Text(
                text = "Budgets",
                style = PocketlyTextStyles.screenTitle,
                color = colors.ink,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item(key = "budgets_period_tabs") {
            SegmentedTabs(
                options = listOf(tabMonths[0].shortLabel(), tabMonths[1].shortLabel(), "This month"),
                selectedIndex = selectedTabIdx,
                onSelect = { onSelectMonth(tabMonths[it]) },
                tagPrefix = "budgets_period"
            )
        }

        item(key = "budgets_liquid_gauge") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            editTarget = BudgetEditTarget(
                                categoryId = "all",
                                label = "All spending",
                                currentAmountCents = allBudget?.amountCents ?: 0L
                            )
                        }
                        .testTag("budgets_gauge_tap")
                ) {
                    LiquidBudgetGauge(
                        status = allStatus,
                        currencyCode = currency
                    )
                }

                if (allStatus != null && !allStatus.isOverBudget && allStatus.spentCents > 0L && allStatus.spentCents <= allStatus.idealPaceCents) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.mintSoft)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        PocketlyMascot(mood = MascotMood.PROUD, modifier = Modifier.size(26.dp))
                        Text(
                            text = "Under your ideal pace for ${selectedMonth.shortMonthOnly()}",
                            style = PocketlyTextStyles.caption,
                            color = colors.mint
                        )
                    }
                }
            }
        }

        if (allBudget != null && paceSeries.isNotEmpty()) {
            item(key = "budgets_pace_card") {
                PocketlyCard(contentPadding = PaddingValues(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monthly pace", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                        val idealText = MoneyMath.formatCents(allStatus?.idealPaceCents ?: 0L, currency, dropWholeZeros = true)
                        Text("Ideal: $idealText", style = PocketlyTextStyles.captionTabular, color = colors.inkMuted)
                    }
                    Spacer(Modifier.height(10.dp))
                    BudgetPaceLineChart(
                        pacePoints = paceSeries,
                        budgetCents = allBudget.amountCents,
                        isOverBudget = allStatus?.isOverBudget == true,
                        currencyCode = currency
                    )
                }
            }
        }

        item(key = "budgets_active_categories") {
            PocketlyCard {
                Text("Categories · Spent / Budget", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                Spacer(Modifier.height(12.dp))

                if (budgetedCategories.isEmpty()) {
                    Text(
                        text = "No category budgets yet. Set one below to track your pace.",
                        style = PocketlyTextStyles.label,
                        color = colors.inkMuted
                    )
                } else {
                    budgetedCategories.forEachIndexed { idx, cat ->
                        val bud = budgetsByCat[cat.id]!!
                        val st = FinanceEngine.computeBudgetStatus(bud, state.transactions, selectedMonth, today)
                        val barColor = if (st.isOverBudget) colors.coral else colors.mint
                        val spentStr = MoneyMath.formatCents(st.spentCents, currency, dropWholeZeros = true)
                        val budStr = MoneyMath.formatCents(st.budgetCents, currency, dropWholeZeros = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    editTarget = BudgetEditTarget(cat.id, cat.name, bud.amountCents)
                                }
                                .padding(vertical = 8.dp)
                                .testTag("budget_row_${cat.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconTile(cat.icon, cat.color, size = 42.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.name,
                                        style = PocketlyTextStyles.rowTitle,
                                        color = colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "$spentStr of $budStr",
                                        style = PocketlyTextStyles.captionTabular,
                                        color = if (st.isOverBudget) colors.coral else colors.inkMuted
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface2)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = st.fillRatio.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(barColor)
                                    )
                                }
                            }
                        }
                        if (idx < budgetedCategories.lastIndex) {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        item(key = "budgets_set_card") {
            PocketlyCard {
                Text("Set a budget", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                Spacer(Modifier.height(10.dp))

                SetBudgetRow(
                    iconKey = "wallet",
                    colorKey = "mint",
                    name = "All spending",
                    actionLabel = if (allBudget != null) MoneyMath.formatCents(allBudget.amountCents, currency, dropWholeZeros = true) else "Set",
                    onClick = {
                        editTarget = BudgetEditTarget("all", "All spending", allBudget?.amountCents ?: 0L)
                    },
                    tag = "set_budget_all"
                )

                unbudgetedCategories.forEach { cat ->
                    Box(Modifier.fillMaxWidth().padding(start = 52.dp).height(1.dp).background(colors.line))
                    SetBudgetRow(
                        iconKey = cat.icon,
                        colorKey = cat.color,
                        name = cat.name,
                        actionLabel = "Set",
                        onClick = { editTarget = BudgetEditTarget(cat.id, cat.name, 0L) },
                        tag = "set_budget_${cat.id}"
                    )
                }
            }
        }
    }

    editTarget?.let { target ->
        BudgetAmountKeypadSheet(
            label = target.label,
            initialCents = target.currentAmountCents,
            currencyCode = currency,
            onDismiss = { editTarget = null },
            onSave = { newCents ->
                onSaveBudget(target.categoryId, newCents)
                editTarget = null
            }
        )
    }
}

@Composable
private fun SetBudgetRow(
    iconKey: String,
    colorKey: String,
    name: String,
    actionLabel: String,
    onClick: () -> Unit,
    tag: String
) {
    val colors = PocketlyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconTile(iconKey, colorKey, size = 38.dp, iconSize = 18.dp, circular = true)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = PocketlyTextStyles.rowTitle,
            color = colors.ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.mintSoft)
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            Text(
                text = actionLabel,
                style = PocketlyTextStyles.chipSelected.copy(fontFeatureSettings = "tnum"),
                color = colors.mint
            )
        }
    }
}

@Composable
private fun BudgetAmountKeypadSheet(
    label: String,
    initialCents: Long,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val colors = PocketlyTheme.colors
    var amountStr by remember(initialCents) { mutableStateOf(MoneyMath.centsToKeypadString(initialCents)) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val amountCents = MoneyMath.parseKeypadInputToCents(amountStr)

    PocketlyBottomSheet(
        title = label,
        onDismiss = onDismiss,
        leadingAction = if (initialCents > 0L) {
            {
                IconButton(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.testTag("budget_remove_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove budget",
                        tint = colors.coral
                    )
                }
            }
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Monthly budget limit", style = PocketlyTextStyles.caption, color = colors.inkMuted)
            Spacer(Modifier.height(4.dp))
            HeroAmountDisplay(
                amountStr = amountStr,
                currencyCode = currencyCode,
                isActive = true,
                onClick = {}
            )
            Spacer(Modifier.height(12.dp))
            NumericAmountKeypad(
                onDigit = { amountStr = appendKeypadDigit(amountStr, it) },
                onDecimal = { if (!amountStr.contains(".")) amountStr = "$amountStr." },
                onBackspace = { amountStr = if (amountStr.length <= 1) "0" else amountStr.dropLast(1) }
            )
            Spacer(Modifier.height(16.dp))
            MintRippleButton(
                text = "Save budget",
                enabled = amountCents > 0L,
                onClick = { onSave(amountCents) },
                testTagName = "budget_save_button"
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showRemoveConfirm) {
        PocketlyDeleteConfirmSheet(
            title = "Remove budget",
            message = "Remove the monthly limit for \"$label\"? Your spending history stays untouched.",
            confirmLabel = "Remove budget",
            onConfirm = { onSave(0L) },
            onDismiss = { showRemoveConfirm = false },
            buttonTestTag = "confirm_remove_budget"
        )
    }
}

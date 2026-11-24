package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.Account
import com.example.domain.AccountKind
import com.example.domain.DateUtils
import com.example.domain.FinanceEngine
import com.example.domain.MoneyMath
import com.example.domain.SeriesPoint
import com.example.domain.Transaction
import com.example.ui.components.AnimatedCurrencyText
import com.example.ui.components.CategoryIconTile
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.HeroAmountDisplay
import com.example.ui.components.LocalPocketlyFeedback
import com.example.ui.components.MintRippleButton
import com.example.ui.components.PocketlyBottomSheet
import com.example.ui.components.PocketlyCard
import com.example.ui.components.PocketlyDeleteConfirmSheet
import com.example.ui.components.PocketlyEmptyState
import com.example.ui.components.PocketlyIcons
import com.example.ui.components.SheetColorSwatches
import com.example.ui.components.SheetGroupedCard
import com.example.ui.components.SheetIconSwatches
import com.example.ui.components.SheetRowDivider
import com.example.ui.components.SheetTextRow
import com.example.ui.components.SmoothAreaLineChart
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.PocketlyPalette
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import com.example.ui.viewmodel.PocketlyUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    state: PocketlyUiState,
    onOpenAccountDetail: (String) -> Unit,
    onOpenInvestTab: () -> Unit,
    onSaveAccount: (Account?, String, AccountKind, String, String, Long) -> Unit,
    onMoveAccount: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val today = remember { DateUtils.today() }
    val thisMonth = remember(today) { today.toMonthKey() }

    var searchQuery by remember { mutableStateOf("") }
    var filterKind by remember { mutableStateOf<AccountKind?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var showNewAccountSheet by remember { mutableStateOf(false) }

    val totalWealthCents = remember(state.accounts, state.transactions, state.holdings, today) {
        FinanceEngine.computeTotalWealthCents(state.accounts, state.transactions, state.holdings, today)
    }
    val lastMonthWealthCents = remember(state.accounts, state.transactions, state.holdings, today) {
        FinanceEngine.computeTotalWealthCents(state.accounts, state.transactions, state.holdings, today.plusDays(-30))
    }
    val wealthMonthlyDelta = totalWealthCents - lastMonthWealthCents

    val wealthSeries = remember(state.accounts, state.transactions, state.holdings, today) {
        (11 downTo 0).map { step ->
            val d = today.plusDays(-step * 8)
            SeriesPoint(
                dayKey = d.toDayKey(),
                valueCents = FinanceEngine.computeTotalWealthCents(
                    state.accounts, state.transactions, state.holdings, d, includeArchivedInHistory = true
                )
            )
        }
    }

    val totalInvestmentsCents = remember(state.activeHoldings, today) {
        state.activeHoldings.sumOf { FinanceEngine.computeHoldingValuation(it, today).currentValueCents }
    }

    val filteredAccounts = remember(state.activeAccounts, searchQuery, filterKind) {
        state.activeAccounts.filter { acc ->
            val matchesName = searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true)
            val matchesKind = filterKind == null || acc.kind == filterKind
            matchesName && matchesKind
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "accounts_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.mint.copy(alpha = if (colors.isDark) 0.14f else 0.10f),
                                colors.bg
                            )
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total wealth", style = PocketlyTextStyles.label, color = colors.inkMuted)
                        TextButton(
                            onClick = { showNewAccountSheet = true },
                            modifier = Modifier.testTag("accounts_new_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.mint, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New account", style = PocketlyTextStyles.label, color = colors.mint)
                        }
                    }
                    AnimatedCurrencyText(
                        targetCents = totalWealthCents,
                        currencyCode = currency,
                        style = PocketlyTextStyles.heroNumber,
                        color = colors.ink
                    )
                    Spacer(Modifier.height(4.dp))
                    val deltaPositive = wealthMonthlyDelta >= 0L
                    val deltaColor = if (deltaPositive) colors.mint else colors.coral
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = if (deltaPositive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = deltaColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${MoneyMath.formatCents(wealthMonthlyDelta, currency, showPlus = true)} last month",
                            style = PocketlyTextStyles.captionTabular,
                            color = deltaColor
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    SmoothAreaLineChart(
                        points = wealthSeries,
                        currencyCode = currency,
                        height = 132.dp
                    )
                }
            }
        }

        item(key = "accounts_list_card") {
            PocketlyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accounts", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                    if (reorderMode) {
                        TextButton(onClick = { reorderMode = false }) {
                            Text("Done", style = PocketlyTextStyles.label, color = colors.mint)
                        }
                    } else if (state.activeAccounts.size > 1) {
                        Text(
                            text = "Hold to reorder",
                            style = PocketlyTextStyles.caption,
                            color = colors.inkMuted
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (filteredAccounts.isEmpty()) {
                    Text(
                        text = "No accounts match your filter.",
                        style = PocketlyTextStyles.label,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    filteredAccounts.forEachIndexed { idx, acc ->
                        val balance = FinanceEngine.computeAccountBalance(acc, state.transactions)
                        val monthChange = FinanceEngine.computeAccountMonthChange(acc, state.transactions, thisMonth)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { if (!reorderMode) onOpenAccountDetail(acc.id) },
                                    onLongClick = { reorderMode = true }
                                )
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("account_row_${acc.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconTile(acc.icon, acc.color, size = 44.dp, iconSize = 21.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.name,
                                    style = PocketlyTextStyles.rowTitle,
                                    color = colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(acc.kind.label, style = PocketlyTextStyles.caption, color = colors.inkMuted)
                            }
                            if (reorderMode) {
                                Row {
                                    IconButton(onClick = { onMoveAccount(acc.id, -1) }) {
                                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up", tint = colors.ink)
                                    }
                                    IconButton(onClick = { onMoveAccount(acc.id, 1) }) {
                                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down", tint = colors.ink)
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = MoneyMath.formatCents(balance, currency),
                                        style = PocketlyTextStyles.rowAmount,
                                        color = colors.ink
                                    )
                                    if (monthChange != 0L) {
                                        val pos = monthChange > 0L
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (pos) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (pos) colors.mint else colors.coral,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = MoneyMath.formatCents(kotlin.math.abs(monthChange), currency, dropWholeZeros = true),
                                                style = PocketlyTextStyles.captionTabular,
                                                color = if (pos) colors.mint else colors.coral
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (idx < filteredAccounts.lastIndex) {
                            Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(colors.line))
                        }
                    }
                }

                if (state.activeHoldings.isNotEmpty() || totalInvestmentsCents > 0L) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenInvestTab)
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("accounts_investments_line"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconTile("trending_up", "mint", size = 44.dp, iconSize = 21.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Investments", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                            Text("${state.activeHoldings.size} active holdings", style = PocketlyTextStyles.caption, color = colors.inkMuted)
                        }
                        Text(
                            text = MoneyMath.formatCents(totalInvestmentsCents, currency),
                            style = PocketlyTextStyles.rowAmount,
                            color = colors.mint
                        )
                    }
                }
            }
        }

        item(key = "accounts_search_filter") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .then(if (colors.isDark) Modifier.border(1.dp, colors.line, CircleShape) else Modifier)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search accounts", style = PocketlyTextStyles.inputValue, color = colors.inkMuted)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = PocketlyTextStyles.inputValue.copy(color = colors.ink),
                            singleLine = true,
                            cursorBrush = SolidColor(colors.mint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("accounts_search_input")
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (filterKind != null) colors.mintSoft else colors.surface)
                        .then(if (colors.isDark) Modifier.border(1.dp, colors.line, CircleShape) else Modifier)
                        .clickable { showFilterMenu = !showFilterMenu }
                        .testTag("accounts_filter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filter accounts by type",
                        tint = if (filterKind != null) colors.mint else colors.ink
                    )
                }
            }
            if (showFilterMenu) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChipPill("All", filterKind == null) { filterKind = null } }
                    items(AccountKind.entries) { k ->
                        FilterChipPill(k.label, filterKind == k) { filterKind = if (filterKind == k) null else k }
                    }
                }
            }
        }
    }

    if (showNewAccountSheet) {
        NewAccountSheet(
            currencyCode = currency,
            onDismiss = { showNewAccountSheet = false },
            onSave = { name, kind, icon, color, openingCents ->
                onSaveAccount(null, name, kind, icon, color, openingCents)
                showNewAccountSheet = false
            }
        )
    }
}

@Composable
private fun FilterChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = PocketlyTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) colors.mintSoft else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = if (selected) PocketlyTextStyles.chipSelected else PocketlyTextStyles.chip,
            color = if (selected) colors.mint else colors.inkMuted
        )
    }
}

/**
 * Redesigned New Account bottom sheet (replaces AlertDialog + OutlinedTextField).
 */
@Composable
fun NewAccountSheet(
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (String, AccountKind, String, String, Long) -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    var name by remember { mutableStateOf("") }
    var nameTouched by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(AccountKind.WALLET) }
    var colorKey by remember { mutableStateOf("mint") }
    var iconKey by remember { mutableStateOf("wallet") }
    var openingStr by remember { mutableStateOf("0") }
    var isNegativeBalance by remember { mutableStateOf(false) }
    var keypadExpanded by remember { mutableStateOf(false) }

    val colorChoices = remember { listOf("mint", "sky", "pink", "purple", "orange", "lime", "teal", "slate") }
    val iconChoices = remember { listOf("wallet", "landmark", "credit_card", "piggy_bank", "cash", "banknote") }

    val nameError = if (nameTouched && name.trim().isEmpty()) "Enter a name" else null
    val canSave = name.trim().isNotEmpty()

    PocketlyBottomSheet(title = "New account", onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Opening balance hero (56sp) + tap for custom keypad
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                HeroAmountDisplay(
                    amountStr = openingStr,
                    currencyCode = currencyCode,
                    isActive = keypadExpanded,
                    isNegative = isNegativeBalance,
                    onClick = { keypadExpanded = !keypadExpanded }
                )
            }

            AnimatedVisibility(
                visible = keypadExpanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    NumericAmountKeypad(
                        onDigit = { openingStr = appendKeypadDigit(openingStr, it) },
                        onDecimal = { if (!openingStr.contains(".")) openingStr = "$openingStr." },
                        onBackspace = { openingStr = if (openingStr.length <= 1) "0" else openingStr.dropLast(1) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Account type scrolling chips (44dp tall, 12dp gaps, 16dp side padding)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(AccountKind.entries) { k ->
                    val sel = kind == k
                    val defaultIcon = when (k) {
                        AccountKind.WALLET -> "wallet"
                        AccountKind.CHECKING -> "landmark"
                        AccountKind.CREDIT -> "credit_card"
                        AccountKind.SAVINGS -> "piggy_bank"
                        AccountKind.CASH -> "cash"
                    }
                    val defaultCol = when (k) {
                        AccountKind.WALLET -> "mint"
                        AccountKind.CHECKING -> "sky"
                        AccountKind.CREDIT -> "pink"
                        AccountKind.SAVINGS -> "purple"
                        AccountKind.CASH -> "lime"
                    }
                    val accent = PocketlyPalette.colorForKey(defaultCol)
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (sel) accent else colors.surface2)
                            .clickable {
                                feedback.tap()
                                kind = k
                                iconKey = defaultIcon
                                colorKey = defaultCol
                                isNegativeBalance = k == AccountKind.CREDIT
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PocketlyIcons.forKey(defaultIcon),
                            contentDescription = null,
                            tint = if (sel) Color.White else accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = k.label,
                            style = if (sel) PocketlyTextStyles.chipSelected else PocketlyTextStyles.chip,
                            color = if (sel) Color.White else colors.ink
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Grouped card (surface-2, 18dp radius, 56dp rows)
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                SheetGroupedCard {
                    SheetTextRow(
                        icon = Icons.Rounded.Badge,
                        label = "Name",
                        value = name,
                        onValueChange = {
                            name = it
                            nameTouched = true
                            keypadExpanded = false
                        },
                        placeholder = kind.label,
                        error = nameError,
                        maxLength = 32,
                        testTagName = "new_account_name_input"
                    )
                    if (kind == AccountKind.CREDIT) {
                        SheetRowDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Balance is owed (negative)", style = PocketlyTextStyles.label, color = colors.inkMuted)
                            Switch(
                                checked = isNegativeBalance,
                                onCheckedChange = { isNegativeBalance = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = colors.coral, checkedThumbColor = Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SheetColorSwatches(
                selectedColorKey = colorKey,
                onSelectColor = { colorKey = it },
                colorKeys = colorChoices
            )
            Spacer(Modifier.height(12.dp))
            SheetIconSwatches(
                iconKeys = iconChoices,
                selectedIconKey = iconKey,
                accentColorKey = colorKey,
                onSelectIcon = { iconKey = it }
            )

            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                MintRippleButton(
                    text = "Add account",
                    enabled = canSave,
                    onClick = {
                        nameTouched = true
                        if (canSave) {
                            val absCents = MoneyMath.parseKeypadInputToCents(openingStr)
                            val signed = if (isNegativeBalance) -absCents else absCents
                            onSave(name.trim(), kind, iconKey, colorKey, signed)
                        }
                    },
                    testTagName = "confirm_add_account"
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AccountDetailScreen(
    accountId: String,
    state: PocketlyUiState,
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAddTransactionForAccount: (String) -> Unit,
    onArchiveAccount: (Account) -> Unit
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val account = state.accountsById[accountId]
    val today = remember { DateUtils.today() }
    val thisMonth = remember(today) { today.toMonthKey() }
    var selectedDonutCat by remember { mutableStateOf<String?>(null) }

    if (account == null) {
        Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            SubScreenTopBar(title = "Account", onBack = onBack)
            PocketlyEmptyState(title = "Account not found", showMascot = false, modifier = Modifier.padding(16.dp))
        }
        return
    }

    val balance = remember(account, state.transactions) {
        FinanceEngine.computeAccountBalance(account, state.transactions)
    }
    val series = remember(account, state.transactions, today) {
        (11 downTo 0).map { step ->
            val d = today.plusDays(-step * 8)
            SeriesPoint(
                dayKey = d.toDayKey(),
                valueCents = FinanceEngine.computeAccountBalance(account, state.transactions, upToDayKeyInclusive = d.toDayKey())
            )
        }
    }
    val expenseSlices = remember(state.transactions, state.categories, state.budgets, thisMonth, accountId) {
        FinanceEngine.computeExpenseBreakdown(state.transactions, state.categories, state.budgets, thisMonth, accountIdFilter = accountId)
    }
    val accountTxs = remember(state.transactions, accountId, selectedDonutCat) {
        state.transactions
            .filter { it.accountId == accountId || it.toAccountId == accountId }
            .filter { selectedDonutCat == null || it.categoryId == selectedDonutCat }
    }

    var showArchiveConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        SubScreenTopBar(
            title = account.name,
            onBack = onBack,
            trailingAction = {
                if (state.activeAccounts.size > 1) {
                    IconButton(onClick = { showArchiveConfirm = true }) {
                        Icon(Icons.Rounded.Archive, contentDescription = "Archive account", tint = colors.inkMuted)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PocketlyCard {
                    Text(account.kind.label, style = PocketlyTextStyles.label, color = colors.inkMuted)
                    Spacer(Modifier.height(4.dp))
                    AnimatedCurrencyText(
                        targetCents = balance,
                        currencyCode = currency,
                        style = PocketlyTextStyles.heroNumber,
                        color = colors.ink
                    )
                    Spacer(Modifier.height(14.dp))
                    SmoothAreaLineChart(points = series, currencyCode = currency, height = 132.dp)
                }
            }

            if (expenseSlices.isNotEmpty()) {
                item {
                    PocketlyCard {
                        Text("Expenses this month", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                        Spacer(Modifier.height(12.dp))
                        ExpenseDonutChart(
                            slices = expenseSlices,
                            currencyCode = currency,
                            selectedCategoryId = selectedDonutCat,
                            onSelectCategory = { selectedDonutCat = it }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transactions", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                    TextButton(onClick = { onAddTransactionForAccount(accountId) }) {
                        Text("Add transaction", style = PocketlyTextStyles.label, color = colors.mint)
                    }
                }
            }

            if (accountTxs.isEmpty()) {
                item {
                    PocketlyEmptyState(
                        title = "No transactions in ${account.name}",
                        primaryLabel = "Add transaction",
                        onPrimaryClick = { onAddTransactionForAccount(accountId) },
                        showMascot = false
                    )
                }
            } else {
                item {
                    PocketlyCard(cornerRadius = 24.dp, contentPadding = PaddingValues(vertical = 4.dp)) {
                        accountTxs.forEachIndexed { idx, tx ->
                            SwipeableTransactionRow(
                                tx = tx,
                                state = state,
                                onEdit = { onEditTransaction(tx) },
                                onDelete = { onDeleteTransaction(tx) }
                            )
                            if (idx < accountTxs.lastIndex) {
                                Box(Modifier.fillMaxWidth().padding(start = 72.dp, end = 16.dp).height(1.dp).background(colors.line))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showArchiveConfirm) {
        PocketlyDeleteConfirmSheet(
            title = "Archive account",
            message = "Archive \"${account.name}\"? Existing transactions stay in your history, and it will be hidden from active accounts.",
            confirmLabel = "Archive account",
            onConfirm = { onArchiveAccount(account) },
            onDismiss = { showArchiveConfirm = false },
            buttonTestTag = "confirm_archive_account"
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ActivityItem
import com.example.domain.ActivityKind
import com.example.domain.Category
import com.example.domain.CategoryKind
import com.example.domain.DateUtils
import com.example.domain.FinanceEngine
import com.example.domain.MoneyMath
import com.example.domain.RepeatFrequency
import com.example.domain.ScheduledItem
import com.example.domain.SupportedCurrencies
import com.example.domain.Transaction
import com.example.domain.TransactionKind
import com.example.ui.components.CategoryIconTile
import com.example.ui.components.HeroAmountDisplay
import com.example.ui.components.InfoBadgePill
import com.example.ui.components.LocalPocketlyFeedback
import com.example.ui.components.MascotMood
import com.example.ui.components.MintRippleButton
import com.example.ui.components.PocketlyBottomSheet
import com.example.ui.components.PocketlyCalendarSheet
import com.example.ui.components.PocketlyCard
import com.example.ui.components.PocketlyChoiceSheet
import com.example.ui.components.PocketlyDeleteConfirmSheet
import com.example.ui.components.PocketlyEmptyState
import com.example.ui.components.PocketlyIcons
import com.example.ui.components.PocketlyMascot
import com.example.ui.components.PocketlySpringSegmentedControl
import com.example.ui.components.SheetChoiceItem
import com.example.ui.components.SheetColorSwatches
import com.example.ui.components.SheetGroupedCard
import com.example.ui.components.SheetIconSwatches
import com.example.ui.components.SheetPickerRow
import com.example.ui.components.SheetRowDivider
import com.example.ui.components.SheetSwitchRow
import com.example.ui.components.SheetTextRow
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.PocketlyPalette
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import com.example.ui.viewmodel.PocketlyUiState

// ─────────────────────────── SCHEDULED SCREEN ───────────────────────────

@Composable
fun ScheduledScreen(
    state: PocketlyUiState,
    onBack: () -> Unit,
    onSaveItem: (ScheduledItem?, String, TransactionKind, Long, String, String?, RepeatFrequency, String, Boolean, Boolean) -> Unit,
    onTogglePaused: (ScheduledItem) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val today = remember { DateUtils.today() }
    val (_, next30Net) = remember(state.scheduledItems, today) {
        FinanceEngine.computeNext30DaysScheduledNetCents(state.scheduledItems, today)
    }

    var editingItem by remember { mutableStateOf<ScheduledItem?>(null) }
    var showForm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Scheduled",
                onBack = onBack,
                trailingAction = {
                    IconButton(
                        onClick = { editingItem = null; showForm = true },
                        modifier = Modifier.testTag("scheduled_add_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add scheduled item", tint = colors.mint)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PocketlyCard(cornerRadius = 20.dp, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Next 30 days · ${MoneyMath.formatCents(next30Net, currency)}",
                                style = PocketlyTextStyles.rowTitle,
                                color = colors.ink
                            )
                        }
                    }
                }

                if (state.scheduledItems.isEmpty()) {
                    item {
                        PocketlyEmptyState(
                            title = "No scheduled items yet",
                            subtitle = "Add recurring rent, bills, or income so Pocketly tracks what's due next.",
                            primaryLabel = "Add scheduled item",
                            onPrimaryClick = { editingItem = null; showForm = true },
                            showMascot = false
                        )
                    }
                } else {
                    item {
                        PocketlyCard(cornerRadius = 24.dp, contentPadding = PaddingValues(vertical = 6.dp)) {
                            state.scheduledItems.forEachIndexed { idx, item ->
                                val cat = item.categoryId?.let { state.categoriesById[it] }
                                val (dueLabel, isOverdue) = DateUtils.formatDueLabel(item.nextDueDayKey, today)
                                val signedCents = if (item.kind == TransactionKind.EXPENSE) -item.amountCents else item.amountCents

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingItem = item; showForm = true }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("scheduled_row_${item.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIconTile(
                                        iconKey = cat?.icon ?: "zap",
                                        colorKey = cat?.color ?: "orange",
                                        size = 44.dp,
                                        iconSize = 20.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = PocketlyTextStyles.rowTitle,
                                            color = if (item.paused) colors.inkMuted else colors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (item.paused) "Paused · ${item.repeat.label}" else item.repeat.label,
                                            style = PocketlyTextStyles.caption,
                                            color = colors.inkMuted
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = DateUtils.formatShortDate(item.nextDueDayKey),
                                            style = PocketlyTextStyles.caption,
                                            color = colors.inkMuted
                                        )
                                        if (!item.paused && dueLabel != null) {
                                            Spacer(Modifier.height(3.dp))
                                            InfoBadgePill(
                                                text = dueLabel,
                                                bgColor = if (isOverdue) colors.coralSoft else colors.amberSoft,
                                                fgColor = if (isOverdue) colors.coral else colors.amber
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = MoneyMath.formatCents(signedCents, currency, showPlus = item.kind == TransactionKind.INCOME),
                                        style = PocketlyTextStyles.rowAmount,
                                        color = if (item.kind == TransactionKind.INCOME) colors.mint else colors.ink
                                    )
                                }
                                if (idx < state.scheduledItems.lastIndex) {
                                    Box(Modifier.fillMaxWidth().padding(start = 72.dp, end = 16.dp).height(1.dp).background(colors.line))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        ScheduledItemSheet(
            existing = editingItem,
            state = state,
            onDismiss = { showForm = false },
            onSave = { name, kind, cents, accId, catId, repeat, dueKey, paused, autoPost ->
                onSaveItem(editingItem, name, kind, cents, accId, catId, repeat, dueKey, paused, autoPost)
                showForm = false
            },
            onDelete = { id ->
                onDeleteItem(id)
                showForm = false
            }
        )
    }
}

@Composable
private fun ScheduledItemSheet(
    existing: ScheduledItem?,
    state: PocketlyUiState,
    onDismiss: () -> Unit,
    onSave: (String, TransactionKind, Long, String, String?, RepeatFrequency, String, Boolean, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var nameTouched by remember { mutableStateOf(false) }
    var amountStr by remember {
        mutableStateOf(if (existing != null) MoneyMath.centsToKeypadString(existing.amountCents) else "0")
    }
    var keypadExpanded by remember { mutableStateOf(existing == null) }
    var kind by remember { mutableStateOf(existing?.kind ?: TransactionKind.EXPENSE) }
    var repeat by remember { mutableStateOf(existing?.repeat ?: RepeatFrequency.MONTHLY) }
    var dueKey by remember { mutableStateOf(existing?.nextDueDayKey ?: DateUtils.today().plusDays(3).toDayKey()) }
    var paused by remember { mutableStateOf(existing?.paused ?: false) }
    var autoPost by remember { mutableStateOf(existing?.autoPost ?: false) }
    var selectedAccountId by remember {
        mutableStateOf(existing?.accountId ?: state.activeAccounts.firstOrNull()?.id ?: "")
    }
    val matchingCategories = remember(state.activeCategories, kind) {
        val target = if (kind == TransactionKind.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        state.activeCategories.filter { it.kind == target }
    }
    var selectedCategoryId by remember(kind) {
        val current = existing?.categoryId?.takeIf { id -> matchingCategories.any { it.id == id } }
            ?: matchingCategories.firstOrNull()?.id
        mutableStateOf(current)
    }

    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDueDateSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val amountCents = MoneyMath.parseKeypadInputToCents(amountStr)
    val nameError = if (nameTouched && name.trim().isEmpty()) "Enter a name" else null
    val canSave = name.trim().isNotEmpty() && amountCents > 0L && selectedAccountId.isNotBlank()

    PocketlyBottomSheet(
        title = if (existing == null) "Add scheduled item" else "Edit scheduled item",
        onDismiss = onDismiss,
        leadingAction = if (existing != null) {
            {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(48.dp).testTag("scheduled_delete_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete scheduled item",
                        tint = colors.coral,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                PocketlySpringSegmentedControl(
                    options = listOf("Expense", "Income"),
                    selectedIndex = if (kind == TransactionKind.EXPENSE) 0 else 1,
                    onSelect = { kind = if (it == 0) TransactionKind.EXPENSE else TransactionKind.INCOME },
                    tagPrefix = "scheduled_kind"
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                HeroAmountDisplay(
                    amountStr = amountStr,
                    currencyCode = state.settings.currency,
                    isActive = keypadExpanded,
                    onClick = { keypadExpanded = !keypadExpanded }
                )
            }

            AnimatedVisibility(visible = keypadExpanded) {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                    NumericAmountKeypad(
                        onDigit = { d -> amountStr = appendKeypadDigit(amountStr, d) },
                        onDecimal = { if (!amountStr.contains(".")) amountStr = "$amountStr." },
                        onBackspace = { amountStr = if (amountStr.length <= 1) "0" else amountStr.dropLast(1) }
                    )
                }
            }

            // Repeat frequency chips (44dp tall, 12dp gaps, 16dp side padding)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(RepeatFrequency.entries) { freq ->
                    val sel = repeat == freq
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (sel) colors.mint else colors.surface2)
                            .clickable {
                                feedback.tap()
                                repeat = freq
                                keypadExpanded = false
                            }
                            .padding(horizontal = 16.dp)
                            .testTag("scheduled_freq_${freq.key}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freq.label.replaceFirstChar { it.uppercase() },
                            style = if (sel) PocketlyTextStyles.chipSelected else PocketlyTextStyles.chip,
                            color = if (sel) Color.White else colors.ink
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Grouped form card
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
                        placeholder = "Rent, phone plan...",
                        error = nameError,
                        maxLength = 36,
                        testTagName = "scheduled_name_input"
                    )
                    SheetRowDivider()
                    SheetPickerRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        label = "Account",
                        value = state.accountsById[selectedAccountId]?.name ?: "Select account",
                        onClick = {
                            keypadExpanded = false
                            showAccountSheet = true
                        },
                        testTagName = "scheduled_account_row"
                    )
                    SheetRowDivider()
                    SheetPickerRow(
                        icon = Icons.Rounded.Category,
                        label = "Category",
                        value = selectedCategoryId?.let { state.categoriesById[it]?.name } ?: "None",
                        onClick = {
                            keypadExpanded = false
                            showCategorySheet = true
                        },
                        testTagName = "scheduled_category_row"
                    )
                    SheetRowDivider()
                    SheetPickerRow(
                        icon = Icons.Rounded.CalendarToday,
                        label = "Next due",
                        value = DateUtils.formatDayHeader(dueKey),
                        onClick = {
                            keypadExpanded = false
                            showDueDateSheet = true
                        },
                        testTagName = "scheduled_due_row"
                    )
                    SheetRowDivider()
                    SheetSwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "Post automatically",
                        subtitle = "Record transaction when due",
                        checked = autoPost,
                        onCheckedChange = { autoPost = it },
                        testTagName = "scheduled_autopost_switch"
                    )
                    if (existing != null) {
                        SheetRowDivider()
                        SheetSwitchRow(
                            icon = Icons.Rounded.PauseCircleOutline,
                            label = "Paused",
                            checked = paused,
                            onCheckedChange = { paused = it },
                            checkedTrackColor = colors.amber,
                            testTagName = "scheduled_paused_switch"
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                MintRippleButton(
                    text = if (existing == null) "Add scheduled item" else "Save changes",
                    enabled = canSave,
                    onClick = {
                        nameTouched = true
                        if (canSave) {
                            onSave(name.trim(), kind, amountCents, selectedAccountId, selectedCategoryId, repeat, dueKey, paused, autoPost)
                        }
                    },
                    testTagName = "scheduled_save_button"
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAccountSheet) {
        PocketlyChoiceSheet(
            title = "Choose account",
            items = state.activeAccounts.map { acc ->
                SheetChoiceItem(acc.id, acc.name, acc.kind.label, iconKey = acc.icon, colorKey = acc.color)
            },
            selectedId = selectedAccountId,
            onSelect = { selectedAccountId = it },
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showCategorySheet) {
        PocketlyChoiceSheet(
            title = "Choose category",
            items = matchingCategories.map { cat ->
                SheetChoiceItem(cat.id, cat.name, iconKey = cat.icon, colorKey = cat.color)
            },
            selectedId = selectedCategoryId,
            onSelect = { selectedCategoryId = it },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showDueDateSheet) {
        PocketlyCalendarSheet(
            title = "Next due date",
            selectedDayKey = dueKey,
            allowNone = false,
            onSelect = { picked -> if (picked != null) dueKey = picked },
            onDismiss = { showDueDateSheet = false }
        )
    }

    if (showDeleteConfirm && existing != null) {
        PocketlyDeleteConfirmSheet(
            title = "Delete scheduled item",
            message = "Remove \"${existing.name}\" from scheduled payments? Past recorded transactions won't be affected.",
            confirmLabel = "Delete item",
            onConfirm = { onDelete(existing.id) },
            onDismiss = { showDeleteConfirm = false },
            buttonTestTag = "confirm_delete_scheduled"
        )
    }
}

// ─────────────────────────── ACTIVITY SCREEN ───────────────────────────

@Composable
fun ActivityScreen(
    state: PocketlyUiState,
    onBack: () -> Unit,
    onTapItem: (ActivityItem) -> Unit,
    onMarkAllRead: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val todayKey = remember { DateUtils.today().toDayKey() }
    val todayItems = remember(state.activityItems, todayKey) {
        state.activityItems.filter { it.createdAt.startsWith(todayKey) }
    }
    val earlierItems = remember(state.activityItems, todayKey) {
        state.activityItems.filter { !it.createdAt.startsWith(todayKey) }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        SubScreenTopBar(
            title = "Activity",
            onBack = onBack,
            trailingAction = {
                if (state.unreadActivityCount > 0) {
                    TextButton(onClick = onMarkAllRead, modifier = Modifier.testTag("activity_mark_all_read")) {
                        Text("Mark all read", style = PocketlyTextStyles.label, color = colors.mint)
                    }
                }
            }
        )

        if (state.activityItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
                PocketlyEmptyState(title = "You're all caught up", subtitle = "Budget alerts and scheduled payments will show up here.", showMascot = true)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (todayItems.isNotEmpty()) {
                    item { Text("Today", style = PocketlyTextStyles.label, color = colors.inkMuted, modifier = Modifier.padding(horizontal = 4.dp)) }
                    items(todayItems, key = { it.id }) { item -> ActivityCard(item, onTapItem) }
                }
                if (earlierItems.isNotEmpty()) {
                    item { Text("Earlier", style = PocketlyTextStyles.label, color = colors.inkMuted, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) }
                    items(earlierItems, key = { it.id }) { item -> ActivityCard(item, onTapItem) }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(item: ActivityItem, onTap: (ActivityItem) -> Unit) {
    val colors = PocketlyTheme.colors
    val (iconKey, colorKey) = when (item.kind) {
        ActivityKind.BUDGET_90 -> "shopping_bag" to "pink"
        ActivityKind.BUDGET_100 -> "zap" to "coral"
        ActivityKind.SCHEDULED_PAID -> "home" to "orange"
        ActivityKind.SCHEDULED_DUE -> "zap" to "orange"
        ActivityKind.MILESTONE -> "trending_up" to "mint"
    }

    PocketlyCard(
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
        onClick = { onTap(item) }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CategoryIconTile(iconKey, colorKey, size = 42.dp, iconSize = 20.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = PocketlyTextStyles.rowTitle, color = colors.ink)
                Spacer(Modifier.height(3.dp))
                Text(item.body, style = PocketlyTextStyles.caption, color = colors.inkMuted)
            }
            if (item.readAt == null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(colors.mint)
                )
            }
        }
    }
}

// ─────────────────────────── TRANSACTION SEARCH SCREEN ───────────────────────────

@Composable
fun TransactionSearchScreen(
    state: PocketlyUiState,
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val colors = PocketlyTheme.colors
    var query by remember { mutableStateOf("") }
    var kindFilter by remember { mutableStateOf<TransactionKind?>(null) }

    val results = remember(state.transactions, query, kindFilter, state.categoriesById, state.accountsById) {
        val q = query.trim().lowercase()
        state.transactions.filter { tx ->
            val kindOk = kindFilter == null || tx.kind == kindFilter
            if (!kindOk) return@filter false
            if (q.isEmpty()) return@filter true
            val catName = tx.categoryId?.let { state.categoriesById[it]?.name }?.lowercase() ?: ""
            val accName = state.accountsById[tx.accountId]?.name?.lowercase() ?: ""
            val amt = MoneyMath.formatCents(tx.amountCents, state.settings.currency).lowercase()
            catName.contains(q) ||
                accName.contains(q) ||
                (tx.merchant?.lowercase()?.contains(q) == true) ||
                (tx.note?.lowercase()?.contains(q) == true) ||
                amt.contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        SubScreenTopBar(title = "Search", onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    if (query.isEmpty()) Text("Merchant, note, category, or amount", style = PocketlyTextStyles.inputValue, color = colors.inkMuted)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = PocketlyTextStyles.inputValue.copy(color = colors.ink),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.mint),
                        modifier = Modifier.fillMaxWidth().testTag("search_query_input")
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = colors.inkMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchFilterPill("All", kindFilter == null) { kindFilter = null }
                SearchFilterPill("Expense", kindFilter == TransactionKind.EXPENSE) { kindFilter = TransactionKind.EXPENSE }
                SearchFilterPill("Income", kindFilter == TransactionKind.INCOME) { kindFilter = TransactionKind.INCOME }
                SearchFilterPill("Transfer", kindFilter == TransactionKind.TRANSFER) { kindFilter = TransactionKind.TRANSFER }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (results.isEmpty()) {
            Box(Modifier.padding(16.dp)) {
                PocketlyEmptyState(title = "No matching transactions", subtitle = "Try a different keyword or filter.", showMascot = false)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PocketlyCard(cornerRadius = 24.dp, contentPadding = PaddingValues(vertical = 4.dp)) {
                        results.forEachIndexed { idx, tx ->
                            SwipeableTransactionRow(tx = tx, state = state, onEdit = { onEditTransaction(tx) }, onDelete = { onDeleteTransaction(tx) })
                            if (idx < results.lastIndex) {
                                Box(Modifier.fillMaxWidth().padding(start = 72.dp, end = 16.dp).height(1.dp).background(colors.line))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = PocketlyTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) colors.mintSoft else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = if (selected) PocketlyTextStyles.chipSelected else PocketlyTextStyles.chip,
            color = if (selected) colors.mint else colors.inkMuted
        )
    }
}

// ─────────────────────────── PROFILE SCREEN (LIGHT & DARK) ───────────────────────────

@Composable
fun ProfileScreen(
    state: PocketlyUiState,
    biometricAvailable: Boolean,
    onUpdateProfile: (String, String) -> Unit,
    onChangePin: (String) -> Unit,
    onRemovePin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onUpdateLockAfter: (String) -> Unit,
    onUpdateCurrency: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenAccountsTab: () -> Unit,
    onOpenBudgetsTab: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onLoadDemoData: () -> Unit,
    onClearAllData: () -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateReduceMotion: (Boolean) -> Unit,
    onUpdateSoundEffects: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val settings = state.settings

    var showProfileDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLockAfterDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val initials = settings.profileName.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "A" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar + Name + Local data only lock badge (matching Profile Light & Dark mockups)
        item(key = "profile_header") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(PocketlyPalette.softBackgroundFor(PocketlyPalette.colorForKey(settings.avatarColor), colors.isDark))
                        .clickable { showProfileDialog = true }
                        .testTag("profile_avatar"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = PocketlyTextStyles.screenTitle,
                        color = PocketlyPalette.colorForKey(settings.avatarColor)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = settings.profileName,
                    style = PocketlyTextStyles.screenTitle,
                    color = colors.ink,
                    modifier = Modifier.clickable { showProfileDialog = true }
                )
                Spacer(Modifier.height(6.dp))
                InfoBadgePill(
                    text = "Local data only",
                    icon = Icons.Rounded.Lock,
                    bgColor = colors.mintSoft,
                    fgColor = colors.mint
                )
            }
        }

        // Security Card
        item(key = "profile_security") {
            PocketlyCard(contentPadding = PaddingValues(vertical = 8.dp)) {
                Text(
                    text = "Security",
                    style = PocketlyTextStyles.rowTitle,
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
                ProfileRowItem(
                    icon = Icons.Rounded.Lock,
                    title = "PIN",
                    value = if (settings.hasPin) "On" else "Off",
                    onClick = { showPinDialog = true },
                    tag = "profile_pin_row"
                )
                ProfileDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fingerprint", style = PocketlyTextStyles.body, color = colors.ink)
                        if (!biometricAvailable) {
                            Text("Not available on this device", style = PocketlyTextStyles.caption, color = colors.inkMuted)
                        } else if (!settings.hasPin) {
                            Text("Set a PIN first", style = PocketlyTextStyles.caption, color = colors.inkMuted)
                        }
                    }
                    Switch(
                        checked = settings.biometricEnabled && settings.hasPin,
                        enabled = biometricAvailable && settings.hasPin,
                        onCheckedChange = onToggleBiometric,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.mint, checkedThumbColor = Color.White),
                        modifier = Modifier.testTag("profile_biometric_switch")
                    )
                }
                ProfileDivider()
                ProfileRowItem(
                    icon = Icons.Rounded.Schedule,
                    title = "Lock after",
                    value = when (settings.lockAfter) {
                        "immediate" -> "Immediately"
                        "5m" -> "5 minutes"
                        else -> "30 seconds"
                    },
                    onClick = { showLockAfterDialog = true },
                    tag = "profile_lock_after_row"
                )
            }
        }

        // Money Card
        item(key = "profile_money") {
            PocketlyCard(contentPadding = PaddingValues(vertical = 8.dp)) {
                Text("Money", style = PocketlyTextStyles.rowTitle, color = colors.ink, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                ProfileRowItem(Icons.Rounded.AttachMoney, "Currency", settings.currency, { showCurrencyDialog = true }, "profile_currency_row")
                ProfileDivider()
                ProfileRowItem(Icons.Rounded.GridView, "Categories", "${state.activeCategories.size}", onOpenCategories, "profile_categories_row")
                ProfileDivider()
                ProfileRowItem(Icons.Rounded.AccountBalanceWallet, "Accounts", "${state.activeAccounts.size}", onOpenAccountsTab, "profile_accounts_row")
                ProfileDivider()
                ProfileRowItem(Icons.Rounded.PieChart, "Budgets", "${state.budgets.size}", onOpenBudgetsTab, "profile_budgets_row")
            }
        }

        // Data Card
        item(key = "profile_data") {
            PocketlyCard(contentPadding = PaddingValues(vertical = 8.dp)) {
                Text("Data", style = PocketlyTextStyles.rowTitle, color = colors.ink, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                ProfileRowItem(Icons.Rounded.Download, "Export transactions (CSV)", null, onExportCsv, "profile_export_csv")
                ProfileDivider()
                ProfileRowItem(Icons.Rounded.Download, "Export full backup (JSON)", null, onExportJson, "profile_export_json")
                ProfileDivider()
                ProfileRowItem(Icons.Rounded.Upload, "Import backup (JSON)", null, onImportJson, "profile_import_json")
                ProfileDivider()
                ProfileRowItem(Icons.Rounded.Restore, "Load demo data", null, onLoadDemoData, "profile_load_demo")
                ProfileDivider()
                ProfileRowItem(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Clear all data",
                    value = null,
                    onClick = { showDeleteConfirmDialog = true },
                    tag = "profile_clear_all",
                    titleColor = colors.coral
                )
            }
        }

        // Appearance & About Card
        item(key = "profile_appearance") {
            PocketlyCard(contentPadding = PaddingValues(vertical = 8.dp)) {
                ProfileRowItem(
                    icon = Icons.Rounded.DarkMode,
                    title = "Appearance",
                    value = settings.theme.replaceFirstChar { it.uppercase() },
                    onClick = { showAppearanceDialog = true },
                    tag = "profile_appearance_row"
                )
                ProfileDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.MotionPhotosOff, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Reduce motion", style = PocketlyTextStyles.body, color = colors.ink, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.reduceMotion,
                        onCheckedChange = {
                            feedback.tap()
                            onUpdateReduceMotion(it)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.mint, checkedThumbColor = Color.White),
                        modifier = Modifier.testTag("profile_reduce_motion_switch")
                    )
                }
                ProfileDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Sound effects", style = PocketlyTextStyles.body, color = colors.ink, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.soundEffects,
                        onCheckedChange = { enabled ->
                            onUpdateSoundEffects(enabled)
                            if (enabled) feedback.tap()
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.mint, checkedThumbColor = Color.White),
                        modifier = Modifier.testTag("profile_sound_effects_switch")
                    )
                }
                ProfileDivider()
                ProfileRowItem(
                    icon = Icons.Rounded.Info,
                    title = "About",
                    value = "v1.0",
                    onClick = { showAboutDialog = true },
                    tag = "profile_about_row"
                )
            }
        }
    }

    // Bottom Sheets (no default AlertDialogs anywhere)
    if (showProfileDialog) {
        var nameInput by remember { mutableStateOf(settings.profileName) }
        var nameTouched by remember { mutableStateOf(false) }
        var colorPick by remember { mutableStateOf(settings.avatarColor) }
        val nameError = if (nameTouched && nameInput.trim().isEmpty()) "Enter a name" else null

        PocketlyBottomSheet(
            title = "Profile",
            onDismiss = { showProfileDialog = false }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SheetGroupedCard {
                        SheetTextRow(
                            icon = Icons.Rounded.Person,
                            label = "Name",
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                nameTouched = true
                            },
                            placeholder = "Your name",
                            error = nameError,
                            maxLength = 28,
                            testTagName = "profile_name_input"
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SheetColorSwatches(
                    selectedColorKey = colorPick,
                    onSelectColor = { colorPick = it },
                    colorKeys = listOf("mint", "sky", "purple", "orange", "pink", "teal", "clay", "slate")
                )
                Spacer(Modifier.height(20.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    MintRippleButton(
                        text = "Save",
                        enabled = nameInput.trim().isNotEmpty(),
                        onClick = {
                            nameTouched = true
                            if (nameInput.trim().isNotEmpty()) {
                                onUpdateProfile(nameInput.trim(), colorPick)
                                showProfileDialog = false
                            }
                        },
                        testTagName = "profile_save_button"
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showPinDialog) {
        PinSetupBottomSheet(
            hasExistingPin = settings.hasPin,
            onSavePin = { pin ->
                onChangePin(pin)
                showPinDialog = false
            },
            onRemovePin = {
                onRemovePin()
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false }
        )
    }

    if (showLockAfterDialog) {
        PocketlyChoiceSheet(
            title = "Lock after",
            items = listOf(
                SheetChoiceItem("immediate", "Immediately", "Lock as soon as you leave Pocketly"),
                SheetChoiceItem("30s", "30 seconds", "Short grace period when switching apps"),
                SheetChoiceItem("5m", "5 minutes", "Stay unlocked for quick checks")
            ),
            selectedId = settings.lockAfter,
            onSelect = onUpdateLockAfter,
            onDismiss = { showLockAfterDialog = false }
        )
    }

    if (showCurrencyDialog) {
        PocketlyChoiceSheet(
            title = "Currency",
            items = SupportedCurrencies.all.map { c ->
                SheetChoiceItem(
                    id = c.code,
                    title = c.name,
                    subtitle = c.code,
                    badgeText = c.symbol.trim()
                )
            },
            selectedId = settings.currency,
            onSelect = onUpdateCurrency,
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showAppearanceDialog) {
        PocketlyChoiceSheet(
            title = "Appearance",
            items = listOf(
                SheetChoiceItem("light", "Light", "Bright lavender-tinted canvas"),
                SheetChoiceItem("dark", "Dark", "True black OLED surfaces"),
                SheetChoiceItem("system", "System", "Match your phone setting")
            ),
            selectedId = settings.theme,
            onSelect = onUpdateTheme,
            onDismiss = { showAppearanceDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        PocketlyDeleteConfirmSheet(
            title = "Clear all data",
            message = "This removes every transaction, account, budget, scheduled item, and investment stored on this phone. Type DELETE to confirm.",
            confirmLabel = "Clear all data",
            requireTypedWord = "DELETE",
            onConfirm = onClearAllData,
            onDismiss = { showDeleteConfirmDialog = false },
            inputTestTag = "clear_all_confirm_input",
            buttonTestTag = "clear_all_confirm_button"
        )
    }

    if (showAboutDialog) {
        PocketlyBottomSheet(
            title = "About Pocketly",
            onDismiss = { showAboutDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PocketlyMascot(mood = MascotMood.SLEEPY, modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(12.dp))
                Text("Pocketly v1.0", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                InfoBadgePill(
                    text = "Local data only",
                    icon = Icons.Rounded.Lock,
                    bgColor = colors.mintSoft,
                    fgColor = colors.mint
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Private, strictly offline personal finance tracker. No accounts, no cloud sync, no ads. Your data never leaves this phone.",
                    style = PocketlyTextStyles.body,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PinSetupBottomSheet(
    hasExistingPin: Boolean,
    onSavePin: (String) -> Unit,
    onRemovePin: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PocketlyTheme.colors
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    val activeEntry = if (confirming) confirmPin else firstPin

    PocketlyBottomSheet(
        title = if (hasExistingPin) "Change PIN" else "Set a 4-digit PIN",
        onDismiss = onDismiss,
        leadingAction = if (hasExistingPin) {
            {
                IconButton(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.size(48.dp).testTag("pin_remove_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove PIN",
                        tint = colors.coral,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (confirming) "Re-enter your 4-digit PIN to confirm" else "Enter a new 4-digit PIN",
                style = PocketlyTextStyles.label,
                color = colors.inkMuted
            )
            Spacer(Modifier.height(18.dp))
            PinDotsRow(length = activeEntry.length, shakeTrigger = shakeTrigger)
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMsg ?: " ",
                style = PocketlyTextStyles.caption,
                color = colors.coral
            )
            Spacer(Modifier.height(10.dp))
            PinKeypadGrid(
                showFingerprint = false,
                enabled = true,
                onDigit = { digit ->
                    errorMsg = null
                    if (!confirming) {
                        if (firstPin.length < 4) {
                            firstPin += digit
                            if (firstPin.length == 4) {
                                confirming = true
                            }
                        }
                    } else {
                        if (confirmPin.length < 4) {
                            confirmPin += digit
                            if (confirmPin.length == 4) {
                                if (confirmPin == firstPin) {
                                    onSavePin(confirmPin)
                                } else {
                                    errorMsg = "That PIN doesn't match. Try again."
                                    shakeTrigger++
                                    confirmPin = ""
                                }
                            }
                        }
                    }
                },
                onBackspace = {
                    errorMsg = null
                    if (confirming) {
                        if (confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                        } else {
                            confirming = false
                            firstPin = firstPin.dropLast(1)
                        }
                    } else if (firstPin.isNotEmpty()) {
                        firstPin = firstPin.dropLast(1)
                    }
                },
                onFingerprint = {}
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showRemoveConfirm) {
        PocketlyDeleteConfirmSheet(
            title = "Remove PIN",
            message = "Pocketly will open immediately without asking for a PIN or fingerprint.",
            confirmLabel = "Remove PIN",
            onConfirm = onRemovePin,
            onDismiss = { showRemoveConfirm = false },
            buttonTestTag = "confirm_remove_pin"
        )
    }
}

@Composable
private fun ProfileRowItem(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit,
    tag: String,
    titleColor: Color = PocketlyTheme.colors.ink
) {
    val colors = PocketlyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (titleColor == colors.coral) colors.coral else colors.inkMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = PocketlyTextStyles.body,
            color = titleColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!value.isNullOrBlank()) {
            Text(value, style = PocketlyTextStyles.label, color = colors.inkMuted)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 18.dp)
            .height(1.dp)
            .background(PocketlyTheme.colors.line)
    )
}

// ─────────────────────────── CATEGORIES MANAGER SCREEN ───────────────────────────

@Composable
fun CategoriesManagerScreen(
    state: PocketlyUiState,
    onBack: () -> Unit,
    onSaveCategory: (Category?, String, String, String, CategoryKind) -> Unit,
    onArchiveCategory: (Category) -> Unit
) {
    val colors = PocketlyTheme.colors
    var editingCat by remember { mutableStateOf<Category?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        SubScreenTopBar(
            title = "Categories",
            onBack = onBack,
            trailingAction = {
                IconButton(onClick = { editingCat = null; showDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add category", tint = colors.mint)
                }
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PocketlyCard(cornerRadius = 24.dp, contentPadding = PaddingValues(vertical = 6.dp)) {
                    state.activeCategories.forEachIndexed { idx, cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingCat = cat; showDialog = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconTile(cat.icon, cat.color, size = 40.dp, iconSize = 19.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, style = PocketlyTextStyles.rowTitle, color = colors.ink)
                                Text(cat.kind.key.replaceFirstChar { it.uppercase() }, style = PocketlyTextStyles.caption, color = colors.inkMuted)
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted)
                        }
                        if (idx < state.activeCategories.lastIndex) {
                            Box(Modifier.fillMaxWidth().padding(start = 68.dp, end = 16.dp).height(1.dp).background(colors.line))
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        EditCategorySheet(
            editingCat = editingCat,
            onSave = { name, iconKey, colorKey, kind ->
                onSaveCategory(editingCat, name, iconKey, colorKey, kind)
                showDialog = false
            },
            onArchive = { cat ->
                onArchiveCategory(cat)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun EditCategorySheet(
    editingCat: Category?,
    onSave: (String, String, String, CategoryKind) -> Unit,
    onArchive: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PocketlyTheme.colors
    var name by remember { mutableStateOf(editingCat?.name ?: "") }
    var nameTouched by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(editingCat?.kind ?: CategoryKind.EXPENSE) }
    var colorKey by remember { mutableStateOf(editingCat?.color ?: "orange") }
    var iconKey by remember { mutableStateOf(editingCat?.icon ?: "utensils") }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    val nameError = if (nameTouched && name.trim().isEmpty()) "Enter a name" else null

    PocketlyBottomSheet(
        title = if (editingCat == null) "New category" else "Edit category",
        onDismiss = onDismiss,
        leadingAction = if (editingCat != null) {
            {
                IconButton(
                    onClick = { showArchiveConfirm = true },
                    modifier = Modifier.size(48.dp).testTag("category_archive_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Archive category",
                        tint = colors.coral,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                PocketlySpringSegmentedControl(
                    options = listOf("Expense", "Income"),
                    selectedIndex = if (kind == CategoryKind.EXPENSE) 0 else 1,
                    onSelect = { kind = if (it == 0) CategoryKind.EXPENSE else CategoryKind.INCOME },
                    tagPrefix = "cat_kind"
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                SheetGroupedCard {
                    SheetTextRow(
                        icon = Icons.Rounded.Category,
                        label = "Name",
                        value = name,
                        onValueChange = {
                            name = it
                            nameTouched = true
                        },
                        placeholder = "Groceries, coffee...",
                        error = nameError,
                        maxLength = 24,
                        testTagName = "category_name_input"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SheetColorSwatches(
                selectedColorKey = colorKey,
                onSelectColor = { colorKey = it },
                colorKeys = listOf("orange", "pink", "sky", "lime", "purple", "teal", "clay", "mint")
            )

            Spacer(Modifier.height(12.dp))

            SheetIconSwatches(
                iconKeys = PocketlyIcons.selectableKeys,
                selectedIconKey = iconKey,
                accentColorKey = colorKey,
                onSelectIcon = { iconKey = it }
            )

            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                MintRippleButton(
                    text = if (editingCat == null) "Add category" else "Save changes",
                    enabled = name.trim().isNotEmpty(),
                    onClick = {
                        nameTouched = true
                        if (name.trim().isNotEmpty()) {
                            onSave(name.trim(), iconKey, colorKey, kind)
                        }
                    },
                    testTagName = "category_save_button"
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showArchiveConfirm && editingCat != null) {
        PocketlyDeleteConfirmSheet(
            title = "Archive category",
            message = "Hide \"${editingCat.name}\" from new transactions? Existing transactions will keep this category.",
            confirmLabel = "Archive category",
            onConfirm = { onArchive(editingCat) },
            onDismiss = { showArchiveConfirm = false },
            buttonTestTag = "confirm_archive_category"
        )
    }
}

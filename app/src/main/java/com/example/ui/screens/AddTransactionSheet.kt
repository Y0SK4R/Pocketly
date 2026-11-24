package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryKind
import com.example.domain.DateUtils
import com.example.domain.MoneyMath
import com.example.domain.SupportedCurrencies
import com.example.domain.Transaction
import com.example.domain.TransactionKind
import com.example.ui.components.HeroAmountDisplay
import com.example.ui.components.LocalPocketlyFeedback
import com.example.ui.components.MintRippleButton
import com.example.ui.components.PocketlyBottomSheet
import com.example.ui.components.PocketlyCalendarSheet
import com.example.ui.components.PocketlyChoiceSheet
import com.example.ui.components.PocketlyDeleteConfirmSheet
import com.example.ui.components.PocketlyIcons
import com.example.ui.components.PocketlySpringSegmentedControl
import com.example.ui.components.SheetChoiceItem
import com.example.ui.components.SheetGroupedCard
import com.example.ui.components.SheetPickerRow
import com.example.ui.components.SheetRowDivider
import com.example.ui.components.SheetTextRow
import com.example.ui.theme.PocketlyPalette
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import com.example.ui.viewmodel.PocketlyUiState

@Composable
fun AddTransactionBottomSheet(
    state: PocketlyUiState,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        kind: TransactionKind,
        amountCents: Long,
        accountId: String,
        toAccountId: String?,
        categoryId: String?,
        merchant: String?,
        note: String?,
        dayKey: String,
        keepOpen: Boolean
    ) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val editing = state.addTxSheet.editingTransaction
    val currency = state.settings.currency
    val currencySymbol = SupportedCurrencies.forCode(currency).symbol

    val activeAccounts = state.activeAccounts
    val defaultAccId = editing?.accountId
        ?: state.addTxSheet.initialAccountId
        ?: state.settings.lastAccountId?.takeIf { id -> activeAccounts.any { it.id == id } }
        ?: activeAccounts.firstOrNull()?.id
        ?: ""
    val defaultToAccId = editing?.toAccountId
        ?: activeAccounts.firstOrNull { it.id != defaultAccId }?.id

    var kind by remember(editing) { mutableStateOf(editing?.kind ?: TransactionKind.EXPENSE) }
    var amountStr by remember(editing) {
        mutableStateOf(if (editing != null) MoneyMath.centsToKeypadString(editing.amountCents) else "0")
    }
    var selectedAccountId by remember(editing) { mutableStateOf(defaultAccId) }
    var selectedToAccountId by remember(editing) { mutableStateOf(defaultToAccId) }

    val availableCategories = remember(state.activeCategories, kind, state.settings.lastCategoryId) {
        val targetKind = if (kind == TransactionKind.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        val list = state.activeCategories.filter { it.kind == targetKind }
        val lastId = state.settings.lastCategoryId
        if (lastId != null) list.sortedByDescending { it.id == lastId } else list
    }

    var selectedCategoryId by remember(editing, kind) {
        val initial = editing?.categoryId
            ?: state.addTxSheet.initialCategoryId?.takeIf { id -> availableCategories.any { it.id == id } }
            ?: availableCategories.firstOrNull()?.id
        mutableStateOf(initial)
    }

    var selectedDayKey by remember(editing) { mutableStateOf(editing?.dayKey ?: DateUtils.today().toDayKey()) }
    var noteText by remember(editing) { mutableStateOf(editing?.note ?: editing?.merchant ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var accountPickerMode by remember { mutableStateOf<Boolean?>(null) } // null=closed, false=from, true=to
    var showCalendarSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val amountCents = MoneyMath.parseKeypadInputToCents(amountStr)

    fun submit(keepOpen: Boolean) {
        if (amountCents <= 0L) {
            errorMessage = "Enter an amount above ${currencySymbol}0."
            return
        }
        if (selectedAccountId.isBlank()) {
            errorMessage = "Create an account first."
            return
        }
        if (kind == TransactionKind.TRANSFER) {
            if (selectedToAccountId.isNullOrBlank() || selectedToAccountId == selectedAccountId) {
                errorMessage = "Choose a different account for the transfer."
                return
            }
        }
        errorMessage = null
        onSave(
            editing?.id,
            kind,
            amountCents,
            selectedAccountId,
            selectedToAccountId,
            selectedCategoryId,
            noteText.trim().takeIf { it.isNotEmpty() },
            noteText.trim().takeIf { it.isNotEmpty() },
            selectedDayKey,
            keepOpen
        )
        if (keepOpen) {
            amountStr = "0"
            noteText = ""
        }
    }

    PocketlyBottomSheet(
        title = if (editing != null) "Edit transaction" else "Add transaction",
        onDismiss = onDismiss,
        leadingAction = if (editing != null) {
            {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(48.dp).testTag("tx_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete transaction",
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
            // 1. Expense / Income / Transfer spring segmented control
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                PocketlySpringSegmentedControl(
                    options = listOf("Expense", "Income", "Transfer"),
                    selectedIndex = when (kind) {
                        TransactionKind.EXPENSE -> 0
                        TransactionKind.INCOME -> 1
                        TransactionKind.TRANSFER -> 2
                    },
                    onSelect = { idx ->
                        errorMessage = null
                        kind = when (idx) {
                            0 -> TransactionKind.EXPENSE
                            1 -> TransactionKind.INCOME
                            else -> TransactionKind.TRANSFER
                        }
                    },
                    useMintSelected = true,
                    tagPrefix = "tx_kind"
                )
            }

            Spacer(Modifier.height(10.dp))

            // 2. Giant 56sp amount with blinking mint caret + currency label
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                HeroAmountDisplay(
                    amountStr = amountStr,
                    currencyCode = currency,
                    isActive = true,
                    onClick = {}
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = PocketlyTextStyles.caption,
                    color = colors.coral,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                )
            }

            // 3. Category chips (44dp+ tactile chips, 12dp gaps, 16dp side padding)
            if (kind != TransactionKind.TRANSFER) {
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(availableCategories, key = { it.id }) { cat ->
                        val selected = selectedCategoryId == cat.id
                        val tint = PocketlyPalette.colorForKey(cat.color)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) tint else colors.surface2)
                                .clickable {
                                    feedback.tap()
                                    selectedCategoryId = cat.id
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("tx_cat_${cat.id}")
                        ) {
                            Icon(
                                imageVector = PocketlyIcons.forKey(cat.icon),
                                contentDescription = cat.name,
                                tint = if (selected) Color.White else tint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = cat.name.split(" ").first(),
                                style = if (selected) PocketlyTextStyles.navLabelSelected else PocketlyTextStyles.navLabel,
                                color = if (selected) Color.White else colors.ink,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4. Grouped card (surface-2, 18dp radius, 56dp rows, 1dp --line dividers)
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                SheetGroupedCard {
                    val fromName = state.accountsById[selectedAccountId]?.name ?: "Select account"
                    SheetPickerRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        label = if (kind == TransactionKind.TRANSFER) "From" else "Account",
                        value = fromName,
                        onClick = { accountPickerMode = false },
                        testTagName = "tx_account_row"
                    )
                    if (kind == TransactionKind.TRANSFER) {
                        SheetRowDivider()
                        val toName = selectedToAccountId?.let { state.accountsById[it]?.name } ?: "Choose destination"
                        SheetPickerRow(
                            icon = Icons.Rounded.SwapHoriz,
                            label = "To",
                            value = toName,
                            onClick = { accountPickerMode = true },
                            testTagName = "tx_to_account_row"
                        )
                    }
                    SheetRowDivider()
                    SheetPickerRow(
                        icon = Icons.Rounded.CalendarToday,
                        label = "Date",
                        value = DateUtils.formatDayHeader(selectedDayKey),
                        onClick = { showCalendarSheet = true },
                        testTagName = "tx_date_row"
                    )
                    SheetRowDivider()
                    SheetTextRow(
                        icon = Icons.Rounded.Description,
                        label = "Note",
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = "Merchant or note",
                        maxLength = 60,
                        testTagName = "tx_note_input"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 5. Custom 3x4 numeric keypad
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                NumericAmountKeypad(
                    onDigit = { d ->
                        errorMessage = null
                        amountStr = appendKeypadDigit(amountStr, d)
                    },
                    onDecimal = {
                        errorMessage = null
                        if (!amountStr.contains(".")) amountStr = "$amountStr."
                    },
                    onBackspace = {
                        errorMessage = null
                        amountStr = if (amountStr.length <= 1) "0" else amountStr.dropLast(1)
                    }
                )
            }

            Spacer(Modifier.height(14.dp))

            // 6. Full-width 56dp Mint Ripple Save button
            val canSave = amountCents > 0L
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                MintRippleButton(
                    text = if (editing != null) "Save changes" else "Save",
                    enabled = canSave,
                    onClick = { submit(keepOpen = false) },
                    testTagName = "tx_save_button"
                )
            }

            // Secondary button: Save and add another (new mode only; edit delete sits cleanly in header)
            if (editing == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { submit(keepOpen = true) },
                        enabled = canSave,
                        modifier = Modifier.testTag("tx_save_another_button")
                    ) {
                        Text(
                            text = "Save and add another",
                            style = PocketlyTextStyles.button,
                            color = if (canSave) colors.mint else colors.inkMuted
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Account picker bottom sheet (replaces AlertDialog)
    accountPickerMode?.let { pickingTo ->
        PocketlyChoiceSheet(
            title = if (pickingTo) "To account" else "Choose account",
            items = activeAccounts.map { acc ->
                SheetChoiceItem(
                    id = acc.id,
                    title = acc.name,
                    subtitle = acc.kind.label,
                    iconKey = acc.icon,
                    colorKey = acc.color
                )
            },
            selectedId = if (pickingTo) selectedToAccountId else selectedAccountId,
            onSelect = { id ->
                if (pickingTo) selectedToAccountId = id else selectedAccountId = id
            },
            onDismiss = { accountPickerMode = null }
        )
    }

    // Calendar date picker bottom sheet (replaces DatePickerDialog)
    if (showCalendarSheet) {
        PocketlyCalendarSheet(
            title = "Transaction date",
            selectedDayKey = selectedDayKey,
            allowNone = false,
            onSelect = { picked -> if (picked != null) selectedDayKey = picked },
            onDismiss = { showCalendarSheet = false }
        )
    }

    if (showDeleteConfirm && editing != null) {
        PocketlyDeleteConfirmSheet(
            title = "Delete transaction",
            message = "Remove this ${MoneyMath.formatCents(editing.amountCents, currency)} transaction? You can still undo right after deleting.",
            confirmLabel = "Delete transaction",
            onConfirm = { onDelete(editing) },
            onDismiss = { showDeleteConfirm = false },
            buttonTestTag = "confirm_delete_tx"
        )
    }
}

@Composable
fun NumericAmountKeypad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface2)
                            .clickable {
                                feedback.scrubTick()
                                onDigit(digit)
                            }
                            .testTag("amt_key_$digit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit, style = PocketlyTextStyles.mediumNumber, color = colors.ink)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .clickable {
                        feedback.scrubTick()
                        onDecimal()
                    }
                    .testTag("amt_key_dot"),
                contentAlignment = Alignment.Center
            ) {
                Text(".", style = PocketlyTextStyles.mediumNumber, color = colors.ink)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .clickable {
                        feedback.scrubTick()
                        onDigit("0")
                    }
                    .testTag("amt_key_0"),
                contentAlignment = Alignment.Center
            ) {
                Text("0", style = PocketlyTextStyles.mediumNumber, color = colors.ink)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .clickable {
                        feedback.scrubTick()
                        onBackspace()
                    }
                    .testTag("amt_key_back")
                    .semantics { contentDescription = "Backspace" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Delete", tint = colors.ink, modifier = Modifier.size(20.dp))
            }
        }
    }
}

fun appendKeypadDigit(current: String, digit: String): String {
    if (current.contains(".")) {
        val decimals = current.substringAfter(".")
        if (decimals.length >= 2) return current
        return current + digit
    }
    if (current == "0") return digit
    if (current.length >= 7) return current
    return current + digit
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.domain.CompoundingMode
import com.example.domain.DateUtils
import com.example.domain.FinanceEngine
import com.example.domain.Holding
import com.example.domain.HoldingMode
import com.example.domain.HoldingType
import com.example.domain.InvestRange
import com.example.domain.MoneyMath
import com.example.domain.SeriesPoint
import com.example.ui.components.AnimatedCurrencyText
import com.example.ui.components.CategoryIconTile
import com.example.ui.components.HeroAmountDisplay
import com.example.ui.components.InfoBadgePill
import com.example.ui.components.InvestmentGrowthLineChart
import com.example.ui.components.LocalPocketlyFeedback
import com.example.ui.components.MintRippleButton
import com.example.ui.components.PocketlyBottomSheet
import com.example.ui.components.PocketlyCalendarSheet
import com.example.ui.components.PocketlyCard
import com.example.ui.components.PocketlyDeleteConfirmSheet
import com.example.ui.components.PocketlyEmptyState
import com.example.ui.components.PocketlyIcons
import com.example.ui.components.PocketlySpringSegmentedControl
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.SheetColorSwatches
import com.example.ui.components.SheetGroupedCard
import com.example.ui.components.SheetIconSwatches
import com.example.ui.components.SheetPercentStepperRow
import com.example.ui.components.SheetPickerRow
import com.example.ui.components.SheetRowDivider
import com.example.ui.components.SheetTextRow
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.PocketlyPalette
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import com.example.ui.viewmodel.GrowthCalculatorState
import com.example.ui.viewmodel.PocketlyUiState
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun InvestScreen(
    state: PocketlyUiState,
    onSelectRange: (InvestRange) -> Unit,
    onOpenHoldingDetail: (String) -> Unit,
    onOpenGrowthCalculator: () -> Unit,
    onOpenAddHolding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val today = remember { DateUtils.today() }

    val valuations = remember(state.activeHoldings, today) {
        state.activeHoldings.map { FinanceEngine.computeHoldingValuation(it, today) }
            .sortedByDescending { it.currentValueCents }
    }
    val totalValue = remember(valuations) { valuations.sumOf { it.currentValueCents } }
    val totalPutIn = remember(valuations) { valuations.sumOf { it.totalPutInCents } }
    val totalGrowth = totalValue - totalPutIn
    val totalGrowthPct = if (totalPutIn > 0L) (totalGrowth.toDouble() / totalPutIn.toDouble()) * 100.0 else null
    val estYearlyYield = remember(valuations) { valuations.sumOf { it.estimatedYearlyYieldCents } }

    val portfolioSeries = remember(state.activeHoldings, state.selectedInvestRange, today) {
        FinanceEngine.computePortfolioSeries(state.activeHoldings, state.selectedInvestRange, today)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "invest_title") {
            Text(
                text = "Investments",
                style = PocketlyTextStyles.screenTitle,
                color = colors.ink,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (state.activeHoldings.isEmpty()) {
            item(key = "invest_empty") {
                PocketlyEmptyState(
                    title = "No investments yet",
                    subtitle = "Track high-yield savings, Treasury bills, CDs, or index funds—or model your plan with the calculator.",
                    primaryLabel = "Add investment",
                    onPrimaryClick = onOpenAddHolding,
                    secondaryLabel = "Try the growth calculator",
                    onSecondaryClick = onOpenGrowthCalculator
                )
            }
            return@LazyColumn
        }

        item(key = "invest_hero_card") {
            PocketlyCard {
                Text("Total invested value", style = PocketlyTextStyles.label, color = colors.inkMuted)
                Spacer(Modifier.height(4.dp))
                AnimatedCurrencyText(
                    targetCents = totalValue,
                    currencyCode = currency,
                    style = PocketlyTextStyles.heroNumber,
                    color = colors.ink
                )
                Spacer(Modifier.height(4.dp))
                val pos = totalGrowth >= 0L
                val gainColor = if (pos) colors.mint else colors.coral
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (pos) Icons.Rounded.NorthEast else Icons.Rounded.SouthEast,
                        contentDescription = null,
                        tint = gainColor,
                        modifier = Modifier.size(16.dp)
                    )
                    val pctPart = totalGrowthPct?.let { " (${MoneyMath.formatPercent(it, showPlus = true, decimals = 1)})" } ?: ""
                    Text(
                        text = "${MoneyMath.formatCents(totalGrowth, currency, showPlus = true)}$pctPart all time",
                        style = PocketlyTextStyles.labelTabular,
                        color = gainColor
                    )
                }

                Spacer(Modifier.height(14.dp))

                SegmentedTabs(
                    options = InvestRange.entries.map { it.label },
                    selectedIndex = InvestRange.entries.indexOf(state.selectedInvestRange),
                    onSelect = { onSelectRange(InvestRange.entries[it]) },
                    tagPrefix = "invest_range"
                )

                Spacer(Modifier.height(10.dp))

                InvestmentGrowthLineChart(
                    points = portfolioSeries,
                    currencyCode = currency,
                    height = 204.dp,
                    showPutInSeries = true,
                    showAxisLabels = true,
                    showLegend = true,
                    testTagName = "invest_portfolio_chart"
                )
            }
        }

        item(key = "invest_holdings_card") {
            val reduceMotion = PocketlyTheme.reduceMotion
            PocketlyCard(
                modifier = if (reduceMotion) Modifier else Modifier.animateContentSize(
                    animationSpec = spring(stiffness = 220f, dampingRatio = 0.78f)
                )
            ) {
                Text("Holdings", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                Spacer(Modifier.height(10.dp))

                valuations.forEachIndexed { idx, v ->
                    val h = v.holding
                    val subLabel = when {
                        v.isMatured -> "Matured"
                        h.mode == HoldingMode.YIELD -> "${MoneyMath.formatPercent((h.apy ?: 0.0) * 100.0, decimals = 1)} APY"
                        else -> "Variable · ${DateUtils.formatRelativeUpdated(v.lastUpdatedDayKey ?: DateUtils.today().toDayKey(), today)}"
                    }
                    val gPos = v.growthCents >= 0L
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenHoldingDetail(h.id) }
                            .padding(vertical = 10.dp, horizontal = 2.dp)
                            .testTag("holding_row_${h.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconTile(h.icon, h.color, size = 44.dp, iconSize = 21.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = h.name,
                                style = PocketlyTextStyles.rowTitle,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (v.isMatured) {
                                    InfoBadgePill(
                                        text = "Matured",
                                        bgColor = colors.amberSoft,
                                        fgColor = colors.amber
                                    )
                                } else {
                                    Text(subLabel, style = PocketlyTextStyles.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = MoneyMath.formatCents(v.currentValueCents, currency, dropWholeZeros = true),
                                style = PocketlyTextStyles.rowAmount,
                                color = colors.ink
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (gPos) colors.mintSoft else colors.coralSoft)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = MoneyMath.formatCents(v.growthCents, currency, showPlus = true, dropWholeZeros = true),
                                    style = PocketlyTextStyles.captionTabular,
                                    color = if (gPos) colors.mint else colors.coral
                                )
                            }
                        }
                    }
                    if (idx < valuations.lastIndex) {
                        Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(colors.line))
                    }
                }
            }
        }

        item(key = "invest_yearly_yield") {
            PocketlyCard(
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.mint, modifier = Modifier.size(20.dp))
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Estimated yearly yield:", style = PocketlyTextStyles.label, color = colors.ink)
                            Text(
                                text = MoneyMath.formatCents(estYearlyYield, currency, showPlus = true, dropWholeZeros = true),
                                style = PocketlyTextStyles.rowAmount,
                                color = colors.mint
                            )
                        }
                        Text(
                            text = "Based on current balances and rates",
                            style = PocketlyTextStyles.caption,
                            color = colors.inkMuted
                        )
                    }
                }
            }
        }

        item(key = "invest_calc_link") {
            PocketlyCard(
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                onClick = onOpenGrowthCalculator,
                modifier = Modifier.testTag("invest_open_calculator")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.mintSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Calculate, contentDescription = null, tint = colors.mint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Try the growth calculator", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                        Text("Model monthly savings and compound yield", style = PocketlyTextStyles.caption, color = colors.inkMuted)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Open growth calculator", tint = colors.inkMuted)
                }
            }
        }
    }
}

/**
 * Redesigned Add Investment Bottom Sheet matching Pocketly's exact 8-section spec:
 * 1. Drag handle + "Add investment" (Bricolage 600, 22sp) + top-right X (no Cancel button)
 * 2. 48dp spring.soft segmented control: "Grows from a rate" | "I'll enter the value"
 * 3. Giant 56sp starting amount + blinking mint caret + muted currency label + inline custom keypad
 * 4. Horizontally scrolling 44dp type chips (12dp gaps, 16dp side padding, color-filled selected)
 * 5. Grouped surface-2 card (18dp radius, 1dp dividers, 56dp rows, 2dp mint focus ring, coral errors)
 * 6. 8 color swatches (40dp, 12dp gaps, 3dp ring) + 6 icons row
 * 7. Live yield preview line ("About $X a year at 4.5%, worth $Y after 5 years")
 * 8. One wide 56dp mint ripple "Add investment" button (disabled until name & amount valid)
 */
@Composable
fun AddInvestmentSheet(
    currencyCode: String,
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        type: HoldingType,
        mode: HoldingMode,
        principalCents: Long,
        apyPercent: Double?,
        startDayKey: String,
        maturityDayKey: String?,
        icon: String,
        color: String
    ) -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val todayKey = remember { DateUtils.today().toDayKey() }

    var mode by remember { mutableStateOf(HoldingMode.YIELD) }
    var amountStr by remember { mutableStateOf("0") }
    var keypadExpanded by remember { mutableStateOf(true) }
    var type by remember { mutableStateOf(HoldingType.SAVINGS) }
    var name by remember { mutableStateOf("") }
    var nameTouched by remember { mutableStateOf(false) }
    var apyText by remember { mutableStateOf("4.5") }
    var startDayKey by remember { mutableStateOf(todayKey) }
    var maturityDayKey by remember { mutableStateOf<String?>(null) }
    var colorKey by remember { mutableStateOf("orange") }
    var iconKey by remember { mutableStateOf("piggy_bank") }

    var showStartDateSheet by remember { mutableStateOf(false) }
    var showMaturityDateSheet by remember { mutableStateOf(false) }

    val amountCents = MoneyMath.parseKeypadInputToCents(amountStr)
    val parsedApy = apyText.toDoubleOrNull()
    val apyInvalid = mode == HoldingMode.YIELD && (parsedApy == null || parsedApy < 0.0 || parsedApy > 20.0)
    val nameError = if (nameTouched && name.trim().isEmpty()) "Enter a name" else null
    val apyError = if (apyInvalid) "APY must be between 0% and 20%" else null
    val canSubmit = name.trim().isNotEmpty() && amountCents > 0L && !apyInvalid

    val typeItems = remember {
        listOf(
            Triple(HoldingType.SAVINGS, "Savings", "piggy_bank" to "orange"),
            Triple(HoldingType.TBILL, "Treasury bills", "document" to "sky"),
            Triple(HoldingType.CD, "CD", "certificate" to "purple"),
            Triple(HoldingType.BOND, "Bond", "landmark" to "teal"),
            Triple(HoldingType.INDEX, "Index fund", "trending_up" to "pink"),
            Triple(HoldingType.OTHER, "Other", "banknote" to "slate")
        )
    }
    val iconChoices = remember { listOf("piggy_bank", "document", "certificate", "landmark", "trending_up", "banknote") }

    PocketlyBottomSheet(
        title = "Add investment",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 2. Segmented control "Grows from a rate | I'll enter the value" (48dp, spring.soft)
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                PocketlySpringSegmentedControl(
                    options = listOf("Grows from a rate", "I'll enter the value"),
                    selectedIndex = if (mode == HoldingMode.YIELD) 0 else 1,
                    onSelect = { idx ->
                        mode = if (idx == 0) HoldingMode.YIELD else HoldingMode.MANUAL
                    },
                    tagPrefix = "add_inv_mode"
                )
            }

            Spacer(Modifier.height(14.dp))

            // 3. Giant 56sp hero amount with blinking caret + muted currency label
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                HeroAmountDisplay(
                    amountStr = amountStr,
                    currencyCode = currencyCode,
                    isActive = keypadExpanded,
                    onClick = { keypadExpanded = !keypadExpanded },
                    modifier = Modifier.testTag("add_inv_hero_amount")
                )
            }

            AnimatedVisibility(
                visible = keypadExpanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    NumericAmountKeypad(
                        onDigit = { amountStr = appendKeypadDigit(amountStr, it) },
                        onDecimal = { if (!amountStr.contains(".")) amountStr = "$amountStr." },
                        onBackspace = { amountStr = if (amountStr.length <= 1) "0" else amountStr.dropLast(1) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4. Horizontally scrolling 44dp type chips (12dp gaps, 16dp side padding)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(typeItems) { (t, label, iconAndColor) ->
                    val selected = type == t
                    val chipAccent = PocketlyPalette.colorForKey(iconAndColor.second)
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (selected) chipAccent else colors.surface2)
                            .clickable {
                                feedback.tap()
                                type = t
                                iconKey = iconAndColor.first
                                colorKey = iconAndColor.second
                                if (t == HoldingType.INDEX) mode = HoldingMode.MANUAL
                                keypadExpanded = false
                            }
                            .padding(horizontal = 14.dp)
                            .testTag("inv_type_${t.key}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PocketlyIcons.forKey(iconAndColor.first),
                            contentDescription = null,
                            tint = if (selected) Color.White else chipAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            style = if (selected) PocketlyTextStyles.chipSelected else PocketlyTextStyles.chip,
                            color = if (selected) Color.White else colors.ink,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 5. Grouped surface-2 card (18dp radius, 1dp --line dividers, 56dp rows)
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
                        placeholder = type.label,
                        error = nameError,
                        maxLength = 36,
                        testTagName = "add_inv_name_input"
                    )

                    if (mode == HoldingMode.YIELD) {
                        SheetRowDivider()
                        SheetPercentStepperRow(
                            icon = Icons.Rounded.Percent,
                            label = "Yearly APY",
                            percentText = apyText,
                            onPercentTextChange = {
                                apyText = it
                                keypadExpanded = false
                            },
                            onStep = { delta ->
                                keypadExpanded = false
                                val cur = apyText.toDoubleOrNull() ?: 0.0
                                val next = (cur + delta).coerceIn(0.0, 20.0)
                                apyText = String.format(Locale.US, "%.1f", next)
                            },
                            error = apyError
                        )
                    }

                    SheetRowDivider()
                    SheetPickerRow(
                        icon = Icons.Rounded.CalendarToday,
                        label = "Start date",
                        value = DateUtils.formatDayHeader(startDayKey),
                        onClick = {
                            keypadExpanded = false
                            showStartDateSheet = true
                        },
                        testTagName = "add_inv_start_date"
                    )

                    if (mode == HoldingMode.YIELD) {
                        SheetRowDivider()
                        SheetPickerRow(
                            icon = Icons.Rounded.EventAvailable,
                            label = "Maturity date",
                            value = maturityDayKey?.let { DateUtils.formatShortDate(it) } ?: "None",
                            valueColor = if (maturityDayKey == null) colors.inkMuted else colors.ink,
                            onClick = {
                                keypadExpanded = false
                                showMaturityDateSheet = true
                            },
                            testTagName = "add_inv_maturity_date"
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 6. Color (8 swatches of 40dp, 12dp gaps, 3dp ring) & Icon (6 icons row)
            SheetColorSwatches(
                selectedColorKey = colorKey,
                onSelectColor = { colorKey = it }
            )
            Spacer(Modifier.height(12.dp))
            SheetIconSwatches(
                iconKeys = iconChoices,
                selectedIconKey = iconKey,
                accentColorKey = colorKey,
                onSelectIcon = { iconKey = it }
            )

            Spacer(Modifier.height(16.dp))

            // 7. Live preview line updating as you type
            val safeRate = (parsedApy ?: 0.0).coerceIn(0.0, 20.0)
            val yearlyYieldCents = (amountCents.toDouble() * (safeRate / 100.0)).roundToLong()
            val fiveYearResult = remember(amountCents, safeRate) {
                FinanceEngine.calculateGrowth(
                    startingCents = amountCents,
                    monthlyContributionCents = 0L,
                    annualYieldRate = safeRate / 100.0,
                    years = 5,
                    compounding = CompoundingMode.MONTHLY
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val preview = if (mode == HoldingMode.YIELD && amountCents > 0L && !apyInvalid) {
                    val yrStr = MoneyMath.formatCents(yearlyYieldCents, currencyCode, dropWholeZeros = true)
                    val fiveYrStr = MoneyMath.formatCents(fiveYearResult.finalValueCents, currencyCode, dropWholeZeros = true)
                    val rateStr = MoneyMath.formatPercent(safeRate, decimals = 1)
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.mint, fontWeight = FontWeight.Bold)) {
                            append("About $yrStr a year")
                        }
                        withStyle(SpanStyle(color = colors.inkMuted)) {
                            append(" at $rateStr, worth ")
                        }
                        withStyle(SpanStyle(color = colors.mint, fontWeight = FontWeight.Bold)) {
                            append(fiveYrStr)
                        }
                        withStyle(SpanStyle(color = colors.inkMuted)) {
                            append(" after 5 years")
                        }
                    }
                } else if (mode == HoldingMode.MANUAL && amountCents > 0L) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.mint, fontWeight = FontWeight.Bold)) {
                            append(MoneyMath.formatCents(amountCents, currencyCode, dropWholeZeros = true))
                        }
                        withStyle(SpanStyle(color = colors.inkMuted)) {
                            append(" starting value · Update anytime in detail view")
                        }
                    }
                } else {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.inkMuted)) {
                            append("Enter a starting amount and name to preview growth")
                        }
                    }
                }
                Text(
                    text = preview,
                    style = PocketlyTextStyles.caption,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("add_inv_live_preview")
                )
            }

            Spacer(Modifier.height(16.dp))

            // 8. One wide 56dp mint "Add investment" button with liquid ripple. No secondary button.
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                MintRippleButton(
                    text = "Add investment",
                    enabled = canSubmit,
                    onClick = {
                        nameTouched = true
                        if (canSubmit) {
                            onAdd(
                                name.trim(),
                                type,
                                mode,
                                amountCents,
                                if (mode == HoldingMode.YIELD) safeRate else null,
                                startDayKey,
                                if (mode == HoldingMode.YIELD) maturityDayKey else null,
                                iconKey,
                                colorKey
                            )
                        }
                    },
                    testTagName = "confirm_add_investment"
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showStartDateSheet) {
        PocketlyCalendarSheet(
            title = "Start date",
            selectedDayKey = startDayKey,
            allowNone = false,
            onSelect = { picked -> if (picked != null) startDayKey = picked },
            onDismiss = { showStartDateSheet = false }
        )
    }

    if (showMaturityDateSheet) {
        PocketlyCalendarSheet(
            title = "Maturity date",
            selectedDayKey = maturityDayKey,
            allowNone = true,
            onSelect = { picked -> maturityDayKey = picked },
            onDismiss = { showMaturityDateSheet = false }
        )
    }
}

@Composable
fun HoldingDetailScreen(
    holdingId: String,
    state: PocketlyUiState,
    onBack: () -> Unit,
    onDepositOrWithdraw: (holdingId: String, signedCents: Long, dayKey: String) -> Unit,
    onEditRate: (Holding, Double) -> Unit,
    onUpdateManualValue: (String, Long) -> Unit,
    onArchive: (Holding) -> Unit
) {
    val colors = PocketlyTheme.colors
    val currency = state.settings.currency
    val today = remember { DateUtils.today() }
    val holding = state.holdings.firstOrNull { it.id == holdingId }

    if (holding == null) {
        Column(Modifier.fillMaxSize().background(colors.bg)) {
            SubScreenTopBar("Investment", onBack)
            PocketlyEmptyState(title = "Holding not found", showMascot = false, modifier = Modifier.padding(16.dp))
        }
        return
    }

    val valuation = remember(holding, today) { FinanceEngine.computeHoldingValuation(holding, today) }
    val series = remember(holding, today) { FinanceEngine.computePortfolioSeries(listOf(holding), InvestRange.ALL, today) }

    var activeSheetMode by remember { mutableStateOf<String?>(null) } // "deposit", "withdraw", "rate", "value"
    var showArchiveConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        SubScreenTopBar(
            title = holding.name,
            onBack = onBack,
            trailingAction = {
                IconButton(onClick = { showArchiveConfirm = true }) {
                    Icon(Icons.Rounded.Archive, contentDescription = "Archive holding", tint = colors.inkMuted)
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
                    Text(
                        text = if (holding.mode == HoldingMode.YIELD) {
                            "${MoneyMath.formatPercent((holding.apy ?: 0.0) * 100.0, decimals = 1)} APY"
                        } else {
                            DateUtils.formatRelativeUpdated(valuation.lastUpdatedDayKey ?: today.toDayKey(), today)
                        },
                        style = PocketlyTextStyles.label,
                        color = colors.inkMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    AnimatedCurrencyText(
                        targetCents = valuation.currentValueCents,
                        currencyCode = currency,
                        style = PocketlyTextStyles.heroNumber
                    )
                    Spacer(Modifier.height(4.dp))
                    val gPos = valuation.growthCents >= 0L
                    val pctText = valuation.growthPercent?.let { " (${MoneyMath.formatPercent(it, showPlus = true)})" } ?: ""
                    Text(
                        text = "${MoneyMath.formatCents(valuation.growthCents, currency, showPlus = true)}$pctText · Put in ${MoneyMath.formatCents(valuation.totalPutInCents, currency, dropWholeZeros = true)}",
                        style = PocketlyTextStyles.labelTabular,
                        color = if (gPos) colors.mint else colors.coral
                    )
                    Spacer(Modifier.height(10.dp))
                    InvestmentGrowthLineChart(
                        points = series,
                        currencyCode = currency,
                        height = 196.dp,
                        showPutInSeries = true,
                        showAxisLabels = true,
                        showLegend = true,
                        testTagName = "holding_detail_chart"
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { activeSheetMode = "deposit" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.mint, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("holding_deposit_btn")
                    ) { Text("Add deposit", style = PocketlyTextStyles.button, maxLines = 1) }

                    OutlinedButton(
                        onClick = { activeSheetMode = "withdraw" },
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface, contentColor = colors.ink),
                        border = BorderStroke(1.dp, colors.line),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("holding_withdraw_btn")
                    ) { Text("Withdraw", style = PocketlyTextStyles.button, color = colors.ink, maxLines = 1) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        activeSheetMode = if (holding.mode == HoldingMode.YIELD) "rate" else "value"
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface, contentColor = colors.mint),
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("holding_secondary_btn")
                ) {
                    Text(
                        text = if (holding.mode == HoldingMode.YIELD) "Edit rate" else "Update value",
                        style = PocketlyTextStyles.button,
                        color = colors.mint
                    )
                }
            }

            item {
                PocketlyCard {
                    Text("Contribution history", style = PocketlyTextStyles.sectionTitle, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    if (holding.contributions.isEmpty()) {
                        Text("No deposits recorded yet.", style = PocketlyTextStyles.label, color = colors.inkMuted)
                    } else {
                        holding.contributions.sortedByDescending { it.dayKey }.forEachIndexed { idx, c ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(if (c.amountCents >= 0L) "Deposit" else "Withdrawal", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                                    Text(DateUtils.formatShortDate(c.dayKey), style = PocketlyTextStyles.caption, color = colors.inkMuted)
                                }
                                Text(
                                    text = MoneyMath.formatCents(c.amountCents, currency, showPlus = true),
                                    style = PocketlyTextStyles.rowAmount,
                                    color = if (c.amountCents >= 0L) colors.mint else colors.coral
                                )
                            }
                            if (idx < holding.contributions.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                            }
                        }
                    }
                }
            }
        }
    }

    activeSheetMode?.let { mode ->
        HoldingActionSheet(
            mode = mode,
            holding = holding,
            currentValueCents = valuation.currentValueCents,
            currencyCode = currency,
            onDismiss = { activeSheetMode = null },
            onDepositOrWithdraw = { signed, dayKey ->
                onDepositOrWithdraw(holding.id, signed, dayKey)
                activeSheetMode = null
            },
            onEditRate = { rate ->
                onEditRate(holding, rate)
                activeSheetMode = null
            },
            onUpdateValue = { cents ->
                onUpdateManualValue(holding.id, cents)
                activeSheetMode = null
            }
        )
    }

    if (showArchiveConfirm) {
        PocketlyDeleteConfirmSheet(
            title = "Archive investment",
            message = "Archive \"${holding.name}\"? It will be hidden from your active holdings and total invested value.",
            confirmLabel = "Archive investment",
            onConfirm = { onArchive(holding) },
            onDismiss = { showArchiveConfirm = false },
            buttonTestTag = "confirm_archive_holding"
        )
    }
}

@Composable
private fun HoldingActionSheet(
    mode: String,
    holding: Holding,
    currentValueCents: Long,
    currencyCode: String,
    onDismiss: () -> Unit,
    onDepositOrWithdraw: (Long, String) -> Unit,
    onEditRate: (Double) -> Unit,
    onUpdateValue: (Long) -> Unit
) {
    val todayKey = remember { DateUtils.today().toDayKey() }
    var amountStr by remember(mode) {
        mutableStateOf(if (mode == "value") MoneyMath.centsToKeypadString(currentValueCents) else "0")
    }
    var rateText by remember(mode) {
        mutableStateOf(String.format(Locale.US, "%.1f", (holding.apy ?: 0.045) * 100.0))
    }
    var dateKey by remember { mutableStateOf(todayKey) }
    var showDateSheet by remember { mutableStateOf(false) }

    val title = when (mode) {
        "deposit" -> "Add deposit"
        "withdraw" -> "Withdraw"
        "rate" -> "Edit yearly APY"
        else -> "Update value"
    }
    val buttonLabel = when (mode) {
        "deposit" -> "Add deposit"
        "withdraw" -> "Log withdrawal"
        "rate" -> "Save rate"
        else -> "Save value"
    }

    val amountCents = MoneyMath.parseKeypadInputToCents(amountStr)
    val parsedRate = rateText.toDoubleOrNull()
    val rateInvalid = mode == "rate" && (parsedRate == null || parsedRate < 0.0 || parsedRate > 20.0)
    val canSave = when (mode) {
        "rate" -> !rateInvalid
        "value" -> true
        else -> amountCents > 0L
    }

    PocketlyBottomSheet(title = title, onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (mode == "rate") {
                SheetGroupedCard {
                    SheetPercentStepperRow(
                        icon = Icons.Rounded.Percent,
                        label = "Yearly APY",
                        percentText = rateText,
                        onPercentTextChange = { rateText = it },
                        onStep = { d ->
                            val next = ((rateText.toDoubleOrNull() ?: 0.0) + d).coerceIn(0.0, 20.0)
                            rateText = String.format(Locale.US, "%.1f", next)
                        },
                        error = if (rateInvalid) "APY must be between 0% and 20%" else null
                    )
                }
                Spacer(Modifier.height(20.dp))
            } else {
                HeroAmountDisplay(
                    amountStr = amountStr,
                    currencyCode = currencyCode,
                    isActive = true,
                    onClick = {}
                )
                if (mode == "deposit" || mode == "withdraw") {
                    Spacer(Modifier.height(10.dp))
                    SheetGroupedCard {
                        SheetPickerRow(
                            icon = Icons.Rounded.CalendarToday,
                            label = "Date",
                            value = DateUtils.formatDayHeader(dateKey),
                            onClick = { showDateSheet = true }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                NumericAmountKeypad(
                    onDigit = { amountStr = appendKeypadDigit(amountStr, it) },
                    onDecimal = { if (!amountStr.contains(".")) amountStr = "$amountStr." },
                    onBackspace = { amountStr = if (amountStr.length <= 1) "0" else amountStr.dropLast(1) }
                )
                Spacer(Modifier.height(16.dp))
            }

            MintRippleButton(
                text = buttonLabel,
                enabled = canSave,
                onClick = {
                    when (mode) {
                        "deposit" -> onDepositOrWithdraw(amountCents, dateKey)
                        "withdraw" -> onDepositOrWithdraw(-amountCents, dateKey)
                        "rate" -> onEditRate((parsedRate ?: 0.0).coerceIn(0.0, 20.0))
                        else -> onUpdateValue(amountCents)
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showDateSheet) {
        PocketlyCalendarSheet(
            title = "Date",
            selectedDayKey = dateKey,
            allowNone = false,
            onSelect = { if (it != null) dateKey = it },
            onDismiss = { showDateSheet = false }
        )
    }
}

@Composable
fun GrowthCalculatorScreen(
    calc: GrowthCalculatorState,
    currencyCode: String,
    onBack: () -> Unit,
    onUpdateCalc: ((GrowthCalculatorState) -> GrowthCalculatorState) -> Unit,
    onSaveAsInvestment: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val projection = remember(calc) {
        FinanceEngine.calculateGrowth(
            startingCents = calc.startingCents,
            monthlyContributionCents = calc.monthlyContributionCents,
            annualYieldRate = calc.annualYieldPercent / 100.0,
            years = calc.years,
            compounding = calc.compounding
        )
    }

    val putInRatio = if (projection.finalValueCents > 0L) {
        (projection.totalPutInCents.toFloat() / projection.finalValueCents.toFloat()).coerceIn(0.05f, 1f)
    } else 1f
    val animPutInRatio by animateFloatAsState(
        targetValue = putInRatio,
        animationSpec = tween(260),
        label = "calc_bar_ratio"
    )

    val chartPoints = remember(projection) {
        val pts = projection.points
        val step = (pts.size / 48).coerceAtLeast(1)
        pts.filterIndexed { idx, _ -> idx % step == 0 || idx == pts.lastIndex }
            .map { p -> SeriesPoint(dayKey = "M${p.monthIndex}", valueCents = p.valueCents, putInCents = p.putInCents) }
    }
    var lastHapticYear by remember { mutableIntStateOf(-1) }
    var editingField by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        SubScreenTopBar(title = "Growth calculator", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PocketlyCard {
                    AnimatedCurrencyText(
                        targetCents = projection.finalValueCents,
                        currencyCode = currencyCode,
                        style = PocketlyTextStyles.heroNumber,
                        color = colors.ink,
                        dropWholeZeros = true
                    )
                    Text(
                        text = "after ${calc.years} ${if (calc.years == 1) "year" else "years"}",
                        style = PocketlyTextStyles.label,
                        color = colors.inkMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "You put in ${MoneyMath.formatCents(projection.totalPutInCents, currencyCode, dropWholeZeros = true)} · You earn ${MoneyMath.formatCents(projection.totalEarnedCents, currencyCode, dropWholeZeros = true)}",
                        style = PocketlyTextStyles.labelTabular,
                        color = colors.ink
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(colors.mint)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animPutInRatio)
                                .fillMaxHeight()
                                .background(colors.lavenderBar)
                        )
                    }
                }
            }

            item {
                PocketlyCard(contentPadding = PaddingValues(16.dp)) {
                    val defaultHighlight = remember(chartPoints.size) {
                        if (chartPoints.size >= 3) (chartPoints.lastIndex * 0.60f).roundToInt() else null
                    }
                    InvestmentGrowthLineChart(
                        points = chartPoints,
                        currencyCode = currencyCode,
                        height = 204.dp,
                        showPutInSeries = true,
                        showAxisLabels = true,
                        showLegend = true,
                        defaultHighlightIndex = defaultHighlight,
                        putInLineColor = colors.lavenderBar.copy(alpha = 0.80f),
                        valueLegendLabel = "Projected value",
                        putInLegendLabel = "You put in",
                        xLabelFormatter = { pt, _ ->
                            val m = pt.dayKey.removePrefix("M").toIntOrNull() ?: 0
                            val yr = m / 12
                            if (yr == 0) "Now" else "Yr $yr"
                        },
                        calloutLabelFormatter = { pt, _ ->
                            val m = pt.dayKey.removePrefix("M").toIntOrNull() ?: 0
                            val yr = m / 12
                            val remM = m % 12
                            if (remM == 0) "Year $yr" else "Year $yr, ${remM}m"
                        },
                        onScrubIndexChanged = { idx ->
                            val pt = idx?.let { chartPoints.getOrNull(it) }
                            val m = pt?.dayKey?.removePrefix("M")?.toIntOrNull()
                            if (m != null) {
                                lastHapticYear = m / 12
                            }
                        },
                        testTagName = "growth_calculator_chart"
                    )
                }
            }

            item {
                PocketlyCard {
                    CalcSliderRow(
                        label = "Starting amount",
                        valueText = MoneyMath.formatCents(calc.startingCents, currencyCode, dropWholeZeros = true),
                        sliderValue = (calc.startingCents / 100f).coerceIn(0f, 100_000f),
                        valueRange = 0f..100_000f,
                        onValueChange = { v ->
                            val rounded = (v / 100f).roundToLong() * 100L * 100L
                            if (rounded != calc.startingCents) {
                                feedback.scrubTick()
                                onUpdateCalc { it.copy(startingCents = rounded) }
                            }
                        },
                        onTapValue = { editingField = "starting" }
                    )
                    Spacer(Modifier.height(10.dp))
                    CalcSliderRow(
                        label = "Monthly contribution",
                        valueText = MoneyMath.formatCents(calc.monthlyContributionCents, currencyCode, dropWholeZeros = true),
                        sliderValue = (calc.monthlyContributionCents / 100f).coerceIn(0f, 5_000f),
                        valueRange = 0f..5_000f,
                        onValueChange = { v ->
                            val rounded = (v / 25f).roundToLong() * 25L * 100L
                            if (rounded != calc.monthlyContributionCents) {
                                feedback.scrubTick()
                                onUpdateCalc { it.copy(monthlyContributionCents = rounded) }
                            }
                        },
                        onTapValue = { editingField = "monthly" }
                    )
                    Spacer(Modifier.height(10.dp))
                    CalcSliderRow(
                        label = "Yearly yield",
                        valueText = MoneyMath.formatPercent(calc.annualYieldPercent, decimals = 1),
                        sliderValue = calc.annualYieldPercent.toFloat().coerceIn(0f, 20f),
                        valueRange = 0f..20f,
                        onValueChange = { v ->
                            val rounded = (v * 10f).roundToInt() / 10.0
                            if (rounded != calc.annualYieldPercent) {
                                feedback.scrubTick()
                                onUpdateCalc { it.copy(annualYieldPercent = rounded) }
                            }
                        },
                        onTapValue = { editingField = "yield" }
                    )
                    Spacer(Modifier.height(10.dp))
                    CalcSliderRow(
                        label = "Time",
                        valueText = "${calc.years} ${if (calc.years == 1) "year" else "years"}",
                        sliderValue = calc.years.toFloat().coerceIn(1f, 50f),
                        valueRange = 1f..50f,
                        onValueChange = { v ->
                            val yrs = v.roundToInt().coerceIn(1, 50)
                            if (yrs != calc.years) {
                                feedback.scrubTick()
                                onUpdateCalc { it.copy(years = yrs) }
                            }
                        },
                        onTapValue = { editingField = "years" }
                    )
                }
            }

            item {
                PocketlyCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Compounding:", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                        PocketlySpringSegmentedControl(
                            options = listOf("Monthly", "Yearly"),
                            selectedIndex = if (calc.compounding == CompoundingMode.MONTHLY) 0 else 1,
                            onSelect = { idx ->
                                onUpdateCalc {
                                    it.copy(compounding = if (idx == 0) CompoundingMode.MONTHLY else CompoundingMode.YEARLY)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            tagPrefix = "calc_compound"
                        )
                    }
                }
            }

            item {
                MintRippleButton(
                    text = "Save as investment",
                    enabled = true,
                    onClick = onSaveAsInvestment,
                    testTagName = "calc_save_investment"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Estimate, not financial advice.",
                    style = PocketlyTextStyles.caption,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    editingField?.let { field ->
        CalculatorInputSheet(
            field = field,
            calc = calc,
            currencyCode = currencyCode,
            onDismiss = { editingField = null },
            onApply = { updater ->
                onUpdateCalc(updater)
                editingField = null
            }
        )
    }
}

@Composable
private fun CalculatorInputSheet(
    field: String,
    calc: GrowthCalculatorState,
    currencyCode: String,
    onDismiss: () -> Unit,
    onApply: ((GrowthCalculatorState) -> GrowthCalculatorState) -> Unit
) {
    val initial = when (field) {
        "starting" -> MoneyMath.centsToKeypadString(calc.startingCents)
        "monthly" -> MoneyMath.centsToKeypadString(calc.monthlyContributionCents)
        "yield" -> String.format(Locale.US, "%.1f", calc.annualYieldPercent)
        else -> calc.years.toString()
    }
    val title = when (field) {
        "starting" -> "Starting amount"
        "monthly" -> "Monthly contribution"
        "yield" -> "Yearly yield"
        else -> "Time in years"
    }
    var valueStr by remember(field) { mutableStateOf(initial) }
    val suffix = when (field) {
        "yield" -> "%"
        "years" -> " yrs"
        else -> null
    }
    val isYieldInvalid = field == "yield" && ((valueStr.toDoubleOrNull() ?: -1.0) !in 0.0..20.0)
    val isYearsInvalid = field == "years" && ((valueStr.toIntOrNull() ?: 0) !in 1..50)
    val colors = PocketlyTheme.colors

    PocketlyBottomSheet(title = title, onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            HeroAmountDisplay(
                amountStr = valueStr,
                currencyCode = currencyCode,
                isActive = true,
                onClick = {},
                suffixLabel = suffix
            )
            if (isYieldInvalid) {
                Text(
                    text = "APY must be between 0% and 20%",
                    style = PocketlyTextStyles.caption,
                    color = colors.coral,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (isYearsInvalid) {
                Text(
                    text = "Enter between 1 and 50 years",
                    style = PocketlyTextStyles.caption,
                    color = colors.coral,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            Spacer(Modifier.height(12.dp))
            NumericAmountKeypad(
                onDigit = { valueStr = appendKeypadDigit(valueStr, it) },
                onDecimal = {
                    if (field != "years" && !valueStr.contains(".")) valueStr = "$valueStr."
                },
                onBackspace = { valueStr = if (valueStr.length <= 1) "0" else valueStr.dropLast(1) }
            )
            Spacer(Modifier.height(16.dp))
            MintRippleButton(
                text = "Apply",
                enabled = !isYieldInvalid && !isYearsInvalid,
                onClick = {
                    when (field) {
                        "starting" -> {
                            val c = MoneyMath.parseKeypadInputToCents(valueStr)
                            onApply { it.copy(startingCents = c) }
                        }
                        "monthly" -> {
                            val c = MoneyMath.parseKeypadInputToCents(valueStr)
                            onApply { it.copy(monthlyContributionCents = c) }
                        }
                        "yield" -> {
                            val r = (valueStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 20.0)
                            onApply { it.copy(annualYieldPercent = r) }
                        }
                        else -> {
                            val y = (valueStr.toIntOrNull() ?: 10).coerceIn(1, 50)
                            onApply { it.copy(years = y) }
                        }
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CalcSliderRow(
    label: String,
    valueText: String,
    sliderValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onTapValue: () -> Unit
) {
    val colors = PocketlyTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = PocketlyTextStyles.rowTitle, color = colors.ink)
            Text(
                text = valueText,
                style = PocketlyTextStyles.rowAmount,
                color = colors.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface2)
                    .clickable(onClick = onTapValue)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = colors.mint,
                activeTrackColor = colors.mint,
                inactiveTrackColor = colors.lavenderBar.copy(alpha = 0.45f)
            )
        )
    }
}

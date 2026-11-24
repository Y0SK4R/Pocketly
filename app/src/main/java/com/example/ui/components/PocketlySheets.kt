package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DateUtils
import com.example.domain.DayDate
import com.example.domain.MonthKey
import com.example.domain.SupportedCurrencies
import com.example.ui.theme.PocketlyPalette
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar

val LocalActiveSheetCount = compositionLocalOf<MutableIntState?> { null }

/**
 * Shared Pocketly Bottom Sheet shell:
 * - Light: #FFFFFF on 40% black scrim
 * - Dark: #121217 on 60% black scrim with a 1dp #25252F top border and faint top edge highlight
 * - Custom drag handle + Bricolage 600 22sp title + top-right X close button (no Cancel text button)
 * - Notifies LocalActiveSheetCount so floating + button and bottom nav stay hidden behind the scrim
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketlyBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    leadingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val activeSheets = LocalActiveSheetCount.current
    LaunchedEffect(Unit) {
        feedback.sheetOpen()
    }
    DisposableEffect(activeSheets) {
        activeSheets?.let { it.intValue += 1 }
        onDispose {
            activeSheets?.let { it.intValue = (it.intValue - 1).coerceAtLeast(0) }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrimColor = if (colors.isDark) Color.Black.copy(alpha = 0.60f) else Color.Black.copy(alpha = 0.40f)
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = scrimColor,
        shape = sheetShape,
        dragHandle = null
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .then(
                    if (colors.isDark) {
                        Modifier
                            .border(BorderStroke(1.dp, colors.line), sheetShape)
                            .drawWithContent {
                                drawContent()
                                // Faint luminous top highlight so dark sheet clearly lifts off black page
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.16f),
                                            Color.Transparent
                                        )
                                    ),
                                    start = Offset(28.dp.toPx(), 1.5.dp.toPx()),
                                    end = Offset(size.width - 28.dp.toPx(), 1.5.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                    } else Modifier
                )
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF3A3A4A) else Color(0xFFD6D5E2))
                )
            }

            // Header: Title (Bricolage 600, 22sp) + optional leading/trailing + small X top-right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = PocketlyTextStyles.sheetTitle,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (leadingAction != null) {
                    leadingAction()
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sheet_close_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = colors.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            content()
        }
    }
}

/**
 * Full-width 48dp segmented control whose raised pill glides smoothly with spring.soft.
 * Light: raised #FFFFFF pill on #F7F6FB track. Dark: raised #1A1A22 pill on #0A0A0F track.
 */
@Composable
fun PocketlySpringSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    useMintSelected: Boolean = false,
    tagPrefix: String = "sheet_segment"
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val feedback = LocalPocketlyFeedback.current
    val trackColor = if (colors.isDark) Color(0xFF0A0A0F) else colors.surface2
    val pillColor = when {
        useMintSelected -> colors.mintSoft
        colors.isDark -> Color(0xFF1A1A22)
        else -> Color.White
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .then(if (colors.isDark) Modifier.border(1.dp, colors.line, RoundedCornerShape(16.dp)) else Modifier)
            .padding(4.dp)
    ) {
        val count = options.size.coerceAtLeast(1)
        val segmentWidth = maxWidth / count
        val targetOffset = segmentWidth * selectedIndex.coerceIn(0, count - 1)
        val indicatorOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = if (reduceMotion) tween(0) else spring(stiffness = 220f, dampingRatio = 0.78f),
            label = "spring_soft_pill"
        )

        // Sliding raised pill
        Surface(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            color = pillColor,
            shadowElevation = if (colors.isDark || useMintSelected) 0.dp else 2.dp,
            border = if (colors.isDark && !useMintSelected) androidx.compose.foundation.BorderStroke(1.dp, colors.line) else null
        ) {}

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                val txtColor = when {
                    selected && useMintSelected -> colors.mint
                    selected -> colors.ink
                    else -> colors.inkMuted
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!selected) {
                                feedback.tap()
                                onSelect(idx)
                            }
                        }
                        .testTag("${tagPrefix}_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = if (selected) PocketlyTextStyles.chipSelected else PocketlyTextStyles.chip,
                        color = txtColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Giant 56sp tabular hero amount display with blinking mint caret and small muted currency label.
 * Tapping activates or toggles the custom keypad.
 */
@Composable
fun HeroAmountDisplay(
    amountStr: String,
    currencyCode: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNegative: Boolean = false,
    suffixLabel: String? = null
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val currencySymbol = SupportedCurrencies.forCode(currencyCode).symbol
    val isZero = (amountStr.toDoubleOrNull() ?: 0.0) == 0.0

    val caretTransition = rememberInfiniteTransition(label = "hero_caret")
    val caretAlpha by caretTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "hero_caret_alpha"
    )

    val formattedDisplay = remember(amountStr, currencySymbol, isNegative, suffixLabel) {
        if (suffixLabel != null) {
            "$amountStr$suffixLabel"
        } else {
            val parts = amountStr.split(".")
            val whole = parts[0].toLongOrNull() ?: 0L
            val wholeText = DecimalFormat("#,##0").format(whole)
            val sign = if (isNegative && !isZero) "-" else ""
            if (parts.size == 2) "$sign$currencySymbol$wholeText.${parts[1]}" else "$sign$currencySymbol$wholeText"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = formattedDisplay,
                transitionSpec = {
                    if (reduceMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else (slideInVertically(tween(110)) { it / 4 } + fadeIn(tween(110))) togetherWith fadeOut(tween(80))
                },
                label = "hero_amt_roll"
            ) { txt ->
                Text(
                    text = txt,
                    style = PocketlyTextStyles.keypadAmount,
                    color = if (isZero) colors.inkMuted else colors.ink,
                    maxLines = 1
                )
            }
            if (isActive) {
                Spacer(Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(colors.mint.copy(alpha = if (reduceMotion) 0.9f else caretAlpha))
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (suffixLabel != null) "Tap to edit" else currencyCode,
            style = PocketlyTextStyles.caption,
            color = colors.inkMuted
        )
    }
}

/**
 * Grouped surface-2 card (18dp radius, 1dp --line dividers) replacing boxed OutlinedTextFields.
 */
@Composable
fun SheetGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PocketlyTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface2)
            .then(if (colors.isDark) Modifier.border(1.dp, colors.line, RoundedCornerShape(18.dp)) else Modifier),
        content = content
    )
}

@Composable
fun SheetRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(PocketlyTheme.colors.line)
    )
}

/**
 * 56dp minimum height grouped text-input row with left icon, real label, right-aligned text input,
 * 2dp mint focus outline, and inline coral error message below the row.
 */
@Composable
fun SheetTextRow(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    maxLength: Int = 40,
    testTagName: String = "sheet_text_row"
) {
    val colors = PocketlyTheme.colors
    var focused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    when {
                        error != null -> Modifier.border(2.dp, colors.coral, RoundedCornerShape(14.dp))
                        focused -> Modifier.border(2.dp, colors.mint, RoundedCornerShape(14.dp))
                        else -> Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (focused) colors.mint else colors.inkMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = PocketlyTextStyles.label,
                color = colors.inkMuted
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = PocketlyTextStyles.inputValue,
                        color = colors.inkMuted.copy(alpha = 0.60f),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                    textStyle = PocketlyTextStyles.inputValue.copy(
                        color = colors.ink,
                        textAlign = TextAlign.End
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.mint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .testTag(testTagName)
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = PocketlyTextStyles.caption,
                color = colors.coral,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

/**
 * 56dp minimum height picker row with icon left, real label, right-aligned value, and chevron.
 */
@Composable
fun SheetPickerRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    showChevron: Boolean = true,
    valueColor: Color = PocketlyTheme.colors.ink,
    testTagName: String = "sheet_picker_row"
) {
    val colors = PocketlyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (focused) Modifier.border(2.dp, colors.mint, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTagName),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (focused) colors.mint else colors.inkMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = PocketlyTextStyles.label,
            color = colors.inkMuted
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = PocketlyTextStyles.inputValue,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 56dp APY stepper row with - / + buttons (0.1% steps), tap-to-type inline input,
 * 2dp mint focus outline, and inline coral error under the row.
 */
@Composable
fun SheetPercentStepperRow(
    icon: ImageVector,
    label: String,
    percentText: String,
    onPercentTextChange: (String) -> Unit,
    onStep: (Double) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    var focused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    when {
                        error != null -> Modifier.border(2.dp, colors.coral, RoundedCornerShape(14.dp))
                        focused -> Modifier.border(2.dp, colors.mint, RoundedCornerShape(14.dp))
                        else -> Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (focused) colors.mint else colors.inkMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = PocketlyTextStyles.label,
                color = colors.inkMuted,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .then(if (colors.isDark) Modifier.border(1.dp, colors.line, CircleShape) else Modifier)
                        .clickable {
                            feedback.scrubTick()
                            onStep(-0.1)
                        }
                        .testTag("apy_minus_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease APY", tint = colors.ink, modifier = Modifier.size(16.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = percentText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() || it == '.' }.take(5)
                            if (clean.count { it == '.' } <= 1) onPercentTextChange(clean)
                        },
                        textStyle = PocketlyTextStyles.rowAmount.copy(
                            color = colors.ink,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.mint),
                        modifier = Modifier
                            .width(44.dp)
                            .onFocusChanged { focused = it.isFocused }
                            .testTag("apy_input")
                    )
                    Text("%", style = PocketlyTextStyles.rowAmount, color = colors.inkMuted)
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .then(if (colors.isDark) Modifier.border(1.dp, colors.line, CircleShape) else Modifier)
                        .clickable {
                            feedback.scrubTick()
                            onStep(0.1)
                        }
                        .testTag("apy_plus_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase APY", tint = colors.ink, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = PocketlyTextStyles.caption,
                color = colors.coral,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

/**
 * 8 color swatches of 40dp with 12dp gaps; selected has a crisp 3dp ring.
 */
@Composable
fun SheetColorSwatches(
    selectedColorKey: String,
    onSelectColor: (String) -> Unit,
    modifier: Modifier = Modifier,
    colorKeys: List<String> = listOf("orange", "pink", "sky", "lime", "purple", "teal", "clay", "slate")
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(colorKeys) { key ->
            val selected = selectedColorKey == key
            val swatchColor = PocketlyPalette.colorForKey(key)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected) Modifier.border(3.dp, colors.ink, CircleShape) else Modifier
                    )
                    .clickable {
                        feedback.tap()
                        onSelectColor(key)
                    }
                    .padding(if (selected) 5.dp else 0.dp)
                    .testTag("swatch_color_$key"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (selected) 30.dp else 40.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                )
            }
        }
    }
}

/**
 * Row of 6 crisp icons (40dp tiles, 12dp gaps) styled identically to the color swatches.
 */
@Composable
fun SheetIconSwatches(
    iconKeys: List<String>,
    selectedIconKey: String,
    accentColorKey: String,
    onSelectIcon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val accent = PocketlyPalette.colorForKey(accentColorKey)
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(iconKeys) { key ->
            val selected = selectedIconKey == key
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) PocketlyPalette.softBackgroundFor(accent, colors.isDark) else colors.surface2)
                    .then(if (selected) Modifier.border(3.dp, accent, CircleShape) else Modifier)
                    .clickable {
                        feedback.tap()
                        onSelectIcon(key)
                    }
                    .testTag("swatch_icon_$key"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PocketlyIcons.forKey(key),
                    contentDescription = key,
                    tint = if (selected) accent else colors.inkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Wide 56dp fully rounded primary action button with Pocketly's signature short mint ripple.
 */
@Composable
fun MintRippleButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PocketlyTheme.colors.mint,
    testTagName: String = "sheet_primary_button"
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val feedback = LocalPocketlyFeedback.current
    val scope = rememberCoroutineScope()
    val ripple = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .clickable(enabled = enabled) {
                if (containerColor == colors.coral) {
                    feedback.deleteOrReject()
                } else {
                    feedback.confirm()
                }
                scope.launch {
                    if (!reduceMotion) {
                        ripple.snapTo(0f)
                        ripple.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                    }
                    onClick()
                    ripple.snapTo(0f)
                }
            }
            .testTag(testTagName),
        contentAlignment = Alignment.Center
    ) {
        if (ripple.value > 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - ripple.value) * 0.45f),
                    radius = size.width * ripple.value,
                    center = center
                )
            }
        }
        Text(
            text = text,
            style = PocketlyTextStyles.button,
            color = Color.White
        )
    }
}

/**
 * Custom Pocketly Calendar Picker Bottom Sheet (replaces all default DatePickerDialogs).
 * Supports quick Today / Yesterday / optional None pills + interactive month grid.
 */
@Composable
fun PocketlyCalendarSheet(
    title: String,
    selectedDayKey: String?,
    allowNone: Boolean = false,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val today = remember { DateUtils.today() }
    val initialDate = remember(selectedDayKey) {
        selectedDayKey?.let { DateUtils.parseDayKey(it) } ?: today
    }
    var visibleMonth by remember { mutableStateOf(initialDate.toMonthKey()) }

    val daysInMonth = DateUtils.daysInMonth(visibleMonth.year, visibleMonth.month)
    val firstDayOfWeek = remember(visibleMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, visibleMonth.year)
            set(Calendar.MONTH, visibleMonth.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sun
    }
    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")

    PocketlyBottomSheet(title = title, onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Quick chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickDateChip("Today", selectedDayKey == today.toDayKey()) {
                    onSelect(today.toDayKey())
                    onDismiss()
                }
                QuickDateChip("Yesterday", selectedDayKey == today.plusDays(-1).toDayKey()) {
                    onSelect(today.plusDays(-1).toDayKey())
                    onDismiss()
                }
                if (allowNone) {
                    QuickDateChip("None", selectedDayKey == null) {
                        onSelect(null)
                        onDismiss()
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(-1) }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month", tint = colors.ink)
                }
                Text(visibleMonth.shortLabel(), style = PocketlyTextStyles.rowTitle, color = colors.ink)
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month", tint = colors.ink)
                }
            }

            // Weekday labels
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { d ->
                    Text(
                        text = d,
                        style = PocketlyTextStyles.caption,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar day cells
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val cellIdx = r * 7 + c
                            val dayNum = cellIdx - firstDayOfWeek + 1
                            if (dayNum in 1..daysInMonth) {
                                val cellDate = DayDate(visibleMonth.year, visibleMonth.month, dayNum)
                                val cellKey = cellDate.toDayKey()
                                val isSelected = selectedDayKey == cellKey
                                val isToday = cellDate == today
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> colors.mint
                                                isToday -> colors.mintSoft
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            onSelect(cellKey)
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = PocketlyTextStyles.labelTabular,
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> colors.mint
                                            else -> colors.ink
                                        }
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f).height(42.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickDateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = PocketlyTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) colors.mintSoft else colors.surface2)
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
 * Shared compact choice list sheet (used for Lock after, Appearance, Currency, Account pickers, etc.).
 */
data class SheetChoiceItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val badgeText: String? = null,
    val iconKey: String? = null,
    val colorKey: String? = null
)

@Composable
fun SheetSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    checkedTrackColor: Color = PocketlyTheme.colors.mint,
    testTagName: String = "sheet_switch_row"
) {
    val colors = PocketlyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = PocketlyTextStyles.label, color = colors.ink)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, style = PocketlyTextStyles.caption, color = colors.inkMuted)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = checkedTrackColor,
                checkedThumbColor = Color.White
            ),
            modifier = Modifier.testTag(testTagName)
        )
    }
}

@Composable
fun PocketlyChoiceSheet(
    title: String,
    items: List<SheetChoiceItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    PocketlyBottomSheet(title = title, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SheetGroupedCard {
                items.forEachIndexed { idx, item ->
                    val selected = item.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .background(if (selected) colors.mintSoft.copy(alpha = 0.55f) else Color.Transparent)
                            .clickable {
                                feedback.tap()
                                onSelect(item.id)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("choice_${item.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.iconKey != null && item.colorKey != null) {
                            CategoryIconTile(item.iconKey, item.colorKey, size = 36.dp, iconSize = 18.dp)
                            Spacer(Modifier.width(12.dp))
                        } else if (item.badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.mint else colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.badgeText,
                                    style = PocketlyTextStyles.labelTabular,
                                    color = if (selected) Color.White else colors.ink
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = PocketlyTextStyles.rowTitle, color = colors.ink)
                            if (!item.subtitle.isNullOrBlank()) {
                                Text(item.subtitle, style = PocketlyTextStyles.caption, color = colors.inkMuted)
                            }
                        }
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = colors.mint, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (idx < items.lastIndex) SheetRowDivider()
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Shared Pocketly delete / destructive confirmation bottom sheet (replaces all AlertDialog confirmations).
 * Supports both instant single-tap confirmation and typed-word confirmation (e.g. "DELETE").
 */
@Composable
fun PocketlyDeleteConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    requireTypedWord: String? = null,
    inputTestTag: String = "delete_confirm_input",
    buttonTestTag: String = "delete_confirm_button"
) {
    val colors = PocketlyTheme.colors
    var typed by remember { mutableStateOf("") }
    val enabled = requireTypedWord == null || typed.trim() == requireTypedWord

    PocketlyBottomSheet(title = title, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = message,
                style = PocketlyTextStyles.body,
                color = colors.inkMuted
            )
            if (requireTypedWord != null) {
                Spacer(Modifier.height(16.dp))
                SheetGroupedCard {
                    SheetTextRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Confirm",
                        value = typed,
                        onValueChange = { typed = it },
                        placeholder = "Type $requireTypedWord",
                        maxLength = 20,
                        testTagName = inputTestTag
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            MintRippleButton(
                text = confirmLabel,
                enabled = enabled,
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                containerColor = colors.coral,
                testTagName = buttonTestTag
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

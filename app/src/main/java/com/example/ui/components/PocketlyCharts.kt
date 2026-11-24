package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetStatus
import com.example.domain.CashFlowSummary
import com.example.domain.CategorySpendSlice
import com.example.domain.DateUtils
import com.example.domain.MoneyMath
import com.example.domain.SeriesPoint
import com.example.ui.theme.PocketlyPalette
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 9-month grouped bar chart (paired mint income + coral expense bars, rounded top corners,
 * 4 horizontal gridlines, current month emphasized, tap tooltip).
 */
@Composable
fun GroupedCashFlowBarChart(
    months: List<CashFlowSummary>,
    currencyCode: String,
    modifier: Modifier = Modifier,
    height: Dp = 172.dp
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val feedback = LocalPocketlyFeedback.current
    var selectedIdx by remember(months) { mutableStateOf<Int?>(null) }

    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(months, reduceMotion) {
        if (reduceMotion) progress.snapTo(1f)
        else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 560, easing = FastOutSlowInEasing))
        }
    }

    val maxVal = months.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) }?.coerceAtLeast(100L) ?: 100L
    val hasAnyData = months.any { it.incomeCents > 0L || it.expenseCents > 0L }

    if (!hasAnyData) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log income or expenses to see monthly cash flow.",
                style = PocketlyTextStyles.label,
                color = colors.inkMuted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        selectedIdx?.let { idx ->
            months.getOrNull(idx)?.let { sel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sel.monthKey.shortLabel(),
                        style = PocketlyTextStyles.labelTabular,
                        color = colors.ink
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = MoneyMath.formatCents(sel.incomeCents, currencyCode, showPlus = true, dropWholeZeros = true),
                            style = PocketlyTextStyles.captionTabular,
                            color = colors.mint
                        )
                        Text(
                            text = MoneyMath.formatCents(-sel.expenseCents, currencyCode, dropWholeZeros = true),
                            style = PocketlyTextStyles.captionTabular,
                            color = colors.coral
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-axis compact labels (4 steps)
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(height - 22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (step in 3 downTo 0) {
                    val v = (maxVal * step) / 3L
                    Text(
                        text = if (step == 0) "0" else MoneyMath.formatCompactAxis(v, currencyCode),
                        style = PocketlyTextStyles.captionTabular,
                        color = colors.inkMuted,
                        maxLines = 1
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height - 22.dp)
                        .pointerInput(months) {
                            detectTapGestures { offset ->
                                if (months.isNotEmpty()) {
                                    val slot = size.width / months.size.toFloat()
                                    val idx = (offset.x / slot).toInt().coerceIn(0, months.lastIndex)
                                    selectedIdx = if (selectedIdx == idx) null else idx
                                    feedback.chartPointTap()
                                }
                            }
                        }
                        .semantics {
                            contentDescription = "Cash flow bar chart across ${months.size} months"
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    // 4 horizontal gridlines
                    for (i in 0..3) {
                        val y = h - (h * i / 3f)
                        drawLine(
                            color = colors.line,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val slotW = w / months.size.toFloat()
                    val barW = (slotW * 0.30f).coerceIn(5.dp.toPx(), 14.dp.toPx())
                    val gap = 3.dp.toPx()
                    val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                    months.forEachIndexed { idx, summary ->
                        val isCurrent = idx == months.lastIndex
                        val isSelected = selectedIdx == idx
                        val alpha = when {
                            selectedIdx != null -> if (isSelected) 1f else 0.45f
                            isCurrent -> 1f
                            else -> 0.80f
                        }
                        val centerX = slotW * idx + slotW / 2f
                        val stagger = ((progress.value * 1.25f) - (idx * 0.03f)).coerceIn(0f, 1f)

                        val incH = (summary.incomeCents.toFloat() / maxVal.toFloat()) * (h - 6.dp.toPx()) * stagger
                        val expH = (summary.expenseCents.toFloat() / maxVal.toFloat()) * (h - 6.dp.toPx()) * stagger

                        if (incH > 1f) {
                            drawRoundRect(
                                color = colors.mint.copy(alpha = alpha),
                                topLeft = Offset(centerX - barW - gap / 2f, h - incH),
                                size = Size(barW, incH),
                                cornerRadius = radius
                            )
                        }
                        if (expH > 1f) {
                            drawRoundRect(
                                color = colors.coral.copy(alpha = alpha),
                                topLeft = Offset(centerX + gap / 2f, h - expH),
                                size = Size(barW, expH),
                                cornerRadius = radius
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    months.forEachIndexed { idx, summary ->
                        val showLabel = months.size <= 6 || idx % 2 == 0 || idx == months.lastIndex
                        Text(
                            text = if (showLabel) summary.monthKey.shortMonthOnly() else "",
                            style = PocketlyTextStyles.caption,
                            color = if (idx == months.lastIndex) colors.ink else colors.inkMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smooth monotone cubic area line chart with optional secondary dashed "Money put in" series
 * and interactive drag/tap scrub tooltip.
 */
@Composable
fun SmoothAreaLineChart(
    points: List<SeriesPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier,
    height: Dp = 152.dp,
    showPutInSeries: Boolean = false,
    lineColor: Color = PocketlyTheme.colors.mint,
    labelFormatter: ((SeriesPoint, Int) -> String)? = null,
    onScrubIndexChanged: ((Int?) -> Unit)? = null
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val feedback = LocalPocketlyFeedback.current
    var scrubIndex by remember(points) { mutableStateOf<Int?>(null) }
    val tapRipple = remember { Animatable(1f) }
    LaunchedEffect(scrubIndex, reduceMotion) {
        if (scrubIndex != null && !reduceMotion) {
            tapRipple.snapTo(0f)
            tapRipple.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        } else {
            tapRipple.snapTo(1f)
        }
    }

    val drawProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(points.size, reduceMotion) {
        if (reduceMotion) drawProgress.snapTo(1f)
        else {
            drawProgress.snapTo(0f)
            drawProgress.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        }
    }

    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not enough history to draw a trend yet.",
                style = PocketlyTextStyles.label,
                color = colors.inkMuted
            )
        }
        return
    }

    val allVals = points.flatMap { p ->
        if (showPutInSeries) listOf(p.valueCents, p.putInCents) else listOf(p.valueCents)
    }
    val rawMin = allVals.minOrNull() ?: 0L
    val rawMax = allVals.maxOrNull() ?: 100L
    val pad = ((rawMax - rawMin).coerceAtLeast(100L) * 0.12).toLong()
    val minVal = (rawMin - pad).coerceAtMost(rawMin)
    val maxVal = (rawMax + pad).coerceAtLeast(minVal + 100L)
    val range = (maxVal - minVal).toFloat().coerceAtLeast(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Floating scrub tooltip above chart
        val activePoint = scrubIndex?.let { points.getOrNull(it) }
        if (activePoint != null) {
            val idx = scrubIndex ?: 0
            val title = labelFormatter?.invoke(activePoint, idx)
                ?: DateUtils.formatShortDate(activePoint.dayKey)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = PocketlyTextStyles.caption,
                    color = colors.inkMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = MoneyMath.formatCents(activePoint.valueCents, currencyCode, dropWholeZeros = true),
                        style = PocketlyTextStyles.labelTabular,
                        color = lineColor
                    )
                    if (showPutInSeries) {
                        Text(
                            text = "Put in ${MoneyMath.formatCents(activePoint.putInCents, currencyCode, dropWholeZeros = true)}",
                            style = PocketlyTextStyles.captionTabular,
                            color = colors.inkMuted
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val idx = ((offset.x / size.width) * (points.lastIndex)).roundToInt()
                                .coerceIn(0, points.lastIndex)
                            scrubIndex = if (scrubIndex == idx) null else idx
                            onScrubIndexChanged?.invoke(scrubIndex)
                            feedback.chartPointTap()
                        }
                    )
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val idx = ((offset.x / size.width) * points.lastIndex).roundToInt()
                                .coerceIn(0, points.lastIndex)
                            scrubIndex = idx
                            onScrubIndexChanged?.invoke(idx)
                            feedback.chartPointTap()
                        },
                        onDragEnd = {
                            scrubIndex = null
                            onScrubIndexChanged?.invoke(null)
                        },
                        onDragCancel = {
                            scrubIndex = null
                            onScrubIndexChanged?.invoke(null)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val idx = ((change.position.x / size.width) * points.lastIndex).roundToInt()
                                .coerceIn(0, points.lastIndex)
                            if (idx != scrubIndex) {
                                scrubIndex = idx
                                onScrubIndexChanged?.invoke(idx)
                                feedback.scrubTick()
                            }
                        }
                    )
                }
                .semantics {
                    contentDescription = "Trend line chart with ${points.size} points"
                }
        ) {
            val w = size.width
            val h = size.height
            val topPad = 10.dp.toPx()
            val bottomPad = 10.dp.toPx()
            val chartH = (h - topPad - bottomPad).coerceAtLeast(10f)

            fun xAt(i: Int): Float = (i.toFloat() / points.lastIndex.toFloat()) * w
            fun yAt(v: Long): Float = topPad + chartH - ((v - minVal).toFloat() / range) * chartH

            val visibleCount = (points.size * drawProgress.value).roundToInt().coerceIn(2, points.size)
            val mainOffsets = (0 until visibleCount).map { i -> Offset(xAt(i), yAt(points[i].valueCents)) }

            val linePath = buildSmoothPath(mainOffsets)
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(mainOffsets.last().x, h)
                lineTo(mainOffsets.first().x, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0.0f)),
                    startY = topPad,
                    endY = h
                )
            )

            // Secondary dashed "Money put in" line
            if (showPutInSeries) {
                val putInOffsets = (0 until visibleCount).map { i -> Offset(xAt(i), yAt(points[i].putInCents)) }
                val putInPath = Path().apply {
                    moveTo(putInOffsets.first().x, putInOffsets.first().y)
                    for (i in 1 until putInOffsets.size) {
                        lineTo(putInOffsets[i].x, putInOffsets[i].y)
                    }
                }
                drawPath(
                    path = putInPath,
                    color = colors.inkMuted.copy(alpha = 0.65f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }

            // Main smooth curve
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active scrub marker OR latest dot
            val highlightIdx = scrubIndex ?: (visibleCount - 1)
            val hx = xAt(highlightIdx)
            val hy = yAt(points[highlightIdx].valueCents)

            if (scrubIndex != null) {
                drawLine(
                    color = colors.line,
                    start = Offset(hx, 0f),
                    end = Offset(hx, h),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            if (scrubIndex != null && tapRipple.value < 1f) {
                val r = 6.dp.toPx() + 20.dp.toPx() * tapRipple.value
                val a = (1f - tapRipple.value).coerceIn(0f, 1f)
                drawCircle(color = lineColor.copy(alpha = a * 0.22f), radius = r, center = Offset(hx, hy))
                drawCircle(
                    color = lineColor.copy(alpha = a * 0.72f),
                    radius = r,
                    center = Offset(hx, hy),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
            drawCircle(color = colors.surface, radius = 6.dp.toPx(), center = Offset(hx, hy))
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(hx, hy))
        }
    }
}

private fun buildSmoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts.first().x, pts.first().y)
    for (i in 0 until pts.lastIndex) {
        val p0 = pts[i]
        val p1 = pts[i + 1]
        val midX = (p0.x + p1.x) / 2f
        path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
    }
    return path
}

/**
 * Expense Donut chart with 2-degree gaps, clockwise sweep animation, interactive slice selection,
 * right-side icon bubbles + percentages (collision-free), and dot legend below.
 */
@Composable
fun ExpenseDonutChart(
    slices: List<CategorySpendSlice>,
    currencyCode: String,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val feedback = LocalPocketlyFeedback.current

    val sweepAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(slices, reduceMotion) {
        if (reduceMotion) sweepAnim.snapTo(1f)
        else {
            sweepAnim.snapTo(0f)
            sweepAnim.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        }
    }

    if (slices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(168.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses logged for this period.",
                style = PocketlyTextStyles.label,
                color = colors.inkMuted
            )
        }
        return
    }

    // Group beyond top 5 into "Other" so labels never crowd
    val displaySlices = remember(slices) {
        if (slices.size <= 6) slices
        else {
            val top = slices.take(5)
            val restSpent = slices.drop(5).sumOf { it.spentCents }
            val restPct = slices.drop(5).sumOf { it.sharePercent }
            top + slices[5].copy(
                category = slices[5].category.copy(id = "__other__", name = "Other", icon = "dots", color = "slate"),
                spentCents = restSpent,
                sharePercent = restPct
            )
        }
    }

    val activeSlice = displaySlices.firstOrNull { it.category.id == selectedCategoryId }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Interactive Donut Ring
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displaySlices) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dist = hypot(offset.x - cx, offset.y - cy)
                                val outerR = size.width / 2f
                                val innerR = outerR * 0.45f
                                if (dist in innerR..outerR) {
                                    var deg = Math.toDegrees(atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())).toFloat()
                                    deg = (deg + 90f + 360f) % 360f
                                    var acc = 0f
                                    for (s in displaySlices) {
                                        val span = (s.sharePercent.toFloat() / 100f) * 360f
                                        if (deg in acc..(acc + span)) {
                                            val id = s.category.id
                                            if (id != "__other__") {
                                                feedback.chartPointTap()
                                                onSelectCategory(if (selectedCategoryId == id) null else id)
                                            }
                                            break
                                        }
                                        acc += span
                                    }
                                } else {
                                    feedback.tap()
                                    onSelectCategory(null)
                                }
                            }
                        }
                        .semantics {
                            contentDescription = "Expense breakdown donut across ${displaySlices.size} categories"
                        }
                ) {
                    val strokeW = 28.dp.toPx()
                    val inset = strokeW / 2f + 4.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    var startAngle = -90f
                    val gapDeg = if (displaySlices.size > 1) 2.5f else 0f

                    displaySlices.forEach { slice ->
                        val sliceSweep = ((slice.sharePercent.toFloat() / 100f) * 360f * sweepAnim.value - gapDeg).coerceAtLeast(1f)
                        val sliceColor = PocketlyPalette.colorForKey(slice.category.color)
                        val dimmed = selectedCategoryId != null && selectedCategoryId != slice.category.id
                        drawArc(
                            color = sliceColor.copy(alpha = if (dimmed) 0.30f else 0.92f),
                            startAngle = startAngle,
                            sweepAngle = sliceSweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = if (selectedCategoryId == slice.category.id) strokeW + 4.dp.toPx() else strokeW)
                        )
                        startAngle += sliceSweep + gapDeg
                    }
                }

                // Center callout when a slice is tapped
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (activeSlice != null) {
                        Text(
                            text = activeSlice.category.name,
                            style = PocketlyTextStyles.caption,
                            color = colors.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = MoneyMath.formatCents(activeSlice.spentCents, currencyCode, dropWholeZeros = true),
                            style = PocketlyTextStyles.labelTabular,
                            color = colors.ink,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "${displaySlices.size} cats",
                            style = PocketlyTextStyles.caption,
                            color = colors.inkMuted
                        )
                    }
                }
            }

            // Right: External icon bubbles + percentage breakdown (matches Overview mockup)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displaySlices.forEach { slice ->
                    val isSelected = selectedCategoryId == slice.category.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.surface2 else Color.Transparent)
                            .clickable(enabled = slice.category.id != "__other__") {
                                onSelectCategory(if (isSelected) null else slice.category.id)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIconTile(
                            iconKey = slice.category.icon,
                            colorKey = slice.category.color,
                            size = 26.dp,
                            iconSize = 15.dp,
                            circular = true
                        )
                        Text(
                            text = MoneyMath.formatPercent(slice.sharePercent, decimals = 1),
                            style = PocketlyTextStyles.labelTabular,
                            color = colors.ink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Clean dot legend row wrapping into 2 compact rows
        val rows = displaySlices.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowSlices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSlices.forEach { slice ->
                        val tint = PocketlyPalette.colorForKey(slice.category.color)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = slice.category.id != "__other__") {
                                    val id = slice.category.id
                                    onSelectCategory(if (selectedCategoryId == id) null else id)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(tint)
                            )
                            Text(
                                text = slice.category.name,
                                style = PocketlyTextStyles.caption,
                                color = colors.inkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(3 - rowSlices.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * 56dp Circular progress ring used in Home's top category strip.
 */
@Composable
fun CategoryProgressRing(
    label: String,
    iconKey: String,
    colorKey: String,
    progress: Float,
    selected: Boolean,
    isOverBudget: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val animatedSweep by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f) * 360f,
        animationSpec = if (reduceMotion) tween(0) else tween(550, easing = FastOutSlowInEasing),
        label = "ring_sweep"
    )
    val accent = when {
        isOverBudget -> colors.coral
        colorKey == "mint" -> colors.mint
        else -> PocketlyPalette.colorForKey(colorKey)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = if (selected) 4.dp.toPx() else 3.dp.toPx()
                val inset = strokeW / 2f
                val s = Size(size.width - strokeW, size.height - strokeW)
                drawArc(
                    color = colors.line,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = s,
                    style = Stroke(width = strokeW)
                )
                if (animatedSweep > 0.5f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = s,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }
            CategoryIconTile(
                iconKey = iconKey,
                colorKey = if (isOverBudget) "coral" else colorKey,
                size = 42.dp,
                iconSize = 20.dp,
                circular = true
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = if (selected) PocketlyTextStyles.navLabelSelected else PocketlyTextStyles.navLabel,
            color = if (selected) colors.ink else colors.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Pocketly's signature circular glass Liquid Budget Gauge.
 * Two phase-shifted animated waves rise smoothly to the saved percentage (mint when under budget,
 * coral when over budget) with a soft glass specular highlight.
 */
@Composable
fun LiquidBudgetGauge(
    status: BudgetStatus?,
    currencyCode: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 224.dp
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion

    val isOver = status?.isOverBudget == true
    val targetFill = when {
        status == null || status.budgetCents <= 0L -> 0.25f
        isOver -> 0.34f
        else -> (status.savedCents.toFloat() / status.budgetCents.toFloat()).coerceIn(0.14f, 0.86f)
    }

    val animatedFill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = if (reduceMotion) tween(0) else spring(stiffness = 220f, dampingRatio = 0.78f),
        label = "liquid_level"
    )

    val infinite = rememberInfiniteTransition(label = "liquid_wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val fluidMain = if (isOver) colors.coral else colors.mint
    val fluidBack = if (isOver) colors.coral.copy(alpha = 0.42f) else colors.mint.copy(alpha = 0.38f)

    Box(
        modifier = modifier
            .size(diameter)
            .semantics {
                contentDescription = if (status != null) {
                    "${status.gaugeHeadline(currencyCode)} ${status.gaugePrimaryValue(currencyCode)} ${status.gaugeSubtitle(currencyCode)}"
                } else {
                    "No overall monthly budget set"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val circlePath = Path().apply {
                addOval(Rect(0f, 0f, w, h))
            }

            // Glass background sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (colors.isDark) {
                        listOf(Color(0xFF1A1D24), Color(0xFF0E1015))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFEFF6F4))
                    },
                    center = Offset(w * 0.38f, h * 0.30f),
                    radius = w * 0.72f
                )
            )

            // Animated dual-wave liquid clipped to glass sphere
            clipPath(circlePath) {
                val baseY = h * (1f - animatedFill)
                val amp1 = h * 0.036f
                val amp2 = h * 0.028f

                // Back wave (phase shifted)
                val backWave = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, baseY)
                    var x = 0f
                    while (x <= w) {
                        val y = baseY + sin((x / w) * 2.0 * PI + phase + 1.8).toFloat() * amp2 - amp2 * 0.3f
                        lineTo(x, y)
                        x += w / 32f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(backWave, color = fluidBack)

                // Front wave
                val frontWave = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, baseY)
                    var x = 0f
                    while (x <= w) {
                        val y = baseY + sin((x / w) * 2.0 * PI - phase).toFloat() * amp1
                        lineTo(x, y)
                        x += w / 32f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = frontWave,
                    brush = Brush.verticalGradient(
                        colors = listOf(fluidMain.copy(alpha = 0.78f), fluidMain),
                        startY = baseY - amp1,
                        endY = h
                    )
                )

                // Subtle specular glass arc highlight top-left
                drawArc(
                    color = Color.White.copy(alpha = if (colors.isDark) 0.14f else 0.70f),
                    startAngle = 205f,
                    sweepAngle = 72f,
                    useCenter = false,
                    topLeft = Offset(w * 0.09f, h * 0.09f),
                    size = Size(w * 0.82f, h * 0.82f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Glass outer rim
            drawCircle(
                color = if (colors.isDark) colors.line else Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = fluidMain.copy(alpha = 0.18f),
                radius = w / 2f - 1.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Centered plain-language gauge readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (status == null || status.budgetCents <= 0L) {
                Text(
                    text = "Set an overall budget",
                    style = PocketlyTextStyles.label,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Track pace",
                    style = PocketlyTextStyles.sectionTitle,
                    color = colors.ink,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = status.gaugeHeadline(currencyCode),
                    style = PocketlyTextStyles.caption,
                    color = if (isOver) colors.coral else colors.inkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                AnimatedCurrencyText(
                    targetCents = abs(status.savedCents),
                    currencyCode = currencyCode,
                    style = PocketlyTextStyles.gaugeHeroNumber,
                    color = colors.ink
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = status.gaugeSubtitle(currencyCode),
                    style = PocketlyTextStyles.caption,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Compact "Spent vs Ideal Pace" line chart beneath the Budgets liquid gauge.
 */
@Composable
fun BudgetPaceLineChart(
    pacePoints: List<Pair<Long, Long>>, // (spentCents or -1, idealPaceCents)
    budgetCents: Long,
    isOverBudget: Boolean,
    currencyCode: String,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp
) {
    val colors = PocketlyTheme.colors
    if (pacePoints.size < 2 || budgetCents <= 0L) return

    val maxVal = maxOf(
        budgetCents,
        pacePoints.maxOfOrNull { maxOf(it.first.coerceAtLeast(0L), it.second) } ?: budgetCents
    ).coerceAtLeast(100L)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = "Spent versus ideal budget pace chart" }
        ) {
            val w = size.width
            val h = size.height
            val padY = 8.dp.toPx()
            val chartH = h - padY * 2

            fun xAt(i: Int): Float = (i.toFloat() / pacePoints.lastIndex.toFloat()) * w
            fun yAt(v: Long): Float = padY + chartH - (v.toFloat() / maxVal.toFloat()) * chartH

            // Budget ceiling line
            val ceilingY = yAt(budgetCents)
            drawLine(
                color = colors.line,
                start = Offset(0f, ceilingY),
                end = Offset(w, ceilingY),
                strokeWidth = 1.dp.toPx()
            )

            // Ideal pace dashed diagonal
            val idealPath = Path().apply {
                moveTo(xAt(0), yAt(pacePoints.first().second))
                lineTo(xAt(pacePoints.lastIndex), yAt(pacePoints.last().second))
            }
            drawPath(
                path = idealPath,
                color = colors.inkMuted.copy(alpha = 0.55f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            )

            // Actual spent line
            val spentPts = pacePoints.mapIndexedNotNull { idx, pair ->
                if (pair.first >= 0L) Offset(xAt(idx), yAt(pair.first)) else null
            }
            if (spentPts.size >= 2) {
                val spentPath = buildSmoothPath(spentPts)
                val lineCol = if (isOverBudget) colors.coral else colors.mint
                val area = Path().apply {
                    addPath(spentPath)
                    lineTo(spentPts.last().x, h)
                    lineTo(spentPts.first().x, h)
                    close()
                }
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineCol.copy(alpha = 0.18f), Color.Transparent),
                        startY = padY,
                        endY = h
                    )
                )
                drawPath(
                    path = spentPath,
                    color = lineCol,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(color = colors.surface, radius = 5.dp.toPx(), center = spentPts.last())
                drawCircle(color = lineCol, radius = 3.5.dp.toPx(), center = spentPts.last())
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(10.dp, 3.dp).clip(CircleShape).background(if (isOverBudget) colors.coral else colors.mint))
                    Text("Spent", style = PocketlyTextStyles.caption, color = colors.inkMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(10.dp, 2.dp).background(colors.inkMuted.copy(alpha = 0.6f)))
                    Text("Ideal pace", style = PocketlyTextStyles.caption, color = colors.inkMuted)
                }
            }
            Text(
                text = "Budget ${MoneyMath.formatCents(budgetCents, currencyCode, dropWholeZeros = true)}",
                style = PocketlyTextStyles.captionTabular,
                color = colors.inkMuted
            )
        }
    }
}

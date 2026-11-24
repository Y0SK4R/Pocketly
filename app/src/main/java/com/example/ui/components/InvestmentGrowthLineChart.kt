package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.domain.DateUtils
import com.example.domain.MoneyMath
import com.example.domain.SeriesPoint
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

private data class ChartRippleWave(
    val id: Long,
    val pointIndex: Int,
    val startNanos: Long,
    val isTapBurst: Boolean
)

/**
 * Dedicated Canvas-based investment growth line chart tailored for Pocketly's light & dark themes.
 *
 * Features:
 * - Fritsch-Carlson monotone cubic Hermite interpolation (smooth, zero overshoot)
 * - Continuous left-to-right clip reveal animation (respects PocketlyTheme.reduceMotion)
 * - Optional dashed "Money put in" principal baseline with theme-consistent legend
 * - Multi-ring custom Canvas ripple waves on data-point tap and horizontal scrub
 * - Coordinated multi-tier haptic detents & subtle global sound effects via [LocalPocketlyFeedback]
 * - Interactive tap & drag scrubbing with an anchored floating callout pill above the active point
 */
@Composable
fun InvestmentGrowthLineChart(
    points: List<SeriesPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier,
    height: Dp = 188.dp,
    showPutInSeries: Boolean = true,
    showAxisLabels: Boolean = true,
    showLegend: Boolean = true,
    defaultHighlightIndex: Int? = null,
    lineColor: Color = PocketlyTheme.colors.mint,
    putInLineColor: Color = PocketlyTheme.colors.inkMuted.copy(alpha = 0.65f),
    valueLegendLabel: String = "Value",
    putInLegendLabel: String = "Money put in",
    xLabelFormatter: ((SeriesPoint, Int) -> String)? = null,
    calloutLabelFormatter: ((SeriesPoint, Int) -> String)? = null,
    onScrubIndexChanged: ((Int?) -> Unit)? = null,
    testTagName: String = "investment_growth_chart"
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val feedback = LocalPocketlyFeedback.current
    val density = LocalDensity.current

    var scrubIndex by remember(points) { mutableStateOf<Int?>(null) }
    var isDragging by remember(points) { mutableStateOf(false) }
    var calloutWidthPx by remember { mutableIntStateOf(0) }

    // Custom Canvas ripple pool (runs a frame loop ONLY while active ripples exist)
    val ripples = remember(points) { mutableStateListOf<ChartRippleWave>() }
    var nextRippleId by remember { mutableLongStateOf(1L) }
    var frameNanos by remember { mutableLongStateOf(System.nanoTime()) }

    fun spawnRipple(pointIdx: Int, isTapBurst: Boolean) {
        if (reduceMotion) return
        val now = System.nanoTime()
        frameNanos = now
        if (ripples.size >= 6) {
            ripples.removeAt(0)
        }
        ripples.add(
            ChartRippleWave(
                id = nextRippleId++,
                pointIndex = pointIdx,
                startNanos = now,
                isTapBurst = isTapBurst
            )
        )
    }

    LaunchedEffect(ripples.size, reduceMotion) {
        if (reduceMotion) {
            ripples.clear()
            return@LaunchedEffect
        }
        while (ripples.isNotEmpty()) {
            withFrameNanos { now ->
                frameNanos = now
                ripples.removeAll { wave ->
                    val durNs = if (wave.isTapBurst) 460_000_000L else 300_000_000L
                    (now - wave.startNanos) >= durNs
                }
            }
        }
    }

    val clampedDefaultIdx = defaultHighlightIndex?.takeIf { points.isNotEmpty() }
        ?.coerceIn(0, points.lastIndex)
    val activeIdx = scrubIndex ?: clampedDefaultIdx

    // Spring pop on the active marker dot while interacting
    val markerPopScale by animateFloatAsState(
        targetValue = if (isDragging || scrubIndex != null) 1.24f else 1f,
        animationSpec = if (reduceMotion) tween(0) else spring(stiffness = 520f, dampingRatio = 0.56f),
        label = "growth_marker_pop"
    )

    // Continuous smooth horizontal reveal when the series changes (e.g. 1M -> 6M -> 1Y -> All)
    val revealProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val seriesKey = remember(points) {
        Triple(points.size, points.firstOrNull()?.dayKey, points.lastOrNull()?.valueCents)
    }
    LaunchedEffect(seriesKey, reduceMotion) {
        if (reduceMotion) {
            revealProgress.snapTo(1f)
        } else {
            revealProgress.snapTo(0f)
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            )
        }
    }

    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .testTag(testTagName),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add a holding or wait for history to visualize growth over time.",
                style = PocketlyTextStyles.label,
                color = colors.inkMuted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val (minVal, maxVal) = remember(points, showPutInSeries) {
        val values = points.flatMap { p ->
            if (showPutInSeries) listOf(p.valueCents, p.putInCents) else listOf(p.valueCents)
        }
        val rawMin = values.minOrNull() ?: 0L
        val rawMax = values.maxOrNull() ?: 100L
        val span = (rawMax - rawMin).coerceAtLeast(100L)
        val padBottom = (span * 0.14).toLong()
        val padTop = (span * 0.18).toLong()
        val minBound = (rawMin - padBottom).coerceAtLeast(0L).coerceAtMost(rawMin)
        val maxBound = (rawMax + padTop).coerceAtLeast(minBound + 100L)
        minBound to maxBound
    }
    val valueRange = (maxVal - minVal).toFloat().coerceAtLeast(1f)

    val startVal = points.first().valueCents
    val endVal = points.last().valueCents
    val netChange = endVal - startVal
    val accessibilitySummary = remember(startVal, endVal, netChange, currencyCode, points.size) {
        "Investment growth chart with ${points.size} points, from " +
            "${MoneyMath.formatCents(startVal, currencyCode, dropWholeZeros = true)} to " +
            "${MoneyMath.formatCents(endVal, currencyCode, dropWholeZeros = true)}, " +
            "change ${MoneyMath.formatCents(netChange, currencyCode, showPlus = true, dropWholeZeros = true)}"
    }

    val targetFraction = activeIdx?.let { it.toFloat() / points.lastIndex.toFloat() } ?: 1f
    val animatedXFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (reduceMotion || isDragging) {
            tween(durationMillis = 0)
        } else {
            spring(stiffness = 280f, dampingRatio = 0.80f)
        },
        label = "growth_marker_x"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTagName)
    ) {
        val yAxisWidth = if (showAxisLabels) 52.dp else 0.dp
        val xAxisHeight = if (showAxisLabels) 24.dp else 0.dp
        val calloutReserveHeight = 48.dp
        val plotCanvasHeight = (height - xAxisHeight).coerceAtLeast(108.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left compact Y-axis ticks (aligned with the 3 horizontal gridlines)
            if (showAxisLabels) {
                Column(
                    modifier = Modifier
                        .width(yAxisWidth)
                        .height(plotCanvasHeight)
                        .padding(top = calloutReserveHeight, bottom = 10.dp, end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val midVal = minVal + (maxVal - minVal) / 2L
                    listOf(maxVal, midVal, minVal).forEach { tick ->
                        Text(
                            text = MoneyMath.formatCompactAxis(tick, currencyCode),
                            style = PocketlyTextStyles.captionTabular,
                            color = colors.inkMuted,
                            maxLines = 1,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // Main Canvas + Anchored Floating Callout Pill
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(plotCanvasHeight)
            ) {
                val plotWidthPx = with(density) { maxWidth.toPx() }
                val topPadPx = with(density) { (calloutReserveHeight + 6.dp).toPx() }
                val bottomPadPx = with(density) { 10.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val idx = ((offset.x / size.width.coerceAtLeast(1)) * points.lastIndex)
                                        .roundToInt()
                                        .coerceIn(0, points.lastIndex)
                                    val next = if (scrubIndex == idx) null else idx
                                    scrubIndex = next
                                    onScrubIndexChanged?.invoke(next)
                                    feedback.chartPointTap()
                                    spawnRipple(pointIdx = idx, isTapBurst = true)
                                }
                            )
                        }
                        .pointerInput(points) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    val idx = ((offset.x / size.width.coerceAtLeast(1)) * points.lastIndex)
                                        .roundToInt()
                                        .coerceIn(0, points.lastIndex)
                                    scrubIndex = idx
                                    onScrubIndexChanged?.invoke(idx)
                                    feedback.chartPointTap()
                                    spawnRipple(pointIdx = idx, isTapBurst = true)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    scrubIndex = null
                                    onScrubIndexChanged?.invoke(null)
                                },
                                onDragCancel = {
                                    isDragging = false
                                    scrubIndex = null
                                    onScrubIndexChanged?.invoke(null)
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val idx = ((change.position.x / size.width.coerceAtLeast(1)) * points.lastIndex)
                                        .roundToInt()
                                        .coerceIn(0, points.lastIndex)
                                    if (idx != scrubIndex) {
                                        scrubIndex = idx
                                        onScrubIndexChanged?.invoke(idx)
                                        feedback.scrubTick()
                                        spawnRipple(pointIdx = idx, isTapBurst = false)
                                    }
                                }
                            )
                        }
                        .semantics { contentDescription = accessibilitySummary }
                ) {
                    val w = size.width
                    val h = size.height
                    val chartH = (h - topPadPx - bottomPadPx).coerceAtLeast(16f)
                    val bottomY = topPadPx + chartH

                    fun xAt(i: Int): Float = (i.toFloat() / points.lastIndex.toFloat()) * w
                    fun yAt(v: Long): Float = bottomY - ((v - minVal).toFloat() / valueRange) * chartH

                    // 1. Subtle dashed horizontal gridlines (top, middle, bottom)
                    val gridEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    val gridAlpha = if (colors.isDark) 0.90f else 0.75f
                    for (step in 0..2) {
                        val gy = topPadPx + (chartH * step / 2f)
                        drawLine(
                            color = colors.line.copy(alpha = gridAlpha),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (step == 2) null else gridEffect
                        )
                    }

                    // 2. Compute smooth monotone Hermite curve points
                    val valueOffsets = points.indices.map { i -> Offset(xAt(i), yAt(points[i].valueCents)) }
                    val valuePath = buildMonotoneCubicPath(valueOffsets)

                    val putInOffsets = if (showPutInSeries) {
                        points.indices.map { i -> Offset(xAt(i), yAt(points[i].putInCents)) }
                    } else emptyList()
                    val putInPath = if (showPutInSeries) buildMonotoneCubicPath(putInOffsets) else null

                    // 3. Continuous left-to-right clip reveal
                    val revealRight = (w * revealProgress.value).coerceIn(0f, w)
                    clipRect(left = 0f, top = 0f, right = revealRight + 8.dp.toPx(), bottom = h) {
                        // Area gradient fill under the Value curve
                        val fillPath = Path().apply {
                            addPath(valuePath)
                            lineTo(valueOffsets.last().x, bottomY)
                            lineTo(valueOffsets.first().x, bottomY)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = if (colors.isDark) 0.28f else 0.22f),
                                    lineColor.copy(alpha = if (colors.isDark) 0.05f else 0.03f),
                                    Color.Transparent
                                ),
                                startY = topPadPx,
                                endY = bottomY
                            )
                        )

                        // Optional dashed "Money put in" baseline
                        if (showPutInSeries && putInPath != null) {
                            drawPath(
                                path = putInPath,
                                color = putInLineColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                )
                            )
                        }

                        // Soft luminous glow under the mint line in dark mode
                        if (colors.isDark) {
                            drawPath(
                                path = valuePath,
                                color = lineColor.copy(alpha = 0.22f),
                                style = Stroke(
                                    width = 8.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Crisp primary investment growth curve
                        drawPath(
                            path = valuePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 4. Custom Canvas tactile ripple waves on tapped / scrubbed data points
                    val nowNs = frameNanos
                    if (!reduceMotion && ripples.isNotEmpty()) {
                        ripples.forEach { wave ->
                            val idx = wave.pointIndex.coerceIn(0, points.lastIndex)
                            val durNs = if (wave.isTapBurst) 460_000_000f else 300_000_000f
                            val rawT = ((nowNs - wave.startNanos).coerceAtLeast(0L) / durNs).coerceIn(0f, 1f)
                            if (rawT < 1f) {
                                // Smooth cubic ease-out for organic liquid expansion
                                val eased = 1f - (1f - rawT).pow(3)
                                val fade = (1f - eased).coerceIn(0f, 1f)
                                val cx = xAt(idx)
                                val cy = yAt(points[idx].valueCents)
                                val center = Offset(cx, cy)

                                val maxRadius = if (wave.isTapBurst) 34.dp.toPx() else 21.dp.toPx()
                                val radius = 6.dp.toPx() + (maxRadius - 6.dp.toPx()) * eased

                                // Soft expanding translucent wash
                                drawCircle(
                                    color = lineColor.copy(alpha = fade * (if (wave.isTapBurst) 0.24f else 0.16f)),
                                    radius = radius * 0.88f,
                                    center = center
                                )

                                // Primary crisp expanding ring
                                drawCircle(
                                    color = lineColor.copy(alpha = fade * (if (colors.isDark) 0.85f else 0.72f)),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = (2.4f - 1.5f * eased).coerceAtLeast(0.8f).dp.toPx())
                                )

                                // Secondary echo shockwave on tap bursts (plus subtle dark-mode highlight)
                                if (wave.isTapBurst && rawT > 0.14f) {
                                    val t2 = ((rawT - 0.14f) / 0.86f).coerceIn(0f, 1f)
                                    val eased2 = 1f - (1f - t2).pow(3)
                                    val fade2 = (1f - eased2).coerceIn(0f, 1f)
                                    val r2 = 5.dp.toPx() + (22.dp.toPx() * eased2)
                                    val ringColor = if (colors.isDark) {
                                        Color.White.copy(alpha = fade2 * 0.36f)
                                    } else {
                                        lineColor.copy(alpha = fade2 * 0.48f)
                                    }
                                    drawCircle(
                                        color = ringColor,
                                        radius = r2,
                                        center = center,
                                        style = Stroke(width = 1.4.dp.toPx())
                                    )
                                }

                                // Companion subtle micro-ripple on dashed "Money put in" baseline
                                if (showPutInSeries) {
                                    val putInCenter = Offset(cx, yAt(points[idx].putInCents))
                                    val putInRadius = 4.dp.toPx() + 10.dp.toPx() * eased
                                    drawCircle(
                                        color = putInLineColor.copy(alpha = fade * 0.55f),
                                        radius = putInRadius,
                                        center = putInCenter,
                                        style = Stroke(width = 1.2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // 5. Active point marker + vertical scrub guideline
                    val markerIdx = activeIdx ?: points.lastIndex
                    val markerX = if (activeIdx != null) animatedXFraction * w else xAt(markerIdx)
                    val markerValY = yAt(points[markerIdx].valueCents)

                    if (revealProgress.value >= 0.85f) {
                        if (activeIdx != null) {
                            drawLine(
                                color = if (colors.isDark) colors.line else colors.inkMuted.copy(alpha = 0.30f),
                                start = Offset(markerX, topPadPx - 4.dp.toPx()),
                                end = Offset(markerX, bottomY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        // Secondary marker on "Money put in" baseline when scrubbing
                        if (showPutInSeries && activeIdx != null) {
                            val putInY = yAt(points[markerIdx].putInCents)
                            drawCircle(
                                color = colors.surface,
                                radius = (5f * markerPopScale.coerceAtMost(1.15f)).dp.toPx(),
                                center = Offset(markerX, putInY)
                            )
                            drawCircle(
                                color = putInLineColor,
                                radius = (3f * markerPopScale.coerceAtMost(1.15f)).dp.toPx(),
                                center = Offset(markerX, putInY)
                            )
                        }

                        // Outer halo + crisp ring + mint core (pops smoothly on touch)
                        val haloAlpha = if (reduceMotion && scrubIndex != null) 0.34f else if (colors.isDark) 0.28f else 0.18f
                        drawCircle(
                            color = lineColor.copy(alpha = haloAlpha),
                            radius = (10.5f * markerPopScale).dp.toPx(),
                            center = Offset(markerX, markerValY)
                        )
                        if (reduceMotion && scrubIndex != null) {
                            drawCircle(
                                color = lineColor.copy(alpha = 0.55f),
                                radius = 13.dp.toPx(),
                                center = Offset(markerX, markerValY),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        drawCircle(
                            color = colors.surface,
                            radius = (6.5f * markerPopScale).dp.toPx(),
                            center = Offset(markerX, markerValY)
                        )
                        drawCircle(
                            color = lineColor,
                            radius = (4f * markerPopScale).dp.toPx(),
                            center = Offset(markerX, markerValY)
                        )
                    }
                }

                // 6. Anchored floating callout pill above the active point (e.g. "Year 6: $14,520")
                if (activeIdx != null) {
                    val activePoint = points[activeIdx]
                    val titlePrefix = calloutLabelFormatter?.invoke(activePoint, activeIdx)
                        ?: DateUtils.formatShortDate(activePoint.dayKey)
                    val valueStr = MoneyMath.formatCents(activePoint.valueCents, currencyCode, dropWholeZeros = true)
                    val earnedCents = activePoint.valueCents - activePoint.putInCents

                    val anchorX = (animatedXFraction * plotWidthPx).roundToInt()
                    val marginPx = with(density) { 4.dp.toPx().roundToInt() }
                    val clampedLeft = (anchorX - calloutWidthPx / 2)
                        .coerceIn(marginPx, (plotWidthPx.roundToInt() - calloutWidthPx - marginPx).coerceAtLeast(marginPx))

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(x = clampedLeft, y = 0) }
                            .onSizeChanged { calloutWidthPx = it.width }
                            .testTag("${testTagName}_callout"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (colors.isDark) colors.surface2 else colors.surface,
                        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                        border = BorderStroke(1.dp, if (isDragging) lineColor.copy(alpha = 0.55f) else colors.line)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "$titlePrefix:",
                                    style = PocketlyTextStyles.caption,
                                    color = colors.inkMuted,
                                    maxLines = 1
                                )
                                Text(
                                    text = valueStr,
                                    style = PocketlyTextStyles.rowAmount,
                                    color = colors.ink,
                                    maxLines = 1
                                )
                            }
                            if (showPutInSeries && abs(earnedCents) >= 100L) {
                                val gainPos = earnedCents >= 0L
                                Text(
                                    text = "${MoneyMath.formatCents(earnedCents, currencyCode, showPlus = true, dropWholeZeros = true)} growth",
                                    style = PocketlyTextStyles.captionTabular,
                                    color = if (gainPos) lineColor else colors.coral,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom X-axis time markers (start, mid, end — collision-free)
        if (showAxisLabels) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = yAxisWidth),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val sampleIndices = remember(points.size) {
                    if (points.size <= 2) listOf(0, points.lastIndex)
                    else listOf(0, points.lastIndex / 2, points.lastIndex)
                }
                sampleIndices.forEachIndexed { pos, idx ->
                    val pt = points[idx]
                    val label = xLabelFormatter?.invoke(pt, idx) ?: DateUtils.formatShortDate(pt.dayKey)
                    Text(
                        text = label,
                        style = PocketlyTextStyles.caption,
                        color = if (pos == sampleIndices.lastIndex) colors.ink else colors.inkMuted,
                        maxLines = 1
                    )
                }
            }
        }

        // Built-in theme-consistent legend (solid mint Value + true dashed Money put in)
        if (showLegend) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Canvas(modifier = Modifier.size(width = 18.dp, height = 4.dp)) {
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        Text(valueLegendLabel, style = PocketlyTextStyles.caption, color = colors.inkMuted)
                    }
                    if (showPutInSeries) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Canvas(modifier = Modifier.size(width = 18.dp, height = 4.dp)) {
                                drawLine(
                                    color = putInLineColor,
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                                )
                            }
                            Text(putInLegendLabel, style = PocketlyTextStyles.caption, color = colors.inkMuted)
                        }
                    }
                }
                if (scrubIndex == null) {
                    Text(
                        text = "Drag or tap to inspect",
                        style = PocketlyTextStyles.caption,
                        color = colors.inkMuted.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

/**
 * Fritsch-Carlson monotone cubic Hermite spline interpolation.
 * Guarantees smooth investment curves that preserve monotonicity without artificial dips or overshoots.
 */
private fun buildMonotoneCubicPath(pts: List<Offset>): Path {
    val path = Path()
    val n = pts.size
    if (n == 0) return path
    path.moveTo(pts[0].x, pts[0].y)
    if (n == 1) return path
    if (n == 2) {
        path.lineTo(pts[1].x, pts[1].y)
        return path
    }

    val dx = FloatArray(n - 1)
    val slopes = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        val d = (pts[i + 1].x - pts[i].x).coerceAtLeast(0.001f)
        dx[i] = d
        slopes[i] = (pts[i + 1].y - pts[i].y) / d
    }

    val tangents = FloatArray(n)
    tangents[0] = slopes[0]
    tangents[n - 1] = slopes[n - 2]
    for (i in 1 until n - 1) {
        if (slopes[i - 1] * slopes[i] <= 0f) {
            tangents[i] = 0f
        } else {
            tangents[i] = (slopes[i - 1] + slopes[i]) * 0.5f
        }
    }

    // Enforce Fritsch-Carlson monotonicity constraint on each interval
    for (i in 0 until n - 1) {
        val m = slopes[i]
        if (abs(m) < 1e-6f) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
        } else {
            val a = tangents[i] / m
            val b = tangents[i + 1] / m
            val sumSq = a * a + b * b
            if (sumSq > 9f) {
                val tau = 3f / kotlin.math.sqrt(sumSq)
                tangents[i] = tau * a * m
                tangents[i + 1] = tau * b * m
            }
        }
    }

    for (i in 0 until n - 1) {
        val step = dx[i] / 3f
        val p0 = pts[i]
        val p1 = pts[i + 1]
        val cp1x = p0.x + step
        val cp1y = p0.y + tangents[i] * step
        val cp2x = p1.x - step
        val cp2y = p1.y - tangents[i + 1] * step
        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p1.x, p1.y)
    }

    return path
}

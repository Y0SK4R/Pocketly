package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.MoneyMath
import com.example.ui.theme.PocketlyPalette
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import kotlin.math.roundToLong

fun Modifier.pressScale(
    enabled: Boolean = true,
    targetScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = PocketlyTheme.reduceMotion
    val feedback = LocalPocketlyFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion && enabled) targetScale else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f),
        label = "press_scale"
    )
    this
        .scale(scale)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                feedback.tap()
                onClick()
            }
        )
}

@Composable
fun PocketlyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    containerColor: Color = PocketlyTheme.colors.surface,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PocketlyTheme.colors
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val border = if (colors.isDark) BorderStroke(1.dp, colors.line) else null
    val shadow = if (colors.isDark) 0.dp else 2.dp

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            shadowElevation = shadow
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            shadowElevation = shadow
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun CategoryIconTile(
    iconKey: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    circular: Boolean = false
) {
    val colors = PocketlyTheme.colors
    val tint = PocketlyPalette.colorForKey(colorKey)
    val bg = PocketlyPalette.softBackgroundFor(tint, colors.isDark)
    val shape = if (circular) CircleShape else RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = PocketlyIcons.forKey(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun MoneyPillChip(
    amountCents: Long,
    currencyCode: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    dropWholeZeros: Boolean = false
) {
    val colors = PocketlyTheme.colors
    val fg = if (positive) colors.mint else colors.coral
    val bg = if (positive) colors.mintSoft else colors.coralSoft
    val formatted = MoneyMath.formatCents(
        cents = if (positive) kotlin.math.abs(amountCents) else -kotlin.math.abs(amountCents),
        currencyCode = currencyCode,
        showPlus = positive,
        dropWholeZeros = dropWholeZeros
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showArrow) {
            Icon(
                imageVector = if (positive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = if (positive) "Income" else "Expense",
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = formatted,
            style = PocketlyTextStyles.captionTabular,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    useMintSelection: Boolean = false,
    tagPrefix: String = "segment"
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            val bg = when {
                selected && useMintSelection -> colors.mintSoft
                selected -> colors.surface
                else -> Color.Transparent
            }
            val textColor = when {
                selected && useMintSelection -> colors.mint
                selected -> colors.ink
                else -> colors.inkMuted
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .then(
                        if (selected && colors.isDark && !useMintSelection) {
                            Modifier.border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        } else Modifier
                    )
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
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedCurrencyText(
    targetCents: Long,
    currencyCode: String,
    style: TextStyle = PocketlyTextStyles.heroNumber,
    color: Color = PocketlyTheme.colors.ink,
    showPlus: Boolean = false,
    dropWholeZeros: Boolean = false,
    modifier: Modifier = Modifier
) {
    val reduceMotion = PocketlyTheme.reduceMotion
    val anim = remember { Animatable(if (reduceMotion) targetCents.toFloat() else 0f) }

    LaunchedEffect(targetCents, reduceMotion) {
        if (reduceMotion) {
            anim.snapTo(targetCents.toFloat())
        } else {
            anim.animateTo(
                targetValue = targetCents.toFloat(),
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    val currentCents = anim.value.roundToLong()
    val text = MoneyMath.formatCents(
        cents = currentCents,
        currencyCode = currencyCode,
        showPlus = showPlus,
        dropWholeZeros = dropWholeZeros
    )
    Text(
        text = text,
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val colors = PocketlyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.ink
            )
        }
        Text(
            text = title,
            style = PocketlyTextStyles.screenTitle,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@Composable
fun PocketlyEmptyState(
    title: String,
    subtitle: String? = null,
    primaryLabel: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    showMascot: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    PocketlyCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showMascot) {
                PocketlyMascot(
                    mood = MascotMood.SLEEPY,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = title,
                style = PocketlyTextStyles.sectionTitle,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = PocketlyTextStyles.body,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center
                )
            }
            if (primaryLabel != null && onPrimaryClick != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onPrimaryClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.mint,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("empty_primary_button")
                ) {
                    Text(
                        text = primaryLabel,
                        style = PocketlyTextStyles.button,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
            if (secondaryLabel != null && onSecondaryClick != null) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("empty_secondary_button")
                ) {
                    Text(
                        text = secondaryLabel,
                        style = PocketlyTextStyles.button,
                        color = colors.mint
                    )
                }
            }
        }
    }
}

@Composable
fun InfoBadgePill(
    text: String,
    icon: ImageVector? = null,
    bgColor: Color = PocketlyTheme.colors.mintSoft,
    fgColor: Color = PocketlyTheme.colors.mint,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            style = PocketlyTextStyles.caption,
            color = fgColor
        )
    }
}

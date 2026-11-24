@file:OptIn(ExperimentalTextApi::class)

package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Bundled offline variable Bricolage Grotesque for big hero numbers.
 * Uses full natural width (wdth = 100) and a sturdy optical size (opsz = 18sp)
 * at heavy weights (700-800) so hero amounts feel broad, chunky, and confident.
 */
val BricolageHeroFontFamily = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.opticalSizing(18.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(100f),
            FontVariation.opticalSizing(18.sp)
        )
    )
)

/**
 * Bundled offline variable Bricolage Grotesque for screen titles, section headers, and sheet titles.
 */
val BricolageFontFamily = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(100f),
            FontVariation.opticalSizing(20.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(100f),
            FontVariation.opticalSizing(22.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.opticalSizing(24.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(100f),
            FontVariation.opticalSizing(28.sp)
        )
    )
)

/**
 * Bundled offline variable Figtree for all UI text.
 * Strict floor at weight 500 (even if a Material component requests Normal/Light/Thin)
 * so no text in the app ever looks thin or frail.
 */
val FigtreeFontFamily = FontFamily(
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Thin,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.figtree,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

@Immutable
data class PocketlyTypeScale(
    val keypadAmount: TextStyle,
    val heroNumber: TextStyle,
    val gaugeHeroNumber: TextStyle,
    val mediumNumber: TextStyle,
    val screenTitle: TextStyle,
    val sheetTitle: TextStyle,
    val sectionTitle: TextStyle,
    val rowTitle: TextStyle,
    val rowAmount: TextStyle,
    val body: TextStyle,
    val inputValue: TextStyle,
    val button: TextStyle,
    val label: TextStyle,
    val labelTabular: TextStyle,
    val chip: TextStyle,
    val chipSelected: TextStyle,
    val caption: TextStyle,
    val captionTabular: TextStyle,
    val navLabel: TextStyle,
    val navLabelSelected: TextStyle
)

fun createPocketlyTypeScale(isDark: Boolean): PocketlyTypeScale {
    // Light text on OLED black reads slightly thinner; add +0.01em tracking on dark mode UI text
    val uiTrack = if (isDark) 0.01.em else 0.em

    return PocketlyTypeScale(
        // Giant 56sp keypad & hero entry amount: Bricolage 800, -0.03em, 1.05 line-height, tnum
        keypadAmount = TextStyle(
            fontFamily = BricolageHeroFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 56.sp,
            lineHeight = 59.sp,
            letterSpacing = (-0.03).em,
            fontFeatureSettings = "tnum"
        ),
        // Hero KPI figures (total wealth, cash flow, invested value, calculator result): 42sp, 800
        heroNumber = TextStyle(
            fontFamily = BricolageHeroFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 42.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.03).em,
            fontFeatureSettings = "tnum"
        ),
        // Liquid budget gauge hero number: 38-40sp, 800, -0.03em, 1.05 line-height, tnum
        gaugeHeroNumber = TextStyle(
            fontFamily = BricolageHeroFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 38.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.03).em,
            fontFeatureSettings = "tnum"
        ),
        // Keypad digits & secondary numeric displays: Bricolage 800, tabular
        mediumNumber = TextStyle(
            fontFamily = BricolageHeroFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 31.sp,
            letterSpacing = (-0.025).em,
            fontFeatureSettings = "tnum"
        ),
        // Screen titles ("Investments", "Budgets", "Pocketly"): Bricolage 800, 30sp, -0.02em
        screenTitle = TextStyle(
            fontFamily = BricolageFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            lineHeight = 35.sp,
            letterSpacing = (-0.02).em
        ),
        // Bottom sheet titles: Bricolage 700, 22sp
        sheetTitle = TextStyle(
            fontFamily = BricolageFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            letterSpacing = (-0.015).em
        ),
        // Section titles ("Holdings", "Categories", "Accounts"): Bricolage 700, 22sp
        sectionTitle = TextStyle(
            fontFamily = BricolageFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            letterSpacing = (-0.015).em
        ),
        // Row titles: Figtree 700, 17sp
        rowTitle = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            letterSpacing = uiTrack
        ),
        // Amounts in lists: Figtree 700, 17sp, tabular
        rowAmount = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            letterSpacing = uiTrack,
            fontFeatureSettings = "tnum"
        ),
        // Body & row text: Figtree 500, 16sp (+0.01em dark)
        body = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = uiTrack
        ),
        // Typed form input values: Figtree 500, 16sp (+0.01em dark)
        inputValue = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = uiTrack
        ),
        // Primary & secondary buttons: Figtree 700, 16sp
        button = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = uiTrack
        ),
        // Form labels & muted descriptive labels: Figtree 600, 14sp
        label = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            letterSpacing = uiTrack
        ),
        // Tabular labels (deltas, percentages, ratios): Figtree 600, 14sp, tnum
        labelTabular = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            letterSpacing = uiTrack,
            fontFeatureSettings = "tnum"
        ),
        // Unselected chips & tabs: Figtree 600, 14sp
        chip = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = uiTrack
        ),
        // Selected chips & tabs: Figtree 700, 14sp
        chipSelected = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = uiTrack
        ),
        // Muted captions & chart labels: Figtree 600, 13sp (OLED readable)
        caption = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            letterSpacing = uiTrack
        ),
        // Tabular captions, pills, chart axis ticks: Figtree 600, 13sp, tnum
        captionTabular = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            letterSpacing = uiTrack,
            fontFeatureSettings = "tnum"
        ),
        // Bottom navigation labels: Figtree 600, 12sp
        navLabel = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            letterSpacing = uiTrack
        ),
        // Selected bottom navigation label: Figtree 700, 12sp
        navLabelSelected = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            letterSpacing = uiTrack
        )
    )
}

val LightPocketlyTypeScale = createPocketlyTypeScale(isDark = false)
val DarkPocketlyTypeScale = createPocketlyTypeScale(isDark = true)

val LocalPocketlyTypeScale = staticCompositionLocalOf { LightPocketlyTypeScale }

/**
 * Theme-aware typography accessor used across all Pocketly screens, sheets, and charts.
 * Automatically adjusts dark-mode letter-spacing (+0.01em) when rendered on AMOLED black.
 */
object PocketlyTextStyles {
    val keypadAmount: TextStyle @Composable get() = LocalPocketlyTypeScale.current.keypadAmount
    val heroNumber: TextStyle @Composable get() = LocalPocketlyTypeScale.current.heroNumber
    val gaugeHeroNumber: TextStyle @Composable get() = LocalPocketlyTypeScale.current.gaugeHeroNumber
    val mediumNumber: TextStyle @Composable get() = LocalPocketlyTypeScale.current.mediumNumber
    val screenTitle: TextStyle @Composable get() = LocalPocketlyTypeScale.current.screenTitle
    val sheetTitle: TextStyle @Composable get() = LocalPocketlyTypeScale.current.sheetTitle
    val sectionTitle: TextStyle @Composable get() = LocalPocketlyTypeScale.current.sectionTitle
    val rowTitle: TextStyle @Composable get() = LocalPocketlyTypeScale.current.rowTitle
    val rowAmount: TextStyle @Composable get() = LocalPocketlyTypeScale.current.rowAmount
    val body: TextStyle @Composable get() = LocalPocketlyTypeScale.current.body
    val inputValue: TextStyle @Composable get() = LocalPocketlyTypeScale.current.inputValue
    val button: TextStyle @Composable get() = LocalPocketlyTypeScale.current.button
    val label: TextStyle @Composable get() = LocalPocketlyTypeScale.current.label
    val labelTabular: TextStyle @Composable get() = LocalPocketlyTypeScale.current.labelTabular
    val chip: TextStyle @Composable get() = LocalPocketlyTypeScale.current.chip
    val chipSelected: TextStyle @Composable get() = LocalPocketlyTypeScale.current.chipSelected
    val caption: TextStyle @Composable get() = LocalPocketlyTypeScale.current.caption
    val captionTabular: TextStyle @Composable get() = LocalPocketlyTypeScale.current.captionTabular
    val navLabel: TextStyle @Composable get() = LocalPocketlyTypeScale.current.navLabel
    val navLabelSelected: TextStyle @Composable get() = LocalPocketlyTypeScale.current.navLabelSelected
}

/**
 * Full 15-slot Material 3 Typography so no M3 Button, Chip, Dialog, Tab, or Snackbar
 * ever falls back to default system fonts or weights below 500.
 */
fun pocketlyMaterialTypography(scale: PocketlyTypeScale): Typography = Typography(
    displayLarge = scale.keypadAmount,
    displayMedium = scale.heroNumber,
    displaySmall = scale.gaugeHeroNumber,
    headlineLarge = scale.screenTitle,
    headlineMedium = scale.sectionTitle,
    headlineSmall = scale.sheetTitle,
    titleLarge = scale.sectionTitle,
    titleMedium = scale.rowTitle,
    titleSmall = scale.button,
    bodyLarge = scale.body,
    bodyMedium = scale.label,
    bodySmall = scale.caption,
    labelLarge = scale.button,
    labelMedium = scale.chip,
    labelSmall = scale.navLabel
)

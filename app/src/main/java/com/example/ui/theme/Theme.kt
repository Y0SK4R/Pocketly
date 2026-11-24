package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class PocketlyColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val inkMuted: Color,
    val line: Color,
    val mint: Color,
    val mintSoft: Color,
    val coral: Color,
    val coralSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val lavenderBar: Color,
    val lockBg: Color,
    val lockWash: Color,
    val isDark: Boolean
)

val LightPocketlyColors = PocketlyColors(
    bg = BgLight,
    surface = SurfaceLight,
    surface2 = Surface2Light,
    ink = InkLight,
    inkMuted = InkMutedLight,
    line = LineLight,
    mint = MintLight,
    mintSoft = MintSoftLight,
    coral = CoralLight,
    coralSoft = CoralSoftLight,
    amber = AmberLight,
    amberSoft = AmberSoftLight,
    lavenderBar = LavenderBarLight,
    lockBg = LockCreamBg,
    lockWash = LockPeachWash,
    isDark = false
)

val DarkPocketlyColors = PocketlyColors(
    bg = BgDark,
    surface = SurfaceDark,
    surface2 = Surface2Dark,
    ink = InkDark,
    inkMuted = InkMutedDark,
    line = LineDark,
    mint = MintDark,
    mintSoft = MintSoftDark,
    coral = CoralDark,
    coralSoft = CoralSoftDark,
    amber = AmberDark,
    amberSoft = AmberSoftDark,
    lavenderBar = LavenderBarDark,
    lockBg = LockDarkBg,
    lockWash = LockDarkWash,
    isDark = true
)

val LocalPocketlyColors = staticCompositionLocalOf { LightPocketlyColors }
val LocalReduceMotion = staticCompositionLocalOf { false }

private val PocketlyLightColorScheme = lightColorScheme(
    primary = MintLight,
    onPrimary = Color.White,
    primaryContainer = MintSoftLight,
    onPrimaryContainer = MintLight,
    secondary = CatPurple,
    onSecondary = Color.White,
    background = BgLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = Surface2Light,
    onSurfaceVariant = InkMutedLight,
    outline = LineLight,
    outlineVariant = LineLight,
    error = CoralLight,
    onError = Color.White,
    errorContainer = CoralSoftLight,
    onErrorContainer = CoralLight
)

private val PocketlyDarkColorScheme = darkColorScheme(
    primary = MintDark,
    onPrimary = Color(0xFF042218),
    primaryContainer = MintSoftDark,
    onPrimaryContainer = MintDark,
    secondary = LavenderBarDark,
    onSecondary = Color(0xFF1A102E),
    background = BgDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = Surface2Dark,
    onSurfaceVariant = InkMutedDark,
    outline = LineDark,
    outlineVariant = LineDark,
    error = CoralDark,
    onError = Color(0xFF2B0709),
    errorContainer = CoralSoftDark,
    onErrorContainer = CoralDark
)

val PocketlyShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object PocketlyTheme {
    val colors: PocketlyColors
        @Composable
        get() = LocalPocketlyColors.current

    val reduceMotion: Boolean
        @Composable
        get() = LocalReduceMotion.current
}

@Composable
fun PocketlyTheme(
    themeMode: String = "system",
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val pocketlyColors = if (useDark) DarkPocketlyColors else LightPocketlyColors
    val typeScale = if (useDark) DarkPocketlyTypeScale else LightPocketlyTypeScale
    val m3Scheme = if (useDark) PocketlyDarkColorScheme else PocketlyLightColorScheme

    CompositionLocalProvider(
        LocalPocketlyColors provides pocketlyColors,
        LocalPocketlyTypeScale provides typeScale,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = pocketlyMaterialTypography(typeScale),
            shapes = PocketlyShapes,
            content = content
        )
    }
}

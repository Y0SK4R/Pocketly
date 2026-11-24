package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light palette tokens (matching Pocketly spec & mockups)
val BgLight = Color(0xFFF3F2F8)
val SurfaceLight = Color(0xFFFFFFFF)
val Surface2Light = Color(0xFFF7F6FB)
val InkLight = Color(0xFF16161D)
val InkMutedLight = Color(0xFF6B6B7B)
val LineLight = Color(0xFFE7E6EF)
val MintLight = Color(0xFF19C48B)
val MintSoftLight = Color(0xFFDDF7EE)
val CoralLight = Color(0xFFFF6B6B)
val CoralSoftLight = Color(0xFFFFE6E6)
val AmberLight = Color(0xFFF5A524)
val AmberSoftLight = Color(0xFFFFF3DC)
val LavenderBarLight = Color(0xFFC3A6F9)
val LockCreamBg = Color(0xFFFBF0DF)
val LockPeachWash = Color(0xFFF8CE8C)

// Dark palette tokens (AMOLED black matching Profile Dark mockup)
val BgDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF121217)
val Surface2Dark = Color(0xFF1A1A22)
val InkDark = Color(0xFFF4F4F8)
val InkMutedDark = Color(0xFFA8A8BA)
val LineDark = Color(0xFF25252F)
val MintDark = Color(0xFF2BE0A4)
val MintSoftDark = Color(0xFF0F2E25)
val CoralDark = Color(0xFFFF8585)
val CoralSoftDark = Color(0xFF33191C)
val AmberDark = Color(0xFFF7B84B)
val AmberSoftDark = Color(0xFF332611)
val LavenderBarDark = Color(0xFFB394F6)
val LockDarkBg = Color(0xFF14110C)
val LockDarkWash = Color(0xFF3B2B1A)

// Category semantic palette
val CatOrange = Color(0xFFFFA31A)
val CatPink = Color(0xFFF2619C)
val CatSky = Color(0xFF3EA6F0)
val CatLime = Color(0xFF78C142)
val CatPurple = Color(0xFF8C56E0)
val CatTeal = Color(0xFF4FC3A9)
val CatClay = Color(0xFFB65A3F)
val CatSlate = Color(0xFF6B7A8F)

object PocketlyPalette {
    val categoryColors: Map<String, Color> = mapOf(
        "orange" to CatOrange,
        "pink" to CatPink,
        "sky" to CatSky,
        "lime" to CatLime,
        "purple" to CatPurple,
        "teal" to CatTeal,
        "clay" to CatClay,
        "slate" to CatSlate,
        "mint" to MintLight,
        "coral" to CoralLight
    )

    fun colorForKey(key: String): Color = categoryColors[key.lowercase()] ?: CatSlate

    fun softBackgroundFor(color: Color, isDark: Boolean): Color =
        color.copy(alpha = if (isDark) 0.22f else 0.16f)
}

package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val minorDigits: Int = 2
)

object SupportedCurrencies {
    val all: List<CurrencyInfo> = listOf(
        CurrencyInfo("USD", "$", "US Dollar", 2),
        CurrencyInfo("EUR", "€", "Euro", 2),
        CurrencyInfo("GBP", "£", "British Pound", 2),
        CurrencyInfo("CAD", "CA$", "Canadian Dollar", 2),
        CurrencyInfo("AUD", "A$", "Australian Dollar", 2),
        CurrencyInfo("JPY", "¥", "Japanese Yen", 0),
        CurrencyInfo("CHF", "CHF ", "Swiss Franc", 2),
        CurrencyInfo("INR", "₹", "Indian Rupee", 2),
        CurrencyInfo("BRL", "R$", "Brazilian Real", 2),
        CurrencyInfo("MXN", "MX$", "Mexican Peso", 2),
        CurrencyInfo("KRW", "₩", "South Korean Won", 0)
    )

    fun forCode(code: String): CurrencyInfo =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: all.first()
}

object MoneyMath {
    const val MAX_AMOUNT_CENTS: Long = 999_999_999L // 9,999,999.99

    private val symbolsUs = DecimalFormatSymbols(Locale.US)

    fun formatCents(
        cents: Long,
        currencyCode: String = "USD",
        showPlus: Boolean = false,
        dropWholeZeros: Boolean = false
    ): String {
        val info = SupportedCurrencies.forCode(currencyCode)
        // Guard against -0.00
        val safeCents = if (cents == 0L) 0L else cents
        val negative = safeCents < 0L
        val absCents = abs(safeCents)
        val signPrefix = when {
            negative -> "-"
            showPlus && absCents > 0L -> "+"
            else -> ""
        }

        if (info.minorDigits == 0) {
            val whole = (absCents / 100.0).roundToLong()
            val formatted = DecimalFormat("#,##0", symbolsUs).format(whole)
            return "$signPrefix${info.symbol}$formatted"
        }

        val dollars = absCents / 100L
        val remainder = absCents % 100L
        val wholeFormatted = DecimalFormat("#,##0", symbolsUs).format(dollars)
        return if (dropWholeZeros && remainder == 0L) {
            "$signPrefix${info.symbol}$wholeFormatted"
        } else {
            val remFormatted = String.format(Locale.US, "%02d", remainder)
            "$signPrefix${info.symbol}$wholeFormatted.$remFormatted"
        }
    }

    fun formatCompactAxis(cents: Long, currencyCode: String = "USD"): String {
        val info = SupportedCurrencies.forCode(currencyCode)
        val absDollars = abs(cents) / 100.0
        val sign = if (cents < 0L) "-" else ""
        return when {
            absDollars >= 1_000_000.0 ->
                String.format(Locale.US, "%s%s%.1fM", sign, info.symbol, absDollars / 1_000_000.0)
            absDollars >= 1_000.0 -> {
                val k = absDollars / 1_000.0
                if (abs(k - k.roundToLong()) < 0.05) {
                    "$sign${info.symbol}${k.roundToLong()}k"
                } else {
                    String.format(Locale.US, "%s%s%.1fk", sign, info.symbol, k)
                }
            }
            else -> "$sign${info.symbol}${absDollars.roundToLong()}"
        }
    }

    /**
     * Parses a keypad string like "18.5", "0", "1200.00" into safe positive cents capped at MAX_AMOUNT_CENTS.
     */
    fun parseKeypadInputToCents(raw: String): Long {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty() || cleaned == ".") return 0L
        val parts = cleaned.split(".")
        if (parts.size > 2) return 0L
        val wholeStr = parts[0].filter { it.isDigit() }
        val fracStr = if (parts.size == 2) parts[1].filter { it.isDigit() }.take(2) else ""
        val whole = wholeStr.toLongOrNull() ?: 0L
        val frac = when (fracStr.length) {
            0 -> 0L
            1 -> (fracStr.toLongOrNull() ?: 0L) * 10L
            else -> fracStr.toLongOrNull() ?: 0L
        }
        val total = whole.coerceAtMost(9_999_999L) * 100L + frac
        return total.coerceIn(0L, MAX_AMOUNT_CENTS)
    }

    fun centsToKeypadString(cents: Long): String {
        val safe = abs(cents).coerceAtMost(MAX_AMOUNT_CENTS)
        if (safe == 0L) return "0"
        val whole = safe / 100L
        val frac = safe % 100L
        return if (frac == 0L) whole.toString() else String.format(Locale.US, "%d.%02d", whole, frac)
    }

    /**
     * Safe percentage change vs previous period.
     * Returns null when previous == 0 so callers render "New" instead of dividing by zero.
     */
    fun percentDelta(current: Long, previous: Long): Double? {
        if (previous == 0L) return null
        val pct = ((current - previous).toDouble() / abs(previous).toDouble()) * 100.0
        if (pct.isNaN() || pct.isInfinite()) return null
        return pct
    }

    fun formatPercent(pct: Double, showPlus: Boolean = false, decimals: Int = 1): String {
        val safe = if (pct.isNaN() || pct.isInfinite() || abs(pct) < 0.0001) 0.0 else pct
        val prefix = if (showPlus && safe > 0.0) "+" else ""
        return String.format(Locale.US, "%s%.${decimals}f%%", prefix, safe)
    }
}

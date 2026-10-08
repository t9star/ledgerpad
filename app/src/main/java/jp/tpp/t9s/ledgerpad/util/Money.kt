package jp.tpp.t9s.ledgerpad.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Currency representation and formatting.
 * All amounts inside the app are stored as Long in minor units (cents / paise / centavos = 1/100).
 * Currencies that traditionally don't use decimals (e.g. JPY, IDR, KRW) simply hide the fractional part in UI.
 */
data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val hasDecimals: Boolean = true,
)

object Money {

    val SUPPORTED = listOf(
        CurrencyInfo("INR", "₹", true),
        CurrencyInfo("BRL", "R$", true),
        CurrencyInfo("IDR", "Rp", false),
        CurrencyInfo("PHP", "₱", true),
        CurrencyInfo("USD", "$", true),
        CurrencyInfo("EUR", "€", true),
        CurrencyInfo("MXN", "$", true),
        CurrencyInfo("JPY", "¥", false),
        CurrencyInfo("BDT", "৳", true),
    )

    fun defaultCurrencyCode(): String {
        val loc = Locale.getDefault()
        return try {
            val c = Currency.getInstance(loc)
            if (SUPPORTED.any { it.code == c.currencyCode }) c.currencyCode else "USD"
        } catch (_: Exception) {
            "USD"
        }
    }

    fun getInfo(code: String): CurrencyInfo =
        SUPPORTED.firstOrNull { it.code == code } ?: CurrencyInfo(code, code, true)

    /** Formats minor units (1/100) to formatted string with symbol, e.g. "₹1,250.00" or "Rp15.000" */
    fun format(amountMinor: Long, code: String, includeSign: Boolean = false): String {
        val info = getInfo(code)
        val isNegative = amountMinor < 0
        val abs = if (isNegative) -amountMinor else amountMinor

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val pattern = if (info.hasDecimals) "#,##0.00" else "#,##0"
        val df = DecimalFormat(pattern, symbols)

        val valueToFormat: Double = if (info.hasDecimals) abs / 100.0 else (abs / 100.0)
        val formattedNumber = df.format(valueToFormat)

        val signPrefix = if (includeSign) {
            if (isNegative) "- " else "+ "
        } else if (isNegative) "- " else ""

        return "$signPrefix${info.symbol}$formattedNumber"
    }

    /**
     * Converts a user entered decimal amount (e.g. 12.50 or 500) into minor units (1250 or 50000).
     */
    fun parseToMinor(amountText: String, code: String): Long {
        val clean = amountText.trim().replace(",", "")
        if (clean.isBlank()) return 0L
        val d = clean.toDoubleOrNull() ?: return 0L
        val info = getInfo(code)
        return if (info.hasDecimals) {
            (d * 100).toLong()
        } else {
            // For non-decimal currencies like IDR, entering 500 means 500. Stored internally as 50000 (cents)
            (d * 100).toLong()
        }
    }

    /**
     * Returns suggested quick chip amounts (in whole major units) according to typical denominations.
     */
    fun quickAmounts(code: String): List<Long> {
        return when (code) {
            "INR" -> listOf(10L, 20L, 50L, 100L, 500L)
            "BRL" -> listOf(5L, 10L, 20L, 50L, 100L)
            "IDR" -> listOf(5000L, 10000L, 20000L, 50000L, 100000L)
            "PHP" -> listOf(20L, 50L, 100L, 200L, 500L)
            "JPY" -> listOf(100L, 500L, 1000L, 5000L)
            "BDT" -> listOf(20L, 50L, 100L, 200L, 500L)
            else -> listOf(5L, 10L, 20L, 50L, 100L)
        }
    }
}

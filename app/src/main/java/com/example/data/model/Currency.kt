package com.example.data.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val rateAgainstUsd: Double
)

object Currencies {
    val SUPPORTED = listOf(
        CurrencyInfo("USD", "$", "US Dollar", 1.0),
        CurrencyInfo("CFA", "CFA", "CFA Franc", 605.0)
    )

    fun getCurrency(code: String): CurrencyInfo {
        if (TwtAccountHelper.isTwtAccountName(code) || code.equals("$", ignoreCase = true)) {
            return SUPPORTED.first() // Always USD ($)
        }
        if (code.equals("FCFA", ignoreCase = true) || code.equals("XOF", ignoreCase = true)) {
            return SUPPORTED[1]
        }
        return SUPPORTED.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED[1]
    }

    fun effectiveCurrencyCode(account: AccountEntity?, fallback: String = "CFA"): String {
        if (TwtAccountHelper.isTwtAccount(account)) return "USD"
        val raw = account?.currencyCode ?: fallback
        return if (raw.equals("USD", ignoreCase = true) || raw.equals("$", ignoreCase = true)) {
            "USD"
        } else {
            "CFA"
        }
    }

    fun toMoneyBigDecimal(value: Double): BigDecimal {
        if (value.isNaN() || value.isInfinite()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    }

    fun parseBigDecimal(raw: String?): BigDecimal? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim()
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace(',', '.')
        if (normalized.isBlank() || normalized == "." || normalized == "-" || normalized == "+") return null
        return try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun sumAmounts(values: Iterable<Double>): BigDecimal {
        var acc = BigDecimal.ZERO
        for (v in values) {
            if (!v.isNaN() && !v.isInfinite()) {
                acc = acc.add(BigDecimal.valueOf(v))
            }
        }
        return acc.setScale(2, RoundingMode.HALF_UP)
    }

    fun computeNetCfaFromFx(amountUsd: BigDecimal, rateUsdToCfa: BigDecimal): BigDecimal {
        return amountUsd.multiply(rateUsdToCfa).setScale(2, RoundingMode.HALF_UP)
    }

    fun computeTotalUsdDeduction(amountUsd: BigDecimal, feeUsd: BigDecimal): BigDecimal {
        return amountUsd.add(feeUsd).setScale(2, RoundingMode.HALF_UP)
    }

    fun formatForAccount(amount: Double, account: AccountEntity?, fallback: String = "CFA"): String {
        return format(toMoneyBigDecimal(amount), effectiveCurrencyCode(account, fallback))
    }

    fun formatForAccount(amount: BigDecimal, account: AccountEntity?, fallback: String = "CFA"): String {
        return format(amount, effectiveCurrencyCode(account, fallback))
    }

    fun format(amount: Double, currencyCode: String = "CFA"): String {
        return format(toMoneyBigDecimal(amount), currencyCode)
    }

    fun format(amount: BigDecimal, currencyCode: String = "CFA"): String {
        val currency = getCurrency(currencyCode)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.00", symbols).apply {
            roundingMode = RoundingMode.HALF_UP
        }
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        val isNegative = scaled.compareTo(BigDecimal.ZERO) < 0
        val sign = if (isNegative) "-" else ""
        val formattedNum = df.format(scaled.abs())

        return when (currency.code) {
            "CFA" -> "$sign$formattedNum CFA"
            else -> "$sign${currency.symbol}$formattedNum"
        }
    }
}

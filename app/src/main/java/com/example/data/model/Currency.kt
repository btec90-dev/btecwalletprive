package com.example.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

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

    fun formatForAccount(amount: Double, account: AccountEntity?, fallback: String = "CFA"): String {
        return format(amount, effectiveCurrencyCode(account, fallback))
    }

    fun format(amount: Double, currencyCode: String = "CFA"): String {
        val currency = getCurrency(currencyCode)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.00", symbols)
        val sign = if (amount < 0) "-" else ""
        val formattedNum = df.format(abs(amount))

        return when (currency.code) {
            "CFA" -> "$sign$formattedNum CFA"
            else -> "$sign${currency.symbol}$formattedNum"
        }
    }
}

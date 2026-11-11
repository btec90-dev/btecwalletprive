package com.example.data.model

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
        CurrencyInfo("CFA", "FCFA", "CFA Franc", 605.0)
    )

    fun getCurrency(code: String): CurrencyInfo {
        return SUPPORTED.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED.first()
    }

    fun format(amount: Double, currencyCode: String = "USD"): String {
        val currency = getCurrency(currencyCode)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val pattern = "#,##0.00"
        val df = DecimalFormat(pattern, symbols)
        val formattedNum = df.format(amount)
        
        return when (currency.code) {
            "CFA" -> "$formattedNum ${currency.symbol}"
            else -> "${currency.symbol}$formattedNum"
        }
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AccountEntity
import com.example.data.model.Currencies
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TwtAccountHelper
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    fun generateTransactionsPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        accountMap: Map<String, AccountEntity>,
        title: String = "Rapport Financier",
        periodLabel: String = "Toutes les dates",
        filterSummary: String? = null
    ): File? {
        var doc: PdfDocument? = null
        return try {
            doc = PdfDocument()
            val pageWidth = 595 // A4 width (72 dpi)
            val pageHeight = 842 // A4 height

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
            val shortDateFormat = SimpleDateFormat("dd/MM/yy", Locale.FRENCH)

            val usdTxs = transactions.filter {
                val acc = accountMap[it.accountId]
                Currencies.effectiveCurrencyCode(acc, "CFA") == "USD"
            }
            val cfaTxs = transactions.filter {
                val acc = accountMap[it.accountId]
                Currencies.effectiveCurrencyCode(acc, "CFA") == "CFA"
            }

            val cfaIncome: BigDecimal = Currencies.sumAmounts(cfaTxs.filter { it.type == TransactionType.INCOME }.map { it.amount })
            val cfaExpense: BigDecimal = Currencies.sumAmounts(cfaTxs.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
            val cfaNet: BigDecimal = cfaIncome.subtract(cfaExpense)

            val usdIncome: BigDecimal = Currencies.sumAmounts(usdTxs.filter { it.type == TransactionType.INCOME }.map { it.amount })
            val usdExpense: BigDecimal = Currencies.sumAmounts(usdTxs.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
            val usdNet: BigDecimal = usdIncome.subtract(usdExpense)

            val itemsPerPage = 21
            val totalPages = if (transactions.isEmpty()) 1 else ((transactions.size - 1) / itemsPerPage) + 1

            val paint = Paint().apply { isAntiAlias = true }

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

            // Header Banner (Espresso Leather Brown)
            paint.color = Color.parseColor("#4A2C2A")
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 64f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 16f
            canvas.drawText("BALATEC SECURE WALLET", 32f, 32f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            canvas.drawText("Grand Livre & Rapports Financiers", 32f, 48f, paint)

            val dateStr = dateFormat.format(Date())
            paint.textSize = 9f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Généré le: $dateStr", (pageWidth - 32).toFloat(), 40f, paint)
            paint.textAlign = Paint.Align.LEFT

            var currentY = 88f

            if (pageIndex == 0) {
                paint.color = Color.parseColor("#212121")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 14f
                canvas.drawText(title, 32f, currentY, paint)

                paint.color = Color.parseColor("#5D4037")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                paint.textSize = 10f
                val subText = buildString {
                    append("Période: $periodLabel • ${transactions.size} transaction(s)")
                    if (!filterSummary.isNullOrBlank()) {
                        append(" • $filterSummary")
                    }
                }
                canvas.drawText(subText, 32f, currentY + 16f, paint)

                currentY += 32f

                val hasUsd = usdTxs.isNotEmpty()
                val hasCfa = cfaTxs.isNotEmpty() || !hasUsd
                val boxHeight = if (hasUsd && cfaTxs.isNotEmpty()) 66f else 44f

                paint.color = Color.parseColor("#F5F5F0")
                paint.style = Paint.Style.FILL
                canvas.drawRect(32f, currentY, (pageWidth - 32).toFloat(), currentY + boxHeight, paint)

                paint.color = Color.parseColor("#8D6E63")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(32f, currentY, (pageWidth - 32).toFloat(), currentY + boxHeight, paint)
                paint.style = Paint.Style.FILL

                var summaryRowY = currentY + 22f
                paint.textSize = 9.5f

                if (hasCfa) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.parseColor("#1B4D3E")
                    canvas.drawText("CFA Revenus: +${Currencies.format(cfaIncome, "CFA")}", 44f, summaryRowY, paint)

                    paint.color = Color.parseColor("#B71C1C")
                    canvas.drawText("Dépenses: -${Currencies.format(cfaExpense, "CFA")}", 225f, summaryRowY, paint)

                    val cfaNetNonNeg = cfaNet.compareTo(BigDecimal.ZERO) >= 0
                    paint.color = if (cfaNetNonNeg) Color.parseColor("#1B4D3E") else Color.parseColor("#B71C1C")
                    val netSign = if (cfaNetNonNeg) "+" else ""
                    canvas.drawText("Net: $netSign${Currencies.format(cfaNet, "CFA")}", 395f, summaryRowY, paint)

                    summaryRowY += 24f
                }

                if (hasUsd) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.parseColor("#1B4D3E")
                    canvas.drawText("USD ($) Revenus: +${Currencies.format(usdIncome, "USD")}", 44f, summaryRowY, paint)

                    paint.color = Color.parseColor("#B71C1C")
                    canvas.drawText("Dépenses: -${Currencies.format(usdExpense, "USD")}", 225f, summaryRowY, paint)

                    val usdNetNonNeg = usdNet.compareTo(BigDecimal.ZERO) >= 0
                    paint.color = if (usdNetNonNeg) Color.parseColor("#1B4D3E") else Color.parseColor("#B71C1C")
                    val netSign = if (usdNetNonNeg) "+" else ""
                    canvas.drawText("Net: $netSign${Currencies.format(usdNet, "USD")}", 395f, summaryRowY, paint)
                }

                currentY += boxHeight + 16f
            } else {
                currentY += 16f
            }

            // Table Header Bar
            paint.color = Color.parseColor("#5D4037")
            paint.style = Paint.Style.FILL
            canvas.drawRect(32f, currentY, (pageWidth - 32).toFloat(), currentY + 22f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText("DATE", 40f, currentY + 15f, paint)
            canvas.drawText("BÉNÉFICIAIRE", 100f, currentY + 15f, paint)
            canvas.drawText("CATÉGORIE", 248f, currentY + 15f, paint)
            canvas.drawText("COMPTE", 358f, currentY + 15f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("MONTANT", (pageWidth - 40).toFloat(), currentY + 15f, paint)
            paint.textAlign = Paint.Align.LEFT

            currentY += 22f

            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, transactions.size)
            val pageItems = if (transactions.isEmpty()) emptyList() else transactions.subList(startIndex, endIndex)

            paint.textSize = 8.5f

            if (pageItems.isEmpty()) {
                paint.color = Color.parseColor("#FAF7F4")
                paint.style = Paint.Style.FILL
                canvas.drawRect(32f, currentY, (pageWidth - 32).toFloat(), currentY + 34f, paint)
                paint.color = Color.parseColor("#757575")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                paint.textSize = 9.5f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    "Aucune transaction enregistrée pour ce filtre (0 ligne).",
                    (pageWidth / 2f),
                    currentY + 21f,
                    paint
                )
                paint.textAlign = Paint.Align.LEFT
            } else {
                pageItems.forEachIndexed { rowIdx, tx ->
                    val rowY = currentY + (rowIdx * 24f)
                    val isEven = rowIdx % 2 == 0

                    paint.color = if (isEven) Color.parseColor("#FAF7F4") else Color.WHITE
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(32f, rowY, (pageWidth - 32).toFloat(), rowY + 24f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.parseColor("#424242")
                    canvas.drawText(shortDateFormat.format(Date(tx.timestamp)), 40f, rowY + 16f, paint)

                    val payeeText = if (tx.payee.length > 23) tx.payee.take(21) + ".." else tx.payee
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.parseColor("#212121")
                    canvas.drawText(payeeText, 100f, rowY + 16f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.parseColor("#5D4037")
                    val catText = if (tx.category.length > 17) tx.category.take(15) + ".." else tx.category
                    canvas.drawText(catText, 248f, rowY + 16f, paint)

                    val account = accountMap[tx.accountId]
                    val accName = if (TwtAccountHelper.isTwtAccount(account)) {
                        TwtAccountHelper.currentTwtAccountName()
                    } else {
                        account?.name ?: "Compte"
                    }
                    val accText = if (accName.length > 16) accName.take(14) + ".." else accName
                    paint.color = Color.parseColor("#616161")
                    canvas.drawText(accText, 358f, rowY + 16f, paint)

                    val isIncome = tx.type == TransactionType.INCOME
                    paint.color = if (isIncome) Color.parseColor("#1B4D3E") else Color.parseColor("#B71C1C")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val effectiveCurrency = Currencies.effectiveCurrencyCode(account, "CFA")
                    val prefix = if (isIncome) "+" else "-"
                    val amountStr = "$prefix${Currencies.format(tx.amount, effectiveCurrency)}"

                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(amountStr, (pageWidth - 40).toFloat(), rowY + 16f, paint)
                    paint.textAlign = Paint.Align.LEFT

                    paint.color = Color.parseColor("#E0D6CD")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(32f, rowY + 24f, (pageWidth - 32).toFloat(), rowY + 24f, paint)
                    paint.style = Paint.Style.FILL
                }
            }

            paint.color = Color.parseColor("#8D6E63")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.textSize = 8f
            canvas.drawText("Balatec Secure Wallet • Confidentialité & Intégrité garanties", 32f, (pageHeight - 24).toFloat(), paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page ${pageIndex + 1} / $totalPages", (pageWidth - 32).toFloat(), (pageHeight - 24).toFloat(), paint)
            paint.textAlign = Paint.Align.LEFT

            doc.finishPage(page)
        }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "balatec_rapport_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                doc?.close()
            } catch (_: Exception) { }
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Rapport Balatec Wallet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Partager le rapport PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

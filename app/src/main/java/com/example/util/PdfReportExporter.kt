package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    fun generateTransactionsPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        accountMap: Map<String, AccountEntity>,
        title: String = "Rapport Financier",
        periodLabel: String = "Toutes les dates"
    ): File? {
        if (transactions.isEmpty()) return null

        val doc = PdfDocument()
        val pageWidth = 595 // A4 standard width (72 dpi)
        val pageHeight = 842 // A4 standard height

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        val shortDateFormat = SimpleDateFormat("dd/MM/yy", Locale.FRENCH)

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        val itemsPerPage = 22
        val totalPages = ((transactions.size - 1) / itemsPerPage) + 1

        val paint = Paint().apply { isAntiAlias = true }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            // Header Banner (Leather Brown accent)
            paint.color = Color.parseColor("#4A2C2A")
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 64f, paint)

            // Header Title
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

            var currentY = 90f

            if (pageIndex == 0) {
                // Subtitle & Period
                paint.color = Color.parseColor("#212121")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 14f
                canvas.drawText(title, 32f, currentY, paint)

                paint.color = Color.parseColor("#5D4037")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                paint.textSize = 10f
                canvas.drawText("Période: $periodLabel • ${transactions.size} transaction(s)", 32f, currentY + 16f, paint)

                currentY += 40f

                // Summary Cards Box
                paint.color = Color.parseColor("#F5F5F0")
                paint.style = Paint.Style.FILL
                canvas.drawRect(32f, currentY, (pageWidth - 32).toFloat(), currentY + 44f, paint)

                paint.color = Color.parseColor("#8D6E63")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(32f, currentY, (pageWidth - 32).toFloat(), currentY + 44f, paint)
                paint.style = Paint.Style.FILL

                // Summary stats text
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                paint.color = Color.parseColor("#1B4D3E")
                canvas.drawText("Revenus: +${String.format(Locale.FRENCH, "%,.0f", totalIncome)}", 44f, currentY + 26f, paint)

                paint.color = Color.parseColor("#B71C1C")
                canvas.drawText("Dépenses: -${String.format(Locale.FRENCH, "%,.0f", totalExpense)}", 200f, currentY + 26f, paint)

                val netColor = if (netBalance >= 0) Color.parseColor("#1B4D3E") else Color.parseColor("#B71C1C")
                paint.color = netColor
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Net: ${if (netBalance >= 0) "+" else ""}${String.format(Locale.FRENCH, "%,.0f", netBalance)}", 380f, currentY + 26f, paint)

                currentY += 60f
            } else {
                currentY += 20f
            }

            // Table Header Bar
            paint.color = Color.parseColor("#5D4037")
            paint.style = Paint.Style.FILL
            canvas.drawRect(32f, currentY, (pageWidth - 32).toFloat(), currentY + 22f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText("DATE", 40f, currentY + 15f, paint)
            canvas.drawText("BÉNÉFICIAIRE / PAYEUR", 100f, currentY + 15f, paint)
            canvas.drawText("CATÉGORIE", 250f, currentY + 15f, paint)
            canvas.drawText("COMPTE", 370f, currentY + 15f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("MONTANT", (pageWidth - 40).toFloat(), currentY + 15f, paint)
            paint.textAlign = Paint.Align.LEFT

            currentY += 22f

            // Table Rows
            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, transactions.size)
            val pageItems = transactions.subList(startIndex, endIndex)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f

            pageItems.forEachIndexed { rowIdx, tx ->
                val rowY = currentY + (rowIdx * 24f)
                val isEven = rowIdx % 2 == 0

                // Background alternate striping
                paint.color = if (isEven) Color.parseColor("#FAF7F4") else Color.WHITE
                paint.style = Paint.Style.FILL
                canvas.drawRect(32f, rowY, (pageWidth - 32).toFloat(), rowY + 24f, paint)

                // Date
                paint.color = Color.parseColor("#424242")
                val itemDate = shortDateFormat.format(Date(tx.timestamp))
                canvas.drawText(itemDate, 40f, rowY + 16f, paint)

                // Payee (truncated if too long)
                val payeeText = if (tx.payee.length > 24) tx.payee.take(22) + ".." else tx.payee
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#212121")
                canvas.drawText(payeeText, 100f, rowY + 16f, paint)

                // Category
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#5D4037")
                val catText = if (tx.category.length > 18) tx.category.take(16) + ".." else tx.category
                canvas.drawText(catText, 250f, rowY + 16f, paint)

                // Account
                val account = accountMap[tx.accountId]
                val accName = account?.name ?: "Compte"
                val accText = if (accName.length > 16) accName.take(14) + ".." else accName
                paint.color = Color.parseColor("#616161")
                canvas.drawText(accText, 370f, rowY + 16f, paint)

                // Amount
                val isIncome = tx.type == TransactionType.INCOME
                paint.color = if (isIncome) Color.parseColor("#1B4D3E") else Color.parseColor("#B71C1C")
                val currencySymbol = if (account?.currencyCode.equals("USD", ignoreCase = true)) "$" else "CFA"
                val prefix = if (isIncome) "+" else "-"
                val amountStr = "$prefix${String.format(Locale.FRENCH, "%,.0f", tx.amount)} $currencySymbol"

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(amountStr, (pageWidth - 40).toFloat(), rowY + 16f, paint)
                paint.textAlign = Paint.Align.LEFT

                // Bottom divider
                paint.color = Color.parseColor("#E0D6CD")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                canvas.drawLine(32f, rowY + 24f, (pageWidth - 32).toFloat(), rowY + 24f, paint)
                paint.style = Paint.Style.FILL
            }

            // Footer
            paint.color = Color.parseColor("#8D6E63")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.textSize = 8f
            canvas.drawText("Balatec Secure Wallet • Confidentialité & Intégrité garanties", 32f, (pageHeight - 24).toFloat(), paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page ${pageIndex + 1} / $totalPages", (pageWidth - 32).toFloat(), (pageHeight - 24).toFloat(), paint)
            paint.textAlign = Paint.Align.LEFT

            doc.finishPage(page)
        }

        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "balatec_rapport_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
            doc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            doc.close()
            null
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

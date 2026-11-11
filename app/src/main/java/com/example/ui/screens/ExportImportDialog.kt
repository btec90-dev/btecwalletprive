package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.LocalArtisanTheme
import com.example.util.PdfReportExporter
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportDialog(
    transactions: List<TransactionEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    onExportJson: suspend () -> String,
    onExportCsv: suspend () -> String,
    onImportJson: suspend (String) -> Pair<Int, Int>,
    onDismissRequest: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Export, 1: Import
    var selectedDateRange by remember { mutableStateOf(DateRangeFilterType.THIS_MONTH) }
    var importInputText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
    }

    fun shareText(title: String, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    // Date range filter calculations
    val startOfThisMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val (startOfLastMonth, endOfLastMonth) = remember {
        val calStart = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val calEnd = startOfThisMonth
        Pair(calStart, calEnd)
    }

    val filteredForExport = remember(transactions, selectedDateRange) {
        when (selectedDateRange) {
            DateRangeFilterType.ALL -> transactions
            DateRangeFilterType.THIS_MONTH -> transactions.filter { it.timestamp >= startOfThisMonth }
            DateRangeFilterType.LAST_MONTH -> transactions.filter { it.timestamp in startOfLastMonth until endOfLastMonth }
        }
    }

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.surfaceColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.export_import),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = theme.backgroundColor,
                    contentColor = theme.primaryColor,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; statusMessage = null },
                        text = { Text("Exportation", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; statusMessage = null },
                        text = { Text("Importation", fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Export Tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Période du Rapport :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.primaryColor
                        )

                        // Date Quick Filter Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedDateRange == DateRangeFilterType.THIS_MONTH,
                                onClick = { selectedDateRange = DateRangeFilterType.THIS_MONTH },
                                label = { Text("Ce mois-ci", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = theme.onPrimaryColor
                                )
                            )
                            FilterChip(
                                selected = selectedDateRange == DateRangeFilterType.LAST_MONTH,
                                onClick = { selectedDateRange = DateRangeFilterType.LAST_MONTH },
                                label = { Text("Mois dernier", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = theme.onPrimaryColor
                                )
                            )
                            FilterChip(
                                selected = selectedDateRange == DateRangeFilterType.ALL,
                                onClick = { selectedDateRange = DateRangeFilterType.ALL },
                                label = { Text("Tout", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = theme.onPrimaryColor
                                )
                            )
                        }

                        // Export to PDF Button
                        Button(
                            onClick = {
                                val periodTitle = when (selectedDateRange) {
                                    DateRangeFilterType.THIS_MONTH -> "Ce mois-ci"
                                    DateRangeFilterType.LAST_MONTH -> "Mois dernier"
                                    DateRangeFilterType.ALL -> "Toutes les dates"
                                }
                                val file = PdfReportExporter.generateTransactionsPdf(
                                    context = context,
                                    transactions = filteredForExport,
                                    accountMap = accountsMap,
                                    title = "Rapport Financier Balatec Wallet",
                                    periodLabel = "$periodTitle (${filteredForExport.size} transactions)"
                                )
                                if (file != null) {
                                    PdfReportExporter.sharePdf(context, file)
                                    statusMessage = "Rapport PDF généré avec succès !"
                                } else {
                                    statusMessage = "Aucune transaction à exporter dans cette période."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primaryColor,
                                contentColor = theme.onPrimaryColor
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_pdf), fontWeight = FontWeight.Bold)
                        }

                        // Export to CSV Button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    val csv = onExportCsv()
                                    copyToClipboard("Balatec Ledger CSV", csv)
                                    shareText("Balatec Wallet Ledger CSV", csv)
                                    statusMessage = "Grand Livre CSV exporté et prêt à partager !"
                                    isProcessing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.export_csv), fontWeight = FontWeight.SemiBold)
                        }

                        // Export JSON Backup
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    val json = onExportJson()
                                    copyToClipboard("Balatec Wallet Backup JSON", json)
                                    statusMessage = "Sauvegarde JSON générée et copiée !"
                                    isProcessing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.export_json), fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // Import Tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Note: Le format PDF est destiné à l'exportation et à l'impression. Pour restaurer des données, collez le contenu JSON ou CSV de sauvegarde ci-dessous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = importInputText,
                            onValueChange = { importInputText = it; statusMessage = null },
                            placeholder = { Text(stringResource(R.string.paste_json_hint), fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (importInputText.isBlank()) {
                                    statusMessage = "Veuillez coller des données de sauvegarde."
                                } else {
                                    scope.launch {
                                        try {
                                            isProcessing = true
                                            val (accCount, txCount) = onImportJson(importInputText)
                                            statusMessage = "$accCount compte(s) et $txCount transaction(s) importés !"
                                            importInputText = ""
                                        } catch (e: Exception) {
                                            statusMessage = "Échec d'importation : Schéma invalide."
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primaryColor,
                                contentColor = theme.onPrimaryColor
                            )
                        ) {
                            Text(stringResource(R.string.import_confirm), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.contains("Échec", ignoreCase = true) || msg.contains("Aucune", ignoreCase = true)) {
                            MaterialTheme.colorScheme.error
                        } else Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.close), color = theme.primaryColor)
                    }
                }
            }
        }
    }
}

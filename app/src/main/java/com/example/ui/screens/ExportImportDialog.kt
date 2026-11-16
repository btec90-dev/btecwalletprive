package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.data.model.Currencies
import com.example.data.model.PresetFamilyContacts
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WalletCategories
import com.example.ui.theme.LocalArtisanTheme
import com.example.util.PdfReportExporter
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class PdfScopeMode(val labelFr: String) {
    ALL("Tout exporter"),
    BY_ACCOUNT("Par Compte"),
    BY_PAYEE("Par Bénéficiaire"),
    BY_CATEGORY("Par Catégorie")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportDialog(
    transactions: List<TransactionEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    customCategories: List<Pair<String, String>> = emptyList(),
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
    var selectedDateRange by remember { mutableStateOf(DateRangeFilterType.ALL) }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var scopeMode by remember { mutableStateOf(PdfScopeMode.ALL) }

    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedPayee by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectorExpanded by remember { mutableStateOf(false) }

    var importInputText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val availablePayees = remember(transactions) {
        (transactions.map { it.payee.trim() }.filter { it.isNotBlank() } + PresetFamilyContacts.NAMES)
            .distinct()
            .sorted()
    }
    val allCategories = remember(customCategories) {
        WalletCategories.combined(customCategories)
    }

    fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { }
    }

    fun shareText(title: String, text: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (_: Exception) { }
    }

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
        Pair(calStart, startOfThisMonth)
    }

    val filteredForExport = remember(
        transactions,
        selectedDateRange,
        selectedTypeFilter,
        scopeMode,
        selectedAccountId,
        selectedPayee,
        selectedCategory
    ) {
        transactions.filter { tx ->
            val matchDate = when (selectedDateRange) {
                DateRangeFilterType.ALL -> true
                DateRangeFilterType.THIS_MONTH -> tx.timestamp >= startOfThisMonth
                DateRangeFilterType.LAST_MONTH -> tx.timestamp in startOfLastMonth until endOfLastMonth
            }
            val matchType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchScope = when (scopeMode) {
                PdfScopeMode.ALL -> true
                PdfScopeMode.BY_ACCOUNT -> selectedAccountId == null || tx.accountId == selectedAccountId
                PdfScopeMode.BY_PAYEE -> selectedPayee.isNullOrBlank() || tx.payee.equals(selectedPayee, ignoreCase = true)
                PdfScopeMode.BY_CATEGORY -> selectedCategory.isNullOrBlank() || tx.category.equals(selectedCategory, ignoreCase = true)
            }
            matchDate && matchType && matchScope
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
                    .verticalScroll(rememberScrollState())
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Périmètre du Rapport PDF :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.primaryColor
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PdfScopeMode.entries) { mode ->
                                FilterChip(
                                    selected = scopeMode == mode,
                                    onClick = {
                                        scopeMode = mode
                                        selectorExpanded = false
                                        if (mode == PdfScopeMode.BY_ACCOUNT && selectedAccountId == null) {
                                            selectedAccountId = accounts.firstOrNull()?.id
                                        } else if (mode == PdfScopeMode.BY_PAYEE && selectedPayee == null) {
                                            selectedPayee = availablePayees.firstOrNull()
                                        } else if (mode == PdfScopeMode.BY_CATEGORY && selectedCategory == null) {
                                            selectedCategory = allCategories.firstOrNull()?.first
                                        }
                                    },
                                    label = { Text(mode.labelFr, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.primaryColor,
                                        selectedLabelColor = theme.onPrimaryColor
                                    )
                                )
                            }
                        }

                        if (scopeMode != PdfScopeMode.ALL) {
                            val currentLabel = when (scopeMode) {
                                PdfScopeMode.BY_ACCOUNT -> accountsMap[selectedAccountId]?.let {
                                    "${it.emojiAvatar} ${it.name} (${Currencies.effectiveCurrencyCode(it, "CFA")})"
                                } ?: "Choisir un compte"
                                PdfScopeMode.BY_PAYEE -> selectedPayee ?: "Choisir un bénéficiaire"
                                PdfScopeMode.BY_CATEGORY -> selectedCategory?.let {
                                    "${WalletCategories.getIcon(it, customCategories)} $it"
                                } ?: "Choisir une catégorie"
                                PdfScopeMode.ALL -> ""
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    onClick = { selectorExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = theme.backgroundColor,
                                    border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = theme.primaryColor
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor)
                                    }
                                }
                                DropdownMenu(
                                    expanded = selectorExpanded,
                                    onDismissRequest = { selectorExpanded = false }
                                ) {
                                    when (scopeMode) {
                                        PdfScopeMode.BY_ACCOUNT -> {
                                            accounts.forEach { acc ->
                                                DropdownMenuItem(
                                                    text = { Text("${acc.emojiAvatar} ${acc.name} (${Currencies.formatForAccount(acc.balance, acc)})") },
                                                    onClick = {
                                                        selectedAccountId = acc.id
                                                        selectorExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                        PdfScopeMode.BY_PAYEE -> {
                                            availablePayees.forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text(p) },
                                                    onClick = {
                                                        selectedPayee = p
                                                        selectorExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                        PdfScopeMode.BY_CATEGORY -> {
                                            allCategories.forEach { (cName, cIcon) ->
                                                DropdownMenuItem(
                                                    text = { Text("$cIcon $cName") },
                                                    onClick = {
                                                        selectedCategory = cName
                                                        selectorExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                        PdfScopeMode.ALL -> Unit
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Période & Type :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.primaryColor
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
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
                            item {
                                FilterChip(
                                    selected = selectedDateRange == DateRangeFilterType.THIS_MONTH,
                                    onClick = { selectedDateRange = DateRangeFilterType.THIS_MONTH },
                                    label = { Text("Ce mois-ci", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.primaryColor,
                                        selectedLabelColor = theme.onPrimaryColor
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedDateRange == DateRangeFilterType.LAST_MONTH,
                                    onClick = { selectedDateRange = DateRangeFilterType.LAST_MONTH },
                                    label = { Text("Mois dernier", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.primaryColor,
                                        selectedLabelColor = theme.onPrimaryColor
                                    )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedTypeFilter == null,
                                onClick = { selectedTypeFilter = null },
                                label = { Text("Tous", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = theme.onPrimaryColor
                                )
                            )
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.INCOME,
                                onClick = { selectedTypeFilter = TransactionType.INCOME },
                                label = { Text("Revenus", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.EXPENSE,
                                onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                                label = { Text("Dépenses", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFC62828),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val periodTitle = when (selectedDateRange) {
                                    DateRangeFilterType.THIS_MONTH -> "Ce mois-ci"
                                    DateRangeFilterType.LAST_MONTH -> "Mois dernier"
                                    DateRangeFilterType.ALL -> "Toutes les dates"
                                }
                                val scopeSummary = when (scopeMode) {
                                    PdfScopeMode.ALL -> "Tout le portefeuille"
                                    PdfScopeMode.BY_ACCOUNT -> "Compte : ${accountsMap[selectedAccountId]?.name ?: ""}"
                                    PdfScopeMode.BY_PAYEE -> "Bénéficiaire : ${selectedPayee ?: ""}"
                                    PdfScopeMode.BY_CATEGORY -> "Catégorie : ${selectedCategory ?: ""}"
                                }
                                val file = PdfReportExporter.generateTransactionsPdf(
                                    context = context,
                                    transactions = filteredForExport,
                                    accountMap = accountsMap,
                                    title = "Rapport Financier Balatec Wallet",
                                    periodLabel = periodTitle,
                                    filterSummary = scopeSummary
                                )
                                if (file != null) {
                                    PdfReportExporter.sharePdf(context, file)
                                    statusMessage = if (filteredForExport.isEmpty()) {
                                        "Rapport PDF vide (0 transaction) généré !"
                                    } else {
                                        "Rapport PDF (${filteredForExport.size} lignes) généré !"
                                    }
                                } else {
                                    statusMessage = "Impossible de générer le fichier PDF."
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
                            Text("${stringResource(R.string.export_pdf)} (${filteredForExport.size})", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        isProcessing = true
                                        val csv = onExportCsv()
                                        copyToClipboard("Balatec Ledger CSV", csv)
                                        shareText("Balatec Wallet Ledger CSV", csv)
                                        statusMessage = "Grand Livre CSV exporté (${transactions.size} ligne(s)) !"
                                    } catch (_: Exception) {
                                        statusMessage = "Échec de l'exportation CSV."
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.export_csv), fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        isProcessing = true
                                        val json = onExportJson()
                                        copyToClipboard("Balatec Wallet Backup JSON", json)
                                        statusMessage = "Sauvegarde JSON générée (${transactions.size} transaction(s)) et copiée !"
                                    } catch (_: Exception) {
                                        statusMessage = "Échec de l'exportation JSON."
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.export_json), fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Note: Le format PDF est destiné à l'exportation et à l'impression. Pour restaurer des données, collez le contenu JSON de sauvegarde ci-dessous.",
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

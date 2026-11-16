package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.data.model.AccountType
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TwtAccountHelper
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.compose.material.icons.filled.CurrencyExchange
import com.example.data.model.AccountEntity
import com.example.data.model.Currencies
import com.example.data.model.PresetFamilyContacts
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WalletCategories
import com.example.ui.components.ArtisanCard
import com.example.ui.theme.LocalArtisanTheme
import com.example.util.PdfReportExporter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class DateRangeFilterType {
    ALL,
    THIS_MONTH,
    LAST_MONTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    customCategories: List<Pair<String, String>> = emptyList(),
    baseCurrency: String,
    isAdminTwtVisible: Boolean = false,
    onAddTransaction: (TransactionEntity) -> Unit,
    onSaveRemittanceFx: (
        usdAccountId: String,
        cfaAccountId: String,
        amountSentUsd: BigDecimal,
        feeUsd: BigDecimal,
        rateUsdToCfa: BigDecimal,
        payee: String,
        category: String,
        timestamp: Long,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onUpdateTransaction: (oldTx: TransactionEntity, newTx: TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onDeleteTransactions: (List<TransactionEntity>) -> Unit = { list -> list.forEach { onDeleteTransaction(it) } },
    onQuickCreateAccount: (name: String, type: AccountType, balance: Double, currency: String, emoji: String, onCreated: (String) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onAddRecurring: (RecurringTransactionEntity) -> Unit = {},
    onToggleRecurringActive: (id: String, active: Boolean) -> Unit = { _, _ -> },
    onExecuteRecurringNow: (id: String) -> Unit = {},
    onDeleteRecurring: (id: String) -> Unit = {},
    onProcessDueRecurring: () -> Unit = {}
) {
    val context = LocalContext.current
    val theme = LocalArtisanTheme.current
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf(DateRangeFilterType.ALL) }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) } // null = All
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedAccountFilter by remember { mutableStateOf<String?>(null) }
    var selectedFamilyOrCityFilter by remember { mutableStateOf<String?>(null) }
    var minAmountFilter by remember { mutableStateOf<Double?>(null) }
    var maxAmountFilter by remember { mutableStateOf<Double?>(null) }

    // Multi-Selection State
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showQuickTwtSheet by remember { mutableStateOf(false) }
    var showRecurringSheet by remember { mutableStateOf(false) }
    var txToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Date range timestamps
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

    // Filter transactions in real-time
    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedDateRange,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedAccountFilter,
        selectedFamilyOrCityFilter,
        minAmountFilter,
        maxAmountFilter
    ) {
        transactions.filter { tx ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                tx.payee.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)
            }

            val matchesDate = when (selectedDateRange) {
                DateRangeFilterType.ALL -> true
                DateRangeFilterType.THIS_MONTH -> tx.timestamp >= startOfThisMonth
                DateRangeFilterType.LAST_MONTH -> tx.timestamp in startOfLastMonth until endOfLastMonth
            }

            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesCategory = selectedCategoryFilter == null || tx.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesAccount = selectedAccountFilter == null || tx.accountId == selectedAccountFilter
            val matchesFamilyOrCity = selectedFamilyOrCityFilter.let { filterLabel ->
                if (filterLabel.isNullOrBlank()) true else {
                    tx.payee.contains(filterLabel, ignoreCase = true) ||
                        tx.category.equals(filterLabel, ignoreCase = true) ||
                        tx.notes.contains(filterLabel, ignoreCase = true)
                }
            }
            val matchesMin = minAmountFilter == null || tx.amount >= minAmountFilter!!
            val matchesMax = maxAmountFilter == null || tx.amount <= maxAmountFilter!!

            matchesQuery && matchesDate && matchesType && matchesCategory && matchesAccount && matchesFamilyOrCity && matchesMin && matchesMax
        }
    }

    val isFilterActive = selectedTypeFilter != null ||
            selectedCategoryFilter != null ||
            selectedAccountFilter != null ||
            selectedFamilyOrCityFilter != null ||
            minAmountFilter != null ||
            maxAmountFilter != null ||
            selectedDateRange != DateRangeFilterType.ALL ||
            searchQuery.isNotBlank()

    Scaffold(
        containerColor = theme.backgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = theme.primaryColor,
                    contentColor = theme.onPrimaryColor,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_transaction))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Multi-Selection Header Toolbar
                if (isSelectionMode) {
                    Surface(
                        color = theme.primaryColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    isSelectionMode = false
                                    selectedIds.clear()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.onPrimaryColor)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${selectedIds.size} ${stringResource(R.string.selected_items)}",
                                    color = theme.onPrimaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        if (selectedIds.size == filteredTransactions.size) {
                                            selectedIds.clear()
                                        } else {
                                            selectedIds.clear()
                                            selectedIds.addAll(filteredTransactions.map { it.id })
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (selectedIds.size == filteredTransactions.size && filteredTransactions.isNotEmpty()) {
                                            stringResource(R.string.deselect_all)
                                        } else {
                                            stringResource(R.string.select_all)
                                        },
                                        color = theme.onPrimaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Global Search Bar placed vertically on top (full width, no crowding)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_hint), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.stitchingColor) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("search_text_field"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Action Tools Bar directly below Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Advanced Filter Sheet Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showFilterSheet = true }
                                .testTag("btn_filter_sheet"),
                            color = if (isFilterActive) theme.primaryColor else theme.surfaceColor,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.filter),
                                    tint = if (isFilterActive) theme.onPrimaryColor else theme.primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.filter),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isFilterActive) theme.onPrimaryColor else theme.primaryColor
                                )
                            }
                        }

                        // Direct Export PDF of Current View
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val periodTitle = when (selectedDateRange) {
                                        DateRangeFilterType.THIS_MONTH -> "Ce mois-ci"
                                        DateRangeFilterType.LAST_MONTH -> "Mois dernier"
                                        DateRangeFilterType.ALL -> "Toutes les dates"
                                    }
                                    val file = PdfReportExporter.generateTransactionsPdf(
                                        context = context,
                                        transactions = filteredTransactions,
                                        accountMap = accountsMap,
                                        title = "Grand Livre - Balatec Secure",
                                        periodLabel = periodTitle
                                    )
                                    if (file != null) {
                                        PdfReportExporter.sharePdf(context, file)
                                    }
                                }
                                .testTag("btn_export_pdf"),
                            color = theme.surfaceColor,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = stringResource(R.string.export_pdf),
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PDF",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.primaryColor
                                )
                            }
                        }

                        // Multi-Selection Toggle Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isSelectionMode = true }
                                .testTag("btn_select_mode"),
                            color = theme.surfaceColor,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.SelectAll,
                                    contentDescription = stringResource(R.string.select_all),
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.select_all).take(7),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.primaryColor
                                )
                            }
                        }

                        // Recurring Transactions / Scheduled Auto-Ledger Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showRecurringSheet = true }
                                .testTag("btn_recurring_schedules"),
                            color = if (recurringList.any { it.isActive }) theme.primaryContainer else theme.surfaceColor,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = stringResource(R.string.recurring_schedule),
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.primaryColor
                                )
                            }
                        }
                    }

                    // Quick Add TWT Shortcut Button (STRICTLY visible to Primary Admin ONLY)
                    if (isAdminTwtVisible && accounts.any { TwtAccountHelper.isTwtAccount(it) }) {
                        Surface(
                            onClick = { showQuickTwtSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("btn_quick_add_twt"),
                            shape = RoundedCornerShape(12.dp),
                            color = theme.primaryColor,
                            border = BorderStroke(1.dp, theme.metallicAccent.copy(alpha = 0.85f)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = theme.metallicAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.quick_add_twt, TwtAccountHelper.currentTwtAccountName()),
                                        color = theme.onPrimaryColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "$ USD • Bonus",
                                        color = theme.metallicAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Accounts Total row on top of the main screen (only show accounts with activities)
                    val activeAccounts = remember(accounts, transactions) {
                        accounts.filter { acc ->
                            acc.balance != 0.0 || transactions.any { it.accountId == acc.id }
                        }
                    }

                    if (activeAccounts.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(activeAccounts, key = { it.id }) { acc ->
                                val isSelectedAcc = selectedAccountFilter == acc.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedAccountFilter = if (isSelectedAcc) null else acc.id
                                        }
                                        .testTag("account_chip_${acc.id}"),
                                    color = if (isSelectedAcc) theme.primaryColor else theme.surfaceColor,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelectedAcc) theme.primaryColor else theme.stitchingColor.copy(alpha = 0.25f)
                                    ),
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = acc.emojiAvatar, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = if (TwtAccountHelper.isTwtAccount(acc)) TwtAccountHelper.currentTwtAccountName() else acc.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelectedAcc) theme.onPrimaryColor else theme.primaryColor
                                            )
                                            Text(
                                                text = Currencies.formatForAccount(acc.balance, acc),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelectedAcc) theme.onPrimaryColor else if (acc.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Date Pills (Ce mois-ci, Mois dernier, Tout) & Type Filters
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Quick Pills
                    item {
                        FilterChip(
                            selected = selectedDateRange == DateRangeFilterType.THIS_MONTH,
                            onClick = {
                                selectedDateRange = if (selectedDateRange == DateRangeFilterType.THIS_MONTH) DateRangeFilterType.ALL else DateRangeFilterType.THIS_MONTH
                            },
                            label = { Text(stringResource(R.string.filter_this_month)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryColor,
                                selectedLabelColor = theme.onPrimaryColor
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateRange == DateRangeFilterType.LAST_MONTH,
                            onClick = {
                                selectedDateRange = if (selectedDateRange == DateRangeFilterType.LAST_MONTH) DateRangeFilterType.ALL else DateRangeFilterType.LAST_MONTH
                            },
                            label = { Text(stringResource(R.string.filter_last_month)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryColor,
                                selectedLabelColor = theme.onPrimaryColor
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDateRange == DateRangeFilterType.ALL && selectedTypeFilter == null,
                            onClick = {
                                selectedDateRange = DateRangeFilterType.ALL
                                selectedTypeFilter = null
                            },
                            label = { Text(stringResource(R.string.filter_all_time)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryContainer,
                                selectedLabelColor = theme.primaryColor
                            )
                        )
                    }

                    // Income / Expense Pills
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.INCOME,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                            },
                            label = { Text(stringResource(R.string.filter_income)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF2E7D32)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.EXPENSE,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                            },
                            label = { Text(stringResource(R.string.filter_expense)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFEBEE),
                                selectedLabelColor = Color(0xFFC62828)
                            )
                        )
                    }

                    if (selectedCategoryFilter != null) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text(selectedCategoryFilter!!) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    if (isFilterActive) {
                        item {
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedDateRange = DateRangeFilterType.ALL
                                selectedTypeFilter = null
                                selectedCategoryFilter = null
                                selectedAccountFilter = null
                                selectedFamilyOrCityFilter = null
                                minAmountFilter = null
                                maxAmountFilter = null
                            }) {
                                Text(
                                    stringResource(R.string.clear_filters),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // Family Contact (21 Presets) & City (Bamako, Abidjan) Quick Filter Chip Bar
                if (!isSelectionMode) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("family_city_filter_bar"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(PresetFamilyContacts.CITIES, key = { "city_${it.first}" }) { (city, flag) ->
                            val isSel = selectedFamilyOrCityFilter.equals(city, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedFamilyOrCityFilter = if (isSel) null else city
                                },
                                label = { Text("$flag $city", fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = theme.onPrimaryColor
                                )
                            )
                        }
                        items(PresetFamilyContacts.NAMES, key = { "fam_$it" }) { contactName ->
                            val isSel = selectedFamilyOrCityFilter.equals(contactName, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedFamilyOrCityFilter = if (isSel) null else contactName
                                },
                                label = { Text("👤 $contactName", fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = theme.onPrimaryColor
                                )
                            )
                        }
                    }

                    // Filtered Total in CFA Banner when Family Contact or City filter is active
                    selectedFamilyOrCityFilter?.let { activeLabel ->
                        val cfaMatching = remember(filteredTransactions, accountsMap) {
                            filteredTransactions.filter { tx ->
                                val acc = accountsMap[tx.accountId]
                                Currencies.effectiveCurrencyCode(acc, "CFA") == "CFA"
                            }
                        }
                        val cfaTotalBd = remember(cfaMatching) {
                            Currencies.sumAmounts(cfaMatching.map { abs(it.amount) })
                        }
                        val cfaIncomeBd = remember(cfaMatching) {
                            Currencies.sumAmounts(cfaMatching.filter { it.type == TransactionType.INCOME }.map { it.amount })
                        }
                        val cfaExpenseBd = remember(cfaMatching) {
                            Currencies.sumAmounts(cfaMatching.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
                        }
                        ArtisanCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("family_city_filtered_total"),
                            containerColor = theme.primaryContainer.copy(alpha = 0.72f),
                            elevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${stringResource(R.string.filtered_cfa_total)} • $activeLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primaryColor
                                    )
                                    Text(
                                        text = "${cfaMatching.size} op. CFA (+${Currencies.format(cfaIncomeBd, "CFA")} / -${Currencies.format(cfaExpenseBd, "CFA")})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.secondaryTextColor
                                    )
                                }
                                Text(
                                    text = Currencies.format(cfaTotalBd, "CFA"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.primaryColor
                                )
                            }
                        }
                    }
                }

                // Transaction List
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.no_transactions),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.no_transactions_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = if (isSelectionMode && selectedIds.isNotEmpty()) 120.dp else 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            val account = accountsMap[tx.accountId]
                            val isSelected = selectedIds.contains(tx.id)

                            if (isSelectionMode) {
                                TransactionSelectableCard(
                                    transaction = tx,
                                    account = account,
                                    customCategories = customCategories,
                                    baseCurrency = baseCurrency,
                                    isSelected = isSelected,
                                    onToggleSelect = {
                                        if (isSelected) selectedIds.remove(tx.id)
                                        else selectedIds.add(tx.id)
                                    }
                                )
                            } else {
                                SwipeToDeleteTransaction(
                                    transaction = tx,
                                    account = account,
                                    customCategories = customCategories,
                                    baseCurrency = baseCurrency,
                                    onEdit = { txToEdit = tx },
                                    onDelete = { onDeleteTransaction(tx) },
                                    onLongClick = {
                                        isSelectionMode = true
                                        selectedIds.add(tx.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Bottom Action Bar when items are selected in Multi-Selection Mode
            AnimatedVisibility(
                visible = isSelectionMode && selectedIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(20.dp)),
                    color = theme.primaryColor,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${selectedIds.size} ${stringResource(R.string.selected_items)}",
                                color = theme.onPrimaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Export Selected to PDF
                            FilledTonalButton(
                                onClick = {
                                    val selectedTxs = filteredTransactions.filter { selectedIds.contains(it.id) }
                                    val file = PdfReportExporter.generateTransactionsPdf(
                                        context = context,
                                        transactions = selectedTxs,
                                        accountMap = accountsMap,
                                        title = "Transactions Sélectionnées",
                                        periodLabel = "Sélection personnalisée (${selectedTxs.size})"
                                    )
                                    if (file != null) {
                                        PdfReportExporter.sharePdf(context, file)
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = theme.onPrimaryColor,
                                    contentColor = theme.primaryColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.export_selected_pdf),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Delete Selected Button
                            IconButton(
                                onClick = { showBulkDeleteConfirm = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD32F2F))
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_selected),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bulk Delete Confirmation Dialog
    if (showBulkDeleteConfirm) {
        val selectedTxs = filteredTransactions.filter { selectedIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_selected)) },
            text = { Text("Êtes-vous sûr de vouloir supprimer les ${selectedIds.size} transactions sélectionnées ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransactions(selectedTxs)
                        selectedIds.clear()
                        isSelectionMode = false
                        showBulkDeleteConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add Transaction Sheet
    if (showAddSheet || showQuickTwtSheet) {
        val isQuickTwt = showQuickTwtSheet
        TransactionFormSheet(
            transaction = null,
            accounts = accounts,
            customCategories = customCategories,
            baseCurrency = baseCurrency,
            initialTwtMode = isQuickTwt,
            onDismiss = {
                showAddSheet = false
                showQuickTwtSheet = false
            },
            onSave = { newTx ->
                onAddTransaction(newTx)
                showAddSheet = false
                showQuickTwtSheet = false
            },
            onSaveRemittanceFx = { usdAccId, cfaAccId, amtUsd, feeUsd, rate, payeeName, cat, ts, txNotes ->
                onSaveRemittanceFx(usdAccId, cfaAccId, amtUsd, feeUsd, rate, payeeName, cat, ts, txNotes)
                showAddSheet = false
                showQuickTwtSheet = false
            },
            onQuickCreateAccount = onQuickCreateAccount,
            onScheduleRecurring = { newRec ->
                onAddRecurring(newRec)
            }
        )
    }

    // Edit Transaction Sheet
    txToEdit?.let { oldTx ->
        TransactionFormSheet(
            transaction = oldTx,
            accounts = accounts,
            customCategories = customCategories,
            baseCurrency = baseCurrency,
            initialTwtMode = false,
            onDismiss = { txToEdit = null },
            onSave = { updatedTx ->
                onUpdateTransaction(oldTx, updatedTx)
                txToEdit = null
            },
            onQuickCreateAccount = onQuickCreateAccount
        )
    }

    // Delete Confirmation
    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text(stringResource(R.string.delete_transaction)) },
            text = { Text(stringResource(R.string.confirm_delete_tx)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTransaction(tx)
                    txToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Advanced Filter Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            selectedCategory = selectedCategoryFilter,
            selectedAccount = selectedAccountFilter,
            minAmount = minAmountFilter,
            maxAmount = maxAmountFilter,
            accounts = accounts,
            customCategories = customCategories,
            onApply = { cat, acc, min, max ->
                selectedCategoryFilter = cat
                selectedAccountFilter = acc
                minAmountFilter = min
                maxAmountFilter = max
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Recurring / Scheduled Auto-Ledger Sheet
    if (showRecurringSheet) {
        RecurringTransactionsSheet(
            recurringList = recurringList,
            accounts = accounts,
            customCategories = customCategories,
            baseCurrency = baseCurrency,
            onAddRecurring = { newRec ->
                onAddRecurring(newRec)
            },
            onToggleActive = onToggleRecurringActive,
            onExecuteNow = onExecuteRecurringNow,
            onDeleteRecurring = onDeleteRecurring,
            onProcessDueNow = onProcessDueRecurring,
            onDismissRequest = { showRecurringSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteTransaction(
    transaction: TransactionEntity,
    account: AccountEntity?,
    customCategories: List<Pair<String, String>> = emptyList(),
    baseCurrency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD32F2F), RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Supprimer",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    ) {
        TransactionItemCard(
            transaction = transaction,
            account = account,
            customCategories = customCategories,
            baseCurrency = baseCurrency,
            onEdit = onEdit,
            onDelete = onDelete,
            onLongClick = onLongClick
        )
    }
}

@Composable
fun TransactionSelectableCard(
    transaction: TransactionEntity,
    account: AccountEntity?,
    customCategories: List<Pair<String, String>> = emptyList(),
    baseCurrency: String,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRENCH) }
    val formattedDate = remember(transaction.timestamp) { sdf.format(Date(transaction.timestamp)) }
    val isIncome = transaction.type == TransactionType.INCOME

    ArtisanCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
            .testTag("tx_item_selectable_${transaction.id}"),
        containerColor = if (isSelected) theme.primaryContainer.copy(alpha = 0.5f) else theme.surfaceColor,
        elevation = if (isSelected) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = theme.primaryColor,
                    checkmarkColor = theme.onPrimaryColor
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isIncome) Color(0xFFE8F5E9) else theme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = WalletCategories.getIcon(transaction.category, customCategories),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.payee,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    if (account != null) {
                        val accDisplayName = if (TwtAccountHelper.isTwtAccount(account)) TwtAccountHelper.currentTwtAccountName() else account.name
                        Text(
                            text = " • ${account.emojiAvatar} $accDisplayName",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.stitchingColor,
                            maxLines = 1
                        )
                    }
                }
            }

            val prefix = if (isIncome) "+" else "-"
            val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

            Text(
                text = "$prefix${Currencies.formatForAccount(transaction.amount, account, baseCurrency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    account: AccountEntity?,
    customCategories: List<Pair<String, String>> = emptyList(),
    baseCurrency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRENCH) }
    val formattedDate = remember(transaction.timestamp) { sdf.format(Date(transaction.timestamp)) }
    val isIncome = transaction.type == TransactionType.INCOME
    var showMenu by remember { mutableStateOf(false) }

    ArtisanCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${transaction.id}"),
        containerColor = theme.surfaceColor,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isIncome) Color(0xFFE8F5E9) else theme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = WalletCategories.getIcon(transaction.category, customCategories),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.payee,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    if (account != null) {
                        val accDisplayName = if (TwtAccountHelper.isTwtAccount(account)) TwtAccountHelper.currentTwtAccountName() else account.name
                        Text(
                            text = " • ${account.emojiAvatar} $accDisplayName",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.stitchingColor,
                            maxLines = 1
                        )
                    }
                }
                if (transaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val prefix = if (isIncome) "+" else "-"
            val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${Currencies.formatForAccount(transaction.amount, account, baseCurrency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_transaction)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_transaction), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarPickerModal(
    selectedDateYmd: String,
    onDateSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val parts = remember(selectedDateYmd) {
        selectedDateYmd.split("-").mapNotNull { it.toIntOrNull() }
    }
    val selYear = if (parts.size == 3) parts[0] else Calendar.getInstance().get(Calendar.YEAR)
    val selMonth = if (parts.size == 3) parts[1] - 1 else Calendar.getInstance().get(Calendar.MONTH)
    val selDay = if (parts.size == 3) parts[2] else Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    var currentDisplayYear by remember { mutableIntStateOf(selYear) }
    var currentDisplayMonth by remember { mutableIntStateOf(selMonth) }

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val currentLocale = Locale.getDefault()
    val monthCal = remember(currentDisplayYear, currentDisplayMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentDisplayYear)
            set(Calendar.MONTH, currentDisplayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthFormatter = remember(currentLocale) {
        SimpleDateFormat("MMMM yyyy", currentLocale)
    }
    val monthTitle = remember(monthCal, monthFormatter) {
        val formatted = monthFormatter.format(monthCal.time)
        formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString() }
    }

    // Total days in current month
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // First day of week offset (Monday = 0 ... Sunday = 6)
    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2
    val leadingEmptyDays = (firstDayOfWeek + 5) % 7

    val weekdayShortNames = remember(currentLocale) {
        val testCal = Calendar.getInstance()
        val days = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )
        val sdf = SimpleDateFormat("EEE", currentLocale)
        days.map { d ->
            testCal.set(Calendar.DAY_OF_WEEK, d)
            sdf.format(testCal.time).take(3).uppercase(currentLocale)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = theme.surfaceColor,
            border = BorderStroke(1.5.dp, theme.stitchingColor.copy(alpha = 0.6f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (currentDisplayMonth == 0) {
                                currentDisplayMonth = 11
                                currentDisplayYear -= 1
                            } else {
                                currentDisplayMonth -= 1
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mois précédent",
                            tint = theme.primaryColor
                        )
                    }

                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor
                    )

                    IconButton(
                        onClick = {
                            if (currentDisplayMonth == 11) {
                                currentDisplayMonth = 0
                                currentDisplayYear += 1
                            } else {
                                currentDisplayMonth += 1
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mois suivant",
                            tint = theme.primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekday Header Row (7 Columns: LUN, MAR, MER...)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekdayShortNames.forEach { dayName ->
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 7-Column Days Grid
                val totalCells = leadingEmptyDays + daysInMonth
                val totalRows = (totalCells + 6) / 7

                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - leadingEmptyDays + 1

                            if (dayNumber in 1..daysInMonth) {
                                val isSelected = currentDisplayYear == selYear &&
                                        currentDisplayMonth == selMonth &&
                                        dayNumber == selDay
                                val isToday = currentDisplayYear == todayYear &&
                                        currentDisplayMonth == todayMonth &&
                                        dayNumber == todayDay

                                val cellColor = when {
                                    isSelected -> theme.primaryColor
                                    else -> Color.Transparent
                                }
                                val textColor = when {
                                    isSelected -> theme.onPrimaryColor
                                    else -> theme.primaryColor
                                }
                                val borderStroke = when {
                                    isSelected -> null
                                    isToday -> BorderStroke(1.5.dp, theme.stitchingColor)
                                    else -> null
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(cellColor)
                                        .then(if (borderStroke != null) Modifier.border(borderStroke, CircleShape) else Modifier)
                                        .clickable {
                                            val ymd = String.format(
                                                Locale.US,
                                                "%04d-%02d-%02d",
                                                currentDisplayYear,
                                                currentDisplayMonth + 1,
                                                dayNumber
                                            )
                                            onDateSelected(ymd)
                                            onDismissRequest()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(38.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = stringResource(R.string.close),
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionFormSheet(
    transaction: TransactionEntity?,
    accounts: List<AccountEntity>,
    customCategories: List<Pair<String, String>> = emptyList(),
    baseCurrency: String,
    initialTwtMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onSaveRemittanceFx: ((
        usdAccountId: String,
        cfaAccountId: String,
        amountSentUsd: BigDecimal,
        feeUsd: BigDecimal,
        rateUsdToCfa: BigDecimal,
        payee: String,
        category: String,
        timestamp: Long,
        notes: String
    ) -> Unit)? = null,
    onQuickCreateAccount: ((name: String, type: AccountType, balance: Double, currency: String, emoji: String, onCreated: (String) -> Unit) -> Unit)? = null,
    onScheduleRecurring: ((RecurringTransactionEntity) -> Unit)? = null
) {
    val theme = LocalArtisanTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val allCategories = remember(customCategories) { WalletCategories.combined(customCategories) }

    val twtAccount = remember(accounts) { accounts.firstOrNull { TwtAccountHelper.isTwtAccount(it) } }
    val defaultNonTwtCfa = remember(accounts) {
        accounts.firstOrNull { !TwtAccountHelper.isTwtAccount(it) && Currencies.effectiveCurrencyCode(it, "CFA") == "CFA" }
            ?: accounts.firstOrNull()
    }
    val defaultUsdAccount = remember(accounts) {
        accounts.firstOrNull { Currencies.effectiveCurrencyCode(it, "CFA") == "USD" }
            ?: twtAccount
            ?: accounts.firstOrNull()
    }

    var isFxMode by remember { mutableStateOf(false) }
    var fxAmountUsdText by remember { mutableStateOf("") }
    var fxFeeUsdText by remember { mutableStateOf("0.00") }
    var fxRateText by remember { mutableStateOf("605") }
    var fxUsdAccountId by remember(defaultUsdAccount) { mutableStateOf(defaultUsdAccount?.id ?: "") }
    var fxCfaAccountId by remember(defaultNonTwtCfa) { mutableStateOf(defaultNonTwtCfa?.id ?: "") }
    var showFxUsdPickerDialog by remember { mutableStateOf(false) }
    var showFxCfaPickerDialog by remember { mutableStateOf(false) }

    var type by remember {
        mutableStateOf(
            transaction?.type ?: if (initialTwtMode) TransactionType.INCOME else TransactionType.EXPENSE
        )
    }
    var amountText by remember { mutableStateOf(transaction?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var payee by remember { mutableStateOf(transaction?.payee ?: if (initialTwtMode) "TWT Bonus" else "") }
    var category by remember {
        mutableStateOf(
            transaction?.category ?: if (initialTwtMode) "Bonus" else "Famille"
        )
    }
    var notes by remember { mutableStateOf(transaction?.notes ?: "") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }

    val initialDateYmd = remember(transaction) {
        val cal = Calendar.getInstance()
        if (transaction != null) {
            cal.timeInMillis = transaction.timestamp
        }
        String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
    var selectedDateYmd by remember { mutableStateOf(initialDateYmd) }
    var showCalendarModal by remember { mutableStateOf(false) }

    var accountId by remember {
        mutableStateOf(
            transaction?.accountId ?: if (initialTwtMode && twtAccount != null) {
                twtAccount.id
            } else {
                defaultNonTwtCfa?.id ?: ""
            }
        )
    }

    val selectedAccount = accounts.firstOrNull { it.id == accountId }
    val isTwtSelected = !isFxMode && TwtAccountHelper.isTwtAccount(selectedAccount)

    var selectedCurrency by remember {
        mutableStateOf(
            when {
                isTwtSelected || initialTwtMode -> "USD"
                transaction != null -> Currencies.effectiveCurrencyCode(
                    accounts.firstOrNull { it.id == transaction.accountId },
                    "CFA"
                )
                else -> Currencies.effectiveCurrencyCode(defaultNonTwtCfa, "CFA")
            }
        )
    }

    // Keep TWT strictly locked to USD ($) whenever TWT account is selected
    LaunchedEffect(accountId, accounts) {
        val acc = accounts.firstOrNull { it.id == accountId }
        if (TwtAccountHelper.isTwtAccount(acc)) {
            selectedCurrency = "USD"
            if (transaction == null && category == "Famille") {
                category = "Bonus"
            }
        } else if (acc != null) {
            selectedCurrency = Currencies.effectiveCurrencyCode(acc, "CFA")
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCategoryPickerDialog by remember { mutableStateOf(false) }
    var showAccountPickerDialog by remember { mutableStateOf(false) }
    var showInlineAddAccountSheet by remember { mutableStateOf(false) }

    val amountBringIntoView = remember { BringIntoViewRequester() }
    val notesBringIntoView = remember { BringIntoViewRequester() }

    val parsedFxAmt = Currencies.parseBigDecimal(fxAmountUsdText) ?: BigDecimal.ZERO
    val parsedFxFee = Currencies.parseBigDecimal(fxFeeUsdText) ?: BigDecimal.ZERO
    val parsedFxRate = Currencies.parseBigDecimal(fxRateText) ?: BigDecimal.ZERO
    val fxTotalUsd = if (parsedFxAmt.compareTo(BigDecimal.ZERO) > 0 && parsedFxFee.compareTo(BigDecimal.ZERO) >= 0) {
        Currencies.computeTotalUsdDeduction(parsedFxAmt, parsedFxFee)
    } else {
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }
    val fxNetCfa = if (parsedFxAmt.compareTo(BigDecimal.ZERO) > 0 && parsedFxRate.compareTo(BigDecimal.ZERO) > 0) {
        Currencies.computeNetCfaFromFx(parsedFxAmt, parsedFxRate)
    } else {
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.surfaceColor
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (transaction == null) {
                    when {
                        isFxMode -> stringResource(R.string.fx_mode_toggle)
                        isTwtSelected -> "Ajouter • ${TwtAccountHelper.currentTwtAccountName()} ($ USD)"
                        else -> stringResource(R.string.add_transaction)
                    }
                } else {
                    stringResource(R.string.edit_transaction)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = theme.primaryColor
            )

            // Optional FX / Remittance Mode Toggle (new transactions only)
            if (transaction == null && onSaveRemittanceFx != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFxMode) theme.primaryContainer else theme.backgroundColor,
                    border = BorderStroke(
                        1.dp,
                        if (isFxMode) theme.primaryColor else theme.stitchingColor.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.fx_mode_toggle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor
                                )
                                Text(
                                    text = stringResource(R.string.fx_mode_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondaryTextColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = isFxMode,
                            onCheckedChange = { enabled ->
                                isFxMode = enabled
                                errorMessage = null
                                if (enabled && category == "Bonus") {
                                    category = "Famille"
                                }
                            },
                            modifier = Modifier.testTag("switch_fx_remittance_mode"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = theme.onPrimaryColor,
                                checkedTrackColor = theme.primaryColor
                            )
                        )
                    }
                }
            }

            if (!isFxMode) {
                // Type Toggle: Income vs Expense
                TabRow(
                    selectedTabIndex = if (type == TransactionType.EXPENSE) 0 else 1,
                    containerColor = theme.backgroundColor,
                    contentColor = theme.primaryColor,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        text = { Text(stringResource(R.string.filter_expense), fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        text = { Text(stringResource(R.string.filter_income), fontWeight = FontWeight.Bold) }
                    )
                }

                // Amount Input with Quick Currency Switcher Pill (Locked to $ when TWT is selected)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMessage = null },
                    label = { Text(stringResource(R.string.amount)) },
                    prefix = if (selectedCurrency == "USD") {
                        { Text("$ ", fontWeight = FontWeight.Bold, color = theme.primaryColor) }
                    } else null,
                    suffix = if (selectedCurrency == "CFA") {
                        { Text(" CFA", fontWeight = FontWeight.Bold, color = theme.stitchingColor) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewRequester(amountBringIntoView)
                        .onFocusEvent { state ->
                            if (state.isFocused) {
                                coroutineScope.launch { amountBringIntoView.bringIntoView() }
                            }
                        }
                        .testTag("tx_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    if (isTwtSelected) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$ USD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = theme.primaryColor
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedCurrency == "CFA") theme.primaryColor else Color.Transparent)
                                    .clickable {
                                        selectedCurrency = "CFA"
                                        val cfaAcc = accounts.firstOrNull {
                                            !TwtAccountHelper.isTwtAccount(it) && Currencies.effectiveCurrencyCode(it, "CFA") == "CFA"
                                        }
                                        if (cfaAcc != null) accountId = cfaAcc.id
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "CFA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedCurrency == "CFA") theme.onPrimaryColor else theme.primaryColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedCurrency == "USD") theme.primaryColor else Color.Transparent)
                                    .clickable {
                                        selectedCurrency = "USD"
                                        val usdAcc = accounts.firstOrNull { Currencies.effectiveCurrencyCode(it, "CFA") == "USD" }
                                        if (usdAcc != null) accountId = usdAcc.id
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "$ USD",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedCurrency == "USD") theme.onPrimaryColor else theme.primaryColor
                                )
                            }
                        }
                    }
                }
            }
            } else {
                // FX / Remittance Mode Inputs (Amount USD, Fee USD, Exchange Rate -> Live Net Received CFA)
                OutlinedTextField(
                    value = fxAmountUsdText,
                    onValueChange = { fxAmountUsdText = it; errorMessage = null },
                    label = { Text(stringResource(R.string.fx_amount_sent_usd)) },
                    prefix = { Text("$ ", fontWeight = FontWeight.Bold, color = theme.primaryColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fx_amount_usd_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fxFeeUsdText,
                        onValueChange = { fxFeeUsdText = it; errorMessage = null },
                        label = { Text(stringResource(R.string.fx_fee_usd)) },
                        prefix = { Text("$ ", fontWeight = FontWeight.Bold, color = theme.primaryColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fx_fee_usd_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fxRateText,
                        onValueChange = { fxRateText = it; errorMessage = null },
                        label = { Text(stringResource(R.string.fx_exchange_rate)) },
                        suffix = { Text(" CFA", fontWeight = FontWeight.Bold, color = theme.stitchingColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fx_rate_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Live Auto-Calculated FX Preview (BigDecimal exact)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = theme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, theme.metallicAccent.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fx_calculation_preview")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.fx_total_deducted_usd),
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.secondaryTextColor
                            )
                            Text(
                                text = "-${Currencies.format(fxTotalUsd, "USD")}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }
                        HorizontalDivider(color = theme.stitchingColor.copy(alpha = 0.25f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.fx_net_received_cfa),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                            Text(
                                text = "+${Currencies.format(fxNetCfa, "CFA")}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // Date Field & Quick-Select Pills ('Today' / 'Aujourd'hui' and 'Yesterday' / 'Hier')
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showCalendarModal = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.date),
                            tint = theme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = selectedDateYmd,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        val cal = Calendar.getInstance()
                        selectedDateYmd = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stringResource(R.string.today),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                    )
                }

                Surface(
                    onClick = {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                        selectedDateYmd = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stringResource(R.string.yesterday),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                    )
                }
            }

            // Frequent Contacts Quick-Chip Bar (21 Family Members • Auto-selects Famille & CFA)
            if (!isTwtSelected) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Bénéficiaires Fréquents (Famille • CFA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(PresetFamilyContacts.NAMES) { contactName ->
                            val isSelected = payee.equals(contactName, ignoreCase = true)
                            SuggestionChip(
                                onClick = {
                                    payee = contactName
                                    category = "Famille"
                                    if (!isFxMode) {
                                        selectedCurrency = "CFA"
                                        val cfaAcc = accounts.firstOrNull {
                                            !TwtAccountHelper.isTwtAccount(it) && Currencies.effectiveCurrencyCode(it, "CFA") == "CFA"
                                        }
                                        if (cfaAcc != null) accountId = cfaAcc.id
                                    }
                                },
                                label = {
                                    Text(
                                        contactName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) theme.primaryColor else theme.backgroundColor,
                                    labelColor = if (isSelected) theme.onPrimaryColor else theme.primaryColor
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isSelected) theme.primaryColor else theme.stitchingColor.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }

            // Payee / Merchant Text Field
            OutlinedTextField(
                value = payee,
                onValueChange = { payee = it; errorMessage = null },
                label = { Text(stringResource(R.string.payee_merchant)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("tx_payee_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Modal Picker (reliable mobile click target inside scrollable ModalBottomSheet)
            Surface(
                onClick = { showCategoryPickerDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_selector_button"),
                shape = RoundedCornerShape(12.dp),
                color = theme.backgroundColor,
                border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.category),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${WalletCategories.getIcon(category, customCategories)} $category",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.primaryColor
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor)
                }
            }

            if (!isFxMode) {
                // Account Modal Picker + Inline "+ Ajouter un compte" (Fixes dropdown failure on mobile!)
                Surface(
                    onClick = { showAccountPickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_selector_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.select_account),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedAccount?.let {
                                    val displayName = if (TwtAccountHelper.isTwtAccount(it)) TwtAccountHelper.currentTwtAccountName() else it.name
                                    val curLabel = Currencies.effectiveCurrencyCode(it, "CFA")
                                    "${it.emojiAvatar} $displayName ($curLabel)"
                                } ?: stringResource(R.string.select_account),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.primaryColor
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor)
                    }
                }
            } else {
                val selectedUsdAcc = accounts.firstOrNull { it.id == fxUsdAccountId }
                val selectedCfaAcc = accounts.firstOrNull { it.id == fxCfaAccountId }

                Surface(
                    onClick = { showFxUsdPickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fx_source_usd_picker"),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.fx_source_usd_account),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedUsdAcc?.let {
                                    val displayName = if (TwtAccountHelper.isTwtAccount(it)) TwtAccountHelper.currentTwtAccountName() else it.name
                                    "${it.emojiAvatar} $displayName (${Currencies.formatForAccount(it.balance, it)})"
                                } ?: stringResource(R.string.select_account),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.primaryColor
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor)
                    }
                }

                Surface(
                    onClick = { showFxCfaPickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fx_dest_cfa_picker"),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.fx_dest_cfa_account),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedCfaAcc?.let {
                                    val displayName = if (TwtAccountHelper.isTwtAccount(it)) TwtAccountHelper.currentTwtAccountName() else it.name
                                    "${it.emojiAvatar} $displayName (${Currencies.formatForAccount(it.balance, it)})"
                                } ?: stringResource(R.string.select_account),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.primaryColor
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor)
                    }
                }
            }

            // Notes / Memo (auto-scrolls above keyboard when focused)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_memo)) },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(notesBringIntoView)
                    .onFocusEvent { state ->
                        if (state.isFocused) {
                            coroutineScope.launch { notesBringIntoView.bringIntoView() }
                        }
                    }
                    .testTag("tx_notes_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Make Recurring Switch (Scheduled Auto-Ledger, standard mode only)
            if (transaction == null && onScheduleRecurring != null && !isFxMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = theme.backgroundColor.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.make_recurring),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primaryColor
                                    )
                                    Text(
                                        text = "Ex: Loyer mensuel, Salaire récurrent...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.onPrimaryColor,
                                    checkedTrackColor = theme.primaryColor
                                )
                            )
                        }

                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Fréquence automatique :",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(RecurrenceFrequency.values()) { freq ->
                                    FilterChip(
                                        selected = recurrenceFrequency == freq,
                                        onClick = { recurrenceFrequency = freq },
                                        label = { Text(freq.displayName(), fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = theme.primaryColor,
                                            selectedLabelColor = theme.onPrimaryColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    modifier = Modifier.testTag("tx_save_button"),
                    onClick = {
                        val dateParts = selectedDateYmd.split("-").mapNotNull { it.toIntOrNull() }
                        val txCal = Calendar.getInstance()
                        if (dateParts.size == 3) {
                            txCal.set(Calendar.YEAR, dateParts[0])
                            txCal.set(Calendar.MONTH, dateParts[1] - 1)
                            txCal.set(Calendar.DAY_OF_MONTH, dateParts[2])
                            txCal.set(Calendar.HOUR_OF_DAY, 12)
                            txCal.set(Calendar.MINUTE, 0)
                            txCal.set(Calendar.SECOND, 0)
                            txCal.set(Calendar.MILLISECOND, 0)
                        }
                        val finalTimestamp = txCal.timeInMillis

                        if (isFxMode && transaction == null && onSaveRemittanceFx != null) {
                            val amtBd = Currencies.parseBigDecimal(fxAmountUsdText)
                            val feeBd = Currencies.parseBigDecimal(fxFeeUsdText) ?: BigDecimal.ZERO
                            val rateBd = Currencies.parseBigDecimal(fxRateText)

                            if (amtBd == null || amtBd.compareTo(BigDecimal.ZERO) <= 0) {
                                errorMessage = "Veuillez entrer un montant USD envoyé valide (> 0)."
                            } else if (feeBd.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessage = "Les frais de transfert ne peuvent pas être négatifs."
                            } else if (rateBd == null || rateBd.compareTo(BigDecimal.ZERO) <= 0) {
                                errorMessage = "Veuillez entrer un taux de change valide (> 0)."
                            } else if (payee.isBlank()) {
                                errorMessage = "Veuillez entrer un nom de bénéficiaire."
                            } else if (fxUsdAccountId.isBlank() || fxCfaAccountId.isBlank()) {
                                errorMessage = "Veuillez sélectionner les comptes source USD et destination CFA."
                            } else if (accounts.size > 1 && fxUsdAccountId == fxCfaAccountId) {
                                errorMessage = "Sélectionnez un compte source USD distinct du compte destination CFA."
                            } else {
                                onSaveRemittanceFx(
                                    fxUsdAccountId,
                                    fxCfaAccountId,
                                    amtBd.setScale(2, RoundingMode.HALF_UP),
                                    feeBd.setScale(2, RoundingMode.HALF_UP),
                                    rateBd,
                                    payee.trim(),
                                    category,
                                    finalTimestamp,
                                    notes.trim()
                                )
                            }
                        } else {
                            val parsedAmountBd = Currencies.parseBigDecimal(amountText)
                            if (parsedAmountBd == null || parsedAmountBd.compareTo(BigDecimal.ZERO) <= 0) {
                                errorMessage = "Veuillez entrer un montant valide supérieur à 0."
                            } else if (payee.isBlank()) {
                                errorMessage = "Veuillez entrer un nom de bénéficiaire."
                            } else if (accountId.isBlank()) {
                                errorMessage = "Veuillez sélectionner un compte."
                            } else {
                                val roundedAmount = parsedAmountBd.setScale(2, RoundingMode.HALF_UP).toDouble()
                                val entity = transaction?.copy(
                                    accountId = accountId,
                                    type = type,
                                    amount = roundedAmount,
                                    payee = payee.trim(),
                                    category = category,
                                    notes = notes.trim(),
                                    timestamp = finalTimestamp,
                                    updatedAt = System.currentTimeMillis()
                                ) ?: TransactionEntity(
                                    accountId = accountId,
                                    type = type,
                                    amount = roundedAmount,
                                    payee = payee.trim(),
                                    category = category,
                                    notes = notes.trim(),
                                    timestamp = finalTimestamp,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSave(entity)

                                if (isRecurring && onScheduleRecurring != null) {
                                    val now = System.currentTimeMillis()
                                    val nextDue = recurrenceFrequency.computeNextDueDate(now)
                                    val recurringEntity = RecurringTransactionEntity(
                                        accountId = accountId,
                                        type = type,
                                        amount = roundedAmount,
                                        payee = payee.trim(),
                                        category = category,
                                        frequency = recurrenceFrequency,
                                        startDate = now,
                                        lastExecutedDate = now,
                                        nextDueDate = nextDue,
                                        isActive = true,
                                        notes = notes.trim(),
                                        updatedAt = now
                                    )
                                    onScheduleRecurring(recurringEntity)
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showCalendarModal) {
        CalendarPickerModal(
            selectedDateYmd = selectedDateYmd,
            onDateSelected = { newDate ->
                selectedDateYmd = newDate
            },
            onDismissRequest = { showCalendarModal = false }
        )
    }

    if (showCategoryPickerDialog) {
        Dialog(onDismissRequest = { showCategoryPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                        items(allCategories) { (catName, icon) ->
                            val isSel = category.equals(catName, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) theme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        category = catName
                                        if (catName in PresetFamilyContacts.REMITTANCE_FAMILY_CATEGORIES && !isTwtSelected) {
                                            selectedCurrency = "CFA"
                                            val cfaAcc = accounts.firstOrNull {
                                                !TwtAccountHelper.isTwtAccount(it) && Currencies.effectiveCurrencyCode(it, "CFA") == "CFA"
                                            }
                                            if (cfaAcc != null) accountId = cfaAcc.id
                                        }
                                        showCategoryPickerDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    catName,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = theme.primaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCategoryPickerDialog = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }

    if (showAccountPickerDialog) {
        Dialog(onDismissRequest = { showAccountPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.select_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (onQuickCreateAccount != null) {
                        Button(
                            onClick = {
                                showAccountPickerDialog = false
                                showInlineAddAccountSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_inline_add_account"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primaryColor,
                                contentColor = theme.onPrimaryColor
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.add_account), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = theme.cardBorderColor.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        items(accounts, key = { it.id }) { acc ->
                            val isSel = acc.id == accountId
                            val displayName = if (TwtAccountHelper.isTwtAccount(acc)) TwtAccountHelper.currentTwtAccountName() else acc.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) theme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        accountId = acc.id
                                        if (TwtAccountHelper.isTwtAccount(acc)) {
                                            selectedCurrency = "USD"
                                            if (transaction == null) category = "Bonus"
                                        } else {
                                            selectedCurrency = Currencies.effectiveCurrencyCode(acc, "CFA")
                                        }
                                        showAccountPickerDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(acc.emojiAvatar, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                        color = theme.primaryColor
                                    )
                                    Text(
                                        text = Currencies.formatForAccount(acc.balance, acc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.secondaryTextColor
                                    )
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAccountPickerDialog = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }

    if (showFxUsdPickerDialog) {
        val sortedUsdFirst = remember(accounts) {
            accounts.sortedByDescending { Currencies.effectiveCurrencyCode(it, "CFA") == "USD" }
        }
        Dialog(onDismissRequest = { showFxUsdPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.fx_source_usd_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(sortedUsdFirst, key = { it.id }) { acc ->
                            val isSel = acc.id == fxUsdAccountId
                            val displayName = if (TwtAccountHelper.isTwtAccount(acc)) TwtAccountHelper.currentTwtAccountName() else acc.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) theme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        fxUsdAccountId = acc.id
                                        showFxUsdPickerDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(acc.emojiAvatar, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayName, fontWeight = FontWeight.SemiBold, color = theme.primaryColor)
                                    Text(Currencies.formatForAccount(acc.balance, acc), style = MaterialTheme.typography.labelSmall, color = theme.secondaryTextColor)
                                }
                                if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showFxUsdPickerDialog = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }

    if (showFxCfaPickerDialog) {
        val sortedCfaFirst = remember(accounts) {
            accounts.filter { !TwtAccountHelper.isTwtAccount(it) }
                .ifEmpty { accounts }
        }
        Dialog(onDismissRequest = { showFxCfaPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.fx_dest_cfa_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(sortedCfaFirst, key = { it.id }) { acc ->
                            val isSel = acc.id == fxCfaAccountId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) theme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        fxCfaAccountId = acc.id
                                        showFxCfaPickerDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(acc.emojiAvatar, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.name, fontWeight = FontWeight.SemiBold, color = theme.primaryColor)
                                    Text(Currencies.formatForAccount(acc.balance, acc), style = MaterialTheme.typography.labelSmall, color = theme.secondaryTextColor)
                                }
                                if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showFxCfaPickerDialog = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }

    if (showInlineAddAccountSheet && onQuickCreateAccount != null) {
        AccountEditSheet(
            account = null,
            baseCurrency = "CFA",
            onDismiss = { showInlineAddAccountSheet = false },
            onSave = { newName, newType, newBalance, newCur, newEmoji ->
                onQuickCreateAccount(newName, newType, newBalance, newCur, newEmoji) { createdId ->
                    accountId = createdId
                    if (TwtAccountHelper.isTwtAccountName(newName)) {
                        selectedCurrency = "USD"
                        if (transaction == null) category = "Bonus"
                    } else {
                        selectedCurrency = newCur
                    }
                }
                showInlineAddAccountSheet = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    selectedCategory: String?,
    selectedAccount: String?,
    minAmount: Double?,
    maxAmount: Double?,
    accounts: List<AccountEntity>,
    customCategories: List<Pair<String, String>> = emptyList(),
    onApply: (category: String?, account: String?, min: Double?, max: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    var cat by remember { mutableStateOf(selectedCategory) }
    var accId by remember { mutableStateOf(selectedAccount) }
    var minText by remember { mutableStateOf(minAmount?.toString() ?: "") }
    var maxText by remember { mutableStateOf(maxAmount?.toString() ?: "") }

    var categoryDropdown by remember { mutableStateOf(false) }
    var accountDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.filter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = theme.primaryColor
            )

            // Category Filter
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = cat ?: stringResource(R.string.filter_all),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { categoryDropdown = true }
                )
                DropdownMenu(
                    expanded = categoryDropdown,
                    onDismissRequest = { categoryDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_all)) },
                        onClick = {
                            cat = null
                            categoryDropdown = false
                        }
                    )
                    WalletCategories.combined(customCategories).forEach { (catName, icon) ->
                        DropdownMenuItem(
                            text = { Text("$icon $catName") },
                            onClick = {
                                cat = catName
                                categoryDropdown = false
                            }
                        )
                    }
                }
            }

            // Account Filter
            val selectedAccount = accounts.firstOrNull { it.id == accId }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAccount?.let { "${it.emojiAvatar} ${it.name}" } ?: stringResource(R.string.all_accounts),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_account)) },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { accountDropdown = true }
                )
                DropdownMenu(
                    expanded = accountDropdown,
                    onDismissRequest = { accountDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.all_accounts)) },
                        onClick = {
                            accId = null
                            accountDropdown = false
                        }
                    )
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.emojiAvatar} ${account.name}") },
                            onClick = {
                                accId = account.id
                                accountDropdown = false
                            }
                        )
                    }
                }
            }

            // Min & Max Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it },
                    label = { Text(stringResource(R.string.min_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it },
                    label = { Text(stringResource(R.string.max_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        cat = null
                        accId = null
                        minText = ""
                        maxText = ""
                        onApply(null, null, null, null)
                    }
                ) {
                    Text(stringResource(R.string.clear_filters), color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val min = Currencies.parseBigDecimal(minText)?.toDouble()
                        val max = Currencies.parseBigDecimal(maxText)?.toDouble()
                        onApply(cat, accId, min, max)
                    }
                ) {
                    Text("Appliquer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

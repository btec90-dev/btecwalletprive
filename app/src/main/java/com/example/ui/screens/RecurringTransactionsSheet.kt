package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountEntity
import com.example.data.model.Currencies
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WalletCategories
import com.example.ui.components.ArtisanCard
import com.example.ui.theme.LocalArtisanTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsSheet(
    recurringList: List<RecurringTransactionEntity>,
    accounts: List<AccountEntity>,
    baseCurrency: String,
    onAddRecurring: (RecurringTransactionEntity) -> Unit,
    onToggleActive: (id: String, isActive: Boolean) -> Unit,
    onExecuteNow: (id: String) -> Unit,
    onDeleteRecurring: (id: String) -> Unit,
    onProcessDueNow: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }

    var showAddRecurringForm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = theme.surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = theme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.recurring_schedule),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                        Text(
                            text = "Ajout automatique au grand livre",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.primaryColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action banner: Add new & Run due check
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddRecurringForm = true },
                    modifier = Modifier.weight(1.2f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primaryColor,
                        contentColor = theme.onPrimaryColor
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_recurring), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onProcessDueNow()
                        successNotice = "Échéances vérifiées et synchronisées avec le grand livre !"
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.primaryColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vérifier", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            successNotice?.let { notice ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notice,
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // List of Recurring Schedules
            if (recurringList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.no_recurring),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.no_recurring_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recurringList, key = { it.id }) { item ->
                        val account = accountsMap[item.accountId]
                        RecurringItemCard(
                            item = item,
                            account = account,
                            baseCurrency = baseCurrency,
                            onToggleActive = { onToggleActive(item.id, it) },
                            onExecuteNow = {
                                onExecuteNow(item.id)
                                successNotice = "Transaction '${item.payee}' ajoutée avec succès au grand livre !"
                            },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Add Recurring Transaction Form Modal
    if (showAddRecurringForm) {
        AddRecurringFormSheet(
            accounts = accounts,
            onDismiss = { showAddRecurringForm = false },
            onSave = { entity ->
                onAddRecurring(entity)
                showAddRecurringForm = false
                successNotice = "Récurrence '${entity.payee}' programmée avec succès !"
            }
        )
    }

    // Delete Confirmation
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Supprimer la récurrence") },
            text = { Text("Voulez-vous supprimer l'échéance programmée '${item.payee}' ? (Les transactions passées du grand livre seront conservées).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecurring(item.id)
                        itemToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun RecurringItemCard(
    item: RecurringTransactionEntity,
    account: AccountEntity?,
    baseCurrency: String,
    onToggleActive: (Boolean) -> Unit,
    onExecuteNow: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val isIncome = item.type == TransactionType.INCOME
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.FRENCH) }
    val now = System.currentTimeMillis()
    val isDue = item.nextDueDate <= now

    val dueDateStr = remember(item.nextDueDate) {
        val diffDays = ((item.nextDueDate - now) / 86400000L).toInt()
        when {
            diffDays < 0 -> "Échue (en attente d'ajout)"
            diffDays == 0 -> "Aujourd'hui"
            diffDays == 1 -> "Demain"
            else -> "Dans $diffDays jours (${sdf.format(Date(item.nextDueDate))})"
        }
    }

    ArtisanCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (item.isActive) theme.surfaceColor else theme.backgroundColor.copy(alpha = 0.6f),
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isIncome) Color(0xFFE8F5E9) else theme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = WalletCategories.getIcon(item.category),
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Payee & Recurrence info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.payee,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = theme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = item.frequency.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (account != null) {
                            Text(
                                text = " • ${account.emojiAvatar} ${account.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Amount
                val prefix = if (isIncome) "+" else "-"
                val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                val currencyCode = account?.currencyCode ?: baseCurrency

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$prefix${Currencies.format(item.amount, currencyCode)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.size(34.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.onPrimaryColor,
                            checkedTrackColor = theme.primaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info: Next due date & execute trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.backgroundColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isDue) Color(0xFFC62828) else theme.stitchingColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Prochaine : $dueDateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDue) Color(0xFFC62828) else theme.primaryColor,
                        fontWeight = if (isDue) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Manual trigger to ledger
                    TextButton(
                        onClick = onExecuteNow,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = theme.primaryColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Générer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringFormSheet(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (RecurringTransactionEntity) -> Unit
) {
    val theme = LocalArtisanTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Housing & Rent") }
    var frequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var notes by remember { mutableStateOf("") }

    var accountId by remember {
        mutableStateOf(accounts.firstOrNull()?.id ?: "")
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Programmer une Transaction Récurrente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = theme.primaryColor
            )

            // Type Toggle: Income vs Expense
            TabRow(
                selectedTabIndex = if (type == TransactionType.EXPENSE) 0 else 1,
                containerColor = theme.backgroundColor,
                contentColor = theme.primaryColor,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = type == TransactionType.EXPENSE,
                    onClick = {
                        type = TransactionType.EXPENSE
                        category = "Housing & Rent"
                    },
                    text = { Text("Dépense Récurrente (Loyer...)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = type == TransactionType.INCOME,
                    onClick = {
                        type = TransactionType.INCOME
                        category = "Salary & Wages"
                    },
                    text = { Text("Revenu Récurrent (Salaire...)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Frequency Picker
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Fréquence de répétition :",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(RecurrenceFrequency.values()) { freq ->
                        val isSelected = frequency == freq
                        FilterChip(
                            selected = isSelected,
                            onClick = { frequency = freq },
                            label = { Text(freq.displayName(), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryColor,
                                selectedLabelColor = theme.onPrimaryColor
                            )
                        )
                    }
                }
            }

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; errorMessage = null },
                label = { Text(stringResource(R.string.amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Payee Field (e.g. "Loyer Appartement", "Salaire Entreprise", "Abonnement Internet")
            OutlinedTextField(
                value = payee,
                onValueChange = { payee = it; errorMessage = null },
                label = { Text("Bénéficiaire / Description (ex: Loyer Mensuel, Salaire)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Picker
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = "${WalletCategories.getIcon(category)} $category",
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
                        .clickable { categoryExpanded = true }
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    WalletCategories.ALL.forEach { (catName, icon) ->
                        DropdownMenuItem(
                            text = { Text("$icon $catName") },
                            onClick = {
                                category = catName
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Account Picker
            val selectedAccount = accounts.firstOrNull { it.id == accountId }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAccount?.let { "${it.emojiAvatar} ${it.name} (${it.currencyCode})" } ?: stringResource(R.string.select_account),
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
                        .clickable { accountExpanded = true }
                )
                DropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.emojiAvatar} ${acc.name} (${Currencies.format(acc.balance, acc.currencyCode)})") },
                            onClick = {
                                accountId = acc.id
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            // Notes / Memo
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_memo)) },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

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
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0) {
                            errorMessage = "Veuillez entrer un montant valide supérieur à 0."
                        } else if (payee.isBlank()) {
                            errorMessage = "Veuillez entrer une description ou nom de bénéficiaire."
                        } else if (accountId.isBlank()) {
                            errorMessage = "Veuillez sélectionner un compte."
                        } else {
                            val now = System.currentTimeMillis()
                            val nextDue = frequency.computeNextDueDate(now)
                            val entity = RecurringTransactionEntity(
                                accountId = accountId,
                                type = type,
                                amount = parsedAmount,
                                payee = payee.trim(),
                                category = category,
                                frequency = frequency,
                                startDate = now,
                                lastExecutedDate = now,
                                nextDueDate = nextDue,
                                isActive = true,
                                notes = notes.trim(),
                                updatedAt = now
                            )
                            onSave(entity)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primaryColor,
                        contentColor = theme.onPrimaryColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Programmer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

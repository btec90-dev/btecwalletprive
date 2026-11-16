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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.Currencies
import com.example.data.model.TwtAccountHelper
import com.example.security.SecurityManager
import com.example.ui.components.AccountAvatarPickerDialog
import com.example.ui.components.ArtisanCard
import com.example.ui.components.TwtSecurityChallengeDialog
import com.example.ui.theme.LocalArtisanTheme
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    totalBalance: BigDecimal,
    totalIncome: BigDecimal,
    totalExpense: BigDecimal,
    baseCurrency: String,
    twtUsdBalance: BigDecimal? = null,
    activity: FragmentActivity? = null,
    securityManager: SecurityManager? = null,
    onAddAccount: (name: String, type: AccountType, balance: Double, currency: String, emoji: String) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    val theme = LocalArtisanTheme.current
    var showAddSheet by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var twtPendingUnlockAccount by remember { mutableStateOf<AccountEntity?>(null) }

    fun requestOpenAccount(account: AccountEntity) {
        if (TwtAccountHelper.isTwtAccount(account) && activity != null && securityManager != null) {
            twtPendingUnlockAccount = account
        } else {
            accountToEdit = account
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = theme.primaryColor,
                contentColor = theme.onPrimaryColor,
                shape = CircleShape,
                modifier = Modifier.testTag("add_account_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_account))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Worth Hero Card with Leather aesthetics
            item {
                ArtisanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("net_worth_card"),
                    containerColor = theme.primaryColor,
                    contentColor = theme.onPrimaryColor,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.total_balance).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.onPrimaryColor.copy(alpha = 0.75f),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Currencies.format(totalBalance, baseCurrency),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = theme.onPrimaryColor
                        )

                        if (twtUsdBalance != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = theme.metallicAccent.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, theme.metallicAccent.copy(alpha = 0.65f)),
                                modifier = Modifier.testTag("twt_usd_hero_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = theme.onPrimaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${TwtAccountHelper.currentTwtAccountName()} (USD $) : ${Currencies.format(twtUsdBalance, "USD")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.onPrimaryColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Income / Expense summary bar inside card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.income),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.onPrimaryColor.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = Currencies.format(totalIncome, baseCurrency),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFC62828).copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = Color(0xFFEF9A9A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.expenses),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.onPrimaryColor.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = Currencies.format(totalExpense, baseCurrency),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFCDD2)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Smart Accounts Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.smart_accounts),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                        Text(
                            text = stringResource(R.string.account_order_info),
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.secondaryTextColor
                        )
                    }
                }
            }

            // Accounts List sorted with Cash first, Card, Savings, Custom (slide-to-delete intentionally disabled)
            items(accounts, key = { it.id }) { account ->
                AccountItemCard(
                    account = account,
                    onOpen = { requestOpenAccount(account) },
                    onEdit = { requestOpenAccount(account) },
                    onDelete = { accountToDelete = account }
                )
            }
        }
    }

    // TWT 2026 Biometric / 4-digit PIN Challenge Dialog
    val pendingTwt = twtPendingUnlockAccount
    if (pendingTwt != null && activity != null && securityManager != null) {
        TwtSecurityChallengeDialog(
            activity = activity,
            securityManager = securityManager,
            title = stringResource(R.string.twt_unlock_title),
            subtitle = stringResource(R.string.twt_unlock_sub),
            onAuthenticated = {
                val acc = pendingTwt
                twtPendingUnlockAccount = null
                accountToEdit = acc
            },
            onDismiss = { twtPendingUnlockAccount = null }
        )
    }

    // Add Account Sheet
    if (showAddSheet) {
        AccountEditSheet(
            account = null,
            baseCurrency = baseCurrency,
            onDismiss = { showAddSheet = false },
            onSave = { name, type, balance, cur, emoji ->
                onAddAccount(name, type, balance, cur, emoji)
                showAddSheet = false
            }
        )
    }

    // Edit Account Sheet
    accountToEdit?.let { acc ->
        AccountEditSheet(
            account = acc,
            baseCurrency = baseCurrency,
            onDismiss = { accountToEdit = null },
            onSave = { name, type, balance, cur, emoji ->
                onUpdateAccount(acc.copy(
                    name = name,
                    type = type,
                    balance = balance,
                    currencyCode = cur,
                    emojiAvatar = emoji
                ))
                accountToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.confirm_delete_account)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(acc.id)
                        accountToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun AccountItemCard(
    account: AccountEntity,
    onOpen: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    var showMenu by remember { mutableStateOf(false) }
    val isTwt = TwtAccountHelper.isTwtAccount(account)
    val displayName = if (isTwt) TwtAccountHelper.currentTwtAccountName() else account.name

    ArtisanCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("account_item_${account.id}"),
        containerColor = if (isTwt) theme.surfaceElevatedColor else theme.surfaceColor,
        border = if (isTwt) BorderStroke(1.5.dp, theme.metallicAccent) else null,
        elevation = if (isTwt) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isTwt) theme.metallicAccent.copy(alpha = 0.24f) else theme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = account.emojiAvatar, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryColor
                    )
                    if (isTwt) {
                        // Embossed lock badge for Private TWT 2026
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = theme.primaryColor,
                            border = BorderStroke(1.dp, theme.metallicAccent),
                            shadowElevation = 3.dp,
                            modifier = Modifier.testTag("twt_embossed_lock_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.twt_private_badge),
                                    tint = theme.metallicAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = stringResource(R.string.twt_private_badge),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.onPrimaryColor
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val typeLabel = when (account.type) {
                        AccountType.CASH -> stringResource(R.string.type_cash)
                        AccountType.CARD -> stringResource(R.string.type_card)
                        AccountType.SAVINGS -> stringResource(R.string.type_savings)
                        AccountType.CUSTOM -> stringResource(R.string.type_custom)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.backgroundColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.stitchingColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (isTwt) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = theme.metallicAccent.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, theme.metallicAccent.copy(alpha = 0.8f)),
                            modifier = Modifier.testTag("twt_usd_currency_badge")
                        ) {
                            Text(
                                text = "USD ($)",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Currencies.formatForAccount(account.balance, account, "CFA"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )
            }

            // More Options
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = theme.stitchingColor
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_account)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditSheet(
    account: AccountEntity?,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, balance: Double, currency: String, emoji: String) -> Unit
) {
    val theme = LocalArtisanTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: AccountType.CASH) }
    var balanceText by remember {
        mutableStateOf(
            account?.balance?.let { Currencies.toMoneyBigDecimal(it).toPlainString() } ?: "0.00"
        )
    }
    var currency by remember {
        mutableStateOf(
            if (TwtAccountHelper.isTwtAccount(account)) "USD"
            else Currencies.effectiveCurrencyCode(account, baseCurrency)
        )
    }
    var emojiAvatar by remember { mutableStateOf(account?.emojiAvatar ?: "💵") }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var balanceError by remember { mutableStateOf<String?>(null) }
    val isTwtLocked = TwtAccountHelper.isTwtAccountName(name)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (account == null) stringResource(R.string.add_account) else stringResource(R.string.edit_account),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = theme.primaryColor
            )

            // Emoji avatar selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.primaryContainer)
                        .clickable { showEmojiDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emojiAvatar, fontSize = 28.sp)
                }
                Column {
                    Text(
                        text = stringResource(R.string.account_avatar),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Appuyez pour changer l'icône",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.secondaryTextColor
                    )
                }
            }

            // Quick Account Presets (Tap Tap Send, MoneyGram, Transfer)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Modèles Rapides (Comptes & Transferts CFA)",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Triple("Tap Tap Send", "📲", AccountType.CUSTOM),
                        Triple("MoneyGram", "💸", AccountType.CUSTOM),
                        Triple("Transfert", "🔄", AccountType.CUSTOM)
                    )
                    for ((presetName, presetEmoji, presetType) in presets) {
                        Surface(
                            onClick = {
                                name = presetName
                                emojiAvatar = presetEmoji
                                type = presetType
                                currency = "CFA"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = theme.backgroundColor,
                            border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$presetEmoji $presetName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.primaryColor,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { newName ->
                    name = newName
                    if (TwtAccountHelper.isTwtAccountName(newName)) {
                        currency = "USD"
                    }
                },
                label = { Text(stringResource(R.string.account_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Currency Selector Toggle (CFA | USD — strictly locked to $ USD for TWT accounts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Devise du Compte",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryColor
                )
                if (isTwtLocked) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = theme.metallicAccent.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, theme.metallicAccent)
                    ) {
                        Text(
                            text = "🔒 USD ($)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = theme.primaryColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = theme.backgroundColor,
                        border = BorderStroke(1.dp, theme.stitchingColor.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (currency == "CFA") theme.primaryColor else Color.Transparent)
                                    .clickable { currency = "CFA" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "CFA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (currency == "CFA") theme.onPrimaryColor else theme.primaryColor
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (currency == "USD") theme.primaryColor else Color.Transparent)
                                    .clickable { currency = "USD" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$ USD",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (currency == "USD") theme.onPrimaryColor else theme.primaryColor
                                )
                            }
                        }
                    }
                }
            }

            // Account Type Selector
            Column {
                Text(
                    text = stringResource(R.string.account_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.values().forEach { t ->
                        val selected = t == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) theme.primaryColor else theme.backgroundColor)
                                .clickable { type = t }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (t) {
                                    AccountType.CASH -> "Cash"
                                    AccountType.CARD -> "Card"
                                    AccountType.SAVINGS -> "Savings"
                                    AccountType.CUSTOM -> "Custom"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) theme.onPrimaryColor else theme.primaryColor
                            )
                        }
                    }
                }
            }

            // Balance
            OutlinedTextField(
                value = balanceText,
                onValueChange = {
                    balanceText = it
                    balanceError = null
                },
                label = { Text(stringResource(R.string.initial_balance)) },
                isError = balanceError != null,
                supportingText = balanceError?.let { err -> { Text(err) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    onClick = {
                        val parsedBd = if (balanceText.isBlank()) {
                            BigDecimal.ZERO
                        } else {
                            Currencies.parseBigDecimal(balanceText)
                        }
                        if (parsedBd == null) {
                            balanceError = "Veuillez entrer un solde numérique valide."
                        } else {
                            val finalName = if (name.isBlank()) "Account" else name.trim()
                            onSave(finalName, type, Currencies.toMoneyBigDecimal(parsedBd.toDouble()).toDouble(), currency, emojiAvatar)
                        }
                    }
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, color = theme.primaryColor)
                }
            }
        }
    }

    if (showEmojiDialog) {
        AccountAvatarPickerDialog(
            selectedEmoji = emojiAvatar,
            onEmojiSelected = { emojiAvatar = it },
            onDismissRequest = { showEmojiDialog = false }
        )
    }
}

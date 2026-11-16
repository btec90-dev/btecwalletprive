package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.model.Currencies
import com.example.data.sync.SyncState
import com.example.data.sync.SyncStatus
import com.example.security.AccessSession
import com.example.security.SecurityManager
import com.example.security.UserRole
import com.example.ui.components.ArtisanCard
import com.example.ui.components.TwtSecurityChallengeDialog
import com.example.ui.theme.ArtisanThemeConfig
import com.example.ui.theme.ArtisanThemes
import com.example.ui.theme.LocalArtisanTheme
import com.example.util.AppDesignSpecBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeConfig: ArtisanThemeConfig,
    baseCurrency: String,
    currentLanguage: String,
    syncState: SyncState,
    accessSession: AccessSession = AccessSession(),
    adminDesignSpecText: String = "",
    isTwtVisibleEnabled: Boolean = true,
    includeSensitiveIdsInSpec: Boolean = false,
    isPinEnabled: Boolean,
    isBiometricEnabled: Boolean,
    activity: FragmentActivity? = null,
    securityManager: SecurityManager? = null,
    onThemeSelected: (ArtisanThemeConfig) -> Unit,
    onCurrencySelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onOpenAuthGate: () -> Unit = {},
    onToggleSync: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onToggleTwtVisibility: (Boolean) -> Unit = {},
    onToggleIncludeSensitiveIds: (Boolean) -> Unit = {},
    onTogglePin: (Boolean) -> Unit,
    onSetPin: (String) -> Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onOpenExportImport: () -> Unit,
    onClearTransactions: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val context = LocalContext.current
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showAdminSpecDialog by remember { mutableStateOf(false) }
    var showTwtVisibilityAuthDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Profile, Role & Switch / Log Out Controls Card
        item {
            ArtisanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_profile_card"),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_balatec_coin),
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.dp, theme.metallicAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = accessSession.activeEmail
                                    ?: syncState.userEmail
                                    ?: "Aucun compte connecté",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val roleLabel = when (accessSession.role) {
                                UserRole.ADMIN -> "Admin • TWT Actif"
                                UserRole.SHARED_USER -> "Utilisateur Partagé • Sync"
                                UserRole.SIGNED_OUT -> "Hors ligne"
                            }
                            val roleBadgeColor = when (accessSession.role) {
                                UserRole.ADMIN -> theme.metallicAccent.copy(alpha = 0.25f)
                                UserRole.SHARED_USER -> Color(0xFF2E7D32).copy(alpha = 0.16f)
                                UserRole.SIGNED_OUT -> theme.backgroundColor
                            }
                            val roleTextColor = when (accessSession.role) {
                                UserRole.ADMIN -> theme.primaryColor
                                UserRole.SHARED_USER -> Color(0xFF2E7D32)
                                UserRole.SIGNED_OUT -> theme.secondaryTextColor
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = roleBadgeColor,
                                modifier = Modifier.testTag("settings_role_badge")
                            ) {
                                Text(
                                    text = roleLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = roleTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenAuthGate,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_settings_login_switch"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primaryColor,
                                contentColor = theme.onPrimaryColor
                            )
                        ) {
                            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (accessSession.isSignedIn || syncState.isAuthenticated) {
                                    stringResource(R.string.switch_user)
                                } else {
                                    stringResource(R.string.sign_in)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (accessSession.isSignedIn || syncState.isAuthenticated) {
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_settings_logout"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.disconnect_account),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1b. Primary Admin Only (410bala@gmail.com): TWT 2026 Visibility Toggle (Biometric / PIN gated on ON)
        if (accessSession.isAdminTwtVisible) {
            item {
                ArtisanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_twt_visibility_card"),
                    containerColor = theme.surfaceColor,
                    border = BorderStroke(1.dp, theme.metallicAccent.copy(alpha = 0.65f)),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = theme.metallicAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.twt_visibility_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor
                                )
                                Text(
                                    text = stringResource(R.string.twt_visibility_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondaryTextColor
                                )
                            }
                        }

                        Switch(
                            checked = isTwtVisibleEnabled,
                            onCheckedChange = { wantVisible ->
                                if (!wantVisible) {
                                    onToggleTwtVisibility(false)
                                } else if (activity != null && securityManager != null) {
                                    showTwtVisibilityAuthDialog = true
                                } else {
                                    onToggleTwtVisibility(true)
                                }
                            },
                            modifier = Modifier.testTag("switch_twt_visibility"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = theme.surfaceColor,
                                checkedTrackColor = theme.primaryColor
                            )
                        )
                    }
                }
            }
        }

        // 1c. Primary Admin Only (410bala@gmail.com): Live App Contents & Design (.txt)
        if (AppDesignSpecBuilder.isEligibleForAdminSpec(accessSession) && adminDesignSpecText.isNotBlank()) {
            item {
                ArtisanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_admin_design_spec_card"),
                    containerColor = theme.surfaceColor,
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = theme.metallicAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.admin_design_spec_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor
                                )
                                Text(
                                    text = stringResource(R.string.admin_design_spec_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondaryTextColor
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = theme.metallicAccent.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = ".TXT LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = theme.backgroundColor,
                            border = BorderStroke(1.dp, theme.cardBorderColor.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val lineCount = remember(adminDesignSpecText) {
                                adminDesignSpecText.lineSequence().count()
                            }
                            val autoSyncStatus = if (currentLanguage.startsWith("en", ignoreCase = true)) {
                                "Auto-sync active"
                            } else {
                                "Auto-sync actif"
                            }
                            Text(
                                text = "${AppDesignSpecBuilder.ADMIN_SPEC_FILENAME} • $lineCount lines • $autoSyncStatus",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondaryTextColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.admin_include_sensitive_ids),
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = includeSensitiveIdsInSpec,
                                onCheckedChange = onToggleIncludeSensitiveIds,
                                modifier = Modifier.testTag("switch_admin_sensitive_ids"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.surfaceColor,
                                    checkedTrackColor = theme.primaryColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAdminSpecDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_admin_view_spec"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.primaryColor,
                                    contentColor = theme.onPrimaryColor
                                )
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.admin_design_spec_view),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText(AppDesignSpecBuilder.ADMIN_SPEC_FILENAME, adminDesignSpecText)
                                        )
                                        Toast.makeText(context, context.getString(R.string.admin_design_spec_copied), Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) { }
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("btn_admin_copy_spec"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy), modifier = Modifier.size(16.dp))
                            }

                            OutlinedButton(
                                onClick = {
                                    AppDesignSpecBuilder.shareSpecTextFile(context, adminDesignSpecText)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_admin_share_spec"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.admin_design_spec_share),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Dynamic Theme Switcher Card (10 Skeuomorphic Leather Themes + Hardware Accents)
        item {
            ArtisanCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = theme.stitchingColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.theme_selection),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                            Text(
                                text = "10 cuirs artisanaux & finitions métalliques",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondaryTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = theme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, theme.metallicAccent.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "${currentThemeConfig.name} • ${currentThemeConfig.hardwareName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = theme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Classic Leather Collection (5 themes)
                    Text(
                        text = stringResource(R.string.theme_classic_group),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.stitchingColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeSwatchGrid(
                        themes = ArtisanThemes.ALL_THEMES.filter { it.isClassic },
                        selectedTheme = currentThemeConfig,
                        onThemeClick = onThemeSelected
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bold / Luxury Collection (5 themes)
                    Text(
                        text = stringResource(R.string.theme_bold_group),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.stitchingColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeSwatchGrid(
                        themes = ArtisanThemes.ALL_THEMES.filter { !it.isClassic },
                        selectedTheme = currentThemeConfig,
                        onThemeClick = onThemeSelected
                    )
                }
            }
        }

        // 3. Cloud Backup & Multi-Device Sync Card
        item {
            ArtisanCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (syncState.isSyncEnabled && syncState.isAuthenticated) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (syncState.isSyncEnabled && syncState.isAuthenticated) Color(0xFF2E7D32) else theme.stitchingColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.cloud_backup_sync),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor
                                )
                                Text(
                                    text = if (syncState.isAuthenticated && syncState.userEmail != null) {
                                        "${stringResource(R.string.signed_in_as)} ${syncState.userEmail}"
                                    } else {
                                        stringResource(R.string.sync_desc)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Switch(
                            checked = syncState.isSyncEnabled,
                            onCheckedChange = { checked -> onToggleSync(checked) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = theme.surfaceColor,
                                checkedTrackColor = theme.primaryColor
                            )
                        )
                    }

                    if (syncState.isSyncEnabled && syncState.isAuthenticated) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val lastSyncText = syncState.lastSyncTime?.let { dateFormat.format(Date(it)) } ?: "À l'instant"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stringResource(R.string.last_synced)}: $lastSyncText",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.onPrimaryContainer
                            )
                            Text(
                                text = when (syncState.status) {
                                    SyncStatus.SYNCING -> stringResource(R.string.sync_status_syncing)
                                    SyncStatus.SYNCED -> stringResource(R.string.sync_status_synced)
                                    else -> stringResource(R.string.sync_status_local)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (syncState.status == SyncStatus.SYNCING) theme.primaryColor else Color(0xFF2E7D32)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onSyncNow,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.sync_now), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onToggleSync(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_settings_connect_google"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primaryColor,
                                contentColor = theme.onPrimaryColor
                            )
                        ) {
                            Text(text = "G", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFF4285F4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_in_google), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Security & PIN Settings
        item {
            ArtisanCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = theme.stitchingColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.pin_lock_enabled),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor
                                )
                                Text(
                                    text = "Verrouillage par code PIN à 4 chiffres",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondaryTextColor
                                )
                            }
                        }

                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showPinSetupDialog = true
                                } else {
                                    onTogglePin(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = theme.surfaceColor,
                                checkedTrackColor = theme.primaryColor
                            )
                        )
                    }

                    if (isPinEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = theme.stitchingColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.use_biometrics), style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = onToggleBiometric,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.surfaceColor,
                                    checkedTrackColor = theme.primaryColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showPinSetupDialog = true }) {
                            Text(stringResource(R.string.change_pin), color = theme.primaryColor)
                        }
                    }
                }
            }
        }

        // 6. Global Base Currency Selector
        item {
            ArtisanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCurrencyDialog = true },
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = theme.stitchingColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.base_currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                            val cur = Currencies.getCurrency(baseCurrency)
                            Text(
                                text = "${cur.name} (${cur.symbol})",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondaryTextColor
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = baseCurrency,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                }
            }
        }

        // 7. Language Switcher (English and Français only)
        item {
            ArtisanCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = theme.stitchingColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.language),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                            Text(
                                text = "English & Français",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondaryTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isEn = currentLanguage.startsWith("en", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isEn) theme.primaryColor else theme.backgroundColor)
                                .clickable { onLanguageSelected("en") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isEn) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = theme.onPrimaryColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = stringResource(R.string.lang_en),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEn) theme.onPrimaryColor else theme.primaryColor
                                )
                            }
                        }

                        val isFr = currentLanguage.startsWith("fr", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isFr) theme.primaryColor else theme.backgroundColor)
                                .clickable { onLanguageSelected("fr") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFr) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = theme.onPrimaryColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = stringResource(R.string.lang_fr),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFr) theme.onPrimaryColor else theme.primaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Data Backup, Flexible PDF Export & Transfer (PDF, JSON & CSV)
        item {
            ArtisanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenExportImport),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = theme.stitchingColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.export_import),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                            Text(
                                text = "Rapports PDF filtrés, sauvegarde JSON & CSV",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondaryTextColor
                            )
                        }
                    }

                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = theme.stitchingColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 9. Clear & Reset All Transactions
        item {
            var showClearConfirm by remember { mutableStateOf(false) }
            ArtisanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearConfirm = true },
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Effacer toutes les transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Supprimer définitivement l'historique du grand livre",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondaryTextColor
                            )
                        }
                    }
                }
            }

            if (showClearConfirm) {
                BasicAlertDialog(onDismissRequest = { showClearConfirm = false }) {
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
                                text = "Effacer toutes les transactions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Voulez-vous vraiment supprimer tout l'historique des transactions ? Cette action est irréversible.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.secondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showClearConfirm = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onClearTransactions()
                                        showClearConfirm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Effacer tout", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Currency Selector Dialog
    if (showCurrencyDialog) {
        CurrencySelectorDialog(
            selectedCurrency = baseCurrency,
            onCurrencySelected = {
                onCurrencySelected(it)
                showCurrencyDialog = false
            },
            onDismissRequest = { showCurrencyDialog = false }
        )
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        PinSetupDialog(
            onPinCreated = { pin ->
                val ok = onSetPin(pin)
                if (ok) {
                    showPinSetupDialog = false
                }
                ok
            },
            onDismissRequest = { showPinSetupDialog = false }
        )
    }

    // TWT Visibility Security Challenge Dialog
    if (showTwtVisibilityAuthDialog && activity != null && securityManager != null) {
        TwtSecurityChallengeDialog(
            activity = activity,
            securityManager = securityManager,
            title = stringResource(R.string.twt_unlock_title),
            subtitle = stringResource(R.string.twt_unlock_sub),
            onAuthenticated = {
                showTwtVisibilityAuthDialog = false
                onToggleTwtVisibility(true)
            },
            onDismiss = { showTwtVisibilityAuthDialog = false }
        )
    }

    // Admin App Details & Design (.txt) Live Viewer Dialog
    if (showAdminSpecDialog && adminDesignSpecText.isNotBlank()) {
        AdminDesignSpecDialog(
            specText = adminDesignSpecText,
            includeSensitiveIds = includeSensitiveIdsInSpec,
            onToggleIncludeSensitiveIds = onToggleIncludeSensitiveIds,
            onCopy = {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(AppDesignSpecBuilder.ADMIN_SPEC_FILENAME, adminDesignSpecText)
                    )
                    Toast.makeText(context, context.getString(R.string.admin_design_spec_copied), Toast.LENGTH_SHORT).show()
                } catch (_: Exception) { }
            },
            onShare = { AppDesignSpecBuilder.shareSpecTextFile(context, adminDesignSpecText) },
            onDismissRequest = { showAdminSpecDialog = false }
        )
    }
}

@Composable
private fun ThemeSwatchGrid(
    themes: List<ArtisanThemeConfig>,
    selectedTheme: ArtisanThemeConfig,
    onThemeClick: (ArtisanThemeConfig) -> Unit
) {
    val activeTheme = LocalArtisanTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (item in themes) {
            val isSelected = item.id == selectedTheme.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onThemeClick(item) }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(item.primaryColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.5.dp,
                            color = item.metallicAccent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = item.name,
                            tint = item.onPrimaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name.split(" ").first(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) activeTheme.primaryColor else activeTheme.secondaryTextColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelectorDialog(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val theme = LocalArtisanTheme.current

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.surfaceColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.base_currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(Currencies.SUPPORTED) { cur ->
                        val isSelected = cur.code == selectedCurrency
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) theme.primaryColor else theme.backgroundColor)
                                .clickable { onCurrencySelected(cur.code) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = cur.symbol,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) theme.onPrimaryColor else theme.primaryColor
                                )
                                Text(
                                    text = cur.code,
                                    fontSize = 12.sp,
                                    color = if (isSelected) theme.onPrimaryColor.copy(alpha = 0.8f) else Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupDialog(
    onPinCreated: (String) -> Boolean,
    onDismissRequest: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_pin),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text(stringResource(R.string.confirm_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pin.length != 4) {
                                errorMessage = "PIN must be exactly 4 digits."
                            } else if (pin != confirmPin) {
                                errorMessage = "PIN codes do not match."
                            } else {
                                onPinCreated(pin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            contentColor = theme.onPrimaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDesignSpecDialog(
    specText: String,
    includeSensitiveIds: Boolean,
    onToggleIncludeSensitiveIds: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val scrollState = rememberScrollState()

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = theme.surfaceColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.admin_design_spec_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.admin_include_sensitive_ids),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = includeSensitiveIds,
                        onCheckedChange = onToggleIncludeSensitiveIds,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.surfaceColor,
                            checkedTrackColor = theme.primaryColor
                        )
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = theme.backgroundColor,
                    border = BorderStroke(1.dp, theme.cardBorderColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    Text(
                        text = specText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = theme.onBackgroundColor,
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(12.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.copy), fontSize = 12.sp)
                    }
                    Button(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            contentColor = theme.onPrimaryColor
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.admin_design_spec_share), fontSize = 12.sp)
                    }
                }
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

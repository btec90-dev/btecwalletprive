package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.sync.CloudSyncManager
import com.example.security.PasscodeLoginResult
import com.example.ui.theme.LocalArtisanTheme

private fun android.content.Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthGateBottomSheet(
    activity: Activity? = null,
    cloudSyncManager: CloudSyncManager,
    currentLanguage: String = "fr",
    onPasscodeLogin: ((email: String, passcode: String) -> PasscodeLoginResult)? = null,
    onDismissRequest: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val context = LocalContext.current
    val targetActivity = activity ?: context.findActivity()
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPasscodeSection by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passcodeInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = theme.surfaceColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gold Circular Medallion Emblem
            Image(
                painter = painterResource(R.drawable.ic_balatec_coin),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, theme.metallicAccent, CircleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Connexion & Synchronisation Cloud",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = theme.primaryColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Accès réservé aux comptes autorisés Balatec. Connectez votre compte Google pour synchroniser en temps réel sur plusieurs appareils.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.secondaryTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-In Primary Action via Jetpack Credential Manager
            Button(
                onClick = {
                    if (targetActivity != null) {
                        isLoading = true
                        errorMessage = null
                        cloudSyncManager.signInWithGoogle(
                            activity = targetActivity,
                            credentialManager = credentialManager,
                            scope = scope,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                                onDismissRequest()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            onCancelled = {
                                isLoading = false
                            }
                        )
                    } else {
                        errorMessage = "Impossible d'initialiser Google Sign-In (Activité non trouvée)"
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_google_sign_in"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primaryColor,
                    contentColor = theme.onPrimaryColor
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = theme.onPrimaryColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "G", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.sign_in_google),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            if (onPasscodeLogin != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showPasscodeSection = !showPasscodeSection },
                    modifier = Modifier.testTag("btn_toggle_passcode_login")
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = theme.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showPasscodeSection) "Masquer la connexion par code" else "Changer d'utilisateur / Code d'accès",
                        color = theme.primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                if (showPasscodeSection) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = theme.cardBorderColor.copy(alpha = 0.4f))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; errorMessage = null },
                        label = { Text(stringResource(R.string.email_address)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passcode_login_email"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = passcodeInput,
                        onValueChange = { passcodeInput = it; errorMessage = null },
                        label = { Text(stringResource(R.string.passcode_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passcode_login_code"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            when (val result = onPasscodeLogin(emailInput, passcodeInput)) {
                                is PasscodeLoginResult.Success -> {
                                    onAuthSuccess()
                                    onDismissRequest()
                                }
                                is PasscodeLoginResult.Error -> {
                                    errorMessage = if (currentLanguage.startsWith("en", ignoreCase = true)) {
                                        result.messageEn
                                    } else {
                                        result.messageFr
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_submit_passcode_login"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sign_in_with_passcode),
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                }
            }

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Security note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = theme.stitchingColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Isolation stricte des données & chiffrement Firestore",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.secondaryTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


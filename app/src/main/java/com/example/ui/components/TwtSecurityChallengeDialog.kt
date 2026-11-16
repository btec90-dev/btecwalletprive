package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.security.BiometricHelper
import com.example.security.SecurityManager
import com.example.ui.theme.LocalArtisanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwtSecurityChallengeDialog(
    activity: FragmentActivity,
    securityManager: SecurityManager,
    title: String,
    subtitle: String,
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalArtisanTheme.current
    val biometricAvailable = remember(activity) { BiometricHelper.isBiometricAvailable(activity) }
    val hasExistingPin = remember(securityManager) { securityManager.hasPin() }

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun launchBiometric() {
        if (!biometricAvailable) return
        BiometricHelper.promptBiometric(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = onAuthenticated,
            onError = { msg -> errorMessage = msg }
        )
    }

    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) {
            launchBiometric()
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.surfaceColor,
            border = BorderStroke(1.5.dp, theme.metallicAccent.copy(alpha = 0.7f)),
            tonalElevation = 6.dp,
            modifier = Modifier.testTag("twt_security_challenge_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = theme.metallicAccent.copy(alpha = 0.22f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = theme.primaryColor,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.secondaryTextColor
                        )
                    }
                }

                if (hasExistingPin) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                pin = it
                                errorMessage = null
                            }
                        },
                        label = { Text(stringResource(R.string.enter_pin)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("twt_challenge_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.twt_setup_pin_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.secondaryTextColor
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                pin = it
                                errorMessage = null
                            }
                        },
                        label = { Text(stringResource(R.string.create_pin)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("twt_challenge_create_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                confirmPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text(stringResource(R.string.confirm_pin)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("twt_challenge_confirm_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (biometricAvailable) {
                    OutlinedButton(
                        onClick = { launchBiometric() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("twt_challenge_biometric_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.biometric_auth), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (hasExistingPin) {
                                if (pin.length != 4) {
                                    errorMessage = activity.getString(R.string.pin_incorrect)
                                } else if (securityManager.verifyPinOnly(pin)) {
                                    onAuthenticated()
                                } else {
                                    errorMessage = activity.getString(R.string.pin_incorrect)
                                    pin = ""
                                }
                            } else {
                                if (pin.length != 4) {
                                    errorMessage = activity.getString(R.string.pin_incorrect)
                                } else if (pin != confirmPin) {
                                    errorMessage = activity.getString(R.string.pin_mismatch)
                                } else if (securityManager.setPin(pin, enableAppLock = false)) {
                                    onAuthenticated()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            contentColor = theme.onPrimaryColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("twt_challenge_verify_btn")
                    ) {
                        Text(stringResource(R.string.twt_unlock_confirm), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

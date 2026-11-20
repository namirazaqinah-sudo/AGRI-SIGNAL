package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AgriAppBackground
import com.example.ui.components.AgriSignalLogoMark
import com.example.ui.components.AgriSignalWordmark
import com.example.ui.theme.ForestGreen
import com.example.ui.viewmodel.AuthFormState
import com.example.ui.viewmodel.AuthMode

@Composable
fun AuthScreen(
    formState: AuthFormState,
    onModeChange: (AuthMode) -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val isRegister = formState.mode == AuthMode.REGISTER

    AgriAppBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 22.dp, end = 22.dp, top = 56.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AgriSignalLogoMark(size = 84.dp)
                        AgriSignalWordmark(fontSize = 28.sp)

                        Text(
                            text = if (isRegister) {
                                "Daftar Akun Petani Baru"
                            } else {
                                "Masuk ke Lahan Anda"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val fieldContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        val fieldTextColor = MaterialTheme.colorScheme.onSurface
                        val fieldSecondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldContainerColor,
                            unfocusedContainerColor = fieldContainerColor,
                            errorContainerColor = fieldContainerColor,
                            focusedTextColor = fieldTextColor,
                            unfocusedTextColor = fieldTextColor,
                            errorTextColor = fieldTextColor,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = fieldSecondaryTextColor,
                            focusedPlaceholderColor = fieldSecondaryTextColor,
                            unfocusedPlaceholderColor = fieldSecondaryTextColor,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            unfocusedLeadingIconColor = fieldSecondaryTextColor,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = ForestGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )

                        OutlinedTextField(
                            value = formState.fullName,
                            onValueChange = onNameChange,
                            label = { Text(stringResource(id = R.string.auth_label_name)) },
                            placeholder = { Text(stringResource(id = R.string.auth_hint_name)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null
                                )
                            },
                            isError = formState.nameError != null,
                            supportingText = formState.nameError?.let { err -> { Text(err) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )

                        OutlinedTextField(
                            value = formState.phoneNumber,
                            onValueChange = onPhoneChange,
                            label = { Text(stringResource(id = R.string.auth_label_phone)) },
                            placeholder = { Text(stringResource(id = R.string.auth_hint_phone)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = null
                                )
                            },
                            isError = formState.phoneError != null,
                            supportingText = formState.phoneError?.let { err -> { Text(err) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSubmit()
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_phone_input")
                        )

                        AnimatedVisibility(
                            visible = formState.formError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            formState.formError?.let { msg ->
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onSubmit()
                            },
                            enabled = !formState.isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("auth_submit_button")
                        ) {
                            if (formState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isRegister) "Daftar" else "Login",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val switchText = buildAnnotatedString {
                            append(if (isRegister) "Sudah punya akun? " else "Belum punya akun? ")
                            withStyle(
                                SpanStyle(
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(if (isRegister) "Login" else "Daftar")
                            }
                        }
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable {
                                    onModeChange(if (isRegister) AuthMode.LOGIN else AuthMode.REGISTER)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag(if (isRegister) "auth_tab_login" else "auth_tab_register"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = switchText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

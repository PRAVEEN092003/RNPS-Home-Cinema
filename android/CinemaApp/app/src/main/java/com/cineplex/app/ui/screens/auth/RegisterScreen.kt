package com.cineplex.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.cineplex.app.R
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.CinemaTextField
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val name by viewModel.nameState.collectAsState()
    val email by viewModel.emailState.collectAsState()
    val phone by viewModel.phoneState.collectAsState()
    val password by viewModel.passwordState.collectAsState()
    val registerState by viewModel.registerState.collectAsState()
    val isServerWakingUp by viewModel.isServerWakingUp.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(registerState) {
        if (registerState is UiState.Success) {
            onRegisterSuccess()
        }
    }

    Scaffold(
        containerColor = BackgroundDeep,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.create_account),
                    style = CinemaTypography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.create_account_subtitle),
                    style = CinemaTypography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(32.dp))

                CinemaTextField(
                    value = name,
                    onValueChange = { viewModel.nameState.value = it },
                    label = stringResource(id = R.string.full_name),
                    leadingIcon = Icons.Default.Person,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CinemaTextField(
                    value = email,
                    onValueChange = { viewModel.emailState.value = it },
                    label = stringResource(id = R.string.email_address),
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CinemaTextField(
                    value = phone,
                    onValueChange = { viewModel.phoneState.value = it },
                    label = stringResource(id = R.string.phone_optional),
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CinemaTextField(
                    value = password,
                    onValueChange = { viewModel.passwordState.value = it },
                    label = stringResource(id = R.string.password),
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(id = R.string.toggle_password_visibility),
                                tint = IconTint,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )

                if (registerState is UiState.Loading && isServerWakingUp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.server_waking_up),
                        color = WarningAmber,
                        style = CinemaTypography.bodySmall,
                    )
                }

                if (registerState is UiState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = (registerState as UiState.Error).message,
                        color = ErrorRed,
                        style = CinemaTypography.bodySmall,
                    )
                } else if (registerState is UiState.Offline) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.error_network_check),
                        color = WarningAmber,
                        style = CinemaTypography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                CinemaButton(
                    text = stringResource(id = R.string.register),
                    onClick = { viewModel.register() },
                    isLoading = registerState is UiState.Loading,
                    enabled = registerState !is UiState.Loading,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.already_have_account),
                        style = CinemaTypography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.sign_in),
                        style = CinemaTypography.bodyMedium.copy(
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.clickable { onNavigateToLogin() },
                    )
                }
            }
        }
    }
}

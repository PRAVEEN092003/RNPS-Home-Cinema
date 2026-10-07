package com.cineplex.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.CinemaTextField
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val email by viewModel.emailState.collectAsState()
    val password by viewModel.passwordState.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = BackgroundDeep,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Welcome Back",
                    style = CinemaTypography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in to your cinema account",
                    style = CinemaTypography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(32.dp))

                CinemaTextField(
                    value = email,
                    onValueChange = { viewModel.emailState.value = it },
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CinemaTextField(
                    value = password,
                    onValueChange = { viewModel.passwordState.value = it },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = IconTint,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )

                if (loginState is UiState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = (loginState as UiState.Error).message,
                        color = ErrorRed,
                        style = CinemaTypography.bodySmall,
                    )
                } else if (loginState is UiState.Offline) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No internet connection. Please check network.",
                        color = WarningAmber,
                        style = CinemaTypography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                CinemaButton(
                    text = "Sign In",
                    onClick = { viewModel.login() },
                    isLoading = loginState is UiState.Loading,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Don't have an account? ",
                        style = CinemaTypography.bodyMedium,
                        color = TextSecondary,
                    )
                    Text(
                        text = "Register",
                        style = CinemaTypography.bodyMedium.copy(
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.clickable { onNavigateToRegister() },
                    )
                }
            }
        }
    }
}

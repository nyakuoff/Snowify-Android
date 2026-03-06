package com.snowify.app.ui.screens.onboarding
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowify.app.R
import com.snowify.app.ui.components.AccentButton
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.OnboardingViewModel
@Composable
fun OnboardingScreen(
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val isLoading by onboardingViewModel.isLoading.collectAsStateWithLifecycle()
    val error by onboardingViewModel.error.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.bgElevated, colors.bgBase)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_snowify_logo),
                contentDescription = "Snowify",
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Welcome to Snowify",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Stream music from YouTube Music",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onboardingViewModel.clearError()
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.bgHighlight,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary,
                ),
                leadingIcon = { Icon(Icons.Filled.Email, null, tint = colors.textSubdued) },
            )
            Spacer(Modifier.height(12.dp))
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onboardingViewModel.clearError()
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.bgHighlight,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary,
                ),
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = colors.textSubdued) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            null,
                            tint = colors.textSubdued,
                        )
                    }
                },
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = colors.red, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            AccentButton(
                text = if (isCreatingAccount) "Create Account" else "Sign In",
                onClick = {
                    if (isCreatingAccount) {
                        onboardingViewModel.createAccount(email, password)
                    } else {
                        onboardingViewModel.signIn(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
            )
            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { isCreatingAccount = !isCreatingAccount }) {
                Text(
                    text = if (isCreatingAccount) "Already have an account? Sign In" else "Don't have an account? Create one",
                    color = colors.accent,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = colors.bgHighlight)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { onboardingViewModel.continueWithoutAccount() }) {
                Text(
                    text = "Continue without an account",
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Without an account, your data is stored locally and can't be recovered if lost.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSubdued,
                textAlign = TextAlign.Center,
            )
        }
    }
}

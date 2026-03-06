package com.snowify.app.ui.screens.settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowify.app.ui.components.SnowifyTopBar
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.util.AppTheme
import com.snowify.app.viewmodel.OnboardingViewModel
import com.snowify.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
    val animationsEnabled by settingsViewModel.animationsEnabled.collectAsStateWithLifecycle()
    val effectsEnabled by settingsViewModel.effectsEnabled.collectAsStateWithLifecycle()
    val autoplayEnabled by settingsViewModel.autoplayEnabled.collectAsStateWithLifecycle()
    val audioQuality by settingsViewModel.audioQuality.collectAsStateWithLifecycle()
    val isSignedIn by settingsViewModel.isSignedIn.collectAsStateWithLifecycle()
    val isLoading by onboardingViewModel.isLoading.collectAsStateWithLifecycle()
    val authError by onboardingViewModel.error.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var signInEmail by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }

    // Dismiss sign-in dialog on successful auth
    val prevSignedIn = remember { mutableStateOf(isSignedIn) }
    LaunchedEffect(isSignedIn) {
        if (!prevSignedIn.value && isSignedIn) {
            showSignInDialog = false
            signInEmail = ""
            signInPassword = ""
        }
        prevSignedIn.value = isSignedIn
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        Spacer(Modifier.height(24.dp))
        SnowifyTopBar(title = "Settings")
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            // Account section
            item {
                SettingsSectionHeader("Account")
                if (isSignedIn) {
                    SettingsItem(
                        title = "Sign Out",
                        subtitle = "Sign out of your account",
                        icon = Icons.Filled.Logout,
                        iconTint = colors.red,
                        onClick = { showSignOutDialog = true },
                    )
                } else {
                    SettingsItem(
                        title = "Sign In",
                        subtitle = "Sign in to sync your data",
                        icon = Icons.Filled.Login,
                        iconTint = colors.accent,
                        onClick = {
                            onboardingViewModel.clearError()
                            signInEmail = ""
                            signInPassword = ""
                            showSignInDialog = true
                        },
                    )
                }
            }
            // Appearance section
            item {
                SettingsSectionHeader("Appearance")
                Text(
                    "Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(AppTheme.values()) { appTheme ->
                        ThemeSwatch(
                            appTheme = appTheme,
                            isSelected = theme == appTheme,
                            onClick = { settingsViewModel.setTheme(appTheme) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                SettingsToggle(
                    title = "Animations",
                    subtitle = "Enable UI animations",
                    checked = animationsEnabled,
                    onToggle = { settingsViewModel.setAnimationsEnabled(it) },
                    icon = Icons.Filled.Animation,
                )
                SettingsToggle(
                    title = "Visual Effects",
                    subtitle = "Enable blur and glow effects",
                    checked = effectsEnabled,
                    onToggle = { settingsViewModel.setEffectsEnabled(it) },
                    icon = Icons.Filled.AutoAwesome,
                )
            }
            // Playback section
            item { SettingsSectionHeader("Playback") }
            item {
                SettingsToggle(
                    title = "Autoplay",
                    subtitle = "Continue playing related songs when queue ends",
                    checked = autoplayEnabled,
                    onToggle = { settingsViewModel.setAutoplayEnabled(it) },
                    icon = Icons.Filled.PlaylistPlay,
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Audio Quality", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = audioQuality == "bestaudio",
                            onClick = { settingsViewModel.setAudioQuality("bestaudio") },
                            label = { Text("Best") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentDim,
                                selectedLabelColor = colors.accent,
                            ),
                        )
                        FilterChip(
                            selected = audioQuality == "worstaudio",
                            onClick = { settingsViewModel.setAudioQuality("worstaudio") },
                            label = { Text("Low (saves data)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentDim,
                                selectedLabelColor = colors.accent,
                            ),
                        )
                    }
                }
            }
            // Data section
            item { SettingsSectionHeader("Data") }
            item {
                SettingsItem(
                    title = "Clear Play History",
                    subtitle = "Remove recently played songs",
                    icon = Icons.Filled.History,
                    onClick = { settingsViewModel.clearPlayHistory() },
                )
                SettingsItem(
                    title = "Clear Search History",
                    subtitle = "Remove recent searches",
                    icon = Icons.Filled.SearchOff,
                    onClick = { settingsViewModel.clearSearchHistory() },
                )
            }
            // About section
            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(
                    title = "Snowify",
                    subtitle = "Version 1.0.0",
                    icon = Icons.Filled.Info,
                )
            }
        }
    }
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History?", color = colors.textPrimary) },
            text = { Text("This will remove all play and search history.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.clearPlayHistory()
                    settingsViewModel.clearSearchHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear", color = colors.red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.bgElevated,
        )
    }
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?", color = colors.textPrimary) },
            text = { Text("Your local data will be kept.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.signOut()
                    showSignOutDialog = false
                }) {
                    Text("Sign Out", color = colors.red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.bgElevated,
        )
    }
    if (showSignInDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSignInDialog = false },
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.bgElevated,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = signInEmail,
                        onValueChange = { signInEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.textSubdued,
                            focusedLabelColor = colors.accent,
                            unfocusedLabelColor = colors.textSubdued,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = signInPassword,
                        onValueChange = { signInPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (signInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { signInPasswordVisible = !signInPasswordVisible }) {
                                Icon(
                                    if (signInPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = colors.textSubdued,
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.textSubdued,
                            focusedLabelColor = colors.accent,
                            unfocusedLabelColor = colors.textSubdued,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!authError.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            authError!!,
                            color = colors.red,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showSignInDialog = false }) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onboardingViewModel.signIn(signInEmail, signInPassword) },
                            enabled = !isLoading && signInEmail.isNotBlank() && signInPassword.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = colors.bgBase,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Sign In")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    val colors = SnowifyTheme.colors
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colors.accent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = SnowifyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: colors.textSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSubdued)
            }
        }
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, null, tint = colors.textSubdued, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colors = SnowifyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSubdued)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accent,
                checkedTrackColor = colors.accentDim,
            ),
        )
    }
}

@Composable
fun ThemeSwatch(
    appTheme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = SnowifyTheme.colors
    val swatchColor = when (appTheme) {
        AppTheme.DARK -> Color(0xFFAA55E6)
        AppTheme.LIGHT -> Color(0xFFF0F0F0)
        AppTheme.OCEAN -> Color(0xFF5B8DEE)
        AppTheme.FOREST -> Color(0xFF48BB78)
        AppTheme.SUNSET -> Color(0xFFED6450)
        AppTheme.ROSE -> Color(0xFFDB7093)
        AppTheme.MIDNIGHT -> Color(0xFF7B7BDA)
    }
    val iconColor = if (appTheme == AppTheme.LIGHT) Color(0xFF1A1A1A) else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(swatchColor, RoundedCornerShape(12.dp))
                .then(
                    if (isSelected) Modifier.border(2.dp, colors.textPrimary, RoundedCornerShape(12.dp))
                    else Modifier.border(1.dp, colors.bgHighlight, RoundedCornerShape(12.dp))
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(appTheme.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) colors.accent else colors.textSubdued)
    }
}

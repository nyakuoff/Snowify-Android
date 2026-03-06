package com.snowify.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.snowify.app.audio.MusicController
import com.snowify.app.ui.navigation.*
import com.snowify.app.ui.screens.nowplaying.MiniPlayer
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.OnboardingViewModel
import com.snowify.app.viewmodel.PlayerViewModel
import com.snowify.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var musicController: MusicController

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — Media3 will handle gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for Android 13+ (required for foreground service notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SnowifyApp(musicController)
        }
    }
}

@Composable
fun SnowifyApp(musicController: MusicController) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
    val animationsEnabled by settingsViewModel.animationsEnabled.collectAsStateWithLifecycle()
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsStateWithLifecycle()

    // Remember start destination to avoid NavHost restart
    val startDestination = remember(onboardingComplete) {
        if (onboardingComplete) Screen.Home.route else Screen.Onboarding.route
    }

    SnowifyTheme(appTheme = theme, animationsEnabled = animationsEnabled) {
        val colors = com.snowify.app.ui.theme.SnowifyTheme.colors
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        // Observe toast messages from MusicController
        LaunchedEffect(Unit) {
            musicController.toastMessage.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe toast messages from OnboardingViewModel (cloud sync, auth)
        LaunchedEffect(Unit) {
            onboardingViewModel.message.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        // Determine if we should show bottom nav
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomNav = currentRoute in listOf(
            Screen.Home.route,
            Screen.Explore.route,
            Screen.Library.route,
            Screen.Settings.route,
        )
        val showMiniPlayer = currentRoute != Screen.NowPlaying.route &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute != Screen.Lyrics.route &&
            currentRoute != Screen.Queue.route &&
            currentRoute != Screen.Search.route

        // Navigate to Home when onboarding completes
        LaunchedEffect(onboardingComplete) {
            if (onboardingComplete && currentRoute == Screen.Onboarding.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgBase),
            containerColor = colors.bgBase,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    if (showMiniPlayer) {
                        MiniPlayer(
                            onExpand = { navController.navigate(Screen.NowPlaying.route) }
                        )
                    }
                    if (showBottomNav) {
                        SnowifyBottomNavBar(navController = navController)
                    }
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                SnowifyNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                )
            }
        }
    }
}

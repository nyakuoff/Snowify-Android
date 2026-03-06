package com.snowify.app.ui.navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.snowify.app.ui.theme.SnowifyTheme
@Composable
fun SnowifyBottomNavBar(navController: NavController) {
    val colors = SnowifyTheme.colors
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val items = listOf(
        BottomNavItemData(Screen.Home.route, "Home", Icons.Filled.Home),
        BottomNavItemData(Screen.Explore.route, "Explore", Icons.Filled.Explore),
        BottomNavItemData(Screen.Library.route, "Library", Icons.Filled.LibraryMusic),
        BottomNavItemData(Screen.Settings.route, "Settings", Icons.Filled.Settings),
    )
    NavigationBar(
        containerColor = colors.bgSurface,
        contentColor = colors.textPrimary,
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.accentDim,
                ),
            )
        }
    }
}
data class BottomNavItemData(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

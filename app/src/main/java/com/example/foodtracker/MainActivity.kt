package com.example.foodtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodtracker.ui.screens.AddFoodScreen
import com.example.foodtracker.ui.screens.InventoryScreen
import com.example.foodtracker.ui.screens.SettingsScreen
import com.example.foodtracker.ui.screens.UpcomingScreen
import com.example.foodtracker.ui.theme.FoodTrackerTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Inventory : Screen("inventory", "Inventory", Icons.Default.List)
    object Upcoming : Screen("upcoming", "Upcoming", Icons.Default.DateRange)
    object Add : Screen("add", "Add Food", Icons.Default.Add)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as FoodApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodTrackerTheme {
                // rememberSaveable: these survive activity recreation (rotation, theme change,
                // process restart) via saved instance state, so the request-once guard below
                // actually holds across recreation instead of resetting every time.
                var notificationPermissionGranted by rememberSaveable {
                    mutableStateOf(hasNotificationPermission())
                }
                var notificationPermissionDenied by rememberSaveable { mutableStateOf(false) }
                var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    notificationPermissionGranted = isGranted
                    notificationPermissionDenied = !isGranted
                }

                LaunchedEffect(Unit) {
                    // Only request when not already granted, and only once per activity
                    // lifetime. Without hasRequestedNotificationPermission this would re-launch
                    // the system permission dialog on every recreation (e.g. rotation) as long
                    // as the permission stayed denied, instead of asking a single time and then
                    // leaving it to the user to change it in system settings.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !notificationPermissionGranted &&
                        !hasRequestedNotificationPermission
                    ) {
                        hasRequestedNotificationPermission = true
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                MainScreen(
                    viewModel = viewModel,
                    notificationPermissionDenied = notificationPermissionDenied
                )
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // POST_NOTIFICATIONS is only a runtime permission from Android 13 (Tiramisu)
            // onward; there is nothing to request pre-33, so treat the gate as satisfied.
            return true
        }
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, notificationPermissionDenied: Boolean = false) {
    val navController = rememberNavController()
    val items = listOf(Screen.Inventory, Screen.Upcoming, Screen.Add, Screen.Settings)

    Scaffold(
        topBar = {
            if (notificationPermissionDenied) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Notifications are turned off, so expiry reminders won't be " +
                            "shown. Enable notifications in system settings to get alerts.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Inventory.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Inventory.route) { InventoryScreen(viewModel) }
            composable(Screen.Upcoming.route) { UpcomingScreen(viewModel) }
            composable(Screen.Add.route) { AddFoodScreen(viewModel, onNavigateToList = {
                navController.navigate(Screen.Inventory.route) {
                    popUpTo(Screen.Inventory.route) { inclusive = true }
                }
            }) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}

// Navigation logic remains the same

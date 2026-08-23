package com.example.foodtracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Inventory, Screen.Upcoming, Screen.Add, Screen.Settings)

    Scaffold(
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

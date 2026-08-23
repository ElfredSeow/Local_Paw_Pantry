package com.example.foodtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.MainViewModel
import com.example.foodtracker.ui.components.FoodItemCard
import com.example.foodtracker.ui.theme.FreshBlue
import kotlinx.coroutines.delay

// MainViewModel exposes no loading flag, so there's no authoritative way to distinguish
// "still loading" from "genuinely empty" here. This is a best-effort mitigation: the first
// Room emission normally arrives within a few ms, so waiting this long before trusting an
// empty list avoids flashing "Your inventory is empty" on cold start, while staying short
// enough that a truly empty inventory still shows promptly.
private const val EMPTY_STATE_SETTLE_DELAY_MS = 300L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: MainViewModel) {
    val items by viewModel.filteredItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var hasSettled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(EMPTY_STATE_SETTLE_DELAY_MS)
        hasSettled = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            color = FreshBlue,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FreshTrack",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All Inventory",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search food items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        when {
            items.isEmpty() && !hasSettled -> {
                // Still within the initial settle window and nothing has arrived yet - avoid
                // asserting "empty" prematurely.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() -> EmptyState(isSearch = searchQuery.isNotEmpty())
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { item ->
                        FoodItemCard(
                            item = item,
                            onDelete = { viewModel.deleteItem(item) },
                            onUpdateQuantity = { delta -> viewModel.updateQuantity(item, delta) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(isSearch: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isSearch) "No matching food found" else "Your inventory is empty",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isSearch) "Try a different search term." else "Tap the Add button below to start tracking your food.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

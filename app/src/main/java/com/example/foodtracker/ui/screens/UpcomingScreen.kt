package com.example.foodtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.MainViewModel
import com.example.foodtracker.ui.components.FoodItemCard
import com.example.foodtracker.ui.theme.FreshBlue
import com.example.foodtracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(viewModel: MainViewModel) {
    val items by viewModel.filteredItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val upcomingItems = remember(items) {
        items.filter { DateUtils.getDaysUntil(it.expiryDate) <= 14 }
            .sortedBy { it.expiryDate }
    }

    val groupedItems = remember(upcomingItems) {
        upcomingItems.groupBy { DateUtils.getGroupKey(it.expiryDate) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = FreshBlue, contentColor = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Expiring Soon Timeline",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // F3: search on the Upcoming tab too (was previously only on Inventory)
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search food items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        if (groupedItems.isEmpty()) {
            // F3: empty state instead of a blank screen
            UpcomingEmptyState(isSearch = searchQuery.isNotEmpty())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                groupedItems.forEach { (group, groupItems) ->
                    item {
                        Text(
                            text = group,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(groupItems, key = { it.id }) { item ->
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
private fun UpcomingEmptyState(isSearch: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isSearch) "No upcoming items match your search" else "All caught up!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isSearch) "Try a different search term." else "Nothing is expiring in the next two weeks.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

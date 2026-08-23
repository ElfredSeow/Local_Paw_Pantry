package com.example.foodtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.MainViewModel
import com.example.foodtracker.ui.components.FoodItemCard
import com.example.foodtracker.ui.theme.FreshBlue
import com.example.foodtracker.util.DateUtils

@Composable
fun UpcomingScreen(viewModel: MainViewModel) {
    val items by viewModel.filteredItems.collectAsState()
    
    val upcomingItems = remember(items) {
        items.filter { 
            DateUtils.getDaysUntil(it.expiryDate) <= 14 
        }.sortedBy { it.expiryDate }
    }

    val groupedItems = remember(upcomingItems) {
        upcomingItems.groupBy { DateUtils.getGroupKey(it.expiryDate) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = FreshBlue, contentColor = Color.White) {
            Text(
                text = "Expiring Soon Timeline",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
        }

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
                items(groupItems) { item ->
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

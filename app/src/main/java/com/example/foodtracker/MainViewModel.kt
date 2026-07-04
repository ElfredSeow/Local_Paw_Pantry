package com.example.foodtracker

import android.app.Application
import androidx.lifecycle.*
import com.example.foodtracker.data.Category
import com.example.foodtracker.data.FoodItem
import com.example.foodtracker.data.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    application: Application,
    private val repository: FoodRepository
) : AndroidViewModel(application) {

    val allItems: StateFlow<List<FoodItem>> = repository.allItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredItems: StateFlow<List<FoodItem>> = combine(allItems, _searchQuery) { items, query ->
        if (query.isEmpty()) items
        else items.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addItem(item: FoodItem) = viewModelScope.launch {
        repository.insertItem(item)
    }

    fun deleteItem(item: FoodItem) = viewModelScope.launch {
        repository.deleteItem(item)
        // Remove the item's stored photo so internal storage doesn't leak (S1)
        item.imagePath?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    fun updateQuantity(item: FoodItem, delta: Int) = viewModelScope.launch {
        val newQuantity = (item.quantity + delta).coerceAtLeast(0)
        repository.updateItem(item.copy(quantity = newQuantity))
    }

    fun addCategory(name: String) = viewModelScope.launch {
        repository.insertCategory(Category(name))
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        // Repository reassigns this category's items to "Uncategorized" transactionally (D2)
        repository.deleteCategory(category)
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: FoodRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

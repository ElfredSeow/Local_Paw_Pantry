package com.example.foodtracker.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodtracker.MainViewModel
import com.example.foodtracker.data.FoodItem
import com.example.foodtracker.data.Reminder
import com.example.foodtracker.ui.components.getEmoji
import com.example.foodtracker.ui.theme.FreshBlue
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Selectable food icons offered when the user doesn't upload a photo (F1). */
private val FOOD_ICONS = listOf(
    "general", "milk", "chicken", "meat", "juice", "fruit",
    "citrus", "veg", "cheese", "bread", "fish"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(viewModel: MainViewModel, onNavigateToList: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var expiryDate by remember { mutableStateOf(LocalDate.now()) }
    var category by remember { mutableStateOf("Uncategorized") }
    var iconId by remember { mutableStateOf("general") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    // F2: user-editable reminders. Defaults to a single sensible reminder instead of the
    // old hardcoded [30, 7, 1] that fired the moment an item was added (N3).
    var reminders by remember { mutableStateOf(listOf(Reminder(daysBefore = 3))) }

    val categories by viewModel.allCategories.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val quantityValue = quantity.toIntOrNull() ?: 0
    val isValid = name.isNotBlank() && quantityValue > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(color = FreshBlue, contentColor = Color.White) {
            Text(
                text = "Add New Food",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Image / Icon Picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = "Selected food photo", modifier = Modifier.fillMaxSize())
                    } else {
                        Text(getEmoji(iconId), fontSize = 40.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { imagePicker.launch("image/*") }) {
                    Text("Upload Photo")
                }
                if (imageUri != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { imageUri = null }) { Text("Remove") }
                }
            }

            // Icon selector (F1) — shown when no photo is chosen
            if (imageUri == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "OR PICK AN ICON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FOOD_ICONS.forEach { id ->
                        val selected = id == iconId
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) FreshBlue.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (selected) FreshBlue else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { iconId = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(getEmoji(id), fontSize = 22.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Picker
            Box {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showCategoryDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select category")
                        }
                    }
                )
                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                category = cat.name
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity (U5: numeric keyboard + digits only)
            OutlinedTextField(
                value = quantity,
                onValueChange = { input -> if (input.all { it.isDigit() }) quantity = input },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = quantity.isNotEmpty() && quantityValue <= 0,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker
            OutlinedTextField(
                value = expiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                onValueChange = {},
                readOnly = true,
                label = { Text("Expiry Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick expiry date")
                    }
                }
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = expiryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                expiryDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reminder editor (F2)
            RemindersEditor(
                reminders = reminders,
                onChange = { reminders = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                enabled = isValid,
                onClick = {
                    val savedImagePath = imageUri?.let { saveImageToInternalStorage(context, it) }
                    val newItem = FoodItem(
                        name = name.trim(),
                        category = category,
                        quantity = quantityValue,
                        expiryDate = expiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        imagePath = savedImagePath,
                        iconId = iconId,
                        // Reset notification status so a fresh item can notify (isNotified defaults false)
                        reminders = reminders
                    )
                    viewModel.addItem(newItem)
                    onNavigateToList()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FreshBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Item", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RemindersEditor(
    reminders: List<Reminder>,
    onChange: (List<Reminder>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Reminders", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = { onChange(reminders + Reminder(daysBefore = 3)) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add")
        }
    }

    if (reminders.isEmpty()) {
        Text(
            text = "No reminders set — you won't be notified for this item.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    reminders.forEachIndexed { index, reminder ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Remind me", fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = reminder.daysBefore.toString(),
                onValueChange = { v ->
                    val days = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                    onChange(reminders.toMutableList().also { it[index] = reminder.copy(daysBefore = days) })
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(88.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("days before", fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { onChange(reminders.filterIndexed { i, _ -> i != index }) }) {
                Icon(Icons.Default.Close, contentDescription = "Remove reminder")
            }
        }
    }
}

/**
 * Copies the picked image into internal storage, downsampled to <= 500 px and JPEG-compressed
 * so photos don't bloat storage (S1). Returns the saved file's absolute path, or null on failure.
 */
private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val resolver = context.contentResolver

        // Pass 1: read bounds only, so we never decode a huge bitmap into memory.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val maxSize = 500
        var sample = 1
        while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) {
            sample *= 2
        }

        // Pass 2: decode downsampled.
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val file = File(context.filesDir, "food_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

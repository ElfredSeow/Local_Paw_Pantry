package com.example.foodtracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.foodtracker.ui.theme.BackgroundGray
import com.example.foodtracker.ui.theme.FreshBlue
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Uri has no built-in Saver, so rememberSaveable needs to be told how to turn it into
// something Bundle-friendly (a String) and back. An empty string stands in for "no photo".
private val UriSaver = Saver<Uri?, String>(
    save = { uri -> uri?.toString() ?: "" },
    restore = { value -> if (value.isEmpty()) null else Uri.parse(value) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(viewModel: MainViewModel, onNavigateToList: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var expiryDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var category by rememberSaveable { mutableStateOf("Uncategorized") }
    var iconId by rememberSaveable { mutableStateOf("general") }
    var imageUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }

    val categories by viewModel.allCategories.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // Validation: a blank name and an unusable quantity must block Save, visibly.
    val nameError = name.isBlank()
    val parsedQuantity = quantity.toIntOrNull()
    val quantityError = parsedQuantity == null || parsedQuantity < 0
    val isFormValid = !nameError && !quantityError

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
                        .background(BackgroundGray)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(getEmoji(iconId), fontSize = 40.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { imagePicker.launch("image/*") }) {
                    Text("Upload Photo")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Name") },
                singleLine = true,
                isError = nameError,
                supportingText = {
                    if (nameError) {
                        Text("Name is required")
                    }
                },
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
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Show category options")
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

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity") },
                singleLine = true,
                isError = quantityError,
                supportingText = {
                    if (quantityError) {
                        Text(
                            if (parsedQuantity == null) "Enter a whole number" else "Quantity cannot be negative"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        Icon(Icons.Default.DateRange, contentDescription = "Select expiry date")
                    }
                }
            )

            if (showDatePicker) {
                // DatePickerState.selectedDateMillis is UTC midnight of the picked day, so both
                // the seed value and the read-back must use ZoneOffset.UTC. Using the device's
                // ZoneId.systemDefault() here would shift the date by one day for any user west
                // of UTC (i.e. all negative offsets, the whole of the Americas).
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = expiryDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                expiryDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        val safeQuantity = (quantity.toIntOrNull() ?: 0).coerceAtLeast(0)
                        val savedImagePath = imageUri?.let { saveImageToInternalStorage(context, it) }
                        val newItem = FoodItem(
                            name = name,
                            category = category,
                            quantity = safeQuantity,
                            expiryDate = expiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            imagePath = savedImagePath,
                            iconId = iconId,
                            reminders = listOf(
                                Reminder(daysBefore = 30),
                                Reminder(daysBefore = 7),
                                Reminder(daysBefore = 1)
                            )
                        )
                        viewModel.addItem(newItem)
                        onNavigateToList()
                    }
                },
                enabled = isFormValid,
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

private fun saveImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "food_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

// Helper for mutableStateFlowOf style (I meant mutableStateOf but I often typo it)
@Composable
fun <T> myMutableStateOf(value: T): MutableState<T> = remember { mutableStateOf(value) }

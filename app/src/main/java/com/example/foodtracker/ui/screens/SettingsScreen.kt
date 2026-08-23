package com.example.foodtracker.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.MainViewModel
import com.example.foodtracker.ui.theme.FreshBlue
import com.example.foodtracker.util.CsvHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val categories by viewModel.allCategories.collectAsState()
    val items by viewModel.allItems.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newCategoryName by remember { mutableStateOf("") }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    // Export: SAF Binder IPC + a full CSV/Gson encode, so it is offloaded to IO and only
    // switches back to Main to report the result.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val success = CsvHelper.exportToCsv(context, it, items)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Export failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Import: same IO offload, plus a single bulk-insert transaction (viewModel.addItems)
    // instead of one insert per row, and an honest imported/skipped count back on Main.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val result = CsvHelper.importFromCsv(context, it)
                if (result.items.isNotEmpty()) {
                    viewModel.addItems(result.items)
                }
                withContext(Dispatchers.Main) {
                    val message = when {
                        result.items.isEmpty() && result.skipped == 0 ->
                            "No items found in that file."
                        result.items.isEmpty() ->
                            "Import failed: all ${result.skipped} row(s) were invalid."
                        result.skipped == 0 ->
                            "Imported ${result.items.size} item(s)."
                        else ->
                            "Imported ${result.items.size} item(s), skipped ${result.skipped} invalid row(s)."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Import from CSV") },
            text = {
                Text(
                    "Importing will overwrite any existing items whose ID matches a row " +
                        "in the file. This cannot be undone. Continue?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = false
                    importLauncher.launch(arrayOf("text/*", "application/csv"))
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(color = FreshBlue, contentColor = Color.White) {
            Text(
                text = "App Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Category Management
            SettingsSection(title = "Food Categories", icon = Icons.Default.Star) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("New category...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName)
                            newCategoryName = ""
                        }
                    }) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.name, fontWeight = FontWeight.Medium)
                        if (cat.name != "Uncategorized") {
                            IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications
            SettingsSection(title = "System Notifications", icon = Icons.Default.Notifications) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to open notification settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Text("Enable Push Notifications", color = Color.Black)
                }
                Text(
                    text = "Global settings have been moved. You now configure specific notification schedules per-item when adding food.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Management
            SettingsSection(title = "Local Data Storage", icon = Icons.Default.Share) {
                Text(
                    text = "To keep your data strictly on your device, export your inventory to a CSV file. You can import it later to restore your data.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { exportLauncher.launch("food_inventory_${System.currentTimeMillis()}.csv") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export to CSV (Save)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showImportConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Import from CSV (Load)")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = FreshBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

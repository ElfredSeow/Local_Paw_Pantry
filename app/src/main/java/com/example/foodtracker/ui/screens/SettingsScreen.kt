package com.example.foodtracker.ui.screens

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
import com.example.foodtracker.util.openAppNotificationSettings
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

    // D3: run file I/O off the main thread so a large inventory can't ANR.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { target ->
            scope.launch {
                val success = withContext(Dispatchers.IO) { CsvHelper.exportToCsv(context, target, items) }
                Toast.makeText(
                    context,
                    if (success) "Exported successfully!" else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { target ->
            scope.launch {
                val importedItems = withContext(Dispatchers.IO) { CsvHelper.importFromCsv(context, target) }
                importedItems.forEach { item -> viewModel.addItem(item) }
                Toast.makeText(context, "Imported ${importedItems.size} items", Toast.LENGTH_SHORT).show()
            }
        }
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
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${cat.name}", tint = Color.Red)
                            }
                        }
                    }
                }

                Text(
                    text = "Deleting a category moves its items to \"Uncategorized\".",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications
            SettingsSection(title = "System Notifications", icon = Icons.Default.Notifications) {
                Button(
                    // N5: actually take the user to notification settings to enable them.
                    onClick = { openAppNotificationSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage notification settings")
                }
                Text(
                    text = "Reminders are sent per-item based on the schedule you set when adding food. Use this to allow or block notifications for the app.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Management
            SettingsSection(title = "Local Data Storage", icon = Icons.Default.Share) {
                Text(
                    text = "To keep your data strictly on your device, export your inventory to a CSV file. You can import it later to restore your data.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onClick = { importLauncher.launch(arrayOf("text/*", "application/csv")) },
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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

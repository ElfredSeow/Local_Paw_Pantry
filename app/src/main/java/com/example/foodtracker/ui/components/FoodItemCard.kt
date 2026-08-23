package com.example.foodtracker.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodtracker.data.FoodItem
import com.example.foodtracker.ui.theme.*
import com.example.foodtracker.util.DateUtils

@Composable
fun FoodItemCard(
    item: FoodItem,
    onDelete: () -> Unit,
    onUpdateQuantity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysUntil = DateUtils.getDaysUntil(item.expiryDate)
    val status = getExpiryStatus(daysUntil)
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image / Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                if (item.imagePath != null) {
                    AsyncImage(
                        model = item.imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = getEmoji(item.iconId), fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    StatusTag(status = status)
                }

                Text(
                    text = "Expires: ${DateUtils.formatDate(item.expiryDate)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = item.category,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            // Implicit web-search intent for this item's name - mirrors the
                            // original JS prototype's
                            // https://www.google.com/search?q=<name>+recipe link.
                            try {
                                val query = Uri.encode("${item.name} recipe")
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/search?q=$query")
                                )
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                // No app can resolve a web search on this device; nothing
                                // reasonable to fall back to, so just no-op rather than crash.
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FreshBlue)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, size = 12.dp)
                        Spacer(Modifier.width(4.dp))
                        Text("Recipe Ideas", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QuantityControls(
                            quantity = item.quantity,
                            onUpdate = onUpdateQuantity,
                            itemName = item.name
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete ${item.name}",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${item.name}?") },
            text = { Text("This removes it from your inventory. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusTag(status: ExpiryStatus) {
    Surface(
        color = status.backgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = status.text,
            color = status.textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun QuantityControls(quantity: Int, onUpdate: (Int) -> Unit, itemName: String) {
    Surface(
        color = BackgroundGray,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { onUpdate(-1) },
                modifier = Modifier
                    .size(48.dp)
                    .clearAndSetSemantics { contentDescription = "Decrease quantity of $itemName" },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = quantity.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            TextButton(
                onClick = { onUpdate(1) },
                modifier = Modifier
                    .size(48.dp)
                    .clearAndSetSemantics { contentDescription = "Increase quantity of $itemName" },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class ExpiryStatus(val text: String, val backgroundColor: Color, val textColor: Color)

fun getExpiryStatus(days: Long): ExpiryStatus {
    return when {
        days < 0 -> ExpiryStatus("EXPIRED", ExpiryRed, Color.White)
        days == 0L -> ExpiryStatus("EXPIRING TODAY", ExpiryRed, Color.White)
        // Tinted background + dark foreground, matching the GOOD state's pattern below -
        // solid WarningYellow with white text was ~1.6:1 contrast, far under the 4.5:1 minimum.
        days <= 7 -> ExpiryStatus("EXPIRING SOON", WarningYellow.copy(alpha = 0.2f), WarningYellowDark)
        else -> ExpiryStatus("GOOD", GoodGreen.copy(alpha = 0.2f), GoodGreen)
    }
}

fun getEmoji(iconId: String): String {
    return when (iconId) {
        "milk" -> "🥛"
        "chicken" -> "🍗"
        "meat" -> "🥩"
        "juice" -> "🧃"
        "fruit" -> "🍎"
        "citrus" -> "🍊"
        "veg" -> "🥬"
        "cheese" -> "🧀"
        "bread" -> "🍞"
        "fish" -> "🐟"
        else -> "🍽️"
    }
}

@Composable
private fun Icon(icon: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.material3.Icon(icon, contentDescription, modifier = Modifier.size(size))
}

@Composable
@Preview(showBackground = true)
fun FoodItemCardPreview() {
    FoodTrackerTheme {
        FoodItemCard(
            item = FoodItem(
                name = "Milk",
                category = "Dairy",
                quantity = 2,
                expiryDate = "2026-05-20",
                iconId = "milk"
            ),
            onDelete = {},
            onUpdateQuantity = {}
        )
    }
}

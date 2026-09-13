package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.ProductEntity
import com.example.data.model.StockMovementEntity
import com.example.ui.components.StockBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.components.sharpCardBorder
import com.example.ui.theme.PosError
import com.example.ui.theme.PosErrorContainer
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.theme.PosWarning
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val movements by viewModel.stockMovements.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Stock Overview, 1 = Stock Movements Trail, 2 = Expired & Expiry Tracker
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var adjustProductTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var writeOffTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var expiryFilter by remember { mutableStateOf("ALL") } // "ALL", "EXPIRED", "EXPIRING_SOON"

    val now = System.currentTimeMillis()
    val lowStockCount = products.count { it.currentStock <= it.minStockLevel }
    val outOfStockCount = products.count { it.currentStock == 0 }

    val expiredProducts = remember(products, now) {
        products.filter { it.expiryDate != null && it.expiryDate <= now }
    }
    val expiringSoonProducts = remember(products, now) {
        products.filter { it.expiryDate != null && it.expiryDate > now && it.expiryDate <= now + (14L * 24 * 3600 * 1000) }
    }
    val trackedExpiryProducts = remember(products, now, expiryFilter) {
        val base = products.filter { it.expiryDate != null }
        when (expiryFilter) {
            "EXPIRED" -> base.filter { it.expiryDate != null && it.expiryDate <= now }
            "EXPIRING_SOON" -> base.filter { it.expiryDate != null && it.expiryDate > now && it.expiryDate <= now + (14L * 24 * 3600 * 1000) }
            else -> base.sortedBy { it.expiryDate ?: Long.MAX_VALUE }
        }
    }

    val displayedProducts = remember(products, filterLowStockOnly) {
        if (filterLowStockOnly) products.filter { it.currentStock <= it.minStockLevel } else products
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stock Status (${products.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Movements (${movements.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            if (expiredProducts.isNotEmpty()) "Expired (${expiredProducts.size})" else "Expiry Tracker",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (expiredProducts.isNotEmpty()) PosError else LocalContentColor.current
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = if (expiredProducts.isNotEmpty()) PosError else LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // Stock Overview Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    // Alert Cards Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = sharpCardBorder(),
                            colors = CardDefaults.cardColors(containerColor = if (lowStockCount > 0) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Low Stock Alert", fontSize = 11.sp, color = PosWarning, fontWeight = FontWeight.Bold)
                                Text("$lowStockCount Items", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("Stock <= Minimum", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = sharpCardBorder(),
                            colors = CardDefaults.cardColors(containerColor = if (outOfStockCount > 0) PosErrorContainer else MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Depleted", fontSize = 11.sp, color = PosError, fontWeight = FontWeight.Bold)
                                Text("$outOfStockCount Items", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("0 Units Remaining", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (filterLowStockOnly) "Showing Low Stock (${displayedProducts.size})" else "All Catalog Stock (${displayedProducts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        FilterChip(
                            selected = filterLowStockOnly,
                            onClick = { filterLowStockOnly = !filterLowStockOnly },
                            label = { Text("Show Critical Only") },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedProducts) { product ->
                            StockItemCard(
                                product = product,
                                onAdjust = { adjustProductTarget = product }
                            )
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // Stock Movement Trail Tab
                if (movements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No stock movements logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(movements) { movement ->
                            StockMovementRow(movement)
                        }
                    }
                }
            } else {
                // Tab 2: Expired Products & Expiry Tracker
                Column(modifier = Modifier.fillMaxSize()) {
                    // KPI Overview Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = sharpCardBorder(),
                            colors = CardDefaults.cardColors(containerColor = if (expiredProducts.isNotEmpty()) PosErrorContainer else MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Expired Now", fontSize = 11.sp, color = PosError, fontWeight = FontWeight.Bold)
                                Text("${expiredProducts.size} Items", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("${expiredProducts.sumOf { it.currentStock }} units at risk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = sharpCardBorder(),
                            colors = CardDefaults.cardColors(containerColor = if (expiringSoonProducts.isNotEmpty()) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Expiring <= 14d", fontSize = 11.sp, color = PosWarning, fontWeight = FontWeight.Bold)
                                Text("${expiringSoonProducts.size} Items", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("${expiringSoonProducts.sumOf { it.currentStock }} units soon", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Filter chips for expired section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = expiryFilter == "ALL",
                            onClick = { expiryFilter = "ALL" },
                            label = { Text("All Tracked (${products.count { it.expiryDate != null }})") },
                            shape = RoundedCornerShape(14.dp)
                        )
                        FilterChip(
                            selected = expiryFilter == "EXPIRED",
                            onClick = { expiryFilter = "EXPIRED" },
                            label = { Text("Expired (${expiredProducts.size})") },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosError,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = expiryFilter == "EXPIRING_SOON",
                            onClick = { expiryFilter = "EXPIRING_SOON" },
                            label = { Text("Expiring Soon (${expiringSoonProducts.size})") },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (trackedExpiryProducts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No products match the expiry filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(trackedExpiryProducts) { product ->
                                ExpiredProductCard(
                                    product = product,
                                    now = now,
                                    onWriteOff = { writeOffTarget = product }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Write-Off Expired Stock Dialog
    writeOffTarget?.let { product ->
        var writeOffReason by remember { mutableStateOf("Expired Stock - Disposal / Write-Off") }
        AlertDialog(
            onDismissRequest = { writeOffTarget = null },
            title = {
                Text("Write-Off Expired Stock", fontWeight = FontWeight.Bold, color = PosError)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Are you sure you want to write off and remove all expired inventory for:",
                        fontSize = 13.sp
                    )
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Batch: ${product.batchNumber} • Units to remove: ${product.currentStock} units",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = writeOffReason,
                        onValueChange = { writeOffReason = it },
                        label = { Text("Audit / Disposal Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.writeOffExpiredStock(
                            product = product,
                            quantity = product.currentStock,
                            reason = writeOffReason
                        ) {
                            Toast.makeText(context, "Written off ${product.currentStock} units of ${product.name}", Toast.LENGTH_SHORT).show()
                            writeOffTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosError)
                ) {
                    Text("Confirm Disposal", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { writeOffTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Stock Adjustment Dialog
    adjustProductTarget?.let { product ->
        var adjustmentQtyText by remember { mutableStateOf("") }
        var isAddition by remember { mutableStateOf(true) } // true = Stock In, false = Stock Out
        var reasonText by remember { mutableStateOf("Manual Audit Count") }

        AlertDialog(
            onDismissRequest = { adjustProductTarget = null },
            title = {
                Text("Adjust Stock: ${product.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current stock on record: ${product.currentStock} units", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    // Direction toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isAddition = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAddition) PosSuccess else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isAddition) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stock In (+)")
                        }

                        Button(
                            onClick = { isAddition = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isAddition) PosError else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isAddition) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stock Out (-)")
                        }
                    }

                    OutlinedTextField(
                        value = adjustmentQtyText,
                        onValueChange = { adjustmentQtyText = it },
                        label = { Text("Quantity to adjust") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason for Adjustment") },
                        placeholder = { Text("e.g. Damaged box, Re-count, Sample") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = adjustmentQtyText.toIntOrNull() ?: 0
                        if (qty > 0) {
                            val change = if (isAddition) qty else -qty
                            viewModel.adjustStock(
                                product = product,
                                quantityChanged = change,
                                reason = reasonText.ifBlank { "Manual Adjustment" }
                            ) {
                                adjustProductTarget = null
                            }
                        }
                    }
                ) {
                    Text("Apply Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { adjustProductTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StockItemCard(
    product: ProductEntity,
    onAdjust: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sharpCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "SKU: ${product.sku} • Min Level: ${product.minStockLevel}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                StockBadge(currentStock = product.currentStock, minStock = product.minStockLevel)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${product.currentStock}", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("units", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAdjust,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StockMovementRow(movement: StockMovementEntity) {
    val isPositive = movement.quantityChanged >= 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sharpCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isPositive) PosSuccessContainer else PosErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isPositive) PosSuccess else PosError,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(movement.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "${movement.type} • ${movement.reason}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "By: ${movement.performedBy} • ${formatDateTime(movement.timestamp)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPositive) "+${movement.quantityChanged}" else "${movement.quantityChanged}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (isPositive) PosSuccess else PosError
                )
                Text("Stock: ${movement.newStock}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExpiredProductCard(
    product: ProductEntity,
    now: Long,
    onWriteOff: () -> Unit
) {
    val expiry = product.expiryDate ?: return
    val diffMillis = expiry - now
    val diffDays = (diffMillis / (24L * 3600 * 1000)).toInt()
    val isExpired = diffMillis <= 0
    val isExpiringSoon = !isExpired && diffDays <= 14

    val statusColor = when {
        isExpired -> PosError
        isExpiringSoon -> PosWarning
        else -> PosSuccess
    }

    val statusBg = when {
        isExpired -> PosErrorContainer
        isExpiringSoon -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else -> PosSuccessContainer
    }

    val statusText = when {
        isExpired -> {
            val daysAgo = kotlin.math.abs(diffDays)
            if (daysAgo == 0) "EXPIRED TODAY" else "EXPIRED $daysAgo DAYS AGO"
        }
        isExpiringSoon -> "EXPIRES IN $diffDays DAYS"
        else -> "Expires in $diffDays days"
    }

    val dateFormatted = remember(expiry) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expiry))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = sharpCardBorder(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired && product.currentStock > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${product.category} • ${product.brand} • SKU: ${product.sku}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Batch: ${product.batchNumber.ifBlank { "N/A" }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Expiry Date: $dateFormatted",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Stock: ${product.currentStock} units",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.currentStock == 0) MaterialTheme.colorScheme.onSurfaceVariant else if (isExpired) PosError else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Val: ${formatCurrency(product.currentStock * product.costPrice)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (product.currentStock > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onWriteOff,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpired) PosError else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isExpired) "Write-Off / Dispose Expired Units" else "Remove / Dispose Batch Units",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

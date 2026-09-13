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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.ProductEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.SupplierEntity
import com.example.data.repository.PurchaseCartItem
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PosViewModel
) {
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()

    var showNewPurchaseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchases & Supplier Restock", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewPurchaseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Purchase Order")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (purchases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No purchase orders yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap + to order new stock from suppliers.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(purchases) { po ->
                        PurchaseOrderCard(po)
                    }
                }
            }
        }
    }

    // New Purchase Order Dialog
    if (showNewPurchaseDialog) {
        NewPurchaseOrderDialog(
            suppliers = suppliers,
            products = products,
            onDismiss = { showNewPurchaseDialog = false },
            onConfirm = { supplier, items, tax, disc ->
                viewModel.processPurchaseOrder(
                    supplier = supplier,
                    items = items,
                    taxPercent = tax,
                    discountPercent = disc,
                    onSuccess = { showNewPurchaseDialog = false },
                    onError = { /* handled */ }
                )
            }
        )
    }
}

@Composable
private fun PurchaseOrderCard(po: PurchaseEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(po.purchaseNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                StatusBadge(text = po.status, containerColor = PosSuccessContainer, contentColor = PosSuccess)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Supplier: ${po.supplierName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Received by: ${po.receivedByCashier} • ${formatDateTime(po.timestamp)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Cost:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatCurrency(po.grandTotal),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPurchaseOrderDialog(
    suppliers: List<SupplierEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (SupplierEntity, List<PurchaseCartItem>, Double, Double) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var supplierExpanded by remember { mutableStateOf(false) }

    val orderItems = remember { androidx.compose.runtime.mutableStateListOf<PurchaseCartItem>() }

    var selectedProductToAdd by remember { mutableStateOf(products.firstOrNull()) }
    var productExpanded by remember { mutableStateOf(false) }
    var itemQtyText by remember { mutableStateOf("10") }
    var itemCostText by remember { mutableStateOf(products.firstOrNull()?.costPrice?.toString() ?: "5.00") }

    var taxPercentText by remember { mutableStateOf("0.0") }
    var discountPercentText by remember { mutableStateOf("0.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val subtotal = orderItems.sumOf { it.totalCost }
    val discPct = discountPercentText.toDoubleOrNull() ?: 0.0
    val taxPct = taxPercentText.toDoubleOrNull() ?: 0.0
    val discAmt = subtotal * (discPct / 100.0)
    val taxAmt = (subtotal - discAmt) * (taxPct / 100.0)
    val grandTotal = subtotal - discAmt + taxAmt

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Purchase Order (Restock)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Supplier dropdown
                Text("Select Supplier:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: "Select Supplier",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
                        suppliers.forEach { supp ->
                            DropdownMenuItem(
                                text = { Text(supp.name) },
                                onClick = {
                                    selectedSupplier = supp
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider(thickness = 0.5.dp)

                // Add Items Section
                Text("Add Products to PO:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProductToAdd?.name ?: "Select Product",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.name} (Current: ${prod.currentStock})") },
                                onClick = {
                                    selectedProductToAdd = prod
                                    itemCostText = "${prod.costPrice}"
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = itemQtyText,
                        onValueChange = { itemQtyText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = itemCostText,
                        onValueChange = { itemCostText = it },
                        label = { Text("Unit Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Button(
                    onClick = {
                        val prod = selectedProductToAdd
                        val qty = itemQtyText.toIntOrNull() ?: 0
                        val cost = itemCostText.toDoubleOrNull() ?: 0.0
                        if (prod != null && qty > 0 && cost >= 0) {
                            orderItems.add(PurchaseCartItem(prod, qty, cost))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Line Item")
                }

                // Line items list
                if (orderItems.isNotEmpty()) {
                    Text("Items to Receive:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    orderItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${item.quantity} units @ ${formatCurrency(item.unitCost)} = ${formatCurrency(item.totalCost)}", fontSize = 11.sp)
                            }
                            IconButton(onClick = { orderItems.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Order Grand Total:", fontWeight = FontWeight.Bold)
                        Text(formatCurrency(grandTotal), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val supp = selectedSupplier
                    if (supp == null) {
                        errorMessage = "Please select a supplier."
                        return@Button
                    }
                    if (orderItems.isEmpty()) {
                        errorMessage = "Please add at least one product."
                        return@Button
                    }
                    onConfirm(supp, orderItems.toList(), taxPct, discPct)
                }
            ) {
                Text("Confirm Stock In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

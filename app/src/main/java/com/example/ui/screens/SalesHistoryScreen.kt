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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SaleEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.PosError
import com.example.ui.theme.PosErrorContainer
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.viewmodel.PosViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: PosViewModel
) {
    val sales by viewModel.sales.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("All") } // "Today", "This Week", "All"
    var selectedPaymentFilter by remember { mutableStateOf("All") } // "All", "CASH", "CARD", "DIGITAL_WALLET"

    var saleToRefund by remember { mutableStateOf<SaleEntity?>(null) }

    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startOfWeek = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val filteredSales = remember(sales, searchQuery, selectedDateFilter, selectedPaymentFilter) {
        sales.filter { sale ->
            val matchesSearch = searchQuery.isBlank() ||
                    sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    sale.customerName.contains(searchQuery, ignoreCase = true) ||
                    sale.cashierName.contains(searchQuery, ignoreCase = true)

            val matchesDate = when (selectedDateFilter) {
                "Today" -> sale.timestamp >= startOfToday
                "This Week" -> sale.timestamp >= startOfWeek
                else -> true
            }

            val matchesPayment = selectedPaymentFilter == "All" ||
                    sale.paymentMethod.equals(selectedPaymentFilter, ignoreCase = true)

            matchesSearch && matchesDate && matchesPayment
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sales & Orders History", fontWeight = FontWeight.Bold)
                        Text("${sales.size} total invoices recorded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
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
            // Search and filters
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search invoice #, customer, cashier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Today", "This Week").forEach { filter ->
                        item {
                            val isSelected = selectedDateFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDateFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    listOf("All", "CASH", "CARD", "SPLIT").forEach { payFilter ->
                        item {
                            val isSelected = selectedPaymentFilter == payFilter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPaymentFilter = payFilter },
                                label = { Text(if (payFilter == "All") "All Payments" else payFilter, fontSize = 11.sp) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // Sales list
            if (filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sales match current filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSales) { sale ->
                        SaleHistoryCard(
                            sale = sale,
                            canRefund = currentUser?.role?.uppercase() in listOf("ADMIN", "MANAGER"),
                            onViewReceipt = { viewModel.inspectSaleReceipt(sale) },
                            onRequestRefund = { saleToRefund = sale }
                        )
                    }
                }
            }
        }
    }

    // Refund Dialog
    saleToRefund?.let { sale ->
        var refundReason by remember { mutableStateOf("Customer Return / Defective") }
        var refundError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { saleToRefund = null },
            title = { Text("Issue Full Refund", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Refund invoice ${sale.invoiceNumber} for ${formatCurrency(sale.grandTotal)}?")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Products will be safely returned to inventory and an audit log will be created.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        label = { Text("Reason for refund") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (refundError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(refundError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refundSale(
                            sale = sale,
                            reason = refundReason.ifBlank { "Customer Return" },
                            onSuccess = { saleToRefund = null },
                            onError = { err -> refundError = err }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Refund")
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToRefund = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SaleHistoryCard(
    sale: SaleEntity,
    canRefund: Boolean,
    onViewReceipt: () -> Unit,
    onRequestRefund: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sale.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (sale.status == "REFUNDED") {
                        StatusBadge("REFUNDED", PosErrorContainer, PosError)
                    } else {
                        StatusBadge(sale.paymentMethod, PosSuccessContainer, PosSuccess)
                    }
                }

                Text(
                    formatCurrency(sale.grandTotal),
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = if (sale.status == "REFUNDED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Customer: ${sale.customerName} • Cashier: ${sale.cashierName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDateTime(sale.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (sale.status == "REFUNDED" && !sale.refundReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Refund reason: ${sale.refundReason} (by ${sale.refundedBy})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewReceipt,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Receipt", fontSize = 11.sp)
                }

                if (sale.status != "REFUNDED" && canRefund) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestRefund,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refund", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

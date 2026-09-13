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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SaleEntity
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.components.sharpCardBorder
import com.example.ui.theme.PosError
import com.example.ui.theme.PosErrorContainer
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.theme.PosWarning
import com.example.ui.viewmodel.PosViewModel
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: PosViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val products by viewModel.products.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()

    // Calculate Today's metrics
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todaySales = sales.filter { it.timestamp >= startOfToday && it.status != "REFUNDED" }
    val todaySalesTotal = todaySales.sumOf { it.grandTotal }
    val todayTransactionsCount = todaySales.size

    val todayPurchases = purchases.filter { it.timestamp >= startOfToday }
    val todayPurchasesTotal = todayPurchases.sumOf { it.grandTotal }

    val lowStockCount = products.count { it.currentStock <= it.minStockLevel }

    // Sales by Cashier today
    val salesByCashier = todaySales.groupBy { it.cashierName }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Store Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Downtown Terminal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PosSuccess)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Logged in: ${currentUser?.fullName ?: "Staff"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        currentUser?.let { RoleBadge(it.role) }
                    }
                }

                // Notification Bell
                Box {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(PosError)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$unreadCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Shortcuts Carousel / Grid
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    QuickActionCard(
                        title = "New Sale",
                        subtitle = "Open POS Register",
                        icon = Icons.Default.PointOfSale,
                        containerColor = PosPrimary,
                        contentColor = Color.White,
                        onClick = onNavigateToPos
                    )
                }
                item {
                    QuickActionCard(
                        title = "Products",
                        subtitle = "${products.size} in catalog",
                        icon = Icons.Default.AddShoppingCart,
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        onClick = onNavigateToProducts
                    )
                }
                item {
                    QuickActionCard(
                        title = "Purchase Stock",
                        subtitle = "Supplier Restock",
                        icon = Icons.Default.LocalShipping,
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White,
                        onClick = onNavigateToPurchases
                    )
                }
                item {
                    QuickActionCard(
                        title = "Inventory",
                        subtitle = "$lowStockCount need restock",
                        icon = Icons.Default.Inventory,
                        containerColor = if (lowStockCount > 0) Color(0xFFD97706) else Color(0xFF475569),
                        contentColor = Color.White,
                        onClick = onNavigateToInventory
                    )
                }
                item {
                    QuickActionCard(
                        title = "Sales History",
                        subtitle = "${sales.size} total orders",
                        icon = Icons.Default.ReceiptLong,
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                        onClick = onNavigateToSales
                    )
                }
                item {
                    QuickActionCard(
                        title = "Customers",
                        subtitle = "CRM & Accounts",
                        icon = Icons.Default.People,
                        containerColor = Color(0xFF0F766E),
                        contentColor = Color.White,
                        onClick = onNavigateToCustomers
                    )
                }
                item {
                    QuickActionCard(
                        title = "Reports",
                        subtitle = "Profit & KPIs",
                        icon = Icons.Default.Assessment,
                        containerColor = Color(0xFF334155),
                        contentColor = Color.White,
                        onClick = onNavigateToReports
                    )
                }
            }
        }

        // Today's Key Performance Indicators
        item {
            Text(
                text = "Today's Performance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Today's Sales",
                    value = formatCurrency(todaySalesTotal),
                    subtitle = "$todayTransactionsCount transactions",
                    icon = Icons.Default.AttachMoney,
                    iconTint = PosSuccess,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Today's Purchases",
                    value = formatCurrency(todayPurchasesTotal),
                    subtitle = "${todayPurchases.size} purchase orders",
                    icon = Icons.Default.LocalShipping,
                    iconTint = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Total Catalog",
                    value = "${products.size} Items",
                    subtitle = "Active products",
                    icon = Icons.Default.Inventory,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Low Stock Alerts",
                    value = "$lowStockCount Items",
                    subtitle = if (lowStockCount > 0) "Immediate reorder required" else "All healthy",
                    icon = Icons.Default.Warning,
                    iconTint = if (lowStockCount > 0) PosWarning else PosSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Cashier Performance Today
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = sharpCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sales Activity by Cashier (Today)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (salesByCashier.isEmpty()) {
                        Text(
                            text = "No sales recorded yet today.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        salesByCashier.forEach { (cashierName, cashierSales) ->
                            val cashierSum = cashierSales.sumOf { it.grandTotal }
                            val cashierCount = cashierSales.size
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            cashierName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(cashierName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("$cashierCount sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Text(
                                    formatCurrency(cashierSum),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        // Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToSales() }
                )
            }
        }

        if (sales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = sharpCardBorder()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No sales recorded yet. Tap 'New Sale' to start ringing up orders!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sales.take(5)) { sale ->
                RecentSaleCard(sale = sale, onClick = { viewModel.inspectSaleReceipt(sale) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(115.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sharpCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentSaleCard(sale: SaleEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sharpCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(sale.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${sale.cashierName} • ${sale.customerName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatDateTime(sale.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(sale.grandTotal),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (sale.status == "REFUNDED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (sale.status == "REFUNDED") {
                    StatusBadge(text = "REFUNDED", containerColor = PosErrorContainer, contentColor = PosError)
                } else {
                    StatusBadge(text = sale.paymentMethod, containerColor = PosSuccessContainer, contentColor = PosSuccess)
                }
            }
        }
    }
}

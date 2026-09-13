package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.model.SaleEntity
import com.example.ui.components.formatCurrency
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSuccess
import com.example.ui.util.PdfReceiptGenerator
import com.example.ui.viewmodel.PosViewModel
import java.util.Calendar

data class TopProductItem(val name: String, val units: Int, val revenue: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsState()
    val saleItems by viewModel.saleItems.collectAsState()
    val products by viewModel.products.collectAsState()

    var selectedPeriod by remember { mutableStateOf("Today") } // "Today", "Weekly", "Monthly", "Yearly", "All Time"

    val startOfPeriod = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "Today" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "Weekly" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "Monthly" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "Yearly" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    val periodSales = remember(sales, startOfPeriod) {
        sales.filter { it.timestamp >= startOfPeriod && it.status != "REFUNDED" }
    }

    val totalRevenue = periodSales.sumOf { it.grandTotal }
    val totalTransactions = periodSales.size
    val averageTicket = if (totalTransactions > 0) totalRevenue / totalTransactions else 0.0

    // Estimation of COGS & Net Profit
    val periodSaleIds = periodSales.map { it.id }.toSet()
    val relevantItems = saleItems.filter { periodSaleIds.contains(it.saleId) }

    var estimatedCOGS = 0.0
    relevantItems.forEach { item ->
        val prod = products.find { it.id == item.productId }
        val cost = prod?.costPrice ?: (item.unitSellingPrice * 0.6)
        estimatedCOGS += (cost * item.quantity)
    }

    val estimatedGrossProfit = (totalRevenue - estimatedCOGS).coerceAtLeast(0.0)
    val profitMargin = if (totalRevenue > 0) (estimatedGrossProfit / totalRevenue) * 100 else 0.0

    // Top Selling Products in this period
    val itemsByProduct = relevantItems.groupBy { it.productName }
    val topProducts = itemsByProduct.map { (name, list) ->
        val units = list.sumOf { it.quantity }
        val revenue = list.sumOf { it.itemTotal }
        TopProductItem(name = name, units = units, revenue = revenue)
    }.sortedByDescending { it.revenue }.take(5)

    // Sales by Cashier
    val salesByCashier = periodSales.groupBy { it.cashierName }.mapValues { (_, sList) ->
        sList.sumOf { it.grandTotal }
    }

    // Payment Methods breakdown
    val paymentBreakdown = periodSales.groupBy { it.paymentMethod }.mapValues { (_, sList) ->
        sList.sumOf { it.grandTotal }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sales & Analytics", fontWeight = FontWeight.Bold)
                        Text("Report: $selectedPeriod", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                val topProductsMapped = topProducts.map { Pair(it.name, Pair(it.units, it.revenue)) }
                                val pdf = PdfReceiptGenerator.generateSalesReportPdf(
                                    context = context,
                                    periodName = selectedPeriod,
                                    totalRevenue = totalRevenue,
                                    estimatedProfit = estimatedGrossProfit,
                                    estimatedCOGS = estimatedCOGS,
                                    totalTransactions = totalTransactions,
                                    averageTicket = averageTicket,
                                    topProducts = topProductsMapped,
                                    sales = periodSales
                                )
                                Toast.makeText(context, "Sales Report PDF created: ${pdf.name}", Toast.LENGTH_SHORT).show()
                                PdfReceiptGenerator.openPdf(context, pdf)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Report export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF Report", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period Selector Chips (Today, Weekly, Monthly, Yearly, All Time)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Today", "Weekly", "Monthly", "Yearly", "All Time").forEach { period ->
                        item {
                            val isSelected = selectedPeriod == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPeriod = period },
                                label = { Text(period, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Hero Summary Card: "Total Sales Done Today / Weekly / Monthly / Yearly"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedPeriod == "Today") "Total Sales Done Today" else "Total Sales ($selectedPeriod)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "$totalTransactions completed",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formatCurrency(totalRevenue),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Net Margin: ${profitMargin.toInt()}% • Avg Order: ${formatCurrency(averageTicket)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick PDF action row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val topProductsMapped = topProducts.map { Pair(it.name, Pair(it.units, it.revenue)) }
                                        val pdf = PdfReceiptGenerator.generateSalesReportPdf(
                                            context = context,
                                            periodName = selectedPeriod,
                                            totalRevenue = totalRevenue,
                                            estimatedProfit = estimatedGrossProfit,
                                            estimatedCOGS = estimatedCOGS,
                                            totalTransactions = totalTransactions,
                                            averageTicket = averageTicket,
                                            topProducts = topProductsMapped,
                                            sales = periodSales
                                        )
                                        PdfReceiptGenerator.openPdf(context, pdf)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View PDF Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val topProductsMapped = topProducts.map { Pair(it.name, Pair(it.units, it.revenue)) }
                                        val pdf = PdfReceiptGenerator.generateSalesReportPdf(
                                            context = context,
                                            periodName = selectedPeriod,
                                            totalRevenue = totalRevenue,
                                            estimatedProfit = estimatedGrossProfit,
                                            estimatedCOGS = estimatedCOGS,
                                            totalTransactions = totalTransactions,
                                            averageTicket = averageTicket,
                                            topProducts = topProductsMapped,
                                            sales = periodSales
                                        )
                                        PdfReceiptGenerator.sharePdf(context, pdf, "Sales_Report_${selectedPeriod}")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share PDF", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Financial KPIs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportStatCard(
                        title = "Gross Revenue",
                        value = formatCurrency(totalRevenue),
                        subtitle = "$totalTransactions orders",
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatCard(
                        title = "Estimated Profit",
                        value = formatCurrency(estimatedGrossProfit),
                        subtitle = "${profitMargin.toInt()}% gross margin",
                        valueColor = PosSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportStatCard(
                        title = "Est. Cost of Goods",
                        value = formatCurrency(estimatedCOGS),
                        subtitle = "Inventory cost",
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatCard(
                        title = "Avg Ticket Size",
                        value = formatCurrency(averageTicket),
                        subtitle = "Per transaction",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Top-Selling Products
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Top-Selling Products ($selectedPeriod)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (topProducts.isEmpty()) {
                            Text("No product sales recorded in this period.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val maxRev = topProducts.firstOrNull()?.revenue ?: 1.0
                            for (item in topProducts) {
                                val fraction = (item.revenue / maxRev).toFloat().coerceIn(0.05f, 1f)

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("${formatCurrency(item.revenue)} (${item.units} sold)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = fraction,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            // Sales by Cashier Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sales Volume by Cashier", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (salesByCashier.isEmpty()) {
                            Text("No cashier data for this period.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for ((cashier, amount) in salesByCashier) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cashier, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(formatCurrency(amount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            // Payment Methods Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Payment Methods Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (paymentBreakdown.isEmpty()) {
                            Text("No payments collected in this period.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            for ((method, amt) in paymentBreakdown) {
                                val pct = if (totalRevenue > 0) (amt / totalRevenue) * 100 else 0.0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(method.replace("_", " "), fontSize = 13.sp)
                                    Text(
                                        "${formatCurrency(amt)} (${pct.toInt()}%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = valueColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

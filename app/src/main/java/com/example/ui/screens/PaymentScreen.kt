package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.formatCurrency
import com.example.ui.components.sharpCardBorder
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSuccess
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit,
    onSaleCompleted: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val cartDiscountPercent by viewModel.cartDiscountPercent.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Calculate figures
    var subtotal = 0.0
    var itemDiscountSum = 0.0
    var taxSum = 0.0

    cartItems.forEach { item ->
        val regPrice = item.product.sellingPrice * item.quantity
        val discAmt = regPrice * (item.itemDiscountPercent / 100.0)
        val lineSub = regPrice - discAmt
        val lineTax = lineSub * (item.product.taxPercent / 100.0)

        subtotal += regPrice
        itemDiscountSum += discAmt
        taxSum += lineTax
    }

    val subtotalAfterItemDisc = subtotal - itemDiscountSum
    val cartDiscountAmt = subtotalAfterItemDisc * (cartDiscountPercent / 100.0)
    val totalDiscount = itemDiscountSum + cartDiscountAmt
    val finalTax = taxSum * (1.0 - (cartDiscountPercent / 100.0))
    val grandTotal = (subtotalAfterItemDisc - cartDiscountAmt + finalTax).coerceAtLeast(0.0)

    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var cashTenderedText by remember { mutableStateOf(String.format("%.2f", grandTotal)) }

    // Split payment fields
    var splitCashText by remember { mutableStateOf(String.format("%.2f", grandTotal / 2)) }
    var splitDigitalText by remember { mutableStateOf(String.format("%.2f", grandTotal / 2)) }

    var isProcessing by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val amountReceived = when (selectedPaymentMethod) {
        "CASH" -> cashTenderedText.toDoubleOrNull() ?: 0.0
        "SPLIT" -> (splitCashText.toDoubleOrNull() ?: 0.0) + (splitDigitalText.toDoubleOrNull() ?: 0.0)
        else -> grandTotal // Card, Bank, Wallet pay exact
    }
    val changeGiven = (amountReceived - grandTotal).coerceAtLeast(0.0)
    val remainingDue = (grandTotal - amountReceived).coerceAtLeast(0.0)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout & Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (paymentError != null) {
                        Text(
                            text = paymentError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (selectedPaymentMethod == "CASH" && amountReceived < grandTotal) {
                                paymentError = "Tender amount cannot be less than total payable."
                                return@Button
                            }
                            if (selectedPaymentMethod == "SPLIT" && remainingDue > 0.01) {
                                paymentError = "Split payment parts must cover the full payable amount."
                                return@Button
                            }

                            isProcessing = true
                            paymentError = null
                            viewModel.checkout(
                                paymentMethod = selectedPaymentMethod,
                                amountReceived = amountReceived,
                                splitCashAmount = if (selectedPaymentMethod == "SPLIT") (splitCashText.toDoubleOrNull() ?: 0.0) else 0.0,
                                splitDigitalAmount = if (selectedPaymentMethod == "SPLIT") (splitDigitalText.toDoubleOrNull() ?: 0.0) else 0.0,
                                onSuccess = {
                                    isProcessing = false
                                    onSaleCompleted()
                                },
                                onError = { err ->
                                    isProcessing = false
                                    paymentError = err
                                    scope.launch { snackbarHostState.showSnackbar(err) }
                                }
                            )
                        },
                        enabled = !isProcessing && cartItems.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "Confirm Payment • ${formatCurrency(grandTotal)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = sharpCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ORDER SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal (${cartItems.sumOf { it.quantity }} items):", fontSize = 13.sp)
                        Text(formatCurrency(subtotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (totalDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Discounts:", fontSize = 13.sp, color = Color(0xFFD97706))
                            Text("-${formatCurrency(totalDiscount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated Tax:", fontSize = 13.sp)
                        Text(formatCurrency(finalTax), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL PAYABLE", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(
                                "Cashier: ${currentUser?.fullName ?: "Staff"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatCurrency(grandTotal),
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Payment Methods Grid / Tabs
            Text(
                text = "Select Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentMethodOption(
                    title = "Cash",
                    icon = Icons.Default.LocalAtm,
                    isSelected = selectedPaymentMethod == "CASH",
                    onClick = {
                        selectedPaymentMethod = "CASH"
                        cashTenderedText = String.format("%.2f", grandTotal)
                    }
                )
                PaymentMethodOption(
                    title = "Credit/Debit Card",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedPaymentMethod == "CARD",
                    onClick = { selectedPaymentMethod = "CARD" }
                )
                PaymentMethodOption(
                    title = "Digital Wallet",
                    icon = Icons.Default.PhoneAndroid,
                    isSelected = selectedPaymentMethod == "DIGITAL_WALLET",
                    onClick = { selectedPaymentMethod = "DIGITAL_WALLET" }
                )
                PaymentMethodOption(
                    title = "Bank Transfer",
                    icon = Icons.Default.AccountBalance,
                    isSelected = selectedPaymentMethod == "BANK_TRANSFER",
                    onClick = { selectedPaymentMethod = "BANK_TRANSFER" }
                )
                PaymentMethodOption(
                    title = "Split Payment",
                    icon = Icons.Default.Payments,
                    isSelected = selectedPaymentMethod == "SPLIT",
                    onClick = {
                        selectedPaymentMethod = "SPLIT"
                        splitCashText = String.format("%.2f", grandTotal / 2)
                        splitDigitalText = String.format("%.2f", grandTotal - (grandTotal / 2))
                    }
                )
            }

            // Payment Details Card based on selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = sharpCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (selectedPaymentMethod) {
                        "CASH" -> {
                            Text("Cash Tendered", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = cashTenderedText,
                                onValueChange = { cashTenderedText = it },
                                label = { Text("Amount Tendered ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Fast tender denomination shortcuts
                            Text("Quick Denominations:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            val nextTen = (ceil(grandTotal / 10.0) * 10.0).coerceAtLeast(10.0)
                            val nextTwenty = (ceil(grandTotal / 20.0) * 20.0).coerceAtLeast(20.0)
                            val nextFifty = (ceil(grandTotal / 50.0) * 50.0).coerceAtLeast(50.0)
                            val nextHundred = (ceil(grandTotal / 100.0) * 100.0).coerceAtLeast(100.0)

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { cashTenderedText = String.format("%.2f", grandTotal) }) {
                                    Text("Exact")
                                }
                                OutlinedButton(onClick = { cashTenderedText = String.format("%.2f", nextTen) }) {
                                    Text("$$nextTen")
                                }
                                OutlinedButton(onClick = { cashTenderedText = String.format("%.2f", nextTwenty) }) {
                                    Text("$$nextTwenty")
                                }
                                OutlinedButton(onClick = { cashTenderedText = String.format("%.2f", nextFifty) }) {
                                    Text("$$nextFifty")
                                }
                                OutlinedButton(onClick = { cashTenderedText = String.format("%.2f", nextHundred) }) {
                                    Text("$$nextHundred")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Change Due to Customer:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    formatCurrency(changeGiven),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = PosSuccess
                                )
                            }
                        }

                        "SPLIT" -> {
                            Text("Split Payment Allocation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = splitCashText,
                                onValueChange = {
                                    splitCashText = it
                                    val cashVal = it.toDoubleOrNull() ?: 0.0
                                    splitDigitalText = String.format("%.2f", (grandTotal - cashVal).coerceAtLeast(0.0))
                                },
                                label = { Text("Portion 1: Cash Amount ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = splitDigitalText,
                                onValueChange = { splitDigitalText = it },
                                label = { Text("Portion 2: Card / Digital Amount ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Covered:", fontSize = 12.sp)
                                Text(formatCurrency(amountReceived), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (remainingDue > 0.01) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining Due:", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    Text(formatCurrency(remainingDue), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        else -> {
                            // Card / Digital / Bank transfer
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("External Terminal Ready", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Collect ${formatCurrency(grandTotal)} on payment reader or digital wallet QR.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(70.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

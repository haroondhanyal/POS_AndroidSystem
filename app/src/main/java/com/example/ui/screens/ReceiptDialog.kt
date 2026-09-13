package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import com.example.ui.util.PdfReceiptGenerator
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.PosSuccess

@Composable
fun ReceiptDialog(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    onDismiss: () -> Unit,
    onStartNewSale: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PosSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sale.status == "REFUNDED") "REFUND RECEIPT" else "TRANSACTION RECEIPT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sale.status == "REFUNDED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Printable Receipt Canvas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RETAIL POS TERMINAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Downtown Flagship Store #104",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Tel: (555) 019-2831 • Tax ID: US-98421034",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(thickness = 1.dp, color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Invoice metadata
                        ReceiptMetaRow("Invoice No:", sale.invoiceNumber)
                        ReceiptMetaRow("Date & Time:", formatDateTime(sale.timestamp))
                        ReceiptMetaRow("Cashier:", sale.cashierName)
                        ReceiptMetaRow("Customer:", sale.customerName)
                        ReceiptMetaRow("Status:", sale.status)

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(thickness = 1.dp, color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Item", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2f))
                            Text("Qty", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                            Text("Price", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Items list
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (item.discountPercent > 0) {
                                        Text("${item.discountPercent.toInt()}% Disc applied", fontSize = 10.sp, color = Color(0xFFD97706))
                                    }
                                }
                                Text("${item.quantity}", fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                Text(formatCurrency(item.unitSellingPrice), fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                Text(formatCurrency(item.itemTotal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(thickness = 1.dp, color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Totals
                        ReceiptAmountRow("Subtotal:", formatCurrency(sale.subtotal))
                        if (sale.itemDiscountTotal > 0) {
                            ReceiptAmountRow("Item Discounts:", "-${formatCurrency(sale.itemDiscountTotal)}", Color(0xFFD97706))
                        }
                        if (sale.cartDiscountTotal > 0) {
                            ReceiptAmountRow("Cart Discount:", "-${formatCurrency(sale.cartDiscountTotal)}", Color(0xFFD97706))
                        }
                        ReceiptAmountRow("Tax (Est):", formatCurrency(sale.taxTotal))
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(formatCurrency(sale.grandTotal), fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(thickness = 1.dp, color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Payment Details
                        ReceiptAmountRow("Payment Method:", sale.paymentMethod.replace("_", " "))
                        if (sale.paymentMethod == "CASH" || sale.paymentMethod == "SPLIT") {
                            ReceiptAmountRow("Amount Tendered:", formatCurrency(sale.amountReceived))
                            ReceiptAmountRow("Change Returned:", formatCurrency(sale.changeGiven))
                        }

                        if (sale.status == "REFUNDED") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "REFUND PROCESSED BY: ${sale.refundedBy ?: "Staff"}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                            if (!sale.refundReason.isNullOrBlank()) {
                                Text("Reason: ${sale.refundReason}", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Simulated Barcode graphic
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(38.dp)
                                .background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "||||| || |||||| | |||||||| |||| |||||",
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = sale.invoiceNumber,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Thank you for your business!",
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PDF & Print Actions
                Button(
                    onClick = {
                        try {
                            val pdfFile = PdfReceiptGenerator.generateReceiptPdf(context, sale, items)
                            Toast.makeText(context, "PDF Receipt generated: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
                            PdfReceiptGenerator.openPdf(context, pdfFile)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View / Open PDF Receipt", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val pdfFile = PdfReceiptGenerator.generateReceiptPdf(context, sale, items)
                                PdfReceiptGenerator.sharePdf(context, pdfFile, sale.invoiceNumber)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val pdfFile = PdfReceiptGenerator.generateReceiptPdf(context, sale, items)
                                PdfReceiptGenerator.printPdf(context, pdfFile, sale.invoiceNumber)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print PDF", fontSize = 13.sp)
                    }
                }

                if (onStartNewSale != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStartNewSale,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Start Next Sale", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReceiptAmountRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double): String {
    return String.format(Locale.US, "$%.2f", amount)
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun sharpCardBorder(): BorderStroke {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    }
    return BorderStroke(1.dp, borderColor)
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(1.dp, contentColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
fun StockBadge(currentStock: Int, minStock: Int) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    when {
        currentStock <= 0 -> {
            val container = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
            val content = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
            StatusBadge(
                text = "Out of Stock",
                containerColor = container,
                contentColor = content
            )
        }
        currentStock <= minStock -> {
            val container = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
            val content = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
            StatusBadge(
                text = "Low: $currentStock left",
                containerColor = container,
                contentColor = content
            )
        }
        else -> {
            val container = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
            val content = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)
            StatusBadge(
                text = "$currentStock in stock",
                containerColor = container,
                contentColor = content
            )
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (bgColor, txtColor) = when (role.uppercase()) {
        "ADMIN" -> if (isDark) Pair(Color(0xFF3B0764), Color(0xFFD8B4FE)) else Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8))
        "MANAGER" -> if (isDark) Pair(Color(0xFF1E3A8A), Color(0xFF93C5FD)) else Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        else -> if (isDark) Pair(Color(0xFF134E4A), Color(0xFF5EEAD4)) else Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
    }
    StatusBadge(text = role, containerColor = bgColor, contentColor = txtColor)
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    MANAGER,
    CASHIER
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val pin: String,
    val fullName: String,
    val role: String, // UserRole.ADMIN.name, etc.
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val category: String,
    val brand: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val taxPercent: Double = 8.0,
    val discountPercent: Double = 0.0,
    val currentStock: Int = 0,
    val minStockLevel: Int = 5,
    val description: String = "",
    val imageRes: Int = 0,
    val imageUrl: String = "",
    val expiryDate: Long? = null,
    val batchNumber: String = ""
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val cashierId: Long,
    val cashierName: String,
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val subtotal: Double,
    val itemDiscountTotal: Double = 0.0,
    val cartDiscountTotal: Double = 0.0,
    val taxTotal: Double,
    val grandTotal: Double,
    val amountReceived: Double,
    val changeGiven: Double,
    val paymentMethod: String, // CASH, CARD, BANK_TRANSFER, DIGITAL_WALLET, SPLIT
    val splitCashAmount: Double = 0.0,
    val splitDigitalAmount: Double = 0.0,
    val status: String = "COMPLETED", // COMPLETED, REFUNDED
    val refundReason: String? = null,
    val refundedBy: String? = null,
    val refundedAt: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val productSku: String,
    val quantity: Int,
    val unitCostPrice: Double,
    val unitSellingPrice: Double,
    val discountPercent: Double = 0.0,
    val taxPercent: Double = 8.0,
    val itemTotal: Double
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseNumber: String,
    val supplierId: Long,
    val supplierName: String,
    val receivedByCashier: String,
    val subtotal: Double,
    val taxTotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val grandTotal: Double,
    val status: String = "RECEIVED",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val totalCost: Double
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val previousStock: Int,
    val quantityChanged: Int, // e.g. -2 for sale, +10 for purchase/adjustment
    val newStock: Int,
    val type: String, // SALE, PURCHASE, ADJUSTMENT_IN, ADJUSTMENT_OUT, REFUND_RESTOCK
    val reason: String,
    val performedBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val totalPurchases: Int = 0,
    val totalSpent: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val productsSupplied: String = "",
    val totalPurchases: Double = 0.0,
    val outstandingPayable: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userName: String,
    val userRole: String,
    val action: String, // e.g. LOGIN, LOGOUT, NEW_SALE, REFUND, STOCK_ADJUST, ADD_PRODUCT, PURCHASE_STOCK
    val details: String,
    val referenceId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // LOW_STOCK, OUT_OF_STOCK, SALE, PURCHASE, REFUND
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bundle_deals")
data class BundleDealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val originalPrice: Double,
    val bundlePrice: Double,
    val discountPercent: Double,
    val productSkus: String, // Comma-separated SKUs e.g. "COF-001,CRO-002"
    val badge: String = "HOT DEAL",
    val pitchLine: String = "",
    val isActive: Boolean = true
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val bundleDeals: List<BundleDealEntity> = emptyList(),
    val discountedProducts: List<ProductEntity> = emptyList()
)


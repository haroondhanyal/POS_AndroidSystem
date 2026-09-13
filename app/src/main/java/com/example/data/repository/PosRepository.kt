package com.example.data.repository

import com.example.R
import com.example.data.dao.PosDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.BundleDealEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ProductEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.PurchaseItemEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class CartItem(
    val product: ProductEntity,
    val quantity: Int,
    val itemDiscountPercent: Double = 0.0
) {
    val unitPriceAfterDiscount: Double
        get() = product.sellingPrice * (1.0 - (itemDiscountPercent / 100.0))

    val lineSubtotal: Double
        get() = unitPriceAfterDiscount * quantity

    val lineTax: Double
        get() = lineSubtotal * (product.taxPercent / 100.0)

    val lineTotal: Double
        get() = lineSubtotal + lineTax
}

data class PurchaseCartItem(
    val product: ProductEntity,
    val quantity: Int,
    val unitCost: Double
) {
    val totalCost: Double
        get() = quantity * unitCost
}

class PosRepository(private val posDao: PosDao) {

    val allUsers: Flow<List<UserEntity>> = posDao.getAllUsersFlow()
    val allProducts: Flow<List<ProductEntity>> = posDao.getAllProductsFlow()
    val allSales: Flow<List<SaleEntity>> = posDao.getAllSalesFlow()
    val allSaleItems: Flow<List<SaleItemEntity>> = posDao.getAllSaleItemsFlow()
    val allPurchases: Flow<List<PurchaseEntity>> = posDao.getAllPurchasesFlow()
    val allStockMovements: Flow<List<StockMovementEntity>> = posDao.getAllStockMovementsFlow()
    val allCustomers: Flow<List<CustomerEntity>> = posDao.getAllCustomersFlow()
    val allSuppliers: Flow<List<SupplierEntity>> = posDao.getAllSuppliersFlow()
    val allAuditLogs: Flow<List<AuditLogEntity>> = posDao.getAllAuditLogsFlow()
    val allNotifications: Flow<List<NotificationEntity>> = posDao.getAllNotificationsFlow()
    val unreadNotificationsCount: Flow<Int> = posDao.getUnreadNotificationCountFlow()
    val allBundleDeals: Flow<List<BundleDealEntity>> = posDao.getAllBundleDealsFlow()

    suspend fun getActiveBundleDeals(): List<BundleDealEntity> = withContext(Dispatchers.IO) {
        val deals = posDao.getActiveBundleDeals()
        if (deals.isEmpty()) {
            val defaults = listOf(
                BundleDealEntity(
                    title = "Morning Coffee & Dark Chocolate Combo",
                    description = "Fresh artisanal roast coffee paired with gourmet dark almond chocolate bar.",
                    category = "Combos",
                    originalPrice = 19.49,
                    bundlePrice = 15.50,
                    discountPercent = 20.0,
                    productSkus = "8901234001,8901234004",
                    badge = "HOT COMBO",
                    pitchLine = "Save 20% by pairing single-origin artisanal roast coffee with rich dark almond chocolate."
                ),
                BundleDealEntity(
                    title = "Zen Wellness Pack (Matcha + Essential Oil)",
                    description = "Ceremonial grade Matcha green tea paired with soothing pure lavender essential oil.",
                    category = "Health & Wellness",
                    originalPrice = 30.49,
                    bundlePrice = 24.99,
                    discountPercent = 18.0,
                    productSkus = "8901234003,8901234007",
                    badge = "WELLNESS SPECIAL",
                    pitchLine = "Perfect gift or stress-relief combo with $5.50 instant promotional discount."
                ),
                BundleDealEntity(
                    title = "Tech & Commuter Audio Bundle",
                    description = "Active noise-cancelling wireless studio headphones with heavy organic canvas tote.",
                    category = "Electronics",
                    originalPrice = 111.99,
                    bundlePrice = 94.99,
                    discountPercent = 15.0,
                    productSkus = "8901234002,8901234005",
                    badge = "SAVE $17",
                    pitchLine = "Buy the premium studio headphones and get the organic canvas tote bag at discount."
                ),
                BundleDealEntity(
                    title = "Quick Protein & Energy Snack Combo",
                    description = "High protein authentic Greek yogurt paired with dark chocolate almond bar.",
                    category = "Snacks",
                    originalPrice = 7.29,
                    bundlePrice = 5.49,
                    discountPercent = 25.0,
                    productSkus = "8901234010,8901234004",
                    badge = "QUICK BITE",
                    pitchLine = "Great healthy snack pairing for quick on-the-go fuel with 25% discount."
                )
            )
            posDao.insertBundleDeals(defaults)
            defaults
        } else {
            deals
        }
    }

    suspend fun saveBundleDeal(deal: BundleDealEntity) = withContext(Dispatchers.IO) {
        if (deal.id == 0L) {
            posDao.insertBundleDeal(deal)
        } else {
            posDao.updateBundleDeal(deal)
        }
    }

    suspend fun deleteBundleDeal(deal: BundleDealEntity) = withContext(Dispatchers.IO) {
        posDao.deleteBundleDeal(deal)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val users = allUsers.first()
        if (users.isEmpty()) {
            // Seed Users
            val admin = UserEntity(
                username = "admin",
                pin = "1234",
                fullName = "Alex Vance (Admin/Owner)",
                role = UserRole.ADMIN.name
            )
            val manager = UserEntity(
                username = "manager",
                pin = "2222",
                fullName = "Morgan Lee (Store Manager)",
                role = UserRole.MANAGER.name
            )
            val cashier1 = UserEntity(
                username = "cashier1",
                pin = "1111",
                fullName = "Johnathan Rivera",
                role = UserRole.CASHIER.name
            )
            val cashier2 = UserEntity(
                username = "cashier2",
                pin = "3333",
                fullName = "Sarah Jenkins",
                role = UserRole.CASHIER.name
            )
            posDao.insertUser(admin)
            posDao.insertUser(manager)
            posDao.insertUser(cashier1)
            posDao.insertUser(cashier2)

            val now = System.currentTimeMillis()
            val oneDay = 24L * 60 * 60 * 1000

            // Seed Products with Expiry Dates & Batches
            val products = listOf(
                ProductEntity(
                    name = "Fresh Organic Whole Milk (1L)",
                    sku = "8901234009",
                    category = "Dairy & Fresh",
                    brand = "GreenFields",
                    costPrice = 1.90,
                    sellingPrice = 3.99,
                    taxPercent = 5.0,
                    currentStock = 8,
                    minStockLevel = 5,
                    description = "Pasteurized pasteurized organic whole dairy milk.",
                    expiryDate = now - (3 * oneDay), // EXPIRED 3 days ago
                    batchNumber = "MK-2026-X9"
                ),
                ProductEntity(
                    name = "Greek Yogurt Vanilla (150g)",
                    sku = "8901234010",
                    category = "Dairy & Fresh",
                    brand = "Olympus",
                    costPrice = 1.10,
                    sellingPrice = 2.79,
                    taxPercent = 5.0,
                    currentStock = 14,
                    minStockLevel = 6,
                    description = "High protein strained authentic Greek yogurt.",
                    expiryDate = now + (4 * oneDay), // EXPIRING in 4 days
                    batchNumber = "YG-0492"
                ),
                ProductEntity(
                    name = "Multivitamin Complex 90ct",
                    sku = "8901234011",
                    category = "Health & Wellness",
                    brand = "VitaHealth",
                    costPrice = 8.50,
                    sellingPrice = 19.99,
                    taxPercent = 8.0,
                    currentStock = 16,
                    minStockLevel = 5,
                    description = "Complete daily essential vitamins and minerals.",
                    expiryDate = now + (20 * oneDay), // EXPIRING in 20 days
                    batchNumber = "VIT-8802"
                ),
                ProductEntity(
                    name = "Artisanal Roast Coffee (12oz)",
                    sku = "8901234001",
                    category = "Beverages",
                    brand = "Roast & Co",
                    costPrice = 6.50,
                    sellingPrice = 14.99,
                    taxPercent = 8.0,
                    currentStock = 24,
                    minStockLevel = 5,
                    description = "Freshly roasted single origin arabica beans.",
                    imageRes = R.drawable.product_coffee,
                    expiryDate = now + (180 * oneDay),
                    batchNumber = "RC-2026-01"
                ),
                ProductEntity(
                    name = "Wireless Studio Headphones",
                    sku = "8901234002",
                    category = "Electronics",
                    brand = "SonicPro",
                    costPrice = 45.00,
                    sellingPrice = 89.99,
                    taxPercent = 8.0,
                    currentStock = 8,
                    minStockLevel = 3,
                    description = "Active noise cancelling bluetooth over-ear headset.",
                    imageRes = R.drawable.product_headset,
                    batchNumber = "SP-H700"
                ),
                ProductEntity(
                    name = "Organic Matcha Green Tea",
                    sku = "8901234003",
                    category = "Beverages",
                    brand = "ZenLeaf",
                    costPrice = 7.00,
                    sellingPrice = 16.50,
                    taxPercent = 8.0,
                    currentStock = 18,
                    minStockLevel = 5,
                    description = "Ceremonial grade pure stone-ground green tea.",
                    expiryDate = now + (270 * oneDay),
                    batchNumber = "ZL-9921"
                ),
                ProductEntity(
                    name = "Dark Chocolate Almond Bar (80g)",
                    sku = "8901234004",
                    category = "Snacks",
                    brand = "ChocoArt",
                    costPrice = 1.80,
                    sellingPrice = 4.50,
                    taxPercent = 5.0,
                    currentStock = 3, // Low stock alert
                    minStockLevel = 10,
                    description = "72% cacao with roasted California almonds.",
                    expiryDate = now + (60 * oneDay),
                    batchNumber = "CA-4410"
                ),
                ProductEntity(
                    name = "Heavy Canvas Tote Bag",
                    sku = "8901234005",
                    category = "Apparel",
                    brand = "EcoWear",
                    costPrice = 8.50,
                    sellingPrice = 22.00,
                    taxPercent = 8.0,
                    currentStock = 14,
                    minStockLevel = 5,
                    description = "100% organic cotton reusable heavy tote.",
                    batchNumber = "EW-TOTE"
                ),
                ProductEntity(
                    name = "Sourdough Country Loaf",
                    sku = "8901234006",
                    category = "Bakery",
                    brand = "OvenCraft",
                    costPrice = 2.20,
                    sellingPrice = 6.50,
                    taxPercent = 0.0,
                    currentStock = 0, // Out of stock & expired
                    minStockLevel = 5,
                    description = "Naturally leavened fermented sourdough bread.",
                    expiryDate = now - (1 * oneDay),
                    batchNumber = "OC-BAKE"
                ),
                ProductEntity(
                    name = "Lavender Pure Essential Oil",
                    sku = "8901234007",
                    category = "Personal Care",
                    brand = "Botanica",
                    costPrice = 5.20,
                    sellingPrice = 13.99,
                    taxPercent = 8.0,
                    currentStock = 12,
                    minStockLevel = 4,
                    description = "French lavender therapeutic grade essential oil.",
                    expiryDate = now + (365 * oneDay),
                    batchNumber = "BT-8201"
                ),
                ProductEntity(
                    name = "Insulated Stainless Bottle 750ml",
                    sku = "8901234008",
                    category = "Accessories",
                    brand = "HydraPeak",
                    costPrice = 9.00,
                    sellingPrice = 24.99,
                    taxPercent = 8.0,
                    currentStock = 16,
                    minStockLevel = 5,
                    description = "Double-wall vacuum insulated water flask.",
                    batchNumber = "HP-750"
                )
            )
            val insertedProductIds = products.map { posDao.insertProduct(it) }

            // Seed Customers
            val c1 = CustomerEntity(
                name = "Jane Miller",
                phone = "+1 (555) 234-5678",
                email = "jane.miller@example.com",
                address = "742 Evergreen Terrace, Springfield",
                totalPurchases = 4,
                totalSpent = 145.50
            )
            val c2 = CustomerEntity(
                name = "Marcus Sterling",
                phone = "+1 (555) 987-6543",
                email = "marcus.s@corptech.io",
                address = "1200 Market St, Suite 400",
                totalPurchases = 2,
                totalSpent = 179.98
            )
            val c1Id = posDao.insertCustomer(c1)
            val c2Id = posDao.insertCustomer(c2)

            // Seed Historical Sales across Today, Weekly, Monthly, and Yearly
            val sToday1Id = posDao.insertSale(
                SaleEntity(
                    invoiceNumber = "INV-2026-TODAY-01",
                    cashierId = 1L,
                    cashierName = "Alex Vance (Admin)",
                    customerId = c1Id,
                    customerName = "Jane Miller",
                    subtotal = 31.49,
                    taxTotal = 2.52,
                    grandTotal = 34.01,
                    amountReceived = 40.00,
                    changeGiven = 5.99,
                    paymentMethod = "CASH",
                    timestamp = now - (3 * 3600 * 1000) // 3 hours ago today
                )
            )
            posDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = sToday1Id,
                        productId = insertedProductIds[3], // Coffee
                        productName = "Artisanal Roast Coffee (12oz)",
                        productSku = "8901234001",
                        quantity = 1,
                        unitCostPrice = 6.50,
                        unitSellingPrice = 14.99,
                        taxPercent = 8.0,
                        itemTotal = 14.99
                    ),
                    SaleItemEntity(
                        saleId = sToday1Id,
                        productId = insertedProductIds[5], // Matcha
                        productName = "Organic Matcha Green Tea",
                        productSku = "8901234003",
                        quantity = 1,
                        unitCostPrice = 7.00,
                        unitSellingPrice = 16.50,
                        taxPercent = 8.0,
                        itemTotal = 16.50
                    )
                )
            )

            val sToday2Id = posDao.insertSale(
                SaleEntity(
                    invoiceNumber = "INV-2026-TODAY-02",
                    cashierId = 3L,
                    cashierName = "Johnathan Rivera",
                    customerId = null,
                    customerName = "Walk-in Customer",
                    subtotal = 89.99,
                    taxTotal = 7.20,
                    grandTotal = 97.19,
                    amountReceived = 97.19,
                    changeGiven = 0.0,
                    paymentMethod = "CARD",
                    timestamp = now - (1 * 3600 * 1000) // 1 hour ago today
                )
            )
            posDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = sToday2Id,
                        productId = insertedProductIds[4], // Headphones
                        productName = "Wireless Studio Headphones",
                        productSku = "8901234002",
                        quantity = 1,
                        unitCostPrice = 45.00,
                        unitSellingPrice = 89.99,
                        taxPercent = 8.0,
                        itemTotal = 89.99
                    )
                )
            )

            // Yesterday / This Week
            val sWeekId = posDao.insertSale(
                SaleEntity(
                    invoiceNumber = "INV-2026-WEEK-01",
                    cashierId = 1L,
                    cashierName = "Alex Vance (Admin)",
                    customerId = c2Id,
                    customerName = "Marcus Sterling",
                    subtotal = 49.98,
                    taxTotal = 4.00,
                    grandTotal = 53.98,
                    amountReceived = 53.98,
                    changeGiven = 0.0,
                    paymentMethod = "DIGITAL_WALLET",
                    timestamp = now - (2 * oneDay) // 2 days ago
                )
            )
            posDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = sWeekId,
                        productId = insertedProductIds[10], // Bottle
                        productName = "Insulated Stainless Bottle 750ml",
                        productSku = "8901234008",
                        quantity = 2,
                        unitCostPrice = 9.00,
                        unitSellingPrice = 24.99,
                        taxPercent = 8.0,
                        itemTotal = 49.98
                    )
                )
            )

            // Earlier This Month
            val sMonthId = posDao.insertSale(
                SaleEntity(
                    invoiceNumber = "INV-2026-MONTH-01",
                    cashierId = 4L,
                    cashierName = "Sarah Jenkins",
                    customerId = null,
                    customerName = "Walk-in Customer",
                    subtotal = 44.00,
                    taxTotal = 3.52,
                    grandTotal = 47.52,
                    amountReceived = 50.00,
                    changeGiven = 2.48,
                    paymentMethod = "CASH",
                    timestamp = now - (10 * oneDay) // 10 days ago
                )
            )
            posDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = sMonthId,
                        productId = insertedProductIds[7], // Tote
                        productName = "Heavy Canvas Tote Bag",
                        productSku = "8901234005",
                        quantity = 2,
                        unitCostPrice = 8.50,
                        unitSellingPrice = 22.00,
                        taxPercent = 8.0,
                        itemTotal = 44.00
                    )
                )
            )

            // Earlier This Year
            val sYearId = posDao.insertSale(
                SaleEntity(
                    invoiceNumber = "INV-2026-YEAR-01",
                    cashierId = 1L,
                    cashierName = "Alex Vance (Admin)",
                    customerId = c1Id,
                    customerName = "Jane Miller",
                    subtotal = 65.96,
                    taxTotal = 5.28,
                    grandTotal = 71.24,
                    amountReceived = 71.24,
                    changeGiven = 0.0,
                    paymentMethod = "CARD",
                    timestamp = now - (45 * oneDay) // 45 days ago
                )
            )
            posDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = sYearId,
                        productId = insertedProductIds[3], // Coffee
                        productName = "Artisanal Roast Coffee (12oz)",
                        productSku = "8901234001",
                        quantity = 4,
                        unitCostPrice = 6.50,
                        unitSellingPrice = 14.99,
                        discountPercent = 10.0,
                        taxPercent = 8.0,
                        itemTotal = 53.96
                    ),
                    SaleItemEntity(
                        saleId = sYearId,
                        productId = insertedProductIds[9], // Lavender oil
                        productName = "Lavender Pure Essential Oil",
                        productSku = "8901234007",
                        quantity = 1,
                        unitCostPrice = 5.20,
                        unitSellingPrice = 13.99,
                        taxPercent = 8.0,
                        itemTotal = 13.99
                    )
                )
            )

            // Seed Suppliers
            val s1 = SupplierEntity(
                name = "Prime Wholesale Ltd",
                contactPerson = "David Vance",
                phone = "+1 (800) 555-0199",
                email = "orders@primewholesale.com",
                address = "Industrial Park 4, Logistics Blvd",
                productsSupplied = "Beverages, Snacks, Bakery"
            )
            val s2 = SupplierEntity(
                name = "Apex Distribution Hub",
                contactPerson = "Elena Rostova",
                phone = "+1 (800) 555-0188",
                email = "sales@apexdist.com",
                address = "Harbor Warehouses, Dock 12",
                productsSupplied = "Electronics, Accessories"
            )
            posDao.insertSupplier(s1)
            posDao.insertSupplier(s2)

            // Seed Notifications (including Expiry Alert!)
            posDao.insertNotification(
                NotificationEntity(
                    title = "Product Expired Alert",
                    message = "Fresh Organic Whole Milk (1L) expired on batch MK-2026-X9. Please write off and remove from shelf.",
                    type = "EXPIRED"
                )
            )
            posDao.insertNotification(
                NotificationEntity(
                    title = "Expiring Soon Alert",
                    message = "Greek Yogurt Vanilla (150g) expires in 4 days. Consider running a flash discount.",
                    type = "EXPIRING_SOON"
                )
            )
            posDao.insertNotification(
                NotificationEntity(
                    title = "Low Stock Alert",
                    message = "Dark Chocolate Almond Bar has only 3 units remaining (min: 10).",
                    type = "LOW_STOCK"
                )
            )

            // Initial Audit Log
            posDao.insertAuditLog(
                AuditLogEntity(
                    userName = "Alex Vance (Admin)",
                    userRole = "ADMIN",
                    action = "SYSTEM_INITIALIZED",
                    details = "Default inventory, demo cashier accounts, catalog, and expiry tracking loaded."
                )
            )
        }
    }

    // --- Authentication & User Operations ---
    suspend fun login(username: String, pin: String): UserEntity? = withContext(Dispatchers.IO) {
        val user = posDao.getUserByUsername(username)
        if (user != null && user.pin == pin && user.isActive) {
            posDao.insertAuditLog(
                AuditLogEntity(
                    userName = user.fullName,
                    userRole = user.role,
                    action = "LOGIN",
                    details = "User logged in successfully."
                )
            )
            user
        } else {
            null
        }
    }

    suspend fun logout(user: UserEntity) = withContext(Dispatchers.IO) {
        posDao.insertAuditLog(
            AuditLogEntity(
                userName = user.fullName,
                userRole = user.role,
                action = "LOGOUT",
                details = "User logged out."
            )
        )
    }

    suspend fun saveUser(user: UserEntity, currentAdmin: UserEntity) = withContext(Dispatchers.IO) {
        if (user.id == 0L) {
            val id = posDao.insertUser(user)
            posDao.insertAuditLog(
                AuditLogEntity(
                    userName = currentAdmin.fullName,
                    userRole = currentAdmin.role,
                    action = "CREATE_USER",
                    details = "Created account for ${user.fullName} (${user.role})"
                )
            )
            id
        } else {
            posDao.updateUser(user)
            posDao.insertAuditLog(
                AuditLogEntity(
                    userName = currentAdmin.fullName,
                    userRole = currentAdmin.role,
                    action = "UPDATE_USER",
                    details = "Updated user ${user.fullName}, active=${user.isActive}"
                )
            )
        }
    }

    suspend fun deleteUser(user: UserEntity, currentAdmin: UserEntity) = withContext(Dispatchers.IO) {
        posDao.deleteUser(user)
        posDao.insertAuditLog(
            AuditLogEntity(
                userName = currentAdmin.fullName,
                userRole = currentAdmin.role,
                action = "DELETE_USER",
                details = "Deleted user ${user.fullName}"
            )
        )
    }

    // --- Product Operations ---
    suspend fun saveProduct(product: ProductEntity, currentUser: UserEntity) = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            val id = posDao.insertProduct(product)
            posDao.insertAuditLog(
                AuditLogEntity(
                    userName = currentUser.fullName,
                    userRole = currentUser.role,
                    action = "CREATE_PRODUCT",
                    details = "Added product: ${product.name}, price: $${product.sellingPrice}",
                    referenceId = product.sku
                )
            )
            id
        } else {
            posDao.updateProduct(product)
            posDao.insertAuditLog(
                AuditLogEntity(
                    userName = currentUser.fullName,
                    userRole = currentUser.role,
                    action = "UPDATE_PRODUCT",
                    details = "Updated product: ${product.name}, stock: ${product.currentStock}",
                    referenceId = product.sku
                )
            )
        }
    }

    suspend fun deleteProduct(product: ProductEntity, currentUser: UserEntity) = withContext(Dispatchers.IO) {
        posDao.deleteProduct(product)
        posDao.insertAuditLog(
            AuditLogEntity(
                userName = currentUser.fullName,
                userRole = currentUser.role,
                action = "DELETE_PRODUCT",
                details = "Deleted product: ${product.name} (SKU: ${product.sku})"
            )
        )
    }

    // --- Stock Adjustments ---
    suspend fun adjustStock(
        product: ProductEntity,
        quantityChanged: Int,
        reason: String,
        currentUser: UserEntity
    ) = withContext(Dispatchers.IO) {
        val newStock = (product.currentStock + quantityChanged).coerceAtLeast(0)
        posDao.updateProductStock(product.id, newStock)

        val movementType = if (quantityChanged >= 0) "ADJUSTMENT_IN" else "ADJUSTMENT_OUT"
        posDao.insertStockMovement(
            StockMovementEntity(
                productId = product.id,
                productName = product.name,
                previousStock = product.currentStock,
                quantityChanged = quantityChanged,
                newStock = newStock,
                type = movementType,
                reason = reason,
                performedBy = currentUser.fullName
            )
        )

        posDao.insertAuditLog(
            AuditLogEntity(
                userName = currentUser.fullName,
                userRole = currentUser.role,
                action = "STOCK_ADJUSTMENT",
                details = "${product.name}: stock adjusted by $quantityChanged ($reason). New stock: $newStock",
                referenceId = product.sku
            )
        )

        if (newStock == 0) {
            posDao.insertNotification(
                NotificationEntity(
                    title = "Out of Stock: ${product.name}",
                    message = "Stock reached 0 after adjustment.",
                    type = "OUT_OF_STOCK"
                )
            )
        } else if (newStock <= product.minStockLevel) {
            posDao.insertNotification(
                NotificationEntity(
                    title = "Low Stock: ${product.name}",
                    message = "Only $newStock remaining (min: ${product.minStockLevel}).",
                    type = "LOW_STOCK"
                )
            )
        }
    }

    // --- POS Sales Processing ---
    suspend fun processSale(
        cashier: UserEntity,
        customer: CustomerEntity?,
        cartItems: List<CartItem>,
        cartDiscountPercent: Double,
        paymentMethod: String,
        amountReceived: Double,
        splitCashAmount: Double = 0.0,
        splitDigitalAmount: Double = 0.0
    ): Pair<SaleEntity, List<SaleItemEntity>> = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        val randomSuffix = Random.nextInt(100, 999)
        val invoiceNumber = "INV-${dateFormat.format(Date())}-$randomSuffix"

        // Calculations
        var subtotal = 0.0
        var itemDiscountTotal = 0.0
        var taxTotal = 0.0

        cartItems.forEach { item ->
            val regularLinePrice = item.product.sellingPrice * item.quantity
            val itemDiscountAmount = regularLinePrice * (item.itemDiscountPercent / 100.0)
            val lineSubtotal = regularLinePrice - itemDiscountAmount
            val lineTax = lineSubtotal * (item.product.taxPercent / 100.0)

            subtotal += regularLinePrice
            itemDiscountTotal += itemDiscountAmount
            taxTotal += lineTax
        }

        val subtotalAfterItemDiscounts = subtotal - itemDiscountTotal
        val cartDiscountTotal = subtotalAfterItemDiscounts * (cartDiscountPercent / 100.0)
        val finalTaxableAmount = subtotalAfterItemDiscounts - cartDiscountTotal
        val finalTaxTotal = taxTotal * (1.0 - (cartDiscountPercent / 100.0))
        val grandTotal = (finalTaxableAmount + finalTaxTotal).coerceAtLeast(0.0)

        val changeGiven = (amountReceived - grandTotal).coerceAtLeast(0.0)

        val sale = SaleEntity(
            invoiceNumber = invoiceNumber,
            cashierId = cashier.id,
            cashierName = cashier.fullName,
            customerId = customer?.id,
            customerName = customer?.name ?: "Walk-in Customer",
            subtotal = subtotal,
            itemDiscountTotal = itemDiscountTotal,
            cartDiscountTotal = cartDiscountTotal,
            taxTotal = finalTaxTotal,
            grandTotal = grandTotal,
            amountReceived = amountReceived,
            changeGiven = changeGiven,
            paymentMethod = paymentMethod,
            splitCashAmount = splitCashAmount,
            splitDigitalAmount = splitDigitalAmount,
            status = "COMPLETED"
        )

        val saleId = posDao.insertSale(sale)
        val createdSale = sale.copy(id = saleId)

        // Create Sale Items & Deduct Stock
        val saleItems = cartItems.map { cartItem ->
            val itemRegular = cartItem.product.sellingPrice * cartItem.quantity
            val itemDisc = itemRegular * (cartItem.itemDiscountPercent / 100.0)
            val itemSub = itemRegular - itemDisc
            val itemTax = itemSub * (cartItem.product.taxPercent / 100.0)

            val saleItem = SaleItemEntity(
                saleId = saleId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                productSku = cartItem.product.sku,
                quantity = cartItem.quantity,
                unitCostPrice = cartItem.product.costPrice,
                unitSellingPrice = cartItem.product.sellingPrice,
                discountPercent = cartItem.itemDiscountPercent,
                taxPercent = cartItem.product.taxPercent,
                itemTotal = itemSub + itemTax
            )

            // Reduce stock
            val freshProduct = posDao.getProductById(cartItem.product.id) ?: cartItem.product
            val newStock = (freshProduct.currentStock - cartItem.quantity).coerceAtLeast(0)
            posDao.updateProductStock(freshProduct.id, newStock)

            // Audit movement
            posDao.insertStockMovement(
                StockMovementEntity(
                    productId = freshProduct.id,
                    productName = freshProduct.name,
                    previousStock = freshProduct.currentStock,
                    quantityChanged = -cartItem.quantity,
                    newStock = newStock,
                    type = "SALE",
                    reason = "Sale invoice: $invoiceNumber",
                    performedBy = cashier.fullName
                )
            )

            // Trigger stock alerts if low
            if (newStock == 0) {
                posDao.insertNotification(
                    NotificationEntity(
                        title = "Out of Stock: ${freshProduct.name}",
                        message = "Sold out after invoice $invoiceNumber.",
                        type = "OUT_OF_STOCK"
                    )
                )
            } else if (newStock <= freshProduct.minStockLevel) {
                posDao.insertNotification(
                    NotificationEntity(
                        title = "Low Stock: ${freshProduct.name}",
                        message = "Only $newStock remaining after sale.",
                        type = "LOW_STOCK"
                    )
                )
            }

            saleItem
        }

        posDao.insertSaleItems(saleItems)

        // Update customer spending
        if (customer != null) {
            posDao.updateCustomer(
                customer.copy(
                    totalPurchases = customer.totalPurchases + 1,
                    totalSpent = customer.totalSpent + grandTotal
                )
            )
        }

        // Audit Log
        posDao.insertAuditLog(
            AuditLogEntity(
                userName = cashier.fullName,
                userRole = cashier.role,
                action = "NEW_SALE",
                details = "Completed sale $invoiceNumber for $${String.format(Locale.US, "%.2f", grandTotal)} ($paymentMethod)",
                referenceId = invoiceNumber
            )
        )

        posDao.insertNotification(
            NotificationEntity(
                title = "Sale Completed",
                message = "Invoice $invoiceNumber processed for $${String.format(Locale.US, "%.2f", grandTotal)} by ${cashier.fullName}",
                type = "SALE"
            )
        )

        Pair(createdSale, saleItems)
    }

    // --- Refund / Return ---
    suspend fun processRefund(
        sale: SaleEntity,
        reason: String,
        currentUser: UserEntity
    ): Boolean = withContext(Dispatchers.IO) {
        if (sale.status == "REFUNDED") return@withContext false

        val saleItems = posDao.getSaleItemsForSale(sale.id)

        // Restock items
        saleItems.forEach { item ->
            val product = posDao.getProductById(item.productId)
            if (product != null) {
                val newStock = product.currentStock + item.quantity
                posDao.updateProductStock(product.id, newStock)

                posDao.insertStockMovement(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        previousStock = product.currentStock,
                        quantityChanged = item.quantity,
                        newStock = newStock,
                        type = "REFUND_RESTOCK",
                        reason = "Refund for ${sale.invoiceNumber}: $reason",
                        performedBy = currentUser.fullName
                    )
                )
            }
        }

        // Update sale status
        posDao.updateSale(
            sale.copy(
                status = "REFUNDED",
                refundReason = reason,
                refundedBy = currentUser.fullName,
                refundedAt = System.currentTimeMillis()
            )
        )

        // Log audit
        posDao.insertAuditLog(
            AuditLogEntity(
                userName = currentUser.fullName,
                userRole = currentUser.role,
                action = "REFUND_ISSUED",
                details = "Issued full refund for ${sale.invoiceNumber} ($${String.format(Locale.US, "%.2f", sale.grandTotal)}). Reason: $reason",
                referenceId = sale.invoiceNumber
            )
        )

        posDao.insertNotification(
            NotificationEntity(
                title = "Refund Issued: ${sale.invoiceNumber}",
                message = "Refund of $${String.format(Locale.US, "%.2f", sale.grandTotal)} authorized by ${currentUser.fullName}.",
                type = "REFUND"
            )
        )

        true
    }

    // --- Purchases / Restock from Supplier ---
    suspend fun processPurchase(
        supplier: SupplierEntity,
        items: List<PurchaseCartItem>,
        taxPercent: Double,
        discountPercent: Double,
        currentUser: UserEntity
    ): Long = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault())
        val purchaseNumber = "PO-${dateFormat.format(Date())}-${Random.nextInt(100, 999)}"

        val subtotal = items.sumOf { it.totalCost }
        val discountTotal = subtotal * (discountPercent / 100.0)
        val taxTotal = (subtotal - discountTotal) * (taxPercent / 100.0)
        val grandTotal = subtotal - discountTotal + taxTotal

        val purchase = PurchaseEntity(
            purchaseNumber = purchaseNumber,
            supplierId = supplier.id,
            supplierName = supplier.name,
            receivedByCashier = currentUser.fullName,
            subtotal = subtotal,
            taxTotal = taxTotal,
            discountTotal = discountTotal,
            grandTotal = grandTotal,
            status = "RECEIVED"
        )

        val purchaseId = posDao.insertPurchase(purchase)

        val purchaseItems = items.map { item ->
            PurchaseItemEntity(
                purchaseId = purchaseId,
                productId = item.product.id,
                productName = item.product.name,
                quantity = item.quantity,
                unitCost = item.unitCost,
                totalCost = item.totalCost
            )
        }
        posDao.insertPurchaseItems(purchaseItems)

        // Restock products & record movement
        items.forEach { item ->
            val product = posDao.getProductById(item.product.id) ?: item.product
            val newStock = product.currentStock + item.quantity
            posDao.updateProductStock(product.id, newStock)

            posDao.insertStockMovement(
                StockMovementEntity(
                    productId = product.id,
                    productName = product.name,
                    previousStock = product.currentStock,
                    quantityChanged = item.quantity,
                    newStock = newStock,
                    type = "PURCHASE",
                    reason = "Purchase order: $purchaseNumber",
                    performedBy = currentUser.fullName
                )
            )
        }

        // Update supplier balance / history
        posDao.updateSupplier(
            supplier.copy(
                totalPurchases = supplier.totalPurchases + grandTotal
            )
        )

        posDao.insertAuditLog(
            AuditLogEntity(
                userName = currentUser.fullName,
                userRole = currentUser.role,
                action = "PURCHASE_STOCK",
                details = "Stock purchased from ${supplier.name} ($purchaseNumber) for $${String.format(Locale.US, "%.2f", grandTotal)}",
                referenceId = purchaseNumber
            )
        )

        posDao.insertNotification(
            NotificationEntity(
                title = "Stock Received",
                message = "Received purchase order $purchaseNumber from ${supplier.name}.",
                type = "PURCHASE"
            )
        )

        purchaseId
    }

    // --- Customers & Suppliers ---
    suspend fun saveCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        if (customer.id == 0L) posDao.insertCustomer(customer) else posDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        posDao.deleteCustomer(customer)
    }

    suspend fun saveSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        if (supplier.id == 0L) posDao.insertSupplier(supplier) else posDao.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        posDao.deleteSupplier(supplier)
    }

    suspend fun writeOffExpiredStock(
        product: ProductEntity,
        quantity: Int,
        reason: String,
        currentUser: UserEntity
    ) = withContext(Dispatchers.IO) {
        val qtyToWriteOff = quantity.coerceAtMost(product.currentStock)
        val newStock = (product.currentStock - qtyToWriteOff).coerceAtLeast(0)
        posDao.updateProductStock(product.id, newStock)

        posDao.insertStockMovement(
            StockMovementEntity(
                productId = product.id,
                productName = product.name,
                previousStock = product.currentStock,
                quantityChanged = -qtyToWriteOff,
                newStock = newStock,
                type = "DAMAGE_EXPIRED",
                reason = "Expired stock write-off: $reason (Batch: ${product.batchNumber.ifBlank { "N/A" }})",
                performedBy = currentUser.fullName
            )
        )

        posDao.insertAuditLog(
            AuditLogEntity(
                userName = currentUser.fullName,
                userRole = currentUser.role,
                action = "STOCK_EXPIRED_WRITEOFF",
                details = "Wrote off $qtyToWriteOff units of expired product '${product.name}' (SKU: ${product.sku}, Batch: ${product.batchNumber}). Reason: $reason"
            )
        )

        posDao.insertNotification(
            NotificationEntity(
                title = "Expired Stock Written Off",
                message = "$qtyToWriteOff units of '${product.name}' removed from inventory due to expiration.",
                type = "EXPIRED"
            )
        )
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        posDao.getSaleItemsForSale(saleId)
    }

    suspend fun markNotificationsRead() = withContext(Dispatchers.IO) {
        posDao.markAllNotificationsAsRead()
    }
}

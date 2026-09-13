package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAssistantManager
import com.example.data.model.AuditLogEntity
import com.example.data.model.BundleDealEntity
import com.example.data.model.ChatMessage
import com.example.data.model.CustomerEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ProductEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.UserEntity
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import com.example.data.repository.PurchaseCartItem
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    private val _themeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cartDiscountPercent = MutableStateFlow(0.0)
    val cartDiscountPercent: StateFlow<Double> = _cartDiscountPercent.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _lastCompletedReceipt = MutableStateFlow<Pair<SaleEntity, List<SaleItemEntity>>?>(null)
    val lastCompletedReceipt: StateFlow<Pair<SaleEntity, List<SaleItemEntity>>?> = _lastCompletedReceipt.asStateFlow()

    private val _inspectionReceipt = MutableStateFlow<Pair<SaleEntity, List<SaleItemEntity>>?>(null)
    val inspectionReceipt: StateFlow<Pair<SaleEntity, List<SaleItemEntity>>?> = _inspectionReceipt.asStateFlow()

    val activeReceipt: StateFlow<SaleEntity?> = kotlinx.coroutines.flow.combine(_lastCompletedReceipt, _inspectionReceipt) { last, insp ->
        last?.first ?: insp?.first
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeReceiptItems: StateFlow<List<SaleItemEntity>> = kotlinx.coroutines.flow.combine(_lastCompletedReceipt, _inspectionReceipt) { last, insp ->
        last?.second ?: insp?.second ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database reactive flows
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleItems: StateFlow<List<SaleItemEntity>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseEntity>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.allStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bundleDeals: StateFlow<List<BundleDealEntity>> = repository.allBundleDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Chatbot Assistant ---
    private val aiAssistant = AiAssistantManager()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "👋 Hello! Main hoon aapka **POS AI Assistant & Offers Finder** ✨\n\nAap mujhse kisi bhi product ki **bundle deal, active discount offers, ya clearance bachat** ke baare mein direct pooch sakte hain — manually search karne ki zaroorat nahi!\n\nNeechay diye gaye quick buttons tap karein ya apna sawal type karein:"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    fun sendChatMessage(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(isUser = true, text = query.trim())
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            val activeDeals = repository.getActiveBundleDeals()
            val allProds = products.value
            val response = aiAssistant.generateResponse(query, allProds, activeDeals)
            _chatMessages.value = _chatMessages.value + response
            _isAiThinking.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                isUser = false,
                text = "👋 Chat reset kar di gayi hai! Mujhse koi bhi new offer ya bundle deal poochein."
            )
        )
    }

    fun applyBundleDealToCart(deal: BundleDealEntity, onResult: (Boolean, String) -> Unit) {
        val skus = deal.productSkus.split(",").map { it.trim() }
        val allProds = products.value
        val matched = allProds.filter { it.sku in skus }
        if (matched.isEmpty()) {
            onResult(false, "Products for '${deal.title}' not found in catalog.")
            return
        }
        val outOfStock = matched.filter { it.currentStock <= 0 }
        if (outOfStock.isNotEmpty()) {
            onResult(false, "${outOfStock.first().name} is out of stock.")
            return
        }

        val currentList = _cartItems.value.toMutableList()
        for (prod in matched) {
            val existingIndex = currentList.indexOfFirst { it.product.id == prod.id }
            if (existingIndex >= 0) {
                val ex = currentList[existingIndex]
                if (ex.quantity < prod.currentStock) {
                    currentList[existingIndex] = ex.copy(
                        quantity = ex.quantity + 1,
                        itemDiscountPercent = deal.discountPercent
                    )
                }
            } else {
                currentList.add(
                    CartItem(
                        product = prod,
                        quantity = 1,
                        itemDiscountPercent = deal.discountPercent
                    )
                )
            }
        }
        _cartItems.value = currentList
        onResult(true, "Applied '${deal.title}' to cart with ${deal.discountPercent.toInt()}% discount!")
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // --- Authentication ---
    fun login(username: String, pin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.login(username.trim(), pin.trim())
            if (user != null) {
                _currentUser.value = user
                onResult(true, null)
            } else {
                onResult(false, "Invalid username or PIN code.")
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logout(user)
                _currentUser.value = null
                clearCart()
            }
        }
    }

    fun switchUserDirectly(user: UserEntity) {
        _currentUser.value = user
    }

    // --- POS Cart Operations ---
    fun addToCart(product: ProductEntity) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentList[index]
            if (existing.quantity < product.currentStock) {
                currentList[index] = existing.copy(quantity = existing.quantity + 1)
                _cartItems.value = currentList
            }
        } else {
            if (product.currentStock > 0) {
                currentList.add(CartItem(product = product, quantity = 1))
                _cartItems.value = currentList
            }
        }
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentList[index]
            if (newQuantity <= 0) {
                currentList.removeAt(index)
            } else {
                val clamped = newQuantity.coerceAtMost(item.product.currentStock)
                currentList[index] = item.copy(quantity = clamped)
            }
            _cartItems.value = currentList
        }
    }

    fun updateCartItemDiscount(productId: Long, discountPercent: Double) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(
                itemDiscountPercent = discountPercent.coerceIn(0.0, 100.0)
            )
            _cartItems.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
    }

    fun setCartDiscountPercent(percent: Double) {
        _cartDiscountPercent.value = percent.coerceIn(0.0, 100.0)
    }

    fun setSelectedCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _cartDiscountPercent.value = 0.0
        _selectedCustomer.value = null
    }

    // --- Checkout & Payment ---
    fun checkout(
        paymentMethod: String,
        amountReceived: Double,
        splitCashAmount: Double = 0.0,
        splitDigitalAmount: Double = 0.0,
        onSuccess: (SaleEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val cashier = _currentUser.value
        if (cashier == null) {
            onError("No active cashier session found.")
            return
        }
        val items = _cartItems.value
        if (items.isEmpty()) {
            onError("Cart is empty.")
            return
        }

        viewModelScope.launch {
            try {
                val (sale, saleItems) = repository.processSale(
                    cashier = cashier,
                    customer = _selectedCustomer.value,
                    cartItems = items,
                    cartDiscountPercent = _cartDiscountPercent.value,
                    paymentMethod = paymentMethod,
                    amountReceived = amountReceived,
                    splitCashAmount = splitCashAmount,
                    splitDigitalAmount = splitDigitalAmount
                )
                _lastCompletedReceipt.value = Pair(sale, saleItems)
                clearCart()
                onSuccess(sale)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to process sale.")
            }
        }
    }

    fun closeReceipt() {
        _lastCompletedReceipt.value = null
        _inspectionReceipt.value = null
    }

    fun clearReceipt() {
        closeReceipt()
    }

    fun inspectSaleReceipt(sale: SaleEntity) {
        viewModelScope.launch {
            val items = repository.getSaleItems(sale.id)
            _inspectionReceipt.value = Pair(sale, items)
        }
    }

    // --- Refund ---
    fun processRefund(
        sale: SaleEntity,
        reason: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || (user.role != "ADMIN" && user.role != "MANAGER")) {
            onResult(false, "Permission denied: Only Admin or Manager can issue refunds.")
            return
        }
        viewModelScope.launch {
            val success = repository.processRefund(sale, reason, user)
            if (success) {
                onResult(true, null)
            } else {
                onResult(false, "Sale has already been refunded.")
            }
        }
    }

    fun refundSale(
        sale: SaleEntity,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        processRefund(sale, reason) { success, error ->
            if (success) onSuccess() else onError(error ?: "Failed to refund sale.")
        }
    }

    // --- Inventory Stock Adjustment ---
    fun adjustStock(
        product: ProductEntity,
        quantityChanged: Int,
        reason: String,
        onDone: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.adjustStock(product, quantityChanged, reason, user)
            onDone()
        }
    }

    // --- Products ---
    fun saveProduct(product: ProductEntity, onDone: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveProduct(product, user)
            onDone()
        }
    }

    fun deleteProduct(product: ProductEntity, onDone: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteProduct(product, user)
            onDone()
        }
    }

    fun writeOffExpiredStock(
        product: ProductEntity,
        quantity: Int = product.currentStock,
        reason: String = "Expired stock disposal",
        onDone: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.writeOffExpiredStock(product, quantity, reason, user)
            onDone()
        }
    }

    // --- Purchases ---
    fun processPurchaseOrder(
        supplier: SupplierEntity,
        items: List<PurchaseCartItem>,
        taxPercent: Double,
        discountPercent: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        if (items.isEmpty()) {
            onError("Please add at least one product to the purchase order.")
            return
        }
        viewModelScope.launch {
            try {
                repository.processPurchase(supplier, items, taxPercent, discountPercent, user)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to record purchase order.")
            }
        }
    }

    // --- Users ---
    fun saveUser(user: UserEntity, onDone: () -> Unit = {}) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveUser(user, admin)
            onDone()
        }
    }

    fun deleteUser(user: UserEntity, onDone: () -> Unit = {}) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteUser(user, admin)
            onDone()
        }
    }

    // --- Customers & Suppliers ---
    fun saveCustomer(customer: CustomerEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            onDone()
        }
    }

    fun deleteCustomer(customer: CustomerEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            onDone()
        }
    }

    fun saveSupplier(supplier: SupplierEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveSupplier(supplier)
            onDone()
        }
    }

    fun deleteSupplier(supplier: SupplierEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            onDone()
        }
    }

    fun markNotificationsAsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead()
        }
    }

    fun markAllNotificationsRead() {
        markNotificationsAsRead()
    }
}

class PosViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

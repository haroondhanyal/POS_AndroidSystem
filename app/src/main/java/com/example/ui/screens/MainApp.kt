package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DrawerThemeSelectorSection
import com.example.ui.components.RoleBadge
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.components.getThemeIcon
import com.example.ui.theme.PosError
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSuccess
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

sealed class ScreenTab(val title: String, val icon: ImageVector) {
    object Dashboard : ScreenTab("Dashboard", Icons.Default.Dashboard)
    object Pos : ScreenTab("Register", Icons.Default.PointOfSale)
    object Products : ScreenTab("Products", Icons.Default.ShoppingBag)
    object Inventory : ScreenTab("Inventory", Icons.Default.Inventory)
    object Sales : ScreenTab("Orders", Icons.Default.ReceiptLong)
    object Purchases : ScreenTab("Purchases", Icons.Default.LocalShipping)
    object Customers : ScreenTab("Customers", Icons.Default.People)
    object Reports : ScreenTab("Reports", Icons.Default.Assessment)
    object Staff : ScreenTab("Staff & Audit", Icons.Default.Security)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: PosViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeReceipt by viewModel.activeReceipt.collectAsState()
    val activeReceiptItems by viewModel.activeReceiptItems.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var currentScreen by remember { mutableStateOf(if (currentUser == null) "login" else "main") }
    var currentTab by remember { mutableStateOf<ScreenTab>(ScreenTab.Dashboard) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (currentUser == null || currentScreen == "login") {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                currentScreen = "main"
                currentTab = ScreenTab.Dashboard
            }
        )
        return
    }

    if (currentScreen == "payment") {
        PaymentScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "main" },
            onSaleCompleted = {
                currentScreen = "main"
            }
        )
    } else {
        // Main App with Navigation Drawer and Bottom Navigation
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    // Drawer Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Retail POS", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Store Terminal #104", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Current User Profile
                        Text("Active Cashier:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentUser?.fullName ?: "Staff", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            currentUser?.let { RoleBadge(it.role) }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Drawer Items
                    val allTabs = listOf(
                        ScreenTab.Dashboard,
                        ScreenTab.Pos,
                        ScreenTab.Products,
                        ScreenTab.Inventory,
                        ScreenTab.Sales,
                        ScreenTab.Purchases,
                        ScreenTab.Customers,
                        ScreenTab.Reports
                    ) + if (currentUser?.role?.uppercase() == "ADMIN") listOf(ScreenTab.Staff) else emptyList()

                    allTabs.forEach { tab ->
                        NavigationDrawerItem(
                            label = { Text(tab.title, fontWeight = FontWeight.SemiBold) },
                            selected = currentTab == tab,
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            onClick = {
                                currentTab = tab
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Theme selector directly inside drawer
                    DrawerThemeSelectorSection(
                        currentTheme = themeMode,
                        onSelectTheme = { viewModel.setThemeMode(it) }
                    )

                    Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Sign Out Drawer Button
                    NavigationDrawerItem(
                        label = { Text("Sign Out / Switch Cashier", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        selected = false,
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            viewModel.logout()
                            scope.launch { drawerState.close() }
                            currentScreen = "login"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        ) {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(currentTab.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(PosSuccess)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${currentUser?.fullName} (${currentUser?.role})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                                }
                            },
                            actions = {
                                // Theme selector quick action
                                IconButton(onClick = { showThemeDialog = true }) {
                                    Icon(
                                        imageVector = getThemeIcon(themeMode),
                                        contentDescription = "Switch UI Theme (${themeMode.title})"
                                    )
                                }

                                // Notifications button
                                IconButton(onClick = { showNotificationsDialog = true }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadNotificationsCount > 0) {
                                                Badge(containerColor = PosError) {
                                                    Text("$unreadNotificationsCount")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                    }
                                }

                                // User menu dropdown
                                Box {
                                    IconButton(onClick = { showUserMenu = true }) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                currentUser?.fullName?.take(1)?.uppercase() ?: "U",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showUserMenu,
                                        onDismissRequest = { showUserMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Logged in as ${currentUser?.fullName}") },
                                            onClick = {},
                                            enabled = false
                                        )
                                        Divider()
                                        DropdownMenuItem(
                                            text = { Text("Display Theme: ${themeMode.title}") },
                                            leadingIcon = { Icon(getThemeIcon(themeMode), contentDescription = null) },
                                            onClick = {
                                                showUserMenu = false
                                                showThemeDialog = true
                                            }
                                        )
                                        Divider()
                                        DropdownMenuItem(
                                            text = { Text("Switch / Log Out", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showUserMenu = false
                                                viewModel.logout()
                                                currentScreen = "login"
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                },
                bottomBar = {
                    Column {
                        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                        val primaryTabs = listOf(
                            ScreenTab.Dashboard,
                            ScreenTab.Pos,
                            ScreenTab.Products,
                            ScreenTab.Inventory,
                            ScreenTab.Sales
                        )

                        primaryTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (tab == ScreenTab.Pos && cartItems.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                    Text("${cartItems.sumOf { it.quantity }}")
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tab.title)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.title)
                                    }
                                },
                                label = { Text(tab.title, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        ScreenTab.Dashboard -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToPos = { currentTab = ScreenTab.Pos },
                            onNavigateToProducts = { currentTab = ScreenTab.Products },
                            onNavigateToPurchases = { currentTab = ScreenTab.Purchases },
                            onNavigateToSales = { currentTab = ScreenTab.Sales },
                            onNavigateToInventory = { currentTab = ScreenTab.Inventory },
                            onNavigateToCustomers = { currentTab = ScreenTab.Customers },
                            onNavigateToReports = { currentTab = ScreenTab.Reports },
                            onOpenNotifications = { showNotificationsDialog = true }
                        )
                        ScreenTab.Pos -> PosScreen(
                            viewModel = viewModel,
                            onProceedToPayment = { currentScreen = "payment" }
                        )
                        ScreenTab.Products -> ProductsScreen(viewModel = viewModel)
                        ScreenTab.Inventory -> InventoryScreen(viewModel = viewModel)
                        ScreenTab.Purchases -> PurchasesScreen(viewModel = viewModel)
                        ScreenTab.Sales -> SalesHistoryScreen(viewModel = viewModel)
                        ScreenTab.Customers -> CustomersSuppliersScreen(viewModel = viewModel)
                        ScreenTab.Reports -> ReportsScreen(viewModel = viewModel)
                        ScreenTab.Staff -> UsersAuditScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Digital Receipt Modal (when sale completes or receipt is viewed)
    activeReceipt?.let { sale ->
        ReceiptDialog(
            sale = sale,
            items = activeReceiptItems,
            onDismiss = { viewModel.clearReceipt() },
            onStartNewSale = {
                viewModel.clearReceipt()
                currentScreen = "main"
                currentTab = ScreenTab.Pos
            }
        )
    }

    // Notifications Dialog
    if (showNotificationsDialog) {
        NotificationsDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    // Theme Selector Dialog
    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = themeMode,
            onSelectTheme = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
}

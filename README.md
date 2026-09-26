# Retail POS (Point of Sale) System for Android 📱🛒

## React Native / Expo app

This repository also contains a separate React Native app at [`react-native-pos/`](react-native-pos/). The original native Android app remains in `app/`; the Expo version is an independent POS implementation with dashboard, register and checkout, AI deals, products, inventory, orders, purchases, customers and suppliers, reports, and staff/audit screens. It stores demo data locally on the device.

```bash
cd react-native-pos
npm install
npm start
```

Open the Expo Go app on your Android phone and scan the QR code shown by Expo. To start the development server on the local network, run `npx expo start --lan`. Demo sign-in: `admin` / `1234` (manager: `manager` / `2222`; cashiers: `cashier1` / `1111` and `cashier2` / `3333`).

The Expo app is implemented in TypeScript with Expo SDK 57. It can be launched independently and does not replace or modify the native Android source under `app/`.

The Expo version includes a Retail POS terminal icon and the `expo-splash-screen` native launch-screen configuration. Expo Go displays the app icon while loading; verify the configured native splash in an Android app build.

A modern, production-ready, native Android Point of Sale (POS) application built with **Jetpack Compose**, **Material Design 3**, and **Room Database**. Designed for retail stores, supermarkets, grocery shops, and pharmacies to streamline sales transactions, catalog control, inventory management, purchase orders, customer loyalty, and financial reporting.

---

## 🚀 Key Highlights & Features

### 🛒 1. Fast POS & Checkout Terminal
- **Instant Product Search & Barcode Scanning**: Search by name, SKU, brand, or scan live barcodes using the device camera (CameraX / MLKit).
- **Interactive Cart System**:
  - Quantity increment, decrement, and quick custom inputs.
  - Item-level and cart-wide discount options (percentage or fixed amount).
  - Configurable tax rate calculations.
  - Expired item safeguards: warns cashier and prevents accidental checkout of expired products.
- **Hold & Resume Carts**: Park ongoing transactions to serve another customer and retrieve them anytime without losing state.
- **Customer Tagging & Loyalty**: Attach customer profiles to transactions to track reward points and purchase history.

### 💳 2. Payment & Digital Receipts
- **Multiple Payment Methods**: Cash (with instant change calculation), Credit/Debit Card, Mobile Wallets (EasyPaisa, JazzCash, Apple Pay, Google Pay), and Store Credit.
- **Digital Receipt Engine**:
  - Clean printable layout with store branding, cashier details, timestamp, line items, taxes, discounts, and payment summary.
  - Share receipt via WhatsApp, Email, or Bluetooth thermal printers.
  - QR / Barcode for quick receipt lookups and returns.

### 📦 3. Product Catalog & Image Management
- **Complete Product Master**: SKU, barcode, category, brand, unit cost, retail price, current stock, minimum stock alert levels, and batch expiry dates.
- **Photo Capture & Gallery Selection**: Capture product images directly via camera or pick from the gallery with automated internal storage caching.
- **Expiry & Low Stock Tracking**: Color-coded badges for items running low or nearing/past expiration dates.

### 📊 4. Inventory & Stock Control
- **Real-Time Stock Tracking**: Automated stock updates on sales, purchases, and returns.
- **Stock Adjustments & Write-Offs**:
  - Quick adjustments for audits, damaged items, and wastage.
  - Dedicated **Expired Stock Disposal Workflow** with mandatory reason logging and audit timestamps.
- **Low Stock & Expiry Alerts**: Real-time notification dialog and badge counters alerting staff to take action before stock runs out.

### 🚚 5. Purchase Orders (Suppliers & Receiving)
- **Supplier Order Management**: Create purchase orders with line items, cost prices, quantities, and expected delivery dates.
- **Inventory Auto-Stocking**: Receiving a purchase order automatically increments inventory counts and records unit cost changes.
- **Payment Status Tracking**: Track orders by payment status (Paid, Partial, Due).

### 👥 6. CRM (Customers & Suppliers)
- **Customer Directory**: Track customer contact info, address, lifetime sales volume, and loyalty points.
- **Supplier Directory**: Manage vendor details, company names, contact numbers, and purchase order history.

### 📈 7. Analytics, Financials & Reports
- **Executive Dashboard**: Today's sales, gross revenue, net profit estimations, transaction count, low stock warnings, and active cashier info.
- **Sales Analytics**: Historical trends, category-wise revenue breakdowns, top-selling products, and payment mode distribution.
- **Export & Print**: Easily review sales summaries for daily closing (Z-reports).

### 🤖 8. AI Chatbot Assistant & Bundle Deals Finder ✨
- **Conversational Deals Discovery**: Cashiers and store managers can ask the AI in English, Urdu, or Roman Urdu (e.g., *"Koi new offer ya bundle deal chal rahi hai?"*, *"What deals are on coffee?"*, *"Show clearance deals"*).
- **Automated Promotional Bundles**: Built-in bundle deals (e.g., Morning Coffee & Dark Chocolate Combo, Zen Wellness Pack, Tech & Commuter Bundle, Protein Snack Combo) with discounted pricing and persuasive sales pitch suggestions.
- **1-Click Cart Addition**: Cashiers can tap **"Add Bundle to Cart"** directly on any AI recommendation card to instantly load the discounted bundle into the POS register.
- **Dual-Mode AI Engine**:
  - Direct Gemini 2.5 Flash cloud API integration via Generative Language API.
  - Smart Offline AI Engine that analyzes live Room database catalog, active promotions, and nearing-expiry inventory with zero latency and zero failure.

### 🔒 9. Security, Roles & Audit Logging
- **Role-Based Access Control (RBAC)**:
  - **Cashier**: Sales register, receipt reprint, hold carts.
  - **Manager**: Products, inventory adjustments, purchase orders, customer credits.
  - **Admin**: Full access including financial reports, user management, and system audit logs.
- **Immutable Audit Trail**: Every stock change, disposal, discount, and administrative action logs the actor, timestamp, and details for fraud prevention.

---

## 🔄 User Journeys & App Flows

### 1. Cashier Daily Checkout Flow
```text
Login with Cashier PIN / Role 
  └── Dashboard (Review today's target) 
      └── POS Register Screen
          ├── Scan Barcode / Search Product
          ├── Adjust Quantities / Apply Discounts
          ├── Select Customer (Optional Loyalty Points)
          ├── Tap "Proceed to Checkout"
          ├── Select Payment Method (Cash -> Enter tender -> View Change)
          └── Complete Sale -> Instant Digital Receipt (Print / Share)
```

### 2. Stock Expiry & Inventory Disposal Flow
```text
Open Inventory / Products Screen 
  └── Filter by "Expired" or "Low Stock"
      └── Select Expired Product 
          └── Click "Dispose / Write Off"
              ├── Enter write-off quantity & reason
              ├── Confirm Action 
              └── System deducts inventory and writes an entry to Audit Log
```

### 3. Purchasing & Supplier Receiving Flow
```text
Purchases Screen 
  └── Click "New Purchase Order" 
      ├── Select Supplier & PO Reference
      ├── Add Products with Purchase Cost & Quantities
      ├── Save as "Pending" or mark "Received"
      └── When "Received": Inventory automatically increments across the store
```

### 4. Admin Daily Audit & Closing Flow
```text
Admin Login 
  └── Dashboard -> Reports Screen
      ├── Review daily gross sales, profit margins, and payment distributions
      ├── Check Cashier Performance & Transaction Count
      └── Users & Audit Screen -> Inspect any manual stock overrides or discounts
```

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
- **Design System**: Material Design 3 (M3) with dynamic color scheme & custom dark/light theme support
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern + Clean Architecture principles
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite ORM with Coroutines & Kotlin Flow)
- **Reactive State**: Kotlin StateFlow & SharedFlow
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (Asynchronous image loading & disk caching)
- **Barcode & Camera**: Android CameraX / MLKit integration for zero-latency camera scanning
- **Dependency Management**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)

---

## 📂 Project Structure

```text
app/src/main/java/com/example/
├── data/
│   ├── local/            # Room Database, DAOs, TypeConverters
│   ├── model/            # Data Entities (Products, Sales, Orders, Customers, Users, Audit)
│   └── repository/       # POS Repository implementation & Business Logic
├── ui/
│   ├── components/       # Reusable Compose widgets (Badges, Buttons, Cards, Dialogs)
│   ├── screens/          # Main application screens:
│   │   ├── LoginScreen.kt
│   │   ├── DashboardScreen.kt
│   │   ├── PosScreen.kt
│   │   ├── PaymentScreen.kt
│   │   ├── ReceiptDialog.kt
│   │   ├── ProductsScreen.kt
│   │   ├── InventoryScreen.kt
│   │   ├── PurchasesScreen.kt
│   │   ├── CustomersSuppliersScreen.kt
│   │   ├── SalesHistoryScreen.kt
│   │   ├── ReportsScreen.kt
│   │   ├── UsersAuditScreen.kt
│   │   └── NotificationsDialog.kt
│   ├── theme/            # Material 3 Color Schemes, Typography, Shape Definitions
│   ├── util/             # Image capture helpers, currency formatters, date utilities
│   └── viewmodel/        # PosViewModel managing reactive UI state & operations
└── MainActivity.kt       # Application entry point with Edge-to-Edge display
```

---

## ⚙️ Setup & Build Instructions

### Prerequisites
- **Android Studio**: Ladybug / Hedgehog or newer
- **JDK**: Java 17+
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)

### Running Locally
1. Clone this repository:
   ```bash
   git clone https://github.com/haroondhanyal/POS_AndroidSystem.git
   ```
2. Open the project folder in **Android Studio**.
3. Let Gradle sync dependencies automatically.
4. Select an Android Emulator or physical device (with USB Debugging enabled).
5. Click **Run** (`Shift + F10`) to build and launch the application.

---

## 📄 License
This project is open-source and available under the **MIT License**.

# Screen ownership map

`App.tsx` is currently the application composition root: it owns session state, permissions, checkout orchestration, persistence and the shared sheet/modal layer. Keep new work screen-oriented and move each independently changing screen into its own file under this directory.

| Screen | Main responsibility | Suggested owner |
| --- | --- | --- |
| Dashboard | KPIs, alerts and quick actions | `DashboardScreen.tsx` |
| Register | Search/scan, cart and payment hand-off | `RegisterScreen.tsx` |
| Products & Inventory | Catalog editing and stock/expiry | `ProductsScreen.tsx`, `InventoryScreen.tsx` |
| Orders & Purchases | Receipt history, refunds and receiving | `OrdersScreen.tsx`, `PurchasesScreen.tsx` |
| Customers & Staff | Profiles, roles and activity | `CustomersScreen.tsx`, `StaffScreen.tsx` |
| Reports & Admin | Period reports, presence and permissions | `ReportsScreen.tsx`, `AdminScreen.tsx` |
| Team Chat | Direct/group messages and attachments | `TeamChatScreen.tsx` |
| Authentication & Profile | Login, signup, security settings | `AuthScreen.tsx`, `ProfileScreen.tsx` |

## Screen contract

Screens should receive typed data and callbacks through props. Keep navigation, shared persistence and API coordination in the application layer; keep screen-specific display state inside the screen. Extract repeated buttons, fields, rows and cards into `src/components/`. Put backend calls in `src/services/` and pure calculations in `src/domain/`.

Before starting a screen task, agree on its prop type and the modules it owns. Avoid editing another screen's JSX or changing the `Store` model without coordinating with its owner. Update this map when a screen is extracted or its ownership changes.

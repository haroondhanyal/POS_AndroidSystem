# Screen ownership and four developer workstreams

Screens are standalone React components with typed props. The app root owns persistent store state, navigation, permissions, API sync, business mutations and shared modal workflows; screen modules own presentation and screen-local state. Prefer a callback in the screen props when an action must change shared data.

## Current screen modules

| Screen file | Responsibility |
| --- | --- |
| `DashboardScreen.tsx` | Store KPIs, quick actions, recent sales and stock alerts |
| `RegisterScreen.tsx` | Customer selection, barcode/search entry, product selection and cart entry point |
| `ProductsScreen.tsx` | Product catalog search, edit/delete and add-to-cart actions |
| `InventoryScreen.tsx` | Stock totals, low/out-of-stock, expiry filters and stock adjustments |
| `OrdersScreen.tsx` | Receipt history and refund hand-off |
| `PurchasesScreen.tsx` | Purchase orders and receiving stock |
| `CustomersScreen.tsx` | Customer/supplier directory and profile details |
| `AIDealsScreen.tsx` | Offer suggestions and bundle actions |
| `ReportsScreen.tsx` | Daily/weekly/monthly totals and payment/staff breakdowns |
| `AdminScreen.tsx` | Currency/rate controls, sales summaries and online presence |
| `StaffScreen.tsx` | Staff directory, reset requests, presence and audit history |
| `ProfileScreen.tsx` | Signed-in profile and credential settings entry |
| `TeamChatScreen.tsx` | Direct/group thread, attachments and voice-message UI |
| `AuthScreen.tsx` | Sign-in, signup, password/PIN mode and signup form |

These components receive typed props and do not import the app root. Shared visual primitives are temporarily passed from `App.tsx`; when the component library is extracted, move those to `src/components/` and update props without changing screen behavior.

## Suggested ownership for four developers

| Developer | Workstream | Files to own / extract next |
| --- | --- | --- |
| A | Sales and checkout | `RegisterScreen.tsx`, `OrdersScreen.tsx`; next extract receipt/cart checkout sheet to `CheckoutSheet.tsx` |
| B | Catalog and supply | `ProductsScreen.tsx`, `InventoryScreen.tsx`, `PurchasesScreen.tsx` |
| C | People and communication | `AuthScreen.tsx`, `StaffScreen.tsx`, `ProfileScreen.tsx`, `TeamChatScreen.tsx` |
| D | Store overview and controls | `DashboardScreen.tsx`, `CustomersScreen.tsx`, `ReportsScreen.tsx`, `AdminScreen.tsx`, `AIDealsScreen.tsx` |

`App.tsx` is shared integration code. Keep each developer's changes in their screen files. Coordinate edits to the root, domain model and shared styles through a single integration owner; merge one screen at a time by matching its props contract.

## Screen contract

- Pass display data and user actions as explicit props. Keep store/API access out of screen modules.
- Put reusable fields, buttons, rows and cards in `src/components/`.
- Put backend calls and device file operations in `src/services/`; pure calculations go in `src/domain/`.
- Do not mutate props. Call the supplied action when the user changes durable data.
- Add comments where a workflow crosses boundaries (checkout, offline sync, refunds or media upload). Explain the sequence/invariant, not obvious JSX.
- Update this map when a screen is extracted or its owner changes.

## Working agreement

Before starting a screen task, agree on the prop type and files that person owns. Avoid simultaneous changes to `App.tsx` or `src/model.ts`; ask the integration owner to add a shared field/action when needed. Add new screen-specific styles beside the screen until a shared design-token module exists.

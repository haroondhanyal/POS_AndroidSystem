# React Native POS

Expo mobile companion app for the POS demo. Run it from this directory with:

```sh
npm install
npx expo start
```

Scan the QR code with Expo Go while the phone and development computer share a Wi-Fi network.

## Included workflows

- POS checkout with barcode camera scanning against product SKUs, stock adjustments, and local sales history.
- Product and staff profile photos selected from the device photo library.
- Shareable PDF receipts and digital receipt sharing.
- Username/email and role-based staff sign-in, password visibility, and admin-approved password reset.
- Admin staff directory, role management, password reset requests, and online/offline session-time tracking.
- Daily, weekly, and monthly sales summaries.
- Local persistence and offline queue. Set `EXPO_PUBLIC_POS_API_URL` to the base URL of a compatible server implementing `POST /pos/sync` to send queued sale and stock events when online. No server is included in this repository; without that service the data stays on this device.

## Demo sign-in

- Admin: `admin` / `1234` / role `ADMIN`
- Sales manager: `manager` / `2222` / role `SALES_MANAGER`
- Sales person: `cashier1` / `1111` / role `SALES_PERSON`
- Cashier: `cashier2` / `3333` / role `CASHIER`

These sample accounts are stored locally for the demo. Create real staff accounts from **Staff & Audit** after signing in as the admin.

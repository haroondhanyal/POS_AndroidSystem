export type Role = 'ADMIN' | 'MANAGER' | 'SALES_MANAGER' | 'SALES_PERSON' | 'INVENTORY_MANAGER' | 'CASHIER' | 'STAFF';
export type Product = {
  id: string; name: string; sku: string; category: string; brand: string;
  cost: number; price: number; tax: number; stock: number; minimum: number;
  expiry?: string; batch: string; discount?: number; imageUri?: string;
};
export type CartLine = { productId: string; quantity: number; discount: number };
export type SaleLine = { name: string; sku: string; quantity: number; price: number };
export type Sale = {
  id: string; invoice: string; timestamp: string; customer: string; cashier: string;
  subtotal: number; discount: number; tax: number; total: number; paid: number;
  change: number; method: string; status: 'COMPLETED' | 'REFUNDED'; items: SaleLine[];
  evidence?: { recordedBy: string; recordedAt: string; paymentReference?: string; photoUri?: string; stockAfter: Record<string, number>; customerSnapshot?: Pick<Person, 'name' | 'phone' | 'email' | 'address' | 'photoUri'> };
};
export type Purchase = { id: string; number: string; supplier: string; timestamp: string; total: number; status: 'RECEIVED' | 'PENDING'; items: { productId: string; quantity: number; cost: number }[] };
export type Person = {
  id: string; name: string; phone: string; email: string; address: string; totalSpent: number;
  role?: Role; username?: string; pin?: string; passwordHash?: string; passwordSalt?: string; pinHash?: string; pinSalt?: string;
  active?: boolean; photoUri?: string; countryCode?: string; dateOfBirth?: string; recoveryRequestedAt?: string;
};
export type Movement = { id: string; product: string; delta: number; reason: string; by: string; timestamp: string };
export type Audit = { id: string; action: string; details: string; by: string; timestamp: string };
export type Notice = { id: string; title: string; body: string; read: boolean; timestamp: string };
export type PresenceSession = { id: string; userId: string; name: string; role: Role; startedAt: string; endedAt?: string; lastTickAt: string; onlineSeconds: number; offlineSeconds: number; lastSeenAt: string };
export type SyncEvent = { id: string; type: string; createdAt: string; payload: unknown };
export type PasswordResetRequest = { id: string; userId: string; username: string; email: string; createdAt: string; completedAt?: string };
export type Store = {
  products: Product[]; sales: Sale[]; purchases: Purchase[]; customers: Person[]; suppliers: Person[]; users: Person[];
  movements: Movement[]; audits: Audit[]; notices: Notice[]; presenceSessions: PresenceSession[];
  syncQueue: SyncEvent[]; passwordResetRequests: PasswordResetRequest[];
};

export const categories = ['All', 'Beverages', 'Snacks', 'Dairy & Fresh', 'Electronics', 'Health & Wellness', 'Bakery', 'Apparel', 'Personal Care', 'Accessories'];
export const deals = [
  { id: 'D1', title: 'Morning Coffee & Dark Chocolate Combo', category: 'Beverages', skus: ['8901234001', '8901234004'], original: 19.49, price: 16.49, discount: 15, pitch: 'The perfect morning pick-me-up with a sweet finish.' },
  { id: 'D2', title: 'Zen Wellness Pack', category: 'Health & Wellness', skus: ['8901234011', '8901234003'], original: 36.49, price: 29.99, discount: 18, pitch: 'A thoughtful daily wellness routine, bundled for less.' },
  { id: 'D3', title: 'Tech & Commuter Bundle', category: 'Electronics', skus: ['8901234002', '8901234008'], original: 114.98, price: 99.99, discount: 13, pitch: 'Great sound and all-day hydration for the commute.' },
  { id: 'D4', title: 'Protein Snack Combo', category: 'Snacks', skus: ['8901234004', '8901234010'], original: 7.29, price: 5.99, discount: 18, pitch: 'A satisfying little snack with a protein boost.' },
];

const day = 86_400_000;
const now = Date.now();
export const initialStore: Store = {
  products: [
    { id: 'p1', name: 'Fresh Organic Whole Milk (1L)', sku: '8901234009', category: 'Dairy & Fresh', brand: 'GreenFields', cost: 1.9, price: 3.99, tax: 5, stock: 8, minimum: 5, expiry: new Date(now - 3 * day).toISOString().slice(0, 10), batch: 'MK-2026-X9' },
    { id: 'p2', name: 'Greek Yogurt Vanilla (150g)', sku: '8901234010', category: 'Dairy & Fresh', brand: 'Olympus', cost: 1.1, price: 2.79, tax: 5, stock: 14, minimum: 6, expiry: new Date(now + 4 * day).toISOString().slice(0, 10), batch: 'YG-0492' },
    { id: 'p3', name: 'Multivitamin Complex 90ct', sku: '8901234011', category: 'Health & Wellness', brand: 'VitaHealth', cost: 8.5, price: 19.99, tax: 8, stock: 16, minimum: 5, expiry: new Date(now + 20 * day).toISOString().slice(0, 10), batch: 'VIT-8802' },
    { id: 'p4', name: 'Artisanal Roast Coffee (12oz)', sku: '8901234001', category: 'Beverages', brand: 'Roast & Co', cost: 6.5, price: 14.99, tax: 8, stock: 24, minimum: 5, expiry: new Date(now + 180 * day).toISOString().slice(0, 10), batch: 'RC-2026-01' },
    { id: 'p5', name: 'Wireless Studio Headphones', sku: '8901234002', category: 'Electronics', brand: 'SonicPro', cost: 45, price: 89.99, tax: 8, stock: 8, minimum: 3, batch: 'SP-H700' },
    { id: 'p6', name: 'Organic Matcha Green Tea', sku: '8901234003', category: 'Beverages', brand: 'ZenLeaf', cost: 7, price: 16.5, tax: 8, stock: 18, minimum: 5, expiry: new Date(now + 270 * day).toISOString().slice(0, 10), batch: 'ZL-9921' },
    { id: 'p7', name: 'Dark Chocolate Almond Bar (80g)', sku: '8901234004', category: 'Snacks', brand: 'ChocoArt', cost: 1.8, price: 4.5, tax: 5, stock: 3, minimum: 10, expiry: new Date(now + 60 * day).toISOString().slice(0, 10), batch: 'CA-4410' },
    { id: 'p8', name: 'Heavy Canvas Tote Bag', sku: '8901234005', category: 'Apparel', brand: 'EcoWear', cost: 8.5, price: 22, tax: 8, stock: 14, minimum: 5, batch: 'EW-TOTE' },
    { id: 'p9', name: 'Sourdough Country Loaf', sku: '8901234006', category: 'Bakery', brand: 'OvenCraft', cost: 2.2, price: 6.5, tax: 0, stock: 0, minimum: 5, expiry: new Date(now - day).toISOString().slice(0, 10), batch: 'OC-BAKE' },
    { id: 'p10', name: 'Lavender Pure Essential Oil', sku: '8901234007', category: 'Personal Care', brand: 'Botanica', cost: 5.2, price: 13.99, tax: 8, stock: 12, minimum: 4, expiry: new Date(now + 365 * day).toISOString().slice(0, 10), batch: 'BT-8201' },
    { id: 'p11', name: 'Insulated Stainless Bottle 750ml', sku: '8901234008', category: 'Accessories', brand: 'HydraPeak', cost: 9, price: 24.99, tax: 8, stock: 16, minimum: 5, batch: 'HP-750' },
  ],
  sales: [
    { id: 's1', invoice: 'POS-10482', timestamp: new Date(now - 40 * 60_000).toISOString(), customer: 'Walk-in Customer', cashier: 'Alex Vance', subtotal: 34.48, discount: 0, tax: 2.76, total: 37.24, paid: 40, change: 2.76, method: 'CASH', status: 'COMPLETED', items: [{ name: 'Artisanal Roast Coffee (12oz)', sku: '8901234001', quantity: 1, price: 14.99 }, { name: 'Organic Matcha Green Tea', sku: '8901234003', quantity: 1, price: 16.5 }, { name: 'Dark Chocolate Almond Bar (80g)', sku: '8901234004', quantity: 1, price: 4.5 }] },
    { id: 's2', invoice: 'POS-10481', timestamp: new Date(now - 2 * 60 * 60_000).toISOString(), customer: 'Olivia Chen', cashier: 'Johnathan Rivera', subtotal: 89.99, discount: 0, tax: 7.2, total: 97.19, paid: 97.19, change: 0, method: 'CARD', status: 'COMPLETED', items: [{ name: 'Wireless Studio Headphones', sku: '8901234002', quantity: 1, price: 89.99 }] },
    { id: 's3', invoice: 'POS-10480', timestamp: new Date(now - 5 * 60 * 60_000).toISOString(), customer: 'Walk-in Customer', cashier: 'Alex Vance', subtotal: 27.98, discount: 0, tax: 2.24, total: 30.22, paid: 35, change: 4.78, method: 'CASH', status: 'COMPLETED', items: [{ name: 'Multivitamin Complex 90ct', sku: '8901234011', quantity: 1, price: 19.99 }, { name: 'Organic Matcha Green Tea', sku: '8901234003', quantity: 1, price: 16.5 }] },
  ],
  purchases: [
    { id: 'po1', number: 'PO-00241', supplier: 'GreenFields Foods', timestamp: new Date(now - 2 * day).toISOString(), total: 412.6, status: 'RECEIVED', items: [] },
    { id: 'po2', number: 'PO-00242', supplier: 'Sonic Distribution', timestamp: new Date(now - day).toISOString(), total: 735.92, status: 'PENDING', items: [] },
  ],
  customers: [
    { id: 'c1', name: 'Olivia Chen', phone: '+1 555 010 2841', email: 'olivia@example.com', address: 'Downtown', totalSpent: 1248.5 },
    { id: 'c2', name: 'Noah Patel', phone: '+1 555 010 1619', email: 'noah@example.com', address: 'River District', totalSpent: 826.25 },
    { id: 'c3', name: 'Amara Wilson', phone: '+1 555 010 7704', email: 'amara@example.com', address: 'West End', totalSpent: 549.8 },
  ],
  suppliers: [
    { id: 'v1', name: 'GreenFields Foods', phone: '+1 555 014 8200', email: 'orders@greenfields.example', address: 'North Market', totalSpent: 5280 },
    { id: 'v2', name: 'Sonic Distribution', phone: '+1 555 014 3600', email: 'sales@sonic.example', address: 'Industrial Park', totalSpent: 9135 },
    { id: 'v3', name: 'VitaHealth Wholesale', phone: '+1 555 014 2600', email: 'hello@vitahealth.example', address: 'East Business Center', totalSpent: 3240 },
  ],
  users: [
    { id: 'u1', name: 'Alex Vance', phone: 'admin', email: 'admin', address: 'Store owner', role: 'ADMIN', username: 'admin', pin: '1234', active: true, totalSpent: 0 },
    { id: 'u2', name: 'Morgan Lee', phone: 'manager', email: 'manager@example.com', address: 'Store manager', role: 'SALES_MANAGER', username: 'manager', pin: '2222', active: true, totalSpent: 0 },
    { id: 'u3', name: 'Johnathan Rivera', phone: 'cashier1', email: 'johnathan@example.com', address: 'Sales team', role: 'SALES_PERSON', username: 'cashier1', pin: '1111', active: true, totalSpent: 0 },
    { id: 'u4', name: 'Sarah Jenkins', phone: 'cashier2', email: 'cashier2', address: 'Cashier', role: 'CASHIER', username: 'cashier2', pin: '3333', active: true, totalSpent: 0 },
  ],
  movements: [],
  presenceSessions: [],
  syncQueue: [],
  passwordResetRequests: [],
  audits: [{ id: 'a1', action: 'SYSTEM READY', details: 'Demo store opened on Terminal #104', by: 'System', timestamp: new Date(now - 60 * 60_000).toISOString() }],
  notices: [
    { id: 'n1', title: 'Low stock: Dark Chocolate Almond Bar', body: '3 remaining · Minimum stock is 10', read: false, timestamp: new Date(now - 30 * 60_000).toISOString() },
    { id: 'n2', title: 'Expiry alert: Greek Yogurt Vanilla', body: 'Batch YG-0492 expires in 4 days', read: false, timestamp: new Date(now - 80 * 60_000).toISOString() },
    { id: 'n3', title: 'Purchase order awaiting delivery', body: 'PO-00242 from Sonic Distribution is pending', read: false, timestamp: new Date(now - 3 * 60 * 60_000).toISOString() },
  ],
};

export const money = (n: number) => `$${n.toFixed(2)}`;
export const fmtDate = (iso: string) => new Date(iso).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });

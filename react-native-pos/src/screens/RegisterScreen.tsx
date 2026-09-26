import React from 'react';
import { Alert, Image, Pressable, ScrollView, Text, View } from 'react-native';
import { CartLine, categories, Person, Product, Store } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;

/** Register UI receives state and actions from the app workflow coordinator. */
export type RegisterScreenProps = {
  c: Palette; s: ScreenStyles; blue: string; store: Store; customer: string;
  setCustomer: (customer: string) => void; search: string; setSearch: (value: string) => void;
  cameraPermission: { granted: boolean } | null; requestCameraPermission: () => Promise<{ granted: boolean }>;
  setBarcodeScanned: (value: boolean) => void; setModal: any;
  category: string; setCategory: (category: string) => void; cart: CartLine[];
  addLine: (product: Product) => void; filteredProducts: Product[];
  displayMoney: (amount: number) => string; Icon: React.ComponentType<any>;
  SearchBox: React.ComponentType<any>; Chip: React.ComponentType<any>;
};

/** Customer selection, barcode search, catalog browsing and cart entry point. */
export function RegisterScreen({ c, s, blue, store, customer, setCustomer, search, setSearch, cameraPermission, requestCameraPermission, setBarcodeScanned, setModal, category, setCategory, cart, addLine, filteredProducts, displayMoney, Icon, SearchBox, Chip }: RegisterScreenProps) {
  return <>
    <View style={[s.customerSelector, { backgroundColor: c.surface, borderColor: c.border }]}><View style={[s.avatar, { backgroundColor: '#DBEAFE' }]}><Icon c={c} name="account-outline" color={blue} /></View><View style={{ flex: 1 }}><Text style={[s.mini, { color: c.muted }]}>CUSTOMER</Text><Text style={[s.rowTitle, { color: c.text }]}>{customer}</Text></View><Pressable onPress={() => Alert.alert('Select customer', ['Walk-in Customer', ...store.customers.map((x: Person) => x.name)].join('\n\n'), [{ text: 'Walk-in', onPress: () => setCustomer('Walk-in Customer') }, ...store.customers.slice(0, 3).map(x => ({ text: x.name, onPress: () => setCustomer(x.name) }))])}><Text style={[s.link, { color: c.primary }]}>Change</Text></Pressable></View>
    <SearchBox c={c} value={search} onChange={setSearch} placeholder="Search products or scan barcode" onScan={async () => { if (!cameraPermission?.granted) { const result = await requestCameraPermission(); if (!result.granted) { Alert.alert('Camera permission required', 'Allow camera access to scan product barcodes.'); return; } } setBarcodeScanned(false); setModal('scanner'); }} />
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.chipsRow}>{categories.map(item => <Chip key={item} c={c} active={item === category} label={item} onPress={() => setCategory(item)} />)}</ScrollView>
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Popular products</Text><Pressable onPress={() => setModal('cart')}><Text style={[s.link, { color: c.primary }]}>Cart · {cart.reduce((sum, item) => sum + item.quantity, 0)}</Text></Pressable></View>
    <View style={s.productGrid}>{filteredProducts.map(product => <Pressable key={product.id} onPress={() => addLine(product)} style={[s.productTile, { backgroundColor: c.surface, borderColor: c.border }]}><View style={[s.productEmoji, { backgroundColor: product.category === 'Beverages' ? '#DBEAFE' : product.category === 'Electronics' ? '#EDE9FE' : '#CCFBF1' }]}>{product.imageUri ? <Image source={{ uri: product.imageUri }} style={{ width: 45, height: 43, borderRadius: 13 }} /> : <Text style={{ fontSize: 29 }}>{product.category === 'Beverages' ? '☕' : product.category === 'Electronics' ? '🎧' : product.category === 'Dairy & Fresh' ? '🥛' : product.category === 'Snacks' ? '🍫' : product.category === 'Health & Wellness' ? '🌿' : '🛍️'}</Text>}</View><Text style={[s.productName, { color: c.text }]} numberOfLines={2}>{product.name}</Text><Text style={[s.mini, { color: c.muted }]}>{product.stock} in stock</Text><View style={s.productTileFoot}><Text style={[s.productPrice, { color: c.primary }]}>{displayMoney(product.price)}</Text><View style={[s.plusCircle, { backgroundColor: c.primary }]}><Icon c={c} name="plus" color="#fff" size={16} /></View></View></Pressable>)}</View>
    <View style={{ height: 16 }} />
  </>;
}

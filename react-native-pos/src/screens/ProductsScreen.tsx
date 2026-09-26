import React from 'react';
import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { categories, Product, Store } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type ProductsScreenProps = {
  c: Palette; s: ScreenStyles; store: Store; search: string; setSearch: (value: string) => void;
  category: string; setCategory: (category: string) => void; filteredProducts: Product[];
  Icon: React.ComponentType<any>; SearchBox: React.ComponentType<any>; Chip: React.ComponentType<any>; ProductRow: React.ComponentType<any>;
  displayMoney: (amount: number) => string; addLine: (product: Product) => void;
  showForm: (kind: any, product?: Product) => void;
  removeProduct: (product: Product) => void;
};

/** Catalog management surface. Mutations are callbacks so this screen owns no store logic. */
export function ProductsScreen({ c, s, store, search, setSearch, category, setCategory, filteredProducts, Icon, SearchBox, Chip, ProductRow, displayMoney, addLine, showForm, removeProduct }: ProductsScreenProps) {
  return <>
    <View style={s.sectionHead}><Text style={[s.subText, { color: c.muted }]}>{store.products.length} products in catalog</Text><Pressable onPress={() => showForm('product')} style={[s.primaryAction, { backgroundColor: c.primary }]}><Icon c={c} name="plus" color="#fff" size={16} /><Text style={s.primaryActionText}>Add product</Text></Pressable></View>
    <SearchBox c={c} value={search} onChange={setSearch} placeholder="Search name, SKU or brand" />
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.chipsRow}>{categories.map(item => <Chip key={item} c={c} active={item === category} label={item} onPress={() => setCategory(item)} />)}</ScrollView>
    {filteredProducts.map(product => <ProductRow key={product.id} c={c} product={product} trailing={<View style={{ flexDirection: 'row', gap: 4 }}><Pressable onPress={() => addLine(product)} style={[s.iconBtnSmall, { backgroundColor: c.primarySoft }]}><Icon c={c} name="cart-plus" color={c.primary} size={16} /></Pressable><Pressable onPress={() => showForm('product', product)} style={[s.iconBtnSmall, { backgroundColor: c.bg }]}><Icon c={c} name="pencil-outline" size={16} /></Pressable><Pressable onPress={() => Alert.alert('Delete product?', `${product.name} will be removed from the active catalog. Existing receipts keep their item records.`, [{ text: 'Cancel', style: 'cancel' }, { text: 'Delete', style: 'destructive', onPress: () => removeProduct(product) }])} style={[s.iconBtnSmall, { backgroundColor: '#FEE2E2' }]}><Icon c={c} name="delete-outline" color={c.danger} size={16} /></Pressable></View>} onPress={() => Alert.alert(product.name, `SKU ${product.sku}\n${product.brand} · ${product.category}\nCost ${displayMoney(product.cost)} · Retail ${displayMoney(product.price)}\nStock ${product.stock} · Min ${product.minimum}\nBatch ${product.batch}${product.expiry ? ` · Expiry ${product.expiry}` : ''}`)} />)}
  </>;
}

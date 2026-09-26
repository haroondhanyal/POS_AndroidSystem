import React from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { Product, Store } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
const daysUntil = (date?: string) => date ? Math.ceil((new Date(`${date}T23:59:59`).getTime() - Date.now()) / 86_400_000) : Infinity;

export type InventoryScreenProps = {
  c: Palette; s: ScreenStyles; store: Store; lowStock: Product[]; inventoryFilter: string;
  setInventoryFilter: (filter: string) => void; Icon: React.ComponentType<any>;
  Chip: React.ComponentType<any>; ProductRow: React.ComponentType<any>; Empty: React.ComponentType<any>;
  stockAdjust: (product: Product, delta: number) => void;
};

/** Stock health, expiry filters and adjustment controls. */
export function InventoryScreen({ c, s, store, lowStock, inventoryFilter, setInventoryFilter, Icon, Chip, ProductRow, Empty, stockAdjust }: InventoryScreenProps) {
  const rows = store.products.filter(p => inventoryFilter === 'All' || (inventoryFilter === 'Low stock' && p.stock > 0 && p.stock <= p.minimum) || (inventoryFilter === 'Out of stock' && p.stock === 0) || (inventoryFilter === 'Expired' && daysUntil(p.expiry) < 0) || (inventoryFilter === 'Expiring soon' && daysUntil(p.expiry) >= 0 && daysUntil(p.expiry) <= 30));
  return <>
    <View style={s.inventoryTotals}>{[[String(store.products.reduce((sum, p) => sum + p.stock, 0)), 'UNITS ON HAND'], [String(lowStock.length), 'LOW STOCK'], [String(store.products.filter(p => daysUntil(p.expiry) < 0).length), 'EXPIRED ITEMS']].map(([value, label]) => <View key={label} style={[s.inventoryTotal, { backgroundColor: c.surface, borderColor: c.border }]}><Text style={[s.metricValue, { color: c.text }]}>{value}</Text><Text style={[s.mini, { color: c.muted }]}>{label}</Text></View>)}</View>
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.chipsRow}>{['All', 'Low stock', 'Out of stock', 'Expiring soon', 'Expired'].map(item => <Chip key={item} c={c} active={inventoryFilter === item} label={item} onPress={() => setInventoryFilter(item)} />)}</ScrollView>
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Stock levels</Text><Text style={[s.mini, { color: c.muted }]}>Sorted by alert status</Text></View>
    {rows.map(product => { const expired = daysUntil(product.expiry) < 0; const near = daysUntil(product.expiry) >= 0 && daysUntil(product.expiry) <= 30; return <ProductRow key={product.id} c={c} product={product} trailing={<View style={{ alignItems: 'flex-end' }}><Text style={[s.stockTag, { color: product.stock === 0 || expired ? c.danger : product.stock <= product.minimum ? c.warn : c.success }]}>{expired ? 'EXPIRED' : product.stock === 0 ? 'OUT' : product.stock <= product.minimum ? 'LOW' : 'HEALTHY'}</Text><View style={s.stockActions}><Pressable onPress={() => stockAdjust(product, -1)} style={[s.stepButton, { backgroundColor: c.bg }]}><Icon c={c} name="minus" size={16} /></Pressable><Pressable onPress={() => stockAdjust(product, 1)} style={[s.stepButton, { backgroundColor: c.primarySoft }]}><Icon c={c} name="plus" color={c.primary} size={16} /></Pressable></View></View>} subline={`SKU ${product.sku} · Min ${product.minimum}${product.expiry ? ` · ${near || expired ? (expired ? 'Expired' : `Expires ${product.expiry}`) : `Expiry ${product.expiry}`}` : ''}`} />; })}
    {rows.length === 0 && <Empty c={c} title="No items in this view" text="There are no products matching this inventory filter." />}
  </>;
}

import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { fmtDate, Purchase, Store } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type PurchasesScreenProps = {
  c: Palette; s: ScreenStyles; store: Store; Icon: React.ComponentType<any>;
  Badge: React.ComponentType<any>; displayMoney: (amount: number) => string;
  showForm: (kind: any) => void; receivePurchase: (purchase: Purchase) => void;
};

/** Purchase order list. Receiving inventory remains a coordinator-owned domain action. */
export function PurchasesScreen({ c, s, store, Icon, Badge, displayMoney, showForm, receivePurchase }: PurchasesScreenProps) {
  return <><View style={s.sectionHead}><Text style={[s.subText, { color: c.muted }]}>{store.purchases.length} purchase orders</Text><Pressable onPress={() => showForm('purchase')} style={[s.primaryAction, { backgroundColor: c.primary }]}><Icon c={c} name="plus" color="#fff" size={16} /><Text style={s.primaryActionText}>New purchase</Text></Pressable></View>{store.purchases.map(purchase => <View key={purchase.id} style={[s.listCard, { backgroundColor: c.surface, borderColor: c.border }]}><View style={s.listCardTop}><View style={[s.iconSquare, { backgroundColor: '#FEF3C7' }]}><Icon c={c} name="truck-delivery-outline" color="#B45309" /></View><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{purchase.supplier}</Text><Text style={[s.mini, { color: c.muted }]}>{purchase.number} · {fmtDate(purchase.timestamp)}</Text></View><Badge label={purchase.status} good={purchase.status === 'RECEIVED'} c={c} /></View><View style={{ height: 1, backgroundColor: c.border }} /><View style={s.rowBetween}><View><Text style={[s.mini, { color: c.muted }]}>{purchase.items.length || '—'} line items</Text><Text style={[s.productPrice, { color: c.text }]}>{displayMoney(purchase.total)}</Text></View>{purchase.status === 'PENDING' && <Pressable onPress={() => receivePurchase(purchase)} style={[s.primaryAction, { backgroundColor: c.success }]}><Icon c={c} name="check" color="#fff" size={16} /><Text style={s.primaryActionText}>Receive stock</Text></Pressable>}</View></View>)}</>;
}

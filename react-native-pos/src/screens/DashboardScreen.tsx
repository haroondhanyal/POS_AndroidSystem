import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Product, Sale, Store, Person } from '../model';

type IconName = React.ComponentProps<typeof MaterialCommunityIcons>['name'];
type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;

/** Dashboard display contract; callbacks return navigation and mutations to the app coordinator. */
export type DashboardScreenProps = {
  c: Palette; s: ScreenStyles; user: Person; store: Store; unread: number;
  todayRevenue: number; todayCount: number; todaySales: Sale[]; lowStock: Product[];
  Icon: React.ComponentType<any>; Metric: React.ComponentType<any>;
  SaleRow: React.ComponentType<any>; ProductRow: React.ComponentType<any>;
  displayMoney: (amount: number) => string; blue: string;
  setTab: any; setModal: any; setInventoryFilter: any; showForm: any;
  stockAdjust: (product: Product, amount: number) => void;
};

/** Sales overview, operational shortcuts, recent receipts and stock alerts. */
export function DashboardScreen({ c, s, user, store, unread, todayRevenue, todayCount, todaySales, lowStock, Icon, Metric, SaleRow, ProductRow, displayMoney, blue, setTab, setModal, setInventoryFilter, showForm, stockAdjust }: DashboardScreenProps) {
  return <>
    <View style={s.storeHeadline}><View style={{ flex: 1 }}><Text style={[s.eyebrow, { color: c.muted }]}>DOWNTOWN TERMINAL  ·  #104</Text><Text style={[s.bigTitle, { color: c.text }]}>Good morning, {user.name.split(' ')[0]}</Text><Text style={[s.subText, { color: c.muted }]}>Here’s your store at a glance.</Text></View><Pressable onPress={() => setModal('notifications')} style={[s.iconBtn, { backgroundColor: c.surface }]}><Icon c={c} name="bell-outline" size={21} /><View style={s.notificationDot}><Text style={s.dotText}>{unread}</Text></View></Pressable></View>
    <View style={s.metricGrid}>
      <Metric c={c} icon="cash-multiple" label="Today's sales" value={displayMoney(todayRevenue)} delta={`${todayCount} completed today`} tint="#DBEAFE" onPress={() => setTab('Reports')} />
      <Metric c={c} icon="receipt-text-outline" label="Transactions" value={String(todayCount)} delta="Completed today" tint="#CCFBF1" onPress={() => setTab('Orders')} />
      <Metric c={c} icon="package-variant-closed" label="Low stock" value={String(lowStock.length)} delta="Needs attention" tint="#FEF3C7" onPress={() => { setInventoryFilter('Low stock'); setTab('Inventory'); }} />
      <Metric c={c} icon="chart-line" label="Est. net profit" value={displayMoney(todaySales.reduce((sum,sale)=>sum+sale.items.reduce((row,item)=>row+(item.price-(store.products.find(product=>product.sku===item.sku)?.cost||0))*item.quantity,0),0))} delta="Est. gross profit" tint="#EDE9FE" onPress={() => setTab('Reports')} />
    </View>
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Quick actions</Text><Text style={[s.link, { color: c.primary }]} onPress={() => setModal('menu')}>All tools</Text></View>
    <View style={s.quickGrid}>{[['Register', 'point-of-sale', '#DBEAFE'], ['Add product', 'plus-box-outline', '#CCFBF1'], ['New purchase', 'truck-plus-outline', '#FEF3C7'], ['Reports', 'chart-box-outline', '#EDE9FE']].map(([label, icon, tint]) => <Pressable key={label} onPress={() => label === 'Register' ? setTab('Register') : label === 'Add product' ? showForm('product') : label === 'New purchase' ? showForm('purchase') : setTab('Reports')} style={[s.quickAction, { backgroundColor: c.surface, borderColor: c.border }]}><View style={[s.iconSquare, { backgroundColor: tint }]}><Icon c={c} name={icon as IconName} color={blue} /></View><Text style={[s.quickText, { color: c.text }]}>{label}</Text><Icon c={c} name="chevron-right" color={c.muted} size={18} /></Pressable>)}</View>
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Sales overview</Text><Pressable onPress={() => setTab('Reports')}><Text style={[s.link, { color: c.primary }]}>View report</Text></Pressable></View>
    <View style={[s.chartCard, { backgroundColor: c.surface, borderColor: c.border }]}><View style={s.chartTop}><View><Text style={[s.cardLabel, { color: c.muted }]}>Gross revenue</Text><Text style={[s.chartAmount, { color: c.text }]}>{displayMoney(todayRevenue || 2486.42)}</Text></View><View style={[s.pill, { backgroundColor: '#DCFCE7' }]}><Text style={{ color: '#15803D', fontSize: 11, fontWeight: '800' }}>↗ 12.8%</Text></View></View><View style={s.chartBars}>{[38, 54, 42, 68, 56, 82, 65, 93, 60, 76, 48, 88, 63, 100, 74, 90, 57, 78, 49, 86, 67, 94, 59, 82].map((height, i) => <View key={i} style={{ flex: 1, justifyContent: 'flex-end' }}><View style={{ height: `${height}%`, borderRadius: 4, backgroundColor: i > 19 ? c.primary : c.primarySoft, opacity: i > 19 ? 1 : 0.6 }} /></View>)}</View><View style={s.chartLabels}><Text style={[s.mini, { color: c.muted }]}>8 AM</Text><Text style={[s.mini, { color: c.muted }]}>12 PM</Text><Text style={[s.mini, { color: c.muted }]}>4 PM</Text><Text style={[s.mini, { color: c.muted }]}>8 PM</Text></View></View>
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Recent sales</Text><Pressable onPress={() => setTab('Orders')}><Text style={[s.link, { color: c.primary }]}>See all</Text></Pressable></View>
    {store.sales.slice(0, 4).map(sale => <SaleRow key={sale.id} c={c} sale={sale} />)}
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Stock alerts</Text><Pressable onPress={() => setTab('Inventory')}><Text style={[s.link, { color: c.primary }]}>Review stock</Text></Pressable></View>
    {lowStock.slice(0, 3).map(product => <ProductRow key={product.id} c={c} product={product} trailing={<Pressable onPress={() => stockAdjust(product, 5)} style={[s.tinyAction, { backgroundColor: c.primarySoft }]}><Text style={[s.tinyActionText, { color: c.primary }]}>+ Restock</Text></Pressable>} />)}
  </>;
}

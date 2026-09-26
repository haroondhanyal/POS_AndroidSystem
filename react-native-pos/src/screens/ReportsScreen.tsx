import React from 'react';
import { Pressable, Share, Text, View } from 'react-native';
import { Product, Person, Store } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type ReportRange = 'Daily' | 'Weekly' | 'Monthly';
export type ReportsScreenProps = {
  c: Palette; s: ScreenStyles; store: Store; user: Person; canViewMyReports: boolean;
  reportRange: ReportRange; setReportRange: (range: ReportRange) => void;
  Chip: React.ComponentType<any>; Metric: React.ComponentType<any>;
  ProgressLine: React.ComponentType<any>; ProductRow: React.ComponentType<any>;
  displayMoney: (amount: number) => string; green: string; amber: string;
};

/** Period totals and breakdowns are derived here; persisted sales stay owned by App. */
export function ReportsScreen({ c, s, store, user, canViewMyReports, reportRange, setReportRange, Chip, Metric, ProgressLine, ProductRow, displayMoney, green, amber }: ReportsScreenProps) {
  const now = new Date();
  const start = new Date(now); start.setHours(0, 0, 0, 0);
  const since = reportRange === 'Daily' ? start.getTime() : now.getTime() - (reportRange === 'Weekly' ? 7 : 30) * 86_400_000;
  const completed = store.sales.filter(sale => sale.status === 'COMPLETED' && new Date(sale.timestamp).getTime() >= since && (!canViewMyReports || sale.cashier === user.name));
  const gross = completed.reduce((sum, sale) => sum + sale.total, 0);
  const cash = completed.filter(sale => sale.method === 'CASH').reduce((sum, sale) => sum + sale.total, 0);
  const card = completed.filter(sale => sale.method === 'CARD').reduce((sum, sale) => sum + sale.total, 0);
  const margin = completed.reduce((sum, sale) => sum + sale.items.reduce((itemSum, item) => { const product = store.products.find(p => p.sku === item.sku); return itemSum + (item.price - (product?.cost || 0)) * item.quantity; }, 0), 0);
  const staffSales = canViewMyReports ? [] : [...new Set(completed.map(sale => sale.cashier))].map(name => { const rows = completed.filter(sale => sale.cashier === name); return { name, rows, total: rows.reduce((sum, sale) => sum + sale.total, 0) }; });
  return <><View style={s.chipsRow}>{(['Daily', 'Weekly', 'Monthly'] as const).map(item => <Chip key={item} c={c} active={item === reportRange} label={item} onPress={() => setReportRange(item)} />)}</View><View style={[s.reportHero, { backgroundColor: c.primary }]}><Text style={{ color: '#DBEAFE', fontWeight: '700', fontSize: 12 }}>TOTAL GROSS SALES · {reportRange.toUpperCase()}</Text><Text style={{ color: '#fff', fontSize: 34, fontWeight: '900', marginTop: 6 }}>{displayMoney(gross)}</Text><Text style={{ color: '#DBEAFE', marginTop: 7, fontSize: 12 }}>{completed.length} completed transactions · {reportRange}</Text></View><View style={s.metricGrid}><Metric c={c} icon="chart-line" label="Est. gross profit" value={displayMoney(margin)} delta="Before operating costs" tint="#CCFBF1" /><Metric c={c} icon="ticket-percent-outline" label="Average sale" value={displayMoney(completed.length ? gross / completed.length : 0)} delta="Per transaction" tint="#EDE9FE" /></View><View style={[s.listCard, { backgroundColor: c.surface, borderColor: c.border }]}><Text style={[s.sectionTitle, { color: c.text, marginBottom: 14 }]}>Payment breakdown</Text><ProgressLine c={c} title="Cash" total={cash} maximum={gross || 1} tint={c.primary} /><ProgressLine c={c} title="Card" total={card} maximum={gross || 1} tint={green} /><ProgressLine c={c} title="Wallet / other" total={Math.max(0, gross - cash - card)} maximum={gross || 1} tint={amber} /></View>
    {staffSales.length > 0 ? <><View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Sales by staff · {reportRange.toLowerCase()}</Text></View>{staffSales.map(person => <View key={person.name} style={[s.auditRow, { borderColor: c.border }]}><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{person.name}</Text><Text style={[s.mini, { color: c.muted }]}>{person.rows.length} transactions · {person.rows.map(sale => sale.invoice).join(', ')}</Text></View><Text style={[s.rowTitle, { color: c.primary }]}>{displayMoney(person.total)}</Text></View>)}</> : null}
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Best sellers</Text><Pressable onPress={() => Share.share({ title: 'POS sales report', message: `Retail POS · Sales summary\nRevenue: ${displayMoney(gross)}\nTransactions: ${completed.length}\nEstimated profit: ${displayMoney(margin)}` })}><Text style={[s.link, { color: c.primary }]}>Export / share</Text></Pressable></View>{store.products.slice().sort((a, b) => b.price - a.price).slice(0, 4).map((product: Product) => <ProductRow key={product.id} c={c} product={product} subline={`${product.category} · ${product.stock} in stock`} trailing={<Text style={[s.rowTitle, { color: c.primary }]}>{displayMoney(product.price)}</Text>} />)}<View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Sales by day</Text></View><View style={[s.listCard, { backgroundColor: c.surface, borderColor: c.border }]}><View style={s.chartBars}>{[58, 72, 48, 82, 66, 91, 75].map((height, index) => <View key={index} style={{ flex: 1, alignItems: 'center', justifyContent: 'flex-end' }}><View style={{ width: 20, height, borderRadius: 6, backgroundColor: index === 6 ? c.primary : c.primarySoft }} /><Text style={[s.mini, { color: c.muted, marginTop: 6 }]}>{['M', 'T', 'W', 'T', 'F', 'S', 'S'][index]}</Text></View>)}</View></View>
  </>;
}

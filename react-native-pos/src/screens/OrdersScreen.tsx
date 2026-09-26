import React from 'react';
import { Alert, Pressable } from 'react-native';
import { fmtDate, Person, Sale } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type OrdersScreenProps = {
  c: Palette; s: ScreenStyles; user: Person; sales: Sale[]; canViewMyReports: boolean;
  Icon: React.ComponentType<any>; SaleRow: React.ComponentType<any>;
  displayMoney: (amount: number) => string; refundSale: (sale: Sale) => void;
};

/** Receipt history and returns. Refund policy and stock restoration stay in the coordinator. */
export function OrdersScreen({ c, s, user, sales, canViewMyReports, Icon, SaleRow, displayMoney, refundSale }: OrdersScreenProps) {
  return <>{sales.filter(sale => !canViewMyReports || sale.cashier === user.name).map(sale => <SaleRow key={sale.id} c={c} sale={sale} trailing={sale.status === 'COMPLETED' && user.role !== 'CASHIER' ? <Pressable onPress={() => refundSale(sale)} style={[s.iconBtnSmall, { backgroundColor: '#FEE2E2' }]}><Icon c={c} name="backup-restore" color="#B91C1C" size={17} /></Pressable> : <Icon c={c} name="chevron-right" />} onPress={() => Alert.alert(sale.invoice, `${sale.customer} · ${fmtDate(sale.timestamp)}\n${sale.items.map(item => `${item.quantity} × ${item.name}`).join('\n')}\n\nSubtotal ${displayMoney(sale.subtotal)} · Tax ${displayMoney(sale.tax)}\n${sale.method} · Total ${displayMoney(sale.total)}\nRecorded by ${sale.evidence?.recordedBy || sale.cashier} · ${sale.evidence?.recordedAt ? fmtDate(sale.evidence.recordedAt) : fmtDate(sale.timestamp)}\nStock snapshot: ${sale.evidence ? Object.entries(sale.evidence.stockAfter).map(([sku, qty]) => `${sku}=${qty}`).join(', ') : 'recorded in local inventory movements'}${sale.status === 'REFUNDED' ? '\nREFUNDED' : ''}`)} />)}</>;
}

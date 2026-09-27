import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { SyncEvent } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type SyncStatusScreenProps = {
  c: Palette; s: ScreenStyles; networkConnected: boolean; internetReachable: boolean | null; serverConfigured: boolean;
  serverReachable: boolean; syncing: boolean; lastSyncedAt?: string; queue: SyncEvent[];
  retryNow: () => void; Icon: React.ComponentType<any>;
};

/** One place for staff to see connectivity layers, pending work and retry failures. */
export function SyncStatusScreen({ c, s, networkConnected, internetReachable, serverConfigured, serverReachable, syncing, lastSyncedAt, queue, retryNow, Icon }: SyncStatusScreenProps) {
  const status = !networkConnected ? 'Device offline' : !serverConfigured ? 'POS server not configured' : serverReachable ? syncing ? 'Syncing changes' : internetReachable === false ? 'POS server reachable · Internet unavailable' : 'POS server connected' : 'POS server unavailable';
  const tint = !networkConnected || (serverConfigured && !serverReachable) ? c.danger : syncing || queue.length ? c.warn : c.success;
  return <>
    <View style={[s.listCard, { backgroundColor: c.surface, borderColor: c.border }]}><View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}><Icon c={c} name={serverReachable ? 'cloud-check-outline' : 'cloud-off-outline'} color={tint} size={25} /><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{status}</Text><Text style={[s.mini, { color: c.muted, marginTop: 3 }]}>{serverConfigured ? `Device network: ${networkConnected ? 'connected' : 'offline'} · Internet: ${internetReachable === null ? 'checking' : internetReachable ? 'reachable' : 'unavailable'}. POS API health is checked separately.` : 'Set EXPO_PUBLIC_POS_API_URL and restart Expo to enable LAN sync.'}</Text></View></View></View>
    <View style={[s.metricGrid, { marginTop: 12 }]}><View style={[s.inventoryTotal, { backgroundColor: c.surface, borderColor: c.border }]}><Text style={[s.metricValue, { color: c.text }]}>{queue.length}</Text><Text style={[s.mini, { color: c.muted }]}>PENDING EVENTS</Text></View><View style={[s.inventoryTotal, { backgroundColor: c.surface, borderColor: c.border }]}><Text style={[s.metricValue, { color: c.text }]}>{lastSyncedAt ? new Date(lastSyncedAt).toLocaleTimeString() : '—'}</Text><Text style={[s.mini, { color: c.muted }]}>LAST SUCCESSFUL SYNC</Text></View></View>
    <Pressable disabled={!queue.length || !serverReachable || syncing} onPress={retryNow} style={[s.submitButton, { backgroundColor: !queue.length || !serverReachable || syncing ? c.raised : c.primary, marginTop: 14 }]}><Text style={[s.submitText, !queue.length || !serverReachable || syncing ? { color: c.muted } : null]}>{syncing ? 'Sync in progress…' : queue.length ? 'Retry pending sync now' : 'Everything is synced'}</Text></Pressable>
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Waiting to sync</Text><Text style={[s.mini, { color: c.muted }]}>{queue.length} events</Text></View>
    {queue.slice(0, 30).map(event => <View key={event.id} style={[s.auditRow, { borderColor: c.border }]}><View style={[s.auditBullet, { backgroundColor: event.lastError ? c.danger : c.warn }]} /><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{event.type.replaceAll('_', ' ')}</Text><Text style={[s.mini, { color: c.muted }]}>{new Date(event.createdAt).toLocaleString()} · attempts {event.attempts || 0}</Text>{event.lastError ? <Text style={[s.mini, { color: c.danger, marginTop: 3 }]} numberOfLines={2}>{event.lastError}</Text> : null}</View></View>)}
    {queue.length === 0 ? <Text style={[s.subText, { color: c.muted }]}>New sales and stock events will appear here if they cannot reach the server.</Text> : null}
  </>;
}

import React from 'react';
import { Image, Pressable, Text, View } from 'react-native';
import { fmtDate, Person, Store } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type StaffScreenProps = {
  c: Palette; s: ScreenStyles; user: Person; store: Store; blue: string;
  Icon: React.ComponentType<any>; Badge: React.ComponentType<any>; Empty: React.ComponentType<any>;
  showForm: (kind: any, person?: Person) => void; setEditingId: (id: string) => void;
};

/** Staff directory, reset requests, presence history and audit evidence. */
export function StaffScreen({ c, s, user, store, blue, Icon, Badge, Empty, showForm, setEditingId }: StaffScreenProps) {
  if (user.role !== 'ADMIN') return <Empty c={c} title="Admin access required" text="Staff and audit records are only visible to an admin." />;
  return <><View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Team members</Text><Pressable onPress={() => showForm('staff')} style={[s.primaryAction, { backgroundColor: c.primary }]}><Icon c={c} name="account-plus-outline" color="#fff" size={16} /><Text style={s.primaryActionText}>Add staff</Text></Pressable></View>{store.users.map(person => <Pressable key={person.id} onPress={() => showForm('staff', person)} style={[s.personCard, { backgroundColor: c.surface, borderColor: c.border }]}>{person.photoUri ? <Image source={{ uri: person.photoUri }} style={s.avatar} /> : <View style={[s.avatar, { backgroundColor: '#DBEAFE' }]}><Icon c={c} name="account-outline" color={blue} /></View>}<View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{person.name}</Text><Text style={[s.mini, { color: c.muted }]}>{person.username} · {person.email}</Text><Text style={[s.mini, { color: c.muted }]}>{person.phone}</Text></View><Badge label={person.role || 'STAFF'} good={person.role === 'ADMIN'} c={c} /></Pressable>)}
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Password reset requests</Text></View>{store.passwordResetRequests.filter(request => !request.completedAt).map(request => { const person = store.users.find(item => item.id === request.userId); return <View key={request.id} style={[s.listCard, { backgroundColor: c.surface, borderColor: c.border }]}><Text style={[s.rowTitle, { color: c.text }]}>{person?.name || request.username} · {request.email}</Text><Text style={[s.mini, { color: c.muted }]}>{fmtDate(request.createdAt)}</Text><Pressable onPress={() => { showForm('reset'); setEditingId(request.userId); }} style={[s.primaryAction, { backgroundColor: c.primary, alignSelf: 'flex-start', marginTop: 8 }]}><Text style={s.primaryActionText}>Approve and set password</Text></Pressable></View>; })}
    {store.presenceSessions.slice(0, 30).map(session => { const isOnline = !session.endedAt && Date.now() - new Date(session.lastSeenAt).getTime() < 30000; return <View key={session.id} style={[s.auditRow, { borderColor: c.border }]}><View style={[s.auditBullet, { backgroundColor: isOnline ? c.success : c.border }]} /><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{session.name} · {session.role} · {isOnline ? 'Online' : session.endedAt ? 'Signed out' : 'Offline'}</Text><Text style={[s.mini, { color: c.muted }]}>{fmtDate(session.startedAt)} · Online {Math.floor(session.onlineSeconds / 60)}m · Offline {Math.floor(session.offlineSeconds / 60)}m</Text></View></View>; })}
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Audit trail · {store.audits.length}</Text></View>{store.audits.slice(0, 35).map(event => <View key={event.id} style={[s.auditRow, { borderColor: c.border }]}><View style={[s.auditBullet, { backgroundColor: c.primary }]} /><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{event.action}</Text><Text style={[s.mini, { color: c.muted }]}>{event.details}</Text><Text style={[s.mini, { color: c.muted }]}>{event.by} · {fmtDate(event.timestamp)}</Text></View></View>)}
  </>;
}

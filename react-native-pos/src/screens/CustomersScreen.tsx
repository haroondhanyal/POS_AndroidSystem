import React from 'react';
import { Image, Pressable, Text, View } from 'react-native';
import { Person, Store } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type CustomersScreenProps = {
  c: Palette; s: ScreenStyles; store: Store; peopleTab: 'Customers' | 'Suppliers';
  setPeopleTab: (tab: 'Customers' | 'Suppliers') => void; showForm: (kind: any, person?: Person) => void;
  Icon: React.ComponentType<any>;
  displayMoney: (amount: number) => string; ageFromDOB: (dob: string) => string;
  blue: string; green: string;
};

/** Customer/supplier directory with profile photos and contact details. */
export function CustomersScreen({ c, s, store, peopleTab, setPeopleTab, showForm, Icon, displayMoney, ageFromDOB, blue, green }: CustomersScreenProps) {
  const list = peopleTab === 'Customers' ? store.customers : store.suppliers;
  return <><View style={[s.segment, { backgroundColor: c.raised }]}>{(['Customers', 'Suppliers'] as const).map(item => <Pressable key={item} onPress={() => setPeopleTab(item)} style={[s.segmentItem, item === peopleTab && { backgroundColor: c.surface }]}><Text style={[s.segmentText, { color: item === peopleTab ? c.text : c.muted }]}>{item} ({item === 'Customers' ? store.customers.length : store.suppliers.length})</Text></Pressable>)}</View><View style={s.sectionHead}><Text style={[s.subText, { color: c.muted }]}>Relationship directory</Text><Pressable onPress={() => showForm(peopleTab === 'Customers' ? 'customer' : 'supplier')} style={[s.primaryAction, { backgroundColor: c.primary }]}><Icon c={c} name="plus" color="#fff" size={16} /><Text style={s.primaryActionText}>Add {peopleTab === 'Customers' ? 'customer' : 'supplier'}</Text></Pressable></View>{list.map(person => <View key={person.id} style={[s.personCard, { backgroundColor: c.surface, borderColor: c.border }]}>{person.photoUri ? <Image source={{ uri: person.photoUri }} style={s.avatar} /> : <View style={[s.avatar, { backgroundColor: peopleTab === 'Customers' ? '#DBEAFE' : '#CCFBF1' }]}><Text style={{ color: peopleTab === 'Customers' ? blue : green, fontWeight: '800' }}>{person.name.split(' ').map(p => p[0]).join('').slice(0, 2)}</Text></View>}<View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{person.name}</Text><Text style={[s.mini, { color: c.muted }]}>{person.phone || 'No phone'}{person.email ? ` · ${person.email}` : ''}</Text><Text style={[s.mini, { color: c.muted }]}>{person.address || 'No address'}{person.dateOfBirth ? ` · DOB ${person.dateOfBirth} · Age ${ageFromDOB(person.dateOfBirth)}` : ''}</Text></View><View style={{ alignItems: 'flex-end' }}><Text style={[s.mini, { color: c.muted }]}>{peopleTab === 'Customers' ? 'LIFETIME' : 'PURCHASES'}</Text><Text style={[s.rowTitle, { color: c.primary }]}>{displayMoney(person.totalSpent)}</Text><Pressable onPress={() => showForm(peopleTab === 'Customers' ? 'customer' : 'supplier', person)}><Text style={[s.link, { color: c.primary, marginTop: 6 }]}>Edit</Text></Pressable></View></View>)}</>;
}

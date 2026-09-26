import React from 'react';
import { Image, Pressable, Text, View } from 'react-native';
import { Person } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
export type ProfileScreenProps = {
  c: Palette; s: ScreenStyles; user: Person; Icon: React.ComponentType<any>;
  ageFromDOB: (dob: string) => string; showForm: (kind: any, person?: Person) => void;
  openSecuritySettings: () => void;
};

/** Signed-in user's profile view; profile edits and credential flows go to app-owned actions. */
export function ProfileScreen({ c, s, user, Icon, ageFromDOB, showForm, openSecuritySettings }: ProfileScreenProps) {
  return <><View style={s.personCard}>{user.photoUri ? <Image source={{ uri: user.photoUri }} style={[s.avatar, { width: 64, height: 64 }]} /> : <Icon c={c} name="account-circle" size={45} color={c.primary} />}<View><Text style={[s.rowTitle, { color: c.text }]}>{user.name}</Text><Text style={[s.mini, { color: c.muted }]}>{user.role} · {user.email}</Text><Text style={[s.mini, { color: c.muted }]}>{user.phone}</Text><Text style={[s.mini, { color: c.muted }]}>{user.dateOfBirth ? `DOB ${user.dateOfBirth} · Age ${ageFromDOB(user.dateOfBirth)}` : 'Date of birth not set'}</Text></View></View><Pressable onPress={() => showForm('staff', user)} style={[s.submitButton, { backgroundColor: c.primary }]}><Text style={s.submitText}>Edit profile</Text></Pressable><Pressable onPress={openSecuritySettings} style={[s.secondaryButton, { borderColor: c.border }]}><Text style={[s.buttonLabel, { color: c.primary }]}>Update password / PIN</Text></Pressable></>;
}

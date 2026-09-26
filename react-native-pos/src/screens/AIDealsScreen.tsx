import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { deals } from '../model';

type Palette = Record<string, string>;
type ScreenStyles = Record<string, any>;
type AssistantMessage = { user: boolean; text: string; dealIds?: string[] };
export type AIDealsScreenProps = {
  c: Palette; s: ScreenStyles; messages: AssistantMessage[]; Icon: React.ComponentType<any>;
  Chip: React.ComponentType<any>; displayMoney: (amount: number) => string;
  sendMessage: (message: string) => void; addToCartFromDeal: (dealId: string) => void;
};

/** Offer suggestions and bundle cards; the assistant/cart actions are supplied by App. */
export function AIDealsScreen({ c, s, messages, Icon, Chip, displayMoney, sendMessage, addToCartFromDeal }: AIDealsScreenProps) {
  return <><View style={[s.aiBanner, { backgroundColor: c.primarySoft }]}><View style={[s.iconSquare, { backgroundColor: c.surface }]}><Icon c={c} name="creation" color={c.primary} /></View><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>Retail POS Genius</Text><Text style={[s.mini, { color: c.muted }]}>Offline offers assistant · Roman Urdu supported</Text></View><View style={[s.pill, { backgroundColor: '#DCFCE7' }]}><Text style={{ color: '#15803D', fontWeight: '800', fontSize: 10 }}>● ONLINE</Text></View></View>
    <View style={s.chipsRow}>{['New offers', 'Coffee deals', 'Clearance'].map((item, i) => <Chip key={item} c={c} label={item} active={false} onPress={() => sendMessage(i === 0 ? 'What offers are active?' : i === 1 ? 'Coffee deals dikhao' : 'Show clearance items')} />)}</View>
    <View style={[s.chatBox, { backgroundColor: c.surface, borderColor: c.border }]}>{messages.map((message, index) => <View key={index} style={[s.chatBubble, message.user ? { alignSelf: 'flex-end', backgroundColor: c.primary } : { alignSelf: 'flex-start', backgroundColor: c.bg, borderColor: c.border }]}><Text style={{ color: message.user ? '#fff' : c.text, fontSize: 13, lineHeight: 19 }}>{message.text}</Text>{!message.user && message.dealIds?.map(id => { const deal = deals.find(item => item.id === id); return deal ? <View key={id} style={[s.dealMini, { borderColor: c.border, backgroundColor: c.surface }]}><Text style={[s.rowTitle, { color: c.text, flex: 1 }]}>{deal.title}</Text><Text style={[s.rowTitle, { color: c.primary }]}>{displayMoney(deal.price)}</Text><Pressable style={[s.addDealBtn, { backgroundColor: c.primary }]} onPress={() => addToCartFromDeal(id)}><Text style={s.primaryActionText}>Add to cart</Text></Pressable></View> : null; })}</View>)}</View>
    <View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>Active bundle deals</Text><Text style={[s.mini, { color: c.muted }]}>{deals.length} available</Text></View>
    {deals.map(deal => <View key={deal.id} style={[s.dealCard, { backgroundColor: c.surface, borderColor: c.border }]}><View style={s.dealCardTop}><View style={[s.iconSquare, { backgroundColor: '#FEF3C7' }]}><Icon c={c} name="tag-heart-outline" color="#B45309" /></View><View style={{ flex: 1 }}><Text style={[s.mini, { color: c.primary, fontWeight: '800' }]}>{deal.category.toUpperCase()} · SAVE {deal.discount}%</Text><Text style={[s.rowTitle, { color: c.text }]}>{deal.title}</Text></View></View><Text style={[s.subText, { color: c.muted }]}>{deal.pitch}</Text><View style={s.dealFoot}><Text style={[s.mini, { color: c.muted, textDecorationLine: 'line-through' }]}>{displayMoney(deal.original)}</Text><Text style={[s.productPrice, { color: c.text }]}>{displayMoney(deal.price)}</Text><Pressable onPress={() => addToCartFromDeal(deal.id)} style={[s.primaryAction, { backgroundColor: c.primary }]}><Icon c={c} name="cart-plus" color="#fff" size={16} /><Text style={s.primaryActionText}>Add bundle</Text></Pressable></View></View>)}
  </>;
}

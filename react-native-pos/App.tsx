import AsyncStorage from '@react-native-async-storage/async-storage';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { StatusBar } from 'expo-status-bar';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert, AppState, Image, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, Share,
  StatusBar as NativeStatusBar, StyleSheet, Switch, Text, TextInput, View,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import * as ImagePicker from 'expo-image-picker';
import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import NetInfo from '@react-native-community/netinfo';
import * as Crypto from 'expo-crypto';
import * as DocumentPicker from 'expo-document-picker';
import { AudioModule, RecordingPresets, setAudioModeAsync, useAudioPlayer, useAudioRecorder } from 'expo-audio';
import DateTimePicker from '@expo/ui/community/datetime-picker';
import { Directory, File, Paths } from 'expo-file-system';
import CountryPicker, { CountryCode as CountryISO } from 'react-native-country-picker-modal';
import { Audit, CartLine, categories, deals, fmtDate, initialStore, money as usdMoney, Movement, Notice, Person, Product, Purchase, Role, Sale, SaleLine, Store, PresenceSession, SyncEvent, PasswordResetRequest, ChatMessage } from './src/model';
import { apiEndpoint, apiFetch, bytesToBase64, remoteFileUri } from './src/services/posApi';
import { CURRENCY_KEY, STORE_KEY, saveLocalStore } from './src/services/localStore';
import { DashboardScreen } from './src/screens/DashboardScreen';
import { RegisterScreen } from './src/screens/RegisterScreen';
import { ProductsScreen } from './src/screens/ProductsScreen';
import { InventoryScreen } from './src/screens/InventoryScreen';
import { OrdersScreen } from './src/screens/OrdersScreen';
import { PurchasesScreen } from './src/screens/PurchasesScreen';
import { CustomersScreen } from './src/screens/CustomersScreen';
import { AIDealsScreen } from './src/screens/AIDealsScreen';
import { ReportsScreen } from './src/screens/ReportsScreen';
import { ProfileScreen } from './src/screens/ProfileScreen';
import { StaffScreen } from './src/screens/StaffScreen';
import { AdminScreen } from './src/screens/AdminScreen';
import { TeamChatScreen } from './src/screens/TeamChatScreen';
import { AuthScreen, roles, SignupFormData } from './src/screens/AuthScreen';


type IconName = React.ComponentProps<typeof MaterialCommunityIcons>['name'];
type Module = 'Dashboard' | 'Register' | 'AI Deals' | 'Products' | 'Inventory' | 'Orders' | 'Purchases' | 'Customers' | 'Reports' | 'Staff & Audit' | 'Admin Panel' | 'My Profile' | 'Team Chat';
const modules: { name: Module; icon: IconName }[] = [
  { name: 'Dashboard', icon: 'view-dashboard-outline' }, { name: 'Register', icon: 'point-of-sale' },
  { name: 'AI Deals', icon: 'creation-outline' }, { name: 'Products', icon: 'shopping-outline' },
  { name: 'Inventory', icon: 'package-variant-closed' }, { name: 'Orders', icon: 'receipt-text-outline' },
  { name: 'Purchases', icon: 'truck-delivery-outline' }, { name: 'Customers', icon: 'account-group-outline' },
  { name: 'Reports', icon: 'chart-box-outline' }, { name: 'Staff & Audit', icon: 'shield-account-outline' },
  { name: 'Admin Panel', icon: 'security' }, { name: 'My Profile', icon: 'account-circle-outline' }, { name: 'Team Chat', icon: 'message-text-outline' },
];
const blue = '#1D4ED8';
const green = '#0F766E';
const amber = '#B45309';
const moneyRound = (n: number) => Number(n.toFixed(2));
const uid = () => `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const daysUntil = (date?: string) => date ? Math.ceil((new Date(`${date}T23:59:59`).getTime() - Date.now()) / 86_400_000) : Infinity;
const divider = (color: string) => ({ height: StyleSheet.hairlineWidth, backgroundColor: color });
const imageLimit = 20 * 1024 * 1024;
let selectedCurrency: 'USD' | 'PKR' = 'USD';
let usdToPkr = 278;
const displayMoney = (amount: number) => selectedCurrency === 'PKR' ? `Rs ${new Intl.NumberFormat('en-PK',{minimumFractionDigits:2,maximumFractionDigits:2}).format(amount*usdToPkr)}` : usdMoney(amount);
const toSelectedCurrency = (amount: number) => selectedCurrency === 'PKR' ? amount*usdToPkr : amount;
const enteredToUsd = (amount: number) => selectedCurrency === 'PKR' ? amount / Math.max(1,usdToPkr) : amount;
const persistPhoto = async (asset: ImagePicker.ImagePickerAsset): Promise<string | null> => {
  const source = new File(asset.uri);
  const size = asset.fileSize ?? source.size ?? 0;
  if (size > imageLimit) { Alert.alert('Photo is too large', 'Choose a profile photo under 20 MB.'); return null; }
  const folder = new Directory(Paths.document, 'pos-profile-photos');
  if (!folder.exists) folder.create({ intermediates: true, idempotent: true });
  const extension = asset.mimeType?.includes('png') ? 'png' : asset.mimeType?.includes('webp') ? 'webp' : 'jpg';
  const destination = new File(folder, `${uid()}.${extension}`);
  await source.copy(destination);
  return destination.uri;
};

export default function App() {
  const [store, setStore] = useState<Store>(initialStore);
  const [currency, setCurrency] = useState<'USD'|'PKR'>('USD');
  const [exchangeRate, setExchangeRate] = useState(278);
  const [exchangeRateInput, setExchangeRateInput] = useState('278');
  const [chatRecipient, setChatRecipient] = useState('u1');
  const [chatDraft, setChatDraft] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const chatScrollRef = useRef<ScrollView>(null);
  const pendingChatSends = useRef(new Set<string>());
  const audioRecorder = useAudioRecorder({ ...RecordingPresets.HIGH_QUALITY, directory: 'document' });
  const [loaded, setLoaded] = useState(false);
  const [user, setUser] = useState<Person | null>(null);
  const [tab, setTab] = useState<Module>('Dashboard');
  const [dark, setDark] = useState(false);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [customer, setCustomer] = useState('Walk-in Customer');
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('All');
  const [inventoryFilter, setInventoryFilter] = useState('All');
  const [peopleTab, setPeopleTab] = useState<'Customers' | 'Suppliers'>('Customers');
  const [modal, setModal] = useState<'menu' | 'notifications' | 'checkout' | 'receipt' | 'form' | 'cart' | 'scanner' | 'forgot' | 'security' | null>(null);
  const [formKind, setFormKind] = useState<'product' | 'customer' | 'supplier' | 'purchase' | 'staff' | 'reset'>('product');
  const [form, setForm] = useState<Record<string, string>>({});
  const [editingId, setEditingId] = useState<string | null>(null);
  const [payment, setPayment] = useState('CASH');
  const [tender, setTender] = useState('');
  const [cartDiscount, setCartDiscount] = useState(0);
  const [receipt, setReceipt] = useState<Sale | null>(null);
  const [query, setQuery] = useState('');
  const [messages, setMessages] = useState<{ user: boolean; text: string; dealIds?: string[] }[]>([
    { user: false, text: '👋 Assalam-o-alaikum! Main aapka POS offers assistant hoon. Deals, discounts, ya clearance ke baare mein poochiye.' },
  ]);
  const [loginName, setLoginName] = useState('admin');
  const [loginPin, setLoginPin] = useState('RetailPOS!01AD9DBF95');
  const [loginRole, setLoginRole] = useState<Role>('ADMIN');
  const [loginMode, setLoginMode] = useState<'password' | 'pin'>('password');
  const [securityPin, setSecurityPin] = useState('');
  const [securityPinConfirm, setSecurityPinConfirm] = useState('');
  const [securityPassword, setSecurityPassword] = useState('');
  const [securityPasswordConfirm, setSecurityPasswordConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [visibleFields, setVisibleFields] = useState<Record<string, boolean>>({});
  const [online, setOnline] = useState(false);
  const [appForeground, setAppForeground] = useState(true);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [reportRange, setReportRange] = useState<'Daily' | 'Weekly' | 'Monthly'>('Daily');
  const [recoveryEmail, setRecoveryEmail] = useState('');
  const [cameraPermission, requestCameraPermission] = useCameraPermissions();
  const [barcodeScanned, setBarcodeScanned] = useState(false);
  const syncInFlight = useRef(false);
  const [loginError, setLoginError] = useState('');
  const [signupForm, setSignupForm] = useState<SignupFormData>({ name:'', username:'', email:'', countryCode:'+92', countryISO:'PK', phone:'', dateOfBirth:'', photoUri:'', role:'SALES_PERSON', password:'', confirmPassword:'', pin:'', confirmPin:'' });
  const [showSignupCalendar, setShowSignupCalendar] = useState(false);
  const [signupOpen, setSignupOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);

  const darkMode = dark;
  selectedCurrency = currency; usdToPkr = exchangeRate;
  const syncChatMessage = async (message:ChatMessage) => {
    if(!apiEndpoint||!online||message.syncedAt||pendingChatSends.current.has(message.id))return;
    pendingChatSends.current.add(message.id);
    try{
      const outgoing:ChatMessage&{attachmentBase64?:string}={...message};
      if(message.uri&&!message.uri.startsWith('http')){const file=new File(message.uri);outgoing.attachmentBase64=bytesToBase64(await file.arrayBuffer());}
      const response=await apiFetch('/pos/chat/messages',{method:'POST',body:JSON.stringify(outgoing)});
      if(!response.ok)throw new Error('Chat server did not accept this message.');
      const result=await response.json() as {message:ChatMessage};
      setStore(prev=>({...prev,messages:(prev.messages||[]).map(item=>item.id===message.id?{...result.message,syncedAt:new Date().toISOString()}:item)}));
    }catch{/* Keep the message in the local outbox and retry after reconnecting. */}
    finally{pendingChatSends.current.delete(message.id);}
  };
  const c = useMemo(() => darkMode ? {
    bg: '#0B0F19', surface: '#141C2E', raised: '#1E293B', text: '#F8FAFC', muted: '#CBD5E1', border: '#334155', primary: '#38BDF8', primarySoft: '#1E3A8A', success: '#2DD4BF', warn: '#FBBF24', danger: '#FB7185', onPrimary: '#03223F',
  } : {
    bg: '#F1F5F9', surface: '#FFFFFF', raised: '#E2E8F0', text: '#090D16', muted: '#475569', border: '#CBD5E1', primary: blue, primarySoft: '#DBEAFE', success: green, warn: amber, danger: '#DC2626', onPrimary: '#FFFFFF',
  }, [darkMode]);

  useEffect(() => {
    (async () => {
      const value = await AsyncStorage.getItem(STORE_KEY);
      const saved = value ? JSON.parse(value) as Partial<Store> : {};
      const hydrated: Store = { ...initialStore, ...saved,
        presenceSessions: saved.presenceSessions || [], syncQueue: saved.syncQueue || [],
        passwordResetRequests: saved.passwordResetRequests || [], messages: saved.messages || [],
      };
      const currencySettings = await AsyncStorage.getItem(CURRENCY_KEY);
      if (currencySettings) { const parsed = JSON.parse(currencySettings) as { currency?:'USD'|'PKR'; rate?:number }; if(parsed.currency)setCurrency(parsed.currency); if(parsed.rate && parsed.rate>0){setExchangeRate(parsed.rate);setExchangeRateInput(String(parsed.rate));} }
      if(apiEndpoint){try{const response=await apiFetch('/pos/users');if(response.ok){const remote=await response.json() as {users:Person[];presence:PresenceSession[]};const merged=new Map((hydrated.users||[]).map(person=>[person.id,person]));for(const person of remote.users||[]){const local=merged.get(person.id);merged.set(person.id,local?{...person,...local}:{...person,phone:person.phone||'',email:person.email||'',address:'',totalSpent:0,active:true});}hydrated.users=[...merged.values()];hydrated.presenceSessions=[...(hydrated.presenceSessions||[]),...(remote.presence||[])];}}catch{/* API is optional while the app is offline. */}}
      const migrationKey = 'retail-pos-admin-credential-reset-20260927';
      if (!await AsyncStorage.getItem(migrationKey)) {
        hydrated.users = hydrated.users.map(person => person.id === 'u1' ? { ...person,
          username: 'admin', email: 'admin', pin: undefined,
          passwordSalt: '89b2643e37a8bebadc167341a56af160', passwordHash: '0adc89afcac5326495b4decd7b7f909e260b7b2fdf96e66bcbc0e3106af9f515',
          pinSalt: '27f25b946a76bec5cd5481268eb92d82', pinHash: '9568cdda7e9ba797465809e7bb104eb0da3b39c66cfc33b94060456958cbca31',
        } : person);
        await AsyncStorage.setItem(migrationKey, 'complete');
      }
      const now = new Date().toISOString();
      hydrated.presenceSessions = hydrated.presenceSessions.map(session => session.endedAt ? session : ({ ...session, endedAt: now, lastSeenAt: now }));
      setStore(hydrated);
      setLoaded(true);
    })().catch(() => setLoaded(true));
  }, []);
  useEffect(() => {
    if (loaded) saveLocalStore(store).catch(() => {});
  }, [store, loaded]);
  useEffect(() => { if (loaded) AsyncStorage.setItem(CURRENCY_KEY,JSON.stringify({currency,rate:exchangeRate})).catch(()=>{}); },[currency,exchangeRate,loaded]);
  useEffect(() => NetInfo.addEventListener(state => setOnline(Boolean(state.isConnected && state.isInternetReachable !== false))), []);
  useEffect(() => { const subscription=AppState.addEventListener('change',state=>setAppForeground(state==='active'));return()=>subscription.remove(); },[]);
  useEffect(() => {
    if (!sessionId) return;
    const interval = setInterval(() => {
      const tick = Date.now();
      setStore(prev => ({ ...prev, presenceSessions: prev.presenceSessions.map(session => {
        if (session.id !== sessionId || session.endedAt) return session;
        const seconds = Math.max(0, Math.floor((tick - new Date(session.lastTickAt).getTime()) / 1000));
        const now = new Date(tick).toISOString();
        return { ...session, lastTickAt: now, lastSeenAt: appForeground ? now : session.lastSeenAt,
          onlineSeconds: session.onlineSeconds + (appForeground ? seconds : 0),
          offlineSeconds: session.offlineSeconds + (appForeground ? 0 : seconds) };
      }) }));
    }, 10_000);
    return () => clearInterval(interval);
  }, [sessionId, appForeground]);
  useEffect(() => {
    const endpoint = process.env.EXPO_PUBLIC_POS_API_URL?.replace(/\/$/, '');
    if (!online || !endpoint || !loaded || !store.syncQueue.length || syncInFlight.current) return;
    syncInFlight.current = true;
    const batch = store.syncQueue;
    apiFetch('/pos/sync', { method: 'POST', body: JSON.stringify({ events: batch }) })
      .then(response => { if (!response.ok) throw new Error('Sync endpoint rejected the queue.'); return response.json().catch(() => ({})); })
      .then(() => { const ids = new Set(batch.map(event => event.id)); setStore(prev => ({ ...prev, syncQueue: prev.syncQueue.filter(event => !ids.has(event.id)) })); })
      .catch(() => {})
      .finally(() => { syncInFlight.current = false; });
  }, [online, loaded, store.syncQueue]);

  useEffect(()=>{
    if(!apiEndpoint||!online||!loaded||!user||!sessionId)return;
    let stopped=false;
    const syncTeam=async()=>{
      try{
        const heartbeat=await apiFetch('/pos/users/heartbeat',{method:'POST',body:JSON.stringify({userId:user.id,name:user.name,role:user.role||'STAFF',email:user.email,phone:user.phone,username:user.username,passwordHash:user.passwordHash,passwordSalt:user.passwordSalt,pinHash:user.pinHash,pinSalt:user.pinSalt,dateOfBirth:user.dateOfBirth,countryCode:user.countryCode,sessionId,isActive:appForeground})});
        if(!heartbeat.ok)return;
        const team=await heartbeat.json() as {users:Person[];presence:PresenceSession[]};
        const [messageResponse,salesResponse,activityResponse]=await Promise.all([apiFetch(`/pos/chat/messages?userId=${encodeURIComponent(user.id)}`),apiFetch('/pos/sales'),apiFetch('/pos/activity')]);
        const remoteMessages=messageResponse.ok?(await messageResponse.json() as {messages:ChatMessage[]}).messages:[];
        const remoteSales=salesResponse.ok?(await salesResponse.json() as {sales:Sale[]}).sales:[];
        const remoteActivity=activityResponse.ok?(await activityResponse.json() as {activity:Audit[]}).activity:[];
        if(stopped)return;
        setStore(prev=>{
          const users=new Map(prev.users.map(person=>[person.id,person]));
          for(const remote of team.users||[]){const local=users.get(remote.id);users.set(remote.id,local?{...remote,...local,role:remote.role||local.role}:({...remote,phone:remote.phone||'',email:remote.email||'',address:'',totalSpent:0,active:true}));}
          const sessions=new Map(prev.presenceSessions.map(session=>[session.id,session]));for(const session of team.presence||[])sessions.set(session.id,{...session,endedAt:session.endedAt||undefined});
          const messages=new Map((prev.messages||[]).map(message=>[message.id,message]));for(const message of remoteMessages)messages.set(message.id,{...message,uri:message.uri?.startsWith('/')?`${apiEndpoint}${message.uri}`:message.uri,syncedAt:message.syncedAt||new Date().toISOString()});
          const sales=new Map(prev.sales.map(sale=>[sale.id,sale]));for(const sale of remoteSales)sales.set(sale.id,sale);
          const audits=new Map(prev.audits.map(event=>[event.id,event]));for(const event of remoteActivity)audits.set(event.id,event);
          const nextUsers=[...users.values()];const stableUsers=nextUsers.length===prev.users.length&&nextUsers.every(person=>JSON.stringify(person)===JSON.stringify(prev.users.find(item=>item.id===person.id)))?prev.users:nextUsers;
          return {...prev,users:stableUsers,presenceSessions:[...sessions.values()],messages:[...messages.values()].sort((a,b)=>b.timestamp.localeCompare(a.timestamp)),sales:[...sales.values()].sort((a,b)=>b.timestamp.localeCompare(a.timestamp)),audits:[...audits.values()].sort((a,b)=>b.timestamp.localeCompare(a.timestamp))};
        });
      }catch{/* Server is unreachable; the local store keeps working and will retry. */}
    };
    syncTeam();const timer=setInterval(syncTeam,8000);return()=>{stopped=true;clearInterval(timer);};
  },[apiEndpoint,online,loaded,user,sessionId,appForeground]);

  useEffect(()=>{if(online&&loaded&&apiEndpoint)apiFetch('/pos/users/sync',{method:'POST',body:JSON.stringify({users:store.users})}).catch(()=>{});},[online,loaded,store.users]);
    useEffect(()=>{if(!online||!apiEndpoint)return;for(const message of store.messages||[])syncChatMessage(message);},[online,store.messages]);

  const log = (action: string, details: string, by = user?.name || 'System') => {
    const event: Audit = { id: uid(), action, details, by, timestamp: new Date().toISOString() };
    setStore(prev => ({ ...prev, audits: [event, ...prev.audits] }));
    if(apiEndpoint&&online)apiFetch('/pos/activity',{method:'POST',body:JSON.stringify(event)}).catch(()=>{});
  };
  const updateProduct = (id: string, patch: Partial<Product>) => setStore(prev => ({ ...prev, products: prev.products.map(p => p.id === id ? { ...p, ...patch } : p) }));
  const addLine = (product: Product, discount = product.discount || 0) => {
    if (product.stock <= 0) { Alert.alert('Out of stock', `${product.name} is not available right now.`); return; }
    setCart(prev => {
      const line = prev.find(item => item.productId === product.id);
      if (line) return prev.map(item => item.productId === product.id ? { ...item, quantity: Math.min(item.quantity + 1, product.stock) } : item);
      return [...prev, { productId: product.id, quantity: 1, discount }];
    });
    setModal(null);
  };
  const changeQty = (productId: string, step: number) => setCart(prev => prev.map(line => line.productId === productId ? { ...line, quantity: line.quantity + step } : line).filter(line => line.quantity > 0 && (store.products.find(p => p.id === line.productId)?.stock || 0) >= line.quantity));
  const cartRows = cart.map(line => ({ line, product: store.products.find(product => product.id === line.productId)! })).filter(row => row.product);
  const subtotal = cartRows.reduce((sum, row) => sum + row.product.price * row.line.quantity, 0);
  const itemDiscount = cartRows.reduce((sum, row) => sum + row.product.price * row.line.quantity * row.line.discount / 100, 0);
  const beforeCartTax = subtotal - itemDiscount;
  const cartDiscountAmount = beforeCartTax * cartDiscount / 100;
  const taxTotal = cartRows.reduce((sum, row) => sum + row.product.price * row.line.quantity * (1 - row.line.discount / 100) * row.product.tax / 100, 0) * (1 - cartDiscount / 100);
  const total = Math.max(0, beforeCartTax - cartDiscountAmount + taxTotal);
  const unreadCount = store.notices.filter(n => !n.read).length;
  const canViewReports = user?.role === 'ADMIN' || user?.role === 'MANAGER' || user?.role === 'SALES_MANAGER' || user?.role === 'SALES_PERSON' || user?.role === 'CASHIER';
  const canViewMyReports = user?.role === 'SALES_PERSON' || user?.role === 'CASHIER';
  const visibleModules = user?.role === 'ADMIN' ? modules : modules.filter(module => {
    if (module.name === 'Admin Panel' || module.name === 'Staff & Audit') return false;
    if (module.name === 'Reports') return canViewReports;
    if (['Purchases', 'Inventory', 'Products'].includes(module.name)) return ['MANAGER','INVENTORY_MANAGER','SALES_MANAGER'].includes(user?.role || '');
    if (module.name === 'Customers') return user?.role !== 'STAFF';
    if (module.name === 'AI Deals') return ['MANAGER','SALES_MANAGER','SALES_PERSON','CASHIER'].includes(user?.role || '');
    return true;
  });

  const authenticate = async () => {
    if (!loaded) { setLoginError('Store credentials are still loading. Try sign in again in a moment.'); return; }
    const found = store.users.find(person => person.active !== false &&
      person.role === loginRole && (person.username?.toLowerCase() === loginName.trim().toLowerCase() || person.email?.toLowerCase() === loginName.trim().toLowerCase()));
    const valid = found && (loginMode === 'password'
      ? (found.passwordHash && found.passwordSalt ? await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, `${found.passwordSalt}:${loginPin}`) === found.passwordHash : found.pin === loginPin)
      : (found.pinHash && found.pinSalt ? await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, `${found.pinSalt}:${loginPin}`) === found.pinHash : found.pin === loginPin));
    if (!found || !valid) { setLoginError('Login details or selected role do not match.'); return; }
    const now = new Date().toISOString();
    const session: PresenceSession = { id: uid(), userId: found.id, name: found.name, role: found.role || 'STAFF', startedAt: now, lastTickAt: now, lastSeenAt: now, onlineSeconds: online ? 0 : 0, offlineSeconds: 0 };
    setSessionId(session.id);
    setStore(prev => ({ ...prev, presenceSessions: [session, ...prev.presenceSessions] }));
    setUser(found); setChatRecipient(found.role==='ADMIN'?(store.users.find(person=>person.id!==found.id)?.id||'*'):'u1'); setLoginError(''); setTab('Dashboard'); log('LOGIN', `${found.name} signed in · ${online ? 'online' : 'offline'}`, found.name);
    if (!found.pinHash) setModal('security');
  };
  const pickSignupPhoto = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) { Alert.alert('Photo permission required','Allow access to choose your profile picture.'); return; }
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes:['images'], allowsEditing:true, aspect:[1,1], quality:0.55 });
    if (!result.canceled && result.assets[0]) { const photoUri=await persistPhoto(result.assets[0]); if(photoUri)setSignupForm(current=>({...current,photoUri})); }
  };
  const createSignup = async () => {
    if (!loaded) { Alert.alert('Please wait','Store accounts are still loading.'); return; }
    const email=signupForm.email.trim().toLowerCase(); const username=(signupForm.username.trim() || email.split('@')[0]).toLowerCase();
    if (!signupForm.name.trim() || !email.includes('@') || !signupForm.phone.trim() || !signupForm.dateOfBirth || !signupForm.photoUri) { Alert.alert('Complete your profile','Name, valid email, phone, date of birth and profile photo are required.'); return; }
    if (store.users.some(person=>person.email?.toLowerCase()===email || person.username?.toLowerCase()===username)) { Alert.alert('Account already exists','That email or username is already registered. Sign in or use forgot password.'); return; }
    if (signupForm.password.length<8 || signupForm.password!==signupForm.confirmPassword) { Alert.alert('Check your password','Password must be at least 8 characters and match confirmation.'); return; }
    if (!/^\d{4,6}$/.test(signupForm.pin) || signupForm.pin!==signupForm.confirmPin) { Alert.alert('Check your PIN','Set the same 4 to 6 digit PIN in both fields.'); return; }
    const birthDate=new Date(`${signupForm.dateOfBirth}T00:00:00`);
    if (Number.isNaN(birthDate.getTime()) || birthDate.getTime()>Date.now()) { Alert.alert('Date of birth required','Choose a valid date of birth from the calendar.'); return; }
    const role=roles.includes(signupForm.role)?signupForm.role:'SALES_PERSON';
    const salt=Crypto.randomUUID(); const passwordHash=await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256,`${salt}:${signupForm.password}`); const pinSalt=Crypto.randomUUID(); const pinHash=await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256,`${pinSalt}:${signupForm.pin}`); const now=new Date().toISOString();
    const person:Person={id:uid(),name:signupForm.name.trim(),username,email,phone:`${signupForm.countryCode} ${signupForm.phone.trim()}`,countryCode:signupForm.countryCode,dateOfBirth:signupForm.dateOfBirth,address:'',role,active:true,totalSpent:0,photoUri:signupForm.photoUri,passwordHash,passwordSalt:salt,pinHash,pinSalt};
    const session:PresenceSession={id:uid(),userId:person.id,name:person.name,role,startedAt:now,lastTickAt:now,lastSeenAt:now,onlineSeconds:0,offlineSeconds:0};
    setStore(prev=>({...prev,users:[person,...prev.users],presenceSessions:[session,...prev.presenceSessions]})); setUser(person); setSessionId(session.id); setChatRecipient(role==='ADMIN'?(store.users[0]?.id||'*'):'u1'); setTab('Dashboard'); setShowSignupCalendar(false); setSignupOpen(false); setSignupForm({name:'',username:'',email:'',countryCode:'+92',countryISO:'PK',phone:'',dateOfBirth:'',photoUri:'',role:'SALES_PERSON',password:'',confirmPassword:'',pin:'',confirmPin:''}); log('SIGN UP',`${person.name} registered as ${role}`,person.name); Alert.alert('Sign up successful',`Your ${role.replaceAll('_',' ').toLowerCase()} account is ready. You are now signed in.`);
  };
  const saveSecuritySetup = async () => {
    if (!user) return;
    if (!/^\d{4,6}$/.test(securityPin) || securityPin !== securityPinConfirm) { Alert.alert('Set your PIN', 'Enter the same 4 to 6 digit PIN twice.'); return; }
    if (securityPassword && (securityPassword.length < 8 || securityPassword !== securityPasswordConfirm)) { Alert.alert('Check password', 'If you set a password, it must be at least 8 characters and match confirmation.'); return; }
    const pinSalt = Crypto.randomUUID();
    const pinHash = await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, `${pinSalt}:${securityPin}`);
    const passwordSalt = securityPassword ? Crypto.randomUUID() : user.passwordSalt;
    const passwordHash = securityPassword && passwordSalt ? await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, `${passwordSalt}:${securityPassword}`) : user.passwordHash;
    const updated = { ...user, pinHash, pinSalt, pin: undefined, passwordHash, passwordSalt };
    setStore(prev => ({ ...prev, users: prev.users.map(person => person.id === user.id ? updated : person) })); setUser(updated); setSecurityPin(''); setSecurityPinConfirm(''); setSecurityPassword(''); setSecurityPasswordConfirm(''); setModal(null); log('SECURITY SETUP', 'PIN configured after sign in', user.name);
  };
  const goLogout = () => {
    if (user) {
      if(apiEndpoint&&online&&sessionId)apiFetch('/pos/presence/offline',{method:'POST',body:JSON.stringify({sessionId})}).catch(()=>{});
      const now = new Date().toISOString();
      setStore(prev => ({ ...prev, presenceSessions: prev.presenceSessions.map(session => {
        if (session.id !== sessionId || session.endedAt) return session;
        const seconds = Math.max(0, Math.floor((Date.now() - new Date(session.lastTickAt).getTime()) / 1000));
        return { ...session, endedAt: now, lastSeenAt: now, onlineSeconds: session.onlineSeconds + (online ? seconds : 0), offlineSeconds: session.offlineSeconds + (online ? 0 : seconds) };
      }) }));
      log('LOGOUT', `${user.name} signed out`, user.name);
    }
    setSessionId(null); setUser(null); setCart([]); setTab('Dashboard'); setModal(null);
  };

  const addToCartFromDeal = (dealId: string) => {
    const deal = deals.find(d => d.id === dealId);
    if (!deal) return;
    const products = deal.skus.map(sku => store.products.find(p => p.sku === sku)).filter((p): p is Product => Boolean(p));
    if (!products.length || products.some(p => p.stock <= 0)) { Alert.alert('Deal unavailable', 'One or more products in this bundle are out of stock.'); return; }
    setCart(prev => {
      const next = [...prev];
      for (const product of products) {
        const found = next.find(line => line.productId === product.id);
        if (found) found.quantity += 1;
        else next.push({ productId: product.id, quantity: 1, discount: deal.discount });
      }
      return next;
    });
    setTab('Register');
    Alert.alert('Bundle added', `${deal.title} added with ${deal.discount}% item discount.`);
  };
  const sendMessage = (text = query) => {
    const clean = text.trim(); if (!clean) return;
    const q = clean.toLowerCase();
    let selected = deals;
    if (q.includes('coffee') || q.includes('chai') || q.includes('beverage') || q.includes('tea')) selected = deals.filter(d => /coffee|beverage/i.test(`${d.title} ${d.category}`));
    else if (q.includes('tech') || q.includes('headphone') || q.includes('electronic')) selected = deals.filter(d => /tech|electronic/i.test(`${d.title} ${d.category}`));
    else if (q.includes('snack') || q.includes('chocolate') || q.includes('food')) selected = deals.filter(d => /snack|chocolate/i.test(`${d.title} ${d.category}`));
    else if (q.includes('health') || q.includes('wellness') || q.includes('vitamin')) selected = deals.filter(d => /wellness/i.test(`${d.title} ${d.category}`));
    else if (q.includes('clearance') || q.includes('expired') || q.includes('expiry')) {
      const near = store.products.filter(p => Number.isFinite(daysUntil(p.expiry)) && daysUntil(p.expiry) <= 30);
      setMessages(prev => [...prev, { user: true, text: clean }, { user: false, text: near.length ? `⚡ Clearance watch: ${near.map(p => `${p.name} (${p.stock} in stock, expires ${p.expiry})`).join(' · ')}. Manager se markdown approve karwa kar sale mein add karein.` : '✨ Koi item aglay 30 din mein expire nahi ho raha.' }]);
      setQuery(''); return;
    }
    const textReply = selected.length ? `✨ ${selected.length === deals.length ? 'Yeh active bundle deals abhi available hain:' : 'Aapke sawal ke liye yeh deals best hain:'} ${selected.map(d => `\n• ${d.title}: ${displayMoney(d.price)} (save ${d.discount}%)`).join('')}\n\n${selected[0].pitch}` : 'Aaj ke liye matching bundle nahi mila. Products tab mein catalog check karein.';
    setMessages(prev => [...prev, { user: true, text: clean }, { user: false, text: textReply, dealIds: selected.map(d => d.id) }]); setQuery('');
  };

  const teamPeers = store.users.filter(person => person.id !== user?.id && (['ADMIN','SALES_MANAGER'].includes(user?.role||'') || ['ADMIN','SALES_MANAGER'].includes(person.role||'STAFF')));
  const currentPeer = chatRecipient==='*' ? null : teamPeers.find(person=>person.id===chatRecipient) || teamPeers[0];
  const sendTeamMessage = (kind:ChatMessage['kind']='text',text=chatDraft,uri?:string,fileName?:string) => {
    if (!user || (!text.trim()&&!uri)) return;
    const recipientId=chatRecipient==='*'?'*':(currentPeer?.id||'');
    if(!recipientId){Alert.alert('Choose a teammate','Select a message recipient first.');return;}
    const message:ChatMessage={id:uid(),senderId:user.id,senderName:user.name,senderRole:user.role||'STAFF',recipientId,recipientName:recipientId==='*'?'All staff':currentPeer?.name||'Team member',kind,text:text.trim(),uri,fileName,timestamp:new Date().toISOString()};
    setStore(prev=>({...prev,messages:[...(prev.messages||[]),message]}));
    log('TEAM MESSAGE',`${kind.toUpperCase()} sent to ${message.recipientName}`);
    setChatDraft('');
  };
  const pickTeamFile = async (imagesOnly=false) => {
    if(imagesOnly){const result=await ImagePicker.launchImageLibraryAsync({mediaTypes:['images'],quality:0.7});if(!result.canceled&&result.assets[0]){const uri=await persistPhoto(result.assets[0]);if(uri)sendTeamMessage('image','',uri,'Image');}return;}
    const result=await DocumentPicker.getDocumentAsync({copyToCacheDirectory:true});
    if(result.canceled||!result.assets[0])return;
    const asset=result.assets[0];if((asset.size||0)>imageLimit){Alert.alert('File too large','Choose a chat attachment under 20 MB.');return;}
    try{const folder=new Directory(Paths.document,'pos-chat-files');if(!folder.exists)folder.create({intermediates:true,idempotent:true});const file=new File(asset.uri);const destination=new File(folder,`${uid()}-${asset.name.replace(/[^a-zA-Z0-9._-]/g,'_')}`);await file.copy(destination);sendTeamMessage('file','',destination.uri,asset.name);}catch(error){Alert.alert('Could not attach file',String(error));}
  };
  const recordVoiceMessage = async () => {
    try{
      if(!isRecording){const permission=await AudioModule.requestRecordingPermissionsAsync();if(!permission.granted){Alert.alert('Microphone permission needed','Allow microphone access to record a voice message.');return;}await setAudioModeAsync({allowsRecording:true,playsInSilentMode:true});await audioRecorder.prepareToRecordAsync();audioRecorder.record();setIsRecording(true);return;}
      await audioRecorder.stop();setIsRecording(false);if(audioRecorder.uri)sendTeamMessage('voice','',audioRecorder.uri,'Voice message');
    }catch(error){setIsRecording(false);Alert.alert('Voice message error',String(error));}
  };

  // Checkout commits the receipt, stock changes and audit evidence as one store update.
  const finishSale = () => {
    if (!cartRows.length) { Alert.alert('Cart is empty', 'Add products before checkout.'); return; }
    const paid = payment === 'CASH' ? enteredToUsd(Number(tender || toSelectedCurrency(total))) : total;
    if (paid < total) { Alert.alert('Payment incomplete', 'Cash tender cannot be less than the total.'); return; }
    if (cartRows.some(({ product, line }) => product.stock < line.quantity)) { Alert.alert('Stock changed', 'Please review the cart: one or more products no longer have enough stock.'); setModal(null); return; }
    const now = new Date().toISOString();
    const saleLines: SaleLine[] = cartRows.map(({ product, line }) => ({ name: product.name, sku: product.sku, quantity: line.quantity, price: product.price * (1 - line.discount / 100) }));
    const stockAfter = Object.fromEntries(store.products.map(product => [product.sku, product.stock - (cart.find(item => item.productId === product.id)?.quantity || 0)]));
    const sale: Sale = { id: uid(), invoice: `POS-${10483 + store.sales.length}`, timestamp: now, customer, cashier: user?.name || 'Staff', evidence: { recordedBy: user?.name || 'Staff', recordedAt: now, paymentReference: payment === 'CASH' ? undefined : `LOCAL-${uid()}`, stockAfter, customerSnapshot: (() => { const person = store.customers.find(x => x.name === customer); return person ? { name: person.name, phone: person.phone, email: person.email, address: person.address, photoUri: person.photoUri } : undefined; })() }, subtotal: moneyRound(subtotal), discount: moneyRound(itemDiscount + cartDiscountAmount), tax: moneyRound(taxTotal), total: moneyRound(total), paid, change: Math.max(0, moneyRound(paid - total)), method: payment, status: 'COMPLETED', items: saleLines };
    setStore(prev => {
      const products = prev.products.map(product => {
        const line = cart.find(x => x.productId === product.id);
        if (!line) return product;
        const nextStock = product.stock - line.quantity;
        return { ...product, stock: nextStock };
      });
      const movements: Movement[] = cartRows.map(({ product, line }) => ({ id: uid(), product: product.name, delta: -line.quantity, reason: `Sale ${sale.invoice}`, by: user?.name || 'Staff', timestamp: now }));
      const syncEvent: SyncEvent = { id: uid(), type: 'SALE_COMPLETED', createdAt: now, payload: sale };
      return { ...prev, products, sales: [sale, ...prev.sales], syncQueue: [...prev.syncQueue, syncEvent], movements: [...movements, ...prev.movements], notices: products.filter(p => p.stock <= p.minimum).map(p => ({ id: uid(), title: `Low stock: ${p.name}`, body: `${p.stock} remaining · Minimum stock is ${p.minimum}`, read: false, timestamp: now })).concat(prev.notices).slice(0, 25) };
    });
    log('NEW SALE', `${sale.invoice} · ${displayMoney(sale.total)} · ${payment}`);
    setReceipt(sale); setCart([]); setCartDiscount(0); setCustomer('Walk-in Customer'); setModal('receipt');
  };
  const refundSale = (sale: Sale) => Alert.alert('Refund sale?', `Refund ${sale.invoice} for ${displayMoney(sale.total)} and restock the sold items?`, [
    { text: 'Cancel', style: 'cancel' }, { text: 'Refund', style: 'destructive', onPress: () => {
      const timestamp = new Date().toISOString();
      setStore(prev => {
        const products = prev.products.map(product => ({ ...product, stock: product.stock + (sale.items.find(item => item.sku === product.sku)?.quantity || 0) }));
        return { ...prev, products, sales: prev.sales.map(item => item.id === sale.id ? { ...item, status: 'REFUNDED' } : item), movements: [...sale.items.map(item => ({ id: uid(), product: item.name, delta: item.quantity, reason: `Refund ${sale.invoice}`, by: user?.name || 'Staff', timestamp })), ...prev.movements] };
      });
      log('REFUND', `${sale.invoice} · ${displayMoney(sale.total)}`);
    } },
  ]);

  const openForm = (kind: typeof formKind, item?: Product | Person) => {
    setFormKind(kind); setEditingId(item?.id || null);
    if (item && kind === 'product') {
      const product = item as Product; setForm({ name: product.name, sku: product.sku, category: product.category, brand: product.brand, cost: String(toSelectedCurrency(product.cost)), price: String(toSelectedCurrency(product.price)), tax: String(product.tax), stock: String(product.stock), minimum: String(product.minimum), expiry: product.expiry || '', batch: product.batch, imageUri: product.imageUri || '' });
    } else if (item && kind === 'staff') {
      const person = item as Person; const countryCode=person.countryCode||'+92'; const rawPhone=(person.phone||'').startsWith(countryCode)?(person.phone||'').slice(countryCode.length).trim():person.phone||''; setForm({ name: person.name, username: person.username || '', email: person.email || '', phone: rawPhone, countryCode, countryISO:countryCode==='+1'?'US':countryCode==='+44'?'GB':'PK', dateOfBirth: person.dateOfBirth || '', address: person.address || '', role: person.role || 'STAFF', photoUri: person.photoUri || '', password: '', confirmPassword: '', pin:'', confirmPin:'' });
    } else if (item && (kind === 'customer' || kind === 'supplier')) {
      const person = item as Person; setForm({ name: person.name, phone: person.phone, email: person.email, address: person.address, photoUri: person.photoUri || '', dateOfBirth: person.dateOfBirth || '' });
    } else setForm(kind === 'product' ? { name: '', sku: '', category: 'Beverages', brand: '', cost: '', price: '', tax: '8', stock: '0', minimum: '5', expiry: '', batch: '', imageUri: '' } : kind === 'purchase' ? { supplier: store.suppliers[0]?.name || '', sku: store.products[0]?.sku || '', quantity: '10', cost: String(toSelectedCurrency(store.products[0]?.cost || 0)), status: 'RECEIVED' } : kind === 'staff' ? { name: '', username: '', email: '', phone: '', countryCode: '+92', countryISO:'PK', dateOfBirth: '', address: '', role: 'STAFF', photoUri: '', password: '', confirmPassword: '', pin:'', confirmPin:'' } : kind === 'reset' ? { password: '', confirmPassword: '' } : { name: '', phone: '', email: '', address: '', photoUri: '', dateOfBirth: '' });
    setModal('form');
  };
  const saveForm = async () => {
    if (formKind === 'product') {
      if (!form.name?.trim() || !form.sku?.trim() || !Number(form.price)) { Alert.alert('Required fields', 'Product name, SKU and price are required.'); return; }
      if (!editingId && !form.imageUri) { Alert.alert('Product photo required', 'Add a photo before saving a new catalog item.'); return; }
      const product: Product = { id: editingId || uid(), name: form.name.trim(), sku: form.sku.trim(), category: form.category || 'Other', brand: form.brand || '—', cost: enteredToUsd(Number(form.cost) || 0), price: enteredToUsd(Number(form.price)), tax: Number(form.tax) || 0, stock: Number(form.stock) || 0, minimum: Number(form.minimum) || 0, expiry: form.expiry || undefined, batch: form.batch || '—', imageUri: form.imageUri || undefined };
      setStore(prev => ({ ...prev, products: editingId ? prev.products.map(p => p.id === editingId ? product : p) : [product, ...prev.products] })); log(editingId ? 'EDIT PRODUCT' : 'ADD PRODUCT', product.name); setModal(null); return;
    }
    if (formKind === 'purchase') {
      const product = store.products.find(p => p.sku === form.sku) || store.products[0]; const quantity = Number(form.quantity) || 1; const cost = enteredToUsd(Number(form.cost)) || product.cost;
      const purchase: Purchase = { id: uid(), number: `PO-${String(243 + store.purchases.length).padStart(5, '0')}`, supplier: form.supplier || 'Supplier', timestamp: new Date().toISOString(), total: moneyRound(quantity * cost), status: form.status === 'PENDING' ? 'PENDING' : 'RECEIVED', items: [{ productId: product.id, quantity, cost }] };
      setStore(prev => ({ ...prev, purchases: [purchase, ...prev.purchases], products: purchase.status === 'RECEIVED' ? prev.products.map(p => p.id === product.id ? { ...p, stock: p.stock + quantity, cost } : p) : prev.products, movements: purchase.status === 'RECEIVED' ? [{ id: uid(), product: product.name, delta: quantity, reason: `Purchase ${purchase.number}`, by: user?.name || 'Staff', timestamp: purchase.timestamp }, ...prev.movements] : prev.movements })); log('NEW PURCHASE', `${purchase.number} · ${purchase.status}`); setModal(null); return;
    }
    if (formKind === 'reset') {
      if ((form.password || '').length < 8) { Alert.alert('Password too short', 'Choose a password with at least 8 characters.'); return; }
      if (form.password !== form.confirmPassword) { Alert.alert('Passwords do not match', 'Enter the same password in both fields.'); return; }
      const target = store.users.find(person => person.id === editingId);
      if (!target) return;
      const salt = Crypto.randomUUID();
      const passwordHash = await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, `${salt}:${form.password}`);
      const completedAt = new Date().toISOString();
      setStore(prev => ({ ...prev, users: prev.users.map(person => person.id === target.id ? { ...person, passwordHash, passwordSalt: salt, pin: undefined, recoveryRequestedAt: undefined } : person), passwordResetRequests: prev.passwordResetRequests.map(request => request.userId === target.id && !request.completedAt ? { ...request, completedAt } : request) }));
      log('PASSWORD RESET', `Admin reset sign-in for ${target.name}`); setModal(null); Alert.alert('Password updated', `${target.name} can now sign in with the new password.`); return;
    }
    if (!form.name?.trim()) { Alert.alert('Name required', 'Please enter a name.'); return; }
    if ((formKind === 'customer' || formKind === 'supplier') && !form.photoUri) { Alert.alert('Profile photo required', 'Choose a photo for this contact before saving.'); return; }
    if (formKind === 'staff') {
      if (!form.username?.trim() || !form.email?.trim()) { Alert.alert('Username and email required', 'Enter a login username and email address.'); return; }
      if (!form.photoUri) { Alert.alert('Profile photo required', 'Choose a photo for this staff profile.'); return; }
      if (!editingId && (form.password || '').length < 8) { Alert.alert('Password required', 'New team members need a password of at least 8 characters.'); return; }
      if (form.password && form.password !== form.confirmPassword) { Alert.alert('Passwords do not match', 'Enter the same password in both fields.'); return; }
      if (!editingId && !/^\d{4,6}$/.test(form.pin||'')) { Alert.alert('PIN required','Set a 4 to 6 digit PIN for this account.'); return; }
      if (form.pin && !/^\d{4,6}$/.test(form.pin)) { Alert.alert('Invalid PIN','PIN must contain 4 to 6 digits.'); return; }
      if (form.pin && form.pin !== form.confirmPin) { Alert.alert('PINs do not match','Enter the same PIN in both fields.'); return; }
      const existing = store.users.find(person => person.id === editingId);
      const salt = form.password ? Crypto.randomUUID() : existing?.passwordSalt;
      const passwordHash = form.password && salt ? await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, `${salt}:${form.password}`) : existing?.passwordHash;
      const pinSalt = form.pin ? Crypto.randomUUID() : existing?.pinSalt;
      const pinHash = form.pin && pinSalt ? await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, `${pinSalt}:${form.pin}`) : existing?.pinHash;
      const countryCode = form.countryCode || '+92';
      const phone = `${countryCode} ${form.phone || ''}`.trim();
      const person: Person = { id: editingId || uid(), name: form.name.trim(), username: form.username.trim(), email: form.email.trim().toLowerCase(), phone, countryCode, dateOfBirth: form.dateOfBirth || undefined, address: form.address || '', role: (roles.includes(form.role as Role) ? form.role : 'STAFF') as Role, active: true, totalSpent: existing?.totalSpent || 0, photoUri: form.photoUri || undefined, passwordHash, passwordSalt: salt, pinHash, pinSalt, pin: undefined };
      setStore(prev => ({ ...prev, users: editingId ? prev.users.map(p => p.id === editingId ? person : p) : [person, ...prev.users] })); if (editingId === user?.id) setUser(person); log(editingId ? 'EDIT USER' : 'ADD USER', `${person.name} (${person.role})`); setModal(null); return;
    }
    const oldPerson = (formKind === 'customer' ? store.customers : store.suppliers).find(item => item.id === editingId);
    const person: Person = { id: editingId || uid(), name: form.name, phone: form.phone || '', email: form.email || '', address: form.address || '', photoUri: form.photoUri || undefined, dateOfBirth: form.dateOfBirth || undefined, totalSpent: oldPerson?.totalSpent || 0 };
    if (formKind === 'customer') setStore(prev => ({ ...prev, customers: editingId ? prev.customers.map(p => p.id === editingId ? person : p) : [person, ...prev.customers] }));
    else setStore(prev => ({ ...prev, suppliers: editingId ? prev.suppliers.map(p => p.id === editingId ? person : p) : [person, ...prev.suppliers] }));
    log(`ADD ${formKind.toUpperCase()}`, person.name); setModal(null);
  };
  const requestPasswordReset = () => {
    const query = recoveryEmail.trim().toLowerCase();
    const target = store.users.find(person => person.email?.toLowerCase() === query || person.username?.toLowerCase() === query);
    if (!target) { Alert.alert('Request received', 'If this account exists, an administrator can review the reset request on this device.'); setModal(null); return; }
    const request: PasswordResetRequest = { id: uid(), userId: target.id, username: target.username || '', email: target.email || '', createdAt: new Date().toISOString() };
    setStore(prev => ({ ...prev, users: prev.users.map(person => person.id === target.id ? { ...person, recoveryRequestedAt: request.createdAt } : person), passwordResetRequests: [request, ...prev.passwordResetRequests] }));
    setModal(null); setRecoveryEmail(''); Alert.alert('Request sent', 'An administrator must approve and set a new password on this device.');
  };
  const pickImage = async (target: 'imageUri' | 'photoUri') => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) { Alert.alert('Photo permission required', 'Allow photo library access to choose an image.'); return; }
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: true, quality: 0.55 });
    if (!result.canceled && result.assets[0]) { const photoUri=await persistPhoto(result.assets[0]); if(photoUri)setField(target,photoUri); }
  };
  const exportReceiptPdf = async (sale: Sale | null) => {
    if (!sale) return;
    const escape = (value: string) => value.replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char] || char));
    const rows = sale.items.map(item => `<tr><td>${item.quantity} × ${escape(item.name)}</td><td>${displayMoney(item.price * item.quantity)}</td></tr>`).join('');
    const html = `<html><body style="font-family:Arial;padding:24px;color:#0f172a"><header style="display:flex;align-items:center;gap:12px;border-bottom:3px solid #1d4ed8;padding-bottom:12px"><div style="width:48px;height:48px;border-radius:14px;background:#1d4ed8;display:flex;align-items:center;justify-content:center"><svg width="44" height="44" viewBox="0 0 64 64"><rect x="7" y="7" width="50" height="38" rx="8" fill="none" stroke="white" stroke-width="5"/><path d="M20 31l8 8 17-19" fill="none" stroke="#5EEAD4" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/><path d="M15 45v9h34v-9" fill="none" stroke="white" stroke-width="5" stroke-linejoin="round"/></svg></div><div><h1 style="margin:0;color:#1d4ed8;font-style:italic">Retail POS</h1><small>Sales receipt &amp; transaction record</small></div></header><p>${escape(sale.invoice)} · ${escape(fmtDate(sale.timestamp))}</p><p>Customer: ${escape(sale.customer)} · Cashier: ${escape(sale.cashier)}</p>${sale.evidence?.customerSnapshot ? `<p>Customer contact: ${escape(sale.evidence.customerSnapshot.phone)} · ${escape(sale.evidence.customerSnapshot.email)} · ${escape(sale.evidence.customerSnapshot.address)}</p>` : ''}<table style="width:100%;border-collapse:collapse" cellpadding="8">${rows}</table><hr/><h2>Total ${displayMoney(sale.total)}</h2><p>${escape(sale.method)} · Paid ${displayMoney(sale.paid)} · Change ${displayMoney(sale.change)}</p><hr/><small>Recorded by ${escape(sale.evidence?.recordedBy || sale.cashier)} at ${escape(fmtDate(sale.evidence?.recordedAt || sale.timestamp))}<br/>Transaction: ${escape(sale.id)} · Status: ${escape(sale.status)}</small><p style="font-size:10px;color:#64748b">Receipt and inventory details recorded locally on this device. Photo evidence attached: ${sale.evidence?.photoUri ? 'Yes (image remains stored on device)' : 'No'}.</p></body></html>`;
    const file = await Print.printToFileAsync({ html });
    if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(file.uri, { mimeType: 'application/pdf', dialogTitle: `Receipt ${sale.invoice}` });
    else Alert.alert('PDF ready', file.uri);
  };
  const attachReceiptEvidence = async () => {
    if (!receipt) return;
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) { Alert.alert('Photo permission required', 'Allow photo library access to attach receipt evidence.'); return; }
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: true, quality: 0.55 });
    if (result.canceled || !result.assets[0]) return;
    const photoUri = await persistPhoto(result.assets[0]);
    if (!photoUri) return;
    const updated = { ...receipt, evidence: { ...receipt.evidence, recordedBy: receipt.evidence?.recordedBy || receipt.cashier, recordedAt: receipt.evidence?.recordedAt || receipt.timestamp, stockAfter: receipt.evidence?.stockAfter || {}, photoUri } };
    setReceipt(updated); setStore(prev => ({ ...prev, sales: prev.sales.map(sale => sale.id === updated.id ? updated : sale) }));
    log('RECEIPT EVIDENCE ADDED', `${receipt.invoice} · photo attached`);
  };
  const scanBarcode = async (data: string) => {
    if (barcodeScanned) return;
    setBarcodeScanned(true); setModal(null);
    const product = store.products.find(item => item.sku.toLowerCase() === data.trim().toLowerCase());
    if (product) { setSearch(''); setCategory('All'); addLine(product); }
    else { setSearch(data); Alert.alert('Barcode not found', `${data} is not in the product catalog. Add it as a product SKU to scan it next time.`); }
    setTimeout(() => setBarcodeScanned(false), 800);
  };
  const receivePurchase = (purchase: Purchase) => {
    setStore(prev => ({ ...prev, purchases: prev.purchases.map(p => p.id === purchase.id ? { ...p, status: 'RECEIVED' } : p), products: prev.products.map(product => { const item = purchase.items.find(i => i.productId === product.id); return item ? { ...product, stock: product.stock + item.quantity, cost: item.cost } : product; }), movements: [...purchase.items.map(i => ({ id: uid(), product: store.products.find(p => p.id === i.productId)?.name || 'Product', delta: i.quantity, reason: `Purchase ${purchase.number}`, by: user?.name || 'Staff', timestamp: new Date().toISOString() })), ...prev.movements] })); log('PURCHASE RECEIVED', purchase.number);
  };
  const stockAdjust = (product: Product, delta: number) => {
    const reason = delta < 0 ? 'Manual write-off / disposal' : 'Manual stock adjustment';
    const timestamp = new Date().toISOString(); const event: SyncEvent = { id: uid(), type: 'STOCK_ADJUSTED', createdAt: timestamp, payload: { sku: product.sku, delta, reason, by: user?.name } }; setStore(prev => ({ ...prev, syncQueue: [...prev.syncQueue, event], products: prev.products.map(p => p.id === product.id ? { ...p, stock: Math.max(0, p.stock + delta) } : p), movements: [{ id: uid(), product: product.name, delta, reason, by: user?.name || 'Staff', timestamp }, ...prev.movements] })); log('STOCK ADJUSTMENT', `${product.name} · ${delta > 0 ? '+' : ''}${delta}`);
  };

  if (!user) return <View style={[s.root, { backgroundColor: darkMode ? c.bg : '#EAF2FF' }]}><StatusBar style={darkMode ? 'light' : 'dark'} /><AuthScreen c={c} s={s} Icon={Icon} Chip={Chip} divider={divider} loginName={loginName} loginPin={loginPin} setLoginName={setLoginName} setLoginPin={setLoginPin} error={loginError} onLogin={authenticate} dark={dark} setDark={setDark} role={loginRole} setRole={setLoginRole} loginMode={loginMode} setLoginMode={setLoginMode} showPassword={showPassword} setShowPassword={setShowPassword} visibleFields={visibleFields} setVisibleFields={setVisibleFields} onForgot={() => setModal('forgot')} signupOpen={signupOpen} setSignupOpen={setSignupOpen} signup={signupForm} setSignup={setSignupForm} onSignup={createSignup} onPickSignupPhoto={pickSignupPhoto} showSignupCalendar={showSignupCalendar} setShowSignupCalendar={setShowSignupCalendar} />
    <Modal visible={modal === 'forgot'} transparent animationType="slide" onRequestClose={() => setModal(null)}><View style={s.overlay}><Pressable style={s.backdrop} onPress={() => setModal(null)} /><View style={[s.sheet, { backgroundColor: c.surface }]}><View style={s.sheetHandle} /><Text style={[s.sectionTitle, { color: c.text }]}>Forgot password</Text><Text style={[s.subText, { color: c.muted, marginVertical: 10 }]}>Enter your account email or username. An administrator must approve the reset on this device.</Text><TextInput value={recoveryEmail} onChangeText={setRecoveryEmail} autoCapitalize="none" placeholder="Email or username" placeholderTextColor={c.muted} style={[s.input, { backgroundColor: c.bg, borderColor: c.border, color: c.text }]} /><Pressable onPress={requestPasswordReset} style={[s.submitButton, { backgroundColor: c.primary, marginTop: 14 }]}><Text style={s.submitText}>Request password reset</Text></Pressable></View></View></Modal></View>;

  const headerTitle = tab === 'Register' ? `${(user.role || 'STAFF').replaceAll('_', ' ')} Register` : tab;
  const filteredProducts = store.products.filter(p => (category === 'All' || p.category === category) && `${p.name} ${p.sku} ${p.brand}`.toLowerCase().includes(search.toLowerCase()));
  const lowStock = store.products.filter(p => p.stock <= p.minimum);
  const todayStart = new Date(); todayStart.setHours(0, 0, 0, 0);
  const todaySales = store.sales.filter(sale => new Date(sale.timestamp) >= todayStart && sale.status !== 'REFUNDED');
  const todayRevenue = todaySales.reduce((sum, sale) => sum + sale.total, 0);
  const todayCount = todaySales.length;
  const unread = unreadCount;

  const setField = (key: string, value: string) => setForm(current => ({ ...current, [key]: value }));
  const field = (label: string, key: string, keyboard: 'default' | 'numeric' = 'default', placeholder = '') => <View style={{ marginBottom: 12 }} key={key}><Text style={[s.fieldLabel, { color: c.muted }]}>{label}</Text><TextInput value={form[key] || ''} onChangeText={value => setField(key, value)} keyboardType={keyboard === 'numeric' ? 'decimal-pad' : 'default'} placeholder={placeholder || label} placeholderTextColor={c.muted} style={[s.input, { backgroundColor: c.bg, borderColor: c.border, color: c.text }]} /></View>;
  const showForm = (kind: typeof formKind, item?: Product | Person) => openForm(kind, item);
  const removeProduct = (product: Product) => { setStore(prev => ({ ...prev, products: prev.products.filter(item => item.id !== product.id) })); setCart(prev => prev.filter(item => item.productId !== product.id)); log('DELETE PRODUCT', product.name); };



  const openSecuritySettings = () => { setSecurityPin(''); setSecurityPinConfirm(''); setSecurityPassword(''); setSecurityPasswordConfirm(''); setModal('security'); };
  const content = tab === 'Dashboard' ? <DashboardScreen c={c} s={s} user={user} store={store} unread={unread} todayRevenue={todayRevenue} todayCount={todayCount} todaySales={todaySales} lowStock={lowStock} Icon={Icon} Metric={Metric} SaleRow={SaleRow} ProductRow={ProductRow} displayMoney={displayMoney} blue={blue} setTab={setTab} setModal={setModal} setInventoryFilter={setInventoryFilter} showForm={showForm} stockAdjust={stockAdjust} /> : tab === 'Register' ? <RegisterScreen c={c} s={s} blue={blue} store={store} customer={customer} setCustomer={setCustomer} search={search} setSearch={setSearch} cameraPermission={cameraPermission} requestCameraPermission={requestCameraPermission} setBarcodeScanned={setBarcodeScanned} setModal={setModal} category={category} setCategory={setCategory} cart={cart} addLine={addLine} filteredProducts={filteredProducts} displayMoney={displayMoney} Icon={Icon} SearchBox={SearchBox} Chip={Chip} /> : tab === 'AI Deals' ? <AIDealsScreen c={c} s={s} messages={messages} Icon={Icon} Chip={Chip} displayMoney={displayMoney} sendMessage={sendMessage} addToCartFromDeal={addToCartFromDeal} /> : tab === 'Products' ? <ProductsScreen c={c} s={s} store={store} search={search} setSearch={setSearch} category={category} setCategory={setCategory} filteredProducts={filteredProducts} Icon={Icon} SearchBox={SearchBox} Chip={Chip} ProductRow={ProductRow} displayMoney={displayMoney} addLine={addLine} showForm={showForm} removeProduct={removeProduct} /> : tab === 'Inventory' ? <InventoryScreen c={c} s={s} store={store} lowStock={lowStock} inventoryFilter={inventoryFilter} setInventoryFilter={setInventoryFilter} Icon={Icon} Chip={Chip} ProductRow={ProductRow} Empty={Empty} stockAdjust={stockAdjust} /> : tab === 'Orders' ? <OrdersScreen c={c} s={s} user={user} sales={store.sales} canViewMyReports={canViewMyReports} Icon={Icon} SaleRow={SaleRow} displayMoney={displayMoney} refundSale={refundSale} /> : tab === 'Purchases' ? <PurchasesScreen c={c} s={s} store={store} Icon={Icon} Badge={Badge} displayMoney={displayMoney} showForm={showForm} receivePurchase={receivePurchase} /> : tab === 'Customers' ? <CustomersScreen c={c} s={s} store={store} peopleTab={peopleTab} setPeopleTab={setPeopleTab} showForm={showForm} Icon={Icon} displayMoney={displayMoney} ageFromDOB={ageFromDOB} blue={blue} green={green} /> : tab === 'Reports' ? <ReportsScreen c={c} s={s} store={store} user={user} canViewMyReports={canViewMyReports} reportRange={reportRange} setReportRange={setReportRange} Chip={Chip} Metric={Metric} ProgressLine={ProgressLine} ProductRow={ProductRow} displayMoney={displayMoney} green={green} amber={amber} /> : tab === 'Team Chat' ? <TeamChatScreen c={c} s={s} user={user} store={store} chatRecipient={chatRecipient} setChatRecipient={setChatRecipient} currentPeer={currentPeer} teamPeers={teamPeers} chatScrollRef={chatScrollRef} chatDraft={chatDraft} setChatDraft={setChatDraft} pickTeamFile={pickTeamFile} isRecording={isRecording} recordVoiceMessage={recordVoiceMessage} sendTeamMessage={sendTeamMessage} Icon={Icon} Chip={Chip} AudioMessage={AudioMessage} Empty={Empty} /> : tab === 'My Profile' ? <ProfileScreen c={c} s={s} user={user} Icon={Icon} ageFromDOB={ageFromDOB} showForm={showForm} openSecuritySettings={openSecuritySettings} /> : tab === 'Admin Panel' ? <AdminScreen c={c} s={s} user={user} store={store} currency={currency} setCurrency={setCurrency} log={log} Chip={Chip} Metric={Metric} Empty={Empty} exchangeRateInput={exchangeRateInput} setExchangeRateInput={setExchangeRateInput} setExchangeRate={setExchangeRate} displayMoney={displayMoney} setTab={setTab} /> : <StaffScreen c={c} s={s} user={user} store={store} blue={blue} Icon={Icon} Badge={Badge} Empty={Empty} showForm={showForm} setEditingId={setEditingId} />;
  const formFields: [string, string, string?][] = formKind === 'product' ? [['Product name', 'name'], ['SKU / Barcode', 'sku'], ['Category', 'category'], ['Brand', 'brand'], [`Cost price (${currency})`, 'cost', 'numeric'], [`Retail price (${currency})`, 'price', 'numeric'], ['Tax %', 'tax', 'numeric'], ['Stock on hand', 'stock', 'numeric'], ['Minimum stock alert', 'minimum', 'numeric'], ['Expiry date (YYYY-MM-DD)', 'expiry'], ['Batch number', 'batch']] : formKind === 'purchase' ? [['Supplier', 'supplier'], ['Product SKU', 'sku'], ['Quantity', 'quantity', 'numeric'], ['Unit cost', 'cost', 'numeric']] : formKind === 'staff' ? [['Full name', 'name'], ['Username', 'username'], ['Email', 'email'], ['Phone number', 'phone', 'numeric'], ['Date of birth (YYYY-MM-DD)', 'dateOfBirth'], ['Address', 'address']] : formKind === 'reset' ? [] : [['Full name', 'name'], ['Phone', 'phone'], ['Email', 'email'], ['Date of birth (YYYY-MM-DD)', 'dateOfBirth'], ['Address', 'address']];
  const hideMenu = () => setModal(null);

  return <View style={[s.root, { backgroundColor: c.bg }]}><StatusBar style={darkMode ? 'light' : 'dark'} />
    <View style={[s.topBar, { backgroundColor: c.surface, borderColor: c.border }]}><Pressable onPress={() => setModal('menu')} style={[s.menuBtn, { backgroundColor: c.primarySoft }]}><Icon c={c} name="storefront-outline" color={c.primary} size={22} /></Pressable><View style={{ flex: 1 }}><Text style={[s.topTitle, { color: c.text }]}>{headerTitle}</Text><Text style={[s.topSubtitle, { color: c.muted }]}>{(user.role || 'STAFF').replaceAll('_', ' ')} · {user.name} · {sessionId&&appForeground ? 'Online' : 'Offline'}</Text></View>{user.photoUri ? <Image source={{uri:user.photoUri}} style={{width:34,height:34,borderRadius:18}} /> : <Pressable onPress={() => setTab('My Profile')} style={[s.iconBtnSmall,{backgroundColor:c.primarySoft}]}><Icon c={c} name='account-outline' color={c.primary} size={18} /></Pressable>}<Pressable onPress={() => setModal('notifications')} style={[s.iconBtnSmall, { backgroundColor: c.bg }]}><Icon c={c} name="bell-outline" size={19} /><View style={[s.smallDot, { backgroundColor: c.danger }]} /></Pressable><Pressable onPress={() => setDark(!dark)} style={[s.iconBtnSmall, { backgroundColor: c.bg }]}><Icon c={c} name={dark ? 'weather-sunny' : 'weather-night'} size={18} /></Pressable></View>
    {tab==='Team Chat'?<KeyboardAvoidingView style={{flex:1}} behavior={Platform.OS==='ios'?'padding':'height'} keyboardVerticalOffset={Platform.OS==='ios'?80:0}><View style={{flex:1,paddingHorizontal:14,paddingBottom:4}}>{<TeamChatScreen c={c} s={s} user={user} store={store} chatRecipient={chatRecipient} setChatRecipient={setChatRecipient} currentPeer={currentPeer} teamPeers={teamPeers} chatScrollRef={chatScrollRef} chatDraft={chatDraft} setChatDraft={setChatDraft} pickTeamFile={pickTeamFile} isRecording={isRecording} recordVoiceMessage={recordVoiceMessage} sendTeamMessage={sendTeamMessage} Icon={Icon} Chip={Chip} AudioMessage={AudioMessage} Empty={Empty} />}</View></KeyboardAvoidingView>:<ScrollView keyboardShouldPersistTaps="handled" automaticallyAdjustKeyboardInsets contentContainerStyle={s.scrollContent} showsVerticalScrollIndicator={false}>{tab !== 'Dashboard' && <View style={s.pageHeading}><Text style={[s.pageTitle, { color: c.text }]}>{tab}</Text><Text style={[s.subText, { color: c.muted }]}>{tab === 'Register' ? 'Search, scan and add items to this sale.' : tab === 'Inventory' ? 'Stock levels, expiry dates and adjustments.' : tab === 'AI Deals' ? 'Offers finder for your sales team.' : tab === 'Products' ? 'Catalog, prices and product details.' : tab === 'Orders' ? 'Receipts, returns and transaction history.' : tab === 'Purchases' ? 'Supplier orders and stock receiving.' : tab === 'Customers' ? 'Loyalty and supplier directory.' : tab === 'Reports' ? 'Store performance and sales analytics.' : 'People, permissions and activity history.'}</Text></View>}{content}</ScrollView>}
    <View style={[s.bottomNav, { backgroundColor: c.surface, borderColor: c.border }]}>{(['Dashboard', 'Register', 'Inventory', 'More'] as const).map(item => { const selected = item === 'More' ? !(['Dashboard', 'Register', 'Inventory'].includes(tab)) : tab === item; const icon: IconName = item === 'Dashboard' ? 'view-dashboard-outline' : item === 'Register' ? 'point-of-sale' : item === 'Inventory' ? 'package-variant-closed' : 'menu'; return <Pressable key={item} onPress={() => item === 'More' ? setModal('menu') : setTab(item)} style={s.navItem}><View style={[s.navIcon, selected && { backgroundColor: c.primarySoft }]}><Icon c={c} name={icon} size={21} color={selected ? c.primary : c.muted} /></View><Text style={[s.navLabel, { color: selected ? c.primary : c.muted }]}>{item}</Text></Pressable>; })}</View>

    <Modal visible={modal === 'menu'} transparent animationType="slide" onRequestClose={hideMenu}><View style={s.overlay}><Pressable style={s.backdrop} onPress={hideMenu} /><View style={[s.menuSheet, { backgroundColor: c.surface }]}><View style={s.sheetHandle} /><View style={s.menuSheetHead}><View><Text style={[s.sectionTitle, { color: c.text }]}>Retail POS</Text><Text style={[s.mini, { color: c.muted }]}>Store Terminal #104</Text></View><Pressable onPress={hideMenu}><Icon c={c} name="close" /></Pressable></View><View style={[s.profileStrip, { backgroundColor: c.primarySoft }]}><View style={[s.avatar, { backgroundColor: c.surface }]}><Text style={{ color: c.primary, fontWeight: '900' }}>{user.name.split(' ').map(n => n[0]).join('').slice(0, 2)}</Text></View><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{user.name}</Text><Text style={[s.mini, { color: c.muted }]}>{user.role} · Active cashier</Text></View><Pressable onPress={() => { hideMenu(); setDark(!dark); }}><Icon c={c} name={dark ? 'weather-sunny' : 'weather-night'} color={c.primary} /></Pressable></View><View style={s.moduleGrid}>{visibleModules.map(module => <Pressable key={module.name} onPress={() => { setTab(module.name); hideMenu(); }} style={[s.moduleCell, { borderColor: c.border, backgroundColor: tab === module.name ? c.primarySoft : c.bg }]}><Icon c={c} name={module.icon} color={tab === module.name ? c.primary : c.muted} size={21} /><Text style={[s.moduleText, { color: tab === module.name ? c.primary : c.text }]}>{module.name}</Text></Pressable>)}</View><Pressable onPress={goLogout} style={[s.logoutButton, { borderColor: '#FECACA' }]}><Icon c={c} name="logout" color={c.danger} /><Text style={{ color: c.danger, fontWeight: '800', marginLeft: 10 }}>Sign out / switch cashier</Text></Pressable></View></View></Modal>

    <Modal visible={modal === 'notifications'} transparent animationType="slide" onRequestClose={hideMenu}><View style={s.overlay}><Pressable style={s.backdrop} onPress={hideMenu} /><View style={[s.sheet, { backgroundColor: c.surface }]}><View style={s.sheetHandle} /><View style={s.sectionHead}><View><Text style={[s.sectionTitle, { color: c.text }]}>Notifications</Text><Text style={[s.mini, { color: c.muted }]}>{unread} unread alerts</Text></View><Pressable onPress={() => setStore(prev => ({ ...prev, notices: prev.notices.map(n => ({ ...n, read: true })) }))}><Text style={[s.link, { color: c.primary }]}>Mark all read</Text></Pressable></View>{store.notices.map(notice => <Pressable key={notice.id} onPress={() => setStore(prev => ({ ...prev, notices: prev.notices.map(n => n.id === notice.id ? { ...n, read: true } : n) }))} style={[s.noticeRow, { borderColor: c.border }]}><View style={[s.auditBullet, { backgroundColor: notice.read ? c.border : c.primary }]} /><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{notice.title}</Text><Text style={[s.mini, { color: c.muted }]}>{notice.body}</Text><Text style={[s.mini, { color: c.muted }]}>{fmtDate(notice.timestamp)}</Text></View></Pressable>)}</View></View></Modal>

    <Modal visible={modal === 'form'} transparent animationType="slide" onRequestClose={hideMenu}><View style={s.overlay}><Pressable style={s.backdrop} onPress={hideMenu} /><KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} keyboardVerticalOffset={Platform.OS==='ios'?20:0} style={{ width: '100%', justifyContent: 'flex-end' }}><View style={[s.sheet, { backgroundColor: c.surface }]}><View style={s.sheetHandle} /><View style={s.sectionHead}><Text style={[s.sectionTitle, { color: c.text }]}>{editingId ? 'Edit' : 'New'} {formKind === 'staff' ? 'team member' : formKind}</Text><Pressable onPress={hideMenu}><Icon c={c} name="close" /></Pressable></View><ScrollView style={{ maxHeight: 420 }} keyboardShouldPersistTaps="handled" automaticallyAdjustKeyboardInsets>{formFields.map(([label, key, keyboard]) => key==='phone'&&formKind==='staff'?<View key='phone' style={{marginBottom:12}}><Text style={[s.fieldLabel,{color:c.muted}]}>PHONE NUMBER</Text><View style={{flexDirection:'row',alignItems:'flex-end',gap:8}}><View><Text style={[s.fieldLabel,{color:c.muted}]}>COUNTRY</Text><CountryPicker countryCode={(form.countryISO||'PK') as CountryISO} withCallingCode withFlag withEmoji withFilter withAlphaFilter withCallingCodeButton containerButtonStyle={{height:46,minWidth:96,paddingHorizontal:8,justifyContent:'center',backgroundColor:c.bg,borderColor:c.border,borderWidth:1,borderRadius:11}} onSelect={item=>setForm(prev=>({...prev,countryISO:item.cca2,countryCode:`+${item.callingCode[0]||'1'}`}))}/></View><View style={{flex:1}}>{field(label,key,'numeric')}</View></View></View>:field(label, key, keyboard === 'numeric' ? 'numeric' : 'default'))}{(formKind === 'product' || formKind === 'staff') && <><Pressable onPress={() => pickImage(formKind === 'product' ? 'imageUri' : 'photoUri')} style={[s.secondaryButton, { borderColor: c.border }]}><Text style={[s.buttonLabel, { color: c.primary }]}>Choose {formKind === 'product' ? 'product image' : 'profile photo'}</Text></Pressable>{form[formKind === 'product' ? 'imageUri' : 'photoUri'] ? <Image source={{uri: form[formKind === 'product' ? 'imageUri' : 'photoUri']}} style={{width:72,height:72,borderRadius:14,marginVertical:8}} /> : null}</>}{(formKind === 'staff' || formKind === 'customer' || formKind === 'supplier') && form.dateOfBirth ? <Text style={[s.mini,{color:c.muted,marginBottom:8}]}>Age: {ageFromDOB(form.dateOfBirth)} years</Text> : null}{(formKind === 'staff' || formKind === 'customer' || formKind === 'supplier') && <><Pressable onPress={() => pickImage('photoUri')} style={[s.secondaryButton,{borderColor:c.border}]}><Text style={[s.buttonLabel,{color:c.primary}]}>Choose photo</Text></Pressable>{form.photoUri ? <Image source={{uri:form.photoUri}} style={{width:72,height:72,borderRadius:36,marginVertical:8}} /> : null}</>}{formKind === 'staff' && <><Text style={[s.fieldLabel,{color:c.muted,marginTop:8}]}>ROLE</Text><View style={s.chipsRow}>{roles.map(role => <Chip key={role} c={c} active={form.role === role} label={role} onPress={() => setField('role',role)} />)}</View>{['password','confirmPassword','pin','confirmPin'].map((key) => {const pinField=key.toLowerCase().includes('pin');const label=key==='password'?(editingId?'NEW PASSWORD (OPTIONAL)':'PASSWORD'):key==='confirmPassword'?'CONFIRM PASSWORD':key==='pin'?(editingId?'NEW PIN (OPTIONAL)':'SET 4–6 DIGIT PIN'):'CONFIRM PIN';const visibilityKey=`staff-${key}`;return <View key={key} style={{marginBottom:12}}><Text style={[s.fieldLabel,{color:c.muted}]}>{label}</Text><View style={{flexDirection:'row',alignItems:'center',gap:8}}><TextInput value={form[key]||''} onChangeText={v=>setField(key,v)} keyboardType={pinField?'number-pad':'default'} secureTextEntry={!visibleFields[visibilityKey]} autoCapitalize='none' placeholder={pinField?'4–6 digits':key==='confirmPassword'?'Confirm password':'At least 8 characters'} placeholderTextColor={c.muted} style={[s.input,{flex:1,backgroundColor:c.bg,borderColor:c.border,color:c.text}]} /><Pressable accessibilityLabel={visibleFields[visibilityKey]?'Hide '+label:'Show '+label} onPress={()=>setVisibleFields(prev=>({...prev,[visibilityKey]:!prev[visibilityKey]}))}><Icon c={c} name={visibleFields[visibilityKey]?'eye-off-outline':'eye-outline'} color={c.primary}/></Pressable></View></View>})}</>}{formKind === 'reset' && ['password','confirmPassword'].map((key,i)=><View key={key} style={{marginBottom:12}}><Text style={[s.fieldLabel,{color:c.muted}]}>{i?'CONFIRM NEW PASSWORD':'NEW PASSWORD'}</Text><View style={{flexDirection:'row',alignItems:'center',gap:8}}><TextInput value={form[key]||''} onChangeText={v=>setField(key,v)} secureTextEntry={!visibleFields[`reset-${key}`]} placeholder='At least 8 characters' placeholderTextColor={c.muted} style={[s.input,{flex:1,backgroundColor:c.bg,borderColor:c.border,color:c.text}]} /><Pressable accessibilityLabel={visibleFields[`reset-${key}`]?'Hide password':'Show password'} onPress={()=>setVisibleFields(prev=>({...prev,[`reset-${key}`]:!prev[`reset-${key}`]}))}><Icon c={c} name={visibleFields[`reset-${key}`]?'eye-off-outline':'eye-outline'} color={c.primary}/></Pressable></View></View>)}{formKind === 'purchase' && <View style={s.formStatus}><Text style={[s.fieldLabel, { color: c.text }]}>Receive stock now?</Text><Switch value={form.status !== 'PENDING'} onValueChange={value => setField('status', value ? 'RECEIVED' : 'PENDING')} trackColor={{ true: c.primary }} /></View>}</ScrollView><Pressable onPress={saveForm} style={[s.submitButton, { backgroundColor: c.primary }]}><Text style={s.submitText}>{formKind === 'purchase' ? 'Create purchase order' : 'Save changes'}</Text></Pressable></View></KeyboardAvoidingView></View></Modal>

    <Modal visible={modal === 'security'} transparent animationType="slide" onRequestClose={()=>{}}><View style={s.overlay}><KeyboardAvoidingView behavior={Platform.OS==='ios'?'padding':'height'} style={{width:'100%',justifyContent:'flex-end'}}><View style={[s.sheet,{backgroundColor:c.surface}]}><View style={s.sheetHandle}/><Text style={[s.sectionTitle,{color:c.text}]}>Secure your account</Text><Text style={[s.subText,{color:c.muted,marginVertical:8}]}>Set a personal PIN for quick sign in. You can also set or update a password. Keep both somewhere safe.</Text><Text style={[s.fieldLabel,{color:c.muted}]}>NEW 4–6 DIGIT PIN</Text><View style={{flexDirection:'row',alignItems:'center',gap:8}}><TextInput value={securityPin} onChangeText={setSecurityPin} keyboardType='number-pad' secureTextEntry={!visibleFields['securityPin']} placeholder='Enter PIN' placeholderTextColor={c.muted} style={[s.input,{flex:1,backgroundColor:c.bg,borderColor:c.border,color:c.text,marginBottom:10}]}/><Pressable accessibilityLabel={visibleFields['securityPin']?'Hide':'Show'} onPress={()=>setVisibleFields(prev=>({...prev,['securityPin']:!prev['securityPin']}))}><Icon c={c} name={visibleFields['securityPin']?'eye-off-outline':'eye-outline'} color={c.primary}/></Pressable></View><Text style={[s.fieldLabel,{color:c.muted}]}>CONFIRM PIN</Text><View style={{flexDirection:'row',alignItems:'center',gap:8}}><TextInput value={securityPinConfirm} onChangeText={setSecurityPinConfirm} keyboardType='number-pad' secureTextEntry={!visibleFields['securityPinConfirm']} placeholder='Confirm PIN' placeholderTextColor={c.muted} style={[s.input,{flex:1,backgroundColor:c.bg,borderColor:c.border,color:c.text,marginBottom:10}]}/><Pressable accessibilityLabel={visibleFields['securityPinConfirm']?'Hide':'Show'} onPress={()=>setVisibleFields(prev=>({...prev,['securityPinConfirm']:!prev['securityPinConfirm']}))}><Icon c={c} name={visibleFields['securityPinConfirm']?'eye-off-outline':'eye-outline'} color={c.primary}/></Pressable></View><Text style={[s.fieldLabel,{color:c.muted}]}>NEW PASSWORD (OPTIONAL)</Text><View style={{flexDirection:'row',alignItems:'center',gap:8}}><TextInput value={securityPassword} onChangeText={setSecurityPassword} secureTextEntry={!visibleFields['securityPassword']} placeholder='At least 8 characters' placeholderTextColor={c.muted} style={[s.input,{flex:1,backgroundColor:c.bg,borderColor:c.border,color:c.text,marginBottom:10}]}/><Pressable accessibilityLabel={visibleFields['securityPassword']?'Hide':'Show'} onPress={()=>setVisibleFields(prev=>({...prev,['securityPassword']:!prev['securityPassword']}))}><Icon c={c} name={visibleFields['securityPassword']?'eye-off-outline':'eye-outline'} color={c.primary}/></Pressable></View>{securityPassword ? <View style={{flexDirection:'row',alignItems:'center',gap:8}}><TextInput value={securityPasswordConfirm} onChangeText={setSecurityPasswordConfirm} secureTextEntry={!visibleFields['securityPasswordConfirm']} placeholder='Confirm password' placeholderTextColor={c.muted} style={[s.input,{flex:1,backgroundColor:c.bg,borderColor:c.border,color:c.text,marginBottom:10}]}/><Pressable accessibilityLabel={visibleFields['securityPasswordConfirm']?'Hide':'Show'} onPress={()=>setVisibleFields(prev=>({...prev,['securityPasswordConfirm']:!prev['securityPasswordConfirm']}))}><Icon c={c} name={visibleFields['securityPasswordConfirm']?'eye-off-outline':'eye-outline'} color={c.primary}/></Pressable></View> : null}<Pressable onPress={()=>{const next=!showPassword;setShowPassword(next);setVisibleFields(prev=>({...prev,securityPin:next,securityPinConfirm:next,securityPassword:next,securityPasswordConfirm:next}));}}><Text style={[s.link,{color:c.primary,marginBottom:10}]}>{showPassword?'Hide':'Show'} PIN / password</Text></Pressable><Pressable onPress={saveSecuritySetup} style={[s.submitButton,{backgroundColor:c.primary}]}><Text style={s.submitText}>Save sign-in options</Text></Pressable></View></KeyboardAvoidingView></View></Modal>
    <Modal visible={modal === 'cart'} transparent animationType="slide" onRequestClose={hideMenu}><View style={s.overlay}><Pressable style={s.backdrop} onPress={hideMenu} /><View style={[s.sheet, { backgroundColor: c.surface }]}><View style={s.sheetHandle} /><View style={s.sectionHead}><View><Text style={[s.sectionTitle, { color: c.text }]}>Current sale</Text><Text style={[s.mini, { color: c.muted }]}>{cart.reduce((n, line) => n + line.quantity, 0)} items · {customer}</Text></View><Pressable onPress={hideMenu}><Icon c={c} name="close" /></Pressable></View>{cartRows.length === 0 ? <Empty c={c} title="Your cart is empty" text="Add products from the register to start a sale." /> : <><ScrollView style={{ maxHeight: 360 }}>{cartRows.map(({ line, product }) => <CartRow key={product.id} c={c} product={product} line={line} onStep={changeQty} />)}</ScrollView><View style={[divider(c.border), { marginVertical: 12 }]} /><View style={s.rowBetween}><Text style={[s.bodyText, { color: c.muted }]}>Subtotal</Text><Text style={[s.bodyText, { color: c.text }]}>{displayMoney(subtotal)}</Text></View><View style={s.rowBetween}><Text style={[s.bodyText, { color: c.muted }]}>Discount · tax included</Text><Text style={[s.bodyText, { color: c.danger }]}>−{displayMoney(itemDiscount + cartDiscountAmount)}</Text></View><View style={s.rowBetween}><Text style={[s.bodyText, { color: c.muted }]}>Tax</Text><Text style={[s.bodyText, { color: c.text }]}>{displayMoney(taxTotal)}</Text></View><View style={s.rowBetween}><Text style={[s.sectionTitle, { color: c.text }]}>Total due</Text><Text style={[s.sectionTitle, { color: c.primary }]}>{displayMoney(total)}</Text></View><View style={[s.rowBetween, { marginTop: 10 }]}><Text style={[s.mini, { color: c.muted }]}>Cart discount (%)</Text><TextInput keyboardType="decimal-pad" value={String(cartDiscount)} onChangeText={value => setCartDiscount(Math.max(0, Math.min(100, Number(value) || 0)))} style={[s.smallInput, { backgroundColor: c.bg, borderColor: c.border, color: c.text }]} /></View><Pressable onPress={() => { setTender(toSelectedCurrency(total).toFixed(2)); setPayment('CASH'); setModal('checkout'); }} style={[s.submitButton, { backgroundColor: c.primary, marginTop: 14 }]}><Text style={s.submitText}>Proceed to payment · {displayMoney(total)}</Text></Pressable></>}</View></View></Modal>

    <Modal visible={modal === 'checkout'} transparent animationType="slide" onRequestClose={hideMenu}><View style={s.overlay}><Pressable style={s.backdrop} onPress={hideMenu} /><View style={[s.sheet, { backgroundColor: c.surface }]}><View style={s.sheetHandle} /><View style={s.sectionHead}><View><Text style={[s.sectionTitle, { color: c.text }]}>Checkout & payment</Text><Text style={[s.mini, { color: c.muted }]}>{cartRows.length} line items</Text></View><Pressable onPress={() => setModal('cart')}><Text style={[s.link, { color: c.primary }]}>Back to cart</Text></Pressable></View><View style={[s.totalBar, { backgroundColor: c.primarySoft }]}><Text style={[s.rowTitle, { color: c.text }]}>Total payable</Text><Text style={[s.chartAmount, { color: c.primary }]}>{displayMoney(total)}</Text></View><Text style={[s.fieldLabel, { color: c.muted, marginTop: 16 }]}>PAYMENT METHOD</Text><View style={s.paymentGrid}>{[['CASH', 'cash'], ['CARD', 'credit-card-outline'], ['DIGITAL WALLET', 'cellphone-check'], ['BANK TRANSFER', 'bank-outline'], ['SPLIT', 'call-split']].map(([method, icon]) => <Pressable key={method} onPress={() => setPayment(method)} style={[s.paymentChoice, { borderColor: payment === method ? c.primary : c.border, backgroundColor: payment === method ? c.primarySoft : c.surface }]}><Icon c={c} name={icon as IconName} color={payment === method ? c.primary : c.muted} /><Text style={[s.mini, { color: payment === method ? c.primary : c.muted, fontWeight: '800' }]}>{method}</Text></Pressable>)}</View>{payment === 'CASH' && <View style={{ marginTop: 12 }}><Text style={[s.fieldLabel, { color: c.muted }]}>CASH RECEIVED · {currency}</Text><TextInput keyboardType="decimal-pad" value={tender} onChangeText={setTender} style={[s.input, { backgroundColor: c.bg, borderColor: c.border, color: c.text }]} placeholder="Amount tendered" placeholderTextColor={c.muted} /><Text style={[s.bodyText, { color: c.success, marginTop: 7 }]}>Change due · {displayMoney(Math.max(0, enteredToUsd(Number(tender)||0) - total))}</Text></View>}{payment !== 'CASH' && <Text style={[s.subText, { color: c.muted, marginTop: 10 }]}>Payment of {displayMoney(total)} will be recorded as {payment.toLowerCase()}.</Text>}<Pressable onPress={finishSale} style={[s.submitButton, { backgroundColor: c.success, marginTop: 18 }]}><Text style={s.submitText}>Complete sale · {displayMoney(total)}</Text></Pressable></View></View></Modal>

    <Modal visible={modal === 'scanner'} animationType="slide" onRequestClose={() => setModal(null)}><View style={{flex:1,backgroundColor:'#000'}}><CameraView style={{flex:1}} facing="back" barcodeScannerSettings={{barcodeTypes:['ean13','ean8','upc_a','upc_e','code128','code39','qr']}} onBarcodeScanned={({data})=>scanBarcode(data)} /><Pressable onPress={()=>setModal(null)} style={{position:'absolute',top:50,right:20,backgroundColor:'#fff',padding:12,borderRadius:22}}><Text style={{fontWeight:'800'}}>Close</Text></Pressable><Text style={{position:'absolute',bottom:45,alignSelf:'center',color:'#fff',fontWeight:'800'}}>Barcode camera · align code in frame</Text></View></Modal>
    <Modal visible={modal === 'receipt'} transparent animationType="fade" onRequestClose={hideMenu}><View style={[s.receiptOverlay, { backgroundColor: 'rgba(3,10,25,.7)' }]}><View style={[s.receiptCard, { backgroundColor: c.surface }]}><View style={[s.receiptIcon, { backgroundColor: '#DCFCE7' }]}><Icon c={c} name="check-bold" color="#15803D" size={27} /></View><Text style={[s.pageTitle, { color: c.text, textAlign: 'center', marginTop: 10 }]}>Sale complete</Text><Text style={[s.subText, { color: c.muted, marginTop: 5 }]}>{receipt?.invoice} · {fmtDate(receipt?.timestamp || new Date().toISOString())}</Text><View style={[divider(c.border), { marginVertical: 15 }]} />{receipt?.items.map((item, i) => <View key={i} style={s.rowBetween}><Text style={[s.bodyText, { color: c.muted, flex: 1 }]}>{item.quantity} × {item.name}</Text><Text style={[s.bodyText, { color: c.text }]}>{displayMoney(item.price * item.quantity)}</Text></View>)}<View style={[divider(c.border), { marginVertical: 14 }]} /><View style={s.rowBetween}><Text style={[s.sectionTitle, { color: c.text }]}>Total paid</Text><Text style={[s.chartAmount, { color: c.primary }]}>{displayMoney(receipt?.total || 0)}</Text></View><Text style={[s.mini, { color: c.muted, textAlign: 'center', marginTop: 8 }]}>{receipt?.method} · Change {displayMoney(receipt?.change || 0)}</Text><Pressable onPress={attachReceiptEvidence} style={[s.secondaryButton,{borderColor:c.border}]}><Icon c={c} name='camera-plus-outline' color={c.primary}/><Text style={[s.buttonLabel,{color:c.text}]}>{receipt?.evidence?.photoUri ? 'Replace attached proof photo' : 'Attach receipt / payment evidence photo'}</Text></Pressable>{receipt?.evidence?.photoUri ? <Image source={{uri:receipt.evidence.photoUri}} style={{width:76,height:76,borderRadius:12,alignSelf:'center',marginTop:8}}/> : null}<Pressable onPress={() => Share.share({ title: 'Retail POS receipt', message: `RETAIL POS · DOWNTOWN TERMINAL\n${receipt?.invoice}\n${receipt?.items.map(item => `${item.quantity} × ${item.name} — ${displayMoney(item.price * item.quantity)}`).join('\n')}\n\nTOTAL ${displayMoney(receipt?.total || 0)} · ${receipt?.method}` })} style={[s.secondaryButton, { borderColor: c.border }]}><Icon c={c} name="share-variant-outline" color={c.primary} /><Text style={[s.buttonLabel, { color: c.text }]}>Share digital receipt</Text></Pressable><Pressable onPress={() => exportReceiptPdf(receipt).catch(error => Alert.alert('PDF failed', String(error)))} style={[s.secondaryButton,{borderColor:c.border}]}><Icon c={c} name='file-pdf-box' color={c.primary} /><Text style={[s.buttonLabel,{color:c.text}]}>Save / share PDF bill</Text></Pressable><Pressable onPress={() => setModal(null)} style={[s.submitButton, { backgroundColor: c.primary, marginTop: 10 }]}><Text style={s.submitText}>Done</Text></Pressable></View></View></Modal>
  </View>;
}

const ageFromDOB = (dob: string) => { const birth = new Date(`${dob}T00:00:00`); if (Number.isNaN(birth.getTime())) return '—'; const now = new Date(); let age = now.getFullYear() - birth.getFullYear(); if (now.getMonth() < birth.getMonth() || (now.getMonth() === birth.getMonth() && now.getDate() < birth.getDate())) age--; return String(Math.max(0, age)); };
function Icon({ c, name, color, size = 19 }: { c: Record<string, string>; name: IconName; color?: string; size?: number }) { return <MaterialCommunityIcons name={name} size={size} color={color || c.muted} />; }

function Metric({ c, icon, label, value, delta, tint, onPress }: { c: Record<string, string>; icon: IconName; label: string; value: string; delta: string; tint: string; onPress?: () => void }) { return <Pressable onPress={onPress} style={[s.metricCard, { backgroundColor: c.surface, borderColor: c.border }]}><View style={[s.metricIcon, { backgroundColor: tint }]}><Icon c={c} name={icon} color={blue} size={19} /></View><Text style={[s.metricLabel, { color: c.muted }]}>{label}</Text><Text style={[s.metricValue, { color: c.text }]}>{value}</Text><Text style={[s.metricDelta, { color: c.success }]}>{delta}</Text></Pressable>; }
function AudioMessage({uri,c}:{uri:string;c:Record<string,string>}) { const player=useAudioPlayer(uri); return <Pressable onPress={()=>player.play()} style={{flexDirection:'row',alignItems:'center',gap:8,marginTop:8}}><Icon c={c} name='play-circle-outline' color={c.primary}/><Text style={{color:c.primary,fontWeight:'700'}}>Play voice message</Text></Pressable>; }
function SearchBox({ c, value, onChange, placeholder, onScan }: { c: Record<string, string>; value: string; onChange: (x: string) => void; placeholder: string; onScan?: () => void }) { return <View style={[s.searchBox, { backgroundColor: c.surface, borderColor: c.border }]}><MaterialCommunityIcons name="magnify" size={19} color={c.muted} /><TextInput value={value} onChangeText={onChange} placeholder={placeholder} placeholderTextColor={c.muted} style={[s.searchInput, { color: c.text }]} /><Pressable onPress={onScan} disabled={!onScan}><MaterialCommunityIcons name="barcode-scan" size={19} color={onScan ? c.primary : c.muted} /></Pressable></View>; }
function Chip({ c, active, label, onPress }: { c: Record<string, string>; active: boolean; label: string; onPress: () => void }) { return <Pressable onPress={onPress} style={[s.chip, { backgroundColor: active ? c.primary : c.surface, borderColor: active ? c.primary : c.border }]}><Text style={{ color: active ? '#fff' : c.muted, fontSize: 12, fontWeight: '700' }}>{label}</Text></Pressable>; }
function Badge({ label, good, c }: { label: string; good: boolean; c: Record<string, string> }) { return <View style={[s.pill, { backgroundColor: good ? '#DCFCE7' : '#FEF3C7' }]}><Text style={{ color: good ? '#15803D' : '#B45309', fontSize: 10, fontWeight: '900' }}>{label}</Text></View>; }
function ProductRow({ c, product, subline, trailing, onPress }: { c: Record<string, string>; product: Product; subline?: string; trailing?: React.ReactNode; onPress?: () => void }) { return <Pressable onPress={onPress} style={[s.productRow, { backgroundColor: c.surface, borderColor: c.border }]}><View style={[s.productThumb, { backgroundColor: product.category === 'Electronics' ? '#EDE9FE' : '#DBEAFE' }]}>{product.imageUri ? <Image source={{uri:product.imageUri}} style={{width:43,height:43,borderRadius:12}} /> : <Text style={{ fontSize: 22 }}>{product.category === 'Beverages' ? '☕' : product.category === 'Electronics' ? '🎧' : product.category === 'Dairy & Fresh' ? '🥛' : product.category === 'Snacks' ? '🍫' : '🛍️'}</Text>}</View><View style={{ flex: 1, paddingRight: 8 }}><Text style={[s.rowTitle, { color: c.text }]} numberOfLines={1}>{product.name}</Text><Text style={[s.mini, { color: c.muted }]}>{subline || `SKU ${product.sku} · ${product.category}`}</Text><Text style={[s.productPrice, { color: c.primary, marginTop: 5 }]}>{displayMoney(product.price)} <Text style={[s.mini, { color: c.muted, fontWeight: '500' }]}>· Stock {product.stock}</Text></Text></View>{trailing}</Pressable>; }
function CartRow({ c, product, line, onStep }: { c: Record<string, string>; product: Product; line: CartLine; onStep: (id: string, step: number) => void }) { return <View style={[s.cartRow, { borderColor: c.border }]}><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{product.name}</Text><Text style={[s.mini, { color: c.muted }]}>{displayMoney(product.price)} each{line.discount ? ` · ${line.discount}% off` : ''}</Text></View><View style={s.stockActions}><Pressable onPress={() => onStep(product.id, -1)} style={[s.stepButton, { backgroundColor: c.bg }]}><Icon c={c} name="minus" size={15} /></Pressable><Text style={[s.rowTitle, { color: c.text, minWidth: 22, textAlign: 'center' }]}>{line.quantity}</Text><Pressable onPress={() => onStep(product.id, 1)} style={[s.stepButton, { backgroundColor: c.primarySoft }]}><Icon c={c} name="plus" color={c.primary} size={15} /></Pressable><Text style={[s.rowTitle, { color: c.text, minWidth: 64, textAlign: 'right' }]}>{displayMoney(product.price * (1 - line.discount / 100) * line.quantity)}</Text></View></View>; }
function SaleRow({ c, sale, trailing, onPress }: { c: Record<string, string>; sale: Sale; trailing?: React.ReactNode; onPress?: () => void }) { return <Pressable onPress={onPress} style={[s.saleRow, { backgroundColor: c.surface, borderColor: c.border }]}><View style={[s.iconSquare, { backgroundColor: '#DBEAFE' }]}><MaterialCommunityIcons name="receipt-text-outline" color={blue} size={20} /></View><View style={{ flex: 1 }}><Text style={[s.rowTitle, { color: c.text }]}>{sale.invoice} · {sale.customer}</Text><Text style={[s.mini, { color: c.muted }]}>{fmtDate(sale.timestamp)} · {sale.items.length} items · {sale.method}</Text></View><View style={{ alignItems: 'flex-end' }}><Text style={[s.rowTitle, { color: c.text }]}>{displayMoney(sale.total)}</Text><Text style={[s.mini, { color: sale.status === 'COMPLETED' ? green : '#B91C1C', fontWeight: '800' }]}>{sale.status}</Text></View>{trailing}</Pressable>; }
function ProgressLine({ c, title, total, maximum, tint }: { c: Record<string, string>; title: string; total: number; maximum: number; tint: string }) { return <View style={{ marginBottom: 15 }}><View style={s.rowBetween}><Text style={[s.bodyText, { color: c.text }]}>{title}</Text><Text style={[s.bodyText, { color: c.text }]}>{displayMoney(total)}</Text></View><View style={[s.progressTrack, { backgroundColor: c.raised }]}><View style={{ width: `${Math.min(100, total / Math.max(1, maximum) * 100)}%`, height: '100%', borderRadius: 5, backgroundColor: tint }} /></View></View>; }
function Empty({ c, title, text }: { c: Record<string, string>; title: string; text: string }) { return <View style={[s.empty, { backgroundColor: c.surface, borderColor: c.border }]}><MaterialCommunityIcons name="tray-alert" size={27} color={c.muted} /><Text style={[s.rowTitle, { color: c.text, marginTop: 8 }]}>{title}</Text><Text style={[s.subText, { color: c.muted, textAlign: 'center', marginTop: 4 }]}>{text}</Text></View>; }

const s = StyleSheet.create({
  root: { flex: 1, paddingTop: Platform.OS === 'android' ? NativeStatusBar.currentHeight || 0 : 0 },
  topBar: { minHeight: 62, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 8, borderBottomWidth: StyleSheet.hairlineWidth, gap: 9 },
  menuBtn: { width: 40, height: 40, borderRadius: 13, alignItems: 'center', justifyContent: 'center' }, topTitle: { fontSize: 17, fontWeight: '900' }, topSubtitle: { fontSize: 10, fontWeight: '600', marginTop: 2 },
  iconBtn: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22 }, iconBtnSmall: { width: 37, height: 37, alignItems: 'center', justifyContent: 'center', borderRadius: 12 }, notificationDot: { position: 'absolute', top: 2, right: 0, minWidth: 16, height: 16, paddingHorizontal: 3, alignItems: 'center', justifyContent: 'center', borderRadius: 9, backgroundColor: '#DC2626' }, dotText: { fontSize: 9, color: '#fff', fontWeight: '900' }, smallDot: { position: 'absolute', width: 7, height: 7, top: 7, right: 8, borderRadius: 4 },
  scrollContent: { paddingHorizontal: 16, paddingTop: 18, paddingBottom: 28 }, pageHeading: { marginBottom: 16 }, pageTitle: { fontSize: 23, fontWeight: '900', letterSpacing: -0.4 }, bigTitle: { fontSize: 22, fontWeight: '900', marginTop: 5, letterSpacing: -0.4 }, subText: { fontSize: 12, lineHeight: 18 }, eyebrow: { fontSize: 10, fontWeight: '900', letterSpacing: 1.3 }, storeHeadline: { flexDirection: 'row', alignItems: 'center', marginBottom: 17 },
  metricGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 }, metricCard: { width: '48.4%', minHeight: 138, borderRadius: 16, borderWidth: 1, padding: 13 }, metricIcon: { width: 34, height: 34, borderRadius: 11, alignItems: 'center', justifyContent: 'center', marginBottom: 9 }, metricLabel: { fontSize: 11, fontWeight: '700' }, metricValue: { fontSize: 21, fontWeight: '900', marginTop: 3 }, metricDelta: { fontSize: 10, fontWeight: '700', marginTop: 5 },
  sectionHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 20, marginBottom: 10, gap: 8 }, sectionTitle: { fontSize: 15, fontWeight: '900' }, link: { fontSize: 12, fontWeight: '800' }, quickGrid: { gap: 9 }, quickAction: { height: 58, flexDirection: 'row', alignItems: 'center', gap: 11, paddingHorizontal: 12, borderRadius: 14, borderWidth: 1 }, iconSquare: { width: 38, height: 38, borderRadius: 12, alignItems: 'center', justifyContent: 'center' }, quickText: { flex: 1, fontSize: 13, fontWeight: '800' },
  chartCard: { borderWidth: 1, borderRadius: 17, padding: 15 }, chartTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, cardLabel: { fontSize: 11, fontWeight: '700' }, chartAmount: { fontSize: 25, fontWeight: '900' }, pill: { borderRadius: 12, paddingHorizontal: 9, paddingVertical: 5 }, chartBars: { height: 83, flexDirection: 'row', alignItems: 'stretch', gap: 4, marginTop: 15 }, chartLabels: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 7 }, mini: { fontSize: 10, lineHeight: 15 },
  saleRow: { flexDirection: 'row', alignItems: 'center', gap: 10, padding: 11, borderRadius: 14, borderWidth: 1, marginBottom: 8 }, rowTitle: { fontSize: 12, fontWeight: '800' }, productPrice: { fontSize: 14, fontWeight: '900' }, tinyAction: { borderRadius: 9, paddingVertical: 7, paddingHorizontal: 9 }, tinyActionText: { fontSize: 10, fontWeight: '800' },
  customerSelector: { minHeight: 55, flexDirection: 'row', alignItems: 'center', padding: 9, gap: 9, borderRadius: 13, borderWidth: 1, marginBottom: 10 }, avatar: { width: 38, height: 38, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  searchBox: { height: 46, borderWidth: 1, borderRadius: 13, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 9 }, searchInput: { flex: 1, fontSize: 13, paddingVertical: 5 }, chipsRow: { flexDirection: 'row', gap: 7, paddingVertical: 5, alignItems: 'center' }, chip: { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 20, borderWidth: 1 },
  productGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 9 }, productTile: { width: '48.5%', borderRadius: 15, borderWidth: 1, padding: 10, minHeight: 160 }, productEmoji: { width: 45, height: 43, borderRadius: 13, alignItems: 'center', justifyContent: 'center', marginBottom: 8 }, productName: { fontSize: 11, fontWeight: '800', lineHeight: 15, minHeight: 30 }, productTileFoot: { marginTop: 7, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, plusCircle: { width: 25, height: 25, borderRadius: 14, alignItems: 'center', justifyContent: 'center' },
  productRow: { flexDirection: 'row', alignItems: 'center', gap: 10, padding: 10, borderRadius: 14, borderWidth: 1, marginBottom: 8 }, productThumb: { width: 43, height: 43, borderRadius: 12, alignItems: 'center', justifyContent: 'center' }, stockTag: { fontSize: 9, fontWeight: '900', marginBottom: 6 }, stockActions: { flexDirection: 'row', alignItems: 'center', gap: 6 }, stepButton: { width: 27, height: 27, borderRadius: 8, alignItems: 'center', justifyContent: 'center' },
  inventoryTotals: { flexDirection: 'row', gap: 8, marginBottom: 10 }, inventoryTotal: { flex: 1, minHeight: 78, padding: 10, justifyContent: 'center', borderRadius: 13, borderWidth: 1 },
  primaryAction: { flexDirection: 'row', alignItems: 'center', gap: 5, borderRadius: 10, paddingHorizontal: 10, paddingVertical: 8 }, primaryActionText: { color: '#fff', fontSize: 11, fontWeight: '900' },
  aiBanner: { flexDirection: 'row', alignItems: 'center', gap: 10, padding: 12, borderRadius: 15 }, chatBox: { padding: 11, borderRadius: 15, borderWidth: 1, gap: 9, marginTop: 9 }, chatBubble: { maxWidth: '90%', padding: 11, borderRadius: 13, borderWidth: 1 }, dealMini: { marginTop: 8, borderWidth: 1, borderRadius: 11, padding: 9, gap: 6 }, addDealBtn: { alignSelf: 'flex-start', borderRadius: 8, paddingVertical: 6, paddingHorizontal: 9 }, dealCard: { borderWidth: 1, padding: 12, borderRadius: 15, marginBottom: 9, gap: 9 }, dealCardTop: { flexDirection: 'row', alignItems: 'center', gap: 9 }, dealFoot: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  listCard: { borderWidth: 1, borderRadius: 15, padding: 12, marginBottom: 9 }, listCardTop: { flexDirection: 'row', alignItems: 'center', gap: 9, marginBottom: 11 }, rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 }, bodyText: { fontSize: 12 },
  segment: { flexDirection: 'row', borderRadius: 12, padding: 4, marginBottom: 4 }, segmentItem: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingVertical: 9, borderRadius: 9 }, segmentText: { fontSize: 12, fontWeight: '800' }, personCard: { flexDirection: 'row', alignItems: 'center', gap: 9, padding: 11, borderWidth: 1, borderRadius: 14, marginBottom: 8 },
  reportHero: { borderRadius: 18, padding: 18, marginTop: 12, marginBottom: 12 }, progressTrack: { height: 8, borderRadius: 5, overflow: 'hidden', marginTop: 7 }, auditRow: { flexDirection: 'row', paddingVertical: 11, gap: 10, borderBottomWidth: StyleSheet.hairlineWidth }, auditBullet: { width: 8, height: 8, borderRadius: 5, marginTop: 4 }, noticeRow: { flexDirection: 'row', gap: 10, paddingVertical: 12, borderBottomWidth: StyleSheet.hairlineWidth },
  bottomNav: { minHeight: 65, paddingBottom: Platform.OS === 'ios' ? 11 : 2, flexDirection: 'row', justifyContent: 'space-around', borderTopWidth: StyleSheet.hairlineWidth }, navItem: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 2 }, navIcon: { width: 48, height: 30, borderRadius: 17, alignItems: 'center', justifyContent: 'center' }, navLabel: { fontSize: 9, fontWeight: '800' },
  overlay: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(4,10,24,.25)' }, backdrop: { ...StyleSheet.absoluteFill }, sheet: { borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: 18, paddingBottom: 30, maxHeight: '90%' }, menuSheet: { borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: 18, paddingBottom: 30, minHeight: '77%' }, sheetHandle: { width: 42, height: 4, borderRadius: 3, backgroundColor: '#94A3B8', alignSelf: 'center', marginBottom: 15 }, menuSheetHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 13 }, profileStrip: { flexDirection: 'row', alignItems: 'center', gap: 9, padding: 10, borderRadius: 13 }, moduleGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 14 }, moduleCell: { width: '31.4%', minHeight: 76, borderWidth: 1, borderRadius: 13, alignItems: 'center', justifyContent: 'center', gap: 6, padding: 6 }, moduleText: { fontSize: 10, fontWeight: '800', textAlign: 'center' }, logoutButton: { minHeight: 46, flexDirection: 'row', justifyContent: 'center', alignItems: 'center', borderWidth: 1, borderRadius: 12, marginTop: 'auto' },
  fieldLabel: { fontSize: 10, fontWeight: '900', letterSpacing: .5, marginBottom: 6 }, input: { height: 44, borderWidth: 1, borderRadius: 11, paddingHorizontal: 11, fontSize: 13 }, smallInput: { width: 58, height: 34, borderWidth: 1, borderRadius: 9, paddingHorizontal: 8, textAlign: 'center' }, formStatus: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 5 }, submitButton: { minHeight: 48, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 7, borderRadius: 12 }, submitText: { color: '#fff', fontWeight: '900', fontSize: 13 }, paymentGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 8 }, paymentChoice: { minWidth: '30%', borderWidth: 1, borderRadius: 11, paddingVertical: 10, paddingHorizontal: 9, alignItems: 'center', gap: 5 }, totalBar: { borderRadius: 13, padding: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  cartRow: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 8, borderBottomWidth: StyleSheet.hairlineWidth, paddingVertical: 8 }, empty: { alignItems: 'center', padding: 22, borderWidth: 1, borderRadius: 14 }, receiptOverlay: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 20 }, receiptCard: { width: '100%', borderRadius: 20, padding: 18 }, receiptIcon: { alignSelf: 'center', width: 54, height: 54, borderRadius: 30, alignItems: 'center', justifyContent: 'center' }, secondaryButton: { height: 44, borderWidth: 1, borderRadius: 11, marginTop: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 }, buttonLabel: { fontWeight: '800', fontSize: 12 },
  loginWrap: { flexGrow: 1, padding: 22, alignItems: 'center', justifyContent: 'center' }, loginTools: { width: '100%', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 27 }, brandMark: { width: 72, height: 72, borderRadius: 23, alignItems: 'center', justifyContent: 'center' }, loginTitle: { fontSize: 27, fontWeight: '900', letterSpacing: 1.4, marginTop: 12 }, localPill: { flexDirection: 'row', alignItems: 'center', gap: 7, borderRadius: 20, paddingHorizontal: 11, paddingVertical: 7, marginTop: 14 }, loginCard: { width: '100%', borderWidth: 1, borderRadius: 18, padding: 17, marginTop: 23 },
});

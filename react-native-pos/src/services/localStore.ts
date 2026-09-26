import AsyncStorage from '@react-native-async-storage/async-storage';
import { Store, initialStore } from '../model';

export const STORE_KEY = 'retail-pos-react-native-v1';
export const CURRENCY_KEY = 'retail-pos-currency-v1';

/** Parse persisted data defensively so a partial or old cache can still open. */
export async function loadLocalStore(): Promise<Store> {
  const raw = await AsyncStorage.getItem(STORE_KEY);
  if (!raw) return initialStore;
  try {
    return { ...initialStore, ...JSON.parse(raw) } as Store;
  } catch {
    return initialStore;
  }
}

/** Local persistence is the app's offline source of truth between syncs. */
export function saveLocalStore(store: Store): Promise<void> {
  return AsyncStorage.setItem(STORE_KEY, JSON.stringify(store));
}

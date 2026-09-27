/** LAN backend boundary. A missing URL intentionally keeps POS usable offline. */
declare const process: { env: { EXPO_PUBLIC_POS_API_URL?: string; EXPO_PUBLIC_POS_API_KEY?: string } };

export const apiEndpoint = process.env.EXPO_PUBLIC_POS_API_URL?.replace(/\/$/, '');

/** Shared request policy keeps the development API key and JSON headers consistent. */
export const apiFetch = (route: string, init: RequestInit = {}) => {
  if (!apiEndpoint) return Promise.reject(new Error('POS API is not configured'));
  return fetch(`${apiEndpoint}${route}`, {
    ...init,
    headers: {
      ...((init.headers || {}) as Record<string, string>),
      'Content-Type': 'application/json',
      'X-POS-API-Key': process.env.EXPO_PUBLIC_POS_API_KEY || 'local-pos-dev-key',
    },
  });
};

/** Encode local media before it is sent through the JSON-only development API. */
export const bytesToBase64 = (buffer: ArrayBuffer) => {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let index = 0; index < bytes.length; index += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
  }
  return btoa(binary);
};

/** Turn backend-relative attachment paths into usable device URLs. */
export const remoteFileUri = (uri?: string) => uri?.startsWith('/') && apiEndpoint ? `${apiEndpoint}${uri}` : uri;

/** Probe the configured POS server separately from device Internet connectivity. */
export async function checkPosServer(timeoutMs = 3_500): Promise<boolean> {
  if (!apiEndpoint) return false;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(`${apiEndpoint}/health`, { method: 'GET', signal: controller.signal });
    return response.ok;
  } catch {
    return false;
  } finally {
    clearTimeout(timeout);
  }
}

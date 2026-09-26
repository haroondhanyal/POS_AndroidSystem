# Retail POS local backend

The React Native app can use this small Node.js API for shared staff accounts, chat, attachments, online presence, activity records, and sales synchronization.

## Run it on the same Wi-Fi as the phones

1. In `react-native-pos/.env.local`, set `EXPO_PUBLIC_POS_API_URL` to the computer's LAN address and port, for example `http://192.168.0.105:8090`. Keep `EXPO_PUBLIC_POS_API_KEY` equal to the server's `POS_API_KEY`.
2. In a terminal, run `npm run server` from this directory. The API listens on port 8090 and writes data under `server/data/`.
3. In another terminal, run `npx expo start --lan --port 8082` and open the Expo URL on each phone. Keep the API computer and phones on the same Wi-Fi.

The API has `/health` for status and stores uploaded files locally. `server/data/` is ignored by Git.

The default API key is only for local development. It is embedded in the Expo client, so it is not a safe authentication scheme for a public or internet facing server. Use per-user server authentication and HTTPS before deploying outside a trusted development LAN.

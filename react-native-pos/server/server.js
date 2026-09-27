'use strict';

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const port = Number(process.env.PORT || 8090);
const host = process.env.HOST || '0.0.0.0';
const apiKey = process.env.POS_API_KEY || 'local-pos-dev-key';
const dataDir = process.env.POS_DATA_DIR || path.join(__dirname, 'data');
const uploadsDir = path.join(dataDir, 'uploads');
const databaseFile = path.join(dataDir, 'pos-store.json');
fs.mkdirSync(uploadsDir, { recursive: true });

const readDb = () => {
  try { return JSON.parse(fs.readFileSync(databaseFile, 'utf8')); }
  catch { return { users: [], presence: [], messages: [], activity: [], sales: [], syncEvents: [] }; }
};
let db = readDb();
const persist = () => {
  const temporary = `${databaseFile}.${process.pid}.tmp`;
  fs.writeFileSync(temporary, JSON.stringify(db), { mode: 0o600 });
  fs.renameSync(temporary, databaseFile);
};
const json = (res, status, value) => {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(value));
};
const getBody = req => new Promise((resolve, reject) => {
  let data = ''; let size = 0;
  req.on('data', chunk => { size += chunk.length; if (size > 28 * 1024 * 1024) { reject(new Error('Request body is larger than 28 MB.')); req.destroy(); return; } data += chunk; });
  req.on('end', () => { try { resolve(data ? JSON.parse(data) : {}); } catch { reject(new Error('Request body must be valid JSON.')); } });
  req.on('error', reject);
});
const online = item => !item.endedAt && Date.now() - Date.parse(item.lastSeenAt) <= 30_000;
const publicUser = person => { const { pin, photoUri, ...safe } = person; return safe; };

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-POS-API-Key');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  if (url.pathname === '/health' && req.method === 'GET') { json(res, 200, { status: 'ok', service: 'retail-pos-api', serverTime: new Date().toISOString() }); return; }

  if (url.pathname.startsWith('/pos/files/') && req.method === 'GET') {
    const name = path.basename(decodeURIComponent(url.pathname.slice('/pos/files/'.length)));
    const file = path.join(uploadsDir, name);
    if (!fs.existsSync(file)) { json(res, 404, { error: 'Attachment not found.' }); return; }
    const ext = path.extname(file).toLowerCase();
    const mime = ext === '.pdf' ? 'application/pdf' : ext === '.png' ? 'image/png' : ext === '.webp' ? 'image/webp' : ext === '.m4a' ? 'audio/mp4' : ext === '.mp3' ? 'audio/mpeg' : ext === '.wav' ? 'audio/wav' : 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': mime, 'Content-Length': fs.statSync(file).size, 'Cache-Control': 'public, max-age=3600' });
    fs.createReadStream(file).pipe(res); return;
  }
  if (req.headers['x-pos-api-key'] !== apiKey) { json(res, 401, { error: 'Missing or invalid POS API key.' }); return; }

  try {
    if (url.pathname === '/pos/users/heartbeat' && req.method === 'POST') {
      const body = await getBody(req);
      if (!body.userId || !body.name || !body.role || !body.sessionId) { json(res, 400, { error: 'userId, name, role and sessionId are required.' }); return; }
      const now = new Date().toISOString();
      let person = db.users.find(item => item.id === body.userId);
      if (!person) { person = { id: body.userId, name: body.name, role: body.role, email: body.email || '', phone: body.phone || '', active: true }; db.users.push(person); }
      Object.assign(person, { name: body.name, role: body.role, email: body.email || person.email, phone: body.phone || person.phone, active: body.active !== false, username: body.username || person.username, passwordHash: body.passwordHash || person.passwordHash, passwordSalt: body.passwordSalt || person.passwordSalt, pinHash: body.pinHash || person.pinHash, pinSalt: body.pinSalt || person.pinSalt, dateOfBirth: body.dateOfBirth || person.dateOfBirth, countryCode: body.countryCode || person.countryCode });
      let session = db.presence.find(item => item.id === body.sessionId);
      if (!session) { session = { id: body.sessionId, userId: body.userId, name: body.name, role: body.role, startedAt: now, lastSeenAt: now, endedAt: null, onlineSeconds: 0, offlineSeconds: 0 }; db.presence.push(session); }
      // App background/foreground is separate from sign-out; only explicit logout ends the session.
      session.name = body.name; session.role = body.role; session.lastSeenAt = now; session.appForeground = body.appForeground !== false; session.endedAt = null;
      persist();
      json(res, 200, { users: db.users.filter(item => item.active !== false).map(publicUser), presence: db.presence.slice(-500) }); return;
    }
    if (url.pathname === '/pos/users' && req.method === 'GET') { json(res, 200, { users: db.users.filter(item => item.active !== false).map(publicUser), presence: db.presence.slice(-500) }); return; }
    if (url.pathname === '/pos/users/sync' && req.method === 'POST') {
      const body = await getBody(req); const users = Array.isArray(body.users) ? body.users : [];
      for (const incoming of users) {
        if (!incoming.id || !incoming.name || !incoming.role) continue;
        const safeUser = { ...incoming }; delete safeUser.pin; delete safeUser.photoUri;
        const person = db.users.find(item => item.id === incoming.id);
        if (person) Object.assign(person, safeUser);
        else db.users.push(safeUser);
      }
      persist(); json(res, 200, { accepted: users.length }); return;
    }
    if (url.pathname === '/pos/presence/offline' && req.method === 'POST') {
      const body = await getBody(req); const session = db.presence.find(item => item.id === body.sessionId);
      if (session) { session.endedAt = new Date().toISOString(); session.lastSeenAt = session.endedAt; persist(); }
      json(res, 200, { ok: true }); return;
    }
    if (url.pathname === '/pos/chat/messages' && req.method === 'GET') {
      const userId = url.searchParams.get('userId'); const after = Number(url.searchParams.get('after') || 0);
      if (!userId) { json(res, 400, { error: 'userId is required.' }); return; }
      const messages = db.messages.filter(item => (item.senderId === userId || item.recipientId === userId || item.recipientId === '*') && Date.parse(item.timestamp) > after);
      json(res, 200, { messages }); return;
    }
    if (url.pathname === '/pos/chat/messages' && req.method === 'POST') {
      const message = await getBody(req);
      if (!message.id || !message.senderId || !message.recipientId || !message.kind || !message.timestamp) { json(res, 400, { error: 'Message id, sender, recipient, kind and timestamp are required.' }); return; }
      if (db.messages.some(item => item.id === message.id)) { json(res, 200, { message: db.messages.find(item => item.id === message.id) }); return; }
      if (message.attachmentBase64) {
        if (message.attachmentBase64.length > 27 * 1024 * 1024) { json(res, 413, { error: 'Attachment exceeds the 20 MB limit.' }); return; }
        const safeName = `${crypto.randomUUID()}${path.extname(String(message.fileName || 'attachment').replace(/[^a-zA-Z0-9._-]/g, '')).slice(0, 12)}`;
        fs.writeFileSync(path.join(uploadsDir, safeName), Buffer.from(message.attachmentBase64, 'base64'), { mode: 0o600 });
        message.uri = `/pos/files/${safeName}`; delete message.attachmentBase64;
      }
      db.messages.push(message); if (db.messages.length > 5000) db.messages = db.messages.slice(-5000);
      persist(); json(res, 201, { message }); return;
    }
    if (url.pathname === '/pos/activity' && req.method === 'GET') {
      json(res, 200, { activity: db.activity.slice(-1000).reverse() }); return;
    }
    if (url.pathname === '/pos/activity' && req.method === 'POST') {
      const event = await getBody(req); if (!event.id || !event.action || !event.timestamp) { json(res, 400, { error: 'Activity id, action and timestamp are required.' }); return; }
      if (!db.activity.some(item => item.id === event.id)) db.activity.push(event);
      if (db.activity.length > 5000) db.activity = db.activity.slice(-5000);
      persist(); json(res, 201, { ok: true }); return;
    }
    if (url.pathname === '/pos/sync' && req.method === 'POST') {
      const body = await getBody(req); const events = Array.isArray(body.events) ? body.events : [];
      const acceptedIds = [];
      for (const event of events) {
        if (!event.id) continue;
        if (db.syncEvents.some(item => item.id === event.id)) { acceptedIds.push(event.id); continue; }
        db.syncEvents.push(event);
        if (event.type === 'SALE_COMPLETED' && event.payload?.id && !db.sales.some(sale => sale.id === event.payload.id)) db.sales.push(event.payload);
        if (event.type === 'ACTIVITY' && event.payload?.id && !db.activity.some(item => item.id === event.payload.id)) db.activity.push(event.payload);
        if (event.type === 'PRESENCE_OFFLINE' && event.payload?.sessionId) {
          const session = db.presence.find(item => item.id === event.payload.sessionId);
          if (session) { session.endedAt = event.payload.endedAt || event.createdAt; session.lastSeenAt = session.endedAt; }
        }
        acceptedIds.push(event.id);
      }
      if (db.syncEvents.length > 10000) db.syncEvents = db.syncEvents.slice(-10000);
      persist(); json(res, 200, { accepted: acceptedIds.length, acceptedIds }); return;
    }
    if (url.pathname === '/pos/sales' && req.method === 'GET') { json(res, 200, { sales: db.sales.slice(-5000).reverse() }); return; }
    json(res, 404, { error: 'Route not found.' });
  } catch (error) { json(res, 400, { error: error.message || 'Bad request.' }); }
});

server.listen(port, host, () => console.log(`Retail POS API listening on http://${host}:${port} · data: ${dataDir}`));

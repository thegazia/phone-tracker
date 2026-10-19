const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(express.json({ limit: '10mb' }));

// PROMIJENI OVO u svoj tajni kljuc (bilo koji tekst)
const API_KEY = 'moj-tajni-kljuc-123';

const DATA_FILE = path.join(__dirname, 'data.json');

// sigurno citanje - uvijek vrati ispravan oblik
function loadData() {
  try {
    const d = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    if (!Array.isArray(d.sms)) d.sms = [];
    if (!Array.isArray(d.locations)) d.locations = [];
    return d;
  } catch (e) {
    return { sms: [], locations: [] };
  }
}

function saveData(d) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(d, null, 2));
}

// provjera kljuca za slanje podataka
function auth(req, res, next) {
  if (req.headers['x-api-key'] !== API_KEY) {
    return res.status(401).json({ error: 'pogresan kljuc' });
  }
  next();
}

// telefon salje SMS ovdje
app.post('/api/sms', auth, (req, res) => {
  const d = loadData();
  const items = Array.isArray(req.body) ? req.body : [req.body];
  items.forEach(x => d.sms.push({ ...x, received: new Date().toISOString() }));
  saveData(d);
  res.json({ ok: true, primljeno: items.length });
});

// telefon salje lokaciju ovdje
app.post('/api/location', auth, (req, res) => {
  const d = loadData();
  d.locations.push({ ...req.body, received: new Date().toISOString() });
  saveData(d);
  res.json({ ok: true });
});

// dashboard cita podatke
app.get('/api/data', (req, res) => res.json(loadData()));

// web stranica
app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'dashboard.html')));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log('Server radi na portu ' + PORT));

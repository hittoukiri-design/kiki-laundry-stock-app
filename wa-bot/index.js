const express = require('express');
const cors = require('cors');
const { initSession, getStatus } = require('./waService');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

app.get('/api/wa/status', (req, res) => {
    res.json(getStatus());
});

app.post('/api/wa/connect', (req, res) => {
    initSession();
    res.json({ message: 'Initializing connection...' });
});

const PORT = process.env.PORT || 8080;

app.listen(PORT, () => {
    console.log(`Server WA JCL berjalan di port ${PORT}`);
    // Otomatis mulai koneksi WA saat server berjalan
    initSession();
});

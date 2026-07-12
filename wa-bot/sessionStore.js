const fs = require('fs');
const path = require('path');
const { storage } = require('./firebaseAdmin');

// Gunakan nama bucket storage default firebase dari project Kiki
const BUCKET_NAME = 'wa-jcl-bot-sessions-kiki';

async function getBucket() {
    return storage.bucket(BUCKET_NAME);
}

async function backupSession(sessionPath) {
    if (!fs.existsSync(sessionPath)) return;
    const bucket = await getBucket();
    const files = fs.readdirSync(sessionPath);
    for (const file of files) {
        const filePath = path.join(sessionPath, file);
        const destination = `wa-sessions/${file}`;
        try {
            await bucket.upload(filePath, { destination });
        } catch (error) {
            console.error(`Gagal upload ${file}:`, error);
        }
    }
}

async function restoreSession(sessionPath) {
    if (!fs.existsSync(sessionPath)) {
        fs.mkdirSync(sessionPath, { recursive: true });
    }
    const bucket = await getBucket();
    const [files] = await bucket.getFiles({ prefix: 'wa-sessions/' });
    
    if (files.length === 0) {
        console.log('Tidak ada sesi yang tersimpan di Cloud Storage.');
        return;
    }

    console.log('Mengunduh sesi dari Cloud Storage...');
    for (const file of files) {
        if (!file.name.endsWith('.json')) continue;
        const fileName = path.basename(file.name);
        const destination = path.join(sessionPath, fileName);
        await file.download({ destination });
    }
    console.log('Sesi berhasil di-restore.');
}

async function deleteStoredSession(sessionPath) {
    if (fs.existsSync(sessionPath)) {
        fs.rmSync(sessionPath, { recursive: true, force: true });
    }
    const bucket = await getBucket();
    const [files] = await bucket.getFiles({ prefix: 'wa-sessions/' });
    for (const file of files) {
        await file.delete().catch(() => {});
    }
    console.log('Sesi WA telah dihapus dari sistem dan Cloud Storage.');
}

module.exports = { backupSession, restoreSession, deleteStoredSession };

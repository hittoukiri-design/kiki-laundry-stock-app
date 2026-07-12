const { default: makeWASocket, useMultiFileAuthState, DisconnectReason, Browsers, fetchLatestBaileysVersion } = require('@whiskeysockets/baileys');
const pino = require('pino');
const path = require('path');
const { db, admin } = require('./firebaseAdmin');
const { restoreSession, backupSession, deleteStoredSession } = require('./sessionStore');

const sessionPath = path.join(__dirname, 'sessions');

let sock;
let currentQR = null;
let isConnected = false;
let isInitializing = false;
let sessionGeneration = 0;
let backupTimeout;

function scheduleBackup() {
    if (backupTimeout) clearTimeout(backupTimeout);
    backupTimeout = setTimeout(() => {
        backupSession(sessionPath).catch(console.error);
    }, 5000);
}

function normalizePhoneJid(jid) {
    if (!jid || typeof jid !== 'string') return null;
    if (jid.endsWith('@s.whatsapp.net')) return jid;
    if (jid.endsWith('@lid') || jid.endsWith('@g.us')) return null;

    const digits = jid.replace(/\D/g, '');
    return digits ? `${digits}@s.whatsapp.net` : null;
}

function getPhoneCandidateJids(msg) {
    const key = msg?.key || {};
    return [
        key.remoteJidAlt,
        key.participantAlt,
        key.senderPn,
        key.participantPn
    ]
        .map(normalizePhoneJid)
        .filter(Boolean);
}

function displayJid(jid) {
    if (!jid || typeof jid !== 'string') return 'unknown';
    if (jid.endsWith('@s.whatsapp.net') || jid.endsWith('@lid')) return jid.split('@')[0];
    return jid;
}

function getSenderDisplayJid(msg) {
    const phoneJid = getPhoneCandidateJids(msg)[0];
    if (phoneJid) return phoneJid;
    return msg?.key?.participant || msg?.key?.remoteJid || 'unknown';
}

function addReplyTarget(targets, jid, quoted = false) {
    if (!jid || typeof jid !== 'string') return;
    if (!jid.endsWith('@s.whatsapp.net') && !jid.endsWith('@lid') && !jid.endsWith('@g.us')) return;
    if (targets.some(target => target.jid === jid)) return;
    targets.push({ jid, quoted });
}

function collectReplyTargets(remoteJid, msg) {
    const targets = [];
    const isLidChat = remoteJid?.endsWith('@lid');

    if (isLidChat) {
        const phoneCandidates = getPhoneCandidateJids(msg);

        for (const jid of phoneCandidates) {
            addReplyTarget(targets, jid, false);
        }

        if (targets.length === 0) {
            addReplyTarget(targets, remoteJid, false);
        }
    } else {
        addReplyTarget(targets, remoteJid, true);
    }

    return targets;
}

function logReplyContext(remoteJid, msg, targets) {
    const key = msg?.key || {};
    console.log('Reply context:', JSON.stringify({
        remoteJid,
        remoteJidAlt: key.remoteJidAlt || null,
        participant: key.participant || null,
        participantAlt: key.participantAlt || null,
        senderPn: key.senderPn || null,
        participantPn: key.participantPn || null,
        preferredSender: getSenderDisplayJid(msg),
        targets: targets.map(target => target.jid)
    }));
}

async function sendSafeReply(remoteJid, msg, text) {
    if (!sock || !remoteJid) return false;

    const targets = collectReplyTargets(remoteJid, msg);
    logReplyContext(remoteJid, msg, targets);

    let sent = false;
    let lastError = null;
    const sentTargets = new Set();

    for (const target of targets) {
        if (!target.jid || sentTargets.has(target.jid)) continue;
        sentTargets.add(target.jid);

        try {
            await sock.presenceSubscribe(target.jid).catch(() => {});
            await sock.sendPresenceUpdate('available', target.jid).catch(() => {});
            const sendOptions = target.quoted ? { quoted: msg } : undefined;
            await sock.sendMessage(target.jid, { text }, sendOptions);
            sent = true;
            console.log(`Reply sent to ${target.jid}${target.quoted ? ' with quoted message' : ''}`);
        } catch (err) {
            lastError = err;
            console.error(`Error sending reply to ${target.jid}:`, err?.message || err);
        }
    }

    if (!sent && lastError) throw lastError;
    return sent;
}

// Format: "ambil 5 clintex di bypass" or multi-line
async function handleTransactionMessage(text, remoteJid, msg) {
    const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
    if (lines.length < 2) return; // Butuh setidaknya 1 baris outlet dan 1 baris item

    const outletNameRaw = lines[0];
    
    // 1. Cari Outlet di Firestore
    const outletsSnapshot = await db.collection('outlets').get();
    let selectedOutlet = null;
    outletsSnapshot.forEach(doc => {
        const outlet = doc.data();
        if (outlet.name && outlet.name.toLowerCase().includes(outletNameRaw.toLowerCase())) {
            selectedOutlet = { id: doc.id, ...outlet };
            console.log(`Matched Outlet: ${outlet.name}`);
        }
    });

    if (!selectedOutlet) {
        console.log(`Outlet not matched for input: ${outletNameRaw}`);
        // Jika tidak ditemukan, jangan dibalas (mungkin bukan format input stok)
        return;
    }

    // 2. Loop setiap item mulai dari baris kedua
    const itemsSnapshot = await db.collection('items').get();
    let addedCount = 0;
    
    const batch = db.batch();

    for (let i = 1; i < lines.length; i++) {
        let line = lines[i];
        let qty = 1;
        let itemName = line;

        // Cek apakah kata terakhir adalah angka
        const words = line.split(' ');
        const lastWord = words[words.length - 1];
        if (!isNaN(parseInt(lastWord))) {
            qty = parseInt(lastWord);
            words.pop();
            itemName = words.join(' ');
        }

        // Cari item di Firestore
        let selectedItem = null;
        itemsSnapshot.forEach(doc => {
            const item = doc.data();
            if (item.name && item.name.toLowerCase().includes(itemName.toLowerCase())) {
                selectedItem = { id: doc.id, ...item };
                console.log(`Matched Item: ${item.name} with qty ${qty}`);
            }
        });

        if (selectedItem) {
            // Update Item
            const itemRef = db.collection('items').doc(selectedItem.id);
            const newTotalOut = (selectedItem.totalOut || 0) + qty;
            const newRemaining = (selectedItem.startingStock || 0) - newTotalOut;
            
            batch.update(itemRef, {
                totalOut: newTotalOut,
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            // Note: remainingStock is a calculated field in Kotlin (startingStock - totalOut)
            // It is not written to the database.

            // Add Transaction
            const txRef = db.collection('transactions').doc();
            batch.set(txRef, {
                date: new Date(),
                outletId: selectedOutlet.id,
                outletName: selectedOutlet.name,
                region: selectedOutlet.region || '',
                itemId: selectedItem.id,
                itemName: selectedItem.name,
                qtyOut: qty,
                notes: 'Input via WA Bot',
                senderId: remoteJid, // Added senderId for undo tracking
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            addedCount++;
        }
    }

    if (addedCount > 0) {
        await batch.commit();
        console.log(`Transaction committed for ${addedCount} items.`);
        
        await sendSafeReply(remoteJid, msg, `Data item ${selectedOutlet.name} sudah di simpan ✅📥`);
    }
}

async function handleUndoMessage(remoteJid, msg) {
    try {
        // Fix Missing Index: Query last 50 transactions and filter in JS
        const latestTxQuery = await db.collection('transactions')
            .orderBy('createdAt', 'desc')
            .limit(50)
            .get();

        let targetTx = null;
        latestTxQuery.forEach(doc => {
            if (!targetTx && doc.data().senderId === remoteJid && doc.data().notes === 'Input via WA Bot') {
                targetTx = doc.data();
            }
        });

        if (!targetTx) {
            await sendSafeReply(remoteJid, msg, 'Tidak ada transaksi terakhir yang bisa dibatalkan. ❌');
            return;
        }

        const targetTimestamp = targetTx.createdAt;
        const targetOutletName = targetTx.outletName;

        // Find all transactions in the exact same batch (exact same timestamp)
        const batchQuery = await db.collection('transactions')
            .where('senderId', '==', remoteJid)
            .where('createdAt', '==', targetTimestamp)
            .get();

        if (batchQuery.empty) return;

        const batch = db.batch();
        let deletedItemsText = `transaksi\n${targetOutletName}\n`;

        batchQuery.forEach(doc => {
            const data = doc.data();
            const itemRef = db.collection('items').doc(data.itemId);
            
            // Atomic decrement using FieldValue.increment
            batch.update(itemRef, {
                totalOut: admin.firestore.FieldValue.increment(-data.qtyOut),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            // Delete transaction record
            batch.delete(doc.ref);
            
            deletedItemsText += `${data.itemName} ${data.qtyOut}\n`;
        });

        await batch.commit();

        deletedItemsText += `sudah di hapus, silahkan input data baru`;
        
        await sendSafeReply(remoteJid, msg, deletedItemsText);
        console.log(`Undo successful for ${remoteJid}`);

    } catch (error) {
        console.error('Error in handleUndoMessage:', error);
        await sendSafeReply(remoteJid, msg, 'Terjadi kesalahan saat membatalkan transaksi. ❌');
    }
}

async function initSession() {
    if (isInitializing) return;
    isInitializing = true;
    sessionGeneration++;
    const currentGeneration = sessionGeneration;

    try {
        await restoreSession(sessionPath);
        const { state, saveCreds } = await useMultiFileAuthState(sessionPath);
        const { version } = await fetchLatestBaileysVersion();

        sock = makeWASocket({
            version,
            auth: state,
            printQRInTerminal: false,
            logger: pino({ level: 'info' }), // Enable info logs temporarily
            browser: ['Ubuntu', 'Chrome', '20.0.04'], // Hardcoded anti-bug identity
        });

        sock.ev.on('connection.update', (update) => {
            if (sessionGeneration !== currentGeneration) return;
            const { connection, lastDisconnect, qr } = update;
            
            if (qr) {
                currentQR = qr;
                isConnected = false;
            }

            if (connection === 'close') {
                isConnected = false;
                const statusCode = lastDisconnect?.error?.output?.statusCode;
                const shouldReconnect = statusCode !== DisconnectReason.loggedOut;
                
                console.log(`Connection closed (${statusCode}). Reconnecting: ${shouldReconnect}`);
                
                if (shouldReconnect) {
                    const reconnectDelay = statusCode === 428 ? 15000 : 2000;
                    setTimeout(() => {
                        isInitializing = false;
                        initSession();
                    }, reconnectDelay);
                } else {
                    currentQR = null;
                    isInitializing = false;
                    deleteStoredSession(sessionPath).then(() => {
                        initSession();
                    });
                }
            } else if (connection === 'open') {
                console.log('WhatsApp connection opened successfully!');
                isConnected = true;
                currentQR = null;
                isInitializing = false;
                scheduleBackup();
            }
        });

        sock.ev.on('creds.update', async () => {
            await saveCreds();
            scheduleBackup();
        });

        sock.ev.on('messages.upsert', async (m) => {
            if (m.type !== 'notify') return;
            const msg = m.messages[0];
            if (!msg.message || msg.key.fromMe) return;

            const text = msg.message.conversation || msg.message.extendedTextMessage?.text;
            if (!text) return;

            if (text.trim().toLowerCase() === 'ping') {
                await sendSafeReply(msg.key.remoteJid, msg, 'Pong! Bot is active. 🤖');
                return;
            }

            const senderDisplay = displayJid(getSenderDisplayJid(msg));
            
            console.log(`Received message from ${senderDisplay}: \n${text}`);
            
            const lowerText = text.trim().toLowerCase();
            if (lowerText === 'batal' || lowerText === 'delete') {
                await handleUndoMessage(msg.key.remoteJid, msg);
            } else {
                await handleTransactionMessage(text, msg.key.remoteJid, msg);
            }
        });
    } catch (e) {
        console.error("Init Error:", e);
        isInitializing = false;
    }
}

function getStatus() {
    return {
        isConnected,
        qr: currentQR
    };
}

module.exports = { initSession, getStatus };

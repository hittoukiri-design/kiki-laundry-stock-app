const { initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getStorage } = require('firebase-admin/storage');

initializeApp();

const db = getFirestore();
const storage = getStorage();

module.exports = {
    admin: { firestore: { FieldValue } },
    db,
    storage
};

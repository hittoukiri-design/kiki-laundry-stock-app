const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();
async function run() {
  const items = await db.collection('items').get();
  items.forEach(doc => console.log('Item:', doc.id, doc.data().name, doc.data().remainingStock, doc.data().totalOut));
}
run();

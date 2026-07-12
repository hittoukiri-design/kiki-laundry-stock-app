const { db } = require('./firebaseAdmin.js');
async function query() {
  const qs = await db.collection('transactions').orderBy('date', 'desc').limit(5).get();
  qs.forEach(doc => console.log(doc.id, doc.data()));
}
query();

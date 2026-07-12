const admin = require('firebase-admin');
admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId: 'laundry-stock-app'
});
const db = admin.firestore();

async function check() {
    const snapshot = await db.collection('items').get();
    snapshot.forEach(doc => {
        const data = doc.data();
        if (data.name && (data.name.toLowerCase().includes('sunlight') || data.name.toLowerCase().includes('pel'))) {
            console.log(`${data.name} - start: ${data.startingStock}, out: ${data.totalOut}`);
        }
    });
}
check().catch(console.error);

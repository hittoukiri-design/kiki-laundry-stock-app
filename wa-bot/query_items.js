const { db } = require('./firebaseAdmin.js');

async function checkMissingItems() {
    try {
        const itemsSnap = await db.collection('items').get();
        itemsSnap.forEach(doc => {
            const data = doc.data();
            console.log(`ID: ${doc.id} | Name: ${data.name} | TotalOut: ${data.totalOut} | Remaining: ${data.remainingStock} | Start: ${data.startingStock}`);
        });
    } catch(e) {
        console.error(e);
    }
}
checkMissingItems();

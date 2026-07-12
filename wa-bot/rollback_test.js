const admin = require('firebase-admin');
admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId: 'laundry-stock-app'
});
const db = admin.firestore();

async function rollback() {
    console.log("Looking for tester transactions...");
    const cutoff = new Date(Date.now() - 2 * 60 * 60 * 1000); // last 2 hours
    
    // Delete tester transactions
    const txSnapshot = await db.collection('transactions')
        .where('timestamp', '>', admin.firestore.Timestamp.fromDate(cutoff))
        .get();

    if (txSnapshot.empty) {
        console.log("No recent transactions found.");
    }

    const batch = db.batch();
    let count = 0;
    
    const itemsToUpdate = {};

    txSnapshot.forEach(doc => {
        const data = doc.data();
        if (data.operatorName === 'WA Bot') {
            console.log(`Found tester transaction to delete: ${data.itemName} x${data.quantity}`);
            batch.delete(doc.ref);
            count++;
            
            if (data.itemId) {
                if (!itemsToUpdate[data.itemId]) itemsToUpdate[data.itemId] = 0;
                itemsToUpdate[data.itemId] += data.quantity;
            }
        }
    });

    if (count > 0) {
        // Fetch items and reduce totalOut
        for (const itemId of Object.keys(itemsToUpdate)) {
            const qtyToRollback = itemsToUpdate[itemId];
            const itemRef = db.collection('items').doc(itemId);
            const itemSnap = await itemRef.get();
            if (itemSnap.exists) {
                const itemData = itemSnap.data();
                const newTotalOut = Math.max(0, (itemData.totalOut || 0) - qtyToRollback);
                console.log(`Rolling back item ${itemData.name} by -${qtyToRollback}, new totalOut: ${newTotalOut}`);
                batch.update(itemRef, { totalOut: newTotalOut, updatedAt: admin.firestore.FieldValue.serverTimestamp() });
            }
        }
        await batch.commit();
        console.log("Rollback completed successfully!");
    }
}
rollback().catch(console.error);

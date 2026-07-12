const { db } = require('./firebaseAdmin');

async function run() {
    const outletsSnapshot = await db.collection('outlets').get();
    console.log("OUTLETS:");
    outletsSnapshot.forEach(doc => {
        console.log("- " + doc.data().name);
    });

    const itemsSnapshot = await db.collection('items').get();
    console.log("\nITEMS:");
    itemsSnapshot.forEach(doc => {
        console.log("- " + doc.data().name);
    });
}
run().catch(console.error);

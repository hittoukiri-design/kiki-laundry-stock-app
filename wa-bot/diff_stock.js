const fs = require('fs');
const itemsRaw = JSON.parse(fs.readFileSync('all_items.json', 'utf8'));
const txRaw = JSON.parse(fs.readFileSync('all_transactions.json', 'utf8'));

const items = {};
if (itemsRaw.documents) {
    itemsRaw.documents.forEach(doc => {
        const id = doc.name.split('/').pop();
        const name = doc.fields?.name?.stringValue || 'Unknown';
        const totalOut = parseInt(doc.fields?.totalOut?.integerValue || '0', 10);
        const startingStock = parseInt(doc.fields?.startingStock?.integerValue || '0', 10);
        items[id] = { id, name, totalOut, expectedTotalOut: 0, startingStock };
    });
}

if (txRaw.documents) {
    txRaw.documents.forEach(doc => {
        const itemId = doc.fields?.itemId?.stringValue;
        const qtyOut = parseInt(doc.fields?.qtyOut?.integerValue || '0', 10);
        if (itemId && items[itemId]) {
            items[itemId].expectedTotalOut += qtyOut;
        }
    });
}

for (const id in items) {
    const item = items[id];
    if (item.totalOut !== item.expectedTotalOut) {
        console.log(`DISCREPANCY DETECTED:`);
        console.log(`- Item: ${item.name} (ID: ${id})`);
        console.log(`  Current totalOut: ${item.totalOut}`);
        console.log(`  Expected totalOut (sum of tx): ${item.expectedTotalOut}`);
        const missing = item.totalOut - item.expectedTotalOut;
        console.log(`  Difference: ${missing} (Stock is lower by this amount)`);
        console.log(`  Current Remaining Stock: ${item.startingStock - item.totalOut}`);
        console.log(`  Correct Remaining Stock: ${item.startingStock - item.expectedTotalOut}`);
        console.log('---');
    }
}

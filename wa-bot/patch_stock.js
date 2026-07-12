const https = require('https');

function patchItem(itemId, totalOut) {
    const data = JSON.stringify({
        fields: {
            totalOut: { integerValue: totalOut.toString() }
        }
    });

    const options = {
        hostname: 'firestore.googleapis.com',
        port: 443,
        path: `/v1/projects/laundry-stock-app/databases/(default)/documents/items/${itemId}?updateMask.fieldPaths=totalOut&key=AIzaSyCXFBkgGw-piTUm10Kn921rSbvsI4T7U14`,
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': data.length
        }
    };

    const req = https.request(options, res => {
        console.log(`Status for ${itemId}: ${res.statusCode}`);
        res.on('data', d => process.stdout.write(d));
    });

    req.on('error', error => console.error(error));
    req.write(data);
    req.end();
}

// Sunlight: startingStock is 20. Target remaining is 12. So totalOut = 20 - 12 = 8.
patchItem('item_sunlight', 8);

// Super Pel: startingStock is 29. Target remaining is 28. So totalOut = 29 - 28 = 1.
patchItem('item_super_pel', 1);


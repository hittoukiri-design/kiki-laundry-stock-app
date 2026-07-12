package com.laundry.stockapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.data.model.Outlet
import com.laundry.stockapp.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor() {
    
    private val db = FirebaseFirestore.getInstance()

    suspend fun seedDatabaseIfEmpty() {
        try {
            val itemsCol = db.collection("items")
            val outletsCol = db.collection("outlets")
            val usersCol = db.collection("users")

            var itemCount = -1
            try {
                val snapshot = try {
                    itemsCol.get(com.google.firebase.firestore.Source.CACHE).await()
                } catch (cacheEx: Exception) {
                    itemsCol.get(com.google.firebase.firestore.Source.SERVER).await()
                }
                itemCount = snapshot.size()
            } catch (e: Exception) {
                // Keep as -1, which prevents seeding if we don't know the status (e.g. offline first)
            }

            var outletCount = -1
            try {
                val snapshot = try {
                    outletsCol.get(com.google.firebase.firestore.Source.CACHE).await()
                } catch (cacheEx: Exception) {
                    outletsCol.get(com.google.firebase.firestore.Source.SERVER).await()
                }
                outletCount = snapshot.size()
            } catch (e: Exception) {
                // Keep as -1, do not seed
            }

            if (itemCount == 0) {
                seedItems(itemsCol)
            }

            if (outletCount == 0) {
                seedOutlets(outletsCol)
            } else {
                // One-time typo fix migration for existing databases
                try {
                    val outletsSnapshot = outletsCol.whereEqualTo("name", "LAUNDRY DEWE").get().await()
                    for (doc in outletsSnapshot.documents) {
                        doc.reference.update("name", "LONDRI DEWE").await()
                    }
                } catch(e: Exception){}
                try {
                    val outletsSnapshot = outletsCol.get().await()
                    for (doc in outletsSnapshot.documents) {
                        val name = doc.getString("name") ?: ""
                        if (name == "GEDE LONDRY") {
                            doc.reference.update("name", "GEDE LONDRI")
                        } else if (name == "LONDRI DEW ANTASURA") {
                            doc.reference.update("name", "LONDRI DEWE ANTASURA")
                        }
                    }
                } catch (e: Exception) {}

                // Migrate/Insert new outlet CL SESETAN if it is missing in existing databases
                try {
                    val clSesetanRef = outletsCol.document("outlet_cl_sesetan")
                    val clSesetanDoc = clSesetanRef.get().await()
                    if (!clSesetanDoc.exists()) {
                        val outlet = Outlet(
                            id = "outlet_cl_sesetan",
                            name = "CL SESETAN",
                            region = "Selatan",
                            createdAt = Date(),
                            updatedAt = Date()
                        )
                        clSesetanRef.set(outlet).await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            seedUsers(usersCol)
            migrateOutletRegionCase(outletsCol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Migration: fix outlets that were seeded with uppercase region (SELATAN→Selatan etc.)
    private suspend fun migrateOutletRegionCase(outletsCol: com.google.firebase.firestore.CollectionReference) {
        val regionMap = mapOf("SELATAN" to "Selatan", "UTARA" to "Utara", "BARAT" to "Barat")
        try {
            val snapshot = outletsCol.get().await()
            for (doc in snapshot.documents) {
                val region = doc.getString("region") ?: continue
                val fixed = regionMap[region]
                if (fixed != null) {
                    doc.reference.update("region", fixed)
                }
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun seedItems(itemsCol: com.google.firebase.firestore.CollectionReference) {
        val seedItems = listOf(
            "SODA", "CITRUN", "SAPU", "SUNLIGHT", "SIKAT MESIN", "KUAS", 
            "KRESEK SAMPAH", "SIKAT KAWAT", "SAPU LIDI", "SEMPROTAN", 
            "SAPU LANTAI", "SUPER PEL", "LAP", "KANEBO", "PULPEN", "BUKU", 
            "STIPO", "PLASTIK JUALAN", "KARET GAS", "KURSI KECIL", "CLINTEX", 
            "KARET", "EMBER", "TONG SAMPAH", "GUNTING", "LAMPU", "SIKAT WC", 
            "SODA API", "SABUN LANTAI", "KESET LANTAI"
        )

        for (itemName in seedItems) {
            val docId = "item_" + itemName.lowercase(java.util.Locale.US).replace("[^a-z0-9]".toRegex(), "_")
            val docRef = itemsCol.document(docId)
            val item = Item(id = docId, name = itemName, startingStock = 0, totalOut = 0, createdAt = Date(), updatedAt = Date())
            docRef.set(item) // DO NOT await()! Force write to local cache instantly.
        }
    }

    private fun seedOutlets(outletsCol: com.google.firebase.firestore.CollectionReference) {
        val seedOutlets = listOf(
            Pair("JCL SIDAKARYA", "Selatan"),
            Pair("BALI WASH GALANG", "Selatan"),
            Pair("JCL BYPASS", "Selatan"),
            Pair("JCL SESETAN", "Selatan"),
            Pair("CL SESETAN", "Selatan"),
            Pair("GRIYA ANYAR", "Selatan"),
            Pair("KLIN WASH", "Selatan"),
            Pair("LONDRI DEWE", "Selatan"),
            Pair("DIY", "Selatan"),
            Pair("JCL PEMOGAN", "Selatan"),
            Pair("JCL AYANI", "Utara"),
            Pair("CUCI DEWE", "Utara"),
            Pair("LONDRI DEWE ANTASURA", "Utara"),
            Pair("GEDE LONDRI", "Utara"),
            Pair("JCL NUSA KAMBANGAN", "Barat"),
            Pair("MERPATI", "Barat"),
            Pair("BALI WASH", "Barat"),
            Pair("RINJANI WASH", "Barat"),
            Pair("MONANG MANING", "Barat")
        )

        for ((name, region) in seedOutlets) {
            val docId = "outlet_" + name.lowercase(java.util.Locale.US).replace("[^a-z0-9]".toRegex(), "_")
            val docRef = outletsCol.document(docId)
            val outlet = Outlet(id = docId, name = name, region = region, createdAt = Date(), updatedAt = Date())
            docRef.set(outlet) // DO NOT await()!
        }
    }

    private suspend fun seedUsers(usersCol: com.google.firebase.firestore.CollectionReference) {
        val users = listOf(
            User(
                name = "Admin User",
                email = "admin@example.com",
                role = "Admin",
                passwordHash = hashString("12345678")
            ),
            User(
                name = "Project Owner",
                email = "chris@tambayong.com",
                role = "Master App",
                passwordHash = hashString("12345678")
            )
        )

        for (user in users) {
            var exists = false
            var checkSuccess = false
            try {
                // Try from cache first, then server
                val existing = try {
                    usersCol.whereEqualTo("email", user.email).limit(1).get(com.google.firebase.firestore.Source.CACHE).await()
                } catch (cacheEx: Exception) {
                    usersCol.whereEqualTo("email", user.email).limit(1).get(com.google.firebase.firestore.Source.SERVER).await()
                }
                exists = !existing.isEmpty
                checkSuccess = true
            } catch (e: Exception) {
                // ignore
            }
            if (checkSuccess && !exists) {
                val docRef = usersCol.document()
                docRef.set(user.copy(id = docRef.id)) // DO NOT await()!
            }
        }
    }

    private fun hashString(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

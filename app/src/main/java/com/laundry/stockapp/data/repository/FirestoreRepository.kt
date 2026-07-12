package com.laundry.stockapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.data.model.Outlet
import com.laundry.stockapp.data.model.Transaction
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val itemsCol = db.collection("items")
    private val outletsCol = db.collection("outlets")
    private val transactionsCol = db.collection("transactions")
    private val backupsCol = db.collection("backups")

    init {
        // Offline persistence is enabled by default in Firestore Android SDK.
        // Do NOT call db.disableNetwork() here as it blocks all server reads.
    }

    // --- Items ---
    suspend fun getItems(): List<Item> {
        return itemsCol.orderBy("name", Query.Direction.ASCENDING).getObjects(Item::class.java)
    }

    suspend fun getItemById(id: String): Item? {
        return itemsCol.document(id).getObject(Item::class.java)
    }

    suspend fun saveItem(item: Item) {
        val docRef = if (item.id.isEmpty()) itemsCol.document() else itemsCol.document(item.id)
        val itemToSave = item.copy(
            id = docRef.id,
            updatedAt = Date(),
            createdAt = if (item.id.isEmpty()) Date() else (item.createdAt ?: Date())
        )
        docRef.set(itemToSave) // Writes locally instantly, syncs in background
    }

    suspend fun deleteItem(id: String) {
        itemsCol.document(id).delete()
    }

    // --- Outlets ---
    suspend fun getOutlets(): List<Outlet> {
        val outlets = outletsCol.getObjects(Outlet::class.java)
        return outlets.sortedWith(compareBy({ it.region }, { it.name }))
    }

    suspend fun saveOutlet(outlet: Outlet) {
        val docRef = if (outlet.id.isEmpty()) outletsCol.document() else outletsCol.document(outlet.id)
        val outletToSave = outlet.copy(
            id = docRef.id,
            updatedAt = Date(),
            createdAt = if (outlet.id.isEmpty()) Date() else (outlet.createdAt ?: Date())
        )
        docRef.set(outletToSave)
    }

    suspend fun deleteOutlet(id: String) {
        outletsCol.document(id).delete()
    }

    // --- Transactions ---
    suspend fun getTransactions(): List<Transaction> {
        return transactionsCol.orderBy("date", Query.Direction.DESCENDING).getObjects(Transaction::class.java)
    }

    suspend fun getRecentTransactions(limit: Long = 10): List<Transaction> {
        return transactionsCol
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .getObjects(Transaction::class.java)
    }

    fun listenToTransactions(onUpdate: () -> Unit): ListenerRegistration {
        return transactionsCol.orderBy("createdAt", Query.Direction.DESCENDING).limit(1).addSnapshotListener { _, e ->
            if (e != null) return@addSnapshotListener
            onUpdate()
        }
    }

    suspend fun getTransactionsByOutlet(outletId: String): List<Transaction> {
        return transactionsCol
            .whereEqualTo("outletId", outletId)
            .orderBy("date", Query.Direction.DESCENDING)
            .getObjects(Transaction::class.java)
    }

    suspend fun saveTransaction(transaction: Transaction) {
        val docRef = if (transaction.id.isEmpty()) transactionsCol.document() else transactionsCol.document(transaction.id)
        val isNew = transaction.id.isEmpty()
        val itemId = transaction.itemId.orEmpty()

        db.runTransaction { firebaseTransaction ->
            if (itemId.isNotEmpty()) {
                val itemRef = itemsCol.document(itemId)
                val itemSnapshot = firebaseTransaction.get(itemRef)
                if (itemSnapshot.exists() && isNew) {
                    val currentItem = itemSnapshot.toObject(Item::class.java)
                    if (currentItem != null) {
                        val newTotalOut = (currentItem.totalOut ?: 0) + (transaction.qtyOut ?: 0)
                        firebaseTransaction.update(itemRef, "totalOut", newTotalOut)
                        firebaseTransaction.update(itemRef, "updatedAt", Date())
                    }
                }
            }

            val transToSave = transaction.copy(
                id = docRef.id,
                updatedAt = Date(),
                createdAt = if (isNew) Date() else (transaction.createdAt ?: Date())
            )
            firebaseTransaction.set(docRef, transToSave)
        }.await()
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        val transRef = transactionsCol.document(transaction.id)
        val itemId = transaction.itemId.orEmpty()

        db.runTransaction { firebaseTransaction ->
            if (itemId.isNotEmpty()) {
                val itemRef = itemsCol.document(itemId)
                val itemSnapshot = firebaseTransaction.get(itemRef)
                if (itemSnapshot.exists()) {
                    val currentItem = itemSnapshot.toObject(Item::class.java)
                    if (currentItem != null) {
                        val newTotalOut = (currentItem.totalOut ?: 0) - (transaction.qtyOut ?: 0)
                        firebaseTransaction.update(itemRef, "totalOut", newTotalOut)
                        firebaseTransaction.update(itemRef, "updatedAt", Date())
                    }
                }
            }
            firebaseTransaction.delete(transRef)
        }.await()
    }

    // --- Backup History ---
    suspend fun getBackupHistory(): List<com.laundry.stockapp.data.model.BackupLog> {
        return backupsCol.orderBy("timestamp", Query.Direction.DESCENDING).getObjects(com.laundry.stockapp.data.model.BackupLog::class.java)
    }

    suspend fun saveBackupLog(log: com.laundry.stockapp.data.model.BackupLog) {
        val docRef = backupsCol.document()
        val logToSave = log.copy(id = docRef.id, timestamp = Date())
        docRef.set(logToSave).await()
    }

    // --- Helper extensions for offline-first cache fetching ---
    private suspend fun <T> Query.getObjects(clazz: Class<T>): List<T> {
        // Gunakan Source.DEFAULT: Firestore otomatis mengambil dari server jika online, 
        // dan otomatis fallback ke CACHE jika offline.
        return try {
            val defaultSnapshot = this.get(com.google.firebase.firestore.Source.DEFAULT).await()
            defaultSnapshot.toObjects(clazz)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun <T> com.google.firebase.firestore.DocumentReference.getObject(clazz: Class<T>): T? {
        return try {
            val defaultSnapshot = this.get(com.google.firebase.firestore.Source.DEFAULT).await()
            defaultSnapshot.toObject(clazz)
        } catch (e: Exception) {
            null
        }
    }

    // --- Maintenance & Safety Checks ---
    suspend fun getMaintenanceItems(outletId: String): List<com.laundry.stockapp.data.model.MaintenanceItem> {
        return outletsCol.document(outletId).collection("maintenance_items")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .getObjects(com.laundry.stockapp.data.model.MaintenanceItem::class.java)
    }

    suspend fun saveMaintenanceItem(outletId: String, item: com.laundry.stockapp.data.model.MaintenanceItem) {
        val col = outletsCol.document(outletId).collection("maintenance_items")
        val docRef = if (item.id.isEmpty()) col.document() else col.document(item.id)
        val toSave = item.copy(
            id = docRef.id,
            updatedAt = Date(),
            createdAt = if (item.id.isEmpty()) Date() else (item.createdAt ?: Date())
        )
        docRef.set(toSave)
    }

    suspend fun deleteMaintenanceItem(outletId: String, itemId: String) {
        outletsCol.document(outletId).collection("maintenance_items").document(itemId).delete()
    }

    suspend fun getRegulatorCheck(outletId: String): com.laundry.stockapp.data.model.SafetyRegulatorCheck? {
        return outletsCol.document(outletId).collection("safety_checks").document("regulator")
            .getObject(com.laundry.stockapp.data.model.SafetyRegulatorCheck::class.java)
    }

    suspend fun saveRegulatorCheck(outletId: String, check: com.laundry.stockapp.data.model.SafetyRegulatorCheck) {
        val docRef = outletsCol.document(outletId).collection("safety_checks").document("regulator")
        docRef.set(check.copy(updatedAt = Date()))
    }

    suspend fun getAparCheck(outletId: String): com.laundry.stockapp.data.model.SafetyAparCheck? {
        return outletsCol.document(outletId).collection("safety_checks").document("apar")
            .getObject(com.laundry.stockapp.data.model.SafetyAparCheck::class.java)
    }

    suspend fun saveAparCheck(outletId: String, check: com.laundry.stockapp.data.model.SafetyAparCheck) {
        val docRef = outletsCol.document(outletId).collection("safety_checks").document("apar")
        docRef.set(check.copy(updatedAt = Date()))
    }

    suspend fun clearAllCollections() {
        try {
            val items = getItems()
            items.forEach { itemsCol.document(it.id).delete() }
            
            val outlets = getOutlets()
            outlets.forEach { outletsCol.document(it.id).delete() }
            
            val transactions = getTransactions()
            transactions.forEach { transactionsCol.document(it.id).delete() }
            
            val backups = getBackupHistory()
            backups.forEach { backupsCol.document(it.id).delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

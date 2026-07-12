package com.laundry.stockapp.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class User(
    @DocumentId val id: String = "",
    val name: String? = "",
    val email: String? = "",
    val phone: String? = "",
    val role: String? = "Admin", // "Master App" or "Admin"
    val passwordHash: String? = "",
    val masterPasswordHash: String? = "",
    val createdAt: Date? = Date()
)

data class Item(
    @DocumentId val id: String = "",
    val name: String? = "",
    val startingStock: Int? = 0,
    val totalOut: Int? = 0,
    val createdAt: Date? = Date(),
    val updatedAt: Date? = Date()
) {
    val remainingStock: Int
        get() = (startingStock ?: 0) - (totalOut ?: 0)
}

data class Outlet(
    @DocumentId val id: String = "",
    val name: String? = "",
    val region: String? = "",
    val createdAt: Date? = Date(),
    val updatedAt: Date? = Date()
)

data class Transaction(
    @DocumentId val id: String = "",
    val date: Date? = Date(),
    val outletId: String? = "",
    val outletName: String? = "",
    val region: String? = "",
    val itemId: String? = "",
    val itemName: String? = "",
    val qtyOut: Int? = 0,
    val notes: String? = "",
    val createdAt: Date? = Date(),
    val updatedAt: Date? = Date()
)

data class BackupLog(
    @DocumentId val id: String = "",
    val timestamp: Date? = Date(),
    val timeFormatted: String? = "",
    val type: String? = "Manual", // "Manual" or "Otomatis"
    val size: String? = "",
    val status: String? = "Berhasil", // "Berhasil" or "Gagal"
    val errorMessage: String? = "",
    val operator: String? = "Admin"
)

data class MaintenanceItem(
    @DocumentId val id: String = "",
    val name: String? = "",
    val lastMaintenanceAt: Date? = null,
    val createdAt: Date? = Date(),
    val updatedAt: Date? = Date()
)

data class SafetyRegulatorCheck(
    val lastTestDay: Int? = null,
    val lastTestMonth: Int? = null,
    val lastTestYear: Int? = null,
    val updatedAt: Date? = Date()
)

data class SafetyAparCheck(
    val lastRefillDate: Date? = null,
    val intervalMonths: Int? = 36, // Default 3 years (36 months)
    val updatedAt: Date? = Date()
)


package com.vsk.orders.data

import com.google.firebase.firestore.DocumentSnapshot

const val ROLE_USER = "user"
const val ROLE_ADMIN = "admin"
const val ROLE_SUPERADMIN = "superadmin"

const val STATUS_PENDING = "pending"
const val STATUS_APPROVED = "approved"

data class AppUser(
    val email: String = "",
    val status: String = STATUS_PENDING,
    val role: String = ROLE_USER,
    val stations: List<String> = emptyList(),
    val pinHash: String = "",
    val createdAt: Long = 0L
) {
    val isApproved: Boolean get() = status == STATUS_APPROVED
    val isAdmin: Boolean get() = role == ROLE_ADMIN || role == ROLE_SUPERADMIN
    val isSuperAdmin: Boolean get() = role == ROLE_SUPERADMIN
    val hasPin: Boolean get() = pinHash.isNotBlank()
}

fun DocumentSnapshot.toAppUser(): AppUser? {
    return try {
        @Suppress("UNCHECKED_CAST")
        val stations = get("stations") as? List<String> ?: emptyList()
        AppUser(
            email = id,
            status = getString("status") ?: STATUS_PENDING,
            role = getString("role") ?: ROLE_USER,
            stations = stations,
            pinHash = getString("pinHash") ?: "",
            createdAt = getLong("createdAt") ?: 0L
        )
    } catch (e: Exception) {
        null
    }
}

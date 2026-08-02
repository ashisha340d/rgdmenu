package com.vsk.orders.data

import com.google.firebase.firestore.DocumentSnapshot

data class Station(
    val id: String = "",
    val name: String = ""
)

fun DocumentSnapshot.toStation(): Station? {
    return try {
        Station(id = id, name = getString("name") ?: "")
    } catch (e: Exception) {
        null
    }
}

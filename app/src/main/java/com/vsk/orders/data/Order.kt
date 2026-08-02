package com.vsk.orders.data

import com.google.firebase.firestore.DocumentSnapshot

data class OrderItem(
    val name: String = "",
    val qty: String = "",
    val adminQty: String = ""
)

data class Order(
    val id: String = "",
    val eventType: String = "",
    val location: String = "",
    val contactName: String = "",
    val pax: String = "",
    val orderTimeMillis: Long = 0L,
    val items: List<OrderItem> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val acknowledgedBy: List<String> = emptyList(),
    val done: Boolean = false,
    val doneBy: String = "",
    val voiceNoteUrl: String = ""
)

fun DocumentSnapshot.toOrder(): Order? {
    return try {
        @Suppress("UNCHECKED_CAST")
        val itemsRaw = get("items") as? List<Map<String, Any>> ?: emptyList()
        val items = itemsRaw.map {
            OrderItem(
                name = it["name"] as? String ?: "",
                qty = it["qty"] as? String ?: "",
                adminQty = it["adminQty"] as? String ?: ""
            )
        }
        Order(
            id = id,
            eventType = getString("eventType") ?: "",
            location = getString("location") ?: "",
            contactName = getString("contactName") ?: "",
            pax = getString("pax") ?: "",
            orderTimeMillis = getLong("orderTimeMillis") ?: 0L,
            items = items,
            createdBy = getString("createdBy") ?: "",
            createdAt = getLong("createdAt") ?: 0L,
            acknowledgedBy = (get("acknowledgedBy") as? List<String>) ?: emptyList(),
            done = getBoolean("done") ?: false,
            doneBy = getString("doneBy") ?: "",
            voiceNoteUrl = getString("voiceNoteUrl") ?: ""
        )
    } catch (e: Exception) {
        null
    }
}

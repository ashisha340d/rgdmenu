package com.vsk.orders.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object Repo {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentEmail: String?
        get() = auth.currentUser?.email

    fun groups() = db.collection("groups")

    fun group(groupId: String) = db.collection("groups").document(groupId)

    fun orders(groupId: String) = group(groupId).collection("orders")

    fun order(groupId: String, orderId: String) = orders(groupId).document(orderId)
}

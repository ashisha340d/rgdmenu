package com.vsk.orders.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object Repo {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    val currentEmail: String?
        get() = auth.currentUser?.email

    fun users() = db.collection("users")
    fun user(email: String) = db.collection("users").document(email)

    fun stations() = db.collection("stations")
    fun station(stationId: String) = db.collection("stations").document(stationId)

    fun orders(stationId: String) = station(stationId).collection("orders")
    fun order(stationId: String, orderId: String) = orders(stationId).document(orderId)

    fun voiceNoteRef(stationId: String, orderId: String) =
        storage.reference.child("stations/$stationId/orders/$orderId/voice.m4a")
}

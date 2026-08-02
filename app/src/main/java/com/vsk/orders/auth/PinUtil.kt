package com.vsk.orders.auth

import java.security.MessageDigest

object PinUtil {
    fun hash(pin: String, email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$email:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

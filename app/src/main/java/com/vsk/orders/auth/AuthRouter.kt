package com.vsk.orders.auth

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.vsk.orders.data.Constants
import com.vsk.orders.data.ROLE_SUPERADMIN
import com.vsk.orders.data.ROLE_USER
import com.vsk.orders.data.Repo
import com.vsk.orders.data.STATUS_APPROVED
import com.vsk.orders.data.STATUS_PENDING
import com.vsk.orders.data.toAppUser
import com.vsk.orders.station.StationListActivity

// Decides where to send a signed-in user: approval gate, PIN setup, PIN
// unlock, or straight into the app. Called both right after sign-in and on
// every cold start where Firebase already has a session.
object AuthRouter {

    fun route(activity: Activity, email: String) {
        Repo.user(email).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    createProfileThenRoute(activity, email)
                    return@addOnSuccessListener
                }
                val user = doc.toAppUser()
                when {
                    user == null || !user.isApproved -> goPendingApproval(activity)
                    !user.hasPin -> goPinSetup(activity)
                    else -> goPinUnlock(activity)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, e.message ?: "Failed to load profile", Toast.LENGTH_LONG).show()
            }
    }

    private fun createProfileThenRoute(activity: Activity, email: String) {
        val isSuperAdmin = Constants.SUPER_ADMIN_EMAILS.contains(email)
        val data = hashMapOf(
            "status" to if (isSuperAdmin) STATUS_APPROVED else STATUS_PENDING,
            "role" to if (isSuperAdmin) ROLE_SUPERADMIN else ROLE_USER,
            "stations" to emptyList<String>(),
            "pinHash" to "",
            "createdAt" to System.currentTimeMillis()
        )
        Repo.user(email).set(data)
            .addOnSuccessListener {
                if (isSuperAdmin) goPinSetup(activity) else goPendingApproval(activity)
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, e.message ?: "Failed to create profile", Toast.LENGTH_LONG).show()
            }
    }

    private fun goPendingApproval(activity: Activity) {
        activity.startActivity(Intent(activity, PendingApprovalActivity::class.java))
        activity.finish()
    }

    private fun goPinSetup(activity: Activity) {
        activity.startActivity(Intent(activity, PinSetupActivity::class.java))
        activity.finish()
    }

    private fun goPinUnlock(activity: Activity) {
        activity.startActivity(Intent(activity, PinUnlockActivity::class.java))
        activity.finish()
    }

    fun goHome(activity: Activity) {
        activity.startActivity(Intent(activity, StationListActivity::class.java))
        activity.finish()
    }
}

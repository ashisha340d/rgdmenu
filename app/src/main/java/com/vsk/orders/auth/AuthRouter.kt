package com.vsk.orders.auth

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestoreException
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

    // Firebase Auth normalises addresses to lower case, so everything that
    // keys off an email (Firestore doc IDs, the super-admin allowlist, the
    // security rules' request.auth.token.email comparison) has to agree on
    // that spelling or the user ends up with an orphaned profile they aren't
    // allowed to read back.
    fun normalize(email: String): String = email.trim().lowercase()

    fun route(activity: Activity, rawEmail: String, onError: ((String) -> Unit)? = null) {
        val email = normalize(rawEmail)
        Repo.user(email).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    createProfileThenRoute(activity, email, onError)
                    return@addOnSuccessListener
                }
                val user = doc.toAppUser()
                when {
                    user == null || !user.isApproved -> goPendingApproval(activity)
                    !user.hasPin -> goPinSetup(activity)
                    else -> goPinUnlock(activity)
                }
            }
            .addOnFailureListener { e -> report(activity, e, "load your profile", onError) }
    }

    private fun createProfileThenRoute(activity: Activity, email: String, onError: ((String) -> Unit)?) {
        val isSuperAdmin = Constants.SUPER_ADMIN_EMAILS.contains(email)
        val data = hashMapOf(
            "status" to if (isSuperAdmin) STATUS_APPROVED else STATUS_PENDING,
            "role" to if (isSuperAdmin) ROLE_SUPERADMIN else ROLE_USER,
            // A bootstrap super admin is assigned to every station up front so
            // they land on a usable board instead of an empty station picker.
            "stations" to if (isSuperAdmin) Constants.SEED_STATIONS.map { it.id } else emptyList(),
            "pinHash" to "",
            "createdAt" to System.currentTimeMillis()
        )
        Repo.user(email).set(data)
            .addOnSuccessListener {
                if (isSuperAdmin) goPinSetup(activity) else goPendingApproval(activity)
            }
            .addOnFailureListener { e -> report(activity, e, "create your profile", onError) }
    }

    // Turns a raw Firestore exception into something that points at the actual
    // fix, since "PERMISSION_DENIED" on a fresh project almost always means the
    // security rules from firestore.rules were never published.
    fun explain(e: Exception, action: String): String {
        val code = (e as? FirebaseFirestoreException)?.code
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Couldn't $action: the database rejected the request. " +
                    "Publish the rules from firestore.rules in the Firebase console " +
                    "(Firestore Database → Rules → Publish), then try again."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Couldn't $action: no connection to the database. Check your internet and try again."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Couldn't $action: the Firestore database hasn't been created yet. " +
                    "Create it in the Firebase console (Firestore Database → Create database)."
            else -> "Couldn't $action: ${e.message ?: "unknown error"}"
        }
    }

    private fun report(activity: Activity, e: Exception, action: String, onError: ((String) -> Unit)?) {
        val message = explain(e, action)
        if (onError != null) onError(message)
        else Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
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

package com.vsk.orders.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ListenerRegistration
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toAppUser
import com.vsk.orders.databinding.ActivityPendingApprovalBinding

class PendingApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingApprovalBinding
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSignOut.setOnClickListener {
            Repo.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val email = Repo.currentEmail
        if (email == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        listenerRegistration = Repo.user(email).addSnapshotListener { doc, error ->
            if (error != null) {
                binding.textError.text = AuthRouter.explain(error, "check your approval status")
                binding.textError.visibility = android.view.View.VISIBLE
                return@addSnapshotListener
            }
            binding.textError.visibility = android.view.View.GONE
            val user = doc?.toAppUser() ?: return@addSnapshotListener
            if (user.isApproved) {
                AuthRouter.route(this, email)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }
}

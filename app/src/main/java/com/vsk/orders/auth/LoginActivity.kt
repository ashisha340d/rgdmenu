package com.vsk.orders.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = Repo.currentEmail
        if (email != null) {
            AuthRouter.route(this, email)
            return
        }

        binding.buttonSignIn.setOnClickListener { submit(isSignUp = false) }
        binding.buttonSignUp.setOnClickListener { submit(isSignUp = true) }
    }

    private fun submit(isSignUp: Boolean) {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Enter email and password")
            return
        }

        setLoading(true)
        val task = if (isSignUp) {
            Repo.auth.createUserWithEmailAndPassword(email, password)
        } else {
            Repo.auth.signInWithEmailAndPassword(email, password)
        }
        task.addOnSuccessListener {
            setLoading(false)
            AuthRouter.route(this, email)
        }.addOnFailureListener { e ->
            setLoading(false)
            showError(e.message ?: "Authentication failed")
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.buttonSignIn.isEnabled = !loading
        binding.buttonSignUp.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = android.view.View.VISIBLE
    }
}

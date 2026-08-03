package com.vsk.orders.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Wire the form up before anything else. An earlier version returned
        // early when a session already existed, which left the buttons dead if
        // the profile lookup then failed — no way forward and no way out.
        binding.buttonSignIn.setOnClickListener { submit(isSignUp = false) }
        binding.buttonSignUp.setOnClickListener { submit(isSignUp = true) }
        binding.buttonSignOut.setOnClickListener {
            Repo.auth.signOut()
            binding.buttonSignOut.visibility = View.GONE
            binding.textError.visibility = View.GONE
            binding.editEmail.setText("")
            binding.editPassword.setText("")
        }

        // Already signed in from a previous launch — try to resume, but leave
        // the form usable if that lookup fails.
        val email = Repo.currentEmail
        if (email != null) {
            binding.editEmail.setText(email)
            setLoading(true)
            AuthRouter.route(this, email) { message ->
                setLoading(false)
                showError(message)
                // Stale session the app can't get past: let them start over.
                binding.buttonSignOut.visibility = View.VISIBLE
            }
        }
    }

    private fun submit(isSignUp: Boolean) {
        val email = AuthRouter.normalize(binding.editEmail.text.toString())
        val password = binding.editPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Enter email and password")
            return
        }
        if (isSignUp && password.length < 6) {
            showError("Password must be at least 6 characters")
            return
        }

        setLoading(true)
        val task = if (isSignUp) {
            Repo.auth.createUserWithEmailAndPassword(email, password)
        } else {
            Repo.auth.signInWithEmailAndPassword(email, password)
        }
        task.addOnSuccessListener {
            AuthRouter.route(this, email) { message ->
                setLoading(false)
                showError(message)
                binding.buttonSignOut.visibility = View.VISIBLE
            }
        }.addOnFailureListener { e ->
            setLoading(false)
            showError(signInError(e, isSignUp))
        }
    }

    // Firebase collapses "no such account" and "wrong password" into one
    // opaque credential error, which reads as a bug the first time you sign up.
    private fun signInError(e: Exception, isSignUp: Boolean): String {
        val raw = e.message ?: "Authentication failed"
        return when {
            !isSignUp && raw.contains("credential is incorrect", ignoreCase = true) ->
                "Wrong password — or there's no account for this email yet. " +
                    "If this is your first time, tap Create Account instead."
            raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Email/password sign-in isn't enabled for this Firebase project. " +
                    "Enable it in the console under Authentication → Sign-in method."
            raw.contains("email address is already in use", ignoreCase = true) ->
                "An account already exists for this email — tap Sign In instead."
            else -> raw
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSignIn.isEnabled = !loading
        binding.buttonSignUp.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }
}

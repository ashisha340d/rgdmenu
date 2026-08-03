package com.vsk.orders.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vsk.orders.R
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toAppUser
import com.vsk.orders.databinding.ActivityPinUnlockBinding

class PinUnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinUnlockBinding
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentEmail = Repo.currentEmail
        if (currentEmail == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        email = currentEmail
        binding.textGreeting.text = getString(R.string.title_enter_pin_for, email)

        binding.buttonUnlock.setOnClickListener { attemptUnlock() }
        binding.buttonForgotPin.setOnClickListener { forgotPin() }
        binding.buttonSignOut.setOnClickListener {
            Repo.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun attemptUnlock() {
        val pin = binding.editPin.text.toString()
        if (pin.length != 4) {
            showError("Enter your 4-digit PIN")
            return
        }
        Repo.user(email).get()
            .addOnSuccessListener { doc ->
                val user = doc.toAppUser()
                if (user != null && PinUtil.hash(pin, email) == user.pinHash) {
                    AuthRouter.goHome(this)
                } else {
                    showError("Incorrect PIN")
                }
            }
            .addOnFailureListener { e -> showError(AuthRouter.explain(e, "verify your PIN")) }
    }

    private fun forgotPin() {
        Repo.user(email).update("pinHash", "")
            .addOnSuccessListener {
                startActivity(Intent(this, PinSetupActivity::class.java))
                finish()
            }
            .addOnFailureListener { e -> showError(AuthRouter.explain(e, "reset your PIN")) }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = android.view.View.VISIBLE
    }
}

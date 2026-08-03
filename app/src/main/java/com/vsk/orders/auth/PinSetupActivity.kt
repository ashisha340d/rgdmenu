package com.vsk.orders.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ActivityPinSetupBinding

class PinSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = Repo.currentEmail
        if (email == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.buttonSavePin.setOnClickListener {
            val pin = binding.editPin.text.toString()
            val confirm = binding.editPinConfirm.text.toString()
            if (pin.length != 4) {
                showError("PIN must be 4 digits")
                return@setOnClickListener
            }
            if (pin != confirm) {
                showError("PINs don't match")
                return@setOnClickListener
            }
            Repo.user(email).update("pinHash", PinUtil.hash(pin, email))
                .addOnSuccessListener { AuthRouter.goHome(this) }
                .addOnFailureListener { e -> showError(AuthRouter.explain(e, "save your PIN")) }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = android.view.View.VISIBLE
    }
}

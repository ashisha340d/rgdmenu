package com.vsk.orders.order

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.vsk.orders.R
import com.vsk.orders.audio.VoicePlayer
import com.vsk.orders.data.Order
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toAppUser
import com.vsk.orders.data.toOrder
import com.vsk.orders.databinding.ActivityOrderDetailBinding
import com.vsk.orders.databinding.ItemOrderDetailRowBinding
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private lateinit var stationId: String
    private lateinit var orderId: String
    private var isAdmin = false
    private var currentOrder: Order? = null
    private var listenerRegistration: ListenerRegistration? = null
    private val voicePlayer = VoicePlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_order_detail)

        stationId = intent.getStringExtra(EXTRA_STATION_ID) ?: run { finish(); return }
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: run { finish(); return }

        binding.buttonAcknowledge.setOnClickListener { toggleAcknowledge() }
        binding.buttonMarkDone.setOnClickListener { markDone() }
        binding.buttonSaveQuantities.setOnClickListener { saveQuantities() }
        binding.buttonPlayVoiceNote.setOnClickListener { playVoiceNote() }
        binding.buttonForwardWhatsApp.setOnClickListener { forwardToWhatsApp() }

        checkAdminThenListen()
    }

    override fun onDestroy() {
        super.onDestroy()
        voicePlayer.stop()
        listenerRegistration?.remove()
    }

    private fun checkAdminThenListen() {
        val email = Repo.currentEmail
        if (email == null) {
            listenForOrder()
            return
        }
        Repo.user(email).get().addOnSuccessListener { doc ->
            isAdmin = doc.toAppUser()?.isAdmin == true
            invalidateOptionsMenu()
            listenForOrder()
        }.addOnFailureListener {
            listenForOrder()
        }
    }

    private fun listenForOrder() {
        listenerRegistration = Repo.order(stationId, orderId).addSnapshotListener { doc, _ ->
            val order = doc?.toOrder() ?: return@addSnapshotListener
            currentOrder = order
            renderOrder(order)
        }
    }

    private fun renderOrder(order: Order) {
        val format = SimpleDateFormat("EEEE h:mm a : d/M/yyyy", Locale.getDefault())
        binding.textDateTime.text = format.format(java.util.Date(order.orderTimeMillis))
        binding.textEventType.text = order.eventType
        binding.textLocation.text = order.location
        binding.textContact.text = order.contactName
        binding.textPax.text = if (order.pax.isNotBlank()) "Pax: ${order.pax}" else ""

        binding.buttonPlayVoiceNote.visibility = if (order.voiceNoteUrl.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE

        binding.itemsContainer.removeAllViews()
        order.items.forEach { item ->
            val rowBinding = ItemOrderDetailRowBinding.inflate(LayoutInflater.from(this), binding.itemsContainer, false)
            rowBinding.textItemName.text = item.name
            rowBinding.textItemQty.text = "Requested: ${item.qty}"
            rowBinding.editAdminQty.setText(item.adminQty)
            rowBinding.editAdminQty.isEnabled = isAdmin
            rowBinding.editAdminQty.hint = getString(R.string.hint_admin_qty)
            binding.itemsContainer.addView(rowBinding.root)
        }
        binding.buttonSaveQuantities.visibility = if (isAdmin) android.view.View.VISIBLE else android.view.View.GONE

        binding.textAcknowledgedBy.text = if (order.acknowledgedBy.isEmpty()) {
            getString(R.string.label_not_acknowledged)
        } else {
            getString(R.string.label_acknowledged_by, order.acknowledgedBy.joinToString(", "))
        }
        val email = Repo.currentEmail
        val alreadyAcked = email != null && order.acknowledgedBy.contains(email)
        binding.buttonAcknowledge.text = if (alreadyAcked) {
            getString(R.string.action_unacknowledge)
        } else {
            getString(R.string.action_acknowledge)
        }

        binding.textDoneStatus.text = if (order.done) {
            getString(R.string.label_done_by, order.doneBy)
        } else {
            getString(R.string.label_not_done)
        }
        binding.buttonMarkDone.visibility = if (isAdmin && !order.done) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun toggleAcknowledge() {
        val order = currentOrder ?: return
        val email = Repo.currentEmail ?: return
        val alreadyAcked = order.acknowledgedBy.contains(email)
        val update = if (alreadyAcked) FieldValue.arrayRemove(email) else FieldValue.arrayUnion(email)
        Repo.order(stationId, orderId).update("acknowledgedBy", update)
    }

    private fun markDone() {
        val email = Repo.currentEmail ?: return
        Repo.order(stationId, orderId).update(mapOf("done" to true, "doneBy" to email))
    }

    private fun playVoiceNote() {
        val url = currentOrder?.voiceNoteUrl ?: return
        if (url.isBlank()) return
        voicePlayer.play(url) {}
    }

    private fun forwardToWhatsApp() {
        val order = currentOrder ?: return
        val format = SimpleDateFormat("EEE h:mm a : d/M/yyyy", Locale.getDefault())
        val itemLines = order.items.joinToString("\n") { item ->
            val qty = if (item.adminQty.isNotBlank()) item.adminQty else item.qty
            "- ${item.name}${if (qty.isNotBlank()) " ($qty)" else ""}"
        }
        val text = buildString {
            append(order.eventType).append("\n")
            append(format.format(java.util.Date(order.orderTimeMillis))).append("\n")
            if (order.location.isNotBlank()) append(order.location).append("\n")
            if (order.contactName.isNotBlank()) append(order.contactName).append("\n")
            if (order.pax.isNotBlank()) append("Pax: ${order.pax}").append("\n")
            append("\n").append(itemLines)
        }

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.setPackage("com.whatsapp")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            intent.setPackage(null)
            startActivity(Intent.createChooser(intent, getString(R.string.action_forward_whatsapp)))
        }
    }

    private fun saveQuantities() {
        val order = currentOrder ?: return
        val items = mutableListOf<Map<String, String>>()
        for (i in 0 until binding.itemsContainer.childCount) {
            val row = binding.itemsContainer.getChildAt(i)
            val adminQty = row.findViewById<android.widget.EditText>(R.id.editAdminQty).text.toString().trim()
            val original = order.items.getOrNull(i)
            if (original != null) {
                items.add(mapOf("name" to original.name, "qty" to original.qty, "adminQty" to adminQty))
            }
        }
        Repo.order(stationId, orderId).update("items", items)
            .addOnSuccessListener { Toast.makeText(this, R.string.msg_quantities_saved, Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, e.message ?: "Failed to save", Toast.LENGTH_LONG).show() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.order_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_order -> {
                val i = Intent(this, OrderEditActivity::class.java)
                i.putExtra(OrderEditActivity.EXTRA_STATION_ID, stationId)
                i.putExtra(OrderEditActivity.EXTRA_ORDER_ID, orderId)
                startActivity(i)
                true
            }
            R.id.action_delete_order -> {
                confirmDelete()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.action_delete)
            .setMessage(R.string.msg_confirm_delete)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                Repo.order(stationId, orderId).delete()
                finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_STATION_ID = "station_id"
        const val EXTRA_ORDER_ID = "order_id"
    }
}

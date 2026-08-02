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
import com.vsk.orders.data.Order
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toOrder
import com.vsk.orders.databinding.ActivityOrderDetailBinding
import com.vsk.orders.databinding.ItemOrderDetailRowBinding
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private lateinit var groupId: String
    private lateinit var orderId: String
    private var isAdmin = false
    private var currentOrder: Order? = null
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_order_detail)

        groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: run { finish(); return }
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: run { finish(); return }

        binding.buttonAcknowledge.setOnClickListener { toggleAcknowledge() }
        binding.buttonMarkServed.setOnClickListener { markServed() }
        binding.buttonSaveQuantities.setOnClickListener { saveQuantities() }

        checkAdminThenListen()
    }

    private fun checkAdminThenListen() {
        val email = Repo.currentEmail
        Repo.group(groupId).get().addOnSuccessListener { doc ->
            @Suppress("UNCHECKED_CAST")
            val admins = (doc.get("admins") as? List<String>) ?: emptyList()
            isAdmin = email != null && admins.contains(email)
            invalidateOptionsMenu()
            listenForOrder()
        }.addOnFailureListener {
            listenForOrder()
        }
    }

    private fun listenForOrder() {
        listenerRegistration = Repo.order(groupId, orderId).addSnapshotListener { doc, _ ->
            val order = doc?.toOrder() ?: return@addSnapshotListener
            currentOrder = order
            renderOrder(order)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    private fun renderOrder(order: Order) {
        val format = SimpleDateFormat("EEEE h:mm a : d/M/yyyy", Locale.getDefault())
        binding.textDateTime.text = format.format(java.util.Date(order.orderTimeMillis))
        binding.textEventType.text = order.eventType
        binding.textLocation.text = order.location
        binding.textContact.text = order.contactName
        binding.textPax.text = if (order.pax.isNotBlank()) "Pax: ${order.pax}" else ""

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

        binding.textServedStatus.text = if (order.served) {
            getString(R.string.label_served_by, order.servedBy)
        } else {
            getString(R.string.label_not_served)
        }
        binding.buttonMarkServed.visibility = if (isAdmin && !order.served) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun toggleAcknowledge() {
        val order = currentOrder ?: return
        val email = Repo.currentEmail ?: return
        val alreadyAcked = order.acknowledgedBy.contains(email)
        val update = if (alreadyAcked) FieldValue.arrayRemove(email) else FieldValue.arrayUnion(email)
        Repo.order(groupId, orderId).update("acknowledgedBy", update)
    }

    private fun markServed() {
        val email = Repo.currentEmail ?: return
        Repo.order(groupId, orderId).update(mapOf("served" to true, "servedBy" to email))
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
        Repo.order(groupId, orderId).update("items", items)
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
                i.putExtra(OrderEditActivity.EXTRA_GROUP_ID, groupId)
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
                Repo.order(groupId, orderId).delete()
                finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_ORDER_ID = "order_id"
    }
}

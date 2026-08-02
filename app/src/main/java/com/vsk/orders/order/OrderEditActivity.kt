package com.vsk.orders.order

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vsk.orders.R
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toOrder
import com.vsk.orders.databinding.ActivityOrderEditBinding
import com.vsk.orders.databinding.ItemOrderEditRowBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrderEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderEditBinding
    private lateinit var groupId: String
    private var orderId: String? = null
    private var selectedTimeMillis: Long = System.currentTimeMillis()
    private var loadedItems: List<com.vsk.orders.data.OrderItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: run { finish(); return }
        orderId = intent.getStringExtra(EXTRA_ORDER_ID)

        supportActionBar?.title = if (orderId == null) getString(R.string.title_add_order) else getString(R.string.title_edit_order)
        updateDateTimeButton()

        binding.buttonPickDateTime.setOnClickListener { pickDateTime() }
        binding.buttonAddItem.setOnClickListener { addItemRow("", "") }
        binding.buttonSaveOrder.setOnClickListener { save() }

        if (orderId != null) {
            loadOrder(orderId!!)
        } else {
            addItemRow("", "")
        }
    }

    private fun loadOrder(id: String) {
        Repo.order(groupId, id).get().addOnSuccessListener { doc ->
            val order = doc.toOrder() ?: return@addOnSuccessListener
            binding.editEventType.setText(order.eventType)
            binding.editLocation.setText(order.location)
            binding.editContactName.setText(order.contactName)
            binding.editPax.setText(order.pax)
            selectedTimeMillis = order.orderTimeMillis
            loadedItems = order.items
            updateDateTimeButton()

            binding.itemsContainer.removeAllViews()
            if (order.items.isEmpty()) {
                addItemRow("", "")
            } else {
                order.items.forEach { addItemRow(it.name, it.qty) }
            }
        }
    }

    private fun addItemRow(name: String, qty: String) {
        val rowBinding = ItemOrderEditRowBinding.inflate(LayoutInflater.from(this), binding.itemsContainer, false)
        rowBinding.editItemName.setText(name)
        rowBinding.editItemQty.setText(qty)
        rowBinding.buttonRemoveItem.setOnClickListener {
            binding.itemsContainer.removeView(rowBinding.root)
        }
        binding.itemsContainer.addView(rowBinding.root)
    }

    private fun pickDateTime() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedTimeMillis
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                selectedTimeMillis = cal.timeInMillis
                updateDateTimeButton()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateTimeButton() {
        val format = SimpleDateFormat("EEE h:mm a : d/M/yyyy", Locale.getDefault())
        binding.buttonPickDateTime.text = format.format(java.util.Date(selectedTimeMillis))
    }

    private fun collectItems(): List<Map<String, String>> {
        val items = mutableListOf<Map<String, String>>()
        for (i in 0 until binding.itemsContainer.childCount) {
            val row = binding.itemsContainer.getChildAt(i)
            val name = row.findViewById<EditText>(R.id.editItemName).text.toString().trim()
            val qty = row.findViewById<EditText>(R.id.editItemQty).text.toString().trim()
            if (name.isNotEmpty()) {
                val adminQty = loadedItems.firstOrNull { it.name == name }?.adminQty ?: ""
                items.add(mapOf("name" to name, "qty" to qty, "adminQty" to adminQty))
            }
        }
        return items
    }

    private fun save() {
        val eventType = binding.editEventType.text.toString().trim()
        val location = binding.editLocation.text.toString().trim()
        val contactName = binding.editContactName.text.toString().trim()
        val pax = binding.editPax.text.toString().trim()
        val items = collectItems()
        val email = Repo.currentEmail ?: return

        if (eventType.isEmpty() || items.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_required, Toast.LENGTH_LONG).show()
            return
        }

        val id = orderId
        if (id == null) {
            val data = hashMapOf(
                "eventType" to eventType,
                "location" to location,
                "contactName" to contactName,
                "pax" to pax,
                "orderTimeMillis" to selectedTimeMillis,
                "items" to items,
                "createdBy" to email,
                "createdAt" to System.currentTimeMillis(),
                "acknowledgedBy" to emptyList<String>(),
                "served" to false,
                "servedBy" to ""
            )
            Repo.orders(groupId).add(data)
                .addOnSuccessListener { finish() }
                .addOnFailureListener { e -> Toast.makeText(this, e.message ?: "Failed to save", Toast.LENGTH_LONG).show() }
        } else {
            val data = mapOf(
                "eventType" to eventType,
                "location" to location,
                "contactName" to contactName,
                "pax" to pax,
                "orderTimeMillis" to selectedTimeMillis,
                "items" to items
            )
            Repo.order(groupId, id).update(data)
                .addOnSuccessListener { finish() }
                .addOnFailureListener { e -> Toast.makeText(this, e.message ?: "Failed to save", Toast.LENGTH_LONG).show() }
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_ORDER_ID = "order_id"
    }
}

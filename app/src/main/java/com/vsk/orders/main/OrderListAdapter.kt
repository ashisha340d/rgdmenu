package com.vsk.orders.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.vsk.orders.R
import com.vsk.orders.data.Order
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Locale

class OrderListAdapter(
    private val groupId: String,
    private val isAdmin: Boolean,
    private val orders: MutableList<Order> = mutableListOf(),
    private val onOpen: (Order) -> Unit,
    private val onEdit: (Order) -> Unit
) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>() {

    class OrderViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    private val dateFormat = SimpleDateFormat("EEE h:mm a : d/M/yyyy", Locale.getDefault())

    fun submitList(newOrders: List<Order>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        val b = holder.binding
        b.textDateTime.text = dateFormat.format(java.util.Date(order.orderTimeMillis))
        b.textEventType.text = order.eventType
        b.textLocation.text = order.location
        b.textContact.text = order.contactName

        if (order.pax.isNotBlank()) {
            b.textPax.visibility = android.view.View.VISIBLE
            b.textPax.text = "Pax: ${order.pax}"
        } else {
            b.textPax.visibility = android.view.View.GONE
        }

        b.textItemsSummary.text = order.items.joinToString("\n") { item ->
            val qty = if (item.adminQty.isNotBlank()) item.adminQty else item.qty
            "• ${item.name}${if (qty.isNotBlank()) " - $qty" else ""}"
        }

        b.textAckCount.text = if (order.acknowledgedBy.isEmpty()) {
            holder.binding.root.context.getString(R.string.label_not_acknowledged)
        } else {
            holder.binding.root.context.getString(R.string.label_acknowledged_count, order.acknowledgedBy.size)
        }

        b.textServedBadge.visibility = if (order.served) android.view.View.VISIBLE else android.view.View.GONE

        val email = Repo.currentEmail
        val alreadyAcked = email != null && order.acknowledgedBy.contains(email)
        b.buttonAcknowledge.text = if (alreadyAcked) {
            holder.binding.root.context.getString(R.string.action_unacknowledge)
        } else {
            holder.binding.root.context.getString(R.string.action_acknowledge)
        }
        b.buttonAcknowledge.setOnClickListener {
            toggleAcknowledge(order, alreadyAcked)
        }

        b.buttonMarkServed.visibility = if (isAdmin && !order.served) android.view.View.VISIBLE else android.view.View.GONE
        b.buttonMarkServed.setOnClickListener { markServed(order) }

        b.buttonEdit.setOnClickListener { onEdit(order) }
        b.buttonDelete.setOnClickListener { confirmDelete(b.root.context, order) }
        b.rowClickArea.setOnClickListener { onOpen(order) }
    }

    override fun getItemCount(): Int = orders.size

    private fun toggleAcknowledge(order: Order, currentlyAcked: Boolean) {
        val email = Repo.currentEmail ?: return
        val update = if (currentlyAcked) FieldValue.arrayRemove(email) else FieldValue.arrayUnion(email)
        Repo.order(groupId, order.id).update("acknowledgedBy", update)
    }

    private fun markServed(order: Order) {
        val email = Repo.currentEmail ?: return
        Repo.order(groupId, order.id).update(
            mapOf("served" to true, "servedBy" to email)
        )
    }

    private fun confirmDelete(context: android.content.Context, order: Order) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.action_delete)
            .setMessage(R.string.msg_confirm_delete)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                Repo.order(groupId, order.id).delete()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}

package com.vsk.orders.main

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.vsk.orders.R
import com.vsk.orders.audio.VoicePlayer
import com.vsk.orders.data.Order
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ItemOrderBinding
import com.vsk.orders.databinding.ItemSectionHeaderBinding
import java.text.SimpleDateFormat
import java.util.Locale

private const val TYPE_HEADER = 0
private const val TYPE_ORDER = 1

private sealed class Row {
    data class Header(val title: String) : Row()
    data class OrderRow(val order: Order) : Row()
}

class OrderListAdapter(
    private val stationId: String,
    private val isAdmin: Boolean,
    private val onOpen: (Order) -> Unit,
    private val onEdit: (Order) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<Row>()
    private val dateFormat = SimpleDateFormat("EEE h:mm a : d/M/yyyy", Locale.getDefault())
    private val voicePlayer = VoicePlayer()

    class HeaderViewHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class OrderViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    /** Flat list, no section headers — used for order history. */
    fun submitFlat(orders: List<Order>) {
        rows.clear()
        orders.forEach { rows.add(Row.OrderRow(it)) }
        notifyDataSetChanged()
    }

    /** Section title -> orders. Empty sections are skipped entirely. */
    fun submitSections(sections: List<Pair<String, List<Order>>>) {
        rows.clear()
        sections.forEach { (title, orders) ->
            if (orders.isNotEmpty()) {
                rows.add(Row.Header(title))
                orders.forEach { rows.add(Row.OrderRow(it)) }
            }
        }
        notifyDataSetChanged()
    }

    val isEmpty: Boolean get() = rows.isEmpty()

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is Row.Header -> TYPE_HEADER
            is Row.OrderRow -> TYPE_ORDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            OrderViewHolder(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderViewHolder).binding.textSectionHeader.text = row.title
            is Row.OrderRow -> bindOrder((holder as OrderViewHolder).binding, row.order)
        }
    }

    private fun bindOrder(b: ItemOrderBinding, order: Order) {
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
            b.root.context.getString(R.string.label_not_acknowledged)
        } else {
            b.root.context.getString(R.string.label_acknowledged_count, order.acknowledgedBy.size)
        }

        b.textDoneBadge.visibility = if (order.done) android.view.View.VISIBLE else android.view.View.GONE

        val email = Repo.currentEmail
        val alreadyAcked = email != null && order.acknowledgedBy.contains(email)
        b.buttonAcknowledge.text = if (alreadyAcked) {
            b.root.context.getString(R.string.action_unacknowledge)
        } else {
            b.root.context.getString(R.string.action_acknowledge)
        }
        b.buttonAcknowledge.setOnClickListener { toggleAcknowledge(order, alreadyAcked) }

        b.buttonMarkDone.visibility = if (isAdmin && !order.done) android.view.View.VISIBLE else android.view.View.GONE
        b.buttonMarkDone.setOnClickListener { markDone(order) }

        b.buttonEdit.setOnClickListener { onEdit(order) }
        b.buttonDelete.setOnClickListener { confirmDelete(b.root.context, order) }
        b.rowClickArea.setOnClickListener { onOpen(order) }

        b.buttonPlayVoice.visibility = if (order.voiceNoteUrl.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
        b.buttonPlayVoice.setOnClickListener { voicePlayer.play(order.voiceNoteUrl) {} }

        b.buttonForward.setOnClickListener { forwardToWhatsApp(b.root.context, order) }
    }

    override fun getItemCount(): Int = rows.size

    private fun toggleAcknowledge(order: Order, currentlyAcked: Boolean) {
        val email = Repo.currentEmail ?: return
        val update = if (currentlyAcked) FieldValue.arrayRemove(email) else FieldValue.arrayUnion(email)
        Repo.order(stationId, order.id).update("acknowledgedBy", update)
    }

    private fun markDone(order: Order) {
        val email = Repo.currentEmail ?: return
        Repo.order(stationId, order.id).update(
            mapOf("done" to true, "doneBy" to email)
        )
    }

    private fun forwardToWhatsApp(context: android.content.Context, order: Order) {
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
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_forward_whatsapp)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun confirmDelete(context: android.content.Context, order: Order) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.action_delete)
            .setMessage(R.string.msg_confirm_delete)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                Repo.order(stationId, order.id).delete()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}

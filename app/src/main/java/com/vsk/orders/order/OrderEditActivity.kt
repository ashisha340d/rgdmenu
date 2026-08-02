package com.vsk.orders.order

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vsk.orders.R
import com.vsk.orders.audio.VoicePlayer
import com.vsk.orders.audio.VoiceRecorder
import com.vsk.orders.data.OrderItem
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toOrder
import com.vsk.orders.databinding.ActivityOrderEditBinding
import com.vsk.orders.databinding.ItemOrderEditRowBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrderEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderEditBinding
    private lateinit var stationId: String
    private var orderId: String? = null
    private var selectedTimeMillis: Long = System.currentTimeMillis()
    private var loadedItems: List<OrderItem> = emptyList()

    private var existingVoiceNoteUrl: String = ""
    private var recordedVoiceFile: java.io.File? = null
    private var isRecording = false
    private val voiceRecorder by lazy { VoiceRecorder(this) }
    private val voicePlayer = VoicePlayer()

    private val requestMicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(this, R.string.error_mic_permission, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        stationId = intent.getStringExtra(EXTRA_STATION_ID) ?: run { finish(); return }
        orderId = intent.getStringExtra(EXTRA_ORDER_ID)

        supportActionBar?.title = if (orderId == null) getString(R.string.title_add_order) else getString(R.string.title_edit_order)
        updateDateTimeButton()

        binding.buttonPickDateTime.setOnClickListener { pickDateTime() }
        binding.buttonAddItem.setOnClickListener { addItemRow("", "") }
        binding.buttonSaveOrder.setOnClickListener { save() }
        binding.buttonRecordVoice.setOnClickListener { toggleRecording() }
        binding.buttonPlayVoice.setOnClickListener { playVoice() }

        if (orderId != null) {
            loadOrder(orderId!!)
        } else {
            addItemRow("", "")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voicePlayer.stop()
        if (isRecording) voiceRecorder.cancel()
    }

    private fun loadOrder(id: String) {
        Repo.order(stationId, id).get().addOnSuccessListener { doc ->
            val order = doc.toOrder() ?: return@addOnSuccessListener
            binding.editEventType.setText(order.eventType)
            binding.editLocation.setText(order.location)
            binding.editContactName.setText(order.contactName)
            binding.editPax.setText(order.pax)
            selectedTimeMillis = order.orderTimeMillis
            loadedItems = order.items
            existingVoiceNoteUrl = order.voiceNoteUrl
            updateVoiceStatus()
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

    private fun toggleRecording() {
        if (isRecording) {
            val file = voiceRecorder.stop()
            isRecording = false
            binding.buttonRecordVoice.text = getString(R.string.action_record_voice_note)
            if (file != null) {
                recordedVoiceFile = file
                updateVoiceStatus()
            }
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) startRecording() else requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecording() {
        voicePlayer.stop()
        voiceRecorder.start()
        isRecording = true
        binding.buttonRecordVoice.text = getString(R.string.action_stop_recording)
        binding.textVoiceStatus.text = getString(R.string.msg_recording)
    }

    private fun updateVoiceStatus() {
        binding.buttonPlayVoice.isEnabled = recordedVoiceFile != null || existingVoiceNoteUrl.isNotBlank()
        binding.textVoiceStatus.text = when {
            recordedVoiceFile != null -> getString(R.string.msg_voice_note_recorded)
            existingVoiceNoteUrl.isNotBlank() -> getString(R.string.msg_voice_note_attached)
            else -> getString(R.string.msg_no_voice_note)
        }
    }

    private fun playVoice() {
        val local = recordedVoiceFile
        val source = if (local != null) local.absolutePath else existingVoiceNoteUrl
        if (source.isBlank()) return
        voicePlayer.play(source) {}
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

        binding.buttonSaveOrder.isEnabled = false

        val id = orderId
        if (id == null) {
            val newRef = Repo.orders(stationId).document()
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
                "done" to false,
                "doneBy" to "",
                "voiceNoteUrl" to ""
            )
            newRef.set(data)
                .addOnSuccessListener { uploadVoiceNoteIfNeeded(newRef.id) { finish() } }
                .addOnFailureListener { e -> onSaveFailed(e) }
        } else {
            val data = mapOf(
                "eventType" to eventType,
                "location" to location,
                "contactName" to contactName,
                "pax" to pax,
                "orderTimeMillis" to selectedTimeMillis,
                "items" to items
            )
            Repo.order(stationId, id).update(data)
                .addOnSuccessListener { uploadVoiceNoteIfNeeded(id) { finish() } }
                .addOnFailureListener { e -> onSaveFailed(e) }
        }
    }

    private fun onSaveFailed(e: Exception) {
        binding.buttonSaveOrder.isEnabled = true
        Toast.makeText(this, e.message ?: "Failed to save", Toast.LENGTH_LONG).show()
    }

    private fun uploadVoiceNoteIfNeeded(savedOrderId: String, onDone: () -> Unit) {
        val file = recordedVoiceFile
        if (file == null) {
            onDone()
            return
        }
        val ref = Repo.voiceNoteRef(stationId, savedOrderId)
        ref.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    Repo.order(stationId, savedOrderId).update("voiceNoteUrl", url.toString())
                        .addOnCompleteListener { onDone() }
                }.addOnFailureListener { onDone() }
            }
            .addOnFailureListener { onDone() }
    }

    companion object {
        const val EXTRA_STATION_ID = "station_id"
        const val EXTRA_ORDER_ID = "order_id"
    }
}

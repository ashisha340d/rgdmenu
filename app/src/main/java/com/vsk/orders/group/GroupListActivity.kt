package com.vsk.orders.group

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldValue
import com.vsk.orders.R
import com.vsk.orders.auth.LoginActivity
import com.vsk.orders.data.Group
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ActivityGroupListBinding
import com.vsk.orders.main.MainActivity

class GroupListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupListBinding
    private lateinit var adapter: GroupAdapter
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = GroupAdapter { group -> openGroup(group) }
        binding.recyclerGroups.layoutManager = LinearLayoutManager(this)
        binding.recyclerGroups.adapter = adapter

        binding.fabAddGroup.setOnClickListener { showAddGroupChoiceDialog() }
    }

    override fun onStart() {
        super.onStart()
        val email = Repo.currentEmail
        if (email == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        listenerRegistration = Repo.groups()
            .whereArrayContains("members", email)
            .addSnapshotListener { snapshot, _ ->
                val groups = snapshot?.documents?.map { doc ->
                    Group(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        members = (doc.get("members") as? List<String>) ?: emptyList(),
                        admins = (doc.get("admins") as? List<String>) ?: emptyList(),
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                adapter.submitList(groups)
                binding.textEmpty.visibility = if (groups.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.group_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_sign_out) {
            Repo.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openGroup(group: Group) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_GROUP_ID, group.id)
        intent.putExtra(MainActivity.EXTRA_GROUP_NAME, group.name)
        startActivity(intent)
    }

    private fun showAddGroupChoiceDialog() {
        val options = arrayOf(getString(R.string.action_create_group), getString(R.string.action_join_group))
        AlertDialog.Builder(this)
            .setTitle(R.string.action_add_group)
            .setItems(options) { _, which ->
                if (which == 0) showCreateGroupDialog() else showJoinGroupDialog()
            }
            .show()
    }

    private fun showCreateGroupDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.hint_group_name)
        AlertDialog.Builder(this)
            .setTitle(R.string.action_create_group)
            .setView(input)
            .setPositiveButton(R.string.action_create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) createGroup(name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showJoinGroupDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.hint_group_code)
        AlertDialog.Builder(this)
            .setTitle(R.string.action_join_group)
            .setView(input)
            .setPositiveButton(R.string.action_join) { _, _ ->
                val code = input.text.toString().trim().uppercase()
                if (code.isNotEmpty()) joinGroup(code)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun createGroup(name: String) {
        val email = Repo.currentEmail ?: return
        val code = generateGroupCode()
        val group = hashMapOf(
            "name" to name,
            "members" to listOf(email),
            "admins" to listOf(email),
            "createdBy" to email,
            "createdAt" to System.currentTimeMillis()
        )
        Repo.group(code).set(group)
            .addOnSuccessListener { Toast.makeText(this, "Group created. Code: $code", Toast.LENGTH_LONG).show() }
            .addOnFailureListener { e -> Toast.makeText(this, e.message ?: "Failed to create group", Toast.LENGTH_LONG).show() }
    }

    private fun joinGroup(code: String) {
        val email = Repo.currentEmail ?: return
        Repo.group(code).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, R.string.error_group_not_found, Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val members = (doc.get("members") as? List<String>) ?: emptyList()
                if (email in members) {
                    Toast.makeText(this, R.string.msg_already_member, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                Repo.group(code).update("members", FieldValue.arrayUnion(email))
                    .addOnSuccessListener { Toast.makeText(this, R.string.msg_joined_group, Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Toast.makeText(this, e.message ?: "Failed to join group", Toast.LENGTH_LONG).show() }
            }
            .addOnFailureListener { e -> Toast.makeText(this, e.message ?: "Failed to join group", Toast.LENGTH_LONG).show() }
    }

    private fun generateGroupCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet.random() }.joinToString("")
    }
}

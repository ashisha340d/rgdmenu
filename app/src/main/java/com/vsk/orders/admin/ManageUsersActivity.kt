package com.vsk.orders.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vsk.orders.R
import com.vsk.orders.auth.AuthRouter
import com.vsk.orders.auth.PinUtil
import com.vsk.orders.data.AppUser
import com.vsk.orders.data.Constants
import com.vsk.orders.data.ROLE_ADMIN
import com.vsk.orders.data.ROLE_SUPERADMIN
import com.vsk.orders.data.ROLE_USER
import com.vsk.orders.data.Repo
import com.vsk.orders.data.STATUS_APPROVED
import com.vsk.orders.data.STATUS_PENDING
import com.vsk.orders.data.toAppUser
import com.vsk.orders.databinding.ActivityManageUsersBinding
import com.vsk.orders.databinding.DialogAssignUserBinding

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var adapter: ManageUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = ManageUsersAdapter { user -> showAssignDialog(user) }
        binding.recyclerUsers.layoutManager = LinearLayoutManager(this)
        binding.recyclerUsers.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        loadUsers()
    }

    private fun loadUsers() {
        Repo.users().get().addOnSuccessListener { snapshot ->
            val all = snapshot.documents.mapNotNull { it.toAppUser() }
            val pending = all.filter { it.status == STATUS_PENDING }
            val approved = all.filter { it.status == STATUS_APPROVED }
            adapter.submit(pending, approved)
        }.addOnFailureListener { e ->
            Toast.makeText(this, AuthRouter.explain(e, "load users"), Toast.LENGTH_LONG).show()
        }
    }

    private fun showAssignDialog(user: AppUser) {
        val dialogBinding = DialogAssignUserBinding.inflate(LayoutInflater.from(this))
        dialogBinding.checkMangarh.isChecked = user.stations.contains("mangarh")
        dialogBinding.checkVrindavan.isChecked = user.stations.contains("vrindavan")
        dialogBinding.checkBarsana.isChecked = user.stations.contains("barsana")
        dialogBinding.checkMussoorie.isChecked = user.stations.contains("mussoorie")
        when (user.role) {
            ROLE_ADMIN -> dialogBinding.radioRoleAdmin.isChecked = true
            ROLE_SUPERADMIN -> dialogBinding.radioRoleSuperAdmin.isChecked = true
            else -> dialogBinding.radioRoleUser.isChecked = true
        }

        AlertDialog.Builder(this)
            .setTitle(user.email)
            .setView(dialogBinding.root)
            .setPositiveButton(if (user.isApproved) R.string.action_save else R.string.action_approve) { _, _ ->
                val stations = mutableListOf<String>()
                if (dialogBinding.checkMangarh.isChecked) stations.add("mangarh")
                if (dialogBinding.checkVrindavan.isChecked) stations.add("vrindavan")
                if (dialogBinding.checkBarsana.isChecked) stations.add("barsana")
                if (dialogBinding.checkMussoorie.isChecked) stations.add("mussoorie")
                val role = when (dialogBinding.radioRole.checkedRadioButtonId) {
                    dialogBinding.radioRoleAdmin.id -> ROLE_ADMIN
                    dialogBinding.radioRoleSuperAdmin.id -> ROLE_SUPERADMIN
                    else -> ROLE_USER
                }
                Repo.user(user.email).update(
                    mapOf(
                        "status" to STATUS_APPROVED,
                        "role" to role,
                        "stations" to stations
                    )
                ).addOnSuccessListener {
                    loadUsers()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Failed to update user", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.manage_users_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_seed_demo_data) {
            seedDemoData()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun seedDemoData() {
        val allStations = Constants.SEED_STATIONS.map { it.id }
        Constants.SEED_STATIONS.forEach { seed ->
            Repo.station(seed.id).set(mapOf("name" to seed.name))
                .addOnFailureListener { e ->
                    Toast.makeText(this, AuthRouter.explain(e, "create stations"), Toast.LENGTH_LONG).show()
                }
        }
        Constants.DEMO_USER_EMAILS.forEach { email ->
            Repo.user(email).set(
                mapOf(
                    "status" to STATUS_APPROVED,
                    "role" to ROLE_USER,
                    "stations" to allStations,
                    "pinHash" to PinUtil.hash(Constants.DEMO_PIN, email),
                    "createdAt" to System.currentTimeMillis()
                )
            )
        }
        // Profiles created before stations existed have an empty station list,
        // which would drop the seeding admin on an empty station picker.
        Repo.currentEmail?.let { email ->
            Repo.user(email).update("stations", allStations)
        }
        Toast.makeText(this, R.string.msg_demo_data_seeded, Toast.LENGTH_LONG).show()
        loadUsers()
    }
}

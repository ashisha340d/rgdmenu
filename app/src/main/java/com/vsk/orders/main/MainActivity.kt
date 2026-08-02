package com.vsk.orders.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.vsk.orders.R
import com.vsk.orders.auth.LoginActivity
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ActivityMainBinding
import com.vsk.orders.group.GroupListActivity
import com.vsk.orders.order.OrderEditActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var groupId: String
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: run { finish(); return }
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: ""
        supportActionBar?.title = groupName

        binding.fabAddOrder.setOnClickListener {
            val i = Intent(this, OrderEditActivity::class.java)
            i.putExtra(OrderEditActivity.EXTRA_GROUP_ID, groupId)
            startActivity(i)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    showFragment(DashboardFragment.newInstance(groupId, isAdmin))
                    true
                }
                R.id.nav_board -> {
                    showFragment(BoardFragment.newInstance(groupId, isAdmin))
                    true
                }
                else -> false
            }
        }

        loadAdminStatusAndStart()
    }

    private fun loadAdminStatusAndStart() {
        val email = Repo.currentEmail
        Repo.group(groupId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val admins = (doc.get("admins") as? List<String>) ?: emptyList()
                isAdmin = email != null && admins.contains(email)
                showFragment(DashboardFragment.newInstance(groupId, isAdmin))
            }
            .addOnFailureListener {
                showFragment(DashboardFragment.newInstance(groupId, false))
            }
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_switch_group -> {
                startActivity(Intent(this, GroupListActivity::class.java))
                finish()
                true
            }
            R.id.action_sign_out -> {
                Repo.auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_GROUP_NAME = "group_name"
    }
}

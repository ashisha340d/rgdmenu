package com.vsk.orders.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.vsk.orders.R
import com.vsk.orders.admin.ManageUsersActivity
import com.vsk.orders.auth.LoginActivity
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toAppUser
import com.vsk.orders.databinding.ActivityMainBinding
import com.vsk.orders.order.OrderEditActivity
import com.vsk.orders.station.StationListActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var stationId: String
    private var isAdmin: Boolean = false
    private var isSuperAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        stationId = intent.getStringExtra(EXTRA_STATION_ID) ?: run { finish(); return }
        val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: ""
        supportActionBar?.title = stationName

        binding.fabAddOrder.setOnClickListener {
            val i = Intent(this, OrderEditActivity::class.java)
            i.putExtra(OrderEditActivity.EXTRA_STATION_ID, stationId)
            startActivity(i)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_board -> {
                    showFragment(BoardFragment.newInstance(stationId, isAdmin))
                    true
                }
                R.id.nav_history -> {
                    showFragment(HistoryFragment.newInstance(stationId, isAdmin))
                    true
                }
                else -> false
            }
        }

        loadRoleAndStart()
    }

    private fun loadRoleAndStart() {
        val email = Repo.currentEmail
        if (email == null) {
            showFragment(BoardFragment.newInstance(stationId, false))
            return
        }
        Repo.user(email).get()
            .addOnSuccessListener { doc ->
                val user = doc.toAppUser()
                isAdmin = user?.isAdmin == true
                isSuperAdmin = user?.isSuperAdmin == true
                invalidateOptionsMenu()
                showFragment(BoardFragment.newInstance(stationId, isAdmin))
            }
            .addOnFailureListener {
                showFragment(BoardFragment.newInstance(stationId, false))
            }
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.action_manage_users).isVisible = isSuperAdmin
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_manage_users -> {
                startActivity(Intent(this, ManageUsersActivity::class.java))
                true
            }
            R.id.action_switch_station -> {
                startActivity(Intent(this, StationListActivity::class.java))
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
        const val EXTRA_STATION_ID = "station_id"
        const val EXTRA_STATION_NAME = "station_name"
    }
}

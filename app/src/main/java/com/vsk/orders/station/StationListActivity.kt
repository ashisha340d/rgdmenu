package com.vsk.orders.station

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldPath
import com.vsk.orders.R
import com.vsk.orders.admin.ManageUsersActivity
import com.vsk.orders.auth.AuthRouter
import com.vsk.orders.auth.LoginActivity
import com.vsk.orders.data.AppUser
import com.vsk.orders.data.Repo
import com.vsk.orders.data.Station
import com.vsk.orders.data.toAppUser
import com.vsk.orders.data.toStation
import com.vsk.orders.databinding.ActivityStationListBinding
import com.vsk.orders.main.MainActivity

class StationListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationListBinding
    private lateinit var adapter: StationAdapter
    private var currentUser: AppUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = StationAdapter { station -> openStation(station) }
        binding.recyclerStations.layoutManager = LinearLayoutManager(this)
        binding.recyclerStations.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val email = Repo.currentEmail
        if (email == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        Repo.user(email).get()
            .addOnSuccessListener { doc ->
                val user = doc.toAppUser() ?: return@addOnSuccessListener
                currentUser = user
                invalidateOptionsMenu()
                loadStations(user)
            }
            .addOnFailureListener { e -> showEmpty(AuthRouter.explain(e, "load your profile")) }
    }

    private fun loadStations(user: AppUser) {
        if (user.stations.isEmpty()) {
            showEmpty(getString(R.string.msg_no_stations))
            return
        }
        Repo.stations()
            .whereIn(FieldPath.documentId(), user.stations.take(30))
            .get()
            .addOnSuccessListener { snapshot ->
                val stations = snapshot.documents.mapNotNull { it.toStation() }
                if (stations.size == 1) {
                    openStation(stations[0])
                    return@addOnSuccessListener
                }
                adapter.submitList(stations)
                if (stations.isEmpty()) {
                    // Assigned to stations that don't exist yet — a Super Admin
                    // still has to seed them from Manage Users.
                    showEmpty(getString(R.string.msg_stations_missing))
                } else {
                    binding.textEmpty.visibility = android.view.View.GONE
                }
            }
            .addOnFailureListener { e -> showEmpty(AuthRouter.explain(e, "load your stations")) }
    }

    private fun showEmpty(message: String) {
        binding.textEmpty.text = message
        binding.textEmpty.visibility = android.view.View.VISIBLE
    }

    private fun openStation(station: Station) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_STATION_ID, station.id)
        intent.putExtra(MainActivity.EXTRA_STATION_NAME, station.name)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.station_list_menu, menu)
        menu.findItem(R.id.action_manage_users).isVisible = currentUser?.isSuperAdmin == true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_manage_users -> {
                startActivity(Intent(this, ManageUsersActivity::class.java))
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
}

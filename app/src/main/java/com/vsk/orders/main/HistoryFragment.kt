package com.vsk.orders.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.vsk.orders.data.Order
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toOrder
import com.vsk.orders.databinding.FragmentHistoryBinding
import com.vsk.orders.order.OrderDetailActivity
import com.vsk.orders.order.OrderEditActivity
import java.util.Calendar

/** Past orders (before today), most recent first. */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var stationId: String
    private lateinit var adapter: OrderListAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stationId = requireArguments().getString(ARG_STATION_ID)!!
        val isAdmin = requireArguments().getBoolean(ARG_IS_ADMIN)

        adapter = OrderListAdapter(
            stationId = stationId,
            isAdmin = isAdmin,
            onOpen = { order -> openDetail(order) },
            onEdit = { order -> openEdit(order) }
        )
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        listenerRegistration = Repo.orders(stationId)
            .whereLessThan("orderTimeMillis", startOfToday)
            .orderBy("orderTimeMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val orders = snapshot?.documents?.mapNotNull { it.toOrder() } ?: emptyList()
                adapter.submitFlat(orders)
                binding.textEmptyHistory.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openDetail(order: Order) {
        val intent = Intent(requireContext(), OrderDetailActivity::class.java)
        intent.putExtra(OrderDetailActivity.EXTRA_STATION_ID, stationId)
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id)
        startActivity(intent)
    }

    private fun openEdit(order: Order) {
        val intent = Intent(requireContext(), OrderEditActivity::class.java)
        intent.putExtra(OrderEditActivity.EXTRA_STATION_ID, stationId)
        intent.putExtra(OrderEditActivity.EXTRA_ORDER_ID, order.id)
        startActivity(intent)
    }

    companion object {
        private const val ARG_STATION_ID = "station_id"
        private const val ARG_IS_ADMIN = "is_admin"

        fun newInstance(stationId: String, isAdmin: Boolean): HistoryFragment {
            val fragment = HistoryFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_STATION_ID, stationId)
                putBoolean(ARG_IS_ADMIN, isAdmin)
            }
            return fragment
        }
    }
}

package com.vsk.orders.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.vsk.orders.data.Order
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toOrder
import com.vsk.orders.databinding.FragmentBoardBinding
import com.vsk.orders.order.OrderDetailActivity
import com.vsk.orders.order.OrderEditActivity
import java.util.Calendar

/** Combined structured view: Today's orders, then Upcoming orders, in one scroll. */
class BoardFragment : Fragment() {

    private var _binding: FragmentBoardBinding? = null
    private val binding get() = _binding!!
    private lateinit var stationId: String
    private lateinit var adapter: OrderListAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardBinding.inflate(inflater, container, false)
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
        binding.recyclerBoard.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBoard.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val startOfTomorrow = startOfToday + 24 * 60 * 60 * 1000

        listenerRegistration = Repo.orders(stationId)
            .orderBy("orderTimeMillis")
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val all = snapshot?.documents?.mapNotNull { it.toOrder() } ?: emptyList()
                val today = all.filter { it.orderTimeMillis in startOfToday until startOfTomorrow }
                val upcoming = all.filter { it.orderTimeMillis >= startOfTomorrow }
                adapter.submitSections(listOf("Today" to today, "Upcoming" to upcoming))
                binding.textEmptyBoard.visibility = if (adapter.isEmpty) View.VISIBLE else View.GONE
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

        fun newInstance(stationId: String, isAdmin: Boolean): BoardFragment {
            val fragment = BoardFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_STATION_ID, stationId)
                putBoolean(ARG_IS_ADMIN, isAdmin)
            }
            return fragment
        }
    }
}

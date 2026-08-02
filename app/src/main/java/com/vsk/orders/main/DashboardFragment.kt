package com.vsk.orders.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.vsk.orders.data.Order
import com.vsk.orders.data.Repo
import com.vsk.orders.data.toOrder
import com.vsk.orders.databinding.FragmentDashboardBinding
import com.vsk.orders.order.OrderDetailActivity
import com.vsk.orders.order.OrderEditActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var groupId: String
    private lateinit var adapter: OrderListAdapter
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        groupId = requireArguments().getString(ARG_GROUP_ID)!!
        val isAdmin = requireArguments().getBoolean(ARG_IS_ADMIN)

        val headerFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault())
        binding.textTodayHeader.text = "Today: ${headerFormat.format(java.util.Date())}"

        adapter = OrderListAdapter(
            groupId = groupId,
            isAdmin = isAdmin,
            onOpen = { order -> openDetail(order) },
            onEdit = { order -> openEdit(order) }
        )
        binding.recyclerDashboard.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDashboard.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000

        listenerRegistration = Repo.orders(groupId)
            .orderBy("orderTimeMillis")
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val orders = snapshot?.documents?.mapNotNull { it.toOrder() }
                    ?.filter { it.orderTimeMillis in startOfDay until endOfDay }
                    ?: emptyList()
                adapter.submitList(orders)
                binding.textEmptyDashboard.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
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
        val intent = android.content.Intent(requireContext(), OrderDetailActivity::class.java)
        intent.putExtra(OrderDetailActivity.EXTRA_GROUP_ID, groupId)
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id)
        startActivity(intent)
    }

    private fun openEdit(order: Order) {
        val intent = android.content.Intent(requireContext(), OrderEditActivity::class.java)
        intent.putExtra(OrderEditActivity.EXTRA_GROUP_ID, groupId)
        intent.putExtra(OrderEditActivity.EXTRA_ORDER_ID, order.id)
        startActivity(intent)
    }

    companion object {
        private const val ARG_GROUP_ID = "group_id"
        private const val ARG_IS_ADMIN = "is_admin"

        fun newInstance(groupId: String, isAdmin: Boolean): DashboardFragment {
            val fragment = DashboardFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_GROUP_ID, groupId)
                putBoolean(ARG_IS_ADMIN, isAdmin)
            }
            return fragment
        }
    }
}


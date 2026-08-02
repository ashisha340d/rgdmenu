package com.vsk.orders.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vsk.orders.data.AppUser
import com.vsk.orders.databinding.ItemManageUserBinding
import com.vsk.orders.databinding.ItemSectionHeaderBinding

private const val TYPE_HEADER = 0
private const val TYPE_USER = 1

sealed class ManageUserRow {
    data class Header(val title: String) : ManageUserRow()
    data class UserRow(val user: AppUser) : ManageUserRow()
}

class ManageUsersAdapter(
    private val onManage: (AppUser) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<ManageUserRow>()

    class HeaderViewHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class UserViewHolder(val binding: ItemManageUserBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(pending: List<AppUser>, approved: List<AppUser>) {
        rows.clear()
        if (pending.isNotEmpty()) {
            rows.add(ManageUserRow.Header("Pending approval (${pending.size})"))
            pending.forEach { rows.add(ManageUserRow.UserRow(it)) }
        }
        rows.add(ManageUserRow.Header("All users (${approved.size})"))
        approved.forEach { rows.add(ManageUserRow.UserRow(it)) }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is ManageUserRow.Header -> TYPE_HEADER
            is ManageUserRow.UserRow -> TYPE_USER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            UserViewHolder(ItemManageUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is ManageUserRow.Header -> {
                (holder as HeaderViewHolder).binding.textSectionHeader.text = row.title
            }
            is ManageUserRow.UserRow -> {
                val user = row.user
                val b = (holder as UserViewHolder).binding
                b.textUserEmail.text = user.email
                b.textUserInfo.text = if (user.isApproved) {
                    "${user.role} • stations: ${if (user.stations.isEmpty()) "none" else user.stations.joinToString(", ")}"
                } else {
                    "pending approval"
                }
                b.buttonManageUser.text = if (user.isApproved) "Edit" else "Approve"
                b.buttonManageUser.setOnClickListener { onManage(user) }
            }
        }
    }

    override fun getItemCount(): Int = rows.size
}

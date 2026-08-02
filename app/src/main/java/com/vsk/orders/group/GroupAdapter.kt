package com.vsk.orders.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vsk.orders.data.Group
import com.vsk.orders.data.Repo
import com.vsk.orders.databinding.ItemGroupBinding

class GroupAdapter(
    private val groups: MutableList<Group> = mutableListOf(),
    private val onClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root)

    fun submitList(newGroups: List<Group>) {
        groups.clear()
        groups.addAll(newGroups)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.binding.textGroupName.text = group.name
        val isAdmin = Repo.currentEmail != null && group.admins.contains(Repo.currentEmail)
        holder.binding.textGroupRole.text = if (isAdmin) {
            "Admin • Code: ${group.id}"
        } else {
            "Member • Code: ${group.id}"
        }
        holder.binding.root.setOnClickListener { onClick(group) }
    }

    override fun getItemCount(): Int = groups.size
}

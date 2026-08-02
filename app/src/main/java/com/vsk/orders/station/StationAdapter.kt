package com.vsk.orders.station

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vsk.orders.data.Station
import com.vsk.orders.databinding.ItemStationBinding

class StationAdapter(
    private val stations: MutableList<Station> = mutableListOf(),
    private val onClick: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    class StationViewHolder(val binding: ItemStationBinding) : RecyclerView.ViewHolder(binding.root)

    fun submitList(newStations: List<Station>) {
        stations.clear()
        stations.addAll(newStations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val binding = ItemStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]
        holder.binding.textStationName.text = station.name
        holder.binding.root.setOnClickListener { onClick(station) }
    }

    override fun getItemCount(): Int = stations.size
}

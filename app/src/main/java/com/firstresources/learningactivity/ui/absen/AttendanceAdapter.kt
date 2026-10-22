package com.firstresources.learningactivity.ui.absen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firstresources.learningactivity.api.models.AttendanceItem
import com.firstresources.learningactivity.databinding.ItemAttendanceBinding

class AttendanceAdapter(private var items: List<AttendanceItem>) :
    RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAttendanceBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateList(newList: List<AttendanceItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvAttendanceSession.text = "Sesi Pelatihan"
        holder.binding.tvAttendanceDate.text = item.date ?: "2026-10-11"
        holder.binding.tvAttendanceStatusBadge.text = item.status ?: "Hadir"
    }

    override fun getItemCount(): Int = items.size
}

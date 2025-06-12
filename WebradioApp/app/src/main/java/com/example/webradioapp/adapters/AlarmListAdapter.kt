package com.example.webradioapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.webradioapp.R
import com.example.webradioapp.databinding.ListItemAlarmBinding
import com.example.webradioapp.model.Alarm
import java.util.Locale

class AlarmListAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onClick: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmListAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ListItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = getItem(position)
        holder.bind(alarm, onToggle, onClick)
    }

    class AlarmViewHolder(private val binding: ListItemAlarmBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alarm: Alarm, onToggle: (Alarm, Boolean) -> Unit, onClick: (Alarm) -> Unit) {
            binding.tvAlarmTime.text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
            binding.tvAlarmStationName.text = alarm.stationName
            binding.switchAlarmEnabled.isChecked = alarm.isEnabled

            Glide.with(binding.ivAlarmStationIcon.context)
                .load(alarm.stationIconUrl)
                .placeholder(R.drawable.ic_radio_placeholder)
                .error(R.drawable.ic_radio_placeholder)
                .into(binding.ivAlarmStationIcon)

            binding.switchAlarmEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }
            binding.root.setOnClickListener {
                onClick(alarm)
            }
        }
    }

    class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}

package com.example.webradioapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.webradioapp.R
import com.example.webradioapp.adapters.AlarmListAdapter
import com.example.webradioapp.databinding.FragmentAlarmsBinding
import com.example.webradioapp.viewmodel.AlarmViewModel

class AlarmsFragment : Fragment() {

    private var _binding: FragmentAlarmsBinding? = null
    private val binding get() = _binding!!

    private lateinit var alarmViewModel: AlarmViewModel
    private lateinit var alarmListAdapter: AlarmListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alarmViewModel = ViewModelProvider(this).get(AlarmViewModel::class.java)

        setupRecyclerView()

        alarmViewModel.allAlarms.observe(viewLifecycleOwner) { alarms ->
            alarms?.let { alarmListAdapter.submitList(it) }
        }

        binding.fabAddAlarm.setOnClickListener {
            // Navigate to Add/Edit Alarm screen
            val action = AlarmsFragmentDirections.actionAlarmsFragmentToAddEditAlarmFragment(-1, "Add Alarm")
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        alarmListAdapter = AlarmListAdapter(
            onToggle = { alarm, isEnabled ->
                alarmViewModel.updateAlarmEnabled(alarm.id, isEnabled)
                // TODO: Update actual alarm scheduling via AlarmManagerHelper
            },
            onClick = { alarm ->
                // Navigate to Add/Edit Alarm screen with alarm id
                val action = AlarmsFragmentDirections.actionAlarmsFragmentToAddEditAlarmFragment(alarm.id, "Edit Alarm")
                findNavController().navigate(action)
            }
        )
        binding.rvAlarmsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alarmListAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

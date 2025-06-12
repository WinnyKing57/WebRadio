package com.example.webradioapp.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.webradioapp.R
import com.example.webradioapp.databinding.FragmentAddEditAlarmBinding
import com.example.webradioapp.model.Alarm
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.viewmodel.AlarmViewModel
import com.example.webradioapp.viewmodel.StationViewModel
import java.util.Calendar

class AddEditAlarmFragment : Fragment() {

    private var _binding: FragmentAddEditAlarmBinding? = null
    private val binding get() = _binding!!

    private lateinit var alarmViewModel: AlarmViewModel
    private lateinit var stationViewModel: StationViewModel // To get station list
    private val args: AddEditAlarmFragmentArgs by navArgs()

    private var currentAlarm: Alarm? = null
    private var selectedStation: RadioStation? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alarmViewModel = ViewModelProvider(this).get(AlarmViewModel::class.java)
        // Assuming StationViewModel exists and provides a list of all stations
        stationViewModel = ViewModelProvider(requireActivity()).get(StationViewModel::class.java)


        val alarmId = args.alarmId
        if (alarmId != -1) { // Editing existing alarm
            alarmViewModel.getAlarmById(alarmId) { alarm ->
                currentAlarm = alarm
                alarm?.let { populateUi(it) }
            }
            binding.btnDeleteAlarm.visibility = View.VISIBLE
        } else { // Adding new alarm
            binding.btnDeleteAlarm.visibility = View.GONE
            // Set default time (e.g., current time or 8:00 AM)
            val calendar = Calendar.getInstance()
            binding.timePickerAlarm.hour = calendar.get(Calendar.HOUR_OF_DAY)
            binding.timePickerAlarm.minute = calendar.get(Calendar.MINUTE)
        }

        binding.btnSelectStation.setOnClickListener {
            showStationSelectionDialog()
        }
        binding.stationSelectionContainer.setOnClickListener {
            showStationSelectionDialog()
        }


        binding.btnSaveAlarm.setOnClickListener {
            saveAlarm()
        }

        binding.btnDeleteAlarm.setOnClickListener {
            deleteAlarm()
        }
    }

    private fun populateUi(alarm: Alarm) {
        binding.timePickerAlarm.hour = alarm.hour
        binding.timePickerAlarm.minute = alarm.minute
        binding.switchAlarmEnabledDetail.isChecked = alarm.isEnabled
        binding.tvSelectedStationName.text = alarm.stationName
        // We need to fetch the full RadioStation object or at least store its details in Alarm
        // For now, create a placeholder RadioStation
        selectedStation = RadioStation(
            id = alarm.stationId,
            name = alarm.stationName,
            streamUrl = "", // Not strictly needed for this screen, but part of RadioStation
            iconUrl = alarm.stationIconUrl,
            isFavorite = false // Not relevant here
        )
    }

    private fun showStationSelectionDialog() {
        // Observe station list from StationViewModel
        // This is a simplified dialog. A more robust solution might use a RecyclerView in a DialogFragment.
        stationViewModel.allStations.observe(viewLifecycleOwner) { stations ->
            if (stations.isNullOrEmpty()) {
                Toast.makeText(context, "No stations available to select.", Toast.LENGTH_SHORT).show()
                return@observe
            }
            val stationNames = stations.map { it.name }.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Radio Station")
                .setItems(stationNames) { dialog, which ->
                    selectedStation = stations[which]
                    binding.tvSelectedStationName.text = selectedStation?.name
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }
         // Trigger fetching if StationViewModel doesn't auto-fetch (depends on its implementation)
        // stationViewModel.refreshStations() // Or similar method
    }


    private fun saveAlarm() {
        val hour = binding.timePickerAlarm.hour
        val minute = binding.timePickerAlarm.minute
        val isEnabled = binding.switchAlarmEnabledDetail.isChecked

        if (selectedStation == null) {
            Toast.makeText(context, "Please select a radio station", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmToSave = Alarm(
            id = currentAlarm?.id ?: 0, // Use 0 for new alarm, Room will autoGenerate
            hour = hour,
            minute = minute,
            stationId = selectedStation!!.id,
            stationName = selectedStation!!.name,
            stationIconUrl = selectedStation!!.iconUrl,
            isEnabled = isEnabled
        )

        if (currentAlarm == null) { // New alarm
            // ViewModel's insert method now handles scheduling
            alarmViewModel.insert(alarmToSave.copy(id = 0)) // Ensure id is 0 for Room autoGenerate
            Toast.makeText(context, "Alarm saved", Toast.LENGTH_SHORT).show()
        } else { // Update existing alarm
            // ViewModel's update method now handles rescheduling
            alarmViewModel.update(alarmToSave)
            Toast.makeText(context, "Alarm updated", Toast.LENGTH_SHORT).show()
        }
        findNavController().popBackStack()
    }

    private fun deleteAlarm() {
        currentAlarm?.let {
            // ViewModel's delete method now handles cancellation
            alarmViewModel.delete(it)
            Toast.makeText(context, "Alarm deleted", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

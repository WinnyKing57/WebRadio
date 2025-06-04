package com.example.webradioapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.webradioapp.R
import com.example.webradioapp.utils.NotificationHelper
import com.example.webradioapp.utils.SharedPreferencesManager

class SettingsFragment : Fragment() {

    private lateinit var rgThemeOptions: RadioGroup
    private lateinit var rbThemeLight: RadioButton
    private lateinit var rbThemeDark: RadioButton
    private lateinit var rbThemeSystem: RadioButton
    private lateinit var btnShowTestNotification: Button
    private lateinit var etSleepTimerMinutes: android.widget.EditText
    private lateinit var btnSetSleepTimer: Button
    private lateinit var btnCancelSleepTimer: Button
    private lateinit var tvCurrentAlarm: android.widget.TextView
    private lateinit var btnSetAlarm: Button
    private lateinit var btnCancelAlarm: Button
    private lateinit var sharedPrefsManager: SharedPreferencesManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Notifications permission granted", Toast.LENGTH_SHORT).show()
                NotificationHelper.showSimpleNotification(
                    requireContext(),
                    "Test Notification",
                    "This is a test notification from WebradioApp.",
                    NotificationHelper.TEST_NOTIFICATION_ID
                )
            } else {
                Toast.makeText(requireContext(), "Notifications permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedPrefsManager = SharedPreferencesManager(requireContext())

        rgThemeOptions = view.findViewById(R.id.rg_theme_options)
        rbThemeLight = view.findViewById(R.id.rb_theme_light)
        rbThemeDark = view.findViewById(R.id.rb_theme_dark)
        rbThemeSystem = view.findViewById(R.id.rb_theme_system)
        btnShowTestNotification = view.findViewById(R.id.btn_show_test_notification)
        etSleepTimerMinutes = view.findViewById(R.id.et_sleep_timer_minutes)
        btnSetSleepTimer = view.findViewById(R.id.btn_set_sleep_timer)
        btnCancelSleepTimer = view.findViewById(R.id.btn_cancel_sleep_timer)
        tvCurrentAlarm = view.findViewById(R.id.tv_current_alarm)
        btnSetAlarm = view.findViewById(R.id.btn_set_alarm)
        btnCancelAlarm = view.findViewById(R.id.btn_cancel_alarm)


        loadCurrentThemePreference()
        setupThemeSelectionListener()
        setupTestNotificationButton()
        setupSleepTimerButtons()
        setupAlarmButtons()
        updateAlarmDisplay()

        return view
    }

    private fun updateAlarmDisplay() {
        val alarmTime = com.example.webradioapp.utils.AlarmScheduler.getScheduledAlarmTime(requireContext())
        if (alarmTime > 0) {
            val station = com.example.webradioapp.utils.AlarmScheduler.getScheduledAlarmStation(requireContext())
            val stationName = station?.name ?: "Last Played"
            tvCurrentAlarm.text = "Alarm set for: ${java.text.SimpleDateFormat("EEE, MMM d, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(alarmTime))} \nStation: $stationName"
        } else {
            tvCurrentAlarm.text = "No alarm set"
        }
    }

    private fun setupAlarmButtons() {
        btnSetAlarm.setOnClickListener {
            // Show TimePickerDialog
            val calendar = java.util.Calendar.getInstance()
            val timePickerDialog = android.app.TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val selectedTime = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                        set(java.util.Calendar.MINUTE, minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    // If selected time is in the past, set it for the next day
                    if (selectedTime.before(java.util.Calendar.getInstance())) {
                        selectedTime.add(java.util.Calendar.DAY_OF_MONTH, 1)
                    }

                    // Station selection for alarm (using last played for now)
                    val stationToPlay = sharedPrefsManager.getStationHistory().firstOrNull()
                    if (stationToPlay == null) {
                        Toast.makeText(requireContext(), "No station in history to set for alarm. Play a station first.", Toast.LENGTH_LONG).show()
                        return@TimePickerDialog
                    }

                    com.example.webradioapp.utils.AlarmScheduler.scheduleAlarm(requireContext(), selectedTime.timeInMillis, stationToPlay)
                    updateAlarmDisplay()
                    Toast.makeText(requireContext(), "Alarm set for ${selectedTime.time}", Toast.LENGTH_SHORT).show()

                },
                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                calendar.get(java.util.Calendar.MINUTE),
                false // 24 hour view or AM/PM
            )
            timePickerDialog.show()
        }

        btnCancelAlarm.setOnClickListener {
            com.example.webradioapp.utils.AlarmScheduler.cancelAlarm(requireContext())
            updateAlarmDisplay()
            Toast.makeText(requireContext(), "Alarm cancelled.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupSleepTimerButtons() {
        btnSetSleepTimer.setOnClickListener {
            val minutesString = etSleepTimerMinutes.text.toString()
            if (minutesString.isNotEmpty()) {
                try {
                    val minutes = minutesString.toLong()
                    if (minutes > 0) {
                        val durationMillis = minutes * 60 * 1000
                        val intent = Intent(activity, com.example.webradioapp.services.StreamingService::class.java).apply {
                            action = com.example.webradioapp.services.StreamingService.ACTION_SET_SLEEP_TIMER
                            putExtra(com.example.webradioapp.services.StreamingService.EXTRA_SLEEP_DURATION_MS, durationMillis)
                        }
                        activity?.startService(intent)
                        etSleepTimerMinutes.text.clear()
                    } else {
                        Toast.makeText(requireContext(), "Please enter a positive number of minutes.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "Invalid number format.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter the sleep duration in minutes.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelSleepTimer.setOnClickListener {
            val intent = Intent(activity, com.example.webradioapp.services.StreamingService::class.java).apply {
                action = com.example.webradioapp.services.StreamingService.ACTION_CANCEL_SLEEP_TIMER
            }
            activity?.startService(intent)
        }
    }

    private fun loadCurrentThemePreference() {
        when (sharedPrefsManager.getThemePreference()) {
            AppCompatDelegate.MODE_NIGHT_NO -> rbThemeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> rbThemeDark.isChecked = true
            else -> rbThemeSystem.isChecked = true
        }
    }

    private fun setupThemeSelectionListener() {
        rgThemeOptions.setOnCheckedChangeListener { _, checkedId ->
            val selectedThemeMode = when (checkedId) {
                R.id.rb_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rb_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                R.id.rb_theme_system -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> return@setOnCheckedChangeListener
            }
            sharedPrefsManager.setThemePreference(selectedThemeMode)
            AppCompatDelegate.setDefaultNightMode(selectedThemeMode)
        }
    }

    private fun setupTestNotificationButton() {
        btnShowTestNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Permission already granted
                        NotificationHelper.showSimpleNotification(
                            requireContext(),
                            "Test Notification",
                            "This is a test notification from WebradioApp.",
                            NotificationHelper.TEST_NOTIFICATION_ID
                        )
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        // Explain to the user why the permission is needed
                        // You can show a dialog here
                        Toast.makeText(requireContext(), "Notification permission is needed to show notifications.", Toast.LENGTH_LONG).show()
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    else -> {
                        // Directly request the permission
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                // No runtime permission needed for older versions
                NotificationHelper.showSimpleNotification(
                    requireContext(),
                    "Test Notification",
                    "This is a test notification from WebradioApp.",
                    NotificationHelper.TEST_NOTIFICATION_ID
                )
            }
        }
    }
}

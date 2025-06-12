package com.example.webradioapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.db.AlarmDao
import com.example.webradioapp.model.Alarm
import com.example.webradioapp.utils.AlarmManagerHelper
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmDao: AlarmDao
    private val alarmManagerHelper: AlarmManagerHelper
    val allAlarms: LiveData<List<Alarm>>

    init {
        val database = AppDatabase.getDatabase(application)
        alarmDao = database.alarmDao()
        alarmManagerHelper = AlarmManagerHelper(application)
        allAlarms = alarmDao.getAllAlarms()
    }

    fun insert(alarm: Alarm, schedule: Boolean = true) = viewModelScope.launch {
        val newId = alarmDao.insert(alarm) // Assuming insert returns the new ID
        if (schedule) {
            // We need the actual ID from the DB for scheduling
            val newAlarm = alarm.copy(id = newId.toInt()) // Create a new instance with the ID
            alarmManagerHelper.scheduleAlarm(newAlarm)
        }
    }

    fun update(alarm: Alarm, reschedule: Boolean = true) = viewModelScope.launch {
        alarmDao.update(alarm)
        if (reschedule) {
            if (alarm.isEnabled) {
                alarmManagerHelper.scheduleAlarm(alarm)
            } else {
                alarmManagerHelper.cancelAlarm(alarm)
            }
        }
    }

    fun delete(alarm: Alarm) = viewModelScope.launch {
        alarmDao.delete(alarm)
        alarmManagerHelper.cancelAlarm(alarm)
    }

    fun getAlarmById(id: Int, callback: (Alarm?) -> Unit) = viewModelScope.launch {
        callback(alarmDao.getAlarmById(id))
    }

    fun updateAlarmEnabled(alarmId: Int, isEnabled: Boolean) = viewModelScope.launch {
        val alarm = alarmDao.getAlarmById(alarmId)
        alarm?.let {
            val updatedAlarm = it.copy(isEnabled = isEnabled)
            alarmDao.update(updatedAlarm)
            if (isEnabled) {
                alarmManagerHelper.scheduleAlarm(updatedAlarm)
            } else {
                alarmManagerHelper.cancelAlarm(updatedAlarm)
            }
        }
    }
}

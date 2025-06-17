package com.example.webradioapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private val _alarmScheduledStatus = MutableLiveData<Pair<Boolean, String?>>() // Pair<Success, ErrorMessage/AlarmName>
    val alarmScheduledStatus: LiveData<Pair<Boolean, String?>> = _alarmScheduledStatus

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
            val scheduled = alarmManagerHelper.scheduleAlarm(newAlarm)
            if (!scheduled) {
                Log.w("AlarmViewModel", "Failed to schedule alarm ${newAlarm.id} after insert.")
                _alarmScheduledStatus.postValue(Pair(false, "Failed to schedule alarm: ${newAlarm.stationName}. Permission issue?"))
            } else {
                _alarmScheduledStatus.postValue(Pair(true, newAlarm.stationName))
            }
        }
    }

    fun update(alarm: Alarm, reschedule: Boolean = true) = viewModelScope.launch {
        alarmDao.update(alarm)
        if (reschedule) {
            if (alarm.isEnabled) {
                val scheduled = alarmManagerHelper.scheduleAlarm(alarm)
                if (!scheduled) {
                    Log.w("AlarmViewModel", "Failed to reschedule alarm ${alarm.id} after update.")
                    _alarmScheduledStatus.postValue(Pair(false, "Failed to reschedule alarm: ${alarm.stationName}. Permission issue?"))
                } else {
                    _alarmScheduledStatus.postValue(Pair(true, alarm.stationName))
                }
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
                val scheduled = alarmManagerHelper.scheduleAlarm(updatedAlarm)
                if (!scheduled) {
                    Log.w("AlarmViewModel", "Failed to schedule alarm ${updatedAlarm.id} after enabling.")
                     _alarmScheduledStatus.postValue(Pair(false, "Failed to schedule alarm: ${updatedAlarm.stationName}. Permission issue?"))
                } else {
                    // Optionally notify success, or only notify on failure
                    // _alarmScheduledStatus.postValue(Pair(true, updatedAlarm.stationName))
                }
            } else {
                alarmManagerHelper.cancelAlarm(updatedAlarm)
            }
        }
    }
}

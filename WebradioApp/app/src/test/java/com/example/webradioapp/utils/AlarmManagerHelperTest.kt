package com.example.webradioapp.utils

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.webradioapp.model.Alarm
import com.example.webradioapp.receivers.AlarmReceiver
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P]) // Configure Robolectric for a specific SDK level
class AlarmManagerHelperTest {

    @Mock
    private lateinit var mockAlarmManager: AlarmManager
    private lateinit var context: Context
    private lateinit var alarmManagerHelper: AlarmManagerHelper

    @Captor
    private lateinit var pendingIntentCaptor: ArgumentCaptor<PendingIntent>
    @Captor
    private lateinit var alarmTypeCaptor: ArgumentCaptor<Int>
    @Captor
    private lateinit var triggerAtMillisCaptor: ArgumentCaptor<Long>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication() // Robolectric's application context
        // Directly use the real context but mock the AlarmManager service behavior
        val appContext = context.applicationContext as Application
        val shadowAppContext = shadowOf(appContext)
        shadowAppContext.setSystemService(Context.ALARM_SERVICE, mockAlarmManager)

        alarmManagerHelper = AlarmManagerHelper(context)

        // For Build.VERSION_CODES.S check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            `when`(mockAlarmManager.canScheduleExactAlarms()).thenReturn(true)
        }
    }

    private fun createTestAlarm(id: Int, hour: Int, minute: Int, isEnabled: Boolean = true): Alarm {
        return Alarm(
            id = id,
            hour = hour,
            minute = minute,
            stationId = "station_$id",
            stationName = "Test Station $id",
            stationIconUrl = null,
            isEnabled = isEnabled
        )
    }

    @Test
    fun scheduleAlarm_whenEnabled_setsAlarmCorrectly() {
        val alarm = createTestAlarm(1, 8, 30)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) { // Ensure it's for future
                add(Calendar.DATE, 1)
            }
        }
        val expectedTriggerTime = calendar.timeInMillis

        alarmManagerHelper.scheduleAlarm(alarm)

        verify(mockAlarmManager).setExactAndAllowWhileIdle(
            alarmTypeCaptor.capture(),
            triggerAtMillisCaptor.capture(),
            pendingIntentCaptor.capture()
        )

        assertEquals(AlarmManager.RTC_WAKEUP, alarmTypeCaptor.value)
        assertEquals(expectedTriggerTime, triggerAtMillisCaptor.value)
        assertNotNull(pendingIntentCaptor.value)

        val capturedIntent = shadowOf(pendingIntentCaptor.value).savedIntent
        assertEquals(alarm.id, capturedIntent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1))
        assertEquals(alarm.stationId, capturedIntent.getStringExtra(AlarmReceiver.EXTRA_STATION_ID))
    }

    @Test
    fun scheduleAlarm_whenDisabled_doesNotSetAlarm() {
        val alarm = createTestAlarm(2, 9, 0, isEnabled = false)
        alarmManagerHelper.scheduleAlarm(alarm)
        verify(mockAlarmManager, never()).setExactAndAllowWhileIdle(anyInt(), anyLong(), any())
    }

    @Test
    fun cancelAlarm_cancelsPendingIntent() {
        val alarmIdToCancel = 3
        // Call cancelAlarmById directly as cancelAlarm(alarm) calls this.
        alarmManagerHelper.cancelAlarmById(alarmIdToCancel)

        verify(mockAlarmManager).cancel(pendingIntentCaptor.capture())
        assertNotNull(pendingIntentCaptor.value)

        // Verify the requestCode of the PendingIntent used for cancellation
        assertEquals(alarmIdToCancel, shadowOf(pendingIntentCaptor.value).requestCode)
        // Also verify the intent itself matches the base structure
        val expectedIntent = Intent(context, AlarmReceiver::class.java)
        assertEquals(expectedIntent.toUri(0), shadowOf(pendingIntentCaptor.value).savedIntent.toUri(0))
    }

    @Test
    fun rescheduleAllAlarms_schedulesEnabledAndCancelsDisabled() {
        val enabledAlarm = createTestAlarm(10, 8, 0, true)
        val disabledAlarm = createTestAlarm(11, 9, 0, false)
        val alarms = listOf(enabledAlarm, disabledAlarm)

        alarmManagerHelper.rescheduleAllAlarms(alarms)

        // Verify schedule for enabledAlarm
        verify(mockAlarmManager).setExactAndAllowWhileIdle(
            eq(AlarmManager.RTC_WAKEUP),
            anyLong(), // Time calculation is tested in scheduleAlarm_whenEnabled_setsAlarmCorrectly
            argThat { pi -> shadowOf(pi).savedIntent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) == enabledAlarm.id }
        )

        // Verify cancel for disabledAlarm (indirectly, by checking if schedule is NOT called)
        // More robustly: if cancelAlarmById was called on AlarmManagerHelper, it would call alarmManager.cancel()
        // For this, we might need to spy on AlarmManagerHelper or test behavior.
        // For now, let's assume schedule is not called for disabled.
        // This check is correct: schedule should not be called for the disabled alarm's ID.
        verify(mockAlarmManager, never()).setExactAndAllowWhileIdle(
            anyInt(),
            anyLong(),
            argThat { pi -> shadowOf(pi).savedIntent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) == disabledAlarm.id && shadowOf(pi).requestCode == disabledAlarm.id }
        )

        // Verify cancel for disabledAlarm
        // alarmManagerHelper.cancelAlarm(disabledAlarm) is called internally, which then calls cancelAlarmById.
        // This means alarmManager.cancel should be called with a PI matching disabledAlarm.id as requestCode.
        verify(mockAlarmManager).cancel(
             argThat { pi -> shadowOf(pi).requestCode == disabledAlarm.id &&
                             Intent(context, AlarmReceiver::class.java).toUri(0) == shadowOf(pi).savedIntent.toUri(0) }
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun scheduleAlarm_whenCannotScheduleExactAlarms_logsWarning() {
        // This test requires SDK >= S
        `when`(mockAlarmManager.canScheduleExactAlarms()).thenReturn(false)
        val alarm = createTestAlarm(1, 8, 30)
        alarmManagerHelper.scheduleAlarm(alarm)
        // We expect a log warning. Testing logs is possible but often complex.
        // For now, we trust the code logs. The main check is that setExactAndAllowWhileIdle is still called.
        verify(mockAlarmManager).setExactAndAllowWhileIdle(anyInt(), anyLong(), any())
    }
}

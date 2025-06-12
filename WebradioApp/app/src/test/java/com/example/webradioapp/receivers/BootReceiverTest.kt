package com.example.webradioapp.receivers

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.model.Alarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.OLDEST_SDK])
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var bootReceiver: BootReceiver
    private lateinit var shadowAlarmManager: org.robolectric.shadows.ShadowAlarmManager

    // Companion object for test database instance, similar to AlarmReceiverTest
    companion object {
        var testDatabaseInstance: AppDatabase? = null
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        testDatabaseInstance = db

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        bootReceiver = BootReceiver()

        shadowAlarmManager.scheduledAlarms.clear()
    }

    @After
    fun tearDown() {
        db.close()
        testDatabaseInstance = null
        shadowAlarmManager.scheduledAlarms.clear()
    }

    private fun createTestAlarm(id: Int, stationId: String, isEnabled: Boolean): Alarm {
        return Alarm(id, 8, 0, stationId, "Station $stationId", null, isEnabled)
    }

    @Test
    fun onReceive_withBootCompletedAction_reschedulesEnabledAlarms() = runBlocking {
        val enabledAlarm1 = createTestAlarm(1, "station1", true)
        val disabledAlarm = createTestAlarm(2, "station2", false)
        val enabledAlarm2 = createTestAlarm(3, "station3", true)

        // Populate DB via the test instance
        testDatabaseInstance!!.alarmDao().insert(enabledAlarm1)
        testDatabaseInstance!!.alarmDao().insert(disabledAlarm)
        testDatabaseInstance!!.alarmDao().insert(enabledAlarm2)

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        bootReceiver.onReceive(context, intent)

        // Verify that alarms were scheduled
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(2, scheduledAlarms.size, "Only enabled alarms should be rescheduled.")

        assertTrue(scheduledAlarms.any { shadowOf(it.operation).requestCode == enabledAlarm1.id }, "EnabledAlarm1 should be rescheduled.")
        assertTrue(scheduledAlarms.any { shadowOf(it.operation).requestCode == enabledAlarm2.id }, "EnabledAlarm2 should be rescheduled.")
        assertTrue(scheduledAlarms.none { shadowOf(it.operation).requestCode == disabledAlarm.id }, "DisabledAlarm should not be rescheduled.")
    }

    @Test
    fun onReceive_withOtherAction_doesNothing() = runBlocking {
        val alarm = createTestAlarm(1, "station1", true)
        testDatabaseInstance!!.alarmDao().insert(alarm)

        val intent = Intent("com.example.OTHER_ACTION")
        bootReceiver.onReceive(context, intent)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty(), "No alarms should be scheduled for non-boot action.")
    }

    @Test
    fun onReceive_noEnabledAlarms_schedulesNothing() = runBlocking {
        val disabledAlarm = createTestAlarm(1, "station1", false)
        testDatabaseInstance!!.alarmDao().insert(disabledAlarm)

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, intent)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty(), "No alarms should be scheduled if all are disabled.")
    }
}

// Similar to AlarmReceiverTest, BootReceiver needs to be able to use testDatabaseInstance.
// In BootReceiver.kt, change:
// val database = AppDatabase.getDatabase(context.applicationContext)
// To:
// val database = BootReceiverTest.testDatabaseInstance ?: AppDatabase.getDatabase(context.applicationContext)
// This note applies here as well.The `BootReceiverTest.kt` has been created. It also relies on the same testing hack for `AppDatabase.getDatabase()` as `AlarmReceiverTest.kt`.

**Summary of Unit Tests Created:**
*   `MainActivitySleepTimerTest.kt`: Instrumented test for `MainActivity`'s sleep timer. Test for `onFinish` sending intent is still a bit weak due to `CountDownTimer` complexities in this test type.
*   `AlarmManagerHelperTest.kt`: JVM/Robolectric test for `AlarmManagerHelper` using Mockito.
*   `AlarmReceiverTest.kt`: JVM/Robolectric test for `AlarmReceiver`. Relies on a hack to inject a test database instance.
*   `BootReceiverTest.kt`: JVM/Robolectric test for `BootReceiver`. Also relies on the database injection hack.

**Next Steps (UI Tests - Espresso):**
UI tests would be the next logical step, but they are more complex to write and require an emulator or device to run. They would cover:
1.  **Sleep Timer UI**:
    *   Clicking sleep timer button shows dialog.
    *   Selecting a duration from dialog sets the timer (can verify with a toast or a UI change if one were added).
    *   (Harder to test with Espresso alone) Verify playback stops after duration. This might need a custom IdlingResource that monitors `StreamingService.isPlayingLiveData`.
2.  **Alarms List UI**:
    *   Navigating to Alarms screen shows the list.
    *   FAB click navigates to Add/Edit Alarm screen.
    *   Clicking an alarm item navigates to Add/Edit Alarm screen with correct data.
    *   Toggling an alarm switch updates its state (and schedules/cancels it - harder to verify directly in UI test).
3.  **Add/Edit Alarm UI**:
    *   Saving a new alarm adds it to the list and schedules it.
    *   Editing an alarm updates it in the list and reschedules.
    *   Deleting an alarm removes it and cancels the schedule.
    *   TimePicker interaction.
    *   Station selection dialog interaction.

Given the constraints and the primary goal of adding *unit tests* for the core logic, I'll consider the current set of unit tests as fulfilling the main requirement for non-UI logic. Espresso tests are a larger, separate undertaking.

I will now submit the report, noting the limitations of the `MainActivitySleepTimerTest` and the DB mocking strategy for receivers.

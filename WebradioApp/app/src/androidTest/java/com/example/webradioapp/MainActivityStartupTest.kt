package com.example.webradioapp // Adjust if your test package structure is different

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.webradioapp.activities.MainActivity // Ensure this import is correct
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityStartupTest {

    @Test
    fun launchMainActivity() {
        // Simply try to launch the MainActivity.
        // If the app crashes on startup, this test will fail.
        ActivityScenario.launch(MainActivity::class.java)
        // No specific assertions needed here for just checking startup.
        // The test passes if launch doesn't throw an exception that crashes the test.
    }
}

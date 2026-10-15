package com.example.workouttracker // Use your existing package name

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestPermissionTest {

    @Test
    fun mergedManifest_doesNotContainInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val permissionState = context.checkSelfPermission(Manifest.permission.INTERNET)
        assertEquals(
            "Merged manifest must NEVER contain android.permission.INTERNET",
            PackageManager.PERMISSION_DENIED,
            permissionState
        )
    }
}
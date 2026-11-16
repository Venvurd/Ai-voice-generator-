package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kokoro Voice", appName)
  }

  @Test
  fun `main activity is locked to portrait and preserves config changes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val componentName = ComponentName(context, MainActivity::class.java)
    val activityInfo = context.packageManager.getActivityInfo(
      componentName,
      PackageManager.GET_META_DATA
    )
    assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityInfo.screenOrientation)
    assertTrue((activityInfo.configChanges and ActivityInfo.CONFIG_ORIENTATION) != 0)
    assertTrue((activityInfo.configChanges and ActivityInfo.CONFIG_SCREEN_SIZE) != 0)
  }

  @Test
  fun `index html includes worker offloading progress bar and prosody engine`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val html = context.assets.open("index.html").bufferedReader().use { it.readText() }

    assertTrue(html.contains("new Worker("))
    assertTrue(html.contains("gen-progress-wrapper"))
    assertTrue(html.contains("gen-progress-percent"))
    assertTrue(html.contains("segmentTextForProsody"))
    assertTrue(html.contains("applyPitchContourWSOLA"))
  }
}

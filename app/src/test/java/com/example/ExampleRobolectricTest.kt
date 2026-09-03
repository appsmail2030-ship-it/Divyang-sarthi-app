package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pink Line Alert System", appName)
  }

  @Test
  fun `pink line station list has 38 official stations`() {
    val stations = com.example.model.PinkLineStationsData.DEFAULT_STATIONS
    assertEquals(38, stations.size)
    assertEquals("Majlis Park", stations.first().name)
    assertEquals("Shiv Vihar", stations.last().name)
  }
}

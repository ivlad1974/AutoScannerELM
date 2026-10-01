package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.logging.LogDataParser
import com.example.data.logging.SensorFileLogger
import com.example.ui.MainViewModel
import com.example.ui.screens.LogGraphViewerScreen
import com.example.ui.screens.LogsHistoryScreen
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Автосканер 6", appName)
  }

  @Test
  fun `test select and render log file`() = kotlinx.coroutines.test.runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val testCsv = File(app.getExternalFilesDir(null), "SensorLogs/test_trip_log.csv").apply {
      parentFile?.mkdirs()
      writeText(
        "Timestamp,Скорость,Обороты двигателя,Температура ОЖ,Напряжение сети\n" +
        "10:00:01,0,800,85.0,14.1\n" +
        "10:00:02,15,1200,85.2,14.2\n" +
        "10:00:03,30,1800,86.0,14.1\n"
      )
    }

    val parsed = LogDataParser.parseCsvFile(testCsv)
    assertNotNull(parsed)

    val viewModel = MainViewModel(app)
    viewModel.selectLogFileForGraph(testCsv)

    composeTestRule.setContent {
      LogGraphViewerScreen(
        viewModel = viewModel,
        onBack = {}
      )
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun `test uneven and missing sensor rows does not crash parser or screen`() = kotlinx.coroutines.test.runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val testCsv = File(app.cacheDir, "test_uneven_sensors.csv").apply {
      writeText(
        "Timestamp,RPM,Speed,UnpolledSensor,Boost\n" +
        "10:00:01,800,0,--,--\n" +
        "10:00:02,1200,--,--,--\n" +
        "10:00:03,1500,20,--,--\n" +
        "10:00:04,2200,45,--,1.2\n"
      )
    }

    val parsed = LogDataParser.parseCsvFile(testCsv)
    assertNotNull(parsed)
    // "UnpolledSensor" should be filtered out because it was never recorded
    assertEquals(false, parsed!!.seriesList.any { it.sensorName == "UnpolledSensor" })
    // Recorded sensors (RPM, Speed, Boost) should be present
    assertEquals(3, parsed.seriesList.size)
    // And all recorded series should have the exact same point count matching dataRows
    assertEquals(4, parsed.seriesList[0].points.size)
    assertEquals(4, parsed.seriesList[1].points.size)
    assertEquals(4, parsed.seriesList[2].points.size)

    val viewModel = MainViewModel(app)
    viewModel.selectLogFileForGraph(testCsv)

    composeTestRule.setContent {
      LogGraphViewerScreen(
        viewModel = viewModel,
        onBack = {}
      )
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun `test vertical sensor list and delete log button exist and function`() = kotlinx.coroutines.test.runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val testFile = File(app.getExternalFilesDir(null), "SensorLogs/test_delete_me.csv").apply {
      parentFile?.mkdirs()
      writeText(
        "Timestamp,Скорость,Обороты двигателя,Температура ОЖ,Напряжение сети\n" +
        "10:00:01,0,800,85.0,14.1\n" +
        "10:00:02,15,1200,85.2,14.2\n"
      )
    }

    val viewModel = MainViewModel(app)
    viewModel.selectLogFileForGraph(testFile)

    composeTestRule.setContent {
      LogGraphViewerScreen(
        viewModel = viewModel,
        onBack = {}
      )
    }

    composeTestRule.waitForIdle()

    // Verify vertical sensor list and delete button exist
    composeTestRule.onNodeWithTag("delete_current_log_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vertical_sensor_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sensor_item_row_0").assertIsDisplayed()
    composeTestRule.onNodeWithTag("select_all_sensors_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("reset_sensors_btn").assertIsDisplayed()

    // Test deleting the log file
    val deleted = viewModel.deleteLogFile(testFile)
    assertEquals(true, deleted)
    assertFalse(testFile.exists())
  }

  @Test
  fun `test voice log command starts separate file with all settings sensors and continuous trip log has 4 dashboard sensors`() = kotlinx.coroutines.test.runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)

    // 1. Verify 4 dashboard tile sensors are configured
    val tiles = viewModel.dashboardTileConfigs.value
    assertEquals(4, tiles.size)

    // 2. Test voice command "запиши лог"
    viewModel.askAiCustomQuery("Запиши лог")
    assertEquals(true, viewModel.isLogging.value)

    val logs = viewModel.getLogFiles()
    val voiceLog = logs.find { it.name.startsWith("AutoScan_VoiceLog_") }
    assertNotNull("Voice log file must be created separately", voiceLog)

    // 3. Test stopping voice logging
    viewModel.askAiCustomQuery("Останови запись лога")
    assertEquals(false, viewModel.isLogging.value)
  }
}


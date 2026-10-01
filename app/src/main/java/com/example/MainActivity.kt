package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.service.ObdForegroundService
import com.example.ui.MainViewModel
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.DesignShowcaseScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogGraphViewerScreen
import com.example.ui.screens.LogsHistoryScreen
import com.example.ui.screens.Mode1DtcScanScreen
import com.example.ui.screens.Mode2ClearDtcScreen
import com.example.ui.screens.Mode3LiveSensorsScreen
import com.example.ui.theme.AutoScanTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val exitReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ObdForegroundService.ACTION_EXIT_APP) {
                finishAndRemoveTask()
                exitProcess(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val filter = IntentFilter(ObdForegroundService.ACTION_EXIT_APP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(exitReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(exitReceiver, filter)
        }

        enableEdgeToEdge()

        // Keep screen on and set maximum brightness while app is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params = window.attributes
        params.screenBrightness = 1.0f
        window.attributes = params

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            AutoScanTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutoScanApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params = window.attributes
        params.screenBrightness = 1.0f
        window.attributes = params
    }

    override fun onPause() {
        super.onPause()
        viewModel.persistProfile()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }

    override fun onStop() {
        super.onStop()
        viewModel.persistProfile()
    }

    override fun onDestroy() {
        try {
            viewModel.persistProfile()
            unregisterReceiver(exitReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        super.onDestroy()
    }
}

@Composable
fun AutoScanApp(viewModel: MainViewModel) {
    val navController = rememberNavController()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            navController.navigate(route)
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateMode1 = { navController.navigate("mode1_dtc") },
                onNavigateMode2 = { navController.navigate("mode2_clear") },
                onNavigateMode3 = { navController.navigate("mode3_sensors") },
                onNavigateAiChat = { navController.navigate("ai_chat") },
                onNavigateLogs = { navController.navigate("logs_history") },
                onNavigateDesignShowcase = { navController.navigate("design_showcase") }
            )
        }
        composable("design_showcase") {
            DesignShowcaseScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("mode1_dtc") {
            Mode1DtcScanScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateAiChat = { navController.navigate("ai_chat") }
            )
        }
        composable("mode2_clear") {
            Mode2ClearDtcScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("mode3_sensors") {
            Mode3LiveSensorsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateAiChat = { navController.navigate("ai_chat") }
            )
        }
        composable("ai_chat") {
            AiAssistantScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("logs_history") {
            LogsHistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateGraphViewer = { navController.navigate("log_graph_viewer") },
                onNavigateAiChat = { navController.navigate("ai_chat") }
            )
        }
        composable("log_graph_viewer") {
            LogGraphViewerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateAiChat = { navController.navigate("ai_chat") }
            )
        }
    }
}

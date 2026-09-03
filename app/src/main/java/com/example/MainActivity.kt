package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.AlertSoundManager
import com.example.audio.VoiceAnnouncementManager
import com.example.model.PassengerType
import com.example.notifications.NotificationHelper
import com.example.repository.PinkLineRepository
import com.example.ui.components.DestinationAlertOverlay
import com.example.ui.components.TwoStationAlertOverlay
import com.example.ui.screens.ActiveRequestsScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.CreateRequestScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RequestDetailsScreen
import com.example.ui.screens.RequestHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PinkLineViewModel
import com.example.ui.viewmodel.PinkLineViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: PinkLineRepository
    private lateinit var alertSoundManager: AlertSoundManager
    private lateinit var voiceAnnouncementManager: VoiceAnnouncementManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = PinkLineRepository(applicationContext)
        alertSoundManager = AlertSoundManager(applicationContext)
        voiceAnnouncementManager = VoiceAnnouncementManager(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)

        setContent {
            MyApplicationTheme {
                // Request notification permissions on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val viewModel: PinkLineViewModel = viewModel(
                    factory = PinkLineViewModelFactory(
                        application = application,
                        repository = repository,
                        alertSoundManager = alertSoundManager,
                        voiceAnnouncementManager = voiceAnnouncementManager,
                        notificationHelper = notificationHelper
                    )
                )

                PinkLineApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alertSoundManager.stopSiren()
        voiceAnnouncementManager.shutdown()
    }
}

@Composable
fun PinkLineApp(viewModel: PinkLineViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val stations by viewModel.allStations.collectAsState()
    val allRequests by viewModel.allRequests.collectAsState()
    val activeRequests by viewModel.activeRequests.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val systemConfig by viewModel.systemConfig.collectAsState()
    val incomingAlert by viewModel.incomingAlert.collectAsState()
    val twoStationAlert by viewModel.twoStationAlert.collectAsState()

    // Listen for source station confirmation notifications ("Request acknowledged by Destination Station")
    LaunchedEffect(Unit) {
        viewModel.userConfirmationMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) "home" else "login",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("login") {
                    LoginScreen(
                        stations = stations,
                        networkStatus = networkStatus,
                        onLoginSuccess = { station, pass ->
                            val success = viewModel.loginStation(station, pass)
                            if (success) {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Invalid password. Please check credentials or contact Admin.")
                                }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        stationName = currentStation ?: "Anand Vihar ISBT",
                        networkStatus = networkStatus,
                        metrics = metrics,
                        activeRequests = activeRequests,
                        isAdmin = isAdminLoggedIn,
                        onCategorySelected = { passengerType ->
                            navController.navigate("create_request/${passengerType.name}")
                        },
                        onNavigateToActiveRequests = {
                            navController.navigate("active_requests")
                        },
                        onNavigateToHistory = {
                            navController.navigate("history")
                        },
                        onNavigateToAdmin = {
                            navController.navigate("admin")
                        },
                        onSelectRequest = { req ->
                            viewModel.selectRequest(req)
                            navController.navigate("request_details/${req.requestId}")
                        },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = "create_request/{passengerType}",
                    arguments = listOf(navArgument("passengerType") { type = NavType.StringType })
                ) { backStackEntry ->
                    val typeParam = backStackEntry.arguments?.getString("passengerType")
                    val passengerType = try {
                        PassengerType.valueOf(typeParam ?: "VISUALLY_IMPAIRED")
                    } catch (e: Exception) {
                        PassengerType.VISUALLY_IMPAIRED
                    }

                    CreateRequestScreen(
                        sourceStationName = currentStation ?: "Anand Vihar ISBT",
                        initialPassengerType = passengerType,
                        allStations = stations,
                        systemConfig = systemConfig,
                        onBack = { navController.popBackStack() },
                        onSubmitRequest = { dst, type, count, train, dir, notes, onSuccess ->
                            viewModel.createRequest(dst, type, count, train, dir, notes, onSuccess)
                        },
                        onRequestCreated = { createdReq ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Request ${createdReq.requestId} transmitted to ${createdReq.destinationStation} Station.")
                            }
                            navController.navigate("active_requests") {
                                popUpTo("home")
                            }
                        }
                    )
                }

                composable("active_requests") {
                    ActiveRequestsScreen(
                        currentStationName = currentStation ?: "Anand Vihar ISBT",
                        activeRequests = activeRequests,
                        onSelectRequest = { req ->
                            viewModel.selectRequest(req)
                            navController.navigate("request_details/${req.requestId}")
                        },
                        onAcknowledgeRequest = { reqId ->
                            viewModel.acknowledgeRequest(reqId, "${currentStation ?: "Station"} Staff")
                            scope.launch {
                                snackbarHostState.showSnackbar("Assistance Request $reqId Acknowledged.")
                            }
                        },
                        onAdvanceTrainProgress = { reqId ->
                            viewModel.advanceTrain(reqId)
                        },
                        onMarkCompleted = { reqId ->
                            viewModel.markRequestCompleted(reqId, "${currentStation ?: "Station"} Staff")
                            scope.launch {
                                snackbarHostState.showSnackbar("Assistance Request $reqId marked as COMPLETED.")
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "request_details/{requestId}",
                    arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val reqId = backStackEntry.arguments?.getString("requestId")
                    val selectedReq = allRequests.find { it.requestId == reqId }

                    if (selectedReq != null) {
                        RequestDetailsScreen(
                            request = selectedReq,
                            currentStation = currentStation ?: "Anand Vihar ISBT",
                            onBack = { navController.popBackStack() },
                            onAcknowledge = { id ->
                                viewModel.acknowledgeRequest(id, "${currentStation ?: "Station"} Staff")
                                scope.launch {
                                    snackbarHostState.showSnackbar("Request $id Acknowledged.")
                                }
                            },
                            onAdvanceTrain = { id ->
                                viewModel.advanceTrain(id)
                            },
                            onMarkCompleted = { id, remarks ->
                                viewModel.markRequestCompleted(id, "${currentStation ?: "Station"} Staff", remarks)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Assistance Request marked as COMPLETED.")
                                }
                            },
                            onPlayAnnouncement = {
                                viewModel.testVoiceAnnouncement(selectedReq.passengerType)
                            }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }

                composable("history") {
                    RequestHistoryScreen(
                        allRequests = allRequests,
                        onSelectRequest = { req ->
                            viewModel.selectRequest(req)
                            navController.navigate("request_details/${req.requestId}")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("admin") {
                    AdminPanelScreen(
                        stations = stations,
                        config = systemConfig,
                        networkStatus = networkStatus,
                        onUpdateConfig = { cfg -> viewModel.updateSystemConfig(cfg) },
                        onAddStation = { stn -> viewModel.addStation(stn) },
                        onUpdateStation = { stn -> viewModel.updateStation(stn) },
                        onDeleteStation = { stn -> viewModel.deleteStation(stn) },
                        onSimulateNetwork = { st -> viewModel.setSimulatedNetworkStatus(st) },
                        onTestSiren = { viewModel.testAudioSiren() },
                        onTestVoiceAnnouncement = { type -> viewModel.testVoiceAnnouncement(type) },
                        onClearCompleted = {
                            viewModel.clearCompletedRequests()
                            scope.launch {
                                snackbarHostState.showSnackbar("Completed assistance records purged.")
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // High Priority Destination Station Alert Overlay
            incomingAlert?.let { alertReq ->
                DestinationAlertOverlay(
                    request = alertReq,
                    onAcknowledge = { req ->
                        viewModel.acknowledgeRequest(req.requestId, "${currentStation ?: "Station"} Staff")
                    },
                    onReject = { req ->
                        viewModel.rejectRequest(req.requestId)
                    }
                )
            }

            // Two-Station Before Reminder Alert Overlay
            twoStationAlert?.let { alertReq ->
                TwoStationAlertOverlay(
                    request = alertReq,
                    onPlayVoice = {
                        viewModel.testVoiceAnnouncement(alertReq.passengerType)
                    },
                    onDismiss = {
                        viewModel.dismissAlerts()
                    }
                )
            }
        }
    }
}

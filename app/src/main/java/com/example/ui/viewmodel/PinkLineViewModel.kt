package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AlertSoundManager
import com.example.audio.VoiceAnnouncementManager
import com.example.model.AssistanceRequest
import com.example.model.AssistanceStatus
import com.example.model.Direction
import com.example.model.NetworkStatus
import com.example.model.PassengerType
import com.example.model.Station
import com.example.model.SystemConfig
import com.example.notifications.NotificationHelper
import com.example.repository.AlertEvent
import com.example.repository.PinkLineRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardMetrics(
    val activeAssistanceCount: Int = 0,
    val pendingAcknowledgementCount: Int = 0,
    val upcomingArrivalsCount: Int = 0
)

data class RequestHistoryFilter(
    val searchQuery: String = "",
    val sourceStation: String? = null,
    val destinationStation: String? = null,
    val passengerType: PassengerType? = null,
    val trainId: String = "",
    val status: AssistanceStatus? = null
)

class PinkLineViewModel(
    application: Application,
    private val repository: PinkLineRepository,
    private val alertSoundManager: AlertSoundManager,
    private val voiceAnnouncementManager: VoiceAnnouncementManager,
    private val notificationHelper: NotificationHelper
) : AndroidViewModel(application) {

    // Login state
    private val _currentStation = MutableStateFlow<String?>("Anand Vihar ISBT")
    val currentStation: StateFlow<String?> = _currentStation.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    // Real-time data streams
    val allStations: StateFlow<List<Station>> = repository.allStationsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val allRequests: StateFlow<List<AssistanceRequest>> = repository.allRequestsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val systemConfig: StateFlow<SystemConfig> = repository.configFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SystemConfig()
    )

    val networkStatus: StateFlow<NetworkStatus> = repository.networkStatus

    // Active requests for the current station (or all for admin)
    val activeRequests: StateFlow<List<AssistanceRequest>> = combine(
        allRequests,
        _currentStation
    ) { requests, station ->
        requests.filter { req ->
            req.status != AssistanceStatus.COMPLETED && req.status != AssistanceStatus.CANCELLED
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allRequests,
        _currentStation
    ) { requests, station ->
        val stationName = station ?: ""
        val relevantActive = requests.filter {
            (it.sourceStation == stationName || it.destinationStation == stationName) &&
                    it.status != AssistanceStatus.COMPLETED && it.status != AssistanceStatus.CANCELLED
        }
        val pendingAck = requests.filter {
            it.destinationStation == stationName && it.status == AssistanceStatus.REQUEST_SENT
        }
        val upcoming = requests.filter {
            it.destinationStation == stationName &&
                    (it.status == AssistanceStatus.TWO_STATION_REMINDER || it.status == AssistanceStatus.ARRIVING || it.stationsRemaining <= 2) &&
                    it.status != AssistanceStatus.COMPLETED && it.status != AssistanceStatus.CANCELLED
        }

        DashboardMetrics(
            activeAssistanceCount = relevantActive.size,
            pendingAcknowledgementCount = pendingAck.size,
            upcomingArrivalsCount = upcoming.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Active full-screen alert dialog state
    private val _incomingAlert = MutableStateFlow<AssistanceRequest?>(null)
    val incomingAlert: StateFlow<AssistanceRequest?> = _incomingAlert.asStateFlow()

    // Active two-station alert popup state
    private val _twoStationAlert = MutableStateFlow<AssistanceRequest?>(null)
    val twoStationAlert: StateFlow<AssistanceRequest?> = _twoStationAlert.asStateFlow()

    // Selected request for details view
    private val _selectedRequest = MutableStateFlow<AssistanceRequest?>(null)
    val selectedRequest: StateFlow<AssistanceRequest?> = _selectedRequest.asStateFlow()

    // Source station confirmation message
    private val _userConfirmationMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val userConfirmationMessage = _userConfirmationMessage.asSharedFlow()

    // History filter
    private val _historyFilter = MutableStateFlow(RequestHistoryFilter())
    val historyFilter: StateFlow<RequestHistoryFilter> = _historyFilter.asStateFlow()

    init {
        listenToAlertEvents()
    }

    private fun listenToAlertEvents() {
        viewModelScope.launch {
            repository.alertEvents.collect { event ->
                when (event) {
                    is AlertEvent.NewDestinationAlert -> {
                        val req = event.request
                        val targetStation = _currentStation.value
                        // Trigger alert if destination is this device's station, or in demo/testing mode
                        if (targetStation == null || req.destinationStation.equals(targetStation, ignoreCase = true)) {
                            _incomingAlert.value = req
                            alertSoundManager.playSiren()
                            notificationHelper.showAssistanceAlertNotification(
                                req,
                                title = "🚨 NEW PASSENGER ASSISTANCE REQUEST",
                                message = "Train Set ${req.trainId} carrying ${req.passengerCount} ${req.passengerType.displayName} passenger(s) approaching ${req.destinationStation}"
                            )
                        }
                    }
                    is AlertEvent.RequestAcknowledgedAlert -> {
                        val req = event.request
                        val myStation = _currentStation.value
                        if (myStation != null && req.sourceStation.equals(myStation, ignoreCase = true)) {
                            _userConfirmationMessage.emit("Request acknowledged by ${req.destinationStation} Station.")
                        }
                    }
                    is AlertEvent.TwoStationBeforeAlert -> {
                        val req = event.request
                        val targetStation = _currentStation.value
                        if (targetStation == null || req.destinationStation.equals(targetStation, ignoreCase = true)) {
                            _twoStationAlert.value = req
                            alertSoundManager.playSiren(durationMillis = 10_000L)
                            voiceAnnouncementManager.speakTwoStationAlert(
                                trainId = req.trainId,
                                passengerCount = req.passengerCount,
                                passengerType = req.passengerType,
                                destinationStation = req.destinationStation
                            )
                            notificationHelper.showAssistanceAlertNotification(
                                req,
                                title = "⚡ 2-STATIONS BEFORE REMINDER",
                                message = "Train Set ${req.trainId} is now 2 stations away from ${req.destinationStation}!"
                            )
                        }
                    }
                    is AlertEvent.FinalArrivalAlert -> {
                        val req = event.request
                        val targetStation = _currentStation.value
                        if (targetStation == null || req.destinationStation.equals(targetStation, ignoreCase = true)) {
                            alertSoundManager.playSiren(durationMillis = 6_000L)
                            voiceAnnouncementManager.speakArrivalAlert(
                                trainId = req.trainId,
                                destinationStation = req.destinationStation
                            )
                            notificationHelper.showAssistanceAlertNotification(
                                req,
                                title = "🚉 FINAL ARRIVAL ALERT",
                                message = "Train Set ${req.trainId} is approaching ${req.destinationStation} Platform!"
                            )
                        }
                    }
                }
            }
        }
    }

    // Login operations
    fun loginStation(stationName: String, password: String): Boolean {
        val config = systemConfig.value
        if (password == config.commonPassword || password == config.adminPassword) {
            _currentStation.value = stationName
            _isLoggedIn.value = true
            _isAdminLoggedIn.value = (password == config.adminPassword)
            return true
        }
        return false
    }

    fun loginAdmin(password: String): Boolean {
        val config = systemConfig.value
        if (password == config.adminPassword) {
            _isAdminLoggedIn.value = true
            return true
        }
        return false
    }

    fun logout() {
        _isLoggedIn.value = false
        _isAdminLoggedIn.value = false
        dismissAlerts()
    }

    fun logoutAdminOnly() {
        _isAdminLoggedIn.value = false
    }

    // Create Request
    fun createRequest(
        destinationStationName: String,
        passengerType: PassengerType,
        passengerCount: Int,
        trainId: String,
        direction: Direction,
        notes: String = "",
        onSuccess: (AssistanceRequest) -> Unit
    ) {
        val src = _currentStation.value ?: "Anand Vihar ISBT"
        viewModelScope.launch {
            val req = repository.createAssistanceRequest(
                sourceStationName = src,
                destinationStationName = destinationStationName,
                passengerType = passengerType,
                passengerCount = passengerCount,
                trainId = trainId,
                direction = direction,
                notes = notes
            )
            onSuccess(req)
        }
    }

    // Acknowledge Request
    fun acknowledgeRequest(requestId: String, staffName: String = "Station Staff") {
        dismissAlerts()
        viewModelScope.launch {
            repository.acknowledgeRequest(requestId, staffName)
        }
    }

    // Reject Request
    fun rejectRequest(requestId: String, reason: String = "Cannot accept at this time") {
        dismissAlerts()
        viewModelScope.launch {
            repository.rejectRequest(requestId, reason)
        }
    }

    // Mark Request Completed
    fun markRequestCompleted(requestId: String, staffName: String = "Station Staff", remarks: String = "") {
        viewModelScope.launch {
            repository.markRequestCompleted(requestId, staffName, remarks)
        }
    }

    // Train journey progression trigger
    fun advanceTrain(requestId: String) {
        viewModelScope.launch {
            repository.advanceTrainProgress(requestId)
        }
    }

    fun dismissAlerts() {
        _incomingAlert.value = null
        _twoStationAlert.value = null
        alertSoundManager.stopSiren()
        voiceAnnouncementManager.stop()
    }

    fun selectRequest(request: AssistanceRequest?) {
        _selectedRequest.value = request
    }

    fun updateHistoryFilter(filter: RequestHistoryFilter) {
        _historyFilter.value = filter
    }

    // Admin updates
    fun updateSystemConfig(config: SystemConfig) {
        viewModelScope.launch {
            repository.updateSystemConfig(config)
        }
    }

    fun addStation(station: Station) {
        viewModelScope.launch {
            repository.addStation(station)
        }
    }

    fun updateStation(station: Station) {
        viewModelScope.launch {
            repository.updateStation(station)
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            repository.deleteStation(station)
        }
    }

    fun setSimulatedNetworkStatus(status: NetworkStatus) {
        repository.setSimulatedNetworkStatus(status)
    }

    fun testAudioSiren() {
        alertSoundManager.playSiren(durationMillis = 4000L)
    }

    fun testVoiceAnnouncement(type: PassengerType = PassengerType.VISUALLY_IMPAIRED) {
        voiceAnnouncementManager.speakTwoStationAlert(
            trainId = "47",
            passengerCount = 2,
            passengerType = type,
            destinationStation = _currentStation.value ?: "Trilokpuri Sanjay Lake"
        )
    }

    fun clearCompletedRequests() {
        viewModelScope.launch {
            repository.clearCompletedRequests()
        }
    }

    override fun onCleared() {
        super.onCleared()
        dismissAlerts()
        voiceAnnouncementManager.shutdown()
    }
}

class PinkLineViewModelFactory(
    private val application: Application,
    private val repository: PinkLineRepository,
    private val alertSoundManager: AlertSoundManager,
    private val voiceAnnouncementManager: VoiceAnnouncementManager,
    private val notificationHelper: NotificationHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PinkLineViewModel::class.java)) {
            return PinkLineViewModel(
                application,
                repository,
                alertSoundManager,
                voiceAnnouncementManager,
                notificationHelper
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

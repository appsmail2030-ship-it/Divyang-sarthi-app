package com.example.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.ConfigEntity
import com.example.data.local.PinkLineDatabase
import com.example.data.local.RequestEntity
import com.example.data.local.StationEntity
import com.example.model.AssistanceRequest
import com.example.model.AssistanceStatus
import com.example.model.Direction
import com.example.model.NetworkStatus
import com.example.model.PassengerType
import com.example.model.PinkLineStationsData
import com.example.model.Station
import com.example.model.SyncStatus
import com.example.model.SystemConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

sealed class AlertEvent {
    data class NewDestinationAlert(val request: AssistanceRequest) : AlertEvent()
    data class RequestAcknowledgedAlert(val request: AssistanceRequest) : AlertEvent()
    data class TwoStationBeforeAlert(val request: AssistanceRequest) : AlertEvent()
    data class FinalArrivalAlert(val request: AssistanceRequest) : AlertEvent()
}

class PinkLineRepository(private val context: Context) {
    private val database = PinkLineDatabase.getInstance(context)
    private val requestDao = database.requestDao()
    private val stationDao = database.stationDao()
    private val configDao = database.configDao()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _networkStatus = MutableStateFlow(NetworkStatus.ONLINE)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _alertEvents = MutableSharedFlow<AlertEvent>(extraBufferCapacity = 64)
    val alertEvents: SharedFlow<AlertEvent> = _alertEvents.asSharedFlow()

    // Real-time observable flows
    val allRequestsFlow: Flow<List<AssistanceRequest>> = requestDao.getAllRequests().map { list ->
        list.map { it.toDomain() }
    }

    val allStationsFlow: Flow<List<Station>> = stationDao.getAllStations().map { list ->
        list.map { it.toDomain() }
    }

    val configFlow: Flow<SystemConfig> = configDao.getConfig().map { entity ->
        entity?.toDomain() ?: SystemConfig()
    }

    init {
        monitorNetworkConnectivity()
        seedInitialDataIfEmpty()
        startJourneyProgressionMonitor()
    }

    private fun seedInitialDataIfEmpty() {
        coroutineScope.launch {
            val count = stationDao.getStationCount()
            if (count == 0) {
                stationDao.insertStations(PinkLineStationsData.DEFAULT_STATIONS.map {
                    StationEntity.fromDomain(it)
                })
            }
            val cfg = configDao.getConfigSync()
            if (cfg == null) {
                configDao.insertOrUpdateConfig(ConfigEntity.fromDomain(SystemConfig()))
            }
        }
    }

    private fun monitorNetworkConnectivity() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        try {
            connectivityManager?.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _networkStatus.value = NetworkStatus.SYNCING
                    coroutineScope.launch {
                        delay(600) // Brief sync transition
                        syncPendingRequests()
                        _networkStatus.value = NetworkStatus.ONLINE
                    }
                }

                override fun onLost(network: Network) {
                    _networkStatus.value = NetworkStatus.OFFLINE
                }
            })
        } catch (e: Exception) {
            Log.e("PinkLineRepo", "Error registering network callback: ${e.message}")
        }
    }

    fun setSimulatedNetworkStatus(status: NetworkStatus) {
        _networkStatus.value = status
        if (status == NetworkStatus.ONLINE) {
            coroutineScope.launch {
                syncPendingRequests()
            }
        }
    }

    suspend fun syncPendingRequests() {
        try {
            val pending = requestDao.getPendingSyncRequests()
            pending.forEach { req ->
                val synced = req.copy(syncStatus = SyncStatus.SYNCED.name)
                requestDao.updateRequest(synced)
            }
        } catch (e: Exception) {
            Log.e("PinkLineRepo", "Sync error: ${e.message}")
        }
    }

    // Journey calculations
    fun calculateJourneyDetails(
        sourceStation: Station,
        destinationStation: Station,
        direction: Direction,
        averageTravelTimeMinutes: Double
    ): Triple<Int, Double, Long> {
        val srcSeq = sourceStation.sequenceNumber
        val dstSeq = destinationStation.sequenceNumber

        val stationsDistance = when (direction) {
            Direction.PLUS_CIRCULAR -> {
                if (dstSeq >= srcSeq) {
                    dstSeq - srcSeq
                } else {
                    (37 - srcSeq) + dstSeq // circular wrap
                }
            }
            Direction.MINUS_CIRCULAR -> {
                if (srcSeq >= dstSeq) {
                    srcSeq - dstSeq
                } else {
                    srcSeq + (37 - dstSeq) // circular wrap
                }
            }
        }.coerceAtLeast(1)

        val estimatedTravelMinutes = stationsDistance * averageTravelTimeMinutes
        val arrivalTimeMillis = System.currentTimeMillis() + (estimatedTravelMinutes * 60 * 1000).toLong()

        return Triple(stationsDistance, estimatedTravelMinutes, arrivalTimeMillis)
    }

    suspend fun createAssistanceRequest(
        sourceStationName: String,
        destinationStationName: String,
        passengerType: PassengerType,
        passengerCount: Int,
        trainId: String,
        direction: Direction,
        notes: String = ""
    ): AssistanceRequest {
        val stations = stationDao.getAllStationsSync().map { it.toDomain() }
        val src = stations.find { it.name == sourceStationName }
            ?: stations.firstOrNull()
            ?: Station("AVH", sourceStationName, 28)
        val dst = stations.find { it.name == destinationStationName }
            ?: stations.lastOrNull()
            ?: Station("TLP", destinationStationName, 24)

        val config = configDao.getConfigSync()?.toDomain() ?: SystemConfig()
        val (dist, _, estArrival) = calculateJourneyDetails(src, dst, direction, config.averageTravelTimeMinutes)

        val uniqueReqId = "PL-REQ-${Random.nextInt(1000, 9999)}"
        val isOnline = _networkStatus.value == NetworkStatus.ONLINE

        val newRequest = AssistanceRequest(
            requestId = uniqueReqId,
            sourceStation = sourceStationName,
            destinationStation = destinationStationName,
            passengerType = passengerType,
            passengerCount = passengerCount,
            trainId = trainId.trim().ifBlank { "PL-${Random.nextInt(10, 99)}" },
            direction = direction,
            createdAt = System.currentTimeMillis(),
            status = AssistanceStatus.REQUEST_SENT,
            notes = notes,
            syncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.PENDING_SYNC,
            totalStationsDistance = dist,
            stationsRemaining = dist,
            estimatedArrivalMillis = estArrival,
            currentStationProgress = sourceStationName
        )

        requestDao.insertRequest(RequestEntity.fromDomain(newRequest))

        // Immediately trigger Destination Alert event
        _alertEvents.emit(AlertEvent.NewDestinationAlert(newRequest))

        return newRequest
    }

    suspend fun acknowledgeRequest(requestId: String, staffName: String): AssistanceRequest? {
        val entity = requestDao.getRequestById(requestId) ?: return null
        val currentDomain = entity.toDomain()

        val updated = currentDomain.copy(
            status = AssistanceStatus.ACKNOWLEDGED,
            acknowledgedBy = staffName,
            acknowledgedAt = System.currentTimeMillis()
        )

        requestDao.updateRequest(RequestEntity.fromDomain(updated))
        _alertEvents.emit(AlertEvent.RequestAcknowledgedAlert(updated))
        return updated
    }

    suspend fun rejectRequest(requestId: String, reason: String = "Cannot accept at this time") {
        val entity = requestDao.getRequestById(requestId) ?: return
        val updated = entity.toDomain().copy(
            status = AssistanceStatus.CANCELLED,
            notes = if (entity.notes.isBlank()) reason else "${entity.notes} | Rejected: $reason"
        )
        requestDao.updateRequest(RequestEntity.fromDomain(updated))
    }

    suspend fun markRequestCompleted(requestId: String, staffName: String, completionRemarks: String = "") {
        val entity = requestDao.getRequestById(requestId) ?: return
        val updated = entity.toDomain().copy(
            status = AssistanceStatus.COMPLETED,
            completedBy = staffName,
            completedAt = System.currentTimeMillis(),
            notes = if (completionRemarks.isBlank()) entity.notes else "${entity.notes} | $completionRemarks"
        )
        requestDao.updateRequest(RequestEntity.fromDomain(updated))
    }

    // Advance train journey (either automatically over time or on manual staff trigger)
    suspend fun advanceTrainProgress(requestId: String) {
        val entity = requestDao.getRequestById(requestId) ?: return
        val req = entity.toDomain()
        if (req.status == AssistanceStatus.COMPLETED || req.status == AssistanceStatus.CANCELLED) return

        val newRemaining = (req.stationsRemaining - 1).coerceAtLeast(0)
        var newStatus = req.status
        var twoStationTriggered = req.twoStationAlertTriggered
        var arrivalTriggered = req.arrivalAlertTriggered

        if (newRemaining in 1..2 && !twoStationTriggered) {
            newStatus = AssistanceStatus.TWO_STATION_REMINDER
            twoStationTriggered = true
            val alertReq = req.copy(
                stationsRemaining = newRemaining,
                status = newStatus,
                twoStationAlertTriggered = true
            )
            _alertEvents.emit(AlertEvent.TwoStationBeforeAlert(alertReq))
        } else if (newRemaining == 0 && !arrivalTriggered) {
            newStatus = AssistanceStatus.ARRIVING
            arrivalTriggered = true
            val alertReq = req.copy(
                stationsRemaining = 0,
                status = newStatus,
                arrivalAlertTriggered = true
            )
            _alertEvents.emit(AlertEvent.FinalArrivalAlert(alertReq))
        }

        val updated = req.copy(
            stationsRemaining = newRemaining,
            status = newStatus,
            twoStationAlertTriggered = twoStationTriggered,
            arrivalAlertTriggered = arrivalTriggered
        )
        requestDao.updateRequest(RequestEntity.fromDomain(updated))
    }

    private fun startJourneyProgressionMonitor() {
        coroutineScope.launch {
            while (true) {
                delay(18_000L) // Progress active journeys every 18 seconds for demo simulation
                try {
                    val activeList = requestDao.getActiveRequests().first()
                    val enRouteRequests = activeList.filter {
                        it.status == AssistanceStatus.ACKNOWLEDGED.name ||
                                it.status == AssistanceStatus.TWO_STATION_REMINDER.name
                    }
                    for (reqEntity in enRouteRequests) {
                        advanceTrainProgress(reqEntity.requestId)
                    }
                } catch (e: Exception) {
                    Log.e("PinkLineRepo", "Progression monitor error: ${e.message}")
                }
            }
        }
    }

    // Admin Configurations
    suspend fun updateSystemConfig(config: SystemConfig) {
        configDao.insertOrUpdateConfig(ConfigEntity.fromDomain(config))
    }

    suspend fun addStation(station: Station) {
        stationDao.insertStation(StationEntity.fromDomain(station))
    }

    suspend fun updateStation(station: Station) {
        stationDao.updateStation(StationEntity.fromDomain(station))
    }

    suspend fun deleteStation(station: Station) {
        stationDao.deleteStation(StationEntity.fromDomain(station))
    }

    suspend fun clearCompletedRequests() {
        requestDao.clearCompletedRequests()
    }
}

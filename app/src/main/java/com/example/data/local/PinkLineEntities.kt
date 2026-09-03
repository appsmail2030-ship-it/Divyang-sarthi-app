package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AssistanceRequest
import com.example.model.AssistanceStatus
import com.example.model.Direction
import com.example.model.PassengerType
import com.example.model.Station
import com.example.model.SyncStatus
import com.example.model.SystemConfig

@Entity(tableName = "assistance_requests")
data class RequestEntity(
    @PrimaryKey
    val requestId: String,
    val sourceStation: String,
    val destinationStation: String,
    val passengerType: String,
    val passengerCount: Int,
    val trainId: String,
    val direction: String,
    val createdAt: Long,
    val status: String,
    val acknowledgedBy: String?,
    val acknowledgedAt: Long?,
    val twoStationAlertTriggered: Boolean,
    val arrivalAlertTriggered: Boolean,
    val completedBy: String?,
    val completedAt: Long?,
    val notes: String,
    val syncStatus: String,
    val totalStationsDistance: Int,
    val stationsRemaining: Int,
    val estimatedArrivalMillis: Long,
    val currentStationProgress: String
) {
    fun toDomain(): AssistanceRequest {
        return AssistanceRequest(
            requestId = requestId,
            sourceStation = sourceStation,
            destinationStation = destinationStation,
            passengerType = try {
                PassengerType.valueOf(passengerType)
            } catch (e: Exception) {
                PassengerType.OTHER
            },
            passengerCount = passengerCount,
            trainId = trainId,
            direction = try {
                Direction.valueOf(direction)
            } catch (e: Exception) {
                Direction.PLUS_CIRCULAR
            },
            createdAt = createdAt,
            status = try {
                AssistanceStatus.valueOf(status)
            } catch (e: Exception) {
                AssistanceStatus.REQUEST_SENT
            },
            acknowledgedBy = acknowledgedBy,
            acknowledgedAt = acknowledgedAt,
            twoStationAlertTriggered = twoStationAlertTriggered,
            arrivalAlertTriggered = arrivalAlertTriggered,
            completedBy = completedBy,
            completedAt = completedAt,
            notes = notes,
            syncStatus = try {
                SyncStatus.valueOf(syncStatus)
            } catch (e: Exception) {
                SyncStatus.SYNCED
            },
            totalStationsDistance = totalStationsDistance,
            stationsRemaining = stationsRemaining,
            estimatedArrivalMillis = estimatedArrivalMillis,
            currentStationProgress = currentStationProgress
        )
    }

    companion object {
        fun fromDomain(model: AssistanceRequest): RequestEntity {
            return RequestEntity(
                requestId = model.requestId,
                sourceStation = model.sourceStation,
                destinationStation = model.destinationStation,
                passengerType = model.passengerType.name,
                passengerCount = model.passengerCount,
                trainId = model.trainId,
                direction = model.direction.name,
                createdAt = model.createdAt,
                status = model.status.name,
                acknowledgedBy = model.acknowledgedBy,
                acknowledgedAt = model.acknowledgedAt,
                twoStationAlertTriggered = model.twoStationAlertTriggered,
                arrivalAlertTriggered = model.arrivalAlertTriggered,
                completedBy = model.completedBy,
                completedAt = model.completedAt,
                notes = model.notes,
                syncStatus = model.syncStatus.name,
                totalStationsDistance = model.totalStationsDistance,
                stationsRemaining = model.stationsRemaining,
                estimatedArrivalMillis = model.estimatedArrivalMillis,
                currentStationProgress = model.currentStationProgress
            )
        }
    }
}

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey
    val code: String,
    val name: String,
    val sequenceNumber: Int,
    val isActive: Boolean,
    val interchangesCsv: String
) {
    fun toDomain(): Station {
        return Station(
            code = code,
            name = name,
            sequenceNumber = sequenceNumber,
            isActive = isActive,
            interchanges = if (interchangesCsv.isBlank()) emptyList() else interchangesCsv.split("|")
        )
    }

    companion object {
        fun fromDomain(model: Station): StationEntity {
            return StationEntity(
                code = model.code,
                name = model.name,
                sequenceNumber = model.sequenceNumber,
                isActive = model.isActive,
                interchangesCsv = model.interchanges.joinToString("|")
            )
        }
    }
}

@Entity(tableName = "system_config")
data class ConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val commonPassword: String,
    val adminPassword: String,
    val averageTravelTimeMinutes: Double,
    val sirenEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val ttsAnnouncementEnabled: Boolean,
    val twoStationDistanceThreshold: Int
) {
    fun toDomain(): SystemConfig {
        return SystemConfig(
            commonPassword = commonPassword,
            adminPassword = adminPassword,
            averageTravelTimeMinutes = averageTravelTimeMinutes,
            sirenEnabled = sirenEnabled,
            vibrationEnabled = vibrationEnabled,
            ttsAnnouncementEnabled = ttsAnnouncementEnabled,
            twoStationDistanceThreshold = twoStationDistanceThreshold
        )
    }

    companion object {
        fun fromDomain(model: SystemConfig): ConfigEntity {
            return ConfigEntity(
                id = 1,
                commonPassword = model.commonPassword,
                adminPassword = model.adminPassword,
                averageTravelTimeMinutes = model.averageTravelTimeMinutes,
                sirenEnabled = model.sirenEnabled,
                vibrationEnabled = model.vibrationEnabled,
                ttsAnnouncementEnabled = model.ttsAnnouncementEnabled,
                twoStationDistanceThreshold = model.twoStationDistanceThreshold
            )
        }
    }
}

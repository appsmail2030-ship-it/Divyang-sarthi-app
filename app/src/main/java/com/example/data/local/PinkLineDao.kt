package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestDao {
    @Query("SELECT * FROM assistance_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM assistance_requests WHERE status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY createdAt DESC")
    fun getActiveRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM assistance_requests WHERE destinationStation = :destination AND status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY createdAt DESC")
    fun getActiveRequestsForDestination(destination: String): Flow<List<RequestEntity>>

    @Query("SELECT * FROM assistance_requests WHERE sourceStation = :source ORDER BY createdAt DESC")
    fun getRequestsForSource(source: String): Flow<List<RequestEntity>>

    @Query("SELECT * FROM assistance_requests WHERE requestId = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: String): RequestEntity?

    @Query("SELECT * FROM assistance_requests WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncRequests(): List<RequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<RequestEntity>)

    @Update
    suspend fun updateRequest(request: RequestEntity)

    @Delete
    suspend fun deleteRequest(request: RequestEntity)

    @Query("DELETE FROM assistance_requests WHERE status = 'COMPLETED'")
    suspend fun clearCompletedRequests()
}

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY sequenceNumber ASC")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations ORDER BY sequenceNumber ASC")
    suspend fun getAllStationsSync(): List<StationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Update
    suspend fun updateStation(station: StationEntity)

    @Delete
    suspend fun deleteStation(station: StationEntity)

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun getStationCount(): Int
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM system_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<ConfigEntity?>

    @Query("SELECT * FROM system_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ConfigEntity)
}

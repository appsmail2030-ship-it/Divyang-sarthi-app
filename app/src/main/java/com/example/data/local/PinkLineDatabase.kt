package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.PinkLineStationsData
import com.example.model.SystemConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [RequestEntity::class, StationEntity::class, ConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PinkLineDatabase : RoomDatabase() {
    abstract fun requestDao(): RequestDao
    abstract fun stationDao(): StationDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: PinkLineDatabase? = null

        fun getInstance(context: Context): PinkLineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PinkLineDatabase::class.java,
                    "pink_line_assistance.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default stations and config asynchronously
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            val stationEntities = PinkLineStationsData.DEFAULT_STATIONS.map {
                                StationEntity.fromDomain(it)
                            }
                            database.stationDao().insertStations(stationEntities)
                            database.configDao().insertOrUpdateConfig(
                                ConfigEntity.fromDomain(SystemConfig())
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

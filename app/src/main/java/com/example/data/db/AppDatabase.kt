package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val vin: String,
    val vehicleModel: String,
    val totalDtcFound: Int,
    val dtcCodesJson: String // Serialized JSON list of DTC codes
)

@Entity(tableName = "sensor_logs")
data class SensorLogSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val timestamp: Long,
    val recordCount: Int,
    val durationSeconds: Long,
    val filePath: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val relatedDtcs: String = ""
)

@Dao
interface AppDao {
    // Scan History
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(scan: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history")
    suspend fun clearScanHistory()

    // Sensor Logs
    @Query("SELECT * FROM sensor_logs ORDER BY timestamp DESC")
    fun getAllLogSessions(): Flow<List<SensorLogSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogSession(session: SensorLogSessionEntity): Long

    @Query("DELETE FROM sensor_logs WHERE id = :id")
    suspend fun deleteLogSession(id: Long)

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteChatMessagesOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}

@Database(
    entities = [ScanHistoryEntity::class, SensorLogSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}

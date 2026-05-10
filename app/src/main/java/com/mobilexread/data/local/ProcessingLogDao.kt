package com.mobilexread.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessingLogDao {

    @Insert
    suspend fun insert(log: ProcessingLog)

    /** Most recent 200 entries, newest first */
    @Query("SELECT * FROM processing_logs ORDER BY timestampMillis DESC LIMIT 200")
    fun observeAll(): Flow<List<ProcessingLog>>

    /** Clear all logs */
    @Query("DELETE FROM processing_logs")
    suspend fun clearAll()
}

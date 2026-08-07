package com.elprompter.promptvault.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Query("SELECT * FROM activity_log ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ActivityLogEntity)

    @Query("DELETE FROM activity_log")
    suspend fun clearAll()

    /** Pangkas ke [maxEntries] baris terbaru saja (mencegah tabel tumbuh tanpa batas). */
    @Query(
        """
        DELETE FROM activity_log WHERE id NOT IN (
            SELECT id FROM activity_log ORDER BY timestampMillis DESC LIMIT :maxEntries
        )
        """
    )
    suspend fun trimToMax(maxEntries: Int)
}

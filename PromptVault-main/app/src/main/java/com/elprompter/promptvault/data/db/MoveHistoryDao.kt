package com.elprompter.promptvault.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoveHistoryDao {

    @Query("SELECT * FROM move_history ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<MoveHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoveHistoryEntity)

    @Query("UPDATE move_history SET undone = 1 WHERE id = :entryId")
    suspend fun markUndone(entryId: String)

    @Query("SELECT * FROM move_history WHERE undone = 0 ORDER BY timestampMillis DESC")
    suspend fun getUndoable(): List<MoveHistoryEntity>

    /** Pangkas ke [maxEntries] baris terbaru saja (mencegah tabel tumbuh tanpa batas). */
    @Query(
        """
        DELETE FROM move_history WHERE id NOT IN (
            SELECT id FROM move_history ORDER BY timestampMillis DESC LIMIT :maxEntries
        )
        """
    )
    suspend fun trimToMax(maxEntries: Int)
}

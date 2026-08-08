package app.omnisim.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.omnisim.android.data.local.entity.ReminderStateEntity
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class OmniSimDao {
    @Query("SELECT * FROM sims ORDER BY archived ASC, nextRenewalDate ASC, name COLLATE NOCASE ASC")
    abstract fun observeAllSims(): Flow<List<SimEntity>>

    @Query("SELECT * FROM renewal_history ORDER BY renewalDate DESC, createdAt DESC")
    abstract fun observeAllHistory(): Flow<List<RenewalHistoryEntity>>

    @Query("SELECT * FROM sims ORDER BY nextRenewalDate ASC")
    abstract suspend fun getAllSims(): List<SimEntity>

    @Query("SELECT * FROM renewal_history ORDER BY createdAt ASC")
    abstract suspend fun getAllHistory(): List<RenewalHistoryEntity>

    @Query("SELECT * FROM reminder_state")
    abstract suspend fun getAllReminderStates(): List<ReminderStateEntity>

    @Query("SELECT * FROM sims WHERE archived = 0 ORDER BY nextRenewalDate ASC")
    abstract suspend fun getActiveSims(): List<SimEntity>

    @Query("SELECT * FROM sims WHERE id = :id LIMIT 1")
    abstract suspend fun getSim(id: String): SimEntity?

    @Upsert
    abstract suspend fun upsertSim(sim: SimEntity)

    @Upsert
    abstract suspend fun upsertSims(sims: List<SimEntity>)

    @Insert
    abstract suspend fun insertHistory(history: RenewalHistoryEntity)

    @Insert
    abstract suspend fun insertHistory(history: List<RenewalHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertReminderState(state: ReminderStateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertReminderStates(states: List<ReminderStateEntity>)

    @Query("DELETE FROM reminder_state WHERE simId = :simId")
    abstract suspend fun deleteReminderStatesForSim(simId: String)

    @Query("DELETE FROM reminder_state")
    abstract suspend fun clearReminderStates()

    @Query("DELETE FROM renewal_history")
    abstract suspend fun clearHistory()

    @Query("DELETE FROM sims")
    abstract suspend fun clearSims()

    @Query("DELETE FROM sims WHERE id = :id")
    abstract suspend fun deleteSim(id: String)

    @Transaction
    open suspend fun replaceAll(
        sims: List<SimEntity>,
        history: List<RenewalHistoryEntity>,
        reminderStates: List<ReminderStateEntity> = emptyList(),
    ) {
        clearReminderStates()
        clearHistory()
        clearSims()
        upsertSims(sims)
        insertHistory(history)
        insertReminderStates(reminderStates)
    }
}


package app.omnisim.android.data.repository

import androidx.room.withTransaction
import app.omnisim.android.data.local.OmniSimDatabase
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class SimRepository(
    private val database: OmniSimDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dao = database.dao()

    fun observeSims(): Flow<List<SimEntity>> = dao.observeAllSims()

    fun observeHistory(): Flow<List<RenewalHistoryEntity>> = dao.observeAllHistory()

    suspend fun save(sim: SimEntity) {
        database.withTransaction {
            val previousRenewalDate = dao.getSim(sim.id)?.nextRenewalDate
            dao.upsertSim(sim)
            if (previousRenewalDate != null && previousRenewalDate != sim.nextRenewalDate) {
                dao.deleteReminderStatesForSim(sim.id)
            }
        }
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        val existing = dao.getSim(id) ?: return
        dao.upsertSim(existing.copy(archived = archived, updatedAt = Instant.now(clock)))
        if (archived) dao.deleteReminderStatesForSim(id)
    }

    suspend fun setReminderSettings(
        id: String,
        enabled: Boolean,
        offsets: Set<Int>?,
    ) {
        database.withTransaction {
            val existing = dao.getSim(id) ?: return@withTransaction
            dao.upsertSim(
                existing.copy(
                    remindersEnabled = enabled,
                    reminderOffsets = offsets,
                    updatedAt = Instant.now(clock),
                ),
            )
            dao.deleteReminderStatesForSim(id)
        }
    }

    suspend fun delete(id: String) = dao.deleteSim(id)

    suspend fun recordRenewal(
        simId: String,
        renewalDate: LocalDate,
        nextRenewalDate: LocalDate,
        amount: Double?,
        notes: String?,
    ) {
        database.withTransaction {
            val sim = dao.getSim(simId) ?: error("SIM not found")
            val now = Instant.now(clock)
            dao.insertHistory(
                RenewalHistoryEntity(
                    id = UUID.randomUUID().toString(),
                    simId = sim.id,
                    renewalDate = renewalDate,
                    previousRenewalDate = sim.lastRenewalDate,
                    previousNextRenewalDate = sim.nextRenewalDate,
                    previousRenewalPrice = sim.renewalPrice,
                    nextRenewalDate = nextRenewalDate,
                    amount = amount,
                    currency = sim.currency,
                    notes = notes,
                    createdAt = now,
                ),
            )
            dao.upsertSim(
                sim.copy(
                    lastRenewalDate = renewalDate,
                    nextRenewalDate = nextRenewalDate,
                    renewalPrice = amount ?: sim.renewalPrice,
                    updatedAt = now,
                ),
            )
            dao.deleteReminderStatesForSim(sim.id)
        }
    }

    suspend fun updateRenewal(
        historyId: String,
        renewalDate: LocalDate,
        nextRenewalDate: LocalDate,
        amount: Double?,
        notes: String?,
    ) {
        database.withTransaction {
            val history = dao.getHistory(historyId) ?: error("Renewal history not found")
            val sim = dao.getSim(history.simId) ?: error("SIM not found")
            val latest = dao.getLatestHistoryForSim(sim.id)
            dao.updateHistory(
                history.copy(
                    renewalDate = renewalDate,
                    nextRenewalDate = nextRenewalDate,
                    amount = amount,
                    notes = notes,
                ),
            )
            if (latest?.id == history.id) {
                dao.upsertSim(
                    sim.copy(
                        lastRenewalDate = renewalDate,
                        nextRenewalDate = nextRenewalDate,
                        renewalPrice = amount ?: sim.renewalPrice,
                        updatedAt = Instant.now(clock),
                    ),
                )
                dao.deleteReminderStatesForSim(sim.id)
            }
        }
    }

    suspend fun undoLatestRenewal(historyId: String) {
        database.withTransaction {
            val history = dao.getHistory(historyId) ?: error("Renewal history not found")
            val previousNextRenewalDate = history.previousNextRenewalDate
                ?: error("Renewal cannot be safely undone")
            val latest = dao.getLatestHistoryForSim(history.simId)
            require(latest?.id == history.id) { "Only the latest renewal can be undone" }
            val sim = dao.getSim(history.simId) ?: error("SIM not found")
            dao.deleteHistory(history.id)
            dao.upsertSim(
                sim.copy(
                    lastRenewalDate = history.previousRenewalDate,
                    nextRenewalDate = previousNextRenewalDate,
                    renewalPrice = history.previousRenewalPrice,
                    updatedAt = Instant.now(clock),
                ),
            )
            dao.deleteReminderStatesForSim(sim.id)
        }
    }
}

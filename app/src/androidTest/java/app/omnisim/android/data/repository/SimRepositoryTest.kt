package app.omnisim.android.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.omnisim.android.data.local.OmniSimDatabase
import app.omnisim.android.data.local.entity.ReminderStateEntity
import app.omnisim.android.data.local.entity.SimEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimRepositoryTest {
    private lateinit var database: OmniSimDatabase
    private lateinit var repository: SimRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OmniSimDatabase::class.java,
        ).build()
        repository = SimRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun changingRenewalDateClearsReminderState() = runBlocking {
        val originalDate = LocalDate.of(2026, 9, 1)
        val sim = testSim(originalDate)
        repository.save(sim)
        database.dao().insertReminderState(
            ReminderStateEntity(sim.id, originalDate, 14, Instant.EPOCH),
        )

        repository.save(sim.copy(notes = "Updated"))
        assertEquals(1, database.dao().getAllReminderStates().size)

        repository.save(sim.copy(nextRenewalDate = LocalDate.of(2026, 10, 1)))
        assertTrue(database.dao().getAllReminderStates().isEmpty())
    }

    @Test
    fun undoLatestRenewalRestoresSimSnapshotAndClearsReminderState() = runBlocking {
        val originalNext = LocalDate.of(2026, 9, 1)
        val originalLast = LocalDate.of(2026, 8, 1)
        val sim = testSim(originalNext).copy(
            lastRenewalDate = originalLast,
            renewalPrice = 12.0,
        )
        repository.save(sim)
        repository.recordRenewal(
            simId = sim.id,
            renewalDate = LocalDate.of(2026, 9, 2),
            nextRenewalDate = LocalDate.of(2026, 10, 2),
            amount = 15.0,
            notes = "Paid",
        )
        val history = database.dao().getAllHistory().single()
        database.dao().insertReminderState(
            ReminderStateEntity(
                sim.id,
                LocalDate.of(2026, 10, 2),
                14,
                Instant.EPOCH,
            ),
        )

        repository.undoLatestRenewal(history.id)

        val restored = database.dao().getSim(sim.id)!!
        assertEquals(originalLast, restored.lastRenewalDate)
        assertEquals(originalNext, restored.nextRenewalDate)
        assertEquals(12.0, restored.renewalPrice)
        assertTrue(database.dao().getAllHistory().isEmpty())
        assertTrue(database.dao().getAllReminderStates().isEmpty())
    }

    @Test
    fun updatingLatestRenewalUpdatesCurrentSimDates() = runBlocking {
        val sim = testSim(LocalDate.of(2026, 9, 1))
        repository.save(sim)
        repository.recordRenewal(
            simId = sim.id,
            renewalDate = LocalDate.of(2026, 9, 1),
            nextRenewalDate = LocalDate.of(2026, 10, 1),
            amount = 15.0,
            notes = null,
        )
        val history = database.dao().getAllHistory().single()

        repository.updateRenewal(
            historyId = history.id,
            renewalDate = LocalDate.of(2026, 9, 3),
            nextRenewalDate = LocalDate.of(2026, 10, 3),
            amount = 18.0,
            notes = "Corrected",
        )

        val updated = database.dao().getSim(sim.id)!!
        val updatedHistory = database.dao().getHistory(history.id)!!
        assertEquals(LocalDate.of(2026, 9, 3), updated.lastRenewalDate)
        assertEquals(LocalDate.of(2026, 10, 3), updated.nextRenewalDate)
        assertEquals(18.0, updated.renewalPrice)
        assertEquals("Corrected", updatedHistory.notes)
    }

    @Test
    fun perSimReminderSettingsPersistAndClearSentState() = runBlocking {
        val renewalDate = LocalDate.of(2026, 9, 1)
        val sim = testSim(renewalDate)
        repository.save(sim)
        database.dao().insertReminderState(
            ReminderStateEntity(sim.id, renewalDate, 14, Instant.EPOCH),
        )

        repository.setReminderSettings(sim.id, enabled = true, offsets = setOf(7, 1))

        val updated = database.dao().getSim(sim.id)!!
        assertTrue(updated.remindersEnabled)
        assertEquals(setOf(7, 1), updated.reminderOffsets)
        assertTrue(database.dao().getAllReminderStates().isEmpty())
    }

    private fun testSim(nextRenewalDate: LocalDate) = SimEntity(
        id = "sim-1",
        name = "Primary",
        carrier = "Carrier",
        nextRenewalDate = nextRenewalDate,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

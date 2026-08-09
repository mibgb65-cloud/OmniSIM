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

    private fun testSim(nextRenewalDate: LocalDate) = SimEntity(
        id = "sim-1",
        name = "Primary",
        carrier = "Carrier",
        nextRenewalDate = nextRenewalDate,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

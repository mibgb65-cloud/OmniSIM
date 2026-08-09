package app.omnisim.android.backup

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class RenewalHistoryCsvTest {
    @Test
    fun `csv is utf8 friendly and escapes user text`() {
        val sim = SimEntity(
            id = "sim-1",
            name = "Main, SIM",
            carrier = "Carrier",
            nextRenewalDate = LocalDate.of(2026, 10, 1),
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val renewal = RenewalHistoryEntity(
            id = "renewal-1",
            simId = sim.id,
            renewalDate = LocalDate.of(2026, 9, 1),
            previousRenewalDate = null,
            nextRenewalDate = LocalDate.of(2026, 10, 1),
            amount = 12.0,
            currency = "USD",
            notes = "Paid \"online\"",
            createdAt = Instant.EPOCH,
        )

        val csv = RenewalHistoryCsv.encode(listOf(sim), listOf(renewal))

        assertTrue(csv.startsWith("\uFEFF\"SIM\""))
        assertTrue("\"Main, SIM\"" in csv)
        assertTrue("\"Paid \"\"online\"\"\"" in csv)
    }
}

package app.omnisim.android.backup

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val sim = SimEntity(
        id = "sim-1",
        name = "Tello",
        carrier = "Tello",
        countryCode = "US",
        countryName = "United States",
        phoneNumber = "+1 202 555 8821",
        simType = "eSIM",
        nextRenewalDate = LocalDate.of(2026, 8, 10),
        renewalCycleDays = 90,
        renewalPrice = 10.0,
        currency = "USD",
        renewalUrl = "https://tello.com/account",
        archived = false,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-08-01T00:00:00Z"),
    )
    private val renewal = RenewalHistoryEntity(
        id = "renewal-1",
        simId = sim.id,
        renewalDate = LocalDate.of(2026, 5, 12),
        previousRenewalDate = null,
        nextRenewalDate = LocalDate.of(2026, 8, 10),
        amount = 10.0,
        currency = "USD",
        notes = null,
        createdAt = Instant.parse("2026-05-12T00:00:00Z"),
    )

    @Test
    fun `valid backup round trips important data`() {
        val encoded = BackupCodec.encode(
            listOf(sim),
            listOf(renewal),
            AppSettings(),
            Instant.parse("2026-08-07T00:00:00Z"),
        )
        val decoded = BackupCodec.decode(encoded)

        assertEquals(listOf(sim), decoded.sims)
        assertEquals(listOf(renewal), decoded.history)
        assertEquals(AppSettings(), decoded.settings)
    }

    @Test
    fun `invalid json is rejected`() {
        assertThrows(BackupValidationException::class.java) {
            BackupCodec.decode("not json")
        }
    }

    @Test
    fun `unsupported backup version is rejected`() {
        val encoded = BackupCodec.encode(listOf(sim), emptyList(), AppSettings())
            .replace("\"backupVersion\": 1", "\"backupVersion\": 99")
        assertThrows(BackupValidationException::class.java) {
            BackupCodec.decode(encoded)
        }
    }

    @Test
    fun `missing required field is rejected`() {
        val missingSims = """
            {
              "backupVersion": 1,
              "exportedAt": "2026-08-07T00:00:00Z",
              "renewalHistory": [],
              "settings": {
                "themeMode": "System",
                "dynamicColor": true,
                "warningPeriodDays": 14,
                "maskPhoneNumbers": true,
                "reminderOffsets": [14, 7, 0],
                "defaultCurrency": "USD"
              }
            }
        """.trimIndent()
        assertThrows(BackupValidationException::class.java) {
            BackupCodec.decode(missingSims)
        }
    }

    @Test
    fun `unsafe renewal website is rejected`() {
        val encoded = BackupCodec.encode(listOf(sim), emptyList(), AppSettings())
            .replace("https://tello.com/account", "javascript:alert(1)")
        assertThrows(BackupValidationException::class.java) {
            BackupCodec.decode(encoded)
        }
    }
}


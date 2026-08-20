package app.omnisim.android.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimDraftValidationTest {
    private fun draft(
        cycleDays: Int? = null,
        dayOfMonth: Int? = null,
    ) = SimDraft(
        name = "Primary",
        carrier = "Carrier",
        countryCode = null,
        countryName = null,
        phoneNumber = null,
        simType = "eSIM",
        planName = null,
        lastRenewalDate = null,
        nextRenewalDate = LocalDate.of(2026, 9, 1),
        renewalCycleDays = cycleDays,
        renewalDayOfMonth = dayOfMonth,
        renewalPrice = null,
        currency = null,
        renewalUrl = null,
        notes = null,
    )

    @Test
    fun `no automatic renewal schedule is valid`() {
        assertNull(validateSimDraft(draft()))
    }

    @Test
    fun `monthly renewal day is valid`() {
        assertNull(validateSimDraft(draft(dayOfMonth = 1)))
    }

    @Test
    fun `monthly renewal day must be between one and thirty one`() {
        assertEquals(
            SimDraftValidationError.InvalidMonthlyDay,
            validateSimDraft(draft(dayOfMonth = 32)),
        )
    }

    @Test
    fun `day cycle and monthly day cannot both be selected`() {
        assertEquals(
            SimDraftValidationError.ConflictingSchedule,
            validateSimDraft(draft(cycleDays = 30, dayOfMonth = 1)),
        )
    }

    @Test
    fun `unsupported currency is rejected`() {
        assertEquals(
            SimDraftValidationError.InvalidCurrency,
            validateSimDraft(draft().copy(currency = "ZZZ")),
        )
    }

    @Test
    fun `currency supplied by an official rate snapshot is valid`() {
        assertNull(
            validateSimDraft(
                draft().copy(currency = "QZZ"),
                officialCurrencyCodes = setOf("QZZ"),
            ),
        )
    }
}

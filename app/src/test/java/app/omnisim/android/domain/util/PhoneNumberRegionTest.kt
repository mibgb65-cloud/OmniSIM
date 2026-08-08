package app.omnisim.android.domain.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberRegionTest {
    @Test
    fun `detects country and calling code from international number`() {
        assertEquals(
            PhoneNumberRegion(regionCode = "GB", callingCode = "+44"),
            detectPhoneNumberRegion("+44 7857779654"),
        )
        assertEquals(
            PhoneNumberRegion(regionCode = "JP", callingCode = "+81"),
            detectPhoneNumberRegion("+81 90 1234 5678"),
        )
        assertEquals(
            PhoneNumberRegion(regionCode = "US", callingCode = "+1"),
            detectPhoneNumberRegion("+1 502-853-8569"),
        )
    }

    @Test
    fun `rejects local and incomplete numbers`() {
        assertNull(detectPhoneNumberRegion("07857 779654"))
        assertNull(detectPhoneNumberRegion("+44"))
    }

    @Test
    fun `country name follows the requested locale`() {
        assertEquals("United Kingdom", localizedCountryName("GB", Locale.ENGLISH))
        assertEquals("英国", localizedCountryName("GB", Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun `country list contains localized name flag and calling code`() {
        val unitedKingdom = callingCodeCountries(Locale.SIMPLIFIED_CHINESE)
            .first { it.regionCode == "GB" }

        assertEquals("英国", unitedKingdom.countryName)
        assertEquals("United Kingdom", unitedKingdom.englishName)
        assertEquals("+44", unitedKingdom.callingCode)
        assertEquals("🇬🇧", unitedKingdom.flag)
    }

    @Test
    fun `country flag follows detected phone region before fallback`() {
        val regionCode = resolvePhoneNumberRegionCode("+44 7857 779654", "US")

        assertEquals("GB", regionCode)
        assertEquals("🇬🇧", countryFlag(regionCode))
    }

    @Test
    fun `country flag uses valid fallback when phone cannot be detected`() {
        assertEquals("US", resolvePhoneNumberRegionCode(null, "us"))
        assertEquals("🇺🇸", countryFlag("US"))
        assertNull(countryFlag("USA"))
    }

    @Test
    fun `international number is split into country and national number`() {
        assertEquals(
            PhoneInputParts(regionCode = "GB", nationalNumber = "7857 779654"),
            splitInternationalPhoneNumber(
                value = "+44 7857 779654",
                locale = Locale.ENGLISH,
            ),
        )
        assertEquals(
            PhoneInputParts(regionCode = "GB", nationalNumber = "7857779654"),
            splitInternationalPhoneNumber(
                value = "7857779654",
                fallbackRegionCode = "44",
                locale = Locale.ENGLISH,
            ),
        )
    }

    @Test
    fun `national number is saved in international format`() {
        assertEquals(
            "+44 7857 779654",
            formatInternationalPhoneNumber("GB", "07857 779654"),
        )
    }
}

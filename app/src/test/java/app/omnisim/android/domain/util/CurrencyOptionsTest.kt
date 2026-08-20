package app.omnisim.android.domain.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyOptionsTest {
    @Test
    fun `local currency is listed first`() {
        assertEquals("USD", currencyOptions(Locale.US).first().code)
    }

    @Test
    fun `currency search matches code regardless of case`() {
        val result = filterCurrencyOptions(currencyOptions(Locale.ENGLISH), "sgd")

        assertEquals(listOf("SGD"), result.map(CurrencyOption::code))
    }

    @Test
    fun `official rate codes missing from the runtime are added once`() {
        val result = currencyOptions(Locale.ENGLISH, setOf("QZZ", "USD"))

        assertEquals(1, result.count { it.code == "USD" })
        assertEquals(1, result.count { it.code == "QZZ" })
    }

    @Test
    fun `supported currency validation rejects unknown codes`() {
        assertTrue(isSupportedCurrencyCode("usd"))
        assertFalse(isSupportedCurrencyCode("ABC"))
        assertFalse(isSupportedCurrencyCode("XXX"))
    }

    @Test
    fun `official rate codes are accepted even when the runtime does not know them`() {
        assertTrue(isSupportedCurrencyCode("qzz", setOf("QZZ")))
    }

    @Test
    fun `current supplemental official currencies remain available offline`() {
        val currencies = currencyOptions(Locale.ENGLISH).map(CurrencyOption::code)

        assertTrue(isSupportedCurrencyCode("xcg"))
        assertTrue(isSupportedCurrencyCode("zig"))
        assertTrue("XCG" in currencies)
        assertTrue("ZIG" in currencies)
    }
}

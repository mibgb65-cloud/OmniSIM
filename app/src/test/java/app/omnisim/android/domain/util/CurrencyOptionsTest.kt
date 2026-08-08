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
    fun `supported currency validation rejects unknown codes`() {
        assertTrue(isSupportedCurrencyCode("usd"))
        assertFalse(isSupportedCurrencyCode("ABC"))
        assertFalse(isSupportedCurrencyCode("XXX"))
    }
}

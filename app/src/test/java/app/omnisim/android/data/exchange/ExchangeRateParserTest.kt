package app.omnisim.android.data.exchange

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExchangeRateParserTest {
    @Test
    fun `parses ECB date and rates with euro base`() {
        val parsed = parseEcbExchangeRates(
            """
            <Envelope>
              <Cube>
                <Cube time='2026-08-07'>
                  <Cube currency='USD' rate='1.20'/>
                  <Cube rate="8.00" currency="CNY"/>
                </Cube>
              </Cube>
            </Envelope>
            """.trimIndent(),
        )

        assertEquals(LocalDate.of(2026, 8, 7), parsed.rateDate)
        assertEquals(1.0, parsed.ratesPerEuro.getValue("EUR"), 0.0)
        assertEquals(1.2, parsed.ratesPerEuro.getValue("USD"), 0.0)
        assertEquals(8.0, parsed.ratesPerEuro.getValue("CNY"), 0.0)
    }

    @Test
    fun `rejects documents without a date or usable rates`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseEcbExchangeRates("<Cube currency='USD' rate='1.20'/>")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseEcbExchangeRates("<Cube time='2026-08-07'/>")
        }
    }
}

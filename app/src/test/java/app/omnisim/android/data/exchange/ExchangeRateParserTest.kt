package app.omnisim.android.data.exchange

import java.time.LocalDate
import java.time.YearMonth
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

    @Test
    fun `parses additional InforEuro currencies with euro base`() {
        val parsed = parseInforEuroExchangeRates(
            """
            [
              {"isoA3Code":"EUR","value":1,"country":"Belgium"},
              {"isoA3Code":"TWD","value":37.1088,"country":"Taiwan"},
              {"isoA3Code":"VND","value":30207.701,"country":"Vietnam"},
              {"isoA3Code":"AED","value":4.212,"country":"United Arab Emirates"},
              {"isoA3Code":"SAR","value":4.30855,"country":"Saudi Arabia"}
            ]
            """.trimIndent(),
            YearMonth.of(2026, 8),
        )

        assertEquals(LocalDate.of(2026, 8, 1), parsed.rateDate)
        assertEquals(1.0, parsed.ratesPerEuro.getValue("EUR"), 0.0)
        assertEquals(37.1088, parsed.ratesPerEuro.getValue("TWD"), 0.0)
        assertEquals(30207.701, parsed.ratesPerEuro.getValue("VND"), 0.0)
        assertEquals(4.212, parsed.ratesPerEuro.getValue("AED"), 0.0)
        assertEquals(4.30855, parsed.ratesPerEuro.getValue("SAR"), 0.0)
    }

    @Test
    fun `rejects invalid InforEuro documents`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseInforEuroExchangeRates("[]", YearMonth.of(2026, 8))
        }
    }
}

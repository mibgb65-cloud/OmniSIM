package app.omnisim.android.ui

import app.omnisim.android.data.exchange.ExchangeRateSourcesUnavailableException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeRateUiStateTest {
    @Test
    fun `network errors have a useful failure reason`() {
        assertEquals(
            ExchangeRateFailureReason.NetworkOrService,
            IOException("offline").toExchangeRateFailureReason(),
        )
    }

    @Test
    fun `invalid official data has a useful failure reason`() {
        val error = ExchangeRateSourcesUnavailableException(
            listOf(IOException("offline"), IllegalArgumentException("invalid data")),
        )

        assertEquals(
            ExchangeRateFailureReason.InvalidData,
            error.toExchangeRateFailureReason(),
        )
    }

    @Test
    fun `unexpected errors keep a generic failure reason`() {
        assertEquals(
            ExchangeRateFailureReason.Unknown,
            IllegalStateException("unexpected").toExchangeRateFailureReason(),
        )
    }
}

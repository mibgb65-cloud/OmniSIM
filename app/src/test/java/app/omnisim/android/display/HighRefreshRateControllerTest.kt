package app.omnisim.android.display

import org.junit.Assert.assertEquals
import org.junit.Test

class HighRefreshRateControllerTest {
    @Test
    fun `selects exact 120hz mode when available`() {
        assertEquals(120f, select(60f, 90f, 120f), 0.001f)
    }

    @Test
    fun `falls back to nearest supported high refresh rate`() {
        assertEquals(90f, select(60f, 90f), 0.001f)
    }

    @Test
    fun `remains compatible with a 60hz display`() {
        assertEquals(60f, select(60f), 0.001f)
    }

    @Test
    fun `prefers the higher rate when two modes are equally close`() {
        assertEquals(150f, select(90f, 150f), 0.001f)
    }

    @Test
    fun `ignores invalid rates`() {
        assertEquals(120f, select(Float.NaN, -1f, 0f, 120f), 0.001f)
    }

    private fun select(vararg rates: Float): Float =
        HighRefreshRateController.selectClosestSupportedRate(
            supportedRates = rates.asIterable(),
            targetRate = HighRefreshRateController.TARGET_REFRESH_RATE,
        )
}

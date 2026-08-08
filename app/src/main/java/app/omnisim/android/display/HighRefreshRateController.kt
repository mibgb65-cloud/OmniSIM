package app.omnisim.android.display

import android.app.Activity
import android.os.Build
import android.view.Display
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs

internal object HighRefreshRateController {
    const val TARGET_REFRESH_RATE = 120f

    fun request(activity: Activity) {
        val preferredRate = if (Build.VERSION.SDK_INT >= 34) {
            TARGET_REFRESH_RATE
        } else {
            selectClosestSupportedRate(
                supportedRates = supportedRatesForCurrentResolution(activity),
                targetRate = TARGET_REFRESH_RATE,
            )
        }

        if (preferredRate > 0f) {
            activity.window.attributes = activity.window.attributes.apply {
                preferredRefreshRate = preferredRate
            }
        }

        if (Build.VERSION.SDK_INT >= 36) {
            (activity.window.decorView as? ViewGroup)?.propagateRequestedFrameRate(
                View.REQUESTED_FRAME_RATE_CATEGORY_HIGH,
                false,
            )
        } else if (Build.VERSION.SDK_INT >= 35) {
            activity.window.decorView.setRequestedFrameRate(
                View.REQUESTED_FRAME_RATE_CATEGORY_HIGH,
            )
        }
    }

    internal fun selectClosestSupportedRate(
        supportedRates: Iterable<Float>,
        targetRate: Float,
    ): Float = supportedRates
        .filter { it.isFinite() && it > 0f }
        .minWithOrNull(
            compareBy<Float> { abs(it - targetRate) }
                .thenByDescending { it },
        )
        ?: 0f

    @Suppress("DEPRECATION")
    private fun supportedRatesForCurrentResolution(activity: Activity): List<Float> {
        val display: Display = if (Build.VERSION.SDK_INT >= 30) {
            activity.display
        } else {
            activity.windowManager.defaultDisplay
        } ?: return emptyList()
        val currentMode = display.mode
        return display.supportedModes
            .asSequence()
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .map(Display.Mode::getRefreshRate)
            .distinct()
            .toList()
    }
}

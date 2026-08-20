package app.omnisim.android.ui

import app.omnisim.android.data.update.AppReleaseInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateUiStateTest {
    private val release = AppReleaseInfo(
        version = "1.2.0",
        title = "OmniSIM 1.2.0",
        notes = null,
        apkDownloadUrl = "https://github.com/example/release.apk",
        checksumDownloadUrl = "https://github.com/example/release.apk.sha256",
    )

    @Test
    fun automaticUpdate_waitsUntilHome() {
        val state = AppUpdateUiState.Available(release, userInitiated = false)

        assertEquals(AppUpdateUiState.Idle, appUpdateStateForRoute(state, isHome = false))
        assertEquals(state, appUpdateStateForRoute(state, isHome = true))
    }

    @Test
    fun userInitiatedUpdate_isVisibleOnAnyRoute() {
        val state = AppUpdateUiState.Available(release, userInitiated = true)

        assertEquals(state, appUpdateStateForRoute(state, isHome = false))
    }
}

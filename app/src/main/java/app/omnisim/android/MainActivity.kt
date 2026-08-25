package app.omnisim.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import app.omnisim.android.ui.AppViewModel
import app.omnisim.android.ui.AppViewModelFactory
import app.omnisim.android.ui.navigation.OmniSimApp

class MainActivity : AppCompatActivity() {
    private var requestedSimId by mutableStateOf<String?>(null)
    private var requestedRenewal by mutableStateOf(false)
    private var systemSplashExited by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { provider ->
            systemSplashExited = true
            provider.remove()
        }
        enableEdgeToEdge()
        requestedSimId = intent.getStringExtra(EXTRA_SIM_ID)
        requestedRenewal = intent.getBooleanExtra(EXTRA_OPEN_RENEWAL, false)
        val playLaunchAnimation = savedInstanceState == null
        val container = (application as OmniSimApplication).container
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory(container))
            OmniSimApp(
                viewModel = appViewModel,
                externalSimId = requestedSimId,
                externalRenewalRequested = requestedRenewal,
                onExternalNavigationHandled = {
                    requestedSimId = null
                    requestedRenewal = false
                },
                playLaunchAnimation = playLaunchAnimation,
                launchAnimationStarted = systemSplashExited,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedSimId = intent.getStringExtra(EXTRA_SIM_ID)
        requestedRenewal = intent.getBooleanExtra(EXTRA_OPEN_RENEWAL, false)
    }

    companion object {
        const val EXTRA_SIM_ID = "sim_id"
        const val EXTRA_OPEN_RENEWAL = "open_renewal"
    }
}

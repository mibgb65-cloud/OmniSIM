package app.omnisim.android.data.update

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface AppUpdateDownloadState {
    data class Downloading(
        val release: AppReleaseInfo,
        val progressPercent: Int?,
    ) : AppUpdateDownloadState

    data class Ready(
        val release: AppReleaseInfo,
        val apkFile: File,
    ) : AppUpdateDownloadState

    data class Failed(val release: AppReleaseInfo) : AppUpdateDownloadState
}

class AppUpdateDownloadController(
    private val repository: AppUpdateRepository,
    private val onState: (AppUpdateDownloadState) -> Unit,
) {
    private var job: Job? = null

    fun download(scope: CoroutineScope, release: AppReleaseInfo) {
        if (job?.isActive == true) return
        job = scope.launch {
            onState(AppUpdateDownloadState.Downloading(release, 0))
            try {
                val file = repository.downloadAndVerify(release) { downloaded, total ->
                    val progress = total
                        ?.takeIf { it > 0L }
                        ?.let { (downloaded * 100L / it).toInt().coerceIn(0, 100) }
                    onState(AppUpdateDownloadState.Downloading(release, progress))
                }
                onState(AppUpdateDownloadState.Ready(release, file))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onState(AppUpdateDownloadState.Failed(release))
            } finally {
                job = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

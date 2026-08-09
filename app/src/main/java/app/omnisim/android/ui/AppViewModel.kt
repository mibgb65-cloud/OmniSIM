package app.omnisim.android.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.omnisim.android.AppContainer
import app.omnisim.android.R
import app.omnisim.android.backup.BackupPayload
import app.omnisim.android.backup.isSafeWebUrl
import app.omnisim.android.data.exchange.ExchangeRateSnapshot
import app.omnisim.android.data.exchange.isFresh
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.data.preferences.ThemeMode
import app.omnisim.android.data.update.AppReleaseInfo
import app.omnisim.android.data.update.compareVersionNames
import app.omnisim.android.domain.util.isSupportedCurrencyCode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val sims: List<SimEntity> = emptyList(),
    val history: List<RenewalHistoryEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val pendingRestore: BackupPayload? = null,
    val exchangeRates: ExchangeRateUiState = ExchangeRateUiState.Idle,
)

sealed interface ExchangeRateUiState {
    data object Idle : ExchangeRateUiState
    data object Loading : ExchangeRateUiState
    data object Unavailable : ExchangeRateUiState

    data class Ready(
        val snapshot: ExchangeRateSnapshot,
        val isRefreshing: Boolean = false,
        val refreshFailed: Boolean = false,
    ) : ExchangeRateUiState
}

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Checking : AppUpdateUiState
    data object Failed : AppUpdateUiState

    data class UpToDate(val latestVersion: String) : AppUpdateUiState

    data class Available(val release: AppReleaseInfo) : AppUpdateUiState
}

data class SimDraft(
    val id: String? = null,
    val name: String,
    val carrier: String,
    val countryCode: String?,
    val countryName: String?,
    val phoneNumber: String?,
    val simType: String,
    val planName: String?,
    val lastRenewalDate: LocalDate?,
    val nextRenewalDate: LocalDate,
    val renewalCycleDays: Int?,
    val renewalDayOfMonth: Int?,
    val renewalPrice: Double?,
    val currency: String?,
    val renewalUrl: String?,
    val notes: String?,
)

data class UiMessage(@param:StringRes val text: Int)

enum class SimDraftValidationError {
    NameRequired,
    CarrierRequired,
    InvalidSimType,
    InvalidCycle,
    InvalidMonthlyDay,
    ConflictingSchedule,
    InvalidPrice,
    InvalidCurrency,
    InvalidWebsite,
}

fun validateSimDraft(draft: SimDraft): SimDraftValidationError? = when {
    draft.name.isBlank() -> SimDraftValidationError.NameRequired
    draft.carrier.isBlank() -> SimDraftValidationError.CarrierRequired
    draft.simType !in setOf("eSIM", "Physical SIM") -> SimDraftValidationError.InvalidSimType
    draft.renewalCycleDays != null && draft.renewalCycleDays <= 0 ->
        SimDraftValidationError.InvalidCycle
    draft.renewalDayOfMonth != null && draft.renewalDayOfMonth !in 1..31 ->
        SimDraftValidationError.InvalidMonthlyDay
    draft.renewalCycleDays != null && draft.renewalDayOfMonth != null ->
        SimDraftValidationError.ConflictingSchedule
    draft.renewalPrice != null && (draft.renewalPrice < 0 || !draft.renewalPrice.isFinite()) ->
        SimDraftValidationError.InvalidPrice
    !draft.currency.isNullOrBlank() && !isSupportedCurrencyCode(draft.currency) ->
        SimDraftValidationError.InvalidCurrency
    !isSafeWebUrl(draft.renewalUrl) -> SimDraftValidationError.InvalidWebsite
    else -> null
}

class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val pendingRestore = MutableStateFlow<BackupPayload?>(null)
    private val exchangeRates = MutableStateFlow<ExchangeRateUiState>(ExchangeRateUiState.Idle)
    private val messagesChannel = Channel<UiMessage>(Channel.BUFFERED)
    private val _appUpdateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    private var exchangeRateRefreshJob: Job? = null
    private var appUpdateJob: Job? = null
    private var automaticUpdateCheckStarted = false
    private var showUpdateCheckResult = false
    val messages = messagesChannel.receiveAsFlow()
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()

    val uiState: StateFlow<AppUiState> = combine(
        container.simRepository.observeSims(),
        container.simRepository.observeHistory(),
        container.settingsRepository.settings,
        pendingRestore,
        exchangeRates,
    ) { sims, history, settings, restore, rates ->
        AppUiState(
            sims = sims,
            history = history,
            settings = settings,
            isLoading = false,
            pendingRestore = restore,
            exchangeRates = rates,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun refreshExchangeRates() {
        if (exchangeRateRefreshJob?.isActive == true) return
        exchangeRateRefreshJob = viewModelScope.launch {
            val cached = runCatching {
                container.exchangeRateRepository.getCachedSnapshot()
            }.getOrNull()
            exchangeRates.value = when {
                cached == null -> ExchangeRateUiState.Loading
                cached.isFresh() -> ExchangeRateUiState.Ready(cached)
                else -> ExchangeRateUiState.Ready(cached, isRefreshing = true)
            }

            runCatching { container.exchangeRateRepository.refreshIfStale() }
                .onSuccess { exchangeRates.value = ExchangeRateUiState.Ready(it) }
                .onFailure {
                    exchangeRates.value = if (cached == null) {
                        ExchangeRateUiState.Unavailable
                    } else {
                        ExchangeRateUiState.Ready(cached, refreshFailed = true)
                    }
                }
        }
    }

    fun checkForUpdates(currentVersion: String) {
        startUpdateCheck(currentVersion, showResult = true)
    }

    fun checkForUpdatesOnLaunch(currentVersion: String) {
        if (automaticUpdateCheckStarted) return
        automaticUpdateCheckStarted = true
        startUpdateCheck(currentVersion, showResult = false)
    }

    private fun startUpdateCheck(currentVersion: String, showResult: Boolean) {
        if (appUpdateJob?.isActive == true) {
            if (showResult) {
                showUpdateCheckResult = true
                _appUpdateState.value = AppUpdateUiState.Checking
            }
            return
        }
        showUpdateCheckResult = showResult
        _appUpdateState.value = if (showResult) {
            AppUpdateUiState.Checking
        } else {
            AppUpdateUiState.Idle
        }
        appUpdateJob = viewModelScope.launch {
            try {
                val release = container.appUpdateRepository.getLatestRelease()
                _appUpdateState.value = if (
                    compareVersionNames(release.version, currentVersion) > 0
                ) {
                    AppUpdateUiState.Available(release)
                } else if (showUpdateCheckResult) {
                    AppUpdateUiState.UpToDate(release.version)
                } else {
                    AppUpdateUiState.Idle
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _appUpdateState.value = if (showUpdateCheckResult) {
                    AppUpdateUiState.Failed
                } else {
                    AppUpdateUiState.Idle
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        appUpdateJob?.cancel()
        appUpdateJob = null
        showUpdateCheckResult = false
        _appUpdateState.value = AppUpdateUiState.Idle
    }

    fun saveSim(draft: SimDraft, onResult: (Boolean) -> Unit) {
        val error = validateSimDraft(draft)
        if (error != null) {
            messagesChannel.trySend(UiMessage(error.toMessageResource()))
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                val existing = draft.id?.let { id -> uiState.value.sims.find { it.id == id } }
                val now = Instant.now()
                container.simRepository.save(
                    SimEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = draft.name.trim(),
                        carrier = draft.carrier.trim(),
                        countryCode = draft.countryCode.nullIfBlank()?.uppercase(),
                        countryName = draft.countryName.nullIfBlank(),
                        phoneNumber = draft.phoneNumber.nullIfBlank(),
                        simType = draft.simType,
                        planName = draft.planName.nullIfBlank(),
                        lastRenewalDate = draft.lastRenewalDate,
                        nextRenewalDate = draft.nextRenewalDate,
                        renewalCycleDays = draft.renewalCycleDays,
                        renewalDayOfMonth = draft.renewalDayOfMonth,
                        renewalPrice = draft.renewalPrice,
                        currency = draft.currency.nullIfBlank()?.uppercase(),
                        renewalUrl = draft.renewalUrl.nullIfBlank(),
                        notes = draft.notes.nullIfBlank(),
                        archived = existing?.archived ?: false,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    ),
                )
                container.reminderScheduler.schedule()
            }
            if (result.isSuccess) {
                messagesChannel.send(UiMessage(R.string.message_sim_saved))
                onResult(true)
            } else {
                messagesChannel.send(UiMessage(R.string.message_sim_save_failed))
                onResult(false)
            }
        }
    }

    fun recordRenewal(
        simId: String,
        renewalDate: LocalDate,
        nextRenewalDate: LocalDate,
        amount: Double?,
        notes: String?,
    ) = launchAction(R.string.message_renewal_recorded, R.string.message_renewal_failed) {
        container.simRepository.recordRenewal(
            simId,
            renewalDate,
            nextRenewalDate,
            amount,
            notes.nullIfBlank(),
        )
        container.reminderScheduler.schedule()
    }

    fun setArchived(id: String, archived: Boolean) = launchAction(
        if (archived) R.string.message_sim_archived else R.string.message_sim_restored,
        R.string.message_sim_update_failed,
    ) {
        container.simRepository.setArchived(id, archived)
        container.reminderScheduler.schedule()
    }

    fun delete(id: String) = launchAction(
        R.string.message_sim_deleted,
        R.string.message_sim_delete_failed,
    ) {
        container.simRepository.delete(id)
    }

    fun setThemeMode(value: ThemeMode) = launchSettings { container.settingsRepository.setThemeMode(value) }
    fun setDynamicColor(value: Boolean) = launchSettings { container.settingsRepository.setDynamicColor(value) }
    fun setWarningPeriod(value: Int) = launchSettings { container.settingsRepository.setWarningPeriod(value) }
    fun setMaskPhoneNumbers(value: Boolean) = launchSettings {
        container.settingsRepository.setMaskPhoneNumbers(value)
    }
    fun setReminderOffsets(value: Set<Int>) = launchSettings {
        container.settingsRepository.setReminderOffsets(value)
        container.reminderScheduler.schedule()
    }
    fun setDefaultCurrency(value: String) = launchSettings {
        container.settingsRepository.setDefaultCurrency(value)
        messagesChannel.send(UiMessage(R.string.message_currency_saved))
    }

    fun exportBackup(uri: Uri) = launchAction(
        R.string.message_backup_exported,
        R.string.message_backup_export_failed,
    ) {
        container.backupManager.export(uri)
    }

    fun prepareRestore(uri: Uri) {
        viewModelScope.launch {
            runCatching { container.backupManager.readAndValidate(uri) }
                .onSuccess { pendingRestore.value = it }
                .onFailure {
                    messagesChannel.send(UiMessage(R.string.message_restore_failed))
                }
        }
    }

    fun cancelRestore() {
        pendingRestore.value = null
    }

    fun confirmRestore() {
        val payload = pendingRestore.value ?: return
        pendingRestore.value = null
        launchAction(R.string.message_backup_restored, R.string.message_restore_failed) {
            container.backupManager.restore(payload)
            container.reminderScheduler.schedule()
        }
    }

    private fun launchSettings(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { messagesChannel.send(UiMessage(R.string.message_setting_failed)) }
        }
    }

    private fun launchAction(
        @StringRes success: Int,
        @StringRes failure: Int,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { messagesChannel.send(UiMessage(success)) }
                .onFailure { messagesChannel.send(UiMessage(failure)) }
        }
    }

    private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}

@StringRes
private fun SimDraftValidationError.toMessageResource(): Int = when (this) {
    SimDraftValidationError.NameRequired -> R.string.error_name_required
    SimDraftValidationError.CarrierRequired -> R.string.error_carrier_required
    SimDraftValidationError.InvalidSimType -> R.string.error_invalid_sim_type
    SimDraftValidationError.InvalidCycle -> R.string.error_positive_cycle
    SimDraftValidationError.InvalidMonthlyDay -> R.string.error_monthly_renewal_day
    SimDraftValidationError.ConflictingSchedule -> R.string.error_single_renewal_schedule
    SimDraftValidationError.InvalidPrice -> R.string.error_non_negative_price
    SimDraftValidationError.InvalidCurrency -> R.string.error_valid_currency
    SimDraftValidationError.InvalidWebsite -> R.string.error_valid_website
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AppViewModel(container) as T
}

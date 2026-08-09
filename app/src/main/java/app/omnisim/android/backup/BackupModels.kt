package app.omnisim.android.backup

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.data.preferences.ThemeMode
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class BackupDocument(
    val backupVersion: Int,
    val exportedAt: String,
    val sims: List<BackupSim>,
    val renewalHistory: List<BackupRenewal>,
    val settings: BackupSettings,
)

@Serializable
data class BackupSim(
    val id: String,
    val name: String,
    val carrier: String,
    val countryCode: String? = null,
    val countryName: String? = null,
    val phoneNumber: String? = null,
    val simType: String,
    val planName: String? = null,
    val lastRenewalDate: String? = null,
    val nextRenewalDate: String,
    val renewalCycleDays: Int? = null,
    val renewalDayOfMonth: Int? = null,
    val renewalPrice: Double? = null,
    val currency: String? = null,
    val renewalUrl: String? = null,
    val notes: String? = null,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupRenewal(
    val id: String,
    val simId: String,
    val renewalDate: String,
    val previousRenewalDate: String? = null,
    val nextRenewalDate: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val notes: String? = null,
    val createdAt: String,
)

@Serializable
data class BackupSettings(
    val themeMode: String,
    val dynamicColor: Boolean,
    val warningPeriodDays: Int,
    val maskPhoneNumbers: Boolean,
    val reminderOffsets: Set<Int>,
    val defaultCurrency: String,
)

data class BackupPayload(
    val sims: List<SimEntity>,
    val history: List<RenewalHistoryEntity>,
    val settings: AppSettings,
)

class BackupValidationException(message: String) : IllegalArgumentException(message)

object BackupCodec {
    const val CURRENT_VERSION = 2

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(
        sims: List<SimEntity>,
        history: List<RenewalHistoryEntity>,
        settings: AppSettings,
        exportedAt: Instant = Instant.now(),
    ): String = json.encodeToString(
        BackupDocument(
            backupVersion = CURRENT_VERSION,
            exportedAt = exportedAt.toString(),
            sims = sims.map { it.toBackup() },
            renewalHistory = history.map { it.toBackup() },
            settings = settings.toBackup(),
        ),
    )

    fun decode(content: String): BackupPayload {
        val document = try {
            json.decodeFromString<BackupDocument>(content)
        } catch (exception: SerializationException) {
            throw BackupValidationException("Backup is not valid OmniSIM JSON")
        } catch (exception: IllegalArgumentException) {
            throw BackupValidationException("Backup contains invalid values")
        }
        if (document.backupVersion !in 1..CURRENT_VERSION) {
            throw BackupValidationException("Unsupported backup version: ${document.backupVersion}")
        }
        runCatching { Instant.parse(document.exportedAt) }
            .getOrElse { throw BackupValidationException("Invalid export timestamp") }

        val sims = document.sims.map { it.toEntity() }
        if (sims.map(SimEntity::id).toSet().size != sims.size) {
            throw BackupValidationException("Duplicate SIM identifiers")
        }
        val simIds = sims.map(SimEntity::id).toSet()
        val history = document.renewalHistory.map { it.toEntity(simIds) }
        if (history.map(RenewalHistoryEntity::id).toSet().size != history.size) {
            throw BackupValidationException("Duplicate renewal identifiers")
        }
        return BackupPayload(sims, history, document.settings.toSettings())
    }

    private fun SimEntity.toBackup() = BackupSim(
        id = id,
        name = name,
        carrier = carrier,
        countryCode = countryCode,
        countryName = countryName,
        phoneNumber = phoneNumber,
        simType = simType,
        planName = planName,
        lastRenewalDate = lastRenewalDate?.toString(),
        nextRenewalDate = nextRenewalDate.toString(),
        renewalCycleDays = renewalCycleDays,
        renewalDayOfMonth = renewalDayOfMonth,
        renewalPrice = renewalPrice,
        currency = currency,
        renewalUrl = renewalUrl,
        notes = notes,
        archived = archived,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

    private fun BackupSim.toEntity(): SimEntity {
        if (id.isBlank() || name.isBlank() || carrier.isBlank()) {
            throw BackupValidationException("A SIM is missing a required field")
        }
        if (simType !in setOf("eSIM", "Physical SIM")) {
            throw BackupValidationException("Invalid SIM type")
        }
        if (renewalCycleDays != null && renewalCycleDays <= 0) {
            throw BackupValidationException("Invalid renewal cycle")
        }
        if (renewalDayOfMonth != null && renewalDayOfMonth !in 1..31) {
            throw BackupValidationException("Invalid monthly renewal day")
        }
        if (renewalCycleDays != null && renewalDayOfMonth != null) {
            throw BackupValidationException("A SIM has conflicting renewal schedules")
        }
        if (renewalPrice != null && (renewalPrice < 0 || !renewalPrice.isFinite())) {
            throw BackupValidationException("Invalid renewal price")
        }
        if (!isSafeWebUrl(renewalUrl)) {
            throw BackupValidationException("Invalid renewal website")
        }
        return try {
            SimEntity(
                id = id,
                name = name,
                carrier = carrier,
                countryCode = countryCode,
                countryName = countryName,
                phoneNumber = phoneNumber,
                simType = simType,
                planName = planName,
                lastRenewalDate = lastRenewalDate?.let(LocalDate::parse),
                nextRenewalDate = LocalDate.parse(nextRenewalDate),
                renewalCycleDays = renewalCycleDays,
                renewalDayOfMonth = renewalDayOfMonth,
                renewalPrice = renewalPrice,
                currency = currency,
                renewalUrl = renewalUrl,
                notes = notes,
                archived = archived,
                createdAt = Instant.parse(createdAt),
                updatedAt = Instant.parse(updatedAt),
            )
        } catch (exception: RuntimeException) {
            throw BackupValidationException("A SIM contains an invalid date")
        }
    }

    private fun RenewalHistoryEntity.toBackup() = BackupRenewal(
        id = id,
        simId = simId,
        renewalDate = renewalDate.toString(),
        previousRenewalDate = previousRenewalDate?.toString(),
        nextRenewalDate = nextRenewalDate?.toString(),
        amount = amount,
        currency = currency,
        notes = notes,
        createdAt = createdAt.toString(),
    )

    private fun BackupRenewal.toEntity(simIds: Set<String>): RenewalHistoryEntity {
        if (id.isBlank() || simId !in simIds) {
            throw BackupValidationException("Renewal history references a missing SIM")
        }
        if (amount != null && (amount < 0 || !amount.isFinite())) {
            throw BackupValidationException("Invalid renewal amount")
        }
        return try {
            RenewalHistoryEntity(
                id = id,
                simId = simId,
                renewalDate = LocalDate.parse(renewalDate),
                previousRenewalDate = previousRenewalDate?.let(LocalDate::parse),
                nextRenewalDate = nextRenewalDate?.let(LocalDate::parse),
                amount = amount,
                currency = currency,
                notes = notes,
                createdAt = Instant.parse(createdAt),
            )
        } catch (exception: RuntimeException) {
            throw BackupValidationException("Renewal history contains an invalid date")
        }
    }

    private fun AppSettings.toBackup() = BackupSettings(
        themeMode = themeMode.name,
        dynamicColor = dynamicColor,
        warningPeriodDays = warningPeriodDays,
        maskPhoneNumbers = maskPhoneNumbers,
        reminderOffsets = reminderOffsets,
        defaultCurrency = defaultCurrency,
    )

    private fun BackupSettings.toSettings(): AppSettings {
        if (warningPeriodDays < 0 || defaultCurrency.isBlank()) {
            throw BackupValidationException("Invalid settings")
        }
        val mode = runCatching { ThemeMode.valueOf(themeMode) }.getOrElse {
            throw BackupValidationException("Invalid theme mode")
        }
        return AppSettings(
            themeMode = mode,
            dynamicColor = dynamicColor,
            warningPeriodDays = warningPeriodDays,
            maskPhoneNumbers = maskPhoneNumbers,
            reminderOffsets = reminderOffsets,
            defaultCurrency = defaultCurrency.uppercase(),
        )
    }
}

fun isSafeWebUrl(value: String?): Boolean {
    if (value.isNullOrBlank()) return true
    return runCatching {
        val uri = URI(value)
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

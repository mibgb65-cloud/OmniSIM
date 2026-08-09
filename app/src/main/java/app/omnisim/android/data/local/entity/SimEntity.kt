package app.omnisim.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "sims",
    indices = [Index("nextRenewalDate"), Index("archived")],
)
data class SimEntity(
    @PrimaryKey val id: String,
    val name: String,
    val carrier: String,
    val countryCode: String? = null,
    val countryName: String? = null,
    val phoneNumber: String? = null,
    val simType: String = "eSIM",
    val planName: String? = null,
    val lastRenewalDate: LocalDate? = null,
    val nextRenewalDate: LocalDate,
    val renewalCycleDays: Int? = null,
    val renewalDayOfMonth: Int? = null,
    val renewalPrice: Double? = null,
    val currency: String? = null,
    val renewalUrl: String? = null,
    val notes: String? = null,
    val remindersEnabled: Boolean = true,
    val reminderOffsets: Set<Int>? = null,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

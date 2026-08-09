package app.omnisim.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "renewal_history",
    foreignKeys = [
        ForeignKey(
            entity = SimEntity::class,
            parentColumns = ["id"],
            childColumns = ["simId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("simId"), Index(value = ["simId", "renewalDate"])],
)
data class RenewalHistoryEntity(
    @PrimaryKey val id: String,
    val simId: String,
    val renewalDate: LocalDate,
    val previousRenewalDate: LocalDate?,
    val previousNextRenewalDate: LocalDate? = null,
    val previousRenewalPrice: Double? = null,
    val nextRenewalDate: LocalDate?,
    val amount: Double?,
    val currency: String?,
    val notes: String?,
    val createdAt: Instant,
)

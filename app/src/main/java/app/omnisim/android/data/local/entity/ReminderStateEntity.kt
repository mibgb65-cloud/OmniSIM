package app.omnisim.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "reminder_state",
    primaryKeys = ["simId", "renewalDate", "reminderOffset"],
    foreignKeys = [
        ForeignKey(
            entity = SimEntity::class,
            parentColumns = ["id"],
            childColumns = ["simId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("simId")],
)
data class ReminderStateEntity(
    val simId: String,
    val renewalDate: LocalDate,
    val reminderOffset: Int,
    val sentAt: Instant,
)


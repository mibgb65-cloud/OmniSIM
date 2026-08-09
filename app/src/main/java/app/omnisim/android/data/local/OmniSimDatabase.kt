package app.omnisim.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import app.omnisim.android.data.local.converter.DateConverters
import app.omnisim.android.data.local.dao.OmniSimDao
import app.omnisim.android.data.local.entity.ReminderStateEntity
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity

@Database(
    entities = [SimEntity::class, RenewalHistoryEntity::class, ReminderStateEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(DateConverters::class)
abstract class OmniSimDatabase : RoomDatabase() {
    abstract fun dao(): OmniSimDao

    companion object {
        fun create(context: Context): OmniSimDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OmniSimDatabase::class.java,
                "omnisim.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

        val MIGRATION_1_2 = Migration(1, 2) { database ->
            database.execSQL("ALTER TABLE sims ADD COLUMN renewalDayOfMonth INTEGER")
        }

        val MIGRATION_2_3 = Migration(2, 3) { database ->
            database.execSQL(
                "ALTER TABLE sims ADD COLUMN remindersEnabled INTEGER NOT NULL DEFAULT 1",
            )
            database.execSQL(
                "ALTER TABLE sims ADD COLUMN reminderOffsets TEXT",
            )
            database.execSQL(
                "ALTER TABLE renewal_history ADD COLUMN previousNextRenewalDate TEXT",
            )
            database.execSQL(
                "ALTER TABLE renewal_history ADD COLUMN previousRenewalPrice REAL",
            )
        }
    }
}

package app.omnisim.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.omnisim.android.data.local.converter.DateConverters
import app.omnisim.android.data.local.dao.OmniSimDao
import app.omnisim.android.data.local.entity.ReminderStateEntity
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity

@Database(
    entities = [SimEntity::class, RenewalHistoryEntity::class, ReminderStateEntity::class],
    version = 1,
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
            ).build()
    }
}


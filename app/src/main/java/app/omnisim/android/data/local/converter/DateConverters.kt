package app.omnisim.android.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class DateConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun reminderOffsetsToString(value: Set<Int>?): String? =
        value?.sortedDescending()?.joinToString(",")

    @TypeConverter
    fun stringToReminderOffsets(value: String?): Set<Int>? = value?.let { stored ->
        if (stored.isBlank()) emptySet() else stored.split(',').mapNotNull(String::toIntOrNull).toSet()
    }
}

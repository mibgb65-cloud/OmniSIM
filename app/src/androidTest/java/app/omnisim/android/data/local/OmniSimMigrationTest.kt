package app.omnisim.android.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OmniSimMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OmniSimDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationFromOneToTwoPreservesSimsAndAddsMonthlyDay() {
        helper.createDatabase(DatabaseName, 1).apply {
            execSQL(
                """
                INSERT INTO sims (
                    id, name, carrier, simType, nextRenewalDate,
                    archived, createdAt, updatedAt
                ) VALUES (
                    'sim-1', 'Primary', 'Carrier', 'eSIM', '2026-09-01',
                    0, 0, 0
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            DatabaseName,
            2,
            true,
            OmniSimDatabase.MIGRATION_1_2,
        ).use { database ->
            database.query(
                "SELECT name, nextRenewalDate, renewalDayOfMonth FROM sims WHERE id = 'sim-1'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Primary", cursor.getString(0))
                assertEquals("2026-09-01", cursor.getString(1))
                assertTrue(cursor.isNull(2))
            }
        }
    }

    @Test
    fun migrationFromTwoToThreeAddsReminderSettingsAndRenewalUndoSnapshot() {
        helper.createDatabase(DatabaseName, 2).apply {
            close()
        }

        helper.runMigrationsAndValidate(
            DatabaseName,
            3,
            true,
            OmniSimDatabase.MIGRATION_2_3,
        ).use { database ->
            database.query("PRAGMA table_info(sims)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val columns = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
                assertTrue("remindersEnabled" in columns)
                assertTrue("reminderOffsets" in columns)
            }
            database.query("PRAGMA table_info(renewal_history)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val columns = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
                assertTrue("previousNextRenewalDate" in columns)
                assertTrue("previousRenewalPrice" in columns)
            }
        }
    }

    private companion object {
        const val DatabaseName = "migration-test"
    }
}

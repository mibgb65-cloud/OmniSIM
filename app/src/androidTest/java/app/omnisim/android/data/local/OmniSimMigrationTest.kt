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

    private companion object {
        const val DatabaseName = "migration-test"
    }
}

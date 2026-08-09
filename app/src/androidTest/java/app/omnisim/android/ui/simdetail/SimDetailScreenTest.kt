package app.omnisim.android.ui.simdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.ui.theme.OmniSimTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun latestSnapshotBackedRenewalOffersEditAndUndo() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val editLabel = context.getString(R.string.edit_renewal_record)
        val undoLabel = context.getString(R.string.undo_this_renewal)
        val sim = testSim()
        val history = RenewalHistoryEntity(
            id = "renewal-1",
            simId = sim.id,
            renewalDate = LocalDate.of(2026, 9, 1),
            previousRenewalDate = LocalDate.of(2026, 8, 1),
            previousNextRenewalDate = LocalDate.of(2026, 9, 1),
            previousRenewalPrice = 10.0,
            nextRenewalDate = LocalDate.of(2026, 10, 1),
            amount = 12.0,
            currency = "USD",
            notes = null,
            createdAt = Instant.EPOCH,
        )
        composeRule.setContent {
            OmniSimTheme(AppSettings()) {
                SimDetailScreen(
                    sim = sim,
                    history = listOf(history),
                    settings = AppSettings(),
                    onRenew = { _, _, _, _ -> },
                    onUpdateRenewal = { _, _, _, _, _ -> },
                    onUndoRenewal = {},
                    onReminderSettings = { _, _ -> },
                    onOpenWebsite = {},
                    onEdit = {},
                    onArchive = {},
                    onDelete = {},
                )
            }
        }
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNode(hasText(editLabel) and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.onNode(hasText(undoLabel) and hasClickAction()).assertIsDisplayed()
    }

    private fun testSim() = SimEntity(
        id = "sim-1",
        name = "Primary",
        carrier = "Carrier",
        lastRenewalDate = LocalDate.of(2026, 9, 1),
        nextRenewalDate = LocalDate.of(2026, 10, 1),
        renewalPrice = 12.0,
        currency = "USD",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

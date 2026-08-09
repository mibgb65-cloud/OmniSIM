package app.omnisim.android.backup

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity

object RenewalHistoryCsv {
    fun encode(
        sims: List<SimEntity>,
        history: List<RenewalHistoryEntity>,
    ): String {
        val simsById = sims.associateBy(SimEntity::id)
        return buildString {
            append('\uFEFF')
            appendRow(
                "SIM",
                "Carrier",
                "Renewal date",
                "Next renewal date",
                "Amount",
                "Currency",
                "Notes",
            )
            history.sortedWith(
                compareByDescending<RenewalHistoryEntity> { it.renewalDate }
                    .thenByDescending { it.createdAt },
            ).forEach { renewal ->
                val sim = simsById[renewal.simId] ?: return@forEach
                appendRow(
                    sim.name,
                    sim.carrier,
                    renewal.renewalDate.toString(),
                    renewal.nextRenewalDate?.toString().orEmpty(),
                    renewal.amount?.toString().orEmpty(),
                    renewal.currency.orEmpty(),
                    renewal.notes.orEmpty(),
                )
            }
        }
    }

    private fun StringBuilder.appendRow(vararg values: String) {
        append(values.joinToString(",") { value -> "\"${value.replace("\"", "\"\"")}\"" })
        append("\r\n")
    }
}

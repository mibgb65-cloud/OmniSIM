package app.omnisim.android.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.exchange.ExchangeRateSnapshot
import app.omnisim.android.ui.SimDraft
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.OmniSheetHeader
import app.omnisim.android.ui.editsim.AddEditSimScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddSimSheet(
    defaultCurrency: String,
    exchangeRateSnapshot: ExchangeRateSnapshot?,
    onSave: (SimDraft, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    ) {
        OmniDialogSystemBars()
        Column(Modifier.fillMaxSize()) {
            OmniSheetHeader(title = stringResource(R.string.title_add_sim), onClose = onDismiss)
            AddEditSimScreen(
                existing = null,
                defaultCurrency = defaultCurrency,
                exchangeRateSnapshot = exchangeRateSnapshot,
                onSave = onSave,
                onDone = onSaved,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

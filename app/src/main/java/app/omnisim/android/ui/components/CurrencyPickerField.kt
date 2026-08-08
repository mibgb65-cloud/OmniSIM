package app.omnisim.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.domain.util.CurrencyOption
import app.omnisim.android.domain.util.currencyOptions
import app.omnisim.android.domain.util.filterCurrencyOptions
import java.util.Locale

@Composable
fun CurrencyPickerField(
    selectedCode: String,
    onSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val languageTag = LocalLocale.current.toLanguageTag()
    val locale = remember(languageTag) { Locale.forLanguageTag(languageTag) }
    val currencies = remember(locale) { currencyOptions(locale) }
    val selectedCurrency = currencies.firstOrNull { it.code.equals(selectedCode, ignoreCase = true) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = { showPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { role = Role.Button },
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = selectedCurrency?.code ?: selectedCode,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    selectedCurrency?.let { currency ->
                        Text(
                            text = currencyFieldDetail(currency),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.change_currency),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (showPicker) {
        CurrencyPickerSheet(
            currencies = currencies,
            selectedCode = selectedCode,
            onSelected = { currency ->
                onSelected(currency.code)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private fun currencyFieldDetail(currency: CurrencyOption): String =
    if (currency.symbol.equals(currency.code, ignoreCase = true)) {
        currency.displayName
    } else {
        "${currency.symbol} · ${currency.displayName}"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    currencies: List<CurrencyOption>,
    selectedCode: String,
    onSelected: (CurrencyOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredCurrencies = remember(currencies, query) {
        filterCurrencyOptions(currencies, query)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    ) {
        OmniDialogSystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
        ) {
            OmniSheetHeader(
                title = stringResource(R.string.select_currency),
                onClose = onDismiss,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.search_currency_or_code)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.action_clear_search),
                            )
                        }
                    }
                },
                colors = omniTextFieldColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))

            if (filteredCurrencies.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_currency_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filteredCurrencies, key = CurrencyOption::code) { currency ->
                        val selected = currency.code.equals(selectedCode, ignoreCase = true)
                        Surface(
                            onClick = { onSelected(currency) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = currency.code,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = currencyFieldDetail(currency),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Spacer(Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

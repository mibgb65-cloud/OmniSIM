package app.omnisim.android.ui.editsim

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.domain.util.CallingCodeCountry
import app.omnisim.android.ui.components.OmniSheetHeader
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.omniTextFieldColors
import java.util.Locale

@Composable
internal fun CountryPhoneField(
    countries: List<CallingCodeCountry>,
    selectedCountry: CallingCodeCountry,
    nationalNumber: String,
    locale: Locale,
    onCountrySelected: (CallingCodeCountry) -> Unit,
    onNationalNumberChange: (String) -> Unit,
) {
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.phone_number_optional),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                onClick = { showCountryPicker = true },
                modifier = Modifier
                    .weight(0.9f)
                    .height(64.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(selectedCountry.flag, style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = selectedCountry.countryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = selectedCountry.callingCode,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.change_country_code),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            OutlinedTextField(
                value = nationalNumber,
                onValueChange = onNationalNumberChange,
                placeholder = { Text(stringResource(R.string.local_phone_number)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = omniTextFieldColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1.25f)
                    .height(64.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.selected_country_phone_hint,
                selectedCountry.countryName,
                selectedCountry.callingCode,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showCountryPicker) {
        CountryPickerSheet(
            countries = countries,
            selectedRegionCode = selectedCountry.regionCode,
            locale = locale,
            onSelected = {
                onCountrySelected(it)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    countries: List<CallingCodeCountry>,
    selectedRegionCode: String,
    locale: Locale,
    onSelected: (CallingCodeCountry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredCountries = remember(countries, query, locale) {
        val normalized = query.trim().lowercase(locale)
        if (normalized.isBlank()) {
            countries
        } else {
            val callingCodeQuery = normalized.removePrefix("+")
            val searchesCallingCode = callingCodeQuery.isNotEmpty() &&
                callingCodeQuery.all(Char::isDigit)
            countries.filter { country ->
                if (searchesCallingCode) {
                    country.callingCode.removePrefix("+").startsWith(callingCodeQuery)
                } else {
                    country.countryName.lowercase(locale).contains(normalized) ||
                        country.englishName.lowercase(Locale.ENGLISH).contains(
                            normalized.lowercase(Locale.ENGLISH),
                        ) ||
                        country.regionCode.lowercase(Locale.ROOT).contains(
                            normalized.lowercase(Locale.ROOT),
                        )
                }
            }
        }
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
                title = stringResource(R.string.select_country_or_region),
                onClose = onDismiss,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.search_country_or_code)) },
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

            if (filteredCountries.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_country_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filteredCountries, key = CallingCodeCountry::regionCode) { country ->
                        Surface(
                            onClick = { onSelected(country) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(country.flag, style = MaterialTheme.typography.headlineSmall)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = country.countryName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = country.regionCode,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = country.callingCode,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (country.regionCode == selectedRegionCode) {
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

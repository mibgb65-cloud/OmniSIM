package app.omnisim.android.ui.home

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.domain.model.RenewalStatus
import app.omnisim.android.domain.util.calculateRenewalStatus
import app.omnisim.android.domain.util.countryFlag
import app.omnisim.android.domain.util.daysUntilRenewal
import app.omnisim.android.domain.util.localizedCountryName
import app.omnisim.android.domain.util.maskPhoneNumber
import app.omnisim.android.domain.util.resolvePhoneNumberRegionCode
import app.omnisim.android.ui.components.RenewalSheet
import app.omnisim.android.ui.components.OmniPrimaryButton
import app.omnisim.android.ui.components.OmniSectionHeader
import app.omnisim.android.ui.components.OmniSheetHeader
import app.omnisim.android.ui.components.OmniPageHeader
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.StatusChip
import app.omnisim.android.ui.components.daysRemainingLabel
import app.omnisim.android.ui.components.displayDate
import app.omnisim.android.ui.components.label
import app.omnisim.android.ui.components.rememberCurrentDate
import app.omnisim.android.ui.simdetail.SimDetailScreen
import app.omnisim.android.ui.splash.rememberSystemAnimationsEnabled
import java.time.LocalDate
import java.util.Locale

@Composable
internal fun RenewalHero(
    sim: SimEntity,
    settings: AppSettings,
    today: LocalDate,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onRenew: () -> Unit,
    compact: Boolean = false,
) {
    val status = calculateRenewalStatus(
        today,
        sim.nextRenewalDate,
        settings.warningPeriodDays,
        false,
    )
    val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
    val phone = if (settings.maskPhoneNumbers) maskPhoneNumber(sim.phoneNumber) else sim.phoneNumber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                top = if (compact) 4.dp else 14.dp,
                end = 20.dp,
                bottom = if (compact) 8.dp else 28.dp,
            ),
    ) {
        Surface(
            onClick = onSelect,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 300.dp)
                .height(if (compact) 48.dp else 56.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SimCountryAvatar(sim, Modifier.size(if (compact) 32.dp else 36.dp))
                Column(Modifier.widthIn(max = 190.dp)) {
                    Text(
                        "${sim.name} · ${status.label()}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        phone ?: sim.carrier,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RenewalSummary(
                    remaining = remaining,
                    renewalDate = sim.nextRenewalDate,
                    compact = true,
                    modifier = Modifier.weight(1f),
                )
                HeroAction(
                    icon = Icons.Default.Check,
                    label = stringResource(R.string.mark_as_renewed),
                    onClick = onRenew,
                    compact = true,
                )
                HeroAction(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = stringResource(R.string.action_view_details),
                    onClick = onOpen,
                    compact = true,
                )
            }
        } else {
            RenewalSummary(
                remaining = remaining,
                renewalDate = sim.nextRenewalDate,
                compact = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 46.dp, bottom = 38.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HeroAction(
                    icon = Icons.Default.Check,
                    label = stringResource(R.string.mark_as_renewed),
                    onClick = onRenew,
                )
                HeroAction(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = stringResource(R.string.action_view_details),
                    onClick = onOpen,
                )
            }
        }
    }
}

@Composable
private fun RenewalSummary(
    remaining: Long,
    renewalDate: LocalDate,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.next_renewal),
            style = if (compact) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
        )
        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
        Text(
            daysRemainingLabel(remaining),
            style = if (compact) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.displaySmall
            },
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
        Text(
            renewalDate.displayDate(),
            style = if (compact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
    }
}
@Composable
internal fun SimCountryAvatar(sim: SimEntity, modifier: Modifier = Modifier) {
    val regionCode = remember(sim.phoneNumber, sim.countryCode) {
        resolvePhoneNumberRegionCode(sim.phoneNumber, sim.countryCode)
    }
    val flag = remember(regionCode) { countryFlag(regionCode) }
    val languageTag = LocalLocale.current.toLanguageTag()
    val countryName = remember(regionCode, languageTag) {
        regionCode?.let {
            localizedCountryName(it, Locale.forLanguageTag(languageTag))
        }
    }
    val description = countryName ?: sim.countryName ?: stringResource(R.string.country)

    Surface(
        modifier = modifier
            .size(44.dp)
            .clearAndSetSemantics { contentDescription = description },
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (flag != null) {
                Text(flag, style = MaterialTheme.typography.titleLarge)
            } else {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    Column(
        modifier = Modifier.width(if (compact) 112.dp else 132.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(if (compact) 48.dp else 66.dp)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 22.dp else 27.dp),
                )
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Text(
            label,
            style = if (compact) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

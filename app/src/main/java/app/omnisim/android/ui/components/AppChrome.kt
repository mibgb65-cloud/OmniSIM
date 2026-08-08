package app.omnisim.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.ui.theme.OmniAccentYellow
import app.omnisim.android.ui.theme.OmniScreenPadding

enum class OmniPageTitleStyle {
    Centered,
    LargeStart,
    CompactLargeStart,
}

@Composable
fun OmniPageSurface(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigate: (() -> Unit)? = null,
    titleStyle: OmniPageTitleStyle = OmniPageTitleStyle.Centered,
    action: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(Modifier.fillMaxSize()) {
            OmniPageHeader(
                title = title,
                navigationIcon = navigationIcon,
                onNavigate = onNavigate,
                titleStyle = titleStyle,
                action = action,
                frosted = true,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                content = content,
            )
        }
    }
}

@Composable
fun OmniPageHeader(
    title: String,
    navigationIcon: ImageVector? = null,
    onNavigate: (() -> Unit)? = null,
    titleStyle: OmniPageTitleStyle = OmniPageTitleStyle.Centered,
    action: (@Composable BoxScope.() -> Unit)? = null,
    frosted: Boolean = false,
) {
    val largeStart = titleStyle != OmniPageTitleStyle.Centered && navigationIcon == null
    val headerHeight = when (titleStyle) {
        OmniPageTitleStyle.Centered -> 78.dp
        OmniPageTitleStyle.LargeStart -> 112.dp
        OmniPageTitleStyle.CompactLargeStart -> 92.dp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(
                if (frosted) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
            )
            .padding(horizontal = OmniScreenPadding),
        contentAlignment = if (largeStart) Alignment.CenterStart else Alignment.Center,
    ) {
        if (navigationIcon != null && onNavigate != null) {
            OmniCircleIconButton(
                onClick = onNavigate,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    navigationIcon,
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            title,
            style = if (largeStart) {
                MaterialTheme.typography.headlineLarge
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.Bold,
        )
        action?.let {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
                content = it,
            )
        }
    }
}

@Composable
fun OmniSheetHeader(title: String, onClose: () -> Unit) {
    OmniPageHeader(
        title = title,
        navigationIcon = Icons.Default.Close,
        onNavigate = onClose,
    )
}

@Composable
fun OmniCircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(50.dp),
        shape = CircleShape,
        color = if (emphasized) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shadowElevation = if (emphasized) 0.dp else 2.dp,
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun OmniPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = OmniAccentYellow,
            contentColor = androidx.compose.ui.graphics.Color(0xFF171717),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OmniSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun OmniSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

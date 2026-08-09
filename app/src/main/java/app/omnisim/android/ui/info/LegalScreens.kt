package app.omnisim.android.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.omnisim.android.R
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing

@Composable
fun LegalConsentDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OmniDialogSystemBars()
                Text(stringResource(R.string.legal_consent_title))
                Text(
                    text = stringResource(R.string.legal_consent_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LegalDocumentSection(
                    title = stringResource(R.string.user_agreement),
                    body = stringResource(R.string.user_agreement_body),
                )
                HorizontalDivider()
                LegalDocumentSection(
                    title = stringResource(R.string.privacy_policy),
                    body = stringResource(R.string.privacy_policy_body),
                )
            }
        },
        confirmButton = {
            Button(onClick = onAgree) {
                Text(stringResource(R.string.action_agree_and_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.action_disagree_and_exit))
            }
        },
    )
}

@Composable
fun LegalDocumentsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OmniScreenPadding,
            top = 8.dp,
            end = OmniScreenPadding,
            bottom = OmniSectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LegalDocumentCard(
                title = stringResource(R.string.user_agreement),
                body = stringResource(R.string.user_agreement_body),
            )
        }
        item {
            LegalDocumentCard(
                title = stringResource(R.string.privacy_policy),
                body = stringResource(R.string.privacy_policy_body),
            )
        }
    }
}

@Composable
private fun LegalDocumentCard(
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        LegalDocumentSection(
            title = title,
            body = body,
            modifier = Modifier.padding(OmniCardPadding),
        )
    }
}

@Composable
private fun LegalDocumentSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

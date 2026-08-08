package app.omnisim.android.ui.splash

import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.omnisim.android.R

internal const val LAUNCH_REVEAL_DURATION_MILLIS = 900L

@Composable
internal fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }
    }
}

@Composable
internal fun OmniLaunchScreen(
    animationsEnabled: Boolean,
    startAnimation: Boolean,
    modifier: Modifier = Modifier,
) {
    var revealed by remember(startAnimation, animationsEnabled) {
        mutableStateOf(!animationsEnabled && startAnimation)
    }
    LaunchedEffect(startAnimation, animationsEnabled) {
        if (startAnimation) {
            revealed = true
        }
    }

    val markAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "launchMarkAlpha",
    )
    val markScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.82f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "launchMarkScale",
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(380, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "launchTitleAlpha",
    )
    val titleOffset by animateDpAsState(
        targetValue = if (revealed) 0.dp else 12.dp,
        animationSpec = tween(430, delayMillis = 130, easing = FastOutSlowInEasing),
        label = "launchTitleOffset",
    )
    val appName = stringResource(R.string.app_name)
    val subtitle = stringResource(R.string.app_subtitle)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (isDark) {
        listOf(Color(0xFF10242C), Color(0xFF101412))
    } else {
        listOf(Color(0xFFDDF2FB), Color(0xFFF3F4F6), Color(0xFFFFFFFF))
    }

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(background))
            .clearAndSetSemantics {
                contentDescription = "$appName. $subtitle"
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            OmniBrandMark(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        alpha = markAlpha
                        scaleX = markScale
                        scaleY = markScale
                    },
            )
            Spacer(Modifier.height(28.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = titleOffset.toPx()
                },
            ) {
                Text(
                    text = appName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7).sp,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun OmniBrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(18.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFFFFE24A)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val scaleX = size.width / 108f
            val scaleY = size.height / 108f
            val mark = Path().apply {
                fillType = PathFillType.EvenOdd
                addOval(
                    Rect(
                        left = 29f * scaleX,
                        top = 26f * scaleY,
                        right = 79f * scaleX,
                        bottom = 82f * scaleY,
                    ),
                )
                addOval(
                    Rect(
                        left = 38f * scaleX,
                        top = 35f * scaleY,
                        right = 70f * scaleX,
                        bottom = 73f * scaleY,
                    ),
                )
            }
            drawPath(
                path = mark,
                color = Color(0xFF101719),
            )
        }
    }
}

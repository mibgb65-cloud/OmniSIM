package app.omnisim.android.ui.theme

import android.os.Build
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.data.preferences.ThemeMode

val OmniAccentYellow = Color(0xFFFFE24A)
val OmniScreenPadding = 20.dp
val OmniCardPadding = 20.dp
val OmniSectionSpacing = 32.dp
val OmniRowSpacing = 12.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF151718),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBEAFA),
    onPrimaryContainer = Color(0xFF101719),
    secondary = Color(0xFF2C729A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6F7FC),
    onSecondaryContainer = Color(0xFF173E50),
    tertiary = Color(0xFF6B5F00),
    onTertiary = Color.White,
    tertiaryContainer = OmniAccentYellow,
    onTertiaryContainer = Color(0xFF171717),
    error = Color(0xFFC94440),
    errorContainer = Color(0xFFFFEDEC),
    onErrorContainer = Color(0xFF842421),
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF0B0B0C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B0B0C),
    surfaceVariant = Color(0xFFF0F1F3),
    onSurfaceVariant = Color(0xFF5F6670),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE1E5E2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEEFF1),
    surfaceContainerHigh = Color(0xFFE8E9EB),
    surfaceContainerHighest = Color(0xFFDFE1E4),
    outline = Color(0xFFD9DCE1),
    outlineVariant = Color(0xFFE7E9ED),
    surfaceTint = Color.Transparent,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF4F7F5),
    onPrimary = Color(0xFF151718),
    primaryContainer = Color(0xFF244C61),
    onPrimaryContainer = Color(0xFFE8F7FF),
    secondary = Color(0xFF98CDE6),
    onSecondary = Color(0xFF003548),
    secondaryContainer = Color(0xFF234B5D),
    onSecondaryContainer = Color(0xFFCDEEFF),
    tertiary = OmniAccentYellow,
    onTertiary = Color(0xFF3C3500),
    tertiaryContainer = OmniAccentYellow,
    onTertiaryContainer = Color(0xFF171717),
    error = Color(0xFFFFB4AF),
    errorContainer = Color(0xFF782C29),
    onErrorContainer = Color(0xFFFFDAD7),
    background = Color(0xFF101412),
    onBackground = Color(0xFFF5F6F8),
    surface = Color(0xFF1A1F1C),
    onSurface = Color(0xFFF5F6F8),
    surfaceVariant = Color(0xFF222824),
    onSurfaceVariant = Color(0xFFB7BDC5),
    surfaceBright = Color(0xFF353C37),
    surfaceDim = Color(0xFF101412),
    surfaceContainerLowest = Color(0xFF0C100E),
    surfaceContainerLow = Color(0xFF191E1B),
    surfaceContainer = Color(0xFF202622),
    surfaceContainerHigh = Color(0xFF282F2A),
    surfaceContainerHighest = Color(0xFF323A34),
    outline = Color(0xFF485049),
    outlineVariant = Color(0xFF303730),
    surfaceTint = Color.Transparent,
)

private val OmniSimTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

private val OmniSimShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun OmniSimTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val activity = context.findActivity()
    val colors = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    SideEffect {
        activity?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                window.navigationBarDividerColor = Color.Transparent.toArgb()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = OmniSimTypography,
        shapes = OmniSimShapes,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

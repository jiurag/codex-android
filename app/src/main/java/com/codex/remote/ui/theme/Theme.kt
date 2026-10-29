package com.codex.remote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.codex.remote.R

val CodexGreen = Color(0xFF167653)
val CodexRed = Color(0xFFB3261E)
val CodexAmber = Color(0xFF8A5A00)
val DiffGreen = Color(0xFFE4F3EA)
val DiffRed = Color(0xFFF9E7E6)
val Ink = Color(0xFF202127)
val CodexMidnight = Color(0xFF2C2E3A)
val CodexSky = Color(0xFFCFF7FF)
val CodexMist = Color(0xFFE9F8FA)
val CodexPaper = Color(0xFFF7F7F3)
val CodexPanel = Color(0xFFEDEDE7)
val CodexStroke = Color(0xFFD4D4CD)
val CodexMuted = Color(0xFF5A5C62)
val CodexNight = Color(0xFF15161A)
val CodexNightSurface = Color(0xFF1E2026)
val CodexNightRaised = Color(0xFF272A31)
val CodexNightMuted = Color(0xFFC5C8CF)
val CodexNightStroke = Color(0xFF3A3E47)

private val LightColors = lightColorScheme(
    primary = CodexMidnight,
    onPrimary = Color(0xFFF8FBFF),
    primaryContainer = CodexMist,
    onPrimaryContainer = CodexMidnight,
    secondary = CodexGreen,
    onSecondary = Color(0xFFF7FFFC),
    secondaryContainer = Color(0xFFDDF3EA),
    onSecondaryContainer = Color(0xFF083A29),
    tertiary = CodexAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6B4),
    onTertiaryContainer = Color(0xFF2F2100),
    error = CodexRed,
    errorContainer = CodexRed,
    onErrorContainer = Color.White,
    background = CodexPaper,
    onBackground = Ink,
    surface = Color(0xFFFDFDF9),
    onSurface = Ink,
    surfaceVariant = CodexPanel,
    onSurfaceVariant = CodexMuted,
    outline = CodexStroke,
    outlineVariant = Color(0xFFE3E3DD),
    surfaceDim = Color(0xFFE5E5E0),
    surfaceBright = Color(0xFFFFFEF9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F8F3),
    surfaceContainer = Color(0xFFF2F2ED),
    surfaceContainerHigh = Color(0xFFECECE7),
    surfaceContainerHighest = Color(0xFFE6E6E1),
    inverseSurface = CodexMidnight,
    inverseOnSurface = Color(0xFFF3F4F6),
    inversePrimary = CodexSky,
)

private val DarkColors = darkColorScheme(
    primary = CodexSky,
    onPrimary = CodexMidnight,
    primaryContainer = CodexNightRaised,
    onPrimaryContainer = Color(0xFFF7FBFE),
    secondary = Color(0xFF73D1B3),
    onSecondary = Color(0xFF082A20),
    secondaryContainer = Color(0xFF1F463A),
    onSecondaryContainer = Color(0xFFD7F8EC),
    tertiary = Color(0xFFFFCD75),
    onTertiary = Color(0xFF2F2100),
    tertiaryContainer = Color(0xFF6A4A00),
    onTertiaryContainer = Color(0xFFFFE6B4),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5D1F1B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = CodexNight,
    onBackground = Color(0xFFF3F4F6),
    surface = CodexNightSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = CodexNightRaised,
    onSurfaceVariant = CodexNightMuted,
    outline = CodexNightStroke,
    outlineVariant = Color(0xFF30343C),
    surfaceDim = Color(0xFF111216),
    surfaceBright = Color(0xFF252830),
    surfaceContainerLowest = Color(0xFF101115),
    surfaceContainerLow = Color(0xFF181A1F),
    surfaceContainer = Color(0xFF1C1E24),
    surfaceContainerHigh = Color(0xFF23262D),
    surfaceContainerHighest = Color(0xFF2A2D35),
    inverseSurface = Color(0xFFF3F4F6),
    inverseOnSurface = CodexMidnight,
    inversePrimary = CodexMidnight,
)

val MiSans = FontFamily(
    Font(R.font.misans_regular, FontWeight.Normal),
    Font(R.font.misans_medium, FontWeight.Medium),
    Font(R.font.misans_semibold, FontWeight.SemiBold),
)

private val CodexTypography = Typography(
    displaySmall = TextStyle(fontFamily = MiSans, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = MiSans, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontFamily = MiSans, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = MiSans, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontFamily = MiSans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontFamily = MiSans, fontSize = 15.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = MiSans, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontFamily = MiSans, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = MiSans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontFamily = MiSans, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = MiSans, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
)

private val CodexShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

val MonoText = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp,
)

@Composable
fun CodexRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(
        LocalContentColor provides colors.onBackground,
        LocalTextStyle provides CodexTypography.bodyLarge,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = CodexTypography,
            shapes = CodexShapes,
            content = content,
        )
    }
}

@Composable
fun CodexDarkSurfaceTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides DarkColors.onBackground,
        LocalTextStyle provides CodexTypography.bodyLarge,
    ) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = CodexTypography,
            shapes = CodexShapes,
            content = content,
        )
    }
}

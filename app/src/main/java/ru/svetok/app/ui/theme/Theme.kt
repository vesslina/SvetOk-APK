package ru.svetok.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary                = CrimsonRed,
    onPrimary              = PureWhite,
    primaryContainer       = CrimsonContainer,
    onPrimaryContainer     = CrimsonDark,
    secondary              = MediumGray,
    onSecondary            = PureWhite,
    secondaryContainer     = LightGray,
    onSecondaryContainer   = DarkText,
    background             = PureWhite,
    onBackground           = DarkText,
    surface                = PureWhite,
    surfaceContainerLowest = OffWhite,
    surfaceContainerLow    = OffWhite,
    surfaceContainer       = LightGray,
    surfaceContainerHigh   = Color(0xFFE8E8E8),
    surfaceContainerHighest= Color(0xFFE0E0E0),
    onSurface              = DarkText,
    onSurfaceVariant       = MediumGray,
    outline                = Color(0xFFBDBDBD),
    error                  = CrimsonRed,
    errorContainer         = CrimsonContainer,
    onError                = PureWhite,
    onErrorContainer       = CrimsonDark,
)

private val DarkColorScheme = darkColorScheme(
    primary                = CrimsonSoft,
    onPrimary              = CrimsonDark,
    primaryContainer       = CrimsonDark,
    onPrimaryContainer     = CrimsonSoft,
    background             = DarkBg,
    onBackground           = PureWhite,
    surface                = DarkSurface,
    surfaceContainerLowest = DarkBg,
    surfaceContainerLow    = DarkSurface,
    surfaceContainer       = Color(0xFF2D0000),
    surfaceContainerHigh   = Color(0xFF3D0000),
    surfaceContainerHighest= Color(0xFF4D0000),
    onSurface              = PureWhite,
    onSurfaceVariant       = CrimsonSoft,
    outline                = Color(0xFF6B3333),
    error                  = CrimsonSoft,
    errorContainer         = CrimsonDark,
    onError                = PureWhite,
    onErrorContainer       = CrimsonSoft,
)

@Composable
fun SvetOkTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}

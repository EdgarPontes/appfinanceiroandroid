package br.com.tecpontes.appfinanceiro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrandPrimary,
    onPrimary = DarkOnBrand,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkOnSurface,
    secondary = DarkBrandSecondary,
    onSecondary = DarkOnBrand,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = DarkCtaPrimary,
    onTertiary = DarkOnCta,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    scrim = DarkScrim
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnBrand,
    primaryContainer = SurfaceVariant,
    onPrimaryContainer = OnSurface,
    secondary = BrandSecondary,
    onSecondary = OnBrand,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = OnSurface,
    tertiary = CtaPrimary,
    onTertiary = OnCta,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    scrim = Scrim
)

@Composable
fun AppfinanceiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

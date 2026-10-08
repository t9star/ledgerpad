package jp.tpp.t9s.ledgerpad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F5A3),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF386A20),
    onSecondary = Color.White,
    background = SurfaceBg,
    onBackground = TextPrimary,
    surface = SurfaceBg,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF72796F),
    outlineVariant = OutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8BD88A),
    onPrimary = Color(0xFF003A0B),
    primaryContainer = GreenDark,
    onPrimaryContainer = Color(0xFFA6F5A3),
    secondary = Color(0xFF9DD67D),
    onSecondary = Color(0xFF133800),
    background = Color(0xFF111411),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF111411),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF222622),
    onSurfaceVariant = Color(0xFFC2C8BC),
)

@Composable
fun LedgerPadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

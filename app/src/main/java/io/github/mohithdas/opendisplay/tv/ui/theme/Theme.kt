package io.github.mohithdas.opendisplay.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF6FCF97)

private val DarkColors = darkColorScheme(
    primary = Accent,
    background = Color.Black,
    surface = Color.Black,
)

/**
 * Deliberately minimal: this app is a fullscreen video receiver, not a
 * content-heavy UI, so there is no design-system investment here yet.
 * A stable dark palette also keeps TV focus and video-edge contrast predictable.
 */
@Composable
fun OpenDisplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}

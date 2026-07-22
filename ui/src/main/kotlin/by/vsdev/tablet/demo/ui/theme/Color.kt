package by.vsdev.tablet.demo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF3D5AFE)
private val IndigoDark = Color(0xFFB9C3FF)
private val Teal = Color(0xFF00897B)
private val TealDark = Color(0xFF4DB6AC)

internal val LightColors =
    lightColorScheme(
        primary = Indigo,
        secondary = Teal,
    )

internal val DarkColors =
    darkColorScheme(
        primary = IndigoDark,
        secondary = TealDark,
    )

@Immutable
internal data class CellColors(
    val selected: Color,
    val onSelected: Color,
)

internal val LightCellColors =
    CellColors(
        selected = Color(0xFF2E7D32),
        onSelected = Color(0xFFFFFFFF),
    )

internal val DarkCellColors =
    CellColors(
        selected = Color(0xFF66BB6A),
        onSelected = Color(0xFF07230C),
    )

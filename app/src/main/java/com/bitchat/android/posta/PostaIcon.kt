package com.bitchat.android.posta

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Marca de Posta: tres teléfonos que se pasan el mensaje, de posta en posta.
 * Se pinta con el tinte del contenedor (Icon), igual que el icono original.
 */
val PostaIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PostaIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 17f)
            lineTo(12f, 7f)
            lineTo(19f, 17f)
        }
        // Nodos (círculos de radio 3 dibujados con dos arcos).
        for ((cx, cy) in listOf(5f to 17f, 12f to 7f, 19f to 17f)) {
            path(fill = SolidColor(Color.Black)) {
                moveTo(cx - 3f, cy)
                arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
                arcToRelative(3f, 3f, 0f, true, true, -6f, 0f)
                close()
            }
        }
    }.build()
}

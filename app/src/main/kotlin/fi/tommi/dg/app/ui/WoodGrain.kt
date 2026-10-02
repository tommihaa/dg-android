package fi.tommi.dg.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Puun syy pinnan taustan päälle ja sisällön alle: vaakasuorat, hieman aaltoilevat
 * hiuslinjat (puulaudat 30.9.2026, ks. [DgBoard.Wood]).
 *
 * Kuvio lasketaan [seed]istä eikä arvota piirrossa, joten sama pinta näyttää samalta joka
 * ruudunpäivityksellä eikä syy värise. Kuvatekstuuria ei käytetä, koska viivat skaalautuvat
 * puhelimen ja tabletin tiheyteen ilman eri kokoisia kuvia. Null [color] ei piirrä mitään.
 */
internal fun Modifier.woodGrain(color: Color?, seed: Int = 0): Modifier =
    if (color == null) {
        this
    } else {
        drawBehind {
            val spacing = 5.dp.toPx()
            val lines = (size.height / spacing).toInt().coerceAtLeast(2)
            val stroke = Stroke(width = 0.8.dp.toPx())
            val random = Random(seed)
            repeat(lines) { i ->
                val y0 = (i + 0.2f + random.nextFloat() * 0.6f) * spacing
                val path = Path().apply { moveTo(0f, y0) }
                // Neljä kaarta leveyttä kohden, kukin oman korkeutensa verran sivuun.
                val segments = 4
                val step = size.width / segments
                var y = y0
                repeat(segments) { s ->
                    val drift = (random.nextFloat() - 0.5f) * spacing * 0.8f
                    val bend = (random.nextFloat() - 0.5f) * spacing * 1.2f
                    val x = step * (s + 1)
                    path.quadraticTo(x - step / 2, y + bend, x, y + drift)
                    y += drift
                }
                drawPath(path, color, style = stroke)
            }
        }
    }

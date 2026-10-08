package fi.tommi.dg.app.ui

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Kultainen laakeriseppele odotuksen ilmaisimena (Tommin ajatus 4.10.2026 kuvakokoelman
 * kultaisista seppeleistä: *"kultaisia mahdollisuuksia animaatioiksi"*). Chatin neljästä
 * liikkeestä Tommi otti kaksi tabletille vertailuun, ja vertailun jälkeen molemmat jäivät:
 * [grow] kasvattaa oksat tyvestä latvaan, ja [grow] = false pitää seppeleen paikallaan ja
 * kuljettaa kiillon tyvestä latvaan.
 *
 * Avoin kaari kahdesta oksasta, solmu alhaalla, kuten kuvan vasemman yläkulman seppele.
 * **Omat värit eivätkä laudan**, koska kulta on muodon syy: sama paletti tavallisena ja
 * deco-viimeistelynä, ja vaaleassa teemassa tumma pronssi kuten `decoBare`lla.
 */
@Composable
internal fun BusyLaurel(modifier: Modifier, grow: Boolean) {
    val jakso = if (grow) LAUREL_GROW_MS else LAUREL_SHIMMER_MS
    val q by rememberPhase(if (grow) "laurelGrow" else "laurelShimmer", jakso)
    // Animaatiot pois: kasvava seppele näytetään valmiina eikä tyhjänä alkukuvana.
    val context = LocalContext.current
    val liikkuu = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val vaihe = if (liikkuu) q else 0.7f
    val p = laurelPalette()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(BUSY_LAUREL)) {
            withTransform({
                translate(center.x, center.y)
                scale(size.minDimension / 290f, size.minDimension / 290f, Offset.Zero)
            }) {
                if (grow) {
                    val kasvu = min(1f, vaihe / 0.62f)
                    val haipuu = if (vaihe > 0.85f) 1f - (vaihe - 0.85f) / 0.15f else 1f
                    wreath(p, kasvu, haipuu) { i, sisa ->
                        val s = (kasvu * (LEAVES + 1.5f) - i - if (sisa) 0.5f else 0f).coerceIn(0f, 1f)
                        s to 0.55f + 0.15f * s
                    }
                } else {
                    val aalto = vaihe * (LEAVES + 6) - 3f
                    wreath(p, 1f, 1f) { i, _ ->
                        val k = max(0f, 1f - abs(i - aalto) / 2.2f)
                        (0.95f + 0.08f * k) to 0.45f + 0.55f * k
                    }
                }
            }
        }
    }
}

/** Kolme sävyä varjosta kiiltoon; [shade] liukuu niiden välillä. */
private class LaurelPalette(val dark: Color, val mid: Color, val shine: Color) {
    fun shade(b: Float): Color =
        if (b < 0.5f) lerp(dark, mid, b * 2f) else lerp(mid, shine, (b - 0.5f) * 2f)
}

@Composable
private fun laurelPalette(): LaurelPalette =
    if (dgDark()) LaurelPalette(Color(0xFF8A6A1E), Color(0xFFD4AF37), Color(0xFFFFE9A0))
    else LaurelPalette(Color(0xFF46300E), Color(0xFF7A5A1E), Color(0xFFB08432))

/**
 * Kaksi oksaa solmusta ylös, [LEAVES] lehtiparia kummallakin. [leaf] antaa lehden koon ja
 * sävyn järjestysnumerosta tyvestä laskien; sisempi lehti on puoli askelta ulompaa jäljessä.
 */
private fun DrawScope.wreath(
    p: LaurelPalette,
    oksa: Float,
    alpha: Float,
    leaf: (i: Int, sisa: Boolean) -> Pair<Float, Float>,
) {
    if (alpha <= 0f) return
    val ala = (Math.PI / 2).toFloat()
    val kaari = (Math.PI * 0.86).toFloat()
    for (suunta in listOf(1f, -1f)) {
        if (oksa > 0f) {
            drawArc(
                color = p.shade(0.45f),
                startAngle = 90f,
                sweepAngle = suunta * 154.8f * oksa,
                useCenter = false,
                topLeft = Offset(-WREATH_R, -WREATH_R),
                size = Size(2 * WREATH_R, 2 * WREATH_R),
                style = Stroke(width = 3f),
                alpha = alpha,
            )
        }
        for (i in 0 until LEAVES) {
            val a = ala + suunta * kaari * (i + 0.5f) / LEAVES
            for (puoli in listOf(1f, -1f)) {
                val (s, b) = leaf(i, puoli < 0f)
                if (s <= 0.01f) continue
                val r = WREATH_R + puoli * 4f
                val kohta = Offset(r * kotlin.math.cos(a), r * kotlin.math.sin(a))
                val kulma = a + (Math.PI / 2).toFloat() + puoli * suunta * 0.9f
                withTransform({
                    translate(kohta.x, kohta.y)
                    rotate(kulma * 180f / Math.PI.toFloat(), Offset.Zero)
                    scale(s, s, Offset.Zero)
                }) {
                    drawPath(LEAF, p.shade(b), alpha = alpha)
                    drawLine(p.shade(max(0f, b - 0.35f)), Offset(0f, -2f), Offset(0f, -26f), 1.2f, alpha = alpha)
                }
            }
        }
    }
    drawCircle(p.shade(0.5f), 6f, Offset(0f, WREATH_R), alpha = alpha)
}

/** Suippo lehti tyvi origossa, kärki ylöspäin. */
private val LEAF = Path().apply {
    moveTo(0f, 0f)
    quadraticTo(9f, -14f, 0f, -30f)
    quadraticTo(-9f, -14f, 0f, 0f)
}

private const val LEAVES = 13

private const val WREATH_R = 95f

/** Kasvu, lepo ja haipuminen; hitaampi kuin muut, jotta lehdet ehtivät näkyä yksitellen. */
private const val LAUREL_GROW_MS = 3200

/** Kiilto kulkee tyvestä latvaan saman ajan kuin tiimalasi valuu. */
private const val LAUREL_SHIMMER_MS = 2400

/** Sama mittaluokka kuin kaarella ja deco-auringolla. */
private val BUSY_LAUREL = 64.dp

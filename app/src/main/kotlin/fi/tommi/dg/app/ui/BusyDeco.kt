package fi.tommi.dg.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.session.BusyStyle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Odotuksen ilmaisimien deco-viimeistely (Tommin tilaus 22.9.2026: *"art deco ja steampunk
 * viehättää minua"*, ja kahden tyylin luonnoksista *"elegantti synteesi"*, eli yksi tyyli
 * jossa decon geometria ja mustakultainen emali kantavat steampunkin mekaniikkaa). Rattaat
 * ovat ohuita kultaisia ääriviivoja eivätkä umpimessinkiä, niitit pieniä nastoja ja kupari
 * vain hehkuvana silmänä, jotta mekaniikka ei raskauta muotoa.
 *
 * **Omat värit eivätkä laudan** (Tommin kuittaus *"omat värit sopivat"*). Tavalliset muodot
 * seuraavat lautaa ([ArcLook]); nämä ovat sama kuva kaikilla laudoilla.
 *
 * Kytkin on [LocalBusyDeco], ja muoto luetaan samasta [LocalBusyStyle]sta kuin tavallisissa.
 * Tahdit ovat tavallisten muotojen tahdit, ja jokainen pyörivä osa kiertää kierroksen aikana
 * kokonaisen määrän hampaanvälejä, jotta kierroksen sauma ei näy.
 */
val LocalBusyDeco = compositionLocalOf { false }

private object Deco {
    val Gold = Color(0xFFD4AF37)
    val Brass = Color(0xFFB08A3E)
    val Cream = Color(0xFFF3E6CC)
    val Enamel = Color(0xFF0B0B0B)
    val Ink = Color(0xFF141414)
    val Copper = Color(0xFFC77A3A)
    val Shine = Color(0xFFF6E2A0)
}

/**
 * Paljaiden muotojen värit: ouroboros, ääretön-merkki ja tiimalasi piirtyvät suoraan ruudun
 * pohjalle eivätkä emalille kuten aurinko ja kuutio.
 *
 * **Vaalea teema saa tummemman paletin** (Tommi 27.9.2026: *"deco odotus-animaatiot ovat
 * valjuja vaaleassa tilassa"*). Kulta ja kerma oli valittu mustaa emalia vasten, ja kermalla
 * pohjalla kerma katosi ja kulta haaleni. Kahdesta laitteella kaapatusta vaihtoehdosta
 * (emalipohja paljaiden alle tai tummempi paletti) Tommi valitsi paletin, joten muodot pysyvät
 * kevyinä viivapiirroksina: kulta on tumma pronssi ja kerma muste. Tumma teema on ennallaan.
 */
private class DecoBare(val gold: Color, val brass: Color, val cream: Color)

@Composable
private fun decoBare(): DecoBare =
    if (dgDark()) DecoBare(Deco.Gold, Deco.Brass, Deco.Cream)
    else DecoBare(Color(0xFF8A6A1C), Color(0xFF6B5320), Color(0xFF2B2418))

private const val TAU = (2 * Math.PI).toFloat()

private fun deg(rad: Float) = rad * 180f / Math.PI.toFloat()

@Composable
fun BusyDeco(modifier: Modifier, style: BusyStyle) = when (style) {
    BusyStyle.ARC -> DecoSunburst(modifier)
    BusyStyle.OUROBOROS -> DecoOuroboros(modifier)
    BusyStyle.CUBE -> DecoCube(modifier)
    BusyStyle.INFINITY -> DecoInfinity(modifier)
    BusyStyle.HOURGLASS -> DecoHourglass(modifier)
    BusyStyle.RANDOM -> error("RANDOM ratkaistaan BusyIndicatorissa")
}

@Composable
private fun rememberPhase(label: String, periodMs: Int): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = label,
    )

/** Piirto luonnoksen yksiköissä: origo keskellä, [k] pikseliä yksikköä kohti. */
private inline fun DrawScope.units(k: Float, crossinline block: DrawScope.() -> Unit) {
    withTransform({
        translate(center.x, center.y)
        scale(k, k, Offset.Zero)
    }) { block() }
}

private fun DrawScope.gearOutline(c: Offset, r: Float, teeth: Int, rot: Float, color: Color, w: Float) {
    val p = Path()
    val d = TAU / teeth
    for (i in 0 until teeth) {
        val a = i * d + rot
        listOf(a to r * 0.84f, a + d * 0.12f to r, a + d * 0.42f to r, a + d * 0.54f to r * 0.84f)
            .forEachIndexed { j, (b, q) ->
                val x = c.x + cos(b) * q
                val y = c.y + sin(b) * q
                if (i == 0 && j == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
    }
    p.close()
    drawPath(p, color, style = Stroke(width = w, join = StrokeJoin.Miter))
    drawCircle(color, r * 0.55f, c, style = Stroke(width = w * 0.7f))
    drawCircle(color, r * 0.18f, c, style = Stroke(width = w * 0.7f))
    for (k in 0 until 6) {
        val b = k * TAU / 6 + rot
        drawLine(
            color,
            c + Offset(cos(b) * r * 0.18f, sin(b) * r * 0.18f),
            c + Offset(cos(b) * r * 0.55f, sin(b) * r * 0.55f),
            w * 0.7f,
        )
    }
}

private fun DrawScope.stud(c: Offset, r: Float) {
    drawCircle(Deco.Brass, r, c)
    drawCircle(Deco.Shine, r * 0.35f, c + Offset(-r * 0.3f, -r * 0.3f))
}

private fun lemniscate(p: Float, a: Float): Offset {
    val s = sin(p)
    val c = cos(p)
    val d = 1f + s * s
    return Offset(a * c / d, a * s * c / d)
}

/** Kaaren tilalla auringonsäteet emalitaulussa: säteet syttyvät kierroksena sekunnissa. */
@Composable
private fun DecoSunburst(modifier: Modifier) {
    val phase by rememberPhase("decoSunburst", 1008)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(DECO_ROUND)) {
            units(size.minDimension / 200f) {
                drawCircle(Deco.Enamel, 86f, Offset.Zero)
                val n = 24
                val head = (phase * n).toInt() % n
                for (i in 0 until n) {
                    val k = ((head - i) % n + n) % n
                    val alpha = 0.12f + 0.88f * max(0f, 1f - k / 12f)
                    rotate(degrees = i * 360f / n, pivot = Offset.Zero) {
                        val ray = Path().apply {
                            moveTo(-2.5f, -34f)
                            lineTo(2.5f, -34f)
                            lineTo(7f, -74f)
                            lineTo(-7f, -74f)
                            close()
                        }
                        drawPath(ray, if (i % 2 == 1) Deco.Gold else Deco.Cream, alpha = alpha)
                    }
                }
                drawCircle(Deco.Gold, 86f, Offset.Zero, style = Stroke(width = 4f))
                drawCircle(Deco.Gold, 93f, Offset.Zero, style = Stroke(width = 1.2f))
                drawCircle(Deco.Gold, 79f, Offset.Zero, style = Stroke(width = 1.2f))
                for (k in 0 until 12) {
                    val b = k * TAU / 12
                    stud(Offset(cos(b) * 86f, sin(b) * 86f), 2.6f)
                }
                // Kaksi hampaanväliä kierroksessa, kymmenhampainen ratas: sauma ei näy.
                gearOutline(Offset.Zero, 24f, 10, -phase * TAU / 5f, Deco.Gold, 2.2f)
            }
        }
    }
}

/** Chevron-suomuinen käärme kiertää vastakkain pyörivää ratasta, silmä hehkuu kuparina. */
@Composable
private fun DecoOuroboros(modifier: Modifier) {
    val phase by rememberPhase("decoOuroboros", 960)
    val c = decoBare()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(DECO_ROUND)) {
            units(size.minDimension / 200f) {
                gearOutline(Offset.Zero, 42f, 16, -phase * TAU * 10f / 16f, c.brass, 1.6f)
                rotate(degrees = phase * 360f, pivot = Offset.Zero) {
                    drawCircle(c.gold, 88f, Offset.Zero, style = Stroke(width = 1f))
                    val m = 20
                    val len = 5.3f
                    for (i in 0 until m) {
                        val s = i / m.toFloat()
                        val w = 4f + 11f * s
                        rotate(degrees = deg(s * len), pivot = Offset.Zero) {
                            translate(66f, 0f) {
                                val v = Path().apply {
                                    moveTo(-w, -w * 0.6f)
                                    lineTo(0f, w * 0.4f)
                                    lineTo(w, -w * 0.6f)
                                }
                                drawPath(
                                    v,
                                    if (i % 2 == 1) c.gold else c.cream,
                                    style = Stroke(width = 3.5f, join = StrokeJoin.Miter),
                                )
                                if (i % 4 == 1) stud(Offset(0f, -w * 0.2f), 1.8f)
                            }
                        }
                    }
                    rotate(degrees = deg(len), pivot = Offset.Zero) {
                        translate(66f, 0f) {
                            val head = Path().apply {
                                moveTo(0f, 21f)
                                lineTo(14f, 2f)
                                lineTo(0f, -8f)
                                lineTo(-14f, 2f)
                                close()
                            }
                            drawPath(head, c.gold)
                            val inner = Path().apply {
                                moveTo(0f, 17f)
                                lineTo(9f, 2f)
                                lineTo(0f, -4f)
                                lineTo(-9f, 2f)
                                close()
                            }
                            drawPath(inner, Deco.Ink, style = Stroke(width = 1.2f))
                            val hehku = 0.6f + 0.4f * sin(phase * TAU)
                            drawCircle(Deco.Copper, 6.5f, Offset(0f, 5f), alpha = 0.45f * hehku)
                            drawCircle(Deco.Copper, 3f, Offset(0f, 5f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kahdeksankulmainen emalikuutio nastoineen, numerot Poiret Onella. Robotin paikalla
 * auringonnousu, jonka keskiönä pyörii puolikas ratas; järjestys ja tahti kuten [BusyCube]lla.
 */
@Composable
private fun DecoCube(modifier: Modifier) {
    // Ratas pyörii tasaisesti omalla kellollaan, kuutio heittelee kuten tavallinen
    // [BusyCube] (24.9.2026, [rememberTumble]).
    val phase by rememberPhase("decoCube", 6 * 467)
    val tumble by rememberTumble(faces = 6, stepMs = 467)
    val density = LocalDensity.current
    val face = tumble.face
    val fontPx = with(LocalDensity.current) { (DECO_CUBE * if (face >= 4) 0.4f else 0.5f).toSp() }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .requiredSize(DECO_CUBE)
                .graphicsLayer { tumble(tumble, density) },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                units(size.minDimension / 124f) {
                    fun oct(r: Float, k: Float) = Path().apply {
                        moveTo(-r + k, -r)
                        lineTo(r - k, -r)
                        lineTo(r, -r + k)
                        lineTo(r, r - k)
                        lineTo(r - k, r)
                        lineTo(-r + k, r)
                        lineTo(-r, r - k)
                        lineTo(-r, -r + k)
                        close()
                    }
                    val outer = oct(58f, 16f)
                    drawPath(outer, Deco.Enamel)
                    drawPath(outer, Deco.Gold, style = Stroke(width = 4f))
                    drawPath(oct(49f, 12f), Deco.Gold, style = Stroke(width = 1.2f))
                    listOf(Offset(-44f, -44f), Offset(44f, -44f), Offset(-44f, 44f), Offset(44f, 44f))
                        .forEach { stud(it, 3.2f) }
                    if (face == 0) {
                        for (k in 0 until 7) {
                            translate(0f, 22f) {
                                rotate(degrees = -90f + (k - 3) * 17.2f, pivot = Offset.Zero) {
                                    val ray = Path().apply {
                                        moveTo(0f, -2.5f)
                                        lineTo(40f, -5f)
                                        lineTo(40f, 5f)
                                        lineTo(0f, 2.5f)
                                        close()
                                    }
                                    drawPath(ray, Deco.Gold)
                                }
                            }
                        }
                        clipRect(-30f, -10f, 30f, 22f) {
                            gearOutline(Offset(0f, 22f), 14f, 10, phase * TAU, Deco.Gold, 2f)
                        }
                    }
                }
            }
            if (face != 0) {
                Text(
                    text = (1 shl face).toString(),
                    color = Deco.Gold,
                    fontSize = fontPx,
                    lineHeight = fontPx,
                    fontFamily = PoiretOne,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** Kaksi lukittua ratasta, niiden ympäri raidoitettu ääretön-merkki ja sitä pitkin timantti. */
@Composable
private fun DecoInfinity(modifier: Modifier) {
    val phase by rememberPhase("decoInfinity", 1440)
    val c = decoBare()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(DECO_INFINITY_W, DECO_INFINITY_H)) {
            units(min(size.width / 192f, size.height / 96f)) {
                val r1 = phase * TAU
                gearOutline(Offset(-39f, 0f), 36f, 12, r1, c.brass, 2f)
                gearOutline(Offset(39f, 0f), 36f, 12, -r1 + TAU / 24f, c.brass, 2f)
                val rata = Path().apply {
                    for (i in 0..120) {
                        val p = lemniscate(i / 120f * TAU, 88f)
                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(rata, c.gold, style = Stroke(width = 8f))
                drawPath(rata, Deco.Ink, style = Stroke(width = 4.5f))
                drawPath(rata, c.gold, style = Stroke(width = 1.2f))
                for (k in 4 downTo 0) {
                    val p = lemniscate(-phase * TAU + k * 0.22f, 88f)
                    val z = if (k > 0) 6f - k else 9f
                    val d = Path().apply {
                        moveTo(p.x, p.y - z)
                        lineTo(p.x + z * 0.7f, p.y)
                        lineTo(p.x, p.y + z)
                        lineTo(p.x - z * 0.7f, p.y)
                        close()
                    }
                    drawPath(d, if (k > 0) c.cream else c.gold, alpha = if (k > 0) 0.5f - k * 0.08f else 1f)
                }
            }
        }
    }
}

/**
 * Viistetty lasi porrastetuin päädyin, nastat ja kummassakin päässä ratas. Hiekka käyttäytyy
 * kuten [BusyHourglass]illa. Rattaat ovat molemmissa päissä, jotta puolikas käännös tuottaa
 * saman kuvan kuin kierroksen alku; yksi ratas vain ylhäällä hyppäisi saumassa alas.
 */
@Composable
private fun DecoHourglass(modifier: Modifier) {
    val q by rememberPhase("decoHourglass", 2400)
    val valuu = 0.85f
    val f = min(q / valuu, 1f)
    val kaanto = if (q > valuu) {
        val u = (q - valuu) / (1f - valuu)
        180f * (0.5f - 0.5f * cos(u * Math.PI.toFloat()))
    } else {
        0f
    }
    val c = decoBare()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(DECO_HOURGLASS_W, DECO_HOURGLASS_H).rotate(kaanto)) {
            units(min(size.width / 110f, size.height / 176f)) {
                val hh = 56f
                val w = 36f
                val n = 4f
                val lasi = Path().apply {
                    moveTo(-n, 0f)
                    lineTo(-w, -hh * 0.45f); lineTo(-w, -hh * 0.85f); lineTo(-w * 0.6f, -hh)
                    lineTo(w * 0.6f, -hh); lineTo(w, -hh * 0.85f); lineTo(w, -hh * 0.45f)
                    lineTo(n, 0f)
                    lineTo(w, hh * 0.45f); lineTo(w, hh * 0.85f); lineTo(w * 0.6f, hh)
                    lineTo(-w * 0.6f, hh); lineTo(-w, hh * 0.85f); lineTo(-w, hh * 0.45f)
                    close()
                }
                clipPath(lasi) {
                    drawRect(c.gold, Offset(-w, -hh), Size(2 * w, 2 * hh), alpha = 0.07f)
                    val jaljella = 1f - f
                    if (jaljella > 0.01f) {
                        val pinta = -hh * 0.8f * sqrt(jaljella)
                        val kuoppa = hh * 0.14f * min(f * 6f, 1f) * sqrt(jaljella)
                        drawPath(
                            Path().apply {
                                moveTo(-w, pinta)
                                quadraticTo(-w * 0.25f, pinta, 0f, pinta + kuoppa)
                                quadraticTo(w * 0.25f, pinta, w, pinta)
                                lineTo(w, 0f)
                                lineTo(-w, 0f)
                                close()
                            },
                            c.cream,
                        )
                    }
                    val pohja = hh - hh * 0.55f * f
                    val huippu = pohja - hh * 0.28f * min(f * 3f, 1f)
                    val leveys = w * (0.35f + 0.65f * min(f * 2.5f, 1f))
                    drawPath(
                        Path().apply {
                            moveTo(-w, hh)
                            lineTo(-w, pohja)
                            lineTo(-leveys, pohja)
                            quadraticTo(-leveys * 0.2f, huippu, 0f, huippu)
                            quadraticTo(leveys * 0.2f, huippu, leveys, pohja)
                            lineTo(w, pohja)
                            lineTo(w, hh)
                            close()
                        },
                        c.cream,
                    )
                    if (q < valuu) {
                        for (i in 0 until 9) {
                            val t = ((q * 2400f / 260f) + i / 9f) % 1f
                            val sivu = ((i * 37) % 7 - 3) / 3f * n * 0.45f
                            drawCircle(c.cream, 2.3f, Offset(sivu, huippu * t * t))
                        }
                    }
                }
                drawPath(lasi, c.gold, style = Stroke(width = 2.5f, join = StrokeJoin.Miter))
                // 1,5 kierrosta eli 15 hampaanväliä: käännetty ala-ratas osuu ylä-rattaan kohdalle.
                val rattaat = q * TAU * 1.5f
                for (d in listOf(-1f, 1f)) {
                    drawLine(c.brass, Offset(d * (w + 9f), -hh - 4f), Offset(d * (w + 9f), hh + 4f), 2f)
                    drawRect(c.gold, Offset(-w - 14f, d * (hh + 9f) - 3f), Size(2 * w + 28f, 6f))
                    drawRect(c.gold, Offset(-w - 6f, d * (hh + 3f) - 2f), Size(2 * w + 12f, 4f))
                    stud(Offset(-w - 9f, d * (hh + 9f)), 2.8f)
                    stud(Offset(w + 9f, d * (hh + 9f)), 2.8f)
                    val top = if (d < 0) -hh - 40f else hh + 12f
                    clipRect(-30f, top, 30f, top + 28f) {
                        gearOutline(Offset(0f, d * (hh + 12f)), 15f, 10, if (d < 0) rattaat else rattaat + TAU / 2f, c.gold, 1.8f)
                    }
                }
            }
        }
    }
}

private val DECO_ROUND = 64.dp

private val DECO_CUBE = 54.dp

private val DECO_INFINITY_W = 84.dp

private val DECO_INFINITY_H = 48.dp

private val DECO_HOURGLASS_W = 46.dp

private val DECO_HOURGLASS_H = 70.dp

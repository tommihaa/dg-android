package fi.tommi.dg.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.Density
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Odotuskuution heittely yhtenä hetkenä: näkyvä tahko ja kolmen akselin kierto.
 *
 * Tommin tilaus 24.9.2026: *"tuplauskuutio-animaatio pitäisi heilua, pyöriä ja muuttua kaikki
 * xyz-akselit ja random spiniä, nyt monotoninen kuvio"*. Aiempi käännös oli joka tahkolla
 * sama litistys ja 17 asteen kallistus, eli kuvio toistui kolmen sekunnin välein
 * täsmälleen samana.
 *
 * Jokainen tahkon vaihto arpoo oman heittonsa: kääntöakselin (X tai Y ja suunta), toisen
 * akselin heilahduksen, Z-kierron uuteen lepokulmaan ja joskus kokonaisen pyörähdyksen
 * sen päälle. Lepokulma jää ±30 asteeseen, joten numero on luettava joka tahkon keskellä.
 * Tahkojen järjestys ja tahti eivät muutu: robotti ensin (18.9.2026) ja [stepMs] tahkolta.
 */
internal class Tumble(
    /** Näkyvän tahkon järjestysnumero, 0 on kierroksen ensimmäinen. */
    val face: Int,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val scale: Float,
)

/** Kuution kierto [GraphicsLayerScope]en; kameran etäisyys pitää perspektiivin maltillisena. */
internal fun GraphicsLayerScope.tumble(t: Tumble, density: Density) {
    rotationX = t.rotationX
    rotationY = t.rotationY
    rotationZ = t.rotationZ
    scaleX = t.scale
    scaleY = t.scale
    cameraDistance = 10f * density.density
}

/** Yhden tahkon vaihdon arvottu heitto. */
private class Throw(
    val aroundX: Boolean,
    val direction: Float,
    val wobble: Float,
    val zFrom: Float,
    val zTo: Float,
)

private fun nextThrow(random: Random, zFrom: Float): Throw {
    // Lepokulma ±30 astetta, ja joka kolmas heitto lisää kokonaisen pyörähdyksen jompaan
    // kumpaan suuntaan: se on "random spin", lepokulma pysyy silti luettavana.
    val rest = random.nextFloat() * 60f - 30f
    val spin = if (random.nextInt(3) == 0) (if (random.nextBoolean()) 360f else -360f) else 0f
    return Throw(
        aroundX = random.nextBoolean(),
        direction = if (random.nextBoolean()) 1f else -1f,
        wobble = (random.nextFloat() * 2f - 1f) * 35f,
        zFrom = zFrom,
        zTo = rest + spin,
    )
}

/**
 * Heittelyn hetki, [faces] tahkoa kierroksella ja [stepMs] millisekuntia tahkolta.
 *
 * Kello on ruudunpäivitys eikä `InfiniteTransition`, koska joka vaihdon heitto arvotaan
 * uudelleen eikä kierros saa toistua.
 */
@Composable
internal fun rememberTumble(faces: Int, stepMs: Int): State<Tumble> {
    val state = remember { mutableStateOf(Tumble(0, 0f, 0f, 0f, 1f)) }
    LaunchedEffect(faces, stepMs) {
        val random = Random(System.nanoTime())
        var step = 0L
        var current = nextThrow(random, zFrom = 0f)
        val start = withFrameMillis { it }
        while (true) {
            val elapsed = withFrameMillis { it } - start
            val nowStep = elapsed / stepMs
            while (step < nowStep) {
                // Uusi heitto alkaa siitä kulmasta johon edellinen jäi, joten Z ei hyppää.
                current = nextThrow(random, zFrom = current.zTo % 360f)
                step++
            }
            val u = (elapsed % stepMs).toFloat() / stepMs
            state.value = pose(current, step, u, faces)
        }
    }
    return state
}

private fun pose(th: Throw, step: Long, u: Float, faces: Int): Tumble {
    // Kääntö 0..90 astetta ensimmäisellä puoliskolla ja -90..0 toisella: tahko vaihtuu
    // reunan kohdalla (u = 0,5), ja uusi tahko nousee oikein päin eikä peilattuna.
    val flip = (if (u < 0.5f) u * 180f else u * 180f - 180f) * th.direction
    val face = ((if (u < 0.5f) step else step + 1) % faces).toInt()
    // Heilahdus on nolla tahkon levossa ja suurin käännön keskellä.
    val swing = sin(u * PI).toFloat()
    val wobble = th.wobble * swing
    // Z liukuu pehmeästi lepokulmasta toiseen (kosini-easing), pyörähdys mukaan lukien.
    val ease = (1f - cos(u * PI).toFloat()) / 2f
    val z = th.zFrom + (th.zTo - th.zFrom) * ease
    // Pieni hyppy: kuutio nousee käännön ajaksi kohti katsojaa.
    val scale = max(1f + 0.12f * swing, 0.1f)
    return Tumble(
        face = face,
        rotationX = if (th.aroundX) flip else wobble,
        rotationY = if (th.aroundX) wobble else flip,
        rotationZ = z,
        scale = scale,
    )
}

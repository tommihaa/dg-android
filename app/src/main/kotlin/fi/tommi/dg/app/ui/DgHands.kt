package fi.tommi.dg.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * Käsijälki: maaliin kastettujen käsien joukko taustakuviona.
 *
 * **Tilaus on Tommin (6.9.2026 pelisession jälkeen)**: *"maaliin kastettujen käsien joukko,
 * eri värejä, käsistä muodostuu kuvioita."* Lähde on Far Cry Primal ja sen takana
 * luolamaalausten käsijälki, esihistoriallinen traditio (Cueva de las Manos, Chauvet). Se on
 * ensimmäinen motiivi jolla on ihmisen tekijä eikä eläin.
 *
 * **Lopputulos on luolan seinä eikä kuvio, ja se on Tommin arvio laitekuvasta.** Saman illan
 * luonnoserissä (`tyokalut/kasiluonnos.py`) hän valitsi ensin *kädet kuviona* ja viisi kuviota
 * (aurinko, Fibonaccin kierre, sydän, puu, nopat), ja ne vietiin laitteelle asti. Laitekuvan
 * jälkeen: *"kädet ei vakuuta, ne ovat liian suuria, kun taas satunnainen luolakuva toimii
 * paremmin, sillä ei ole rajoituksia."* Kuvioversio on git-historiassa (commit 30e5deb), ja
 * luonnostyökalu piirtää sen yhä `--kuviot`-lipulla.
 *
 * **Koko on dp, ei ruudun osuus.** Kuvioversio mitoitti kädet leveyden osuuksina, ja
 * tabletilla (1752 px) ne olivat 1,6-kertaisia luonnoksen puhelimeen (1080 px) nähden. Se on
 * yksi syy siihen että ne olivat liian suuria. Käsi on nyt [HandDp] kertaa tiheys, eli sama
 * fyysinen koko joka laitteella, ja joukossa on lapsen käsiä (Tommi: *"kädet ei pidä olla
 * samankokoisia"*): noin joka neljäs on puolet aikuisen koosta.
 *
 * **Käsi on kämmen ja viisi sormea, ei valokuvan ääriviiva.** Sormet ovat pyöreäpäisiä
 * palkkeja ja kämmen soikio, koska luolan jälki on sekin tahra eikä piirros.
 *
 * **Käsiä ei enää piirretä (Tommi 7.9.2026 illalla: *"kädet voi tiputtaa myös pois"*).**
 * Ne olivat mukana neljänä muotona: viisi kuviota (commit 30e5deb), seinä mustalla (79f0879),
 * riitin kädet šamaanin ympärillä (92ac098) ja Cueva de las Manosin ryhmät täytekuvana
 * (1d4d5d3 asti). Jokainen kaatui laitekuvaan. Tiedosto jää, koska [DgHand], [dgHandPath] ja
 * arvonta-apurit ([uniform], [gauss], [handSize]) ovat yhä käytössä tai testattavia, ja koska
 * käden geometria on valmis jos siihen palataan.
 *
 * **Asettelu on puhdasta laskentaa ja polut rakennetaan erikseen** ([dgHandsScene]), jotta
 * JVM-testi voi todeta sijainnit ilman `android.graphics.Path`ia, samoin kuin
 * `DgSkyLayoutTest` tekee taivaalle.
 *
 * Luettavuus on mitattu lukupinnan alta kuten muut: maavärien kirkkain (kaoliini) pinnan alla
 * antaa Lounge-aksentille 7,83:1 tummassa, ja vaalean pigmenttien tummin 4,90:1. AA-raja on
 * 4,5:1. Luvut ovat luonnostyökalun tulosteesta.
 */

/**
 * Yksi käsi. Sijainti ja [size] (kämmenen korkeus) pikseleinä, [angle] radiaaneina niin että
 * 0 on sormet ylös, [color] paletin indeksi, [spread] sormien haja-asento 0…1 ja [mirror]
 * vasen vai oikea käsi.
 */
internal data class DgHand(
    val cx: Float,
    val cy: Float,
    val size: Float,
    val angle: Float,
    val color: Int,
    val alpha: Float,
    val spread: Float,
    val mirror: Boolean,
)

internal data class DgHandsLayout(val hands: List<DgHand>)

/**
 * Maavärit: okra, punamulta, hiili (vaaleana, koska tausta on musta) ja kaoliini. Tumman
 * teeman kädet. Luonnoksen luolan seinä oli näillä, ja Primal (kylläisempi, yksi kylmä) jäi
 * kuvioversion mukana pois; se on `tyokalut/kasiluonnos.py`:ssä jos siihen palataan.
 */
internal val DgHandsEarth: List<Color> = listOf(
    Color(0xFFD9A441), Color(0xFFB8442C), Color(0xFF8C8378), Color(0xFFEDE3CF),
)

// --- apurit ------------------------------------------------------------------------------

internal fun Random.uniform(a: Float, b: Float): Float = a + nextFloat() * (b - a)

/** Likimääräinen normaalijakauma: kolmen tasajakauman summa, hajonta [sd]. */
internal fun Random.gauss(sd: Float): Float = (nextFloat() + nextFloat() + nextFloat() - 1.5f) * 2f * sd

/** Käden koko lapsesta aikuiseen; kaksihuippuinen, ks. tiedoston kommentti. */
internal fun Random.handSize(size: Float): Float =
    if (nextFloat() < 0.28f) size * uniform(0.50f, 0.70f) else size * uniform(0.80f, 1.35f)

/** Polun piste: sijainti ja sormien suunta. */
private class Along(val x: Float, val y: Float, val direction: Float)

/** Kädet polun varrelle. Väri kiertää [colors]-listaa, jotta vierekkäiset eivät ole samaa. */
private fun Random.handsAlong(path: List<Along>, colors: List<Int>, size: Float): List<DgHand> =
    path.mapIndexed { i, a ->
        DgHand(
            cx = a.x, cy = a.y, size = handSize(size), angle = a.direction + gauss(0.10f),
            color = colors[i % colors.size], alpha = uniform(0.6f, 0.85f),
            spread = nextFloat(), mirror = i % 2 == 0,
        )
    }

/** Aikuisen käden mitta (kämmenen korkeus) dp:nä. Ks. tiedoston kommentti koosta. */
internal const val DgHandDp = 40f

// --- käden geometria ---------------------------------------------------------------------

/** Sormi paikalliskoordinaateissa: tyvi, pituus ja leveys kämmenen koon osuuksina, kallistus radiaaneina. */
private class Finger(val bx: Float, val by: Float, val length: Float, val width: Float, val tilt: Float)

/** Neljä sormea kämmenen yläreunasta ja peukalo sivusta. Pikkusormi lyhin, keskisormi pisin. */
private fun fingers(h: DgHand): List<Finger> {
    val s = h.size
    val pw = 0.42f * s
    val ph = 0.5f * s
    val fw = 0.19f * s
    val spread = 0.10f + 0.22f * h.spread
    val four = listOf(-0.72f to 0.62f, -0.24f to 0.84f, 0.24f to 0.90f, 0.72f to 0.76f).map { (rel, ln) ->
        Finger(rel * pw * 0.95f, -ph * 0.72f, s * ln, fw, rel * spread)
    }
    return four + Finger(-pw * 0.95f, ph * 0.05f, s * 0.62f, fw * 1.05f, -(0.95f + 0.35f * h.spread))
}

/** Paikallispiste ruudulle: peilaus, kierto ja siirto samassa järjestyksessä kuin polulla. */
private fun DgHand.place(lx0: Float, ly: Float): Offset {
    val lx = if (mirror) -lx0 else lx0
    val ca = cos(angle)
    val sa = sin(angle)
    return Offset(cx + lx * ca - ly * sa, cy + lx * sa + ly * ca)
}

/** Sormenpäät ruudun koordinaateissa. Negatiivijäljen pilvet keskittyvät näihin ja kämmeneen. */
internal fun dgHandTips(h: DgHand): List<Offset> = fingers(h).map { f ->
    h.place(f.bx + sin(f.tilt) * f.length, f.by - cos(f.tilt) * f.length)
}

/**
 * Käden polku: soikio kämmeneksi, nelikulmio ranteeksi ja viisi pyöreäpäistä palkkia sormiksi,
 * yhdistettynä unionilla jotta puoliksi läpinäkyvä täyttö ei tummene päällekkäisyyksissä.
 */
internal fun dgHandPath(h: DgHand): Path {
    val s = h.size
    val pw = 0.42f * s
    val ph = 0.5f * s
    val hand = Path().apply { addOval(Rect(-pw, -ph, pw, ph)) }
    val wrist = Path().apply {
        moveTo(-pw * 0.7f, ph * 0.3f); lineTo(pw * 0.7f, ph * 0.3f)
        lineTo(pw * 0.55f, ph * 1.05f); lineTo(-pw * 0.55f, ph * 1.05f); close()
    }
    hand.op(hand, wrist, PathOperation.Union)
    for (f in fingers(h)) {
        val capsule = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(-f.width / 2f, -f.length - f.width / 2f, f.width / 2f, f.width / 2f),
                    CornerRadius(f.width / 2f),
                ),
            )
        }
        val m = Matrix()
        m.translate(f.bx, f.by)
        m.rotateZ(f.tilt * 180f / PI.toFloat())
        capsule.transform(m)
        hand.op(hand, capsule, PathOperation.Union)
    }
    val m = Matrix()
    m.translate(h.cx, h.cy)
    m.rotateZ(h.angle * 180f / PI.toFloat())
    if (h.mirror) m.scale(-1f, 1f, 1f)
    hand.transform(m)
    return hand
}

/** Piirtovalmiit kädet: polut laskettu kerran per koko ja siemen. */
internal class DgHandsScene(val hands: List<Pair<Path, DgHand>>)

internal fun dgHandsScene(layout: DgHandsLayout): DgHandsScene =
    DgHandsScene(layout.hands.map { dgHandPath(it) to it })

/** Piirto: positiivijälki, käsi paletin värillä puoliksi läpinäkyvänä, kerrostuen. */
internal fun DrawScope.drawHands(scene: DgHandsScene, palette: List<Color>) {
    for ((path, hand) in scene.hands) drawPath(path, palette[hand.color], alpha = hand.alpha, style = Fill)
}

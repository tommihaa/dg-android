package fi.tommi.dg.app.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Täytekuva: kuva joka asettuu tyhjään tilaan sisällön alle.
 *
 * **Tausta ja täytekuva ovat eri lajia, ja tila ratkaisee kumpi näkyy** (Tommin päätökset
 * 7.9.2026, `docs/AVOIMET.md` › *Taustageneraattori ja teemakirjasto*). Tausta on koko ruudun
 * tessellaatio (`DgPattern.kt`), jonka päällä teksti lukee lukupinnan läpi. Täytekuva
 * piirretään vain kun sisällön alle jää vähintään [DgFillMinHeight] vapaata; muuten ruudulla
 * on pelkkä tausta. Kynnys on Tommin (*"180 dp"*) luonnoksesta jossa riitti 156 dp:ssä oli
 * pieni ja tungettu (`tyokalut/tayteluonnos.py`).
 *
 * **Täytekuva on laitteen kansion taustakuvat** (`DgWallpaper.kt`, Tommin päätökset
 * 15.9.2026). Kansio on oma kytkimensä, ja kuvat näkyvät vaikka tessellaatio olisi pois.
 * Kun kansiota ei ole tai kuvat ovat vielä purkamatta, tila saa pelkän taustan. Luolan seinä
 * (`DgCave.kt`, 7.9.–17.9.2026) oli ensimmäinen täytekuva samaan paikkaan ja taustakuvien
 * varakuva; se poistettiin Tommin sanalla 17.9.2026 (*"luolamaalauskuvia ei enää tarvita
 * taustaksi"*), ja sen luonnos on yhä `tyokalut/luolaluonnos.py`.
 *
 * **Vapaan alueen raja tunnetaan vain piirtovaiheessa**, samasta syystä kuin lukupinnan raja
 * (`DgReadingSurface.kt`): laiska lista mittautuu aina täyteen kokoon. Siksi täytekuva
 * piirretään samassa `drawBehind`-kutsussa kuin lukupinta, alueelle joka alkaa viimeisen
 * rivin alareunasta. Vierivälle sarakkeelle sama tehdään kehyksellä ([DgFillFrame]), joka
 * mittaa sisällön korkeuden.
 */

/** Vapaan tilan kynnys: alle tämän piirretään pelkkä tausta. Tommin päätös 7.9.2026. */
internal val DgFillMinHeight: Dp = 180.dp

/** Onko täytekuvalle tilaa. Erillinen, jotta JVM-testi voi todeta kynnyksen ilman `DrawScope`a. */
internal fun dgFillShows(freePx: Float, minPx: Float): Boolean = freePx >= minPx

/** Täytekuvan parametrit piirtoon: taustakuvat ja nimilaatikon tyyli. */
internal class DgFill(
    val wallpaper: DgWallpaper,
    /** Nimilaatikon mittari, tyyli ja tekstin väri; laatikon pohja on lukupinnan väri. */
    val measurer: TextMeasurer? = null,
    val labelStyle: TextStyle = TextStyle.Default,
    val labelColor: Color = Color.Unspecified,
    val labelSurface: Color = Color.Unspecified,
)

@Composable
internal fun rememberDgFill(): DgFill? {
    val wallpaper = LocalDgWallpaper.current ?: return null
    return DgFill(
        wallpaper,
        measurer = rememberTextMeasurer(),
        labelStyle = MaterialTheme.typography.labelMedium,
        labelColor = MaterialTheme.colorScheme.onBackground,
        labelSurface = dgReadingSurfaceColor(),
    )
}

/**
 * Napautus täytekuvan alueella: kuvan päällä merkitsee sen nimen näkyviin, muualla poistaa
 * merkinnän. Vieritys ei ole napautus, joten lista vierii kuten ennenkin.
 */
internal fun Modifier.dgFillTaps(fill: DgFill?): Modifier {
    val wallpaper = fill?.wallpaper ?: return this
    return this.pointerInput(wallpaper) {
        detectTapGestures { wallpaper.tap(it) }
    }
}

/** Nimilaatikon sisäreunus ja etäisyys kuvan yläreunasta. */
internal val DgLabelPad: Dp = 6.dp

/**
 * Nimilaatikko kuvan [rect] yläpuolelle, keskitettynä ja kuvan levyisenä enintään. Jos
 * yläpuolella ei ole vapaata tilaa ([freeTop] on vapaan alueen yläreuna samoissa
 * koordinaateissa), laatikko menee kuvan sisään sen yläreunaan: tausta piirtyy sisällön alle,
 * joten listan alueelle piirretty laatikko jäisi rivien alle (Tommin havainto laitteelta
 * 15.9.2026 illalla, ylin kuva).
 */
private fun DrawScope.drawLabel(fill: DgFill, name: String, rect: Rect, freeTop: Float) {
    val measurer = fill.measurer ?: return
    val pad = DgLabelPad.toPx()
    val maxWidth = (rect.width - 2 * pad).toInt().coerceAtLeast(1)
    val layout = measurer.measure(
        text = name,
        style = fill.labelStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = maxWidth),
    )
    val w = layout.size.width + 2 * pad
    val h = layout.size.height + 2 * pad
    val left = rect.left + (rect.width - w) / 2f
    var top = rect.top - pad - h
    if (top < freeTop) top = rect.top + pad
    drawRoundRect(fill.labelSurface, Offset(left, top), androidx.compose.ui.geometry.Size(w, h), CornerRadius(pad))
    drawText(layout, fill.labelColor, Offset(left + pad, top + pad))
}

/**
 * Piirtää täytekuvan alueelle [top]…alareuna, jos [fill] on annettu ja alue on kynnyksen
 * korkuinen. Palauttaa piirrettiinkö, jotta kutsupaikka voi jättää muun taustan siltä osin pois.
 */
internal fun DrawScope.drawDgFill(fill: DgFill?, top: Float): Boolean {
    if (fill == null) return false
    val free = size.height - top
    if (!dgFillShows(free, DgFillMinHeight.toPx())) return false
    val wallpaper = fill.wallpaper
    val images = wallpaper.images
    if (images.isEmpty()) return false
    // Asettelu on halpa (kolme suorakulmiota), joten se lasketaan joka piirrossa. Listan ja
    // kuvien väliin jää sama väli kuin kuvien kesken (Tommin pyyntö laitekuvasta 15.9.2026
    // illalla: kuva alkoi suoraan viimeisen rivin alareunasta).
    val gap = DgWallpaperGap.toPx()
    val ratios = images.map { it.width.toFloat() / it.height.toFloat() }
    val rects = dgWallpaperLayout(size.width, free - gap, ratios, gap)
    // Osumapaikat piirtoalueen koordinaateissa, jotta napautus lukee saman kuin silmä.
    wallpaper.rects = rects.map { it.translate(0f, top + gap) }
    val labelled = wallpaper.labelled
    translate(top = top + gap) {
        rects.forEachIndexed { i, r ->
            val image = images[i]
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(r.left.toInt(), r.top.toInt()),
                dstSize = IntSize(r.width.toInt(), r.height.toInt()),
            )
        }
        if (labelled != null && labelled < rects.size) {
            wallpaper.names.getOrNull(labelled)?.let { drawLabel(fill, it, rects[labelled], -gap) }
        }
    }
    return true
}

/**
 * Kehys vierivälle sarakkeelle: sisältö ylhäällä omassa koossaan, täytekuva sen alle jäävään
 * tilaan. Sisältö joka täyttää kehyksen jättää vapaata nolla, jolloin kuvaa ei piirretä.
 */
@Composable
internal fun DgFillFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val fill = rememberDgFill()
    var contentHeight by remember { mutableFloatStateOf(0f) }
    Box(modifier = modifier.fillMaxSize().dgFillTaps(fill).drawBehind { drawDgFill(fill, contentHeight) }) {
        Box(modifier = Modifier.onSizeChanged { contentHeight = it.height.toFloat() }) { content() }
    }
}

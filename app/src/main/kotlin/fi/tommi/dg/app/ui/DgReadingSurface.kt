package fi.tommi.dg.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Lukupinnan peittävyys: taustaväri tällä alphalla sisällön alla, kuvion päällä.
 *
 * **Tommin päätös 6.9.2026: *"vain tekstin alta"*, ja lukupinta kelpaa mekanismiksi.** Kuvio
 * saa olla tyhjässä tilassa täysi, mutta tekstin ja toimintojen alla se ei saa vaikeuttaa
 * lukemista. Lukupinta on tapa jolla sama kuvio täyttää molemmat ehdot: sisältö piirtyy
 * taustavärisen pinnan päälle, joka vaimentaa kuvion, ja pinta loppuu siihen mihin sisältö
 * loppuu.
 *
 * **Arvo on mitattu** (`tyokalut/taustaluonnos.py`, `docs/UI.md` › *Tyhjä tila täyteen*).
 * Kuvion tummin kohta (lintuviiva, peittävyys 0,65 mustetta `#6B5844`) antaa pinnan alla
 * `#F4F1EC`, jota vasten matalin välilehtiaksentti (Lounge `#2A7430`) on 5,12:1. Sumi-e-tilan
 * lintuviiva antaa `#F7EBE6` ja 4,94:1. AA-raja on 4,5:1. Ilman pintaa kumpikaan ei ylitä.
 * Pinta oli ensin 0,85, sitten 0,90, ja 7.9.2026 se nousi 0,93:een kun Tommi sanoi tekstin
 * lukemisen vaikeutuvan: tekstin alle jäävä tehollinen peittävyys on nyt enintään 0,045,
 * eli ohuempi kuin alkuperäisen kuvion tummin tahko (0,075).
 */
internal const val DgReadingSurfaceAlpha = 0.93f

/**
 * Lukupinnan väri nykyisessä teemassa. Tumma teema sai kuvion 6.9.2026 illalla, ja sama alpha
 * kelpaa sinne Tommin päätöksellä (*"lukupinta sama kuin vaalealla"*): kirkkain viiva
 * (`#DDF7FF`) antaa pinnan alla `#2A2A2F`, jota vasten tumman Lounge `#A8C69A` on 7,62:1.
 */
@Composable
internal fun dgReadingSurfaceColor(): Color =
    MaterialTheme.colorScheme.background.copy(alpha = DgReadingSurfaceAlpha)

/**
 * Lukupinta sisällön korkuisena: vierivälle sarakkeelle (`verticalScroll`) tämä on tavallinen
 * tausta, koska sarake mittautuu sisältönsä korkuiseksi. Sijoita ketjussa `verticalScroll`in
 * jälkeen, jotta pinta vierii sisällön mukana eikä jää ikkunan kokoiseksi.
 */
@Composable
internal fun Modifier.dgReadingSurface(): Modifier = this.background(dgReadingSurfaceColor())

/**
 * Vuororaidoituksen (zebra striping) rivin tunniste: `items`-kutsun `contentType`. Lukupinta
 * piirtää joka toiselle tällä merkitylle riville raidan, ja otsikot, selitteet ja muut
 * yksittäiset `item`-kohdat jäävät raidoituksen ulkopuolelle.
 *
 * **Tommin tilaus 17.9.2026**: *"luetteloihin pitäisi saada eri rivit hieman eri väreillä,
 * helpottaa luettavuutta"*, ja valinta kahdesta kysymyksestä: kaikkiin riviluetteloihin, ja
 * raita viivojen lisäksi eikä niiden sijaan. Raita on lukupinnan päällä sen tekstivärin
 * ohut kerros ([DgStripeAlpha]), joten se tummentaa vaaleassa ja vaalentaa tummassa
 * teemassa, eikä kuvion vaimennus muutu. Parillisuus luetaan listan indeksistä: peräkkäiset
 * rivit vuorottelevat aina, ja otsikon jälkeen ensimmäinen rivi saa kumman tahansa.
 */
internal const val DgStripedRow = "dg-striped-row"

/**
 * Raita yhdelle riville tavallisessa sarakkeessa, jossa `contentType`-mekanismia ei ole
 * (asetusruutu, Tommin tilaus 17.9.2026). Kutsupaikka laskee parillisuuden itse. Sijoita
 * ketjussa ennen `padding`ia ja kosketusmuokkaajia, jotta raita on rivin levyinen.
 */
@Composable
internal fun Modifier.dgStripe(striped: Boolean): Modifier =
    if (striped) this.background(MaterialTheme.colorScheme.onBackground.copy(alpha = DgStripeAlpha)) else this

/**
 * Raidan peittävyys tekstivärillä lukupinnan päällä. Tommin valinta 17.9.2026 neljästä
 * kuvasta (4 %, 6 %, 9 %, primary 10 %) molemmissa teemoissa: *"C, näkyy parhaiten"*.
 */
internal const val DgStripeAlpha = 0.09f

/**
 * Lukupinta laiskalle listalle: pinta ulottuu viimeisen näkyvän rivin alareunaan, kun se on
 * listan viimeinen, ja muuten koko korkeuteen. Lista mittautuu aina täyteen kokoon, joten
 * pinnan raja on luettava asettelutiedosta eikä koosta. Luku tehdään piirtovaiheessa, joten
 * vieritys piirtää pinnan uudestaan ilman uudelleenkompositiota.
 *
 * **Sama raja on täytekuvan yläreuna** (`DgFill.kt`, 7.9.2026): pinnan alle jäävä tila saa
 * taustakuvat, jos sitä on kynnyksen verran. Tyhjä lista on kokonaan vapaata.
 */
@Composable
internal fun Modifier.dgReadingSurface(state: LazyListState): Modifier {
    val color = dgReadingSurfaceColor()
    val stripe = MaterialTheme.colorScheme.onBackground.copy(alpha = DgStripeAlpha)
    val fill = rememberDgFill()
    return this.dgFillTaps(fill).drawBehind {
        val info = state.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull()
        if (last == null) {
            drawDgFill(fill, 0f)
            return@drawBehind
        }
        val bottom = if (last.index == info.totalItemsCount - 1) {
            (last.offset + last.size + info.afterContentPadding - info.viewportStartOffset)
                .toFloat()
                .coerceIn(0f, size.height)
        } else {
            size.height
        }
        drawRect(color, size = Size(size.width, bottom))
        for (item in info.visibleItemsInfo) {
            if (item.contentType != DgStripedRow || item.index % 2 == 0) continue
            val top = (item.offset - info.viewportStartOffset).toFloat()
            drawRect(stripe, topLeft = Offset(0f, top), size = Size(size.width, item.size.toFloat()))
        }
        if (bottom < size.height) drawDgFill(fill, bottom)
    }
}

/**
 * `LazyColumn` lukupinnalla. Sama kutsumuoto kuin alkuperäisellä niiltä osin kuin ruudut
 * sitä käyttävät, jotta vaihto on yhden sanan muutos kutsupaikassa.
 */
@Composable
internal fun DgLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    val state = rememberLazyListState()
    LazyColumn(
        modifier = modifier.dgReadingSurface(state),
        state = state,
        contentPadding = contentPadding,
        content = content,
    )
}

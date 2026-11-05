package fi.tommi.dg.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.session.AppTheme
import kotlinx.coroutines.launch

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
    if (striped) this.background(dgStripeColor()) else this

/**
 * Raidan väri. Oletuksena tekstiväri [DgStripeAlpha]n peittävyydellä, mutta vaaleassa Decossa
 * messinki, koska lähes musta teksti norsunluulla antoi harmaan raidan (laitteella 26.9.2026,
 * Tommin havainto). Messinki 8,5 %:lla antaa norsunluun päälle luettelorivin `#EDE3CC`, jota
 * vasten vaalean Decon kontrastit on mitattu (`docs/ASETUKSET.md` › Vaalea Deco).
 */
@Composable
internal fun dgStripeColor(): Color =
    if (LocalAppTheme.current == AppTheme.DECO && !dgDark()) {
        MaterialTheme.colorScheme.primary.copy(alpha = DecoLightStripeAlpha)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = DgStripeAlpha)
    }

private const val DecoLightStripeAlpha = 0.085f

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
    val stripe = dgStripeColor()
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
 *
 * **[onPull] antaa listalle vedon alaspäin** (testaajan toive palstalla 20.10.2026, Tommin
 * rajaus: jokainen listaruutu jolla on `Refresh`, paitsi asetukset). Ele tekee täsmälleen
 * saman kuin ruudun `Refresh`-nappi, joka jää paikalleen: ele ei näy ruudulla eikä
 * ruudunlukija voi tehdä sitä. Asetuksissa veto voisi hakea lomakkeen keskeneräisten
 * muutosten päälle, joten sinne sitä ei anneta.
 *
 * Toisin kuin lautaruudulla (`pullDownToRefresh`), tässä kelpaa Material3:n valmis
 * komponentti, koska lista vierii ja vieritys on juuri se mitä komponentti kuuntelee.
 * `isRefreshing` on aina epätosi: haun etenemisen näyttää ruudun oma `ProgressSlot`, joten
 * pyöreä ilmaisin näkyy vain vedon aikana eikä kahta latausmerkkiä ole yhtä aikaa.
 *
 * **Ilmaisin piilotetaan käsin.** Komponentti vetää sen pois vasta kun `isRefreshing` vaihtuu,
 * ja koska se ei tässä vaihdu koskaan, ilmaisin jäi laitteella kynnykselle (Pixel 8a
 * 20.10.2026). Siksi haun käynnistys kutsuu perään `animateToHidden`in.
 */
/**
 * Ruudun `Refresh` sen listoille vetoeleenä ([DgLazyColumn]in `onPull`in oletus). Ruutu
 * antaa sen kerran sisältönsä ympärille, koska yhdellä ruudulla voi olla monta listaa
 * (sivuruudulla kahdeksan sisältölajia), ja jokainen niistä hakee saman sivun. Anna se vain
 * sille osalle jonka yläpalkissa `Refresh` on näkyvissä, jotta ele ja nappi tekevät aina
 * saman eikä ele toimi siellä missä nappia ei ole.
 */
internal val LocalListPull = staticCompositionLocalOf<(() -> Unit)?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DgLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** Kutsujan oma tila kun ruutu vierittää listaa itse (Lounge 29.9.2026). */
    state: LazyListState = rememberLazyListState(),
    /** Veto alaspäin, sama teko kuin ruudun `Refresh`. Null: ei elettä. */
    onPull: (() -> Unit)? = LocalListPull.current
        .takeIf { fi.tommi.dg.app.session.Gesture.PULL_TO_REFRESH in LocalGestures.current },
    content: LazyListScope.() -> Unit,
) {
    // Valitun välilehden uusi napautus vie alkuun (`TabSwipe.kt`).
    ScrollToTopOnTabTap { state.animateScrollToItem(0) }
    if (onPull == null) {
        LazyColumn(
            modifier = modifier.dgReadingSurface(state),
            state = state,
            contentPadding = contentPadding,
            content = content,
        )
        return
    }
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    // Värähdys kun veto ylittää kynnyksen (20.10.2026): pelaaja tietää irrottaessaan että haku
    // lähtee, katsomatta ilmaisinta.
    val buzz = rememberGestureBuzz()
    LaunchedEffect(pullState, buzz) {
        snapshotFlow { pullState.distanceFraction >= 1f }
            .distinctUntilChanged()
            .collect { over -> if (over) buzz() }
    }
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = {
            onPull()
            scope.launch { pullState.animateToHidden() }
        },
        modifier = modifier,
        state = pullState,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().dgReadingSurface(state),
            state = state,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

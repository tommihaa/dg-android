package fi.tommi.dg.app.ui

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/*
 * Taustakuvat tyhjään tilaan: laitteen kansion kuvia sisällön alle jäävässä tilassa.
 *
 * **Mitä Tommi päätti 15.9.2026** (`docs/AVOIMET.md` › *Katutaide tyhjään tilaan*), ja mikä
 * tässä tiedostossa vastaa mihinkin:
 *
 * - *"vain luettelojen alaosa"*: kuvat piirretään samaan paikkaan kuin luolan seinä, eli
 *   vierivän sisällön alle jäävään vapaaseen tilaan (`DgFill.kt`). Otsikkorivi, kiinteät
 *   osat ja lauta ovat ulkona samalla mekanismilla kuin täytekuva.
 * - *"kuten desktop taustakuva"*: lähde on kansio jonka käyttäjä osoittaa kerran
 *   (`WallpaperStore`), ja kuvat arvotaan siitä. Kuratointi on kansion sisältö.
 * - *"jos tyhjää tilaa jää runsaasti, niin siihen voi skaalata 1-3 kuvaa"*: [dgWallpaperLayout]
 *   valitsee yhden, kaksi tai kolme sen mukaan mikä peittää vapaasta alueesta eniten, ja
 *   asettaa ne rinnakkain tai allekkain samalla perusteella.
 * - *"en halua että kuvia leikkautuu"*: kuva on aina kokonainen, pienennettynä kunnes mahtuu
 *   (sama kuin `ContentScale.Fit`). Ympärille jäävä tausta on hyväksytty hinta, ja kuvamäärä
 *   yhdestä kolmeen on se joka täyttää tilan leikkaamatta.
 * - *"tässä vaiheessa näkymän tahtiin"*: arpa on ruudun avauksen siemen, sama kuin taivaalla
 *   ja luolalla. Päivän tahti on toinen vaihe, kun näkymän tahti on todettu toimivaksi.
 *
 * **Lataus on kaksivaiheinen** ([rememberDgWallpaper]): kansion luettelo luetaan kerran
 * per kansio (`DocumentsContract`, satoja rivejä, ei kuvien avaamista), ja ruudun avaus
 * purkaa vain arvotut kuvat pienennettyinä ([DgWallpaperMaxSide]). Kansio on laitteella,
 * joten mikään tässä ei käy verkossa; pilvikansio toimisi valitsimessa mutta hakisi joka
 * avauksella, ja siksi suositus on laitteen oma kansio (Tommin huoli 15.9.2026 illalla).
 */

/**
 * Kuvat yhdelle ruudun avaukselle, purkujärjestyksessä, ja niiden nimet ilman päätettä.
 * Tyhjä lista kun purku on kesken.
 *
 * **Napautus näyttää nimen** (Tommin tilaus 15.9.2026 illalla): kuvan päällä napautus
 * merkitsee sen, ja nimi piirtyy laatikkoon kuvan yläpuolelle. Toinen napautus samaan kuvaan
 * tai napautus muualle tyhjään tilaan poistaa merkinnän. Osuma luetaan viimeksi piirretyistä
 * paikoista ([rects]), jotka piirto kirjoittaa, joten napautus ja kuva eivät voi erota.
 */
internal class DgWallpaper(val images: List<ImageBitmap>, val names: List<String> = emptyList()) {

    /** Merkityn kuvan indeksi, tai null. Tila, jotta piirto seuraa napautusta. */
    var labelled: Int? by mutableStateOf(null)

    /** Viimeksi piirretyt paikat piirtoalueen koordinaateissa, indeksi sama kuin [images]. */
    var rects: List<Rect> = emptyList()

    fun tap(at: Offset) {
        val hit = rects.indexOfFirst { it.contains(at) }
        labelled = if (hit < 0 || hit == labelled) null else hit
    }
}

/** Tiedostonimi ilman päätettä laatikkoon: `x.jpg` on `x`, ja päätteetön nimi jää ennalleen. */
internal fun dgWallpaperLabel(displayName: String): String {
    val dot = displayName.lastIndexOf('.')
    return if (dot > 0) displayName.substring(0, dot) else displayName
}

/** Taustakuvat tälle ruudulle, tai null kun kansiota ei ole valittu tai ruutu on lauta. */
internal val LocalDgWallpaper = compositionLocalOf<DgWallpaper?> { null }

/** Kuvien väli kun niitä on useampi. */
internal val DgWallpaperGap: Dp = 12.dp

/** Enintään näin monta kuvaa yhteen tilaan (Tommi: *"1-3 kuvaa"*). */
internal const val DgWallpaperMaxCount = 3

/**
 * Puretun kuvan pisin sivu enintään tässä. Lähteet ovat 1920 ja 3440 pikseliä leveitä ja
 * kuva piirtyy enintään ruudun levyisenä, joten suurempi olisi muistia ilman tarkkuutta.
 */
internal const val DgWallpaperMaxSide = 1600

/**
 * Kuvien paikat vapaalla alueella [width] × [height]. [ratios] on kuvien leveys jaettuna
 * korkeudella siinä järjestyksessä jossa ne on arvottu, ja tulos käyttää alusta niin monta
 * kuin kannattaa.
 *
 * Valinta on peitto: jokaiselle määrälle 1…3 lasketaan rinnakkain ja allekkain se yhteinen
 * mitta jolla kaikki mahtuvat kokonaisina, ja voittaja on se jonka kuvat peittävät alueesta
 * suurimman osuuden. Useampi kuva voittaa vain jos se peittää aidosti enemmän, joten
 * pieneen tilaan tulee yksi ja leveään kolme pystykuvaa rinnakkain. Ryhmä keskitetään.
 */
internal fun dgWallpaperLayout(width: Float, height: Float, ratios: List<Float>, gap: Float): List<Rect> {
    if (width <= 0f || height <= 0f || ratios.isEmpty()) return emptyList()
    var best: List<Rect> = emptyList()
    var bestCover = 0f
    val area = width * height
    for (count in 1..minOf(DgWallpaperMaxCount, ratios.size)) {
        val used = ratios.take(count)
        val gaps = gap * (count - 1)

        // Rinnakkain: yhteinen korkeus, leveydet suhteista.
        val rowHeight = minOf(height, (width - gaps) / used.sum())
        if (rowHeight > 0f) {
            val widths = used.map { it * rowHeight }
            val cover = widths.sum() * rowHeight / area
            if (cover > bestCover) {
                bestCover = cover
                val top = (height - rowHeight) / 2f
                var left = (width - widths.sum() - gaps) / 2f
                best = widths.map { w -> Rect(left, top, left + w, top + rowHeight).also { left += w + gap } }
            }
        }

        // Allekkain: yhteinen leveys, korkeudet suhteista.
        val columnWidth = minOf(width, (height - gaps) / used.sumOf { (1f / it).toDouble() }.toFloat())
        if (columnWidth > 0f) {
            val heights = used.map { columnWidth / it }
            val cover = heights.sum() * columnWidth / area
            if (cover > bestCover) {
                bestCover = cover
                val left = (width - columnWidth) / 2f
                var top = (height - heights.sum() - gaps) / 2f
                best = heights.map { h -> Rect(left, top, left + columnWidth, top + h).also { top += h + gap } }
            }
        }
    }
    return best
}

/**
 * Kansion nimi asetusruudulle uri-merkkijonosta: `content://…/tree/primary%3APictures%2Fdg`
 * antaa `Pictures/dg`. Muun tarjoajan (muistikortti, pilvi) tunniste jää näkyviin
 * sellaisenaan, koska sen muoto ei ole tiedossa eikä sitä kannata arvata.
 */
internal fun dgWallpaperFolderName(uri: String): String {
    val tree = uri.substringAfter("/tree/", "")
    if (tree.isEmpty()) return uri
    val decoded = runCatching { URLDecoder.decode(tree, "UTF-8") }.getOrDefault(tree)
    return decoded.removePrefix("primary:").ifEmpty { decoded }
}

/**
 * Kansion kuvat dokumentti-ureina. Pelkkä luettelo: yhtään kuvaa ei avata. Heittää kun
 * oikeus on mennyt, ja kutsupaikka päättää mitä silloin tehdään.
 */
internal fun listWallpaperImages(resolver: ContentResolver, folder: String): List<Pair<Uri, String>> {
    val tree = Uri.parse(folder)
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(
        tree, DocumentsContract.getTreeDocumentId(tree),
    )
    val out = mutableListOf<Pair<Uri, String>>()
    val columns = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    )
    resolver.query(children, columns, null, null, null)?.use { c ->
        while (c.moveToNext()) {
            val mime = c.getString(1) ?: continue
            if (mime.startsWith("image/")) {
                out += DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)) to
                    (c.getString(2) ?: "")
            }
        }
    } ?: error("no cursor for $folder")
    return out
}

/** Purkaa kuvan pienennettynä niin että pisin sivu on enintään noin [maxSide]. */
internal fun decodeWallpaper(resolver: ContentResolver, uri: Uri, maxSide: Int): ImageBitmap? {
    // Rajojen luku palauttaa aina nullin (`inJustDecodeBounds`), joten puuttuva virta
    // tarkistetaan erikseen eikä paluuarvosta. Ensimmäinen laiteajo 15.9.2026 osui tähän:
    // kaikki kuvat "puuttuivat" ja tila sai luolan seinän.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val probe = resolver.openInputStream(uri) ?: return null
    probe.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return resolver.openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it, null, options) }
        ?.asImageBitmap()
}

/**
 * Taustakuvat tälle ruudun avaukselle. [folder] on valittu kansio tai null, [seed] ruudun
 * arpa. Luettelo luetaan kerran per kansio, kuvat puretaan per siemen, molemmat IO-säikeessä.
 * Kun kansiota ei enää voi lukea, [onLost] kutsutaan kerran ja tulos on null.
 */
@Composable
internal fun rememberDgWallpaper(folder: String?, seed: Int, onLost: () -> Unit): DgWallpaper? {
    if (folder == null) return null
    val resolver = LocalContext.current.contentResolver
    var uris by remember(folder) { mutableStateOf<List<Pair<Uri, String>>?>(null) }
    var wallpaper by remember(folder) { mutableStateOf(DgWallpaper(emptyList())) }

    LaunchedEffect(folder) {
        val listed = withContext(Dispatchers.IO) {
            runCatching { listWallpaperImages(resolver, folder) }
        }
        listed.onSuccess { uris = it }.onFailure { onLost() }
    }

    LaunchedEffect(folder, seed, uris) {
        val all = uris ?: return@LaunchedEffect
        if (all.isEmpty()) return@LaunchedEffect
        val picked = all.shuffled(Random(seed)).take(DgWallpaperMaxCount)
        val decoded = withContext(Dispatchers.IO) {
            picked.mapNotNull { (uri, name) ->
                runCatching { decodeWallpaper(resolver, uri, DgWallpaperMaxSide) }.getOrNull()
                    ?.let { it to dgWallpaperLabel(name) }
            }
        }
        wallpaper = DgWallpaper(decoded.map { it.first }, decoded.map { it.second })
    }
    return wallpaper
}

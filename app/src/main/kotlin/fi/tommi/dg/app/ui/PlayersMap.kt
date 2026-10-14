package fi.tommi.dg.app.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import fi.tommi.dg.app.R
import fi.tommi.dg.scrape.MapPoint
import fi.tommi.dg.scrape.PlayersMapParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pelaajien ylläpitämä DailyGammon Players map (Tommin tilaus 9.10.2026). Yhteisön oma
 * lyhytosoite on `tiny.cc/DG_map`, mutta sitä ei käytetä: se ohjaa polkuun `/maps/d/viewer`,
 * jonka Androidin Maps-sovellus kaappaa, eikä Maps osaa avata My Maps -karttaa ("Kartan
 * avaaminen epäonnistui", mitattu tabletilla samana iltana). Polku `/maps/d/embed` ei ole
 * Mapsin, joten se aukeaa selaimeen ja näyttää saman kartan.
 *
 * Hinta: jos kartan ylläpitäjä vaihtaa lyhytosoitteen uuteen karttaan, tämä jää vanhaan.
 */
private const val PLAYERS_MAP_ID = "1BygIDS5hn_hrTuQG-c6tfm4YoyGGTQVU"

internal const val PLAYERS_MAP_URL = "https://www.google.com/maps/d/embed?mid=$PLAYERS_MAP_ID"

/** Kartan julkinen KML-vienti: merkkien nimet ja koordinaatit. Googlelta, ei DailyGammonilta. */
private const val PLAYERS_MAP_KML = "https://www.google.com/maps/d/kml?mid=$PLAYERS_MAP_ID&forcekml=1"

/** Kaupungin tarkkuus: naapurit näkyvät, mutta merkki ei huku maanosaan. */
private const val PLAYER_ZOOM = 8

/** Kartan osoite pelaajan kohdalle; `ll` ja `z` toimivat upotetussa kartassa (mitattu 9.10.2026). */
internal fun playersMapUrlAt(point: MapPoint): String =
    "$PLAYERS_MAP_URL&ll=${point.latitude},${point.longitude}&z=$PLAYER_ZOOM"

/**
 * Luettu KML prosessin muistissa. Haetaan vasta painalluksesta, koska App Help lupaa että
 * sovellus lukee vain avatut sivut (Tommin valinta 9.10.2026: linkki aina, haku painalluksesta).
 */
private object PlayersMapCache {
    @Volatile var kml: String? = null
}

private fun fetchKml(): String? = PlayersMapCache.kml ?: runCatching {
    val connection = URL(PLAYERS_MAP_KML).openConnection() as HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 15_000
    try {
        if (connection.responseCode != 200) null
        else connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    } finally {
        connection.disconnect()
    }
}.getOrNull()?.also { PlayersMapCache.kml = it }

/**
 * Palauttaa napautuksen käsittelijän, joka avaa kartan [player]in kohdalle. Ilman nimeä,
 * ilman merkkiä tai ilman yhteyttä kartta avautuu koko näkymään, ja lyhyt ilmoitus kertoo miksi.
 * Toinen napautus kesken haun ei tee mitään.
 */
@Composable
internal fun rememberPlayersMapOpener(player: String?): () -> Unit {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    return {
        if (!busy) {
            busy = true
            scope.launch {
                val name = player?.trim().orEmpty()
                val kml = if (name.isEmpty()) null else withContext(Dispatchers.IO) { fetchKml() }
                val point = kml?.let { PlayersMapParser.locate(it, name) }
                val note = when {
                    name.isEmpty() -> null
                    kml == null -> context.getString(R.string.players_map_unread)
                    point == null -> context.getString(R.string.players_map_not_on_map, name)
                    else -> null
                }
                note?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                uriHandler.openUri(point?.let(::playersMapUrlAt) ?: PLAYERS_MAP_URL)
                busy = false
            }
        }
    }
}

package fi.tommi.dg.scrape

import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/** Kartan piste asteina. KML kirjoittaa pituuden ensin, tämä leveyden ensin kuten osoite. */
data class MapPoint(val latitude: Double, val longitude: Double)

/**
 * Pelaajien ylläpitämän DailyGammon Players mapin KML-tiedosto (Tommin tilaus 9.10.2026).
 *
 * Tiedosto ei ole DailyGammonin sivu vaan Googlen My Maps -kartan julkinen vienti, jossa
 * jokainen merkki on `Placemark` ja sen `name` on pelaajan DG-nimi (mitattu: 235 merkkiä,
 * `tommih` mukana). Nimi verrataan kirjainkoosta välittämättä, koska merkin nimi on ylläpitäjän
 * käsin kirjoittama eikä sivuston tuottama.
 */
object PlayersMapParser {

    /** Pelaajan merkki, tai `null` jos nimeä ei ole kartalla tai koordinaatit eivät jäsenny. */
    fun locate(kml: String, player: String): MapPoint? {
        val wanted = player.trim()
        if (wanted.isEmpty()) return null
        val document = Jsoup.parse(kml, "", Parser.xmlParser())
        val mark = document.select("Placemark").firstOrNull {
            it.selectFirst("name")?.text()?.trim().equals(wanted, ignoreCase = true)
        } ?: return null
        // `pituus,leveys[,korkeus]`; Point-merkillä yksi kolmikko.
        val parts = mark.selectFirst("coordinates")?.text()?.trim()?.split(',') ?: return null
        val longitude = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
        val latitude = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
        return MapPoint(latitude, longitude)
    }
}

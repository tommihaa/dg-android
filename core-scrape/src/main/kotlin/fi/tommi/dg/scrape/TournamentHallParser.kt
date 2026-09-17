package fi.tommi.dg.scrape

import fi.tommi.dg.domain.HallTournament
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.TournamentHall
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Tournament Hallin jäsennin `/bg/thall`. Sivun mitattu rakenne on `docs/KOHDE.md`:ssä
 * (27.8.2026), lähteenä `raakasivut/tournament_hall.html`.
 *
 * Sama doktriini kuin [LoungeParser]illa: taulukot tunnistetaan captionista
 * (*Finished Tournaments*, *Active Tournaments*) eikä järjestyksestä, ja solut luetaan
 * turnauslinkin suhteen.
 *
 * **Taulukot ovat eri levyiset**, ja se on tämän sivun ainoa mutka: päättyneellä on
 * kolme saraketta (nimi, voittaja, päättymisaika) ja aktiivisella kaksi (nimi,
 * alkamisaika). Aika on siis molemmilla rivin viimeinen solu, mutta se ei tarkoita samaa
 * asiaa, ja siksi merkitys tulee listasta eikä kentästä.
 */
object TournamentHallParser {

    fun parse(html: String): TournamentHall? {
        val document = Jsoup.parse(html)
        if (!DgPages.isTournamentHall(document)) return null

        var active = emptyList<HallTournament>()
        var finished = emptyList<HallTournament>()
        document.select("table:has(caption)").forEach { table ->
            val caption = table.selectFirst("caption")?.text().orEmpty()
            when {
                caption.contains("Active Tournaments", ignoreCase = true) ->
                    active = table.select("tr").mapNotNull(::parseRow)
                caption.contains("Finished Tournaments", ignoreCase = true) ->
                    finished = table.select("tr").mapNotNull(::parseRow)
            }
        }

        return TournamentHall(active = active, finished = finished)
    }

    private fun parseRow(row: Element): HallTournament? {
        val cells = row.tableCells()
        val eventLink = row.selectFirst("a[href*=/bg/event/]") ?: return null // otsikkorivi
        val eventIndex = cells.indexOfFirst { it.selectFirst("a[href*=/bg/event/]") != null }
        if (eventIndex < 0) return null

        val winnerLink = row.selectFirst("a[href*=/bg/user/]")

        return HallTournament(
            name = eventLink.text().trim().takeIf { it.isNotEmpty() },
            eventId = SiteIds.eventId(eventLink.attr("href")),
            eventPath = eventLink.attr("href").takeIf { it.isNotBlank() },
            // Aika on rivin viimeinen solu molemmissa taulukoissa, mutta vain silloin kun
            // se ei ole turnaus- tai voittajasolu itse.
            dateText = cells.lastOrNull()
                ?.takeIf { cells.indexOf(it) > eventIndex && it.selectFirst("a") == null }
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            // Null kun riviltä puuttuu voittajalinkki, eli turnaus on yhä käynnissä.
            winner = winnerLink?.let {
                PlayerRef(
                    name = it.text().trim().takeIf { name -> name.isNotEmpty() },
                    userId = SiteIds.userId(it.attr("href")),
                    profilePath = it.attr("href").takeIf { path -> path.isNotBlank() },
                )
            },
        )
    }
}

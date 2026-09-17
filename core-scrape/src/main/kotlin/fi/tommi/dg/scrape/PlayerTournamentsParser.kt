package fi.tommi.dg.scrape

import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.PlayerTournamentRow
import fi.tommi.dg.domain.PlayerTournaments
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Pelaajan omien turnausten jäsennin: `/bg/userevent/<id>` ja `/bg/userwins/<id>`.
 * Sivujen mitattu rakenne on `docs/KOHDE.md`:ssä (27.8.2026), lähteinä
 * `raakasivut/user_events.html` ja `user_wins.html`.
 *
 * **Yksi jäsennin kahdelle sivulle**, koska ne ovat sama muoto eri sarakkeilla: molemmilla
 * on `<h2>`-otsikko jossa on pelaajan profiililinkki ja yksi taulukko jonka rivit
 * tunnistaa `/bg/event/`-linkistä. Käynnissä olevien sivulla on voittojen määrä ja
 * mahdollinen `Active Game` -linkki, voitettujen sivulla ajankohta.
 *
 * Sarakkeet luetaan turnauslinkin suhteen samasta syystä kuin muuallakin, ja tällä sivulla
 * syy on mitattu: `Active Game` puuttuu riviltä silloin kun ottelua ei ole käynnissä.
 * Linkki ei kerro vuorosta (laiteajo 15.9.2026, ks. `PlayerTournamentRow.activeMatchPath`).
 */
object PlayerTournamentsParser {

    /** Turnausrivi kummallakin sivulla. */
    const val EVENT_ROW = "tr:has(a[href~=^/bg/event/\\d+])"

    fun parse(html: String): PlayerTournaments? {
        val document = Jsoup.parse(html)
        if (!DgPages.isPlayerTournaments(document)) return null
        val heading = document.selectFirst("h2")
        val playerLink = heading?.selectFirst("a[href*=/bg/user/]")

        return PlayerTournaments(
            player = PlayerRef(
                name = playerLink?.text()?.trim()?.takeIf { it.isNotEmpty() },
                profilePath = playerLink?.attr("href")?.takeIf { it.isNotBlank() },
            ),
            rows = document.select(EVENT_ROW).mapNotNull(::parseRow),
        )
    }

    private fun parseRow(row: Element): PlayerTournamentRow? {
        val cells = row.tableCells()
        val eventIndex = cells.indexOfFirst { it.selectFirst("a[href*=/bg/event/]") != null }
        if (eventIndex < 0) return null
        val eventLink = cells[eventIndex].selectFirst("a[href*=/bg/event/]") ?: return null

        val matchLink = row.selectFirst("a[href*=/bg/game/]")
        // Turnauslinkin jälkeinen solu: käynnissä olevalla se on voittojen määrä,
        // voitetulla ajankohta. Kumpi se on, ratkeaa siitä onko solu luku.
        val afterEvent = cells.getOrNull(eventIndex + 1)
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return PlayerTournamentRow(
            name = eventLink.text().trim().takeIf { it.isNotEmpty() },
            eventId = SiteIds.eventId(eventLink.attr("href")),
            eventPath = eventLink.attr("href").takeIf { it.isNotBlank() },
            winsText = afterEvent?.takeIf { it.toIntOrNull() != null },
            activeMatchPath = matchLink?.attr("href")?.takeIf { it.isNotBlank() },
            whenText = afterEvent?.takeIf { it.toIntOrNull() == null },
        )
    }
}

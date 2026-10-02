package fi.tommi.dg.scrape

import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.TopPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Top Pagen jäsennin.
 *
 * Kohde on HTML 4 ilman id- ja class-attribuutteja, joten mihinkään ei voi tarttua nimellä.
 * Jäsennin nojaa siksi kahteen asiaan jotka sivulla oikeasti ovat pysyviä:
 *
 * 1. **Linkkien muoto.** `/bg/event/`, `/bg/user/`, `/bg/game/`, `/bg/move/` ovat sivuston
 *    omia reittejä. Sarake voi vaihtaa paikkaa, linkin muoto ei.
 * 2. **Taulukon caption.** Vuoro ei ole rivillä vaan taulukon otsikossa
 *    ("Matches where you can move:"), joten ryhmä ratkaisee eikä rivi.
 *
 * Sarakkeiden lukeminen indeksillä on välttämätön paha aika- ja kierrossarakkeille, koska
 * niissä ei ole linkkiä. Indeksointi tehdään suhteessa tapahtumasarakkeeseen eikä rivin
 * alkuun, jotta ylimääräinen sarake rivin alussa ei siirrä kaikkea.
 *
 * Rivi jolta puuttuu pelitunniste ohitetaan sen sijaan että jäsennin kaatuisi: yksi
 * tuntematon rivi ei saa viedä koko listaa.
 */
object TopPageParser {

    private val MATCH_ID = Regex("""/bg/(?:game|move)/(\d+)""")
    private val WELCOME = Regex("""Welcome to DailyGammon,\s*(.+?)\.?\s*$""")

    fun parse(html: String): TopPage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isTopPage(document)) return null

        val matches = document.select("table:has(caption)")
            .flatMap { table -> parseMatchTable(table) }

        // Oma käyttäjänumero erottuu vastustajien linkeistä kyselyparametrilla:
        // /bg/user/20311?days_to_view=30&active=1&finished=1
        val profileLink = document.selectFirst("a[href*=days_to_view]")

        return TopPage(
            user = PlayerRef(
                name = document.select("h2").firstNotNullOfOrNull { heading ->
                    WELCOME.find(heading.text())?.groupValues?.get(1)
                },
                userId = profileLink?.let { SiteIds.userId(it.attr("href")) },
                // Polku otetaan linkistä sellaisenaan, ei koota numerosta.
                profilePath = profileLink?.attr("href")?.takeIf { it.isNotBlank() },
            ),
            // Huom: tämä ei ole henkilöviestin merkki. Sama ilmoitus tulee myös
            // turnausuutisista, joten sisällön laji selviää vasta sivun haettua.
            //
            // Polku otetaan linkistä sellaisenaan samasta syystä kuin profiililla, ja
            // painavammin: tämä on kuluttava osoite, joten koottu osoite ei tuottaisi
            // virhettä vaan peruuttamattoman teon väärään kohteeseen.
            messageQueuePath = document.selectFirst("a[href*=/bg/nextgame]")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() },
            matches = matches,
            // Navigointipalkin linkit, arvo sellaisenaan. Ankkuri on linkin muoto eikä
            // vihreä taulukko tai linkkiteksti, samalla perusteella kuin /bg/event/.
            loungePath = document.selectFirst("a[href^=/bg/lounge]")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() },
            forumPath = document.selectFirst("a[href^=/bg/forum2]")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() },
            // Alalaidan "active tournaments" -linkki kantaa pelaajan oman tunnuksen.
            activeTournamentsPath = document.selectFirst("a[href*=/bg/userevent/]")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() },
        )
    }

    private fun parseMatchTable(table: Element): List<Match> {
        val caption = table.selectFirst("caption")?.text().orEmpty()
        if (!caption.contains("match", ignoreCase = true)) return emptyList()

        val myTurn = isCanMoveCaption(caption)

        return table.select("tr").mapNotNull { row -> parseRow(row, myTurn) }
    }

    /**
     * "Matches where you can move:" tarkoittaa vuoroa. Kiellettyä muotoilua ei ole nähty
     * datassa, mutta sitä varotaan silti, koska "cannot move" sisältää merkkijonon
     * "can move" osana itseään ja tuottaisi väärän positiivisen.
     */
    private fun isCanMoveCaption(caption: String): Boolean {
        val normalized = caption.lowercase()
        if (normalized.contains("cannot move") || normalized.contains("can not move")) return false
        return normalized.contains("can move")
    }

    private fun parseRow(row: Element, myTurn: Boolean): Match? {
        val cells = row.tableCells()
        if (cells.isEmpty()) return null // otsikkorivi

        val playLink = row.selectFirst("a[href*=/bg/move/]")
        val reviewLink = row.selectFirst("a[href*=/bg/game/]")

        val matchId = listOfNotNull(playLink, reviewLink)
            .firstNotNullOfOrNull { MATCH_ID.find(it.attr("href"))?.groupValues?.get(1) }
            ?: return null

        val eventLink = row.selectFirst("a[href*=/bg/event/]")
        val opponentLink = row.selectFirst("a[href*=/bg/user/]")

        // Tapahtumasarake on ankkuri jonka jälkeen tulevat aika- ja kierrossarakkeet.
        // Ilman tapahtumalinkkiä (esim. mahdollinen ystävyysottelu) oletetaan että
        // sarake on järjestysnumeron jälkeinen.
        val anchorIndex = cells.indexOfFirst { it.selectFirst("a[href*=/bg/event/]") != null }
            .takeIf { it >= 0 }
            ?: 1

        return Match(
            id = MatchId(matchId),
            eventName = eventLink?.text() ?: cells.getOrNull(anchorIndex)?.text().orEmpty(),
            eventId = eventLink?.let { SiteIds.eventId(it.attr("href")) },
            opponent = PlayerRef(
                name = opponentLink?.text()?.takeIf { it.isNotBlank() },
                userId = opponentLink?.let { SiteIds.userId(it.attr("href")) },
            ),
            myTurn = myTurn,
            graceText = cells.textAt(anchorIndex + 1),
            timePoolText = cells.textAt(anchorIndex + 2),
            round = cells.textAt(anchorIndex + 3),
            matchLength = cells.textAt(anchorIndex + 4)?.toIntOrNull(),
            playPath = playLink?.attr("href")?.takeIf { it.isNotBlank() },
            reviewPath = reviewLink?.attr("href")?.takeIf { it.isNotBlank() },
        )
    }
}

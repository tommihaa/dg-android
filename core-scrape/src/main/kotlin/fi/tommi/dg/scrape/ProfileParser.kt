package fi.tommi.dg.scrape

import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.ProfileField
import fi.tommi.dg.domain.ProfilePage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Profiilisivun jäsennin.
 *
 * Sivu on eri asia kuin Top Page, eikä `TopPageParser` kelpaa siihen. Kaksi eroa tekevät
 * siitä oman jäsentimensä eivätkä lippua olemassa olevaan:
 *
 * 1. **Sarakkeet eivät ole samat kahdessa taulukossa.** `Active games:` on
 *    `# · Event · Grace · Time Pool · Round · Length · Opponent`, kun taas
 *    `Finished matches:` jättää Gracen ja Time Poolin kokonaan pois. Top Pagen tapa laskea
 *    indeksi tapahtumasarakkeesta eteenpäin lukisi jälkimmäisestä kierroksen graceksi.
 * 2. **Vuorosta ei ole sanaa.** Top Pagella se on captionissa, täällä ei missään, joten
 *    `myTurn` jää nulliksi. Ks. `Match.myTurn`.
 *
 * Siksi sarakkeet luetaan **otsikkorivin nimistä**, ei paikoista. Se on mahdollista vain
 * täällä: Top Pagella otsikoita on vähemmän kuin sarakkeita, mutta profiilisivulla vain
 * otsikottomat Review ja Export ovat rivin lopussa, joten alkupää on kohdakkain.
 *
 * Jos otsikkoriviä ei löydy, rivi luetaan silti linkeistä ja nimetyt sarakkeet jäävät
 * nulleiksi. Puuttuva arvo on täällä parempi kuin arvattu: sama sääntö kuin lautasivun
 * pistenumeroissa.
 */
object ProfileParser {

    private val MATCH_ID = Regex("""/bg/(?:game|move)/(\d+)""")
    private val OWNER_ID = Regex("""/bg/user(?:event|wins)/(\d+)""")
    private val TITLE = Regex("""Info on player\s+(.+?)\s*$""")

    private const val ACTIVE_CAPTION = "active games"
    private const val FINISHED_CAPTION = "finished matches"

    fun parse(html: String): ProfilePage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isProfilePage(document)) return null

        return ProfilePage(
            player = PlayerRef(
                name = TITLE.find(document.title())?.groupValues?.get(1),
                // Profiilin kohteen numero luetaan linkeistä /bg/userevent/ ja /bg/userwins/.
                // Muoto /bg/user/<id> ei kelpaa: vastustajilla on sivulla samat linkit, ja
                // sivun omat lajittelulinkit ovat samassa muodossa kuin ne.
                userId = document.selectFirst("a[href*=/bg/userevent/], a[href*=/bg/userwins/]")
                    ?.let { OWNER_ID.find(it.attr("href"))?.groupValues?.get(1) },
            ),
            activeMatches = matchesIn(document, ACTIVE_CAPTION),
            finishedMatches = matchesIn(document, FINISHED_CAPTION),
            ratingText = labelledNumber(document, "Rating"),
            experienceText = labelledNumber(document, "Experience"),
            fields = fields(document),
            // Nämä kaksi ovat vain profiilisivulla, eikä niiden osoitetta koota
            // käyttäjänumerosta. Sama sääntö kuin loungen omilla linkeillä.
            activeTournamentsPath = document.selectFirst("a[href*=/bg/userevent/]")
                ?.attr("href")?.takeIf { it.isNotBlank() },
            tournamentWinsPath = document.selectFirst("a[href*=/bg/userwins/]")
                ?.attr("href")?.takeIf { it.isNotBlank() },
            // Sivun omat linkit sellaisinaan, ks. ProfilePage. Lajitteluista otetaan
            // sort_name jos se on tarjolla, muuten mikä tahansa: jo lajitellulta sivulta
            // puuttuu nykyisen lajittelun linkki (mitattu profile_page_games.html:sta).
            // Versus-linkki suodatetaan pois, koska siinäkin on sort-parametri mutta se
            // on eri kohde: suodatettu lista eikä koko pelilista.
            gamesPath = document.select("a[href*=sort_]")
                .map { it.attr("href") }
                .filter { it.isNotBlank() && !it.contains("versus=") }
                .let { paths ->
                    paths.firstOrNull { it.contains("sort_name") } ?: paths.firstOrNull()
                },
            // Lainausmerkit ovat pakolliset: arvon oma =-merkki katkaisisi valitsimen.
            versusPath = document.selectFirst("""a[href*="versus="]""")
                ?.attr("href")?.takeIf { it.isNotBlank() },
            // Sama lukija kuin jonon viestisivulla, ks. QuickMessageForms. Null omalla
            // profiililla, ja se on mitattu eikä oletettu: raakasivuista `user_profile.html`
            // ei sisällä osajonoa `sendmsg` kertaakaan, kun taas molemmat toisen pelaajan
            // sivut sisältävät sen kerran.
            messageForm = QuickMessageForms.read(document),
            // Kaksi muuta tekolomaketta samalta sivulta, 3.9.2026 alkaen (ProfileForms).
            inviteForm = ProfileForms.invite(document),
            ignoreForm = ProfileForms.ignore(document),
        )
    }

    /**
     * Esittelytaulukon rivit: rivi jolla on täsmälleen yksi `<th>` ja yksi `<td>`.
     *
     * Rakenteellinen ehto eikä nimilista, koska kentät riippuvat siitä mitä pelaaja on
     * täyttänyt: omalla profiililla on E-mail, toisella voi olla Home Page. Ottelutaulukot
     * eivät osu tähän, koska niiden otsikkorivillä on monta `<th>`:tä eikä yhtään `<td>`:tä.
     */
    private fun fields(document: Document): List<ProfileField> =
        document.select("tr").mapNotNull { row ->
            val headers = row.children().filter { it.normalName() == "th" }
            val cells = row.tableCells()
            if (headers.size != 1 || cells.size != 1) return@mapNotNull null

            val label = headers.first().text().trim().takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val text = cells.first().text().trim().takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            ProfileField(label = label, text = text)
        }

    /**
     * Rating ja Experience: pieni taulukko jossa nimi on omassa solussaan ja luku
     * seuraavassa. Luetaan nimen suhteen eikä paikasta, koska taulukossa ei ole otsikkoa
     * joka kertoisi kumpi rivi on kumpi.
     */
    private fun labelledNumber(document: Document, label: String): String? =
        document.select("td").firstOrNull { it.text().trim().equals(label, ignoreCase = true) }
            ?.nextElementSibling()
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun matchesIn(document: Document, caption: String): List<Match> =
        document.select("table:has(caption)")
            .filter { it.selectFirst("caption")?.text()?.lowercase()?.contains(caption) == true }
            .flatMap { table -> parseTable(table) }

    private fun parseTable(table: Element): List<Match> {
        val columns = headerColumns(table)
        return table.select("tr").mapNotNull { row -> parseRow(row, columns) }
    }

    /** Otsikon teksti pienellä -> sarakkeen indeksi. Tyhjä jos otsikkoriviä ei ole. */
    internal fun headerColumns(table: Element): Map<String, Int> {
        val headerRow = table.select("tr").firstOrNull { row ->
            row.children().any { it.normalName() == "th" }
        } ?: return emptyMap()

        return headerRow.children()
            .filter { it.normalName() == "th" }
            .withIndex()
            .associate { (index, cell) -> cell.text().trim().lowercase() to index }
    }

    internal fun parseRow(row: Element, columns: Map<String, Int>): Match? {
        val cells = row.tableCells()
        if (cells.isEmpty()) return null // otsikkorivi

        val reviewLink = row.selectFirst("a[href*=/bg/game/]")
        val playLink = row.selectFirst("a[href*=/bg/move/]")
        val exportLink = row.selectFirst("a[href*=/bg/export/]")

        val matchId = listOfNotNull(reviewLink, playLink)
            .firstNotNullOfOrNull { MATCH_ID.find(it.attr("href"))?.groupValues?.get(1) }
            ?: return null

        val eventLink = row.selectFirst("a[href*=/bg/event/]")
        val opponentLink = row.selectFirst("a[href*=/bg/user/]")

        return Match(
            id = MatchId(matchId),
            // Ystävyysottelulla ei ole tapahtumalinkkiä lainkaan, vaan pelkkä teksti
            // solussa. Nimetty sarake antaa senkin oikein, toisin kuin linkin puuttumisesta
            // päätelty indeksi.
            eventName = eventLink?.text() ?: cells.textAt(columns["event"]).orEmpty(),
            eventId = eventLink?.let { SiteIds.eventId(it.attr("href")) },
            eventPath = eventLink?.attr("href")?.takeIf { it.isNotBlank() },
            opponent = PlayerRef(
                name = opponentLink?.text()?.takeIf { it.isNotBlank() },
                userId = opponentLink?.let { SiteIds.userId(it.attr("href")) },
                profilePath = opponentLink?.attr("href")?.takeIf { it.isNotBlank() },
            ),
            // Sivu ei kerro vuoroa kummassakaan taulukossa.
            myTurn = null,
            graceText = cells.textAt(columns["grace"]),
            timePoolText = cells.textAt(columns["time pool"]),
            round = cells.textAt(columns["round"]),
            matchLength = cells.textAt(columns["length"])?.toIntOrNull(),
            playPath = playLink?.attr("href")?.takeIf { it.isNotBlank() },
            reviewPath = reviewLink?.attr("href")?.takeIf { it.isNotBlank() },
            exportPath = exportLink?.attr("href")?.takeIf { it.isNotBlank() },
        )
    }
}

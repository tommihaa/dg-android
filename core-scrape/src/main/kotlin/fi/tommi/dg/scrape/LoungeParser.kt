package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.LoungeInvitation
import fi.tommi.dg.domain.LoungePage
import fi.tommi.dg.domain.LoungeTournament
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.PlayerSearchForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Game Loungen jäsennin. Sivun mitattu rakenne on `docs/KOHDE.md`:ssä (26.8.2026),
 * lähteenä `raakasivut/sessio/0574_bg_lounge.html`.
 *
 * Sama doktriini kuin [TopPageParser]illa: kohde on HTML 4 ilman id- ja
 * class-attribuutteja, joten ryhmä tunnistetaan taulukon captionista ja solut luetaan
 * linkkisolun suhteen eikä rivin alusta tai lopusta. Jälkimmäinen ei ole tässä
 * varovaisuutta vaan mitattu pakko: turnausrivin perässä voi olla ylimääräinen
 * `Has Note` -solu, ja Sign Up -solu voi olla pelkkä `&nbsp;` tai `Cancel Signup` -linkki.
 *
 * Rivi jolta ankkurilinkki puuttuu ohitetaan sen sijaan että jäsennin kaatuisi.
 */
object LoungeParser {

    private val WELCOME = Regex("""Welcome to the DailyGammon waiting lounge,\s*(.+?)\.?\s*$""")

    fun parse(html: String): LoungePage? {
        val document = Jsoup.parse(html)
        if (!DgPages.isLoungePage(document)) return null

        var invitations = emptyList<LoungeInvitation>()
        var tournaments = emptyList<LoungeTournament>()
        document.select("table:has(caption)").forEach { table ->
            val caption = table.selectFirst("caption")?.text().orEmpty()
            when {
                caption.contains("waiting for opponents", ignoreCase = true) ->
                    invitations = table.select("tr").mapNotNull(::parseInvitation)
                caption.contains("Tournament Sign-Up", ignoreCase = true) ->
                    tournaments = table.select("tr").mapNotNull(::parseTournament)
            }
        }

        return LoungePage(
            userName = document.select("h2").firstNotNullOfOrNull { heading ->
                WELCOME.find(heading.text())?.groupValues?.get(1)
            },
            invitations = invitations,
            tournaments = tournaments,
            // Loungen alalaidan omat linkit.
            playerListPath = document.selectFirst("a[href^=/bg/plist]")?.attr("href")
                ?.takeIf { it.isNotBlank() },
            tournamentHallPath = document.selectFirst("a[href^=/bg/thall]")?.attr("href")
                ?.takeIf { it.isNotBlank() },
            // Hakulomake luetaan 3.9.2026 alkaen (Tommin tilaus). Aiemmin se jätettiin
            // lukematta koska lähettäminen oli oma päätöksensä; päätös on nyt tehty.
            searchForm = parseSearchForm(document),
        )
    }

    /**
     * Pelaajahaun lomake: `form[action=/bg/plist]`, tekstikenttä ja piilokentät sivulta.
     * Lomake jolla ei ole tekstikenttää ei ole haku, ja silloin palautetaan null eikä
     * vajaata lomaketta.
     */
    private fun parseSearchForm(document: org.jsoup.nodes.Document): PlayerSearchForm? {
        val form = document.selectFirst("form[action=/bg/plist]") ?: return null
        val field = form.selectFirst("input[type=text]") ?: return null
        val name = field.attr("name").takeIf { it.isNotBlank() } ?: return null
        return PlayerSearchForm(
            action = form.attr("action"),
            method = FormMethod.from(form.attr("method")),
            field = name,
            hidden = form.select("input[type=hidden]")
                .filter { it.attr("name").isNotBlank() }
                .associate { it.attr("name") to it.attr("value") },
            maxLength = field.attr("maxlength").toIntOrNull(),
        )
    }

    private fun parseInvitation(row: Element): LoungeInvitation? {
        val cells = row.tableCells()
        if (cells.isEmpty()) return null // otsikkorivi

        // Ankkuri on tarjoajan linkki eikä Join, koska Join puuttuu omalta tarjoukselta
        // mutta tarjoaja on rivillä aina.
        val playerLink = row.selectFirst("a[href*=/bg/user/]") ?: return null
        val playerIndex = cells.indexOfFirst { it.selectFirst("a[href*=/bg/user/]") != null }

        return LoungeInvitation(
            variant = cells.textAt(playerIndex - 2),
            length = cells.textAt(playerIndex - 1)?.toIntOrNull(),
            player = PlayerRef(
                name = playerLink.text().takeIf { it.isNotBlank() },
                userId = SiteIds.userId(playerLink.attr("href")),
                profilePath = playerLink.attr("href").takeIf { it.isNotBlank() },
            ),
            timeout = cells.textAt(playerIndex + 1),
            comment = cells.textAt(playerIndex + 2),
            // Polku sellaisenaan kyselyineen; kysely on palvelimen oma eikä sitä pureta.
            joinPath = row.selectFirst("a[href*=action=accept]")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() },
        )
    }

    private fun parseTournament(row: Element): LoungeTournament? {
        val cells = row.tableCells()
        if (cells.isEmpty()) return null // otsikkorivi

        // Ensimmäinen tapahtumalinkkisolu on nimisolu. `Has Note` -solussa on myös
        // /bg/event/-linkki, mutta se on rivin lopussa eikä voi olla ensimmäinen.
        val eventIndex = cells.indexOfFirst { it.selectFirst("a[href*=/bg/event/]") != null }
            .takeIf { it >= 0 }
            ?: return null
        val eventLink = cells[eventIndex].selectFirst("a[href*=/bg/event/]") ?: return null

        return LoungeTournament(
            name = eventLink.text().takeIf { it.isNotBlank() },
            eventId = SiteIds.eventId(eventLink.attr("href")),
            eventPath = eventLink.attr("href").takeIf { it.isNotBlank() },
            variant = cells.textAt(eventIndex + 1),
            length = cells.textAt(eventIndex + 2)?.toIntOrNull(),
            rounds = cells.textAt(eventIndex + 3)?.toIntOrNull(),
            timeText = cells.textAt(eventIndex + 4),
            incrementText = cells.textAt(eventIndex + 5),
            graceText = cells.textAt(eventIndex + 6),
            signupPath = row.selectFirst("a[href*=action=signup]")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() },
            // Jo ilmoittaudutulla rivillä Sign Upin tilalla on Cancel Signup (mitattu
            // 3.9.2026). Polku sellaisenaan kyselyineen kuten signupPath.
            cancelPath = row.selectFirst("a[href*=action=cancel]")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() },
            // Has Note on oma lisäsolunsa rivin perässä, ja sen linkki on sama
            // turnaussivu kuin nimisolun. Tunnistus tekstistä, koska linkki ei erota sitä.
            hasNote = cells.any { it.text().equals("Has Note", ignoreCase = true) },
        )
    }
}

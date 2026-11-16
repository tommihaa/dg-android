package fi.tommi.dg.net

import fi.tommi.dg.domain.Match
import fi.tommi.dg.scrape.ProfileParser
import fi.tommi.dg.scrape.TopPageParser
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Mittaa, kuluttaako katselulaudan (`/bg/game/<id>/`) haku jonoa, ja kaappaa sivun
 * jäsentimen kirjoittamista varten (8.11.2026, Tommin suunta A).
 *
 * Mittaus on sama kuin siirtolistalla: Top Page ennen ja jälkeen. Jos haku muuttaisi
 * jonoa, otteluiden joukko, vuorot tai Play-linkit eroaisivat.
 *
 * **Ottelu valitaan niin, ettei mittaus voi hävittää viestiä.** Ensisijainen on ottelu
 * ackammonia vastaan (Tommin oma testitili, lupa 29.9.2026). Muuten valitaan ottelu jossa
 * vuoro on vastustajalla: vastustajan viesti tulee hänen siirtonsa mukana, joten silloin
 * lukematonta viestiä ei ole.
 */
@Tag("live")
class ViewerCaptureLiveTest {

    @Test
    fun `katselulaudan haku ei muuta Top Pagea`() {
        val client = clientOrSkip()

        val topBefore = client.safeFetch("/bg/top")
        save("top_ennen.html", topBefore)
        val before = snapshot(topBefore)
        println("Top Page ennen: $before")

        val profilePath = Jsoup.parse(topBefore).selectFirst("a[href*=days_to_view]")?.attr("href")
        assumeTrue(profilePath != null, "Oman profiilin linkkiä ei löytynyt Top Pagelta")
        val profile = ProfileParser.parse(client.safeFetch(profilePath!!))
        assumeTrue(profile != null, "Profiilisivu ei jäsentynyt")
        val active = profile!!.activeMatches
        val myTurnIds = before.map { it.first }.toSet()

        val chosen: Match? = active.firstOrNull { it.opponent.name.equals("ackammon", ignoreCase = true) }
            ?: active.firstOrNull { it.id.value !in myTurnIds }
        assumeTrue(chosen != null, "Ei ackammon-ottelua eikä vastustajan vuorossa olevaa ottelua")
        println("Valittu ottelu: ${chosen!!.id} vastaan ${chosen.opponent.name}, oma vuoro: ${chosen.id.value in myTurnIds}")

        val viewerPath = "/bg/game/${chosen.id.value}/"
        val last = client.safeFetch(viewerPath)
        save("game_last.html", last)

        // Yksi askel taaksepäin, jotta myös tila jossa Next ja Last ovat linkkejä tulee talteen.
        val prev = Jsoup.parse(last).select("a").firstOrNull { it.text().trim() == "Prev" }?.attr("href")
        println("Prev-linkki: $prev")
        if (prev != null) save("game_prev.html", client.safeFetch(prev))
        val first = Jsoup.parse(last).select("a").firstOrNull { it.text().trim() == "First" }?.attr("href")
        println("First-linkki: $first")
        if (first != null) save("game_first.html", client.safeFetch(first))

        val topAfter = client.safeFetch("/bg/top")
        save("top_jalkeen.html", topAfter)
        val after = snapshot(topAfter)
        println("Top Page jälkeen: $after")
        assertEquals(before, after, "Top Pagen ottelut muuttuivat katselulaudan haun jälkeen")
    }


    /**
     * Sama mittaus ottelulle jossa vuoro on omalla kohdalla (8.11.2026 laiteajon jälkeen:
     * tällainen sivu ei jäsentynyt). Katselusivulla ei ole viestiä, joten haku ei voi
     * hävittää viestiä; Top Page ennen ja jälkeen kertoo kuluttaako se jonoa.
     */
    @Test
    fun `oman vuoron katselulauta ei muuta Top Pagea`() {
        val client = clientOrSkip()
        val topBefore = client.safeFetch("/bg/top")
        val before = snapshot(topBefore)
        val id = before.firstOrNull()?.first
        assumeTrue(id != null, "Ei ottelua omalla vuorolla")
        val html = client.safeFetch("/bg/game/$id/")
        save("game_own_turn.html", html)
        val after = snapshot(client.safeFetch("/bg/top"))
        assertEquals(before, after, "Top Pagen ottelut muuttuivat oman vuoron katselulaudan haun jälkeen")
    }


    /** Siirtolistan solun linkki, eli se polku jonka sovellus avaa (laiteajo 8.11.2026). */
    @Test
    fun `siirtolistan solulinkki kaapataan`() {
        val client = clientOrSkip()
        val top = TopPageParser.parse(client.safeFetch("/bg/top"))!!
        val review = top.matches.firstOrNull { it.eventName.contains("Pentamerous") }?.reviewPath
            ?: top.matches.firstNotNullOfOrNull { it.reviewPath }
        assumeTrue(review != null, "Ei Review-linkkiä")
        val log = fi.tommi.dg.scrape.MatchLogParser.parse(client.safeFetch(review!!))!!
        val path = log.games.first().turns[1].leftPath
        println("Solun polku: $path")
        assumeTrue(path != null, "Solulla ei linkkiä")
        save("game_from_list.html", client.safeFetch(path!!))
    }

    /** Ottelut, joissa on oma vuoro: tunniste ja Play-linkki. Järjestys ei ole osa mittausta. */
    private fun snapshot(html: String): List<Pair<String, String?>> =
        (TopPageParser.parse(html)?.matches ?: emptyList())
            .map { it.id.value to it.playPath }
            .sortedBy { it.first }

    private fun clientOrSkip(): DgClient {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        return DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )
    }

    private fun DgClient.safeFetch(path: String): String {
        val forbidden = CONSUMING.filter { path.contains(it, ignoreCase = true) }
        check(forbidden.isEmpty()) { "Polku on kuluttava (${forbidden.joinToString()}), tätä ei haeta: $path" }
        val response = fetch(path)
        assertTrue(response is DgResponse.Ok) { "Haku epäonnistui ($path): ${response::class.simpleName}" }
        return (response as DgResponse.Ok).html
    }

    private fun save(name: String, html: String) {
        val target = File("../raakasivut/katselulauta-8-11/$name")
        target.parentFile.mkdirs()
        target.writeText(html)
        println("Tallennettu: ${target.canonicalPath} (${html.length} merkkiä)")
    }

    private companion object {
        val CONSUMING = listOf("nextgame", "submit", "skip", "move=")
    }
}

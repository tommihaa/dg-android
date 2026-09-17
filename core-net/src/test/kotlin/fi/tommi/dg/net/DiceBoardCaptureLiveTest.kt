package fi.tommi.dg.net

import fi.tommi.dg.scrape.BoardParser
import fi.tommi.dg.scrape.TopPageParser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Kaappauslistan 5: lauta kun nopat on heitetty mutta siirto on tekemättä.
 *
 * Lista sanoo tästä "vain Tommi, heitto on kuluttava", ja se pitää paikkansa siitä
 * tilanteesta jossa nopat pitää heittää: `?submit=Roll+Dice` muuttaa oikeaa ottelua.
 * **Tämä testi ei heitä noppia.** Se etsii ottelun joka on jo valmiiksi siinä tilassa, ja
 * sellainen syntyy itsestään: sivusto heittää nopat puolesta silloin kun asetus
 * `Skip all automatic pages` on päällä, ja tila jää odottamaan siirtoa.
 *
 * Haku on siis pelkkää lukemista, mutta se koskee useaa sivua. Siksi määrä on rajattu ja
 * `DgClient`in minimiväli hoitaa tahdin.
 */
@Tag("live")
class DiceBoardCaptureLiveTest {

    @Test
    fun `lauta kaapataan kun nopat on jo heitetty`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        val playPaths = TopPageParser.parse(client.ok("/bg/top"))!!
            .matches
            .mapNotNull { it.playPath }
            .take(MAX_BOARDS)
        assumeTrue(playPaths.isNotEmpty(), "Yhdessäkään ottelussa ei ole vuoro")
        println("Käydään läpi ${playPaths.size} lautaa")

        // Tunnusmerkki EI ole noppien näkyminen: nopat näkyvät myös heittoa edeltävällä
        // sivulla, koska ne ovat edellisestä heitosta. Mitattu 1.8.2026, ja se ehti
        // tuottaa yhden väärän kaappauksen. Oikea merkki on **siirtolinkin olemassaolo**:
        // niitä on vain silloin kun nopat ovat omat ja siirto on vielä tekemättä.
        val found = playPaths.firstNotNullOfOrNull { path ->
            check(!path.contains("submit", ignoreCase = true)) { "Kuluttava polku: $path" }
            val html = client.ok(path)
            val board = BoardParser.parse(html)
            val dice = board?.dice.orEmpty()
            val moves = board?.moves.orEmpty()
            println("  $path: nopat=${dice.map { it.value }} siirtoja=${moves.size} kehote=${board?.prompt}")
            if (moves.isNotEmpty()) html to dice else null
        }

        assumeTrue(found != null) {
            "Yhdessäkään ottelussa nopat eivät ole valmiiksi heitettyinä. Tämä syntyy " +
                "itsestään ajan kanssa. Vaihtoehto on että Tommi heittää nopat itse ja " +
                "tallentaa sivun, koska heittoa ei tehdä puolesta."
        }

        val (html, dice) = found!!
        File("../raakasivut/move_nopat.html").writeText(html)
        println("Tallennettu: move_nopat.html (${html.length} merkkiä), nopat ${dice.map { it.value }}")
    }

    /**
     * Sivu jonka kehote ei ole kumpikaan tutuista. Tuntematon kehote on halvin merkki
     * tilasta jota jäsennin ei vielä tunne, eikä se maksa yhtään ylimääräistä pyyntöä.
     *
     * Kannatti heti: 1.8.2026 se osui sivuun jonka `<h4>` sanoi
     * `Klaus Schicks declines the cube action.` ja jonka yläreunassa luki
     * `Your opponent made an unexpected move, and the game has been rolled back to that
     * point.` Sama sivu on siis kaappauslistan 11, jota oli siihen asti vain kuvana, joten
     * tiedosto tallennetaan sillä nimellä.
     */
    @Test
    fun `lauta kaapataan kun kehote on tuntematon`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        val playPaths = TopPageParser.parse(client.ok("/bg/top"))!!
            .matches
            .mapNotNull { it.playPath }
            .take(MAX_BOARDS)
        assumeTrue(playPaths.isNotEmpty(), "Yhdessäkään ottelussa ei ole vuoro")

        val found = playPaths.firstNotNullOfOrNull { path ->
            check(!path.contains("submit", ignoreCase = true)) { "Kuluttava polku: $path" }
            val html = client.ok(path)
            val prompt = BoardParser.parse(html)?.prompt.orEmpty()
            if (prompt.isNotBlank() && KNOWN_PROMPTS.none { prompt.startsWith(it) }) {
                println("Tuntematon kehote: $prompt")
                html
            } else {
                null
            }
        }
        assumeTrue(found != null, "Kaikkien lautojen kehote oli tuttu")

        File("../raakasivut/rollback.html").writeText(found!!)
        println("Tallennettu: rollback.html (${found.length} merkkiä)")
    }

    private fun DgClient.ok(path: String): String {
        val response = fetch(path)
        assertTrue(response is DgResponse.Ok) { "Haku epäonnistui ($path): ${response::class.simpleName}" }
        return (response as DgResponse.Ok).html
    }

    private companion object {
        /** Kohteliaisuusraja: pieni sivusto, ja yksi pyyntö sekunnissa. */
        const val MAX_BOARDS = 10

        val KNOWN_PROMPTS = listOf(
            "Please select your action",
            "Please make a checker move",
        )
    }
}

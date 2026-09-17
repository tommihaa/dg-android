package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Fixture on typistetty aidosta sivusta (`raakasivut/game_list.html`, haettu 1.8.2026):
 * pelaajanimet on vaihdettu ja vuororiveistä on jäljellä otos, mutta jokainen rivimuoto on
 * sivulta sellaisenaan: avausvuoro (`colspan=2`-tyhjä), tanssi (tyhjä siirtolinkki),
 * kuutiotoimet omina `colspan=2`-soluinaan, pelin perässä irtonainen `<tr>`, ja
 * navigointitaulukot molemmissa päissä.
 */
class MatchLogParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val html = fixture("match_log.html")
    private val page = MatchLogParser.parse(html)!!

    @Test
    fun `sivu tunnistetaan valiotsikoista eika lautasivu kelpaa`() {
        assertTrue(DgPages.isMatchLogPage(html))
        // Lautasivun otsikko on samaa muotoa (`Match <id>, Move <n>`), joten juuri tämä
        // pari on se jonka takia tunnistus on rakenteessa eikä otsikossa.
        assertFalse(DgPages.isMatchLogPage(fixture("move_board.html")))
    }

    @Test
    fun `otsikko ja pituusrivi luetaan sivulta`() {
        assertEquals("March 26 Deja Vu, Round 3", page.heading)
        assertEquals("11 point match", page.matchLengthText)
    }

    @Test
    fun `pelit erottuvat valiotsikoista ja pistetilanne kulkee mukana`() {
        assertEquals(listOf("Game 1", "Game 2"), page.games.map { it.title })
        assertEquals("alpha : 0", page.games[0].scoreLeft)
        assertEquals("beta : 0", page.games[0].scoreRight)
        assertEquals("beta : 1", page.games[1].scoreRight)
    }

    @Test
    fun `avausvuorolla toinen puoli on tyhja`() {
        val avaus = page.games[0].turns.first()
        assertEquals("1)", avaus.number)
        assertEquals("", avaus.left)
        assertEquals("61: 13/7 8/7", avaus.right)
    }

    @Test
    fun `tanssi on heitto ilman siirtoa eika sita taydenneta`() {
        val tanssi = page.games[0].turns.first { it.number == "7)" }
        assertEquals("66:", tanssi.left)
        assertEquals("62: 24/18 9/7", tanssi.right)
    }

    @Test
    fun `kuutiotoimet osuvat oikealle puolelle colspanin mukaan`() {
        val tarjous = page.games[0].turns.first { it.number == "20)" }
        assertEquals("51: 20/15 2/1", tarjous.left)
        assertEquals("Doubles => 2", tarjous.right)

        val paatos = page.games[0].turns.first { it.number == "21)" }
        assertEquals("Drops", paatos.left)
        assertEquals("Wins 1 point", paatos.right)

        val otto = page.games[1].turns.first { it.number == "8)" }
        assertEquals("Doubles => 2", otto.left)
        assertEquals("Takes", otto.right)
    }

    @Test
    fun `irtorivit ja navigointitaulukot eivat tuota vuoroja`() {
        // Game 1: avaus, tanssi, tarjous, päätös. Irtonainen <tr> pelin perässä ja
        // navigointirivit eivät näy missään.
        assertEquals(4, page.games[0].turns.size)
        assertEquals(3, page.games[1].turns.size)
    }
}

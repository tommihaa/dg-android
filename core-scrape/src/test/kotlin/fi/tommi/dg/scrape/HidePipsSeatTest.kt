package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.Seat
import fi.tommi.dg.domain.resolveSeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Istuin kun sivuston asetus `Hide pip counts` on päällä (testaajapalaute 10.10.2026,
 * `docs/AVOIMET.md`).
 *
 * Testaajalla siirtonuolet ja noppien painallus eivät toimineet. Kaikki kolme nojaavat
 * paikalliseen kokoamiseen, ja kokoaminen alkoi vasta kun istuin ratkesi pip-luvuista. Asetus
 * poistaa paneelista vain rivin `117 pips` (sama ero on fixtureparissa `move_board.html` ja
 * `move_board_no_pips.html`), joten oman vuoron lauta ilman pip-lukuja tehdään tässä
 * poistamalla se rivi mitatusta laudasta.
 */
class HidePipsSeatTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).reader(Charsets.ISO_8859_1).readText()

    private fun parse(html: String) = checkNotNull(BoardParser.parse(html)) { "Lautaa ei jäsennetty" }

    private fun hidePips(html: String) = html.replace(Regex("<BR>\\d+ pips", RegexOption.IGNORE_CASE), "")

    @Test
    fun `oman vuoron lauta kokoaa paikallisesti ilman pip-lukuja`() {
        val withPips = parse(fixture("move_board.html"))
        val hidden = parse(hidePips(fixture("move_board.html")))

        // Lähtötilanne: pip-luvut todella puuttuvat, ja laudalla on omia askelia tarjolla.
        assertEquals(listOf(null, null), hidden.players.map { it.pips })
        assert(hidden.moves.isNotEmpty())

        assertEquals(withPips.resolveSeat(), hidden.resolveSeat())
        assertNotNull(LocalComposition.begin(hidden), "Kokoaminen putosi vanhaan reittiin")
    }

    @Test
    fun `mitattu lauta ilman pip-lukuja saa istuimen oman paneelin varista`() {
        // Vastustajan vuoro, joten omia noppia ei ole ja väri tulee pelkästä paneelista.
        val board = parse(fixture("move_board_no_pips.html"))
        assertEquals(listOf(null, null), board.players.map { it.pips })

        assertEquals(Seat(CheckerColor.YELLOW, homeIsLow = true), board.resolveSeat())
    }

    @Test
    fun `samanvariset paneelit jattavat istuimen ratkaisematta`() {
        // Oma paneeli vastustajan värillä on ristiriita, eikä sitä ratkaista arvaamalla.
        val html = hidePips(fixture("move_board.html"))
        val opponentColor = Regex("<TD VALIGN=top BGCOLOR=(#[0-9A-F]{6})>").find(html)!!.groupValues[1]
        val same = html.replace(Regex("<TD VALIGN=bottom BGCOLOR=#[0-9A-F]{6}>"), "<TD VALIGN=bottom BGCOLOR=$opponentColor>")

        assertNull(parse(same).resolveSeat())
    }
}

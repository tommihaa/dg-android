package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckerColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Heittoa edeltävä lautasivu, jolla on **nuolet** edellisestä siirrosta
 * (kaapattu 3.8.2026, fixture `move_board_arrows.html`).
 *
 * Sivu tuli mukaan sattumalta: se oli ainoa jonka runko oli vielä selaimen muistissa, kun
 * loput oli jo pudotettu. Se osoittautui silti kolmella tavalla erilaiseksi kuin aiemmat
 * lautafixturet, ja kukin niistä on oma testinsä alla.
 */
class BoardParserArrowTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val board = checkNotNull(BoardParser.parse(fixture("move_board_arrows.html"))) {
        "Lautasivu ei jäsentynyt lainkaan"
    }

    @Test
    fun `nuolet eivat paady laudalle pisteina`() {
        // Nuolikuvien ALT on "A", ja ne ovat samassa taulukossa kuin pisteet. Jos valinta
        // tehtaisiin ALTin perusteella eika kuvatiedoston nimen, "A" tulisi mukaan.
        // Piste 1-24 on kukin tasan kerran, ei enempaa.
        assertEquals(24, board.points.size)
        assertEquals((1..24).toList(), board.points.map { it.number })
    }

    @Test
    fun `pip-luku todistaa jasennyksen oikeaksi`() {
        // Sivun kaksi riippumatonta osaa vahvistavat toisensa: laudasta laskettu summa ja
        // pelaajapaneelin luku. Sininen laskee 25:sta alaspain, keltainen pisteen numerona.
        val blue = board.points.filter { it.owner == CheckerColor.BLUE }.sumOf { (25 - it.number) * it.count }
        val yellow = board.points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.number * it.count }

        assertEquals(156, blue)
        assertEquals(150, yellow)
        assertTrue(board.players.any { it.pips == blue })
        assertTrue(board.players.any { it.pips == yellow })
    }

    @Test
    fun `molempia vareja on 15 nappulaa`() {
        assertEquals(15, board.points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count })
        assertEquals(15, board.points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count })
    }

    @Test
    fun `nopat ovat edellisesta heitosta eivatka omat`() {
        // Kehote on heittoa edeltava ja lomake tarjoaa Roll Dicea, mutta laudalla on nopat.
        // Sama havainto kuin 1.8.2026: noppien olemassaolo ei kerro kenen vuoro on.
        assertEquals(listOf(5, 2), board.dice.map { it.value })
        assertTrue(board.moves.isEmpty(), "Siirtolinkkeja ei saa olla ennen heittoa")
        assertEquals("Please select your action/make your move:", board.prompt)
    }

    @Test
    fun `noppien vari kertoo kenen heitto on nakyvissa`() {
        // Vari ei ole ALTissa vaan kuvatiedoston nimessa (die_b5.gif). Tama on ainoa kohta
        // jossa tiedostonimi kantaa tietoa jota ALT ei kanna, ja siksi se on kirjattu.
        assertTrue(board.dice.all { it.owner == CheckerColor.BLUE })
    }

    @Test
    fun `korostusvari on sinisen pelaajan paneelissa`() {
        // ~~HYPOTEESI: vari seuraa edellista siirtajaa.~~ **Kumottu 31.8.2026 mittauksella.**
        // Sadasta kaapatusta laudasta: arvo on vakio ottelun sisalla kolmentoista tilan yli
        // molempien vuoroilla, eli se ei seuraa siirtajaa. Se on pelaajan vari: #3399CC on
        // sininen 132 yksiselitteisessa tapauksessa ilman yhtaan ristiriitaa, vertailuna
        // pisteluvuista paatelty vari. Ks. `PlayerPanel.checkerColor`.
        //
        // Tama otos sopii molempiin selityksiin, koska korostettu paneeli on sinisen ja
        // nopat ovat sinisen. Juuri siksi yksi otos ei riittanyt.
        val highlighted = board.players.filter { it.backgroundColor.equals("#3399CC", ignoreCase = true) }
        assertEquals(1, highlighted.size)
        assertEquals(156, highlighted.single().pips) // 156 = sinisen pip-luku, ks. ylla
    }

    @Test
    fun `lomakkeen tilatunniste on eri kuin sivun oma`() {
        // Sama saanto kuin muualla: lue linkki, ala laske sita. Sivu on tilassa 2183 ja
        // lomake osoittaa tilaan 2184.
        assertEquals("2184", board.stateToken)
    }

    @Test
    fun `kuutio ja ottelun pituus luetaan`() {
        assertEquals(1, board.cube?.value)
        assertEquals(21, board.matchLength)
        assertEquals("Round 3", board.round)
        assertEquals(930, board.moveNumber)
    }

    @Test
    fun `peruutusilmoitusta ei ole`() {
        assertNull(board.notices.firstOrNull { it.contains("rolled back", ignoreCase = true) })
    }
}

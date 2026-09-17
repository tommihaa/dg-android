package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.resolveSeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Pistelukujen tasapeli, mitattu lauta 1.9.2026 (`raakasivut/sessio-1-9`, ottelu 5315033).
 *
 * Molemmilla on 148 pippiä, ja se riitti kääntämään istuimen väärin päin: `selfPips`
 * täsmäsi sekä keltaiseen että siniseen, ja valinta osui ensimmäiseen haaraan. Vika oli
 * hiljainen kahdesti — istuin ei ollut ilmeisen väärä vaan uskottava, ja seuraus ei ollut
 * väärä siirto vaan hitaus: kokoaminen putosi vanhaan reittiin ja jokainen nappula maksoi
 * oman pyyntönsä.
 */
class PipTieSeatTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).reader(Charsets.ISO_8859_1).readText()

    private fun board(name: String) =
        checkNotNull(BoardParser.parse(fixture(name))) { "Lautaa ei jäsennetty: $name" }

    @Test
    fun `tasapelissa istuimen vari luetaan sivun nopista`() {
        val board = board("move_pip_tie.html")

        // Lähtötilanne on nimenomaan tasapeli: ilman sitä tämä testi ei testaisi mitään.
        assertEquals(listOf(148, 148), board.players.mapNotNull { it.pips })
        assertEquals(listOf(CheckerColor.BLUE), board.dice.map { it.owner }.distinct())

        val seat = checkNotNull(board.resolveSeat()) { "Istuin jäi ratkeamatta" }
        assertEquals(CheckerColor.BLUE, seat.color)
    }

    @Test
    fun `tasapelilauta kokoaa paikallisesti eika pudota vanhaan reittiin`() {
        // Ennen korjausta tämä oli null, ja se maksoi kaksi ylimääräistä pyyntöä vuorossa.
        assertNotNull(LocalComposition.begin(board("move_pip_tie.html")))
    }
}

package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Fixturet ovat aitoja katselusivuja (`raakasivut/katselulauta-8-11`, haettu 8.11.2026)
 * anonymisoituna: pelaajanimet, käyttäjänumerot ja ottelunumero on vaihdettu, muu on
 * sivulta sellaisenaan. Kolme tilaa: viimeinen asema (Next ja Last tekstinä), sitä edeltävä
 * (kaikki neljä linkkeinä) ja ensimmäinen (First ja Prev tekstinä).
 */
class GameViewerParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val last = GameViewerParser.parse(fixture("game_viewer_last.html"))!!
    private val prev = GameViewerParser.parse(fixture("game_viewer_prev.html"))!!
    private val first = GameViewerParser.parse(fixture("game_viewer_first.html"))!!

    @Test
    fun `navigointi luetaan linkeistä ja reunan teksti on null`() {
        assertEquals("/bg/game/7000071/0/1", last.firstPath)
        assertEquals("/bg/game/7000071/0/880", last.prevPath)
        assertNull(last.nextPath)
        assertNull(last.lastPath)

        assertEquals("/bg/game/7000071/0/879", prev.prevPath)
        assertEquals("/bg/game/7000071/0/881", prev.nextPath)
        assertEquals("/bg/game/7000071/0", prev.lastPath)

        assertNull(first.firstPath)
        assertNull(first.prevPath)
        assertEquals("/bg/game/7000071/0/2", first.nextPath)
    }

    @Test
    fun `siirtolistan polku on sivun oma`() {
        assertEquals("/bg/game/7000071/0/list", prev.listPath)
    }

    @Test
    fun `lauta luetaan samalla jäsentimellä kuin pelattava sivu`() {
        val board = prev.board
        assertEquals("7000071", board.matchId.value)
        assertEquals(880, board.moveNumber)
        assertEquals("The Marathon #4294", board.eventName)
        assertEquals("Round 3", board.round)
        assertEquals(21, board.matchLength)
        assertEquals(24, board.points.size)
        assertEquals(2, board.dice.size)
        assertEquals(listOf("pelaaja", "vastapelaaja"), board.players.map { it.player.name })
        assertTrue(board.moves.isEmpty())
        assertNull(board.form)
    }

    @Test
    fun `oman vuoron välitilan Move-linkki ei estä tunnistusta`() {
        // Laiteajo 8.11.2026: siirtolistalta avattu asema näytti "page this app cannot read",
        // koska sivulla oli `/ Move` -linkki pelattavaan sivuun.
        val page = GameViewerParser.parse(fixture("game_viewer_move_link.html"))!!
        assertEquals(4, page.board.moveNumber)
        assertEquals("/bg/game/7000072/0/5", page.nextPath)
        assertTrue(page.board.moves.isEmpty())
    }

    @Test
    fun `pelattava lautasivu ja siirtolista eivät ole katselusivuja`() {
        assertTrue(GameViewerParser.isViewerPage(fixture("game_viewer_first.html")))
        assertFalse(GameViewerParser.isViewerPage(fixture("move_board.html")))
        assertFalse(GameViewerParser.isViewerPage(fixture("match_log.html")))
        assertNull(GameViewerParser.parse(fixture("move_board.html")))
    }
}

/** Siirtolistan rivi kantaa kummankin puolen asemalinkin, josta katselulauta avataan. */
class MatchLogStateLinkTest {
    private val page = MatchLogParser.parse(
        checkNotNull(javaClass.getResourceAsStream("/fixtures/match_log.html")).reader().readText(),
    )!!

    @Test
    fun `puolen linkki on sen viimeinen solu ja tyhja puoli on null`() {
        val opening = page.games[0].turns[0]
        assertNull(opening.leftPath)
        assertEquals("/bg/game/5290918/1/2", opening.rightPath)

        // Tanssin tyhjä siirtolinkki on sivun oma asema.
        val dance = page.games[0].turns[1]
        assertEquals("/bg/game/5290918/1/24", dance.leftPath)
        assertEquals("/bg/game/5290918/1/26", dance.rightPath)

        val cube = page.games[0].turns[2]
        assertEquals("/bg/game/5290918/1/77", cube.rightPath)
    }
}

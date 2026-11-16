package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.CommandLink
import fi.tommi.dg.domain.CompositionSession
import fi.tommi.dg.domain.MatchId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Sivun `Swap Dice` ja `Skip Game` piiloon omilla kytkimillään (Tommin päätös 12.11.2026,
 * `docs/UI.md` › *Swap Dice ja Skip Game omille kytkimilleen*), ks. [boardShown].
 *
 * Suodatus koskee vain piirrettävää lautaa. Sivun muut komennot jäävät, koska kytkin nimeää
 * yhden napin eikä komentojen lajia.
 */
class BoardShownTest {

    private val swap = CommandLink("Swap Dice", "/bg/move/7000011/1?swap=1")
    private val localSwap = CommandLink(CompositionSession.SWAP_DICE, CompositionSession.LOCAL_SWAP)
    private val other = CommandLink("Something Else", "/bg/move/7000011/1?other=1")

    @Test
    fun `molemmat paalla lauta on sama olio`() {
        val board = lauta(listOf(swap, other))
        assertSame(board, boardShown(board, swapDiceShown = true, skipGameShown = true))
    }

    @Test
    fun `swap dice pois poistaa vain sen komennon`() {
        val shown = boardShown(lauta(listOf(swap, other)), swapDiceShown = false, skipGameShown = true)
        assertEquals(listOf(other), shown.commands)
        assertEquals("/bg/skip", shown.skipHref)
    }

    @Test
    fun `swap dice pois poistaa myos kokoamisen paikallisen vastineen`() {
        val shown = boardShown(lauta(listOf(localSwap)), swapDiceShown = false, skipGameShown = true)
        assertEquals(emptyList<CommandLink>(), shown.commands)
    }

    @Test
    fun `skip game pois poistaa vain ohituksen`() {
        val shown = boardShown(lauta(listOf(swap)), swapDiceShown = true, skipGameShown = false)
        assertNull(shown.skipHref)
        assertEquals(listOf(swap), shown.commands)
    }

    private fun lauta(commands: List<CommandLink>) = BoardState(
        matchId = MatchId("7000011"),
        moveNumber = null,
        stateToken = null,
        eventName = null,
        eventId = null,
        round = null,
        matchLength = null,
        cube = null,
        dice = emptyList(),
        scheme = BoardScheme.BLUE_WHITE,
        points = emptyList(),
        players = emptyList(),
        prompt = null,
        notices = emptyList(),
        rolledBack = false,
        speculative = false,
        moves = emptyList(),
        commands = commands,
        form = null,
        undoHref = null,
        borneOff = emptyList(),
        bar = emptyList(),
        skipHref = "/bg/skip",
    )
}

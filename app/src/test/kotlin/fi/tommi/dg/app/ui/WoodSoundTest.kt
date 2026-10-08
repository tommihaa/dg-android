package fi.tommi.dg.app.ui

import fi.tommi.dg.app.session.AppTheme
import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.app.session.enter
import fi.tommi.dg.app.session.leave
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.Die
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Wood-teema (Tommin tilaus 4.10.2026): mikä ääni soi laudan vaihtuessa ([woodSoundFor]) ja
 * mitä teeman vaihto kirjoittaa laudalle ja ilmaisimelle ([enter], [leave]).
 */
class WoodSoundTest {

    private val noppa = listOf(Die(3, CheckerColor.BLUE), Die(5, CheckerColor.BLUE))
    private val alku = listOf(Point(6, CheckerColor.BLUE, 5), Point(8, CheckerColor.BLUE, 3))
    private val siirretty = listOf(
        Point(3, CheckerColor.BLUE, 1),
        Point(6, CheckerColor.BLUE, 4),
        Point(8, CheckerColor.BLUE, 3),
    )

    @Test
    fun `ensimmainen lauta on hiljainen`() {
        assertNull(woodSoundFor(null, lauta(dice = noppa)))
    }

    @Test
    fun `toisen ottelun lauta on hiljainen`() {
        assertNull(woodSoundFor(lauta(), lauta(id = "7000012", dice = noppa)))
    }

    @Test
    fun `uusi heitto kolahtaa noppana`() {
        assertEquals(WoodSound.DICE, woodSoundFor(lauta(), lauta(dice = noppa)))
    }

    @Test
    fun `askel kolahtaa nappulana eika noppana vaikka kaytetty osa muuttuu`() {
        val ennen = lauta(dice = noppa)
        val jalkeen = lauta(dice = listOf(noppa[0].copy(spent = 1f), noppa[1]), points = siirretty)
        assertEquals(WoodSound.CHECKER, woodSoundFor(ennen, jalkeen))
    }

    @Test
    fun `paikallinen askel kolahtaa nappulana vaikka lauta ei muutu`() {
        val lauta = lauta(dice = noppa)
        assertEquals(WoodSound.CHECKER, woodSoundFor(lauta, lauta, prevSteps = 0, nextSteps = 1))
    }

    @Test
    fun `peruminen on hiljainen`() {
        val lauta = lauta(dice = noppa)
        assertNull(woodSoundFor(lauta, lauta, prevSteps = 2, nextSteps = 0))
    }

    @Test
    fun `uudet nopat voittavat nappulat`() {
        assertEquals(WoodSound.DICE, woodSoundFor(lauta(), lauta(dice = noppa, points = siirretty)))
    }

    @Test
    fun `sama lauta uudestaan on hiljainen`() {
        assertNull(woodSoundFor(lauta(dice = noppa), lauta(dice = noppa)))
    }

    @Test
    fun `kuution napit kolahtavat`() {
        assertEquals(setOf("Double", "Accept", "Beaver!", "Accept Beaver"), WOOD_CUBE_SUBMITS)
    }

    @Test
    fun `wood pitaa valitun puulaudan ja vaihtaa muun pahkinaksi`() {
        assertEquals(BoardStyle.OLIVE to false, enter(AppTheme.WOOD, BoardStyle.OLIVE, busyDeco = true))
        assertEquals(BoardStyle.WALNUT to false, enter(AppTheme.WOOD, BoardStyle.X22, busyDeco = false))
    }

    @Test
    fun `woodista poistuminen palauttaa laudan ja viimeistelyn`() {
        val before = BoardStyle.X22 to true
        assertEquals(before, leave(AppTheme.WOOD, BoardStyle.WALNUT, busyDeco = false, before))
        // Pelaajan oma ei-puinen lauta teeman aikana jää voimaan.
        assertEquals(BoardStyle.SITE to true, leave(AppTheme.WOOD, BoardStyle.SITE, busyDeco = false, before))
    }

    @Test
    fun `decosta suoraan woodiin kulkee plainin kautta`() {
        val before = BoardStyle.MAPLE to false
        val left = leave(AppTheme.DECO, BoardStyle.DECO, busyDeco = true, before)
        assertEquals(BoardStyle.MAPLE to false, left)
        assertEquals(BoardStyle.MAPLE to false, enter(AppTheme.WOOD, left.first, left.second))
    }

    private fun lauta(
        id: String = "7000011",
        dice: List<Die> = emptyList(),
        points: List<Point> = alku,
    ) = BoardState(
        matchId = MatchId(id),
        moveNumber = null,
        stateToken = null,
        eventName = null,
        eventId = null,
        round = null,
        matchLength = null,
        cube = null,
        dice = dice,
        scheme = BoardScheme.BLUE_WHITE,
        points = points,
        players = emptyList(),
        prompt = null,
        notices = emptyList(),
        rolledBack = false,
        speculative = false,
        moves = emptyList(),
        commands = emptyList(),
        form = null,
        undoHref = null,
        borneOff = emptyList(),
        bar = emptyList(),
    )
}

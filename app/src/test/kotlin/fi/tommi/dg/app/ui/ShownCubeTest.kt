package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.BoardForm
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.Cube
import fi.tommi.dg.domain.CubePosition
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.MatchId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tarjottu kuutio näyttää tarjotun arvon (Tommin tilaus 8.9.2026), ks. [shownCube].
 *
 * Sivu ei käännä kuutiota ennen hyväksyntää (`cube1.gif` tarjouksen aikana), joten arvo on
 * sovelluksen oma johtopäätös lomakkeen `Accept`-napista.
 */
class ShownCubeTest {

    private val middleOne = Cube(label = "1", value = 1, position = CubePosition.MIDDLE)

    @Test
    fun `tarjottu ykkonen nayttaa kakkosen`() {
        val shown = shownCube(lauta(cube = middleOne, submits = listOf("Accept", "Decline")))
        assertEquals(2, shown?.value)
        assertEquals("2", shown?.label)
        assertEquals(CubePosition.MIDDLE, shown?.position)
    }

    @Test
    fun `uudelleentuplaus kakkosesta nayttaa nelosen`() {
        val owned = Cube(label = "2", value = 2, position = CubePosition.TOP)
        assertEquals(4, shownCube(lauta(cube = owned, submits = listOf("Accept", "Decline")))?.value)
    }

    @Test
    fun `peruutettu sivu ei muuta tarjousta`() {
        // Tony 77 tuplasi peruutetulla sivulla 8.9.2026 (`sessio-8-9-ilta/0104`).
        val shown = shownCube(lauta(cube = middleOne, submits = listOf("Accept", "Decline"), rolledBack = true))
        assertEquals(2, shown?.value)
    }

    @Test
    fun `ilman tarjousta kuutio on sivun arvossa`() {
        assertEquals(middleOne, shownCube(lauta(cube = middleOne, submits = listOf("Roll Dice", "Double"))))
        assertEquals(middleOne, shownCube(lauta(cube = middleOne, submits = null)))
    }

    @Test
    fun `dr ja tuntematon arvo jaavat sellaisenaan`() {
        val dr = Cube(label = "dr", value = null, doubleRepeat = true, position = CubePosition.MIDDLE)
        assertEquals(dr, shownCube(lauta(cube = dr, submits = listOf("Accept", "Decline"))))
        val odd = Cube(label = "cubex", value = null, position = CubePosition.MIDDLE)
        assertEquals(odd, shownCube(lauta(cube = odd, submits = listOf("Accept", "Decline"))))
    }

    @Test
    fun `crawford-sivulla ei ole kuutiota`() {
        assertNull(shownCube(lauta(cube = null, submits = listOf("Accept", "Decline"))))
    }

    private fun lauta(cube: Cube?, submits: List<String>?, rolledBack: Boolean = false) = BoardState(
        matchId = MatchId("7000011"),
        moveNumber = null,
        stateToken = null,
        eventName = null,
        eventId = null,
        round = null,
        matchLength = null,
        cube = cube,
        dice = emptyList(),
        scheme = BoardScheme.BLUE_WHITE,
        points = emptyList(),
        players = emptyList(),
        prompt = null,
        notices = emptyList(),
        rolledBack = rolledBack,
        speculative = false,
        moves = emptyList(),
        commands = emptyList(),
        form = submits?.let {
            BoardForm(action = "/bg/move/7000011/1", method = FormMethod.GET, pendingMove = null, submits = it, verify = "Accept")
        },
        undoHref = null,
        borneOff = emptyList(),
        bar = emptyList(),
    )
}

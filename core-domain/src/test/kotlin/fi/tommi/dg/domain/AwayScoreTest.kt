package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Away-notaation johdos, eli [BoardState.awayOf].
 *
 * **Testattava on rajatapaus eikä laskutoimitus.** Vähennyslasku ei voi mennä väärin, mutta
 * kolme `null`-tapausta voivat: rahapelillä ei ole ottelupituutta, pistekenttä voi jäädä
 * lukematta, ja epäkelvon lukeman on parempi kadota ruudulta kuin näkyä lukuna joka väittää
 * jotain. Juuri ne kolme ovat ne joissa ruutu näyttäisi vastauksen silloinkin kun sitä ei ole.
 */
class AwayScoreTest {

    @Test
    fun `puuttuvat pisteet johdetaan ottelupituudesta ja saaduista`() {
        val lauta = lauta(matchLength = 5, pisteet = listOf(1, 3))
        assertEquals(4, lauta.awayOf(lauta.players[0]))
        assertEquals(2, lauta.awayOf(lauta.players[1]))
    }

    @Test
    fun `rahapelissa awayta ei ole`() {
        val lauta = lauta(matchLength = null, pisteet = listOf(1, 3))
        assertNull(lauta.awayOf(lauta.players[0]))
    }

    @Test
    fun `lukematon pistekentta ei tuota lukua`() {
        val lauta = lauta(matchLength = 5, pisteet = listOf(null))
        assertNull(lauta.awayOf(lauta.players[0]))
    }

    @Test
    fun `nolla tai sen alle jaa nayttamatta`() {
        val lauta = lauta(matchLength = 5, pisteet = listOf(5, 6))
        assertNull(lauta.awayOf(lauta.players[0]))
        assertNull(lauta.awayOf(lauta.players[1]))
    }

    private fun lauta(matchLength: Int?, pisteet: List<Int?>) = BoardState(
        matchId = MatchId("7000011"),
        moveNumber = null,
        stateToken = null,
        eventName = null,
        eventId = null,
        round = null,
        matchLength = matchLength,
        cube = null,
        dice = emptyList(),
        scheme = BoardScheme.BLUE_WHITE,
        points = emptyList(),
        players = pisteet.mapIndexed { i, piste ->
            PlayerPanel(
                player = PlayerRef(name = "pelaaja$i"),
                pips = null,
                score = piste,
                scoreLabel = piste?.toString(),
                backgroundColor = null,
            )
        },
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

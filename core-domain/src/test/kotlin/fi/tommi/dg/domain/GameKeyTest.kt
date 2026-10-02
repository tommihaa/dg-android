package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Pelin tunniste, eli [BoardState.gameKey].
 *
 * **Testattava on tunnisteen ehto eikä tietue.** Kolmen kentän kokoaminen ei voi mennä
 * väärin, mutta se milloin tunnistetta **ei ole** voi: puuttuva piste tarkoittaa ettei peliä
 * tunneta, ja jos se joskus muuttuu arvaukseksi, muistutus ilmestyisi väärään peliin. Samoin
 * paneelien järjestys on merkitsevä, koska `2-1` ja `1-2` ovat eri pelit, ja järjestys on
 * sivun oma havainto eikä sopimus.
 */
class GameKeyTest {

    @Test
    fun `tunniste on ottelu ja pistepari`() {
        val avain = lauta(pisteet = listOf(8, 7)).gameKey
        assertEquals(GameKey(MatchId(OTTELU), opponentScore = 8, selfScore = 7), avain)
    }

    @Test
    fun `pisteparin jarjestys erottaa kaksi eri pelia`() {
        // Sivun ensimmäinen paneeli on vastustaja ja toinen kirjautunut. Jos järjestys
        // joskus kääntyy, se näkyy tässä eikä muistutuksen katoamisena laitteella.
        assertNotEquals(lauta(pisteet = listOf(2, 1)).gameKey, lauta(pisteet = listOf(1, 2)).gameKey)
    }

    @Test
    fun `pisteiden muuttuminen on uusi peli`() {
        val ennen = lauta(pisteet = listOf(8, 7)).gameKey
        val jalkeen = lauta(pisteet = listOf(8, 8)).gameKey
        // Tämä ero on koko elinkaari: kysely osuu toisiin riveihin, ja edellisen pelin
        // muistutukset lakkaavat näkymästä ilman että mitään poistetaan.
        assertNotEquals(ennen, jalkeen)
    }

    @Test
    fun `puuttuva piste tarkoittaa ettei tunnistetta ole`() {
        // Rahapeli ja muu sivu joka ei kerro pisteitä. Arvattu tunniste olisi tässä pahempi
        // kuin puuttuva: se näyttäisi muistutuksen väärässä pelissä.
        assertNull(lauta(pisteet = listOf(8, null)).gameKey)
        assertNull(lauta(pisteet = listOf(null, 7)).gameKey)
    }

    @Test
    fun `paneeliton lauta ei tuota tunnistetta`() {
        assertNull(lauta(pisteet = emptyList()).gameKey)
        assertNull(lauta(pisteet = listOf(8)).gameKey)
    }

    private fun lauta(pisteet: List<Int?>) = BoardState(
        matchId = MatchId(OTTELU),
        moveNumber = null,
        stateToken = null,
        eventName = null,
        eventId = null,
        round = null,
        matchLength = 9,
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

    private companion object {
        const val OTTELU = "5302842"
    }
}

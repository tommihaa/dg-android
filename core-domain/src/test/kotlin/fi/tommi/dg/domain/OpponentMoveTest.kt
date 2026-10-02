package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Vastustajan edellinen siirto: siirtolistan merkintä, oikean puolen löytäminen ja nuolten
 * paikat laudalla joka on jo siirron jälkeen. Merkinnän muodot ovat aidosta sivusta
 * (`raakasivut/game_list.html`), ks. [LoggedStep].
 */
class OpponentMoveTest {

    private val keltainenMatalalla = Seat(CheckerColor.YELLOW, homeIsLow = true)

    /** Lauta jolla vain vastustajan (sininen) nappulat ovat merkitseviä. */
    private fun lauta(
        sininen: Map<Int, Int>,
        muurilla: Int = 0,
        ulkona: Int = 0,
        vari: CheckerColor = CheckerColor.BLUE,
    ) = BoardState(
        matchId = MatchId("7000011"),
        moveNumber = null,
        stateToken = "123",
        eventName = null,
        eventId = null,
        round = null,
        matchLength = 7,
        cube = null,
        dice = emptyList(),
        scheme = BoardScheme.BLUE_WHITE,
        points = (1..24).map { Point(it, if (sininen.containsKey(it)) vari else null, sininen[it] ?: 0) },
        players = emptyList(),
        prompt = null,
        notices = emptyList(),
        rolledBack = false,
        speculative = false,
        moves = emptyList(),
        commands = emptyList(),
        form = null,
        undoHref = null,
        borneOff = if (ulkona > 0) listOf(BorneOffCheckers(vari, ulkona)) else emptyList(),
        bar = if (muurilla > 0) listOf(BarCheckers(vari, muurilla)) else emptyList(),
    )

    private fun loki(vararg vuorot: Pair<String, String>, vasen: String = "alpha : 0", oikea: String = "beta : 1") =
        MatchLogPage(
            heading = "",
            matchLengthText = null,
            games = listOf(
                MatchLogGame(
                    title = "Game 1",
                    scoreLeft = vasen,
                    scoreRight = oikea,
                    turns = vuorot.mapIndexed { i, (l, r) -> MatchLogTurn("${i + 1})", l, r) },
                ),
            ),
        )

    @Test
    fun `merkinta puretaan askeliksi, lyonti muuri ja uloskanto mukaan lukien`() {
        assertEquals(
            listOf(LoggedStep(24, 18, false), LoggedStep(9, 7, false)),
            parseLoggedSteps("62: 24/18 9/7"),
        )
        assertEquals(
            listOf(LoggedStep(25, 22, true), LoggedStep(14, 9, true)),
            parseLoggedSteps("35: 25/22* 14/9*"),
        )
        assertEquals(
            listOf(LoggedStep(7, 4, false), LoggedStep(6, 0, false)),
            parseLoggedSteps("63: 7/4 6/0"),
        )
        assertEquals(4, parseLoggedSteps("33: 13/10 10/7 8/5 8/5").size)
    }

    @Test
    fun `heitto tanssi ja kuutiotoimi eivat ole askeleita`() {
        assertEquals(emptyList<LoggedStep>(), parseLoggedSteps("66:"))
        assertEquals(emptyList<LoggedStep>(), parseLoggedSteps("Doubles => 2"))
        assertEquals(emptyList<LoggedStep>(), parseLoggedSteps("Takes"))
    }

    @Test
    fun `viimeinen siirto luetaan nimen puolelta`() {
        val log = loki(
            "" to "21: 24/23 13/11",
            "43: 13/9 13/10" to "55: 13/8 13/8 8/3 8/3",
        )
        assertEquals(4, log.lastMoveOf("beta")?.size)
        assertEquals(4, log.lastMoveOf("BETA")?.size)
        // Alphan siirto ei ole listan viimeinen, joten se ei ole "edellinen siirto".
        assertNull(log.lastMoveOf("alpha"))
        assertNull(log.lastMoveOf("gamma"))
    }

    @Test
    fun `oma heitto siirron perassa ei peita vastustajan siirtoa`() {
        val log = loki(
            "" to "21: 24/23 13/11",
            "43:" to "",
        )
        assertEquals(listOf(LoggedStep(24, 23, false), LoggedStep(13, 11, false)), log.lastMoveOf("beta"))
    }

    @Test
    fun `kuutiotoimi tai tanssi viimeisena ei anna siirtoa`() {
        assertNull(loki("" to "21: 24/23 13/11", "43: 13/9 13/10" to "Doubles => 2").lastMoveOf("beta"))
        assertNull(loki("" to "21: 24/23 13/11", "43: 13/9 13/10" to "66:").lastMoveOf("beta"))
    }

    @Test
    fun `nuolet puretaan siirron jalkeisesta laudasta`() {
        // Sininen pelasi 24/18 13/11 eli sivulla 1→7 ja 12→14.
        val board = lauta(mapOf(1 to 1, 7 to 1, 12 to 4, 14 to 1, 17 to 3, 19 to 5))
        val steps = listOf(LoggedStep(24, 18, false), LoggedStep(13, 11, false))
        assertEquals(
            listOf(
                MoveArrow(ArrowEnd.OnPoint(1, 1), ArrowEnd.OnPoint(7, 0)),
                MoveArrow(ArrowEnd.OnPoint(12, 4), ArrowEnd.OnPoint(14, 0)),
            ),
            opponentArrows(board, keltainenMatalalla, steps),
        )
    }

    @Test
    fun `tuplan ketju jatkuu siita mihin edellinen nuoli paattyi`() {
        val board = lauta(mapOf(12 to 4, 18 to 1))
        val steps = listOf(LoggedStep(13, 10, false), LoggedStep(10, 7, false))
        assertEquals(
            listOf(
                MoveArrow(ArrowEnd.OnPoint(12, 4), ArrowEnd.OnPoint(15, 0)),
                MoveArrow(ArrowEnd.OnPoint(15, 0), ArrowEnd.OnPoint(18, 0)),
            ),
            opponentArrows(board, keltainenMatalalla, steps),
        )
    }

    @Test
    fun `muurilta tulo ja uloskanto`() {
        assertEquals(
            listOf(MoveArrow(ArrowEnd.OnBar(0), ArrowEnd.OnPoint(3, 0))),
            opponentArrows(lauta(mapOf(3 to 1)), keltainenMatalalla, listOf(LoggedStep(25, 22, true))),
        )
        assertEquals(
            listOf(MoveArrow(ArrowEnd.OnPoint(19, 2), ArrowEnd.Off)),
            opponentArrows(lauta(mapOf(19 to 2), ulkona = 1), keltainenMatalalla, listOf(LoggedStep(6, 0, false))),
        )
    }

    @Test
    fun `lista ja lauta eri hetkista ei piirra mitaan`() {
        // Kohteessa 14 ei ole sinistä, joten lista ei kuvaa tätä lautaa.
        val board = lauta(mapOf(1 to 2, 12 to 5))
        assertNull(opponentArrows(board, keltainenMatalalla, listOf(LoggedStep(13, 11, false))))
    }

    @Test
    fun `kaannetty istuin kayttaa pisteita sellaisenaan`() {
        // Oma koti korkeissa numeroissa: vastustajan piste on sama kuin sivun.
        val seat = Seat(CheckerColor.BLUE, homeIsLow = false)
        val board = lauta(mapOf(18 to 1, 24 to 1), vari = CheckerColor.YELLOW)
        assertEquals(
            listOf(MoveArrow(ArrowEnd.OnPoint(24, 1), ArrowEnd.OnPoint(18, 0))),
            opponentArrows(board, seat, listOf(LoggedStep(24, 18, false))),
        )
    }

    @Test
    fun `laudasta jaljessa oleva lista ei kelpaa`() {
        // Marathon #4327 30.9.2026: lista päättyi laskuriin 903, lauta sanoi Move 916.
        val jaljessa = loki("" to "63: 11/5 8/5").copy(lastState = 903)
        assertFalse(jaljessa.reaches(916))
        // Tuore: vastustajan siirto 915, oma heitto 916, tai lista jo heiton kohdalla.
        assertTrue(jaljessa.copy(lastState = 915).reaches(916))
        assertTrue(jaljessa.copy(lastState = 916).reaches(916))
        // Tuntematon kumpi tahansa ei piirrä.
        assertFalse(jaljessa.reaches(null))
        assertFalse(jaljessa.copy(lastState = null).reaches(916))
    }
}

package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Asema laudalta ja asema `.mat`-toistosta: kummankin on tuotettava sama luku samasta
 * tilanteesta, koska merkin kohdistus `.sgf`:aan on niiden vertailu.
 *
 * Toiston odotusarvot on laskettu käsin `match_four_games.mat`:n pelin 1 kahdesta
 * ensimmäisestä rivistä; koodia ei käytetä oraakkelina itselleen.
 */
class CheckerPositionTest {

    private fun side(vararg pisteet: Pair<Int, Int>): List<Int> =
        MutableList(CheckerPosition.SIZE) { 0 }.also { for ((p, n) in pisteet) it[p] = n }

    @Test
    fun `aloitusasema on sama kummallekin ja tallennusrivi palaa samaksi`() {
        assertEquals(side(24 to 2, 13 to 5, 8 to 3, 6 to 5), CheckerPosition.START.self)
        assertEquals(CheckerPosition.START.self, CheckerPosition.START.opponent)

        val rivi = CheckerPosition.START.encode()
        assertTrue(rivi.startsWith("1:0,0,0,0,0,0,5,0,3,"), rivi)
        assertEquals(CheckerPosition.START, CheckerPosition.decode(rivi))
        assertNull(CheckerPosition.decode("2:" + rivi.drop(2)))
        assertNull(CheckerPosition.decode("1:1,2,3"))
        assertNull(CheckerPosition.decode(""))
    }

    @Test
    fun `toisto siirtaa, lyo muurille ja tuo muurilta`() {
        // Peli 1: W 41: 24/23 13/9; B 44: 24/20 24/20 20/16* 20/16; W 54: 25/20 24/20.
        val w41 = MatAction.Roll(MatSide.FIRST, 4, 1, listOf(CheckerMove(24, 23), CheckerMove(13, 9)))
        val b44 = MatAction.Roll(
            MatSide.SECOND, 4, 4,
            listOf(CheckerMove(24, 20), CheckerMove(24, 20), CheckerMove(20, 16, hit = true), CheckerMove(20, 16)),
        )
        val w54 = MatAction.Roll(MatSide.FIRST, 5, 4, listOf(CheckerMove(25, 20), CheckerMove(24, 20)))

        val a = MatPosition.START.after(w41)
        assertEquals(side(24 to 1, 23 to 1, 13 to 4, 9 to 1, 8 to 3, 6 to 5), a.first)
        assertEquals(CheckerPosition.START.self, a.second)

        // B:n 16 on W:n 9, ja siellä yksin ollut nappula menee muurille.
        val b = a.after(b44)
        assertEquals(side(25 to 1, 24 to 1, 23 to 1, 13 to 4, 8 to 3, 6 to 5), b.first)
        assertEquals(side(16 to 2, 13 to 5, 8 to 3, 6 to 5), b.second)

        val c = b.after(w54)
        assertEquals(side(23 to 1, 20 to 2, 13 to 4, 8 to 3, 6 to 5), c.first)
        assertEquals(b.second, c.second)
    }

    @Test
    fun `pelin asemat ovat yksi per teko ja kuutioteko ei muuta asemaa`() {
        val mat = javaClass.getResourceAsStream("/match_four_games.mat")!!.readBytes().decodeToString()
        val g1 = MatMatch.parse(mat)!!.games[0]
        val asemat = g1.positions()

        assertEquals(g1.actions.size, asemat.size)
        // Viimeiset teot ovat B double, W drop, W win: kaikki sama asema.
        val n = asemat.size
        assertEquals(asemat[n - 3], asemat[n - 2])
        assertEquals(asemat[n - 2], asemat[n - 1])
        // Toisen rivin jälkeen sama kuin käsin laskettu yllä.
        assertEquals(side(23 to 1, 20 to 2, 13 to 4, 8 to 3, 6 to 5), asemat[2].first)
    }

    @Test
    fun `asetetusta asemasta alkavaa pelia ei toisteta`() {
        val g = MatGame(1, "a", "b", 0, 0, listOf(MatAction.Roll(MatSide.FIRST, 3, 1, emptyList())), setup = true)
        assertTrue(g.positions().isEmpty())
    }

    @Test
    fun `lauta luetaan istuimen suunnasta ja vastustaja omastaan`() {
        // Katsoja on keltainen, koti 1..6. Vastustajan piste 20 on hänen suunnastaan 5.
        val board = lauta(
            omat = mapOf(6 to 5, 8 to 3, 13 to 4, 24 to 1),
            vastustaja = mapOf(20 to 2, 12 to 5, 17 to 3, 1 to 2),
            omaMuuri = 1, vastustajaUlos = 3,
        )
        val asema = CheckerPosition.of(board, Seat(CheckerColor.YELLOW, homeIsLow = true))!!

        assertEquals(side(6 to 5, 8 to 3, 13 to 4, 24 to 1, 25 to 1), asema.self)
        assertEquals(side(5 to 2, 13 to 5, 8 to 3, 24 to 2, 0 to 3), asema.opponent)

        // Sama lauta sinisen istuimelta, koti 19..24: luvut vaihtavat paikkaa.
        val toisin = CheckerPosition.of(board, Seat(CheckerColor.BLUE, homeIsLow = false))!!
        assertEquals(asema.opponent, toisin.self)
        assertEquals(asema.self, toisin.opponent)
    }

    @Test
    fun `lauta ilman pisteita ei anna asemaa`() {
        assertNull(CheckerPosition.of(lauta(emptyMap(), emptyMap()).copy(points = emptyList()), Seat(CheckerColor.YELLOW, true)))
    }

    private fun lauta(
        omat: Map<Int, Int>,
        vastustaja: Map<Int, Int>,
        omaMuuri: Int = 0,
        vastustajaUlos: Int = 0,
    ): BoardState {
        val points = (1..24).map { number ->
            val own = omat[number] ?: 0
            val opp = vastustaja[number] ?: 0
            Point(number, if (own > 0) CheckerColor.YELLOW else if (opp > 0) CheckerColor.BLUE else null, own + opp)
        }
        return BoardState(
            matchId = MatchId("7000011"),
            moveNumber = 12,
            stateToken = "123",
            eventName = null,
            eventId = null,
            round = null,
            matchLength = 7,
            cube = null,
            dice = emptyList(),
            scheme = BoardScheme.BLUE_WHITE,
            points = points,
            players = listOf(
                PlayerPanel(PlayerRef(name = "vastapelaaja"), null, 0, "0", null),
                PlayerPanel(PlayerRef(name = "pelaaja"), null, 0, "0", null),
            ),
            prompt = null,
            notices = emptyList(),
            rolledBack = false,
            speculative = false,
            moves = emptyList(),
            commands = emptyList(),
            form = null,
            undoHref = null,
            borneOff = if (vastustajaUlos > 0) listOf(BorneOffCheckers(CheckerColor.BLUE, vastustajaUlos)) else emptyList(),
            bar = if (omaMuuri > 0) listOf(BarCheckers(CheckerColor.YELLOW, omaMuuri)) else emptyList(),
        )
    }
}

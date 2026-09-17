package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pakollisten askelten esipoiminta (`CompositionSession.forcedSteps` ja `prefillForced`).
 *
 * Tommin vaatimus 2.9.2026: jos joku siirto on pakollinen, niin se pelataan, ja siirto on
 * askel eikä vuoro. Nämä testit rakentavat asemat käsin kuten LocalCompositionTest;
 * oikeita asemia vasten sama on mitattu korpuksesta (PakkosiirtoKorpusTest, core-scrape).
 */
class PrefillForcedTest {

    private fun lauta(
        omat: Map<Int, Int>,
        vastustaja: Map<Int, Int> = emptyMap(),
        nopat: List<Int>,
        omaMuuri: Int = 0,
        linkit: Set<Int>,
    ): BoardState {
        val points = (1..24).map { number ->
            val own = omat[number] ?: 0
            val opp = vastustaja[number] ?: 0
            require(own == 0 || opp == 0) { "piste $number kahdella värillä" }
            Point(
                number = number,
                owner = if (own > 0) CheckerColor.YELLOW else if (opp > 0) CheckerColor.BLUE else null,
                count = own + opp,
            )
        }
        val yellowPips = points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count * it.number } +
            omaMuuri * 25
        val bluePips = points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count * (25 - it.number) }
        val bar = if (omaMuuri > 0) {
            listOf(BarCheckers(CheckerColor.YELLOW, omaMuuri, href = "/bg/move/7000011/123?move=y"))
        } else {
            emptyList()
        }
        return BoardState(
            matchId = MatchId("7000011"),
            moveNumber = null,
            stateToken = "123",
            eventName = null,
            eventId = null,
            round = null,
            matchLength = 7,
            cube = null,
            dice = nopat.map { Die(it, CheckerColor.YELLOW) },
            scheme = BoardScheme.BLUE_WHITE,
            points = points,
            players = listOf(
                PlayerPanel(PlayerRef(name = "vastapelaaja"), bluePips, 0, "0", null),
                PlayerPanel(PlayerRef(name = "pelaaja"), yellowPips, 0, "0", null),
            ),
            prompt = null,
            notices = emptyList(),
            rolledBack = false,
            speculative = false,
            moves = linkit.filter { it != BAR_POINT }.map { from ->
                MoveLink(from, moveLetter(from).toString(), "/bg/move/7000011/123?move=${moveLetter(from)}")
            },
            commands = emptyList(),
            form = null,
            undoHref = null,
            borneOff = emptyList(),
            bar = bar,
        )
    }

    /**
     * Muurilta tulo on pakko kun vain toinen noppa pääsee sisään. Piste 19 on tukittu
     * (kuutonen), piste 23 auki (kakkonen), joten askel (muuri, 2) on jokaisessa vuorossa
     * ja kuutonen on valinta kolmen lähdön välillä.
     */
    @Test
    fun `muurilta tulo poimitaan valmiiksi ja loppu jaa pelaajalle`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(19 to 2, 1 to 2),
            nopat = listOf(6, 2),
            omaMuuri = 1,
            linkit = setOf(13, 8),
        )
        val plain = checkNotNull(LocalComposition.begin(board))
        assertEquals(mapOf(MoveStep(BAR_POINT, 2) to 1), plain.forcedSteps())
        assertTrue(plain.steps.isEmpty(), "Ilman asetusta mitään ei poimita")

        val prefilled = checkNotNull(LocalComposition.begin(board, playForcedSteps = true))
        assertEquals(listOf(MoveStep(BAR_POINT, 2)), prefilled.steps)
        assertEquals("y", prefilled.letters)
        assertEquals(listOf(6), prefilled.remainingDice())
        assertTrue(!prefilled.isComplete, "Kuutonen on yhä pelaajan valinta")
        assertEquals(setOf(23, 13, 8), prefilled.legalFromPoints())

        val done = checkNotNull(prefilled.step(13))
        assertTrue(done.isComplete)
        assertEquals("ym", done.letters)
        assertNotNull(done.submission(), "Toisto päätyy samaan monijoukkoon kuin esipoiminta")
    }

    @Test
    fun `ilman pakollista askelta sessio pysyy koskemattomana`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13, 8),
        )
        val session = checkNotNull(LocalComposition.begin(board, playForcedSteps = true))
        assertEquals(emptyMap<MoveStep, Int>(), session.forcedSteps())
        assertTrue(session.steps.isEmpty())
        assertSame(session, session.prefillForced(), "Tyhjä leikkaus palauttaa saman session")
    }

    @Test
    fun `peruminen tyhjentaa myos esipoimitut kuten sivun oma Undo`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(19 to 2, 1 to 2),
            nopat = listOf(6, 2),
            omaMuuri = 1,
            linkit = setOf(13, 8),
        )
        val prefilled = checkNotNull(LocalComposition.begin(board, playForcedSteps = true))
        val cleared = prefilled.undo()
        assertTrue(cleared.steps.isEmpty())
        // Esipoiminta ei toistu itsestään: pelaaja pyysi tyhjää lautaa ja saa sen.
        assertEquals(setOf(BAR_POINT, 13, 8), cleared.legalFromPoints())
    }

    /**
     * Tupla: kolme nappulaa muurilla kakkosilla, piste 23 auki. Kolme tuloa ovat
     * jokaisessa vuorossa, neljäs kakkonen on valinta. Leikkaus laskee toiston oikein
     * (kolme kertaa sama askel), eikä neljättä poimita.
     */
    @Test
    fun `tuplassa sama askel kolmesti on kolme pakollista eika nelja`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(2, 2),
            omaMuuri = 3,
            linkit = setOf(13, 8),
        )
        val prefilled = checkNotNull(LocalComposition.begin(board, playForcedSteps = true))
        assertEquals(mapOf(MoveStep(BAR_POINT, 2) to 3), prefilled.forcedSteps())
        assertEquals(List(3) { MoveStep(BAR_POINT, 2) }, prefilled.steps)
        assertEquals("yyy", prefilled.letters)
        assertEquals(listOf(2), prefilled.remainingDice())
        assertTrue(!prefilled.isComplete)
    }
}

package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ahneen uloskannon esipoiminta (`CompositionSession.greedySteps` ja `prefillGreedy`).
 *
 * Tommin tilaus 2.9.2026: kun kontaktia ei ole, sovellus poimii ahneen uloskannon ja
 * `Submit Move` jää pelaajalle. Sääntö on mitattu sivuston omasta esitäytöstä
 * (GreedyKorpusTest, core-scrape): eniten ulos, ja kun sellaisia loppuasemia on useampi,
 * vain niiden yhteinen osa. Asemat rakennetaan käsin kuten PrefillForcedTest.
 */
class PrefillGreedyTest {

    private fun lauta(
        omat: Map<Int, Int>,
        vastustaja: Map<Int, Int> = emptyMap(),
        nopat: List<Int>,
        linkit: Set<Int>,
        vastustajaMuurilla: Int = 0,
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
        val yellowPips = points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count * it.number }
        val bluePips = points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count * (25 - it.number) } +
            vastustajaMuurilla * 25
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
            moves = linkit.map { from ->
                MoveLink(from, moveLetter(from).toString(), "/bg/move/7000011/123?move=${moveLetter(from)}")
            },
            commands = emptyList(),
            form = null,
            undoHref = null,
            borneOff = emptyList(),
            bar = if (vastustajaMuurilla > 0) listOf(BarCheckers(CheckerColor.BLUE, vastustajaMuurilla)) else emptyList(),
        )
    }

    /**
     * Nopat 2 ja 1, omat pisteillä 6, 5, 4 ja 2, vastustaja omassa kodissaan. Kakkonen
     * kantaa ulos vain kakkospisteeltä, ja ykkönen ei kanna mitään, koska ykköspisteelle
     * pääsee vain kakkosella 2->1 jolloin ykkönen on jo käytetty. Eniten ulos (yksi) kantavat
     * vuorot ovat (2 ulos) + ykkönen jollekin kolmesta, eli loppuasemia on kolme ja yhteinen
     * osa on askel (2, 2). Se ei ole pakollinen, koska vuoro 6->5, 5->3 on laillinen ja kantaa
     * nolla: tässä ahne ja pakollinen eroavat. (Ensimmäinen versio piti pistettä 3 ja unohti
     * että 3->1 ja sitten 1 ulos ykkösellä kantaa myös yhden.)
     */
    @Test
    fun `useampi ahne loppuasema poimii vain yhteisen osan`() {
        val board = lauta(
            omat = mapOf(6 to 1, 5 to 1, 4 to 1, 2 to 1),
            vastustaja = mapOf(20 to 3, 22 to 3),
            nopat = listOf(2, 1),
            linkit = setOf(6, 5, 4, 2),
        )
        val plain = checkNotNull(LocalComposition.begin(board))
        assertEquals(mapOf(MoveStep(2, 2) to 1), plain.greedySteps())
        assertTrue(plain.forcedSteps().isEmpty(), "Uloskanto ei ole pakko: 6->5, 5->3 on laillinen")

        val prefilled = checkNotNull(LocalComposition.begin(board, playGreedyBearoff = true))
        assertEquals(listOf(MoveStep(2, 2)), prefilled.steps)
        assertEquals(listOf(1), prefilled.remainingDice())
        assertTrue(!prefilled.isComplete, "Ykkönen jää pelaajan valinnaksi")
    }

    /**
     * Nopat 6 ja 4, omat pisteillä 6 ja 4 vain: molemmat kantavat ulos ja loppuasema on
     * yksikäsitteinen, joten koko vuoro poimitaan ja lomake on heti valmis. Sivusto tarjoaisi
     * tässä `Submit Greedy Bearoff` -napin; täällä `Submit Move` jää pelaajalle.
     */
    @Test
    fun `yksikasitteinen ahne vuoro poimitaan kokonaan`() {
        val board = lauta(
            omat = mapOf(6 to 1, 4 to 1, 2 to 3),
            vastustaja = mapOf(19 to 2),
            nopat = listOf(6, 4),
            linkit = setOf(6, 4),
        )
        val prefilled = checkNotNull(LocalComposition.begin(board, playGreedyBearoff = true))
        assertEquals(mapOf(MoveStep(6, 6) to 1, MoveStep(4, 4) to 1), prefilled.steps.groupingBy { it }.eachCount())
        assertTrue(prefilled.isComplete)
        assertNotNull(prefilled.submission(), "Toisto päätyy samaan monijoukkoon")
    }

    /** Kontakti: vastustajan nappula on yhä omien takana, joten ahne ei koske lautaan. */
    @Test
    fun `kontaktissa ei poimita mitaan`() {
        val board = lauta(
            omat = mapOf(6 to 1, 4 to 1, 2 to 3),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 4),
            linkit = setOf(6, 4),
        )
        val plain = checkNotNull(LocalComposition.begin(board))
        assertTrue(plain.greedySteps().isEmpty())
        val prefilled = checkNotNull(LocalComposition.begin(board, playGreedyBearoff = true))
        assertTrue(prefilled.steps.isEmpty())
    }

    /** Vastustaja muurilla on takimmainen, eli kontakti vaikka lauta näyttäisi vapaalta. */
    @Test
    fun `vastustaja muurilla on kontakti`() {
        val board = lauta(
            omat = mapOf(6 to 1, 4 to 1, 2 to 3),
            vastustaja = mapOf(19 to 2),
            nopat = listOf(6, 4),
            linkit = setOf(6, 4),
            vastustajaMuurilla = 1,
        )
        val plain = checkNotNull(LocalComposition.begin(board))
        assertTrue(plain.greedySteps().isEmpty())
    }

    /** Kilpajuoksu jossa uloskantoa ei vielä ole: ahne ei tee mitään, pakollinen yhä voi. */
    @Test
    fun `ilman uloskantoa ahne on tyhja`() {
        val board = lauta(
            omat = mapOf(10 to 3, 8 to 2),
            vastustaja = mapOf(20 to 3),
            nopat = listOf(6, 3),
            linkit = setOf(10, 8),
        )
        val plain = checkNotNull(LocalComposition.begin(board))
        assertTrue(plain.greedySteps().isEmpty())
    }

    /**
     * Molemmat päällä: ahne kokeillaan ensin ja pakollinen vain jos ahne ei poiminut.
     * Tässä ahne poimii (2, 2) vaikka se ei ole pakollinen, ja pakollinen ei poimi päälle.
     */
    @Test
    fun `ahne kokeillaan ennen pakollista`() {
        val board = lauta(
            omat = mapOf(6 to 1, 5 to 1, 4 to 1, 2 to 1),
            vastustaja = mapOf(20 to 3, 22 to 3),
            nopat = listOf(2, 1),
            linkit = setOf(6, 5, 4, 2),
        )
        val both = checkNotNull(LocalComposition.begin(board, playForcedSteps = true, playGreedyBearoff = true))
        assertEquals(listOf(MoveStep(2, 2)), both.steps)
    }
}

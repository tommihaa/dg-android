package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Avausheiton johdos, eli [BoardState.openingRoll] ja [BoardState.withOpeningDice].
 *
 * Testattava on laukeamisen raja eikä isomman nopan valinta. Esitys jakaa nopat kahdelle
 * puolelle, ja väärään aikaan lauennut jako väittäisi keskellä peliä että vastustajalla on
 * noppa. Siksi tapaukset ovat ne joissa lähtöasema melkein täyttyy: yksi nappula liikkunut,
 * nappula muurilla, sama silmäluku, tai nopat jo kahdella omistajalla.
 */
class OpeningRollTest {

    private val nearStart = mapOf(1 to 2, 12 to 5, 17 to 3, 19 to 5)
    private val farStart = mapOf(24 to 2, 13 to 5, 8 to 3, 6 to 5)

    private fun lauta(
        keltainen: Map<Int, Int> = farStart,
        sininen: Map<Int, Int> = nearStart,
        nopat: List<Die>,
        muuri: List<BarCheckers> = emptyList(),
        ulkona: List<BorneOffCheckers> = emptyList(),
    ): BoardState {
        val points = (1..24).map { number ->
            val y = keltainen[number] ?: 0
            val b = sininen[number] ?: 0
            require(y == 0 || b == 0) { "piste $number kahdella varilla" }
            Point(
                number = number,
                owner = if (y > 0) CheckerColor.YELLOW else if (b > 0) CheckerColor.BLUE else null,
                count = y + b,
            )
        }
        return BoardState(
            matchId = MatchId("7000032"),
            moveNumber = 1,
            stateToken = "1",
            eventName = null,
            eventId = null,
            round = null,
            matchLength = 5,
            cube = null,
            dice = nopat,
            scheme = BoardScheme.BLUE_WHITE,
            points = points,
            players = listOf(
                PlayerPanel(PlayerRef(name = "vastapelaaja"), 167, 0, "0", null),
                PlayerPanel(PlayerRef(name = "pelaaja"), 167, 0, "0", null),
            ),
            prompt = null,
            notices = emptyList(),
            rolledBack = false,
            speculative = false,
            moves = emptyList(),
            commands = emptyList(),
            form = null,
            undoHref = null,
            borneOff = ulkona,
            bar = muuri,
        )
    }

    private fun keltaiset(vararg arvot: Int) = arvot.map { Die(it, CheckerColor.YELLOW) }

    @Test
    fun `lahtoasemassa isompi noppa on aloittajan ja pienempi vastustajan`() {
        val lauta = lauta(nopat = keltaiset(1, 5))
        assertEquals(OpeningRoll(CheckerColor.YELLOW, starterValue = 5, otherValue = 1), lauta.openingRoll)
        assertEquals(
            listOf(Die(1, CheckerColor.BLUE), Die(5, CheckerColor.YELLOW)),
            lauta.withOpeningDice().dice,
        )
    }

    @Test
    fun `varit voivat olla toisin pain`() {
        val lauta = lauta(
            keltainen = nearStart,
            sininen = farStart,
            nopat = listOf(Die(3, CheckerColor.BLUE), Die(6, CheckerColor.BLUE)),
        )
        assertEquals(OpeningRoll(CheckerColor.BLUE, 6, 3), lauta.openingRoll)
    }

    @Test
    fun `kulutus kulkee esitykseen mukana`() {
        val lauta = lauta(nopat = listOf(Die(2, CheckerColor.YELLOW, spent = 1f), Die(6, CheckerColor.YELLOW)))
        assertEquals(
            listOf(Die(2, CheckerColor.BLUE, spent = 1f), Die(6, CheckerColor.YELLOW)),
            lauta.withOpeningDice().dice,
        )
    }

    @Test
    fun `yksi siirtynyt nappula riittaa sammuttamaan`() {
        val siirtynyt = farStart - 13 + mapOf(13 to 4, 11 to 1)
        val lauta = lauta(keltainen = siirtynyt, nopat = keltaiset(2, 6))
        assertNull(lauta.openingRoll)
        assertEquals(lauta, lauta.withOpeningDice())
    }

    @Test
    fun `nappula muurilla tai ulkona ei ole lahtoasema`() {
        val muurilla = lauta(
            nopat = keltaiset(2, 6),
            muuri = listOf(BarCheckers(CheckerColor.BLUE, 1)),
        )
        assertNull(muurilla.openingRoll)
        val ulkona = lauta(
            nopat = keltaiset(2, 6),
            ulkona = listOf(BorneOffCheckers(CheckerColor.YELLOW, 1)),
        )
        assertNull(ulkona.openingRoll)
    }

    @Test
    fun `sama silmaluku ei ole avausheitto`() {
        assertNull(lauta(nopat = keltaiset(4, 4)).openingRoll)
    }

    @Test
    fun `nopat kahdella omistajalla tai ilman eivat laukaise`() {
        val kahdella = lauta(nopat = listOf(Die(1, CheckerColor.BLUE), Die(5, CheckerColor.YELLOW)))
        assertNull(kahdella.openingRoll)
        assertNull(lauta(nopat = listOf(Die(1, null), Die(5, null))).openingRoll)
        assertNull(lauta(nopat = emptyList()).openingRoll)
    }

    @Test
    fun `esitys on idempotentti`() {
        val kerran = lauta(nopat = keltaiset(1, 5)).withOpeningDice()
        assertEquals(kerran, kerran.withOpeningDice())
    }
}

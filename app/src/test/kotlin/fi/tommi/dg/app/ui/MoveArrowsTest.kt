package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.BoardState
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.Die
import fi.tommi.dg.domain.LocalComposition
import fi.tommi.dg.domain.LoggedStep
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.MoveLink
import fi.tommi.dg.domain.Point
import fi.tommi.dg.domain.PlayerPanel
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.Seat
import fi.tommi.dg.domain.opponentArrows
import fi.tommi.dg.domain.reconcilePips
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Siirtonuolten geometria: mihin sarakkeeseen ja korkeudelle nuolen päät osuvat.
 *
 * Nuolten sisältö (mistä mihin, missä pinon paikassa) on todennettu `LocalCompositionTest`issä
 * (`core-domain`). Tämä testi väittää vain sen, että sama paikka piirtyy laudan omaan
 * ruudukkoon: 13 saraketta, peilaus ja [CheckerStack]in pinon askel.
 */
class MoveArrowsTest {

    /** Lauta 1300 px leveä ilman lokeroa, jolloin sarake on tasan 100 px. */
    private val kehys = ArrowFrame(
        width = 1300f,
        trayWidth = 0f,
        bandWidth = 0f,
        wedgeHeight = 300f,
        bandHeight = 100f,
        checkerSize = 50f,
        mirrored = false,
    )

    @Test
    fun `sarakkeet seuraavat laudan numerointia molemmissa suunnissa`() {
        assertEquals(0, pointColumn(13, mirrored = false))
        // Muuri on sarake 6, joten oikean puoliskon ensimmäinen piste on 7.
        assertEquals(7, pointColumn(19, mirrored = false))
        assertEquals(12, pointColumn(24, mirrored = false))
        assertEquals(0, pointColumn(12, mirrored = false))
        assertEquals(12, pointColumn(1, mirrored = false))

        assertEquals(0, pointColumn(24, mirrored = true))
        assertEquals(12, pointColumn(13, mirrored = true))
        assertEquals(0, pointColumn(1, mirrored = true))
        assertEquals(12, pointColumn(12, mirrored = true))
    }

    @Test
    fun `pinon paikka noudattaa pinon omaa askelta`() {
        assertEquals(25f, stackOffset(count = 1, slot = 0, available = 300f, checkerSize = 50f), 0.01f)
        assertEquals(125f, stackOffset(count = 3, slot = 2, available = 300f, checkerSize = 50f), 0.01f)
        // Kahdeksan nappulaa 300 px:n kiilassa: askel (300 - 50) / 7, eli pino tiivistyy.
        assertEquals(25f + 250f, stackOffset(count = 8, slot = 7, available = 300f, checkerSize = 50f), 0.01f)
    }

    @Test
    fun `kokoamisen nuoli kulkee lahtopisteelta kohteen paallimmaiseen`() {
        val session = checkNotNull(LocalComposition.begin(lauta()))
        val koottu = checkNotNull(checkNotNull(session.step(13)).step(8))

        val viivat = arrowLines(koottu.arrows(), koottu.boardNow(), CheckerColor.YELLOW, kehys)

        // 13/7: piste 13 on ylärivin vasen reuna, 7 alarivin kuudes sarake vasemmalta.
        // Alarivin kanta on 2 * 300 + 100 = 700 px, ja 7 oli tyhjä, joten paikka 0.
        assertEquals(ArrowLine(50f, 75f, 550f, 675f), viivat[0])
        // 8/5: kahden nappulan pisteen päällimmäinen lähtee, ja 5 oli tyhjä.
        assertEquals(ArrowLine(450f, 625f, 850f, 675f), viivat[1])
    }

    @Test
    fun `vastustajan siirto ja latausrivi naytetaan vain ennen omaa askelta`() {
        // sessio-29-9-ackammon: esipoimittu pakkosiirto on koottu askel, joten latausrivi
        // ei saa näkyä sen päällä. Ehto on sama kuin vastustajan nuolilla.
        val board = lauta()
        val session = checkNotNull(LocalComposition.begin(board))
        val tyhja = BoardUiState.Loaded(board, board.reconcilePips(), composition = session, readingOpponent = true)
        assertTrue(tyhja.showsOpponentMove)
        assertFalse(tyhja.copy(composition = session.step(13)).showsOpponentMove)
        assertTrue(tyhja.copy(composition = null).showsOpponentMove)
    }

    @Test
    fun `peilattu lauta siirtaa pelialueen lokeron oikealle puolelle`() {
        val session = checkNotNull(LocalComposition.begin(lauta()))
        val koottu = checkNotNull(session.step(13))
        val peilattu = kehys.copy(width = 1500f, trayWidth = 200f, mirrored = true)

        val viiva = arrowLines(koottu.arrows(), koottu.boardNow(), CheckerColor.YELLOW, peilattu).single()

        // Pelialue alkaa lokeron jälkeen (200 px), ja peilattuna 13 on ylärivin oikea reuna.
        assertEquals(200f + 1250f, viiva.fromX, 0.01f)
        // Peilattuna alarivi on 1..6 ja 7..12, joten 7 on heti muurin oikealla puolella.
        assertEquals(200f + 750f, viiva.toX, 0.01f)
    }

    @Test
    fun `vastustajan muurilta tulo lahtee muurin alasegmentista`() {
        // Sininen tuli muurilta pisteeseen 3 (oma merkintä 25/22). Vastustajan nappula on
        // muurin alasegmentissä ja pinoutuu keskikaistasta alaspäin ([BarSegment]).
        val board = lauta().copy(
            points = lauta().points.map { if (it.number == 3) it.copy(owner = CheckerColor.BLUE, count = 1) else it },
        )
        val seat = Seat(CheckerColor.YELLOW, homeIsLow = true)
        val nuolet = checkNotNull(opponentArrows(board, seat, listOf(LoggedStep(25, 22, hit = true))))

        val viiva = arrowLines(nuolet, board, CheckerColor.BLUE, kehys, barOnTop = false).single()

        // Muuri on sarake 6 (x 650). Alasegmentti alkaa kiilan ja kaistan jälkeen (400 px),
        // ja ensimmäisen nappulan keskipiste on puolen nappulan päässä siitä.
        assertEquals(ArrowLine(650f, 425f, 1050f, 675f), viiva)
    }

    /** Sama asema kuin `DiceTapTest`issä: kaksi omaa pistettä ja nopat 6 ja 3. */
    private fun lauta(): BoardState {
        val omat = mapOf(13 to 2, 8 to 2)
        val vastustaja = mapOf(1 to 2)
        val points = (1..24).map { number ->
            val own = omat[number] ?: 0
            val opp = vastustaja[number] ?: 0
            Point(
                number = number,
                owner = if (own > 0) CheckerColor.YELLOW else if (opp > 0) CheckerColor.BLUE else null,
                count = own + opp,
            )
        }
        val yellowPips = points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count * it.number }
        val bluePips = points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count * (25 - it.number) }
        return BoardState(
            matchId = MatchId("7000011"),
            moveNumber = null,
            stateToken = "123",
            eventName = null,
            eventId = null,
            round = null,
            matchLength = 7,
            cube = null,
            dice = listOf(Die(6, CheckerColor.YELLOW), Die(3, CheckerColor.YELLOW)),
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
            moves = listOf(
                MoveLink(13, "m", "/bg/move/7000011/123?move=m"),
                MoveLink(8, "h", "/bg/move/7000011/123?move=h"),
            ),
            commands = emptyList(),
            form = null,
            undoHref = null,
            borneOff = emptyList(),
            bar = emptyList(),
        )
    }
}

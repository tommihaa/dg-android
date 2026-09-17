package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Paikallinen kokoaminen: poiminta, ahne noppavalinta, vapaa jarjestys ja lahetys.
 *
 * Saannoston ja sivun yhtapitavyys todennetaan fixtureista LocalCompositionOracleTestissa
 * (core-scrape); nama testit rakentavat asemat kasin ja vaittavat kayttaytymisesta sen
 * minka oraakkeli ei kata: askelten kertyman, nopan valinnan, peruutuksen ja lahetyksen.
 */
class LocalCompositionTest {

    private fun lauta(
        omat: Map<Int, Int>,
        vastustaja: Map<Int, Int> = emptyMap(),
        nopat: List<Int>,
        omaMuuri: Int = 0,
        linkit: Set<Int>? = null,
    ): BoardState {
        val points = (1..24).map { number ->
            val own = omat[number] ?: 0
            val opp = vastustaja[number] ?: 0
            require(own == 0 || opp == 0) { "piste $number kahdella varilla" }
            Point(
                number = number,
                owner = if (own > 0) CheckerColor.YELLOW else if (opp > 0) CheckerColor.BLUE else null,
                count = own + opp,
            )
        }
        // Keltainen kulkee kohti pienia numeroita, sininen isoja. Paneelin pipit lasketaan
        // asemasta, jotta istuin ratkeaa samalla paattelylla kuin oikealla sivulla.
        val yellowPips = points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count * it.number } +
            omaMuuri * 25
        val bluePips = points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count * (25 - it.number) }
        val bar = if (omaMuuri > 0) {
            listOf(BarCheckers(CheckerColor.YELLOW, omaMuuri, href = "/bg/move/7000011/123?move=y"))
        } else {
            emptyList()
        }
        val froms = linkit ?: emptySet()
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
            moves = froms.filter { it != BAR_POINT }.map { from ->
                MoveLink(from, moveLetter(from).toString(), "/bg/move/7000011/123?move=${moveLetter(from)}")
            },
            commands = emptyList(),
            form = null,
            undoHref = null,
            borneOff = emptyList(),
            bar = bar,
        )
    }

    @Test
    fun `poiminta kayttaa noppia jarjestyksessa ja taydentyy lahetykseksi`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13, 8),
        )
        val session = checkNotNull(LocalComposition.begin(board))
        assertEquals(setOf(13, 8), session.legalFromPoints())

        val first = checkNotNull(session.step(13))
        assertEquals(listOf(MoveStep(13, 6)), first.steps, "Ahne valinta ottaa naytetyn jarjestyksen ensimmaisen")
        assertTrue(!first.isComplete)

        val second = checkNotNull(first.step(8))
        assertEquals(listOf(MoveStep(13, 6), MoveStep(8, 3)), second.steps)
        assertTrue(second.isComplete)
        assertEquals("mh", second.letters)

        val submission = checkNotNull(second.submission())
        assertEquals("/bg/move/7000011/123", submission.action)
        assertEquals(FormMethod.GET, submission.method)
        assertEquals(mapOf("move" to "mh", "submit" to "Submit Move"), submission.fields)
    }

    @Test
    fun `jarjestyksen vaihto valitsee toisen nopan ja tuottaa S-etuliitteen`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13, 8),
        )
        val session = checkNotNull(LocalComposition.begin(board))
        assertTrue(session.canSwap)

        val swapped = checkNotNull(session.swap())
        val first = checkNotNull(swapped.step(13))
        assertEquals(listOf(MoveStep(13, 3)), first.steps)
        assertTrue(!first.canSwap, "Vaihto on mahdollinen vain ennen ensimmaista poimintaa")

        val second = checkNotNull(first.step(8))
        assertEquals("Smh", second.letters)
        assertNotNull(second.submission())
    }

    @Test
    fun `peruminen tyhjentaa koko kertyman kuten sivun oma Undo`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13, 8),
        )
        val session = checkNotNull(LocalComposition.begin(board))
        val composed = checkNotNull(checkNotNull(session.step(13)).step(8))

        val undone = composed.undo()
        assertEquals(emptyList<MoveStep>(), undone.steps)
        assertEquals(setOf(13, 8), undone.legalFromPoints())
    }

    @Test
    fun `syonti siirtaa vastustajan blotin muurille naytettavassa laudassa`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(7 to 1, 1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13, 8),
        )
        val session = checkNotNull(LocalComposition.begin(board))
        val first = checkNotNull(session.step(13))

        val shown = first.boardNow()
        assertEquals(CheckerColor.YELLOW, shown.points.first { it.number == 7 }.owner, "13-6 osuu blottiin")
        assertEquals(listOf(BarCheckers(CheckerColor.BLUE, 1)), shown.bar)
        assertEquals(listOf(6 to 1f, 3 to 0f), shown.dice.map { it.value to it.spent }, "Kaytetty noppa jaa nakyviin harmaana")
    }

    @Test
    fun `sivuston noppaesitys pitaa kaksi noppaa eika vahenna niita`() {
        // Mitattu kaytos 27.8.2026: sivusto nayttaa aina tasan kaksi noppakuvaa, myos
        // kokoamisen keskella. siteDice koskee vain piirtoa: poiminta ja kirjaimet ovat
        // samat kuin laskurimuodossa.
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13, 8),
        )
        val session = checkNotNull(LocalComposition.begin(board))
        val first = checkNotNull(session.step(13))

        assertEquals(listOf(6 to 1f, 3 to 0f), first.boardNow(siteDice = true).dice.map { it.value to it.spent })
        assertEquals(listOf(6 to 1f, 3 to 0f), first.boardNow(siteDice = false).dice.map { it.value to it.spent })

        val second = checkNotNull(first.step(8))
        assertEquals(
            listOf(6 to 1f, 3 to 1f),
            second.boardNow(siteDice = true).dice.map { it.value to it.spent },
            "Nopat eivat katoa edes taydesta vuorosta ennen lahetysta, molemmat harmaina",
        )
        assertEquals("mh", second.letters, "Esitys ei muuta lahetettavaa kirjainjonoa")
    }

    @Test
    fun `tupla nayttaa sivuston esityksessa kaksi noppaa ja laskurissa nelja`() {
        val board = lauta(
            omat = mapOf(13 to 4, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(3, 3),
            linkit = setOf(13, 8),
        )
        val session = checkNotNull(LocalComposition.begin(board))

        assertEquals(listOf(3, 3), session.boardNow(siteDice = true).dice.map { it.value })
        assertEquals(
            listOf(3, 3, 3, 3),
            session.boardNow(siteDice = false).dice.map { it.value },
            "Laskuri laajentaa tuplan yhdeksi kuutioksi per jaljella oleva siirto",
        )

        val first = checkNotNull(session.step(13))
        assertEquals(
            listOf(0.5f, 0f),
            first.boardNow(siteDice = true).dice.map { it.spent },
            "Sivuston tuplassa kuutio harmaantuu puolikas kerrallaan",
        )
        assertEquals(
            listOf(1f, 0f, 0f, 0f),
            first.boardNow(siteDice = false).dice.map { it.spent },
            "Laskurissa nelja kuutiota pysyvat ja pelattu harmaantuu",
        )

        val third = checkNotNull(checkNotNull(first.step(13)).step(13))
        assertEquals(listOf(1f, 0.5f), third.boardNow(siteDice = true).dice.map { it.spent })
        assertEquals(listOf(1f, 1f, 1f, 0f), third.boardNow(siteDice = false).dice.map { it.spent })
    }

    @Test
    fun `kun vain toinen noppa mahtuu ja kumpikin kelpaisi yksin, isompi on pakko`() {
        // Ainoa nappula 24:lla. Kumpikin noppa kelpaa yksinaan (18 ja 21 auki), mutta
        // jatko on tukossa (15 tukittu 21:sta ja 18:sta). Vakiosaanto: isompi on pakko.
        val board = lauta(
            omat = mapOf(24 to 1),
            vastustaja = mapOf(15 to 2, 12 to 2, 1 to 2),
            nopat = listOf(3, 6),
            linkit = setOf(24),
        )
        val session = checkNotNull(LocalComposition.begin(board))
        val first = checkNotNull(session.step(24))
        assertEquals(listOf(MoveStep(24, 6)), first.steps, "Pienempi ei kelpaa vaikka on jarjestyksessa ensin")
        assertTrue(first.isComplete)
    }

    @Test
    fun `muurilta tulo ei pakota poimintajarjestysta`() {
        // Klassisesti muurilta on tultava ensin. Sivusto sallii poiminnan vapaassa
        // jarjestyksessa kunhan kokonaisuus taydentyy lailliseksi (mitattu
        // move_bar_entry.html:sta), joten pisteen 6 saa poimia ennen muuria.
        val board = lauta(
            omat = mapOf(6 to 1),
            vastustaja = mapOf(12 to 2),
            nopat = listOf(5, 4),
            omaMuuri = 1,
            linkit = setOf(6, BAR_POINT),
        )
        val session = checkNotNull(LocalComposition.begin(board))
        assertEquals(setOf(6, BAR_POINT), session.legalFromPoints())

        val first = checkNotNull(session.step(6))
        assertEquals(listOf(MoveStep(6, 5)), first.steps)
        assertEquals(setOf(BAR_POINT), first.legalFromPoints(), "Jaljella vain muurilta tulo")

        val second = checkNotNull(first.step(BAR_POINT))
        assertTrue(second.isComplete)
        assertEquals("fy", second.letters)
        assertNotNull(second.submission())
    }

    @Test
    fun `oraakkelin erimielisyys pudottaa vanhaan reittiin`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13),
        )
        assertNull(LocalComposition.begin(board), "Sivu ei tarjonnut kaikkia lahtoja, joten ei kosketa")
    }

    @Test
    fun `keskeneraisen palvelinkokoamisen sivu ei kaynnisty paikallisena`() {
        val board = lauta(
            omat = mapOf(13 to 2, 8 to 2),
            vastustaja = mapOf(1 to 2),
            nopat = listOf(6, 3),
            linkit = setOf(13, 8),
        ).copy(undoHref = "/bg/move/7000011/123")
        assertNull(LocalComposition.begin(board))
    }
}

package fi.tommi.dg.scrape

import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.ownLinks
import fi.tommi.dg.domain.pipCount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(Charsets.UTF_8).readText()

    private val board = checkNotNull(BoardParser.parse(fixture("move_board.html")))

    @Test
    fun `ottelun perustiedot luetaan`() {
        assertEquals("7000002", board.matchId.value)
        assertEquals(579, board.moveNumber)
        assertEquals("Nine Lives #2222", board.eventName)
        assertEquals("900002", board.eventId)
        assertEquals("/bg/event/900002", board.eventPath)
        assertEquals("Round 4", board.round)
        assertEquals(9, board.matchLength)
        assertEquals(1, board.cube?.value)
        assertEquals("Please make a checker move.", board.prompt)
    }

    @Test
    fun `tilatunniste luetaan sivun linkeistä eikä osoitteesta`() {
        // Haettu sivu oli /1497 mutta sen omat linkit osoittavat /1507. Tunnistetta ei voi
        // laskea, ja väärä tunniste tuottaisi väärän siirron eikä virhettä.
        assertEquals("1507", board.stateToken)
    }

    @Test
    fun `lauta sisältää 24 pistettä`() {
        assertEquals(24, board.points.size)
        assertEquals((1..24).toList(), board.points.map { it.number })
    }

    @Test
    fun `nappuloita on 15 kumpaakin väriä`() {
        // Backgammonin invariantti. Pätee tässä fixturessa koska yhtään nappulaa ei ole
        // muurilla eikä ulkona.
        assertEquals(15, board.points.filter { it.owner == CheckerColor.YELLOW }.sumOf { it.count })
        assertEquals(15, board.points.filter { it.owner == CheckerColor.BLUE }.sumOf { it.count })
    }

    @Test
    fun `laudasta laskettu pip-luku täsmää pelaajapaneelin lukuun`() {
        // Sivun kaksi riippumatonta osaa todistavat toisensa. Tämä on jäsentimen vahvin
        // vahti: numeroinnin, omistajuuden tai suunnan virhe rikkoo summan.
        val yellowPips = board.pipCount(CheckerColor.YELLOW, homeIsLowNumbers = true)
        val bluePips = board.pipCount(CheckerColor.BLUE, homeIsLowNumbers = false)

        assertEquals(99, yellowPips)
        assertEquals(117, bluePips)

        val panelPips = board.players.mapNotNull { it.pips }.toSet()
        assertEquals(setOf(99, 117), panelPips)
        assertTrue(yellowPips in panelPips && bluePips in panelPips)
    }

    @Test
    fun `yksittäiset pisteet jäsentyvät oikein`() {
        fun point(number: Int) = board.points.single { it.number == number }

        assertEquals(CheckerColor.YELLOW, point(13).owner)
        assertEquals(3, point(13).count)

        assertEquals(CheckerColor.BLUE, point(21).owner)
        assertEquals(3, point(21).count)

        // Tyhjä piste: ALT on "_", ei väriä eikä lukumäärää.
        assertEquals(null, point(15).owner)
        assertEquals(0, point(15).count)
    }

    @Test
    fun `siirtolinkit luetaan sivulta ja osoittavat nappuloilliselle pisteelle`() {
        val fromPoints = board.moves.map { it.fromPoint }
        assertEquals(listOf(5, 6, 7, 13), fromPoints.sorted())

        board.moves.forEach { move ->
            val point = board.points.single { it.number == move.fromPoint }
            assertTrue(point.count > 0, "Siirtolinkki tyhjältä pisteeltä ${move.fromPoint}")
            assertTrue(move.href.contains("/bg/move/7000002/1507"))
        }
    }

    @Test
    fun `siirtokirjaimet vastaavat oletusta a on piste 1`() {
        // Oletusta ei käytetä osoitteiden rakentamiseen, koodi lukee kirjaimen sivulta.
        // Tämä testi on siis vahti: jos oletus joskus pettää, se näkyy punaisena testinä
        // eikä vääränä siirtona oikeassa ottelussa.
        board.moves.forEach { move ->
            val expected = 'a' + (move.fromPoint - 1)
            assertEquals(
                expected.toString(),
                move.code,
                "Piste ${move.fromPoint} antoi kirjaimen ${move.code}",
            )
        }
    }

    @Test
    fun `nopat luetaan arvoineen ja väreineen`() {
        assertEquals(listOf(6, 3), board.dice.map { it.value })
        assertTrue(board.dice.all { it.owner == CheckerColor.YELLOW })
    }

    @Test
    fun `pelaajapaneelit luetaan`() {
        val opponent = board.players.single { it.player.name == "vastapelaaja" }
        assertEquals("91002", opponent.player.userId)
        assertEquals(117, opponent.pips)
        assertEquals(5, opponent.score)
        assertEquals("#3399CC", opponent.backgroundColor)

        val self = board.players.single { it.player.name == "pelaaja" }
        assertEquals("90001", self.player.userId)
        assertEquals(99, self.pips)
        assertEquals(7, self.score)
    }

    @Test
    fun `nappuloiden vari luetaan nimisolun taustasta`() {
        // Mitattu 31.8.2026: #3399CC = sininen, #FFFFFF = keltainen, 132 tapausta ilman
        // ristiriitaa. Tama on se lahde jonka takia roolia ei enaa pääteltä pisteluvuista.
        assertEquals(CheckerColor.BLUE, board.players.single { it.player.name == "vastapelaaja" }.checkerColor)
        assertEquals(CheckerColor.YELLOW, board.players.single { it.player.name == "pelaaja" }.checkerColor)
    }

    @Test
    fun `tuntematonta taustavaria ei arvata`() {
        val panel = board.players.first().copy(backgroundColor = "#123456")
        assertEquals(null, panel.checkerColor)
        assertEquals(null, panel.copy(backgroundColor = null).checkerColor)
    }

    @Test
    fun `komennot erottuvat nappulalinkeistä`() {
        val swap = board.commands.single { it.label == "Swap Dice" }
        assertTrue(swap.href.contains("move=S"))
        // Nappulalinkit kietovat kuvan eivätkä tuota tekstiä, joten ne eivät saa päätyä
        // komentolistaan.
        assertTrue(board.commands.none { it.label.isBlank() })
    }

    @Test
    fun `Skip Game luetaan osoitteesta eikä komentona`() {
        assertEquals("/bg/nextgame?skip=7000002", board.skipHref)
        // Ehto on `move=`, joten skip ei saa vuotaa komennoksi: muuten se piirtyisi kahdesti
        // ja toinen niistä ilman ottelunvaihdon poikkeusta.
        assertTrue(board.commands.none { it.href.contains("nextgame") })
        // Navigointipalkki on samalla sivulla, eikä siitä saa tulla nappeja.
        assertTrue(board.commands.none { it.label == "Top Page" || it.label == "Game Lounge" })
    }

    @Test
    fun `Skip Game on laudan oma linkki, joten portti päästää sen läpi`() {
        assertTrue(board.ownLinks().contains("/bg/nextgame?skip=7000002"))
    }

    @Test
    fun `tyhjä sivu ei kaada jäsennintä`() {
        assertEquals(null, BoardParser.parse("<HTML><BODY>Ei mitään</BODY></HTML>"))
    }

    @Test
    fun `lomakkeen piilokentät luetaan sellaisinaan`() {
        // Fixture on oikea sivu 22.8.2026 pelisessiosta, se johon `Next Game` kaatui:
        // POSTissa lähti vain `submit`, ja sivusto palautti saman sivun 200:lla. Sivun oma
        // `commit=1` on siis lomakkeen osa, ja sen pudottaminen on hiljainen vika.
        val pending = checkNotNull(BoardParser.parse(fixture("move_pending_next_game.html")))
        val form = checkNotNull(pending.form)

        assertEquals(mapOf("commit" to "1"), form.hiddenFields)
        assertEquals(listOf("Next Game", "To Top"), form.submits)
        assertEquals("/bg/move/7000011/556", form.action)
    }

    @Test
    fun `navigaation DG-kuvaa ei luulla nopaksi`() {
        // Alanavigaatiossa on /images/die.gif jonka ALT on "DG". Jos nopat haettaisiin
        // liian väljällä ehdolla, se tulisi mukaan.
        assertNotNull(board.dice)
        assertEquals(2, board.dice.size)
    }

    // Liigaottelun otsikko on pelkkaa tekstia ilman tapahtumalinkkia (mitattu 14.9.2026,
    // `sessio-14-9-ilta` sivu 0003). Nimi luetaan silloin h3:sta, ja kierrosta ei ole.
    @Test
    fun `liigaottelun nimi luetaan otsikosta ilman linkkia`() {
        val html = fixture("move_board.html").replace(
            "<h3><a href=/bg/event/900002>Nine Lives #2222</a>, Round 4</h3>",
            "<h3>DG 8x8 2026 - H vs C - Div 1</h3>",
        )
        val league = checkNotNull(BoardParser.parse(html))
        assertEquals("DG 8x8 2026 - H vs C - Div 1", league.eventName)
        assertEquals(null, league.eventId)
        assertEquals(null, league.eventPath)
        assertEquals(null, league.round)
    }
}

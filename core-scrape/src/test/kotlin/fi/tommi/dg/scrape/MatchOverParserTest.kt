package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.press
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ottelun päättymissivu, kolme mitattua muunnelmaa (31.8. ja 1.9.2026).
 *
 * Muunnelmat eroavat juuri siinä osassa joka sovellukselle merkitsee: ensimmäisellä on
 * chat-lomake, toisella pelkkä `Next Game>>` -linkki ja kolmannella nappilomake ilman
 * chattia. Kolmas löytyi vasta laitteelta, ja se oli se jolla sivu jäi umpikujaksi.
 */
class MatchOverParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).reader().readText()

    /** Jäsennetty sivu, tai selkeä kaatuminen: jokainen testi alla olettaa sivun luetuksi. */
    private fun page(name: String) =
        checkNotNull(MatchOverParser.parse(fixture(name))) { "Sivua ei jäsennetty: $name" }

    @Test
    fun `tulos ja pisteet luetaan sivun omin sanoin`() {
        val page = page("match_over.html")

        assertEquals("pelaaja wins 2 points and the match.", page.resultText)
        assertEquals(7, page.matchLength)
        assertEquals(listOf("pelaaja", "vastapelaaja"), page.scores.map { it.player.name })
        assertEquals(listOf(7, 6), page.scores.map { it.score })
        assertEquals(listOf("90001", "91002"), page.scores.map { it.player.userId })
    }

    @Test
    fun `tapahtuma kierros ja ottelutunniste luetaan`() {
        val page = page("match_over.html")

        assertEquals("Slower Sevens #4562", page.eventName)
        assertEquals("/bg/event/900006", page.eventPath)
        assertEquals("Round 3", page.roundLabel)
        assertEquals("7000006", page.matchId?.value)
    }

    @Test
    fun `sivun omat linkit luetaan eika koota`() {
        val page = page("match_over.html")

        assertEquals("/bg/nextgame", page.nextGamePath)
        assertEquals("/bg/game/7000006/1/list#end", page.reviewPath)
        assertEquals("/bg/nextgame?skip=7000006", page.skipPath)
    }

    @Test
    fun `chat-muunnelmassa Next Game on lomakkeen nappi eika linkki`() {
        val page = page("match_over_chat.html")

        assertNull(page.nextGamePath)
        // Sama lomake jonka ChatParser lukee: kaksi lukijaa samasta asiasta olisi kaksi
        // totuutta, joten tama tarkistaa etta toinen lukija kelpaa tallakin sivulla.
        val chat = checkNotNull(ChatParser.parse(fixture("match_over_chat.html")))
        assertEquals("vastapelaaja", chat.opponentName)
        assertEquals(listOf("Next Game", "To Top"), chat.form?.submits)
        assertEquals("chat", chat.form?.field)
        assertEquals(mapOf("commit" to "1"), chat.form?.hiddenFields)
        assertNull(chat.incoming)
    }

    @Test
    fun `ennakoitu tulos merkitaan sivun oman merkinnan mukaan`() {
        assertTrue(page("match_over_chat.html").predicted)
        assertFalse(page("match_over.html").predicted)
    }

    @Test
    fun `nappimuunnelmassa Next Game ja To Top luetaan lomakkeesta`() {
        val page = page("match_over_buttons.html")
        val form = checkNotNull(page.form) { "Nappilomaketta ei luettu" }

        // Sivulla ei ole tekstikenttää lainkaan, joten chat-lukija ei näe näitä nappeja.
        assertNull(ChatParser.parse(fixture("match_over_buttons.html"))?.form)
        assertNull(page.nextGamePath)
        assertEquals(listOf("Next Game", "To Top"), form.submits)
        assertEquals("/bg/move/7000021/1491", form.action)
        assertEquals(FormMethod.POST, form.method)
        assertEquals(mapOf("commit" to "1"), form.hiddenFields)
    }

    @Test
    fun `nappi lahtee piilokenttineen ja vain sivun tarjoamana`() {
        val form = checkNotNull(page("match_over_buttons.html").form)

        val submission = checkNotNull(form.press("Next Game"))
        assertEquals("/bg/move/7000021/1491", submission.action)
        assertEquals(FormMethod.POST, submission.method)
        // `commit=1` mukana: ilman sitä sivusto vastaa 200 eikä tee mitään (22.8.2026).
        assertEquals(mapOf("commit" to "1", "submit" to "Next Game"), submission.fields)
        // Nappia jota sivu ei tarjoa ei voi painaa.
        assertNull(form.press("Roll Dice"))
    }

    @Test
    fun `chat-muunnelmassa nappeja ei lueta kahdesti`() {
        // Napit ovat chat-lomakkeen omia, joten tämä lukija jättää ne rauhaan.
        assertNull(page("match_over_chat.html").form)
    }

    @Test
    fun `lauta ei ole paattymissivu`() {
        assertNull(MatchOverParser.parse(fixture("move_board.html")))
    }

    /**
     * Neljäs muunnelma (6.9.2026): tuplauksen hylkääminen päätti pelin, ottelu jatkuu ja
     * sivulla ei ole lautaa. Tämä sivu jäi ennen korjausta umpikujaksi, ks. `MatchOverParser`.
     */
    @Test
    fun `pelin paattymissivu luetaan ja ottelu jatkuu`() {
        val page = page("game_over_no_board.html")

        assertTrue(page.matchContinues)
        assertEquals("winner wins 1 point.", page.resultText)
        assertEquals("5300001", page.matchId?.value)
        assertEquals("Weekday Warriors #4000", page.eventName)
        assertEquals("Round 6", page.roundLabel)
        assertEquals(5, page.matchLength)
        assertEquals(listOf("winner", "reader"), page.scores.map { it.player.name })
        assertEquals(listOf(4, 2), page.scores.map { it.score })
    }

    @Test
    fun `pelin paattymissivun ainoa nappi on Next`() {
        val form = checkNotNull(page("game_over_no_board.html").form) { "Lomaketta ei luettu" }

        assertEquals(listOf("Next"), form.submits)
        assertEquals("/bg/move/5300001/681", form.action)
        assertEquals(FormMethod.GET, form.method)
        // Sivun omat linkit ovat samat kuin ottelun lopussa, eli tie eteenpäin ei ole yksi.
        assertEquals("/bg/game/5300001/0/list#end", page("game_over_no_board.html").reviewPath)
        assertEquals("/bg/nextgame?skip=5300001", page("game_over_no_board.html").skipPath)
    }

    @Test
    fun `ottelun loppu ei ole pelin loppu`() {
        assertFalse(page("match_over.html").matchContinues)
        // Predikaatti on yhä ottelun oma: pelin päättyminen ei saa laueta siihen.
        assertFalse(DgPages.isMatchOverPage(fixture("game_over_no_board.html")))
    }

    @Test
    fun `viestin kantava paattymissivu antaa napit lomakkeesta`() {
        // Neljäs muunnelma (7.9.2026): viesti ilman kenttää. Napit ovat sivun lomakkeessa
        // kuten nappimuunnelmassa, koska chat-kortilla ei ole lomaketta jolla ne tulisivat.
        val page = checkNotNull(page("match_over_says.html"))
        assertEquals("pelaaja wins 1 point and the match.", page.resultText)
        assertEquals(listOf("Next Game", "To Top"), page.form?.submits)
        assertEquals(mapOf("commit" to "1"), page.form?.hiddenFields)
    }
}

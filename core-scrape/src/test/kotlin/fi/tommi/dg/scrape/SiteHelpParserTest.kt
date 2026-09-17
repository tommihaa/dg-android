package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Fixture on typistetty aidosta sivusta (`raakasivut/site_help.html`, 29.8.2026):
 * rakenne ja ansat ovat sivulta, vastaustekstit lyhennettyjä. Koko sivun mitta on
 * todennettu jäsennintä kirjoitettaessa raakasivua vasten: 9 osastoa ja 67 kysymystä,
 * eli sama määrä kuin sivun `h3`-elementtejä.
 */
class SiteHelpParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val page = SiteHelpParser.parse(fixture("site_help.html"))!!

    @Test
    fun `osastot tulevat rungon h2-otsikoista ja tyhja jaa pois`() {
        // "Frequently Asked Questions" on sisällysluettelon otsikko eikä sen alla ole
        // yhtään kysymystä, joten se ei ole osasto.
        assertEquals(
            listOf("About DailyGammon", "The Games", "Troubleshooting"),
            page.sections.map { it.title },
        )
    }

    @Test
    fun `kysymys ja vastaus luetaan ankkurimuodosta`() {
        val entry = page.sections[0].entries[0]
        assertEquals("What is DailyGammon?", entry.question)
        assertTrue(entry.answer.startsWith("A turn-based backgammon site."))
        // Vastaus päättyy Top-paluulinkkiin: linkin teksti ei ole vastausta.
        assertFalse(entry.answer.contains("Top"))
    }

    @Test
    fun `luettelo muuttuu riveiksi`() {
        val entry = page.sections[0].entries[1]
        val lines = entry.answer.lines()
        assertEquals(3, lines.count { it.startsWith("• ") })
        assertTrue(lines.any { it == "• There are no advertisements." })
    }

    @Test
    fun `vastauksen taulukko litistyy riveiksi eika katoa`() {
        val entry = page.sections[1].entries.single()
        assertTrue(entry.answer.contains("byte# 1 2"))
        assertTrue(entry.answer.contains("Each byte becomes one roll."))
    }

    @Test
    fun `valilyonnein kirjoitettu ankkuri ja lihavoinnin rikkoma virke kestavat`() {
        // Troubleshooting-osaston muoto on <a name="pw"> <h3>...</h3> </a> ja tekstissä
        // on <b>-elementtejä keskellä virkettä; molemmat ovat sivun omaa kirjoitusasua.
        val entry = page.sections[2].entries.single()
        assertEquals("What do I do if I forget my password?", entry.question)
        assertTrue(entry.answer.startsWith("Send your user name"))
    }

    @Test
    fun `sulkematon ankkuri ei niela vastauksen alkua`() {
        // Sivun oma muoto on `<a name="run"><h3>..</h3>` ilman sulkevaa tagia, ja Jsoup
        // sulkee ankkurin vasta vastauksen ensimmäiseen linkkiin. Väliin jäävä lainausmerkki
        // on ankkurin lapsi, ja se katosi hiljaa laitteella 29.8.2026 asti: ruudulla luki
        // `Admin", aka` vaikka sivulla lukee `"Admin", aka`.
        val entry = page.sections[0].entries[2]
        assertEquals("Who runs the site?", entry.question)
        assertTrue(entry.answer.startsWith("\"Admin\", aka the site programmer"), entry.answer)
    }

    @Test
    fun `osaston hanta ei tartu viimeiseen vastaukseen`() {
        // Sivun päivitysrivi seuraa viimeistä Top-linkkiä eikä kuulu kysymykseen.
        val entry = page.sections[2].entries.single()
        assertFalse(entry.answer.contains("Updated November"))
    }
}

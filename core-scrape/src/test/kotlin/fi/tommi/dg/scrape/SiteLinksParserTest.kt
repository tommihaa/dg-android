package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Fixture on typistetty aidosta sivusta (`raakasivut/site_links.html`, 29.8.2026):
 * rakenne ja ansat sivulta, nimet ja osoitteet keksittyjä paitsi ensimmäinen rivi.
 */
class SiteLinksParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val page = SiteLinksParser.parse(fixture("site_links.html"))!!

    @Test
    fun `navigointipalkin taulukko ei ole osasto`() {
        // Palkilla ei ole edeltävää h2:ta, joten sen /bg/-linkit eivät päädy listalle.
        assertEquals(
            listOf("Sites Related to DailyGammon", "Recommended Reading"),
            page.sections.map { it.title },
        )
    }

    @Test
    fun `rivi luetaan linkkeineen ja selitteineen`() {
        val first = page.sections[0].links.single()
        assertEquals("Helpful Hints for New Players", first.title)
        assertEquals("http://www.dailygammon.com/help/hints.html", first.url)
        assertEquals("by Emma and Jadzia", first.note)
    }

    @Test
    fun `rikkinaiset rivit, kommenttirivit ja linkiton rivi putoavat`() {
        // Sivulla on sisäkkäinen <tr><tr>, kaksi kommentteihin piilotettua riviä ja
        // logorivi ilman linkkiä. Jäljelle jäävät tasan oikeat kolme.
        assertEquals(
            listOf("Glossary", "Play Server", "Free Program"),
            page.sections[1].links.map { it.title },
        )
    }

    @Test
    fun `selitteen rivinvaihto litistyy tekstiksi`() {
        assertEquals(
            "A real-time server with a second line in the note.",
            page.sections[1].links[1].note,
        )
    }

    @Test
    fun `selite voi puuttua`() {
        val row = SiteLinksParser.parse(
            // Otsikko on mukana koska se on se mikä tekee sivusta linkkisivun
            // (`DgPages.isSiteLinksPage`).
            "<title>Dailygammon Links</title>" +
                "<h2>X</h2><table><tr><td><a href=\"http://a.example/\">A</a></td></tr></table>"
        )!!.sections.single().links.single()
        assertNull(row.note)
    }
}

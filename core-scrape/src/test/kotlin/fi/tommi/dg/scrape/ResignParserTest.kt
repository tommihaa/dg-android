package fi.tommi.dg.scrape

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.MatchId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResignParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader().readText()

    private val page = ResignParser.parse(fixture("resign_page.html"))!!

    @Test
    fun `lomake ja napit luetaan sivulta`() {
        assertEquals("/bg/resign/doit", page.action)
        assertEquals(FormMethod.POST, page.method)
        assertEquals("reellysure", page.confirmField)
        assertEquals("Yes! I really want to resign.", page.confirmLabel)
        assertEquals("Resign Selected Matches", page.submitLabel)
    }

    @Test
    fun `rivin valintaruudun nimi on ottelun tunnus`() {
        assertEquals(3, page.rows.size)
        page.rows.forEach { row -> assertEquals(row.match.id.value, row.field) }
        assertEquals("March 26 Deja Vu", page.rows[0].match.eventName)
        assertEquals("ekavastus", page.rows[0].match.opponent.name)
        assertEquals("3/8", page.rows[0].match.round)
        assertEquals(11, page.rows[0].match.matchLength)
    }

    @Test
    fun `ystavyysottelun rivi luetaan ilman tapahtumalinkkia`() {
        // Sama muoto kuin profiililla: tapahtuma tekstina, ajat viivoina.
        val friendly = page.rows[1]
        assertEquals("nack", friendly.match.eventName)
        assertNull(friendly.match.eventPath)
        assertEquals(1, friendly.match.matchLength)
        assertEquals("5319548", friendly.field)
    }

    @Test
    fun `lahetys kantaa valitut ruudut ja varmistuksen sivun nimilla`() {
        val submission = page.write(setOf(MatchId("5290918"), MatchId("5320443")))!!
        assertEquals("/bg/resign/doit", submission.action)
        assertEquals(
            mapOf("5290918" to "on", "5320443" to "on", "reellysure" to "on"),
            submission.fields,
        )
    }

    @Test
    fun `tyhja valinta tai vieras tunnus ei tuota lahetysta`() {
        assertNull(page.write(emptySet()))
        assertNull(page.write(setOf(MatchId("999"))))
    }

    /**
     * Kuittaussivu, mitattu 4.9.2026 illalla. Kolme asiaa yhdessa testissa, koska ne ovat
     * saman havainnon puolia: sivu ei ole luovutussivu, se tunnistuu otsikosta, ja luku
     * luetaan lauseesta eika sanoista.
     */
    @Test
    fun `kuittaussivulta luetaan sivuston oma lukema`() {
        val html = fixture("resign_done.html")

        assertNull(ResignParser.parse(html), "Kuittaussivulla ei ole lomaketta")
        assertTrue(DgPages.isResignDonePage(html))

        val outcome = checkNotNull(ResignParser.resigned(html))
        assertEquals(1, outcome.count)
        assertEquals("1 match resigned.", outcome.notice)
    }

    @Test
    fun `luovutussivu itse ei ole kuittaus`() {
        assertNull(ResignParser.resigned(fixture("resign_page.html")))
        assertNull(ResignParser.resigned(fixture("top_page.html")))
    }

    @Test
    fun `muu sivu ei ole luovutussivu`() {
        assertNull(ResignParser.parse(fixture("profile_page_other.html")))
        assertNull(ResignParser.parse(fixture("top_page.html")))
    }
}

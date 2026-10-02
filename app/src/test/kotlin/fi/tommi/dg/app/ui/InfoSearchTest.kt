package fi.tommi.dg.app.ui

import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.SiteHelpEntry
import fi.tommi.dg.domain.SiteHelpPage
import fi.tommi.dg.domain.SiteHelpSection
import fi.tommi.dg.net.DgResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Infon haku ja Inboxin päiväotsikot (24.9.2026). Haun väite on kaksiosainen: kaikki sanat
 * vaaditaan mutta ei järjestystä, ja `/help` noudetaan kerran eikä joka näppäilyllä.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InfoSearchTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun asetaDispatcher() = Dispatchers.setMain(dispatcher)

    @After
    fun palautaDispatcher() = Dispatchers.resetMain()

    private val app = listOf(
        InfoEntry("E.4", "How is the archive kept?", "Back up writes the archive to a file once a day."),
        InfoEntry("D.1", "How do I move?", "Tap a checker, then its target."),
    )
    private val site = SiteHelpPage(
        listOf(SiteHelpSection("Accounts", listOf(SiteHelpEntry("Forgot password?", "Write to support with your archive name.")))),
    )

    @Test
    fun kaikkiSanatVaaditaanJarjestyksestaRiippumatta() {
        val hits = searchInfo("FILE archive", app, null)
        assertEquals(listOf("E.4"), hits.map { it.number })
        assertTrue(searchInfo("archive dice", app, null).isEmpty())
    }

    @Test
    fun kysymysosumaEnnenVastausosumaaJaSivustonOhjeMukana() {
        val hits = searchInfo("archive", app, site)
        assertEquals(listOf(InfoHitSource.APP, InfoHitSource.SITE), hits.map { it.source })
        assertEquals("Accounts", hits.last().number)
    }

    @Test
    fun tyhjaHakuEiPalautaMitaan() {
        assertTrue(searchInfo("   ", app, site).isEmpty())
    }

    @Test
    fun ohjeNoudetaanKerranVaikkaLataustaPyydetaanToistuvasti() = runTest(dispatcher) {
        val requested = mutableListOf<String>()
        val fetcher = PageFetcher { path -> requested += path; DgResponse.Ok("<html></html>") }
        val model = InfoSearchViewModel(fetcher, dispatcher)
        model.load()
        model.load()
        advanceUntilIdle()
        // Tyhjä sivu ei jäsenny, joten tila on Failed ja uusinta saa noutaa uudelleen.
        assertEquals(SiteHelpSearchState.Failed, model.site.value)
        assertEquals(listOf("/help"), requested)
        model.load()
        advanceUntilIdle()
        assertEquals(2, requested.size)
    }

    @Test
    fun paivaotsikkoOnPitkaJaVuosiAinaMukana() {
        val today = LocalDate.of(2026, 9, 24)
        assertEquals("Wednesday 23 September 2026", inboxDayLabel(LocalDate.of(2026, 9, 23)))
        assertEquals("12 Sep", inboxRowDate(LocalDate.of(2026, 9, 12), today))
    }
}

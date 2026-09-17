package fi.tommi.dg.app.ui

import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.SettingsBaseline
import fi.tommi.dg.app.session.SettingsSnapshot
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.domain.SettingsUpdate
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
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

/**
 * Asetusnäkymän näkymämalli.
 *
 * HTML on synteettistä samasta syystä ja samalla varauksella kuin `BoardViewModelTest`issä:
 * jäsennystarkkuutta koskevat väitteet asuvat `core-scrape`ssa oikeita palvelintavuja
 * vasten, ja nämä testit väittävät vain haarautumista ja sitä mitä polkuja pyydettiin.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Kirjaa jokaisen pyydetyn polun, jotta testi voi väittää mitä EI haettu. */
    private class RecordingFetcher(var answer: (String) -> DgResponse) : PageFetcher {
        val requested = mutableListOf<String>()
        override fun fetch(path: String): DgResponse {
            requested += path
            return answer(path)
        }
    }

    @Before
    fun asetaDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun palautaDispatcher() {
        Dispatchers.resetMain()
    }

    /** Lähetin joka kirjaa lähetykset ja vastaa halutulla tavalla. */
    class RecordingSender(var answer: () -> DgResponse = { DgResponse.Ok("") }) : FormSender {
        val sent = mutableListOf<FormSubmission>()
        override fun send(submission: FormSubmission): DgResponse {
            sent += submission
            return answer()
        }
    }

    /** Lähtötilan säilö muistissa, jotta testi ei tarvitse Androidia. */
    class FakeBaseline : SettingsBaseline {
        var snapshot: SettingsSnapshot? = null
        override fun get(): SettingsSnapshot? = snapshot
        override fun captureOnce(
            checked: Set<String>,
            radios: Map<String, String>,
            now: Long,
        ): Boolean {
            if (snapshot != null) return false
            snapshot = SettingsSnapshot(checked, radios, now)
            return true
        }
    }

    /** Laitteen kopio lauta-asetuksista. Sama olio koko testin ajan, jotta sitä voi väittää. */
    private val siteSettings = FakeSiteSettings()

    private fun malli(
        fetcher: RecordingFetcher,
        sender: RecordingSender = RecordingSender(),
        baseline: SettingsBaseline = FakeBaseline(),
    ) = SettingsViewModel(
        fetcher,
        sender,
        baseline,
        siteSettings,
        io = dispatcher,
        clock = { 1000L },
    )

    // --- Lukutila ---

    @Test
    fun `asetusnakyma hakee vain kuluttamattoman vakio-osoitteen`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(ASETUSSIVU) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        // Sama väitteen muoto kuin kahdessa muussa näkymämallissa, jolloin kunkin
        // polkulista on täydellinen lausuma omasta luokastaan.
        assertEquals(
            listOf(DgPages.SETTINGS_PATH, DgPages.SETTINGS_PATH),
            fetcher.requested,
        )
    }

    @Test
    fun `asetusnakyma ei hae mitaan kuluttavaa`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(ASETUSSIVU) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        assertTrue(
            fetcher.requested.none {
                it.contains("nextgame") || it.contains("submit=") ||
                    it.contains("move=") || it.contains("skip=")
            }
        )
    }

    @Test
    fun `onnistunut luku paivittaa laitteen kopion lauta-asetuksista`() {
        // Tämä on se kytkentä joka oli aiemmin valinnainen (`SiteSettingsStore? = null`).
        // Ilman sitä lauta piirtyisi vanhoilla asetuksilla eikä mikään kertoisi siitä, joten
        // väite on tässä eikä pelkässä konstruktorin tyypissä.
        val fetcher = RecordingFetcher { DgResponse.Ok(ASETUSSIVU) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(1, siteSettings.saved.size)
        assertEquals(true, siteSettings.get().hidePips)
        assertEquals(BoardScheme.BLUE_WHITE, siteSettings.get().scheme)
    }

    // --- Sivu ei ollutkaan asetussivu ---

    @Test
    fun `vaara sivu ei nayta tyhjia asetuksia`() {
        // Tämä on ruudun kantava väite. SettingsParser.parse ei kieltäydy väärästä sivusta
        // vaan palauttaa siitä tyhjän tuloksen, ja tyhjä tulos ruudulla väittäisi että
        // kaikki asetukset ovat pois päältä. Tunnistus erottaa ne toisistaan.
        val fetcher = RecordingFetcher { DgResponse.Ok(EI_ASETUKSIA) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(SettingsUiState.NotSettingsPage, malli.state.value)
    }

    @Test
    fun `asetussivu luetaan valintoineen`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(ASETUSSIVU) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value
        assertTrue("Asetuksia ei luettu", tila is SettingsUiState.Loaded)
        val sivu = (tila as SettingsUiState.Loaded).page
        assertEquals(setOf("6"), sivu.checked)
        assertEquals("Blue/White", sivu.choices.single().selectedLabel)
    }

    // --- Verkko ja istunto ---

    @Test
    fun `verkkovirheet erottuvat toisistaan`() {
        val offline = malli(RecordingFetcher { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val palvelin = malli(RecordingFetcher { DgResponse.ServerError(503) })

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(SettingsUiState.Failed(Failure.Offline), offline.state.value)
        assertEquals(SettingsUiState.Failed(Failure.Server(503)), palvelin.state.value)
    }

    @Test
    fun `katkennut istunto ei poista tunnuksia taalla`() {
        val malli = malli(RecordingFetcher { DgResponse.AuthFailed })

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(SettingsUiState.SessionExpired, malli.state.value)
    }

    @Test
    fun `epaonnistunut paivitys ei havita nakyvia asetuksia`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(ASETUSSIVU) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            fetcher.answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
            malli.refresh()
            advanceUntilIdle()
        }

        val tila = malli.state.value
        assertTrue("Asetukset katosivat ruudulta", tila is SettingsUiState.Loaded)
        assertEquals(false, (tila as SettingsUiState.Loaded).refreshing)
    }

    // --- Kirjoitustila (25.8.2026) ---

    /**
     * Lukee ennen ja jälkeen, ja vasta täsmäävä paluuluku muuttaa ruudun tilan.
     *
     * Kolme lukua yhtä tallennusta kohti on tämän ruudun sopimus eikä varovaisuutta, joten
     * testi väittää lukujen määrän eikä vain lopputulosta.
     */
    @Test
    fun `onnistunut tallennus lukee ennen ja jalkeen ja paivittaa tilan`() {
        var vastaus = ASETUSSIVU
        val fetcher = RecordingFetcher { DgResponse.Ok(vastaus) }
        val sender = RecordingSender { vastaus = ASETUSSIVU_ILMAN_RASTIA; DgResponse.Ok("") }
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.save(checked = emptySet(), radios = mapOf("board" to "1"))
            advanceUntilIdle()
        }

        assertEquals(SettingsSaveResult.Saved, malli.saveResult.value)
        assertEquals(1, sender.sent.size)
        // Rastittamaton kenttä ei ole lähetyksessä lainkaan, ei myöskään arvolla "off".
        assertEquals(mapOf("board" to "1"), sender.sent.single().fields)
        // Kolme lukua: alkulataus, lähetystä edeltävä ja paluuluku.
        assertEquals(3, fetcher.requested.size)
        assertTrue(fetcher.requested.all { it == DgPages.SETTINGS_PATH })
        val tila = malli.state.value as SettingsUiState.Loaded
        assertTrue("Ruudun tila ei seurannut paluulukua", tila.page.checked.isEmpty())
    }

    @Test
    fun `paluuluku joka ei tasmaa on Mismatch eika ruudun tila muutu`() {
        // Vaarallisin haara: lähetys lähti, mutta sivustolla lukee jotain muuta kuin
        // lähetettiin. Ruutu ei saa väittää tallennusta onnistuneeksi.
        val fetcher = RecordingFetcher { DgResponse.Ok(ASETUSSIVU) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.save(checked = emptySet(), radios = mapOf("board" to "1"))
            advanceUntilIdle()
        }

        assertEquals(SettingsSaveResult.Mismatch, malli.saveResult.value)
        val tila = malli.state.value as SettingsUiState.Loaded
        assertEquals(setOf("6"), tila.page.checked)
    }

    @Test
    fun `muuttunut lomake pysayttaa ennen verkkoa eika mitaan laheteta`() {
        var vastaus = ASETUSSIVU
        val fetcher = RecordingFetcher { DgResponse.Ok(vastaus) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            // Sivu vaihtuu ruudun piirtämisen ja napin painamisen välissä.
            vastaus = ASETUSSIVU_ERI_KENTAT
            malli.save(checked = setOf("6"), radios = mapOf("board" to "1"))
            advanceUntilIdle()
        }

        val tulos = malli.saveResult.value
        assertTrue("Odotettiin vartion pysayttavan", tulos is SettingsSaveResult.Blocked)
        assertTrue(
            (tulos as SettingsSaveResult.Blocked).reason is SettingsUpdate.FormChanged,
        )
        assertTrue("Lahetys lahti vaikka ei olisi saanut", sender.sent.isEmpty())
    }

    @Test
    fun `yhteydeton lahetys ei muuta ruudun tilaa`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(ASETUSSIVU) }
        val sender = RecordingSender { DgResponse.Offline("ei verkkoa") }
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.save(checked = emptySet(), radios = mapOf("board" to "1"))
            advanceUntilIdle()
        }

        assertEquals(
            SettingsSaveResult.NotSent(Failure.Offline),
            malli.saveResult.value,
        )
        assertEquals(setOf("6"), (malli.state.value as SettingsUiState.Loaded).page.checked)
    }

    @Test
    fun `lahtotila otetaan talteen vain ensimmaisesta luvusta`() {
        // Myöhempi lukema on jo sen jälkeinen tila että sovellus on voinut kirjoittaa,
        // joten sen tallentaminen tekisi varmuuskopiosta kopion mahdollisesta virheestä.
        var vastaus = ASETUSSIVU
        val fetcher = RecordingFetcher { DgResponse.Ok(vastaus) }
        val baseline = FakeBaseline()
        val malli = malli(fetcher, baseline = baseline)

        runTest(dispatcher) {
            advanceUntilIdle()
            vastaus = ASETUSSIVU_ILMAN_RASTIA
            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals(setOf("6"), baseline.snapshot?.checked)
        assertEquals(1000L, baseline.snapshot?.storedAt)
    }

    private companion object {

        /**
         * Asetussivun olennainen piirre on `pref`-lomake, koska tunnistus nojaa siihen.
         * Yksi rastitettu ruutu ja yksi radioryhmä riittävät haarautumisen väittämiseen.
         */
        val ASETUSSIVU = """
            <HTML><HEAD><TITLE>DailyGammon -- Settings for tommi</TITLE></HEAD><BODY>
            <FORM METHOD=POST ACTION=/bg/profile/pref>
            <TABLE><TR><TD><INPUT TYPE=checkbox NAME=6 CHECKED> Hide pip counts
            </TABLE>
            <H4>Board Scheme</H4>
            <TABLE><TR><TD><INPUT TYPE=radio NAME=board VALUE=0><TD>Classic
            <TR><TD><INPUT TYPE=radio NAME=board VALUE=1 CHECKED><TD>Blue/White
            </TABLE></FORM></BODY></HTML>
        """.trimIndent()

        /** Sama sivu ilman rastia, eli se miltä onnistunut muutos näyttää paluuluvussa. */
        val ASETUSSIVU_ILMAN_RASTIA = ASETUSSIVU.replace("NAME=6 CHECKED", "NAME=6")

        /** Sama lomake josta kenttä `6` puuttuu, eli renumeroitunut sivu. */
        val ASETUSSIVU_ERI_KENTAT = ASETUSSIVU.replace("NAME=6 CHECKED", "NAME=9 CHECKED")

        /** Sivu ilman asetuslomaketta, eli se mitä väärä vastaus käytännössä on. */
        const val EI_ASETUKSIA =
            "<HTML><HEAD><TITLE>DailyGammon</TITLE></HEAD>" +
                "<BODY>There are no matches where you can move.</BODY></HTML>"
    }
}

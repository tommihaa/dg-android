package fi.tommi.dg.app.ui

import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.ListedRounds
import fi.tommi.dg.app.session.CredentialsStore
import fi.tommi.dg.app.session.MatchOrderStore
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.MatchOrder
import fi.tommi.dg.domain.MatchSortKey
import fi.tommi.dg.net.DgCredentials
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TopViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Kirjaa jokaisen pyydetyn polun, jotta testi voi väittää mitä EI haettu. */
    private class RecordingFetcher(var answer: (String) -> DgResponse) : PageFetcher {
        val requested = mutableListOf<String>()
        override fun fetch(path: String): DgResponse {
            requested += path
            return answer(path)
        }
    }

    private class FakeCredentials(
        private var creds: DgCredentials? = null,
        private val saveSucceeds: Boolean = true,
    ) : CredentialsStore {
        var cleared = false
        override fun get(): DgCredentials? = creds
        override fun save(login: String, password: String): Boolean {
            if (!saveSucceeds) return false
            creds = DgCredentials(login, password)
            return true
        }

        override fun clear() {
            creds = null
            cleared = true
        }

        override fun hasCredentials(): Boolean = creds != null
    }

    @Before
    fun asetaDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun palautaDispatcher() {
        Dispatchers.resetMain()
    }

    /** Muistinvarainen järjestysvarasto: valinta ei saa vuotaa testien välillä. */
    private class FakeMatchOrder(private var order: MatchOrder = MatchOrder.SITE) : MatchOrderStore {
        override fun get(): MatchOrder = order
        override fun save(order: MatchOrder) {
            this.order = order
        }
    }

    private fun malli(
        fetcher: RecordingFetcher,
        credentials: FakeCredentials,
        matchOrder: MatchOrderStore = FakeMatchOrder(),
        listedRounds: ListedRounds = ListedRounds(),
    ) = TopViewModel(fetcher, credentials, matchOrder, listedRounds, io = dispatcher)

    @Test
    fun `jarjestys luetaan varastosta ja kirjoitetaan sinne`() {
        // Valinta muistetaan käynnistysten yli (Tommin valinta 26.8.2026), joten varasto on
        // ainoa totuus: ilman kirjoitusta ruutu näyttäisi lajitellun listan ja seuraava
        // käynnistys sivuston järjestyksen, eikä ero näkyisi mistään.
        val varasto = FakeMatchOrder(MatchOrder(MatchSortKey.OPPONENT))
        val malli = malli(RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }, FakeCredentials(), varasto)

        assertEquals(MatchOrder(MatchSortKey.OPPONENT), malli.order.value)

        malli.sortBy(MatchSortKey.OPPONENT)
        assertEquals(MatchOrder(MatchSortKey.OPPONENT, descending = true), malli.order.value)
        assertEquals(malli.order.value, varasto.get())

        malli.sortBy(MatchSortKey.OPPONENT)
        assertEquals(MatchOrder.SITE, malli.order.value)
        assertEquals(MatchOrder.SITE, varasto.get())
    }

    @Test
    fun `sovellus hakee oma-aloitteisesti vain Top Pagen`() = runTest(dispatcher) {
        // Tämä on kova sääntö eikä tyylivalinta: /bg/nextgame kuluttaa jonoa, joten
        // oma-aloitteinen haku sinne hävittäisi viestin jota kukaan ei ehtinyt nähdä.
        val fetcher = RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))

        advanceUntilIdle()
        malli.refresh()
        advanceUntilIdle()

        assertEquals(listOf("/bg/top", "/bg/top"), fetcher.requested)
        assertTrue(fetcher.requested.none { it.contains("nextgame") })
    }

    @Test
    fun `ilman tunnuksia ei tehda yhtaan pyyntoa`() = runTest(dispatcher) {
        val fetcher = RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }
        val malli = malli(fetcher, FakeCredentials(null))

        advanceUntilIdle()

        assertEquals(emptyList<String>(), fetcher.requested)
        assertTrue(malli.state.value is TopUiState.SignedOut)
    }

    @Test
    fun `onnistunut haku muistaa luettelon kierrokset laudalle`() = runTest(dispatcher) {
        // Lauta kysyy `Round 5/6`:n tästä sivun omalla tunnisteella, ks. ListedRounds.
        val muisti = ListedRounds()
        val malli = malli(
            RecordingFetcher { DgResponse.Ok(YKSI_OTTELU) },
            FakeCredentials(DgCredentials("tommi", "salasana")),
            listedRounds = muisti,
        )

        advanceUntilIdle()

        assertEquals("5/6", muisti.of(MatchId("5302842")))
        assertNull(muisti.of(MatchId("1")))
    }

    @Test
    fun `onnistunut haku tuottaa jasennetyn otteluluettelon`() = runTest(dispatcher) {
        val fetcher = RecordingFetcher { DgResponse.Ok(YKSI_OTTELU) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))

        advanceUntilIdle()

        val tila = malli.state.value as TopUiState.Loaded
        assertEquals(1, tila.page.matches.size)
        assertEquals("vastustaja", tila.page.matches.single().opponent.name)
        assertEquals(true, tila.page.matches.single().myTurn)
        assertEquals(1, tila.page.yourTurnCount)
        assertFalse(tila.refreshing)
    }

    @Test
    fun `kelpaamaton tunnus poistetaan eika sita jaada yrittamaan`() = runTest(dispatcher) {
        // Jos tunnus jäisi talteen, DgClientin hiljainen uusiminen yrittäisi sitä joka
        // pyynnöllä, ja käyttäjä näkisi vain sen ettei mikään toimi.
        val credentials = FakeCredentials(DgCredentials("tommi", "vanha"))
        val fetcher = RecordingFetcher { DgResponse.AuthFailed }
        val malli = malli(fetcher, credentials)

        advanceUntilIdle()

        val tila = malli.state.value as TopUiState.SignedOut
        assertTrue(tila.lastAttemptFailed)
        assertTrue(credentials.cleared)
        assertNull(credentials.get())
    }

    @Test
    fun `epaonnistunut paivitys ei havita jo nakyvaa listaa`() = runTest(dispatcher) {
        // Vanhentunut lista on enemmän kuin virheruutu: se on yhä oikeaa tietoa siitä
        // hetkestä jolloin se haettiin.
        val fetcher = RecordingFetcher { DgResponse.Ok(YKSI_OTTELU) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()

        fetcher.answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        malli.refresh()
        advanceUntilIdle()

        val tila = malli.state.value as TopUiState.Loaded
        assertEquals(1, tila.page.matches.size)
        assertFalse(tila.refreshing)
    }

    @Test
    fun `ensimmainen haku ilman yhteytta nayttaa virheen`() = runTest(dispatcher) {
        val fetcher = RecordingFetcher { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))

        advanceUntilIdle()

        assertEquals(TopUiState.Failed(Failure.Offline), malli.state.value)
    }

    @Test
    fun `yhteyden palaaminen hakee uudelleen kun edellinen kaatui verkkoon`() = runTest(dispatcher) {
        val fetcher = RecordingFetcher { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()
        assertEquals(TopUiState.Failed(Failure.Offline), malli.state.value)

        fetcher.answer = { DgResponse.Ok(YKSI_OTTELU) }
        malli.onNetworkAvailable()
        advanceUntilIdle()

        assertEquals(2, fetcher.requested.size)
        assertTrue(malli.state.value is TopUiState.Loaded)
    }

    @Test
    fun `yhteyden palaaminen ei hae uudelleen kun lista on jo ruudulla`() = runTest(dispatcher) {
        // Ehto on tila eikä ilmoitus: ilman sitä jokainen wifin heilahdus kuluttaisi
        // kohteen pyyntötahtia silloinkin kun ruudulla on ajantasainen lista.
        val fetcher = RecordingFetcher { DgResponse.Ok(YKSI_OTTELU) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()

        malli.onNetworkAvailable()
        advanceUntilIdle()

        assertEquals(1, fetcher.requested.size)
    }

    @Test
    fun `yhteyden palaaminen ei hae uudelleen palvelinvirheen jalkeen`() = runTest(dispatcher) {
        // Palvelimen virhe ei korjaannu siitä että laitteella on taas yhteys.
        val fetcher = RecordingFetcher { DgResponse.ServerError(503) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()

        malli.onNetworkAvailable()
        advanceUntilIdle()

        assertEquals(1, fetcher.requested.size)
    }

    @Test
    fun `palvelinvirhe sailyttaa koodin`() = runTest(dispatcher) {
        val fetcher = RecordingFetcher { DgResponse.ServerError(503) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))

        advanceUntilIdle()

        assertEquals(TopUiState.Failed(Failure.Server(503)), malli.state.value)
    }

    @Test
    fun `tyhja tunnus ei laukaise pyyntoa`() = runTest(dispatcher) {
        val fetcher = RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }
        val malli = malli(fetcher, FakeCredentials(null))
        advanceUntilIdle()

        malli.signIn(login = "  ", password = "")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), fetcher.requested)
        assertTrue((malli.state.value as TopUiState.SignedOut).lastAttemptFailed)
    }

    @Test
    fun `kirjautuminen tallettaa tunnuksen ja hakee listan`() = runTest(dispatcher) {
        val credentials = FakeCredentials(null)
        val fetcher = RecordingFetcher { DgResponse.Ok(YKSI_OTTELU) }
        val malli = malli(fetcher, credentials)
        advanceUntilIdle()

        malli.signIn("tommi", "salasana")
        advanceUntilIdle()

        assertEquals(listOf("/bg/top"), fetcher.requested)
        assertEquals("tommi", credentials.get()!!.login)
        assertTrue(malli.state.value is TopUiState.Loaded)
    }

    @Test
    fun `kirjautuminen trimmaa tunnuksen muttei salasanaa`() = runTest(dispatcher) {
        // Autofill on lisannyt tunnukseen valilyonteja (DG-foorumi 4/2021), ja sivusto
        // hylkaa sellaisen tunnuksen. Salasanaan ei kosketa: valilyonti voi olla sen aito
        // osa, ja hiljaa muutettu salasana olisi pahempi vika kuin hylatty.
        val credentials = FakeCredentials(null)
        val fetcher = RecordingFetcher { DgResponse.Ok(YKSI_OTTELU) }
        val malli = malli(fetcher, credentials)
        advanceUntilIdle()

        malli.signIn(" tommi ", " salasana ")
        advanceUntilIdle()

        assertEquals("tommi", credentials.get()!!.login)
        assertEquals(" salasana ", credentials.get()!!.password)
    }

    @Test
    fun `jos salasanaa ei saada suojattua ei kirjauduta lainkaan`() = runTest(dispatcher) {
        // Muuten seuraisi silmukka: kirjautuminen näyttäisi menevän läpi mutta unohtuisi
        // heti, eikä käyttäjä saisi tietää miksi. Pyyntöä ei myöskään lähetetä, koska
        // istunto ei olisi uusittavissa.
        val fetcher = RecordingFetcher { DgResponse.Ok(YKSI_OTTELU) }
        val malli = malli(fetcher, FakeCredentials(null, saveSucceeds = false))
        advanceUntilIdle()

        malli.signIn("tommi", "salasana")
        advanceUntilIdle()

        val tila = malli.state.value as TopUiState.SignedOut
        assertTrue(tila.storageFailed)
        assertFalse(tila.lastAttemptFailed)
        assertEquals(emptyList<String>(), fetcher.requested)
    }

    @Test
    fun `uloskirjautuminen poistaa tunnuksen`() = runTest(dispatcher) {
        val credentials = FakeCredentials(DgCredentials("tommi", "salasana"))
        val fetcher = RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }
        val malli = malli(fetcher, credentials)
        advanceUntilIdle()

        malli.signOut()

        assertTrue(credentials.cleared)
        assertEquals(TopUiState.SignedOut(), malli.state.value)
    }

    @Test
    fun `odottavan kohteen lippu valittyy nakymaan`() = runTest(dispatcher) {
        // Lippu kertoo vain että osoitteessa on jotain. Näkymä ei saa haetakaan sitä,
        // ja siksi testi tarkistaa myös ettei pyyntöä syntynyt.
        val fetcher = RecordingFetcher { DgResponse.Ok(TOP_PAGE_JOSSA_ODOTTAA) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))

        advanceUntilIdle()

        assertTrue((malli.state.value as TopUiState.Loaded).page.hasMessageNotice)
        assertEquals(listOf("/bg/top"), fetcher.requested)
    }

    @Test
    fun `tuntematon sivu ei ole tyhja lista`() = runTest(dispatcher) {
        // Tämä on koko vahdin syy. Ilman sitä TopPageParser jäsentää tämän sivun tyhjäksi
        // TopPageksi, ruutu sanoo "No matches." ja väite on väärä: sivustosta ei tiedetä
        // mitään. Havaittu laitteella 21.8.2026 kertaalleen, syy jäi tuntemattomaksi, ja
        // juuri siksi tämä tila ei erittele syytä.
        val fetcher = RecordingFetcher { DgResponse.Ok(EI_OTTELULUETTELO) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))

        advanceUntilIdle()

        assertEquals(TopUiState.NotATopPage, malli.state.value)
    }

    @Test
    fun `oteluton mutta oikea luettelo on yha luettelo`() = runTest(dispatcher) {
        // Vahdin toinen puoli, ja se on se joka voisi mennä väärin hiljaa: pelaajalla voi
        // aidosti olla nolla siirrettävää ottelua, eikä sitä saa lukea rikkinäiseksi
        // sivuksi. Taulukoihin nojaava tunnistus kaatuisi juuri tässä.
        val fetcher = RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))

        advanceUntilIdle()

        val tila = malli.state.value as TopUiState.Loaded
        assertEquals(emptyList<Any>(), tila.page.matches)
        // Nolla eikä puuttuva: rivi `0 waiting for your move` on juuri tämän tilan lukema.
        assertEquals(0, tila.page.yourTurnCount)
    }

    @Test
    fun `tuntematon sivu korvaa nakyvan listan eika sailyta sita`() = runTest(dispatcher) {
        // Ero verkkovirheeseen on tarkoituksellinen: siellä vanha lista on vanhentunutta
        // mutta oikeaa tietoa, tässä sen säilyttäminen piilottaisi juuri sen tapauksen jota
        // varten tarkistus on olemassa. Sama valinta kuin lautaruudulla.
        var vastaus = DgResponse.Ok(YKSI_OTTELU) as DgResponse
        val fetcher = RecordingFetcher { vastaus }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()
        assertEquals(1, (malli.state.value as TopUiState.Loaded).page.matches.size)

        vastaus = DgResponse.Ok(EI_OTTELULUETTELO)
        malli.refresh()
        advanceUntilIdle()

        assertEquals(TopUiState.NotATopPage, malli.state.value)
    }

    private companion object {
        /**
         * Oman profiilin linkki, ja se on **pakollinen osa jokaista top-sivua** eikä
         * koriste. `DgPages.isTopPage` tunnistaa sivun juuri tästä, koska taulukot katoavat
         * kun otteluita ei ole ja `<h2>` on olemassa myös palstalla ja asetuksissa.
         *
         * **Tämä lisättiin 21.8.2026 ja se kaatoi neljä testiä**, jotka olivat siihen asti
         * menneet läpi. Ne oli kirjoitettu synteettisellä HTML:llä josta linkki puuttui, eli
         * ne kuvasivat sivua jota sivusto ei koskaan palauta. Vika ei tullut esiin ennen kuin
         * jokin alkoi tarkistaa sivun lajia, ja se on sama muoto kuin itse vialla jota vahti
         * korjaa: kelvollisen näköinen tulos jota mikään ei kyseenalaista.
         */
        const val OMA_PROFIILI = """<a href="/bg/user/20311?days_to_view=30&active=1">tommi</a>"""

        val TYHJA_TOP_PAGE =
            "<html><body><p>Welcome to DailyGammon, tommi.</p>$OMA_PROFIILI</body></html>"

        /** Sivu joka tulee 200 OK:lla muttei ole otteluluettelo. */
        val EI_OTTELULUETTELO =
            "<html><body><h2>Something else entirely</h2><p>No list here.</p></body></html>"

        val YKSI_OTTELU = """
            <html><body>
            <p>Welcome to DailyGammon, tommi.</p>
            $OMA_PROFIILI
            <table>
              <caption>Matches where you can move:</caption>
              <tr><th>#</th><th>Event</th><th>Opponent</th></tr>
              <tr>
                <td>1</td>
                <td><a href="/bg/event/1234">Turnaus</a></td>
                <td>0:00</td>
                <td>278:29</td>
                <td>5/6</td>
                <td>9</td>
                <td><a href="/bg/user/9999">vastustaja</a></td>
                <td><a href="/bg/move/5302842/541">Play</a></td>
                <td><a href="/bg/game/5302842/0/list">Review</a></td>
              </tr>
            </table>
            </body></html>
        """.trimIndent()

        val TOP_PAGE_JOSSA_ODOTTAA = """
            <html><body>
            <p>Welcome to DailyGammon, tommi.</p>
            $OMA_PROFIILI
            <a href="/bg/nextgame">You have Messages!</a>
            </body></html>
        """.trimIndent()
    }
    /**
     * Vanhentuneet ottelurivit piiloon haun ajaksi (Tommin paatos 4.9.2026).
     *
     * Havainto oli etta ottelulistan tyhjennyttya ruutu naytti hetken ne ottelut jotka oli
     * juuri pelattu. Lippu on siis se ero jonka kutsuja tietaa ja luettelo ei: lauta palasi
     * Top Pagella, joten lista on vanhentunut jo haun alkaessa.
     */
    @Test
    fun `tiedossa vanhentuneet rivit piiloon vain haun ajaksi`() = runTest(dispatcher) {
        val fetcher = RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()

        malli.refresh(matchesKnownStale = true)
        val kesken = malli.state.value as TopUiState.Loaded
        assertTrue("Rivien pitaisi olla piilossa haun ajan", kesken.matchesStale)
        assertTrue(kesken.refreshing)

        advanceUntilIdle()
        val valmis = malli.state.value as TopUiState.Loaded
        assertFalse("Tuore vastaus nollaa lipun", valmis.matchesStale)
    }

    @Test
    fun `kasin painettu Refresh ei piilota riveja`() = runTest(dispatcher) {
        // Silloin lista voi olla ajan tasalla, eika sen pyyhkiminen olisi rauhallisempaa
        // vaan pelkka valahdys.
        val fetcher = RecordingFetcher { DgResponse.Ok(TYHJA_TOP_PAGE) }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()

        malli.refresh()

        assertFalse((malli.state.value as TopUiState.Loaded).matchesStale)
    }

    @Test
    fun `kaatunut haku palauttaa rivit nakyviin`() = runTest(dispatcher) {
        // Ilman nollausta rivit jaisivat piiloon siihen asti etta jokin haku onnistuu, ja
        // ruudulla olisi pelkka ylaosa ilman tieta eteenpain.
        var vastaus: DgResponse = DgResponse.Ok(TYHJA_TOP_PAGE)
        val fetcher = RecordingFetcher { vastaus }
        val malli = malli(fetcher, FakeCredentials(DgCredentials("tommi", "salasana")))
        advanceUntilIdle()

        vastaus = DgResponse.Offline("verkko poikki")
        malli.refresh(matchesKnownStale = true)
        advanceUntilIdle()

        val tila = malli.state.value as TopUiState.Loaded
        assertFalse("Kaatunut haku ei saa jattaa riveja piiloon", tila.matchesStale)
        assertFalse(tila.refreshing)
    }

}

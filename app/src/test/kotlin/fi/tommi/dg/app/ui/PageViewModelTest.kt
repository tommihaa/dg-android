package fi.tommi.dg.app.ui

import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.domain.InviteChoice
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.MessageSource
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Porautumisruudun näkymämalli, ja ennen kaikkea sen ainoa varsinainen väite: **sivun laji
 * luetaan vastauksesta eikä pyydetystä osoitteesta.** Sama polku voi siis palauttaa minkä
 * tahansa näistä sivuista, ja tuntematon vastaus on oma tilansa eikä tyhjä sisältö.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PageViewModelTest {

    private val dispatcher = StandardTestDispatcher()

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

    /**
     * Kirjaa jokaisen lahetetyn lomakkeen, jotta testi voi vaittaa mita EI lahetetty.
     * Sama muoto ja sama peruste kuin `MessagesViewModelTest`illa.
     */
    private class RecordingSender(var answer: (FormSubmission) -> DgResponse) : FormSender {
        val sent = mutableListOf<FormSubmission>()
        override fun send(submission: FormSubmission): DgResponse {
            sent += submission
            return answer(submission)
        }
    }

    private val dao = FakeArchive()
    private val merkit = FakeMarks()

    private fun malli(html: String, path: String = POLKU): Pair<PageViewModel, RecordingFetcher> {
        val fetcher = RecordingFetcher { DgResponse.Ok(html) }
        return malli(fetcher, path) to fetcher
    }

    private fun malli(
        fetcher: RecordingFetcher,
        path: String = POLKU,
        sender: RecordingSender = RecordingSender { DgResponse.Ok(KUITTAUS) },
    ) = PageViewModel(
        pages = fetcher,
        forms = sender,
        archive = dao,
        path = path,
        self = { OMA_NIMI },
        marks = merkit,
        io = dispatcher,
        now = { HETKI },
    )

    private fun paattynyt(malli: PageViewModel) =
        ((malli.state.value as PageUiState.Loaded).content as PageContent.Profile)
            .page.finishedMatches.single()

    @Test
    fun `paattyneen ottelun vienti haetaan rivin omasta linkista ja nimetaan tunnuksella`() {
        val fetcher = RecordingFetcher { path ->
            if (path == VIENTIPOLKU) DgResponse.Ok(MAT) else DgResponse.Ok(PROFIILI_PAATTYNYT)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.export(paattynyt(malli))
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU, VIENTIPOLKU), fetcher.requested)
        val ready = malli.export.value as ExportUiState.Ready
        assertEquals("dg-7000034.mat", ready.fileName)
        assertEquals(MAT, ready.text)

        // Kuittaus palauttaa lepotilaan, jotta valikko ei avaudu toista kertaa.
        malli.exportConsumed()
        assertEquals(ExportUiState.Idle, malli.export.value)
    }

    @Test
    fun `sgf-vienti kirjoittaa tiedoston uudestaan ja liittaa ottelun merkit`() {
        // Tommin päätös 15.9.2026: merkitty asema kulkee GNU:n `.sgf`:ssä. Merkki luetaan
        // kannasta ottelun numerolla; toisen ottelun merkki ei tule mukaan.
        val fetcher = RecordingFetcher { path ->
            when (path) {
                VIENTIPOLKU -> DgResponse.Ok(MAT)
                TURNAUSPOLKU -> DgResponse.Ok(TURNAUS_BACKGAMMON)
                else -> DgResponse.Ok(PROFIILI_PAATTYNYT)
            }
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            merkit.add(GameKey(MatchId("7000034"), opponentScore = 0, selfScore = 0), 12, "hit or run?", HETKI)
            merkit.add(GameKey(MatchId("999"), opponentScore = 0, selfScore = 0), 3, "toinen ottelu", HETKI)
            advanceUntilIdle()
            malli.export(paattynyt(malli), ExportFormat.SGF)
            advanceUntilIdle()
        }

        // Turnaussivu haetaan rivin omasta linkistä muunnelmaa varten, viennin jälkeen.
        assertEquals(listOf(POLKU, VIENTIPOLKU, TURNAUSPOLKU), fetcher.requested)
        val ready = malli.export.value as ExportUiState.Ready
        assertEquals("dg-7000034.sgf", ready.fileName)
        assertTrue(ready.text, ready.text.startsWith("(;FF[4]GM[6]"))
        assertTrue(ready.text, ready.text.contains("MI[length:15][game:0][ws:0][bs:0]PW[pelaaja]PB[vastapelaaja32]"))
        assertTrue(ready.text, ready.text.contains("RU[Crawford]"))
        assertTrue(ready.text, ready.text.contains(";W[41xwmi]C[MARK Move 12: hit or run?]"))
        assertTrue(!ready.text.contains("toinen ottelu"))
    }

    @Test
    fun `double repeat -turnauksen sgf ei vaita Crawfordia`() {
        // Double repeat luopuu Crawfordista (SUBSTANSSI 8), eikä `.mat` kerro muunnelmaa.
        // Turnaussivun `Game`-ehto on ainoa varma lähde, ja ilman sitä DR-ottelu jossa
        // tuplausta ei otettu vastaan kirjoitettiin väärällä säännöllä (AVOIMET, 17.9.2026).
        val fetcher = RecordingFetcher { path ->
            when (path) {
                VIENTIPOLKU -> DgResponse.Ok(MAT)
                TURNAUSPOLKU -> DgResponse.Ok(TURNAUS)
                else -> DgResponse.Ok(PROFIILI_PAATTYNYT)
            }
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.export(paattynyt(malli), ExportFormat.SGF)
            advanceUntilIdle()
        }

        val ready = malli.export.value as ExportUiState.Ready
        assertTrue(ready.text, ready.text.startsWith("(;FF[4]GM[6]"))
        assertTrue(ready.text, !ready.text.contains("RU[Crawford]"))
    }

    @Test
    fun `sgf-vientia ei kirjoiteta jos turnaussivu ei tule`() {
        // Säännön puuttuessa ei arvata: väärällä `RU`-lipulla kirjoitettu tiedosto olisi
        // huonompi kuin virhe josta voi yrittää uudestaan.
        val fetcher = RecordingFetcher { path ->
            when (path) {
                VIENTIPOLKU -> DgResponse.Ok(MAT)
                TURNAUSPOLKU -> DgResponse.ServerError(503)
                else -> DgResponse.Ok(PROFIILI_PAATTYNYT)
            }
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.export(paattynyt(malli), ExportFormat.SGF)
            advanceUntilIdle()
        }

        assertEquals(ExportUiState.Failed(Failure.Server(503)), malli.export.value)
    }

    @Test
    fun `sgf-vientia ei kirjoiteta jos tiedosto ei jasenny`() {
        // Tiedosto on `.mat`-otsikoltaan kelvollinen mutta ilman yhtään peliä.
        val fetcher = RecordingFetcher { path ->
            if (path == VIENTIPOLKU) DgResponse.Ok(" 15 point match\n") else DgResponse.Ok(PROFIILI_PAATTYNYT)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.export(paattynyt(malli), ExportFormat.SGF)
            advanceUntilIdle()
        }

        assertEquals(ExportUiState.NotConvertible, malli.export.value)
    }

    @Test
    fun `sgf-vientia ei kirjoiteta jos peli alkaa asetetusta asemasta`() {
        // Mitattu 16.9.2026: nackgammonin `.mat` alkaa asemarivillä, ja `.sgf` tavallisesta
        // aloitusasemasta näyttäisi GNU:ssa laittomia siirtoja. Tila on oma, jotta teksti
        // sanoo syyn eikä väitä ettei tiedosto jäsentynyt.
        val nack = """
            | 7 point match
            |
            | Game 1
            | pelaaja : 0                         vastapelaaja32 : 0
            |  1) 12: Illegal play (7;0;1;0;0;a;b;0;0;0;1;0;0;-2;-2;0;0;0;4;0;3;0;0;0;-4;4;0;0;0;-3;0;-4;0;0;0;2;2;0;5;1;) 51: 24/23 23/18
            |  2) 54: 24/20 23/18                43: 24/20 23/20
            |
        """.trimMargin()
        val fetcher = RecordingFetcher { path ->
            if (path == VIENTIPOLKU) DgResponse.Ok(nack) else DgResponse.Ok(PROFIILI_PAATTYNYT)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.export(paattynyt(malli), ExportFormat.SGF)
            advanceUntilIdle()
        }

        assertEquals(ExportUiState.SetupPosition, malli.export.value)
    }

    @Test
    fun `sivuvastaus vientiosoitteesta ei ole tiedosto eika sita jaeta`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PROFIILI_PAATTYNYT) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.export(paattynyt(malli))
            advanceUntilIdle()
        }

        assertEquals(ExportUiState.NotAnExport, malli.export.value)
    }

    @Test
    fun `ottelu jota profiililla ei ole ei tuota pyyntoa`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PROFIILI_PAATTYNYT) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.export(paattynyt(malli).copy(id = MatchId("999")))
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU), fetcher.requested)
        assertEquals(ExportUiState.Idle, malli.export.value)
    }

    @Test
    fun `profiili tunnistetaan sisallosta`() {
        val (malli, fetcher) = malli(PROFIILI)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(listOf(POLKU), fetcher.requested)
        val content = (malli.state.value as PageUiState.Loaded).content
        assertEquals("pelaaja", (content as PageContent.Profile).page.player.name)
    }

    @Test
    fun `sama polku voi palauttaa turnaussivun ja se luetaan sellaisena`() {
        // Polku on täsmälleen sama kuin edellisessä testissä. Jos laji pääteltäisiin
        // osoitteesta, tämä sivu jäsentyisi profiiliksi ja näyttäisi tyhjältä.
        val (malli, _) = malli(TURNAUS)

        runTest(dispatcher) { advanceUntilIdle() }

        val content = (malli.state.value as PageUiState.Loaded).content
        assertEquals("Sample Deja Vu", (content as PageContent.Event).page.name)
    }

    @Test
    fun `pelaajan turnauslista tunnistetaan otsikosta`() {
        val (malli, _) = malli(TURNAUSLISTA)

        runTest(dispatcher) { advanceUntilIdle() }

        val content = (malli.state.value as PageUiState.Loaded).content
        assertEquals("pelaaja", (content as PageContent.Tournaments).page.player.name)
        assertEquals(1, content.page.rows.size)
    }

    @Test
    fun `tournament hall tunnistetaan captioneista`() {
        // Halli on porautumissivu 3.9.2026 alkaen (oli Loungen segmentti).
        val (malli, _) = malli(THALL)

        runTest(dispatcher) { advanceUntilIdle() }

        val content = (malli.state.value as PageUiState.Loaded).content
        assertEquals("Uusi turnaus", (content as PageContent.Hall).page.active.single().name)
        assertEquals("voittaja", content.page.finished.single().winner?.name)
    }

    @Test
    fun `tuntematon sivu on oma tilansa eika tyhja sisalto`() {
        val (malli, _) = malli("<html><body>jotain muuta</body></html>")

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(PageUiState.NotAKnownPage, malli.state.value)
    }

    @Test
    fun `katkennut istunto ei jasenny miksikaan`() {
        val fetcher = RecordingFetcher { DgResponse.AuthFailed }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(PageUiState.SessionExpired, malli.state.value)
    }

    @Test
    fun `refresh hakee saman polun uudelleen eika pyyhi sisaltoa ruudulta`() {
        val (malli, fetcher) = malli(PROFIILI)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.refresh()
            // Tarkoituksella kesken latauksen: sisällön pitää olla yhä ruudulla.
            assertTrue((malli.state.value as PageUiState.Loaded).refreshing)
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU, POLKU), fetcher.requested)
    }

    // --- Viestin lahetys profiililta: ruudun ainoa teko ---

    @Test
    fun `viestia ei voi lahettaa sivulta jolla ei ole lomaketta`() {
        // Vahvin vaite jonka tama osa voi tehda: teko on portitettu sivun sisallolla eika
        // ruudun kurinalaisuudella. Turnaussivulla ei ole viestilomaketta lainkaan.
        val sender = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(TURNAUS) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onDraftChange("moi")
            malli.send()
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
        assertEquals(ReplyUiState.Idle, malli.send.value)
    }

    @Test
    fun `tyhjaa viestia ei laheteta`() {
        // `ReplyForm.write` palauttaa nullin tyhjasta tekstista, ja tyhjan viestin lahetys
        // kuluttaisi teon oikealle ihmiselle.
        val sender = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_LOMAKKEELLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onDraftChange("   ")
            malli.send()
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun `lahetys menee sivun omaan osoitteeseen ja arkistoon vasta kuittauksesta`() {
        val sender = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_LOMAKKEELLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onDraftChange("hyvin pelattu")
            malli.send()
            advanceUntilIdle()
        }

        // Osoite on sivun oma eika koottu kayttajanumerosta, ja kentta on sivun oma nimi.
        assertEquals(1, sender.sent.size)
        assertEquals("/bg/sendmsg/90001", sender.sent.single().action)
        assertEquals(mapOf("text" to "hyvin pelattu"), sender.sent.single().fields)

        assertEquals(ReplyUiState.Sent, malli.send.value)
        // Luonnos tyhjenee vain onnistuessa.
        assertEquals("", malli.draft.value)

        val tallennettu = dao.rows.value.single()
        assertEquals("hyvin pelattu", tallennettu.body)
        assertEquals("pelaaja", tallennettu.opponent)
        assertEquals(OMA_NIMI, tallennettu.sender)
        assertEquals(MessageSource.QUICK_MESSAGE, tallennettu.source)
        // Aloitus eika vastaus: viestia ei liiteta mihinkaan aiempaan.
        assertNull(tallennettu.replyTo)
        // Otsikko on sivun oma lause sellaisenaan.
        assertTrue(tallennettu.rawHeader.contains("has been sent to"))
    }

    @Test
    fun `kuittaamatonta vastausta ei lueta onnistumiseksi`() {
        // Palvelin vastaa 200:lla myos silloin kun sivu ei ole kuittaus. Silloin ei tiedeta
        // meniko viesti perille, ja arkistoon kirjoittaminen olisi vaite ilman katetta.
        val sender = RecordingSender { DgResponse.Ok(TURNAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_LOMAKKEELLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onDraftChange("moi")
            malli.send()
            advanceUntilIdle()
        }

        assertEquals(ReplyUiState.Unconfirmed, malli.send.value)
        assertTrue(dao.rows.value.isEmpty())
        // Teksti jaa kenttaan: se on ainoa kopio siita mita kayttaja kirjoitti.
        assertEquals("moi", malli.draft.value)
    }

    @Test
    fun `epaonnistunut lahetys sailyttaa tekstin eika kirjoita arkistoon`() {
        val sender = RecordingSender { DgResponse.Offline("testi") }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_LOMAKKEELLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onDraftChange("moi")
            malli.send()
            advanceUntilIdle()
        }

        assertEquals(ReplyUiState.Failed(Failure.Offline), malli.send.value)
        assertTrue(dao.rows.value.isEmpty())
        assertEquals("moi", malli.draft.value)
    }

    @Test
    fun `kutsu menee sivun omaan osoitteeseen sivun kentilla ja jaa vastatuksi`() {
        val sender = RecordingSender { DgResponse.Ok(KUITTAUS_TUNTEMATON) }
        val fetcher = RecordingFetcher { DgResponse.Ok(PROFIILI_TEOILLA) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.invite(InviteChoice(variant = "2", length = "7", timeControl = "11", comment = "hi", privateMatch = true))
            advanceUntilIdle()
        }

        val lahetetty = sender.sent.single()
        assertEquals("/bg/invite/new", lahetetty.action)
        assertEquals(FormMethod.POST, lahetetty.method)
        // Piilokentta sivulta, valinnat sivun arvoina, ruksattu ruutu sivun arvolla.
        assertEquals("90001", lahetetty.fields["player"])
        assertEquals("2", lahetetty.fields["variant"])
        assertEquals("7", lahetetty.fields["length"])
        assertEquals("11", lahetetty.fields["time_control"])
        assertEquals("hi", lahetetty.fields["comment"])
        assertEquals("private", lahetetty.fields["private"])
        // Vastaussivua ei ole mitattu: tila sanoo etta vastaus tuli, ei etta onnistui.
        val tila = malli.action.value as ProfileActionUiState.Answered
        assertEquals("pelaaja", tila.player)
        assertEquals("Your invitation has been sent to pelaaja", tila.notice)
        // Profiili haettiin uudelleen, koska vastaus ei ollut profiili.
        assertEquals(2, fetcher.requested.size)
    }

    @Test
    fun `kutsua ei laheteta arvolla jota sivu ei tarjoa`() {
        val sender = RecordingSender { DgResponse.Ok(KUITTAUS_TUNTEMATON) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_TEOILLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.invite(InviteChoice(variant = "9", length = "7", timeControl = "11"))
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
        assertEquals(ProfileActionUiState.Idle, malli.action.value)
    }

    @Test
    fun `sivuutus on GET sivun piilokentilla ja onnistuminen luetaan napin vaihtumisesta`() {
        val sender = RecordingSender { DgResponse.Ok(PROFIILI_SIVUUTETTU) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_TEOILLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.ignore()
            advanceUntilIdle()
        }

        val lahetetty = sender.sent.single()
        assertEquals("/bg/ignore", lahetetty.action)
        assertEquals(FormMethod.GET, lahetetty.method)
        assertEquals(mapOf("changeto" to "1", "user" to "90001"), lahetetty.fields)

        val tila = malli.action.value as ProfileActionUiState.Done
        assertEquals("Unignore pelaaja", tila.label)
        // Vastaus oli profiili, ja se korvasi ruudun sisallon.
        val sivu = ((malli.state.value as PageUiState.Loaded).content as PageContent.Profile).page
        assertEquals("0", sivu.ignoreForm?.changeTo)
    }

    @Test
    fun `sivuutus jonka vastaus ei nayta muutosta jaa vahvistamatta`() {
        val sender = RecordingSender { DgResponse.Ok(PROFIILI_TEOILLA) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_TEOILLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.ignore()
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertTrue(malli.action.value is ProfileActionUiState.Unconfirmed)
    }

    @Test
    fun `luovutus lahettaa valitut ruudut ja varmistuksen sivun nimilla ja lukee tuloksen listasta`() {
        val sender = RecordingSender { DgResponse.Ok(LUOVUTUS_JALKEEN) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(LUOVUTUS) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            assertTrue(malli.state.value.let { (it as PageUiState.Loaded).content is PageContent.Resign })
            malli.resign(setOf(MatchId("5290918")))
            advanceUntilIdle()
        }

        val lahetetty = sender.sent.single()
        assertEquals("/bg/resign/doit", lahetetty.action)
        assertEquals(FormMethod.POST, lahetetty.method)
        assertEquals(mapOf("5290918" to "on", "reellysure" to "on"), lahetetty.fields)
        // Vastaus oli luovutussivu jolta valittu rivi puuttuu: se on todiste.
        assertEquals(ResignUiState.Done(1), malli.resign.value)
        val sivu = ((malli.state.value as PageUiState.Loaded).content as PageContent.Resign).page
        assertEquals(listOf("5319548"), sivu.rows.map { it.field })
    }

    /**
     * Kuittaussivu on ensisijainen lopputulos (Tommin paatos 4.9.2026 illalla): luku ja lause
     * tulevat sivustolta. Lista haetaan silti tuoreena, koska kuittaussivulla ei ole riveja.
     */
    @Test
    fun `luovutuksen kuittaussivu antaa sivuston oman lukeman ja listaa haetaan silti`() {
        val sender = RecordingSender { DgResponse.Ok(LUOVUTUS_KUITTAUS) }
        val haut = mutableListOf<String>()
        val fetcher = RecordingFetcher { path ->
            haut += path
            DgResponse.Ok(if (haut.size == 1) LUOVUTUS else LUOVUTUS_JALKEEN)
        }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.resign(setOf(MatchId("5290918")))
            advanceUntilIdle()
        }

        assertEquals(ResignUiState.Done(1, "1 match resigned."), malli.resign.value)
        // Toinen haku on lista ruudulle, ja se on ajettu: kuittaussivulla ei ole riveja.
        assertEquals(2, haut.size)
        val sivu = (malli.state.value as PageUiState.Loaded).content as PageContent.Resign
        assertEquals(listOf("5319548"), sivu.page.rows.map { it.field })
    }

    @Test
    fun `luovutus jonka jalkeen rivi on yha listalla jaa vahvistamatta`() {
        val sender = RecordingSender { DgResponse.Ok(LUOVUTUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(LUOVUTUS) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.resign(setOf(MatchId("5290918")))
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(ResignUiState.Unconfirmed, malli.resign.value)
    }

    @Test
    fun `tyhja valinta ei laheta luovutusta`() {
        val sender = RecordingSender { DgResponse.Ok(LUOVUTUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(LUOVUTUS) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.resign(emptySet())
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun `kaksoisnapautus ei laheta kahta viestia`() {
        val sender = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PROFIILI_LOMAKKEELLA) }, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onDraftChange("moi")
            malli.send()
            malli.send()
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
    }

    private companion object {
        const val POLKU = "/bg/user/90001"
        const val OMA_NIMI = "omanimi"
        const val HETKI = 1_724_000_000_000L

        val PROFIILI = """
            <html><head><title>DailyGammon: Info on player pelaaja</title></head><body>
            <table><tr><th align=right>Location</th><td>paikka</td></tr></table>
            <table><tr><td align=right>Rating</td><td>1234.56</td></tr></table>
            <a href=/bg/userevent/90001>active tournaments</a>
            </body></html>
        """.trimIndent()

        const val VIENTIPOLKU = "/bg/export/7000034"
        const val TURNAUSPOLKU = "/bg/event/900027"

        /** Profiili jolla on yksi päättynyt ottelu sivun omalla `Export`-linkillä. */
        val PROFIILI_PAATTYNYT = """
            <html><head><title>DailyGammon: Info on player pelaaja</title></head><body>
            <table><tr><th align=right>Location</th><td>paikka</td></tr></table>
            <TABLE CELLSPACING=3><CAPTION><FONT SIZE=+1>Finished matches:</font></caption>
            <TR><TH align=left>#</TH><TH align=left>Event</TH><TH>Round</TH><TH align=right>Length</TH><TH>Opponent</TH></TR>
            <TR><TD>1. </TD><TD><A HREF=/bg/event/900027>Sample Event</A></TD><TD>2/4</TD><TD>15</TD>
            <TD><A href=/bg/user/91032>vastapelaaja32</A></TD>
            <TD><A href=/bg/game/7000034/1/list>Review</A></TD>
            <TD><A href=/bg/export/7000034>Export</A></TD></TR>
            </TABLE>
            </body></html>
        """.trimIndent()

        /** Viennin runko, sama muoto kuin `raakasivut/match_export.mat`. */
        val MAT = " 15 point match\n\n Game 1\n pelaaja : 0                    vastapelaaja32 : 0\n  1) 41: 24/23 13/9                  44: 24/20 24/20\n"

        /** Tavallinen turnaus: `Game`-ehto sanoo backgammon, joten ottelu on Crawford. */
        val TURNAUS_BACKGAMMON = """
            <html><body>
            <h2>Sample Marathon</h2>
            <h3>Conditions of Contest</h3>
            <h4>Game</h4>backgammon, 15 point matches.
            <table><caption>Brackets</caption>
            <tr><th>Round 1<th>Winner
            <tr><td><a href=/bg/user/17135>ekapelaaja</a><td rowspan=2>&nbsp;
            </table>
            </body></html>
        """.trimIndent()

        val TURNAUS = """
            <html><body>
            <h2>Sample Deja Vu</h2>
            <h3>Conditions of Contest</h3>
            <h4>Game</h4>double-repeat, 11 point matches.
            <table><caption>Brackets</caption>
            <tr><th>Round 1<th>Winner
            <tr><td><a href=/bg/user/17135>ekapelaaja</a><td rowspan=2>&nbsp;
            </table>
            </body></html>
        """.trimIndent()

        /**
         * Profiili jolla on sivun oma viestilomake. Kutsulomake on mukana ja se on ENNEN
         * viestilomaketta, kuten aidolla sivulla: jos lomake valittaisiin jarjestyksesta,
         * teksti lahtisi ottelukutsuna.
         */
        val PROFIILI_LOMAKKEELLA = """
            <html><head><title>DailyGammon: Info on player pelaaja</title></head><body>
            <table><tr><th align=right>Location</th><td>paikka</td></tr></table>
            <a href=/bg/userevent/90001>active tournaments</a>
            <form action=/bg/invite/new method=post>
            <input type=text name=comment maxlength=80><input type=submit value=Invite></form>
            <form action=/bg/sendmsg/90001 method=post>
            Quick message to pelaaja:<input type=text name=text size=40 maxlength=80>
            <input type=submit value=Send></form>
            </body></html>
        """.trimIndent()

        /** Toisen pelaajan profiili kaikilla kolmella lomakkeella, raakasivun muotoa. */
        val PROFIILI_TEOILLA = """
            <html><head><title>DailyGammon: Info on player pelaaja</title></head><body>
            <table><tr><th align=right>Location</th><td>paikka</td></tr></table>
            <a href=/bg/userevent/90001>active tournaments</a>
            <form action=/bg/invite/new method=post>
            <input type=hidden name=player value=90001>
            <select name=variant><option value=1 selected>backgammon<option value=2>nack</select>
            <select name=length><option value=5 selected>5<option value=7>7</select>
            <input type=text name=comment maxlength=80><input type=text name=name maxlength=40>
            <select name=time_control><option value=0 selected>Never</option><option value=11>Weekday</option></select>
            <input type=checkbox name=private value=private> Private Match
            <input type=submit value=Invite></form>
            <form action=/bg/sendmsg/90001 method=post>
            Quick message to pelaaja:<input type=text name=text size=40 maxlength=80>
            <input type=submit value=Send></form>
            <form action=/bg/ignore method=get><input type=hidden name=changeto value=1>
            <input type=hidden name=user value=90001><input type=submit value="Ignore pelaaja"></form>
            </body></html>
        """.trimIndent()

        /** Sama profiili sivuutuksen jalkeen: nappi on vaihtunut, changeto on 0. Muoto on oletus, ei mitattu. */
        val PROFIILI_SIVUUTETTU = PROFIILI_TEOILLA
            .replace("name=changeto value=1", "name=changeto value=0")
            .replace("Ignore pelaaja", "Unignore pelaaja")

        /** Kutsun vastaussivu, jota ei ole mitattu: yksi lause ilman tunnistettavaa muotoa. */
        val KUITTAUS_TUNTEMATON = """
            <html><head><title>DailyGammon</title></head><body>
            Your invitation has been sent to <a href=/bg/user/90001>pelaaja</a>
            <p><a href=/bg/top>Top Page</a>
            </body></html>
        """.trimIndent()

        /** Luovutussivu kahdella rivilla, sama muoto kuin fixture `resign_page.html`. */
        val LUOVUTUS = """
            <html><head><title>DailyGammon: Resignation Page</title></head><body>
            <form action=/bg/resign/doit method=post>
            <table><caption>Unfinished matches:</caption>
            <tr><th>#</th><th>Event</th><th>Grace</th><th>Time Pool</th><th>Round</th><th>Length</th><th>Opponent</th></tr>
            <tr><td>1.</td><td><a href=/bg/event/1>Turnaus</a></td><td>n/a</td><td>430:19</td><td>3/8</td><td>11</td>
            <td><a href=/bg/user/2>vastus</a></td><td><a href=/bg/game/5290918/1/list>Review</a></td>
            <td><input type=checkbox name=5290918></td></tr>
            <tr><td>2.</td><td>nack</td><td>-</td><td>-</td><td>-</td><td>1</td>
            <td><a href=/bg/user/3>toinen</a></td><td><a href=/bg/game/5319548/0/list>Review</a></td>
            <td><input type=checkbox name=5319548></td></tr>
            </table><p>
            <input type=checkbox name=reellysure> Yes! I really want to resign.
            <br><input type=submit value="Resign Selected Matches">
            </form></body></html>
        """.trimIndent()

        /** Sama sivu luovutuksen jalkeen: ensimmainen rivi on poissa. Muoto on oletus, ei mitattu. */
        val LUOVUTUS_JALKEEN = LUOVUTUS.replace(
            Regex("""<tr><td>1\..*?</tr>""", RegexOption.DOT_MATCHES_ALL), "",
        )

        /**
         * Luovutuksen kuittaussivu, sama muoto kuin fixture `resign_done.html` (mitattu
         * 4.9.2026). Ei lomaketta, joten ResignParser.parse antaa nullin, ja luku on
         * sivuston oma.
         */
        val LUOVUTUS_KUITTAUS = """
            <html><head><title>DailyGammon: Post Resignation</title></head><body>
            <table><tr><td><a href=/bg/top>Top Page</a></td></tr></table>
            <br>1 match resigned.
            <br>Back to <a href=/bg/resign>resigns</a>
            </body></html>
        """.trimIndent()

        /** Lahetyksen kuittaussivu, sama muoto kuin fixture `message_sent.html`. */
        val KUITTAUS = """
            <html><head><title>Message Sent</title></head><body>
            Your message has been sent to <a href=/bg/user/90001>pelaaja</a>
            <p><a href=/bg/nextgame>Next &gt;&gt;</a>
            </body></html>
        """.trimIndent()

        /** Synteettinen halli: molemmat captionit, yksi rivi kummassakin. */
        val THALL = """
            <html><body>
            <table><caption>Finished Tournaments</caption>
            <tr><th>Name<th>Winner<th>Completion Date
            <tr><td><a href=/bg/event/1>Vanha turnaus</a>
            <td><a href=/bg/user/10002>voittaja</a><td>2026-08-26 14:59:38
            </table>
            <table><caption>Active Tournaments</caption>
            <tr><th>Name<th>Start Date
            <tr><td><a href=/bg/event/2>Uusi turnaus</a><td>2026-08-26 14:12:04
            </table>
            </body></html>
        """.trimIndent()

        val TURNAUSLISTA = """
            <html><head><title>DailyGammon Events For pelaaja</title></head><body>
            <h2>Active Tournaments for <a href=/bg/user/90001>pelaaja</a></h2>
            <table><tr><th><th>#<th><th align=left>Event<th>Wins
            <tr><td width=30><td>1.<td width=10><td><a href=/bg/event/111128>Turnaus</a>
            <td align=center>2</td></tr>
            </table>
            </body></html>
        """.trimIndent()
    }
}

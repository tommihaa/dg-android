package fi.tommi.dg.app.ui

import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PendingAction
import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.net.DgResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Lautanäkymän näkymämalli, ja ennen kaikkea sen lukutila.
 *
 * **HTML on näissä testeissä synteettistä, ja se on tässä tapauksessa oikein.** Projektin
 * oppi kuvakaappauksesta kirjoitetusta sivusta koskee tilannetta jossa **jäsennin
 * kirjoitetaan** keksittyä sivua vasten: kirjoittaja ei keksi rakennetta jota ei näe, joten
 * jäsennin oppii vain sen mitä osattiin kuvitella. Täällä mitään ei kirjoiteta näitä vasten.
 * Jokainen jäsennystarkkuutta koskeva väite asuu core-scrapessa oikeita palvelintavuja
 * vasten, ja nämä testit väittävät vain näkymämallin haarautumista: mihin tilaan mikäkin
 * vastaus johtaa ja mitä polkuja pyydettiin.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Kirjaa jokaisen pyydetyn polun, jotta testi voi väittää mitä EI haettu. */
    private class RecordingFetcher(var answer: (String) -> DgResponse) : PageFetcher {
        val requested = mutableListOf<String>()
        override fun fetch(path: String): DgResponse {
            requested += path
            return answer(path)
        }
    }

    /**
     * Kirjaa jokaisen lähetyksen. Erillään hakijasta samasta syystä kuin oikeassa koodissa:
     * teko ja haku ovat eri asioita, ja testin on voitava väittää kummastakin erikseen.
     */
    private class RecordingSender(var answer: (FormSubmission) -> DgResponse = { DgResponse.Ok(lauta(OTTELU)) }) : FormSender {
        val sent = mutableListOf<FormSubmission>()
        override fun send(submission: FormSubmission): DgResponse {
            sent += submission
            return answer(submission)
        }
    }

    private val queueDao = FakeQueue()
    private val katkoLoki = FakeDropLog()

    private val reminderDao = FakeReminders()
    private val markDao = FakeMarks()


    @Before
    fun asetaDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun palautaDispatcher() {
        Dispatchers.resetMain()
    }

    private val viestiDao = FakeArchive()

    private fun malli(
        fetcher: RecordingFetcher,
        sender: RecordingSender = RecordingSender(),
        path: String = PELIPOLKU,
        expected: MatchId? = MatchId(OTTELU),
        listedRound: (MatchId) -> String? = { null },
    ) = BoardViewModel(
        pages = fetcher,
        forms = sender,
        queue = queueDao,
        dropLog = katkoLoki,
        reminderBook = reminderDao,
        markBook = markDao,
        archive = viestiDao,
        self = { OMA_NIMI },
        playPath = path,
        expectedMatchId = expected,
        listedRound = listedRound,
        now = { HETKI },
        io = dispatcher,
    )

    // --- Portti: mitä ei voi pyytää eikä lähettää ---

    @Test
    fun `paattynyt ottelu luetaan omaksi tilakseen eika vaaraksi sivuksi`() {
        // Mitattu sivu 31.8.2026. Ennen tätä se päätyi NotABoardKind.WrongPageen, jolloin
        // ruudulla luki "This is no longer a board" eikä sivulta luettu mitään.
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT) }
        val malli = malli(fetcher, expected = null)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value as BoardUiState.MatchOver
        assertEquals("pelaaja wins 2 points and the match.", tila.page.resultText)
        assertEquals(listOf(7, 6), tila.page.scores.map { it.score })
        assertEquals("/bg/game/7000006/1/list#end", tila.page.reviewPath)
        assertEquals("/bg/nextgame", tila.page.nextGamePath)
        // Tällä muunnelmalla ei ole chat-kenttää, eikä sellaista keksitä.
        assertNull(tila.chat)
    }

    @Test
    fun `lautapolulla tullut pikaviesti menee arkistoon eika vaaraksi sivuksi`() {
        // Mitattu 13.9.2026 klo 01.00: Submit Move palautti jonon pikaviestisivun, ja se
        // päätyi WrongPageen ilman että viestiä kirjoitettiin kantaan. Sivusto ei säilytä
        // sitä, joten tämä on ykkösominaisuuden portti.
        val fetcher = RecordingFetcher { DgResponse.Ok(PIKAVIESTI_LAUDALLA) }
        val malli = malli(fetcher, expected = null)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value as BoardUiState.NotABoard
        val laji = tila.kind as NotABoardKind.InboxItem
        assertTrue(laji.saved)
        assertEquals("Thank you too. Good luck in your others.", laji.message.body)
        assertEquals("vastapelaaja", laji.message.sender)
        assertEquals(listOf("Thank you too. Good luck in your others."), viestiDao.rows.value.map { it.body })
        assertEquals(listOf("vastapelaaja"), viestiDao.rows.value.map { it.sender })
    }

    @Test
    fun `tuntematon sivu lautapolulla ei kirjoitu kantaan`() {
        // InboxParser jäsentää minkä tahansa h3-sivun; laji UNKNOWN ei saa tuottaa
        // valeviestiä arkistoon vaan jää WrongPageksi.
        val fetcher = RecordingFetcher { DgResponse.Ok("<html><body><h3>Something else</h3><pre>x</pre></body></html>") }
        val malli = malli(fetcher, expected = null)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value as BoardUiState.NotABoard
        assertEquals(NotABoardKind.WrongPage, tila.kind)
        assertEquals(emptyList<String>(), viestiDao.rows.value.map { it.body })
    }

    @Test
    fun `paattymissivun chat-kentta luetaan samalla lukijalla kuin laudalla`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT_CHAT) }
        val malli = malli(fetcher, expected = null)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value as BoardUiState.MatchOver
        val chat = requireNotNull(tila.chat)
        assertEquals("vastapelaaja", chat.opponent)
        val form = requireNotNull(chat.form)
        assertEquals(listOf("Next Game", "To Top"), form.submits)
        // Kentän nimi ja piilokenttä tulevat sivulta: ilman commit-kenttää lähetys ei tee
        // sivustolla mitään.
        assertEquals("chat", form.field)
        assertEquals(mapOf("commit" to "1"), form.hiddenFields)
    }

    @Test
    fun `paattymissivun saapunut viesti menee arkistoon`() {
        // Tämä on koko sivun tärkein osa: ottelun viimeinen viesti ei ehdi arkistoon mitään
        // muuta reittiä, koska sivusto ei säilytä keskustelua eikä ottelu enää ole listalla.
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT_VIESTI) }
        val malli = malli(fetcher, expected = null)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value as BoardUiState.MatchOver
        assertEquals("gg, well played", tila.chat?.incoming)
        assertEquals(listOf("gg, well played"), viestiDao.rows.value.map { it.body })
        assertEquals(listOf("vastapelaaja"), viestiDao.rows.value.map { it.sender })
    }

    @Test
    fun `paattymissivun viesti menee arkistoon vaikka kenttaa ei ole`() {
        // Mitattu 7.9.2026 (`raakasivut/sessio-7-9-yo/0062`): vastustaja kirjoitti ottelun
        // viimeisellä vuorolla, ja lomakkeessa oli vain napit. Lukija palautti nullin ennen
        // arkistointia, ruutu näytti pelkän tuloksen, ja viesti katosi sivustolta seuraavaan
        // lukuun mennessä. Tämä lukitsee sekä arkistoinnin että ruudun: viesti näkyy, kenttää
        // ei keksitä, ja napit tulevat sivun omasta lomakkeesta.
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT_SANOO) }
        val malli = malli(fetcher, expected = null)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value as BoardUiState.MatchOver
        val chat = requireNotNull(tila.chat)
        assertEquals("Another good win! Keep it going!", chat.incoming)
        assertNull(chat.form)
        assertEquals("vastapelaaja", chat.opponent)
        assertEquals(listOf("Next Game", "To Top"), tila.page.form?.submits)
        assertEquals(listOf("Another good win! Keep it going!"), viestiDao.rows.value.map { it.body })
        assertEquals(listOf("vastapelaaja"), viestiDao.rows.value.map { it.sender })
    }

    @Test
    fun `paattymissivun nappi lahtee sivun omalla lomakkeella`() {
        // Laiteajossa 1.9.2026 ruutu näytti vain Review- ja Skip-linkit, koska napit ovat
        // lomakkeessa eikä niitä luettu. Tämä lukitsee sekä luvun että lähetyksen.
        val sender = RecordingSender { DgResponse.Ok(lauta(OTTELU)) }
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT_NAPIT) }
        val malli = malli(fetcher, sender, expected = null)

        runTest(dispatcher) {
            advanceUntilIdle()
            val tila = malli.state.value as BoardUiState.MatchOver
            assertEquals(listOf("Next Game", "To Top"), tila.page.form?.submits)
            malli.press("Next Game")
            advanceUntilIdle()
        }

        val lahetys = sender.sent.single()
        assertEquals("/bg/move/7000006/1491", lahetys.action)
        assertEquals(mapOf("commit" to "1", "submit" to "Next Game"), lahetys.fields)
    }

    @Test
    fun `paattymissivun keksitty nappi ei lahde`() {
        val sender = RecordingSender()
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT_NAPIT) }
        val malli = malli(fetcher, sender, expected = null)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun `paattymissivun oma linkki seurataan ja vieras hylataan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT) }
        val malli = malli(fetcher, expected = null)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.follow("/bg/nextgame?skip=7000009")
            advanceUntilIdle()
            malli.follow("/bg/nextgame?skip=7000006")
            advanceUntilIdle()
        }

        // Ensimmäinen haku on ruudun avaus, toinen on sivun oma Skip. Vieras osoite ei
        // tuottanut pyyntöä lainkaan.
        assertEquals(listOf(PELIPOLKU, "/bg/nextgame?skip=7000006"), fetcher.requested)
    }

    @Test
    fun `paattymissivulta lahetetty viesti menee perille ja arkistoon`() {
        // Sivun tärkein teko: kiitokset kirjoitetaan juuri tässä, eikä sivusto säilytä niitä.
        val sender = RecordingSender { DgResponse.Ok(lauta(OTTELU)) }
        val fetcher = RecordingFetcher { DgResponse.Ok(PAATTYNYT_VIESTI) }
        val malli = malli(fetcher, sender, expected = null)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.sendChat("ty gg u2", quote = false, submit = "To Top")
            advanceUntilIdle()
        }

        assertEquals("/bg/move/7000005/2097", sender.sent.single().action)
        assertEquals(listOf("gg, well played", "ty gg u2"), viestiDao.rows.value.map { it.body })
        assertEquals(listOf("vastapelaaja", OMA_NIMI), viestiDao.rows.value.map { it.sender })
    }

    @Test
    fun `lautanakyma hakee tasmalleen sen polun jonka sivu antoi`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        // Polku on Match.playPathista sellaisenaan. Jos tähän ilmestyy normalisointia tai
        // kokoamista, se näkyy tässä listassa.
        assertEquals(listOf(PELIPOLKU, PELIPOLKU), fetcher.requested)
    }

    @Test
    fun `kierrosrivi taydentyy luettelon kokonaismaaralla`() {
        // Tommin tilaus 8.9.2026: lautasivu sanoo "Round 3", luettelo "3/5", ruudulle
        // "Round 3/5". Numero on sivulta, kokonaismäärä luettelosta.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaKierroksella(OTTELU, 3)) }
        val malli = malli(fetcher, listedRound = { if (it == MatchId(OTTELU)) "3/5" else null })

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals("Round 3/5", (malli.state.value as BoardUiState.Loaded).roundLabel)
    }

    @Test
    fun `ilman luettelon kierrosta rivi on sivun oma sana`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaKierroksella(OTTELU, 3)) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals("Round 3", (malli.state.value as BoardUiState.Loaded).roundLabel)
    }

    @Test
    fun `Next Game -ketjussa tullut ottelu saa oman kokonaismaaransa luettelosta`() {
        // Mitattu sessio-8-9-yo5: neljä viidestä ottelusta tuli ketjussa eikä napauttamalla,
        // ja ensimmäinen toteutus (reittiparametri) näytti niille pelkän sivun numeron.
        // Haku on sivun omalla tunnisteella, joten napautetun ottelun 3/5 ei vuoda tänne.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto(submit = "Next Game")) }
        val sender = RecordingSender { DgResponse.Ok(lautaKierroksella("7000099", 1)) }
        val luettelo = mapOf(MatchId(OTTELU) to "3/5", MatchId("7000099") to "1/4")
        val malli = malli(fetcher, sender, listedRound = { luettelo[it] })

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Next Game")
            advanceUntilIdle()
        }

        assertEquals("Round 1/4", (malli.state.value as BoardUiState.Loaded).roundLabel)
    }

    @Test
    fun `tuntematon ottelu ketjussa saa sivun oman sanan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto(submit = "Next Game")) }
        val sender = RecordingSender { DgResponse.Ok(lautaKierroksella("7000099", 1)) }
        val malli = malli(fetcher, sender, listedRound = { if (it == MatchId(OTTELU)) "3/5" else null })

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Next Game")
            advanceUntilIdle()
        }

        assertEquals("Round 1", (malli.state.value as BoardUiState.Loaded).roundLabel)
    }

    @Test
    fun `lautanakyma ei tee mitaan itsestaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        // Portin purku ei muuttanut tätä: ilman käyttäjän elettä ei lähde tekoa eikä
        // kuluttavaa hakua, vaikka sivu tarjoaa sekä siirtolinkin että lähetysnapin.
        assertTrue("Teko lähti ilman elettä", sender.sent.isEmpty())
        assertTrue(
            fetcher.requested.none { it.contains("nextgame") || it.contains("skip=") }
        )
        assertEquals(listOf(PELIPOLKU, PELIPOLKU), fetcher.requested)
    }

    @Test
    fun `kyselyparametrillinen sisaankaynti ei laukaise pyyntoa lainkaan`() {
        // Sisäänkäynnin ehto on portin se puolikas joka ei purkautunut: ruutua ei voi avata
        // suoraan tekoon. Väite on yhä muodossa "pyyntö ei lähtenyt" eikä "nappia ei painettu".
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(fetcher, path = "/bg/move/$OTTELU/541?submit=Roll+Dice")

        runTest(dispatcher) { advanceUntilIdle() }

        assertTrue("Pyyntö lähti vaikkei olisi saanut", fetcher.requested.isEmpty())
        assertEquals(
            BoardUiState.NotABoard(NotABoardKind.PathNotReadOnly),
            malli.state.value,
        )
    }

    // --- Kokoaminen: vain laudan oma linkki kelpaa ---

    @Test
    fun `laudan oma linkki haetaan sellaisenaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.follow(SIIRTOLINKKI)
            advanceUntilIdle()
        }

        // Merkkijonovertailu eikä muotovertailu: jos tähän ilmestyy normalisointia,
        // yhdistämistä tai uudelleenkoodausta, se näkyy tässä listassa.
        assertEquals(listOf(PELIPOLKU, SIIRTOLINKKI), fetcher.requested)
    }

    @Test
    fun `keksittya osoitetta ei haeta vaikka se nayttaisi oikealta`() {
        // Tämä on kokoamiskiellon ydin. Osoite on saman ottelun, saman tilatunnisteen ja
        // oikean muotoinen; se vain ei ole sivun antama. Sivusto pitää siirron välitilaa
        // palvelimella, joten itse koottu kirjain tuottaisi väärän siirron eikä virhettä.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.follow("$PELIPOLKU?move=g")
            advanceUntilIdle()
        }

        assertEquals(listOf(PELIPOLKU), fetcher.requested)
    }

    @Test
    fun `edellisen laudan linkki ei kelpaa uudella laudalla`() {
        // Linkin jäsenyys vanhenee laudan mukana, koska tilatunniste on osa osoitetta.
        // Ilman tätä ruutu voisi lähettää eilisen linkin tämän päivän asemaan.
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.follow(SIIRTOLINKKI)
            advanceUntilIdle()
        }

        assertEquals(listOf(PELIPOLKU), fetcher.requested)
    }

    // --- Lähettäminen: vain sivun oma nappi ---

    @Test
    fun `sivun oma nappi lahtee sivun omilla kentilla`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(PELIPOLKU, sender.sent.single().action)
        assertEquals(
            mapOf("move" to "rrmm", "submit" to "Submit Move"),
            sender.sent.single().fields,
        )
    }

    @Test
    fun `nappia jota sivu ei tarjoa ei laheteta`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
        }

        assertTrue("Keksitty nappi lähti", sender.sent.isEmpty())
    }

    @Test
    fun `kaksoisnapautus ei tee kahta tekoa`() {
        // Teot eivät ole peruttavissa, joten yksi kerrallaan on tässä eri asia kuin
        // muualla: toinen napautus ei jää jonoon vaan katoaa.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
    }

    @Test
    fun `epaonnistunut teko sailyttaa laudan ja merkitsee sen vahvistamattomaksi`() {
        // 24.8.2026 asti teon epäonnistuminen pyyhki laudan, koska hiljaa säilynyt lauta
        // olisi luettu todisteeksi teon onnistumisesta. Nyt lauta säilyy (laitteessa jo
        // ollut tieto ei katoa lähetysyrityksen mukana) ja äänen kantaa unconfirmed:
        // ruutu näyttää siitä ilmoituksen eikä lauta ota vastaan tekoja.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        val tila = malli.state.value
        assertTrue("Lauta katosi teon epäonnistuessa", tila is BoardUiState.Loaded)
        assertEquals(Failure.Offline, (tila as BoardUiState.Loaded).unconfirmed)
    }

    @Test
    fun `vahvistamaton lauta ei ota vastaan tekoja`() {
        // Vahvistamattoman laudan linkit luettiin sivusta jonka tilaa lähetysyritys on
        // voinut muuttaa. Niiden seuraaminen olisi sama sokea toisto jonka takia odottava
        // rivi ratkaistaan haetusta sivusta, joten sekä painallus että linkki pysähtyvät.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
            val lahetettyEnnen = sender.sent.size
            val haettuEnnen = fetcher.requested.size

            malli.press("Submit Move")
            malli.follow(SIIRTOLINKKI)
            advanceUntilIdle()

            assertEquals("Vahvistamaton lauta lähetti", lahetettyEnnen, sender.sent.size)
            assertEquals("Vahvistamaton lauta haki linkin", haettuEnnen, fetcher.requested.size)
        }
    }

    @Test
    fun `yhteyden palaaminen hakee laudan kun teko kaatui verkkoon`() {
        // Sama sääntö kuin haun epäonnistumisella: verkon paluu saa hakea sivun kerran.
        // Onnistunut haku tuottaa tuoreen laudan, jolloin vahvistamattomuus ja lukitus
        // poistuvat itsestään. Tekoa ei lähetetä (erillinen testi alla).
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
            val haettuEnnen = fetcher.requested.size

            malli.onNetworkAvailable()
            advanceUntilIdle()

            assertEquals(haettuEnnen + 1, fetcher.requested.size)
        }

        val tila = malli.state.value
        assertTrue(tila is BoardUiState.Loaded)
        assertNull((tila as BoardUiState.Loaded).unconfirmed)
    }

    @Test
    fun `yhteyden palaaminen hakee laudan uudelleen kun edellinen kaatui verkkoon`() {
        val fetcher = RecordingFetcher { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            assertEquals(BoardUiState.Failed(Failure.Offline), malli.state.value)
            fetcher.answer = { DgResponse.Ok(lauta(OTTELU)) }
            malli.onNetworkAvailable()
            advanceUntilIdle()
        }

        assertEquals(2, fetcher.requested.size)
        assertTrue(malli.state.value is BoardUiState.Loaded)
    }

    @Test
    fun `yhteyden palaaminen ei laheta odottavaa tekoa`() {
        // Tämän kohdan tärkein rajaus. Jonossa oleva teko on peruuttamaton, ja sen
        // uusiminen on käyttäjän päätös eikä verkon paluun seuraus.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
            val lahetettyEnnen = sender.sent.size
            malli.onNetworkAvailable()
            advanceUntilIdle()

            assertEquals(lahetettyEnnen, sender.sent.size)
        }
    }

    // --- Jono: teko joka ei mennyt perille ---

    @Test
    fun `katkennut lahetys kirjataan painalluksena eika osoitteena`() {
        // Tämä on jonon ydin. Rivi kuvaa sitä mitä käyttäjä teki, ei valmista lähetystä:
        // valmiin lähetyksen tallentaminen pakottaisi kokoamaan sen takaisin kannasta, ja
        // FormSubmissionin käännösaikainen tae purkautuisi juuri siihen kohtaan.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        val odottava = malli.pending.value
        assertNotNull("Katkennut teko katosi jäljettömiin", odottava)
        assertEquals(PELIPOLKU, odottava!!.boardPath)
        assertEquals("Submit Move", odottava.submit)
        assertEquals("rrmm", odottava.pendingMove)
        assertEquals(MatchId(OTTELU), odottava.matchId)
        assertEquals(HETKI, odottava.createdAtEpochMillis)
    }

    @Test
    fun `onnistunut teko ei jata jonoon mitaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        assertNull(malli.pending.value)
    }

    @Test
    fun `palvelimen vastaus ei ole jonon tapaus`() {
        // Rajaus on tarkoituksellinen: palvelinvirhe on palvelimen oma vastaus, eli yhteys
        // toimi ja pyyntö tuli käsitellyksi. Epäselvä on vain katkennut yhteys.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.ServerError(503) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        assertNull(malli.pending.value)
    }

    @Test
    fun `uusi katkennut teko korvaa saman ottelun edellisen`() {
        // Kaksi riviä kuvaisivat samaa tekemätöntä tekoa: jos ensimmäinen ei mennyt perille,
        // palvelimen tila ei liikkunut, joten toinen painallus koskee samaa hetkeä.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        assertEquals(1, queueDao.rows.value.size)
    }

    @Test
    fun `paivitys ei laheta mitaan vaikka sivu tarjoaa napin yha`() {
        // `Try again` poistui 8.9.2026 ja haku peri sen työn, mutta vain sen ensimmäisen
        // puoliskon. Jos sivu yhä tarjoaa napin, nappi on laudalla ja pelaaja painaa sitä
        // itse; sovellus ei lähetä mitään omasta aloitteestaan.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaNoppa()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals("Päivitys lähetti teon uudestaan", 1, sender.sent.size)
        assertNotNull("Rivi katosi vaikka sivu tarjoaa napin yhä", malli.pending.value)
        assertNull(malli.note.value)
    }

    @Test
    fun `paivitys tyhjentaa rivin kun sivu ei enaa tarjoa nappia`() {
        // Sivu itse vastaa siihen kysymykseen jota verkkokerros ei voi vastata. Kun nappia
        // ei enää tarjota, tilanne on toinen kuin painallushetkellä, eikä toistokelvoton
        // rivi saa jäädä jonoon ikuisesti. Mitään ei lähetetä.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaNoppa()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
            fetcher.answer = { DgResponse.Ok(lautaJossaSiirto()) }
            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals("Teko lähti vaikka sivu oli toisessa tilassa", 1, sender.sent.size)
        assertNull(malli.pending.value)
        assertEquals(PendingNote.NoLongerOffered, malli.note.value)
    }

    @Test
    fun `epaonnistunut paivitys ei tyhjenna rivia`() {
        // Vain oikeasti saatu sivu kelpaa todisteeksi: epäonnistunut haku säilyttää vanhan
        // laudan ruudulla, ja sen lukeminen vastaisi kysymykseen edellisen sivun tiedoilla.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaNoppa()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
            fetcher.answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
            malli.refresh()
            advanceUntilIdle()
        }

        assertNotNull("Rivi katosi epäonnistuneella haulla", malli.pending.value)
        assertNull(malli.note.value)
    }

    @Test
    fun `ensimmainen katkos kirjaa syyn jo riville`() {
        // Uusinta ei ole kirjannut vielä mitään, joten tämä väittää nimenomaan siitä
        // katkoksesta jonka käyttäjä koki painaessaan. Ilman sitä rivi olisi tyhjä juuri siltä
        // osin jota se on olemassa kertomaan.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaNoppa()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
        }

        val odottava = malli.pending.value
        assertNotNull("Painallus ei päätynyt jonoon", odottava)
        assertEquals("Offline: $TEST_OFFLINE_CAUSE", odottava!!.lastErrorText)
    }

    @Test
    fun `katkos kirjautuu historiaan joka jaa kun jonon rivi poistuu`() {
        // Jonon rivi poistuu kun seuraava teko onnistuu, ja sen mukana katosi syy (18.9.2026,
        // laitteen laskuri 41 ilman yhtään riviä). Historian rivi on olemassa juuri sitä
        // varten, ja se ei saa poistua jonon mukana. Kaksi painallusta: katko, sitten
        // onnistunut teko joka tyhjentää jonon. Onnistunut teko ei kirjaa historiaan mitään.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaNoppa()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
            sender.answer = { DgResponse.Ok(lautaJossaNoppa()) }
            malli.press("Roll Dice")
            advanceUntilIdle()
        }

        assertEquals(0, queueDao.rows.value.size)
        val katko = katkoLoki.rows.value.single()
        assertEquals("Roll Dice", katko.submit)
        assertEquals(TEST_OFFLINE_CAUSE, katko.cause)
        assertEquals(MatchId(OTTELU), katko.matchId)
        assertEquals(HETKI, katko.atEpochMillis)
    }

    @Test
    fun `kootun siirron rivin paivitys tyhjentaa ilman ilmoitusta ja lahettamatta`() {
        // Haku nollaa kokoamisen palvelimella (mitattu 10.8.2026), joten `stillOffers` ei
        // kerro kootun siirron kohtalosta mitään. 8.9.–16.9.2026 rivi odotti siksi Discardia;
        // 16.9. Discard poistui (Tommin tilaus ensimmäisen oikean laukeamisen jälkeen) ja
        // tuore lauta on se joka vastaa: siirto joko näkyy siinä tai nopat odottavat yhä.
        // Rivi poistuu samalla haulla, ilmoitusta ei tule koska lauta itse on ilmoitus, ja
        // mitään ei lähetetä uudestaan.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender(answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
            assertNotNull("Katkennut lähetys ei päätynyt jonoon", malli.pending.value)
            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals("Päivitys lähetti teon uudestaan", 1, sender.sent.size)
        assertNull("Kootun siirron rivi jäi jonoon tuoreen haun jälkeen", malli.pending.value)
        assertNull("Kootun siirron poisto ei saa väittää ettei sivu tarjoa tekoa", malli.note.value)
    }

    @Test
    fun `edellisesta ajosta jaanyt rivi nakyy heti`() {
        // Jono kestää sovelluksen sulkemisen, ja se on koko syy sille että se on kannassa
        // eikä muistissa. Rivi on kannassa ennen kuin näkymämalli on olemassa. Polku on
        // toinen kuin avattava lauta, koska 16.9.2026 alkaen saman laudan tuore haku
        // ratkaisee rivin (ks. edellinen testi); toisen laudan rivi ei ratkea tästä.
        queueDao.rows.value = listOf(
            PendingAction(
                id = 7,
                matchId = MatchId(OTTELU),
                boardPath = "/bg/move/$OTTELU/1",
                submit = "Submit Move",
                pendingMove = "rrmm",
                verified = false,
                createdAtEpochMillis = HETKI,
            )
        )
        val malli = malli(RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) })

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals("Submit Move", malli.pending.value?.submit)
    }

    // --- Sivu ei ollutkaan lauta ---

    @Test
    fun `top-sivu lautaosoitteesta tunnistetaan otteluluetteloksi`() {
        // Mitattu tapaus 3.8.2026: sivusto palautti /bg/move/-osoitteesta tyhjän Top Pagen
        // 200 OK:lla, kun viimeinenkin siirto oli tehty. Sama muoto mitattiin 28.8.2026
        // kahdesti To Topilla, ja kolmen tapauksen joukko on erottelun peruste
        // (docs/AVOIMET.md, suljettu 29.8.2026).
        val fetcher = RecordingFetcher { DgResponse.Ok(TOP_SIVU) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        // Sivu kannetaan mukana 4.9.2026 alkaen, jotta otteluluettelo voi lukea sen ilman
        // toista hakua (`TopViewModel.adopt`). Tavut ovat samat jotka haku palautti.
        assertEquals(BoardUiState.NotABoard(NotABoardKind.TopPage(TOP_SIVU)), malli.state.value)
    }

    @Test
    fun `tuntematon sivu lautaosoitteesta ei ole lauta`() {
        // Sivu jota mikään predikaatti ei tunne: ei lautaa eikä otteluluetteloa. Tähän
        // päätyisi esimerkiksi käyttökatkoilmoitus, jota ei ole vielä nähty.
        val fetcher = RecordingFetcher { DgResponse.Ok(EI_LAUTAA) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(BoardUiState.NotABoard(NotABoardKind.WrongPage), malli.state.value)
    }

    @Test
    fun `eri ottelun lauta tunnistetaan eika piirreta`() {
        // Mitattu 4.8.2026: /bg/move/<A>/457 palautti ottelun <B> laudan. Jäsennin kestää
        // sen, koska se lukee tunnisteen sivulta. Näkymä ei saa kestää.
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta("7000099")) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(
            BoardUiState.NotABoard(NotABoardKind.DifferentMatch(MatchId("7000099"))),
            malli.state.value,
        )
    }

    @Test
    fun `Next Game saa vaihtaa ottelun`() {
        // Mitattu 22.8.2026: teko meni sivustolla läpi ja palautti seuraavan ottelun, mutta
        // ruutu torjui sen samalla portilla joka on rakennettu pyytämätöntä vaihtoa vastaan.
        // Nappi jonka koko tarkoitus on vaihtaa ottelua ei voi olla väärä vastaus.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto(submit = "Next Game")) }
        val sender = RecordingSender { DgResponse.Ok(lauta("7000099")) }
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Next Game")
            advanceUntilIdle()
        }

        val tila = malli.state.value
        assertTrue("odotettiin lautaa, oli $tila", tila is BoardUiState.Loaded)
        assertEquals("7000099", (tila as BoardUiState.Loaded).board.matchId.value)
    }

    @Test
    fun `paivitys hakee ruudulla olevan laudan eika sita jolla ruutu avattiin`() {
        // Mitattu laiteajossa 23.8.2026: ottelu avattiin listasta, `Next Game` siirsi toiseen
        // otteluun, ja siellä painettu päivitys haki sen osoitteen jolla ruutu oli avattu.
        // Sivusto palautti täsmälleen mitä pyydettiin, ja ottelun vahti torjui sen oikein,
        // joten virhe näkyi vahdin ilmoituksena vaikka vika oli haetussa osoitteessa.
        val toinenPolku = "/bg/move/7000099/12"
        val fetcher = RecordingFetcher { polku ->
            if (polku == toinenPolku) {
                DgResponse.Ok(lautaJossaOmaPolku("7000099", toinenPolku))
            } else {
                DgResponse.Ok(lautaJossaSiirto(submit = "Next Game"))
            }
        }
        val sender = RecordingSender { DgResponse.Ok(lautaJossaOmaPolku("7000099", toinenPolku)) }
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Next Game")
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        // Toinen haku on vaihtuneen ottelun oma polku, ei sisäänkäynti. Osoite on luettu
        // lomakkeen actionista, joten kokoamisen ilmestyminen näkyisi tässä listassa.
        assertEquals(listOf(PELIPOLKU, toinenPolku), fetcher.requested)
        val tila = malli.state.value
        assertTrue("odotettiin lautaa, oli $tila", tila is BoardUiState.Loaded)
        assertEquals("7000099", (tila as BoardUiState.Loaded).board.matchId.value)
    }

    @Test
    fun `siirron jalkeen vaihtunut ottelu torjutaan yha`() {
        // Portin alkuperäinen tapaus 4.8.2026, ja se on tässä nimenomaan siksi että edellinen
        // testi ei saa purkaa sitä: käyttäjä pyysi siirtoa eikä ottelunvaihtoa, joten väärän
        // pelin piirtäminen olisi hiljainen virhe. Ero on pyynnössä eikä vastauksessa.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val sender = RecordingSender { DgResponse.Ok(lauta("7000099")) }
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Submit Move")
            advanceUntilIdle()
        }

        assertEquals(
            BoardUiState.NotABoard(NotABoardKind.DifferentMatch(MatchId("7000099"))),
            malli.state.value,
        )
    }

    @Test
    fun `pisteeton lauta on oma tilansa eika tyhja lauta`() {
        // Mini-skeema pudottaa pistenumerorivin, jolloin jäsennin antaa laudan jonka points
        // on tyhjä. Käyttäjä voi korjata tämän itse asetuksistaan, joten hänen on saatava
        // tietää kumpi vika on kyseessä.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaIlmanNumeroita()) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(
            BoardUiState.NotABoard(NotABoardKind.MiniScheme),
            malli.state.value,
        )
    }

    @Test
    fun `pisteettomyyden syyta ei arvata Miniksi kun skeema on muu`() {
        // Sama tyhjä pistelista, eri skeema. Ennen 13.8.2026 ruutu neuvoi vaihtamaan
        // Board Scheme -asetuksen myös tässä, eli se päätteli syyn seurauksesta ja neuvoi
        // korjaamaan asetuksen jota vika ei koske. Syy luetaan nyt sivun kuvapoluista.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaIlmanNumeroita(hakemisto = "2")) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(
            BoardUiState.NotABoard(NotABoardKind.PositionUnreadable),
            malli.state.value,
        )
    }

    @Test
    fun `arvattu asema piirretaan lautana ja merkitaan`() {
        // Regressiovahti 27.8.2026: tämä sivu torjuttiin hetken omana virhetilanaan, ja se
        // olisi vienyt joka kolmannen siirron virheruutuun (110/312 kaapattua
        // siirtovastausta). Sivu on lauta, ja se mitä siitä on sanottava, sanotaan lipulla.
        val fetcher = RecordingFetcher { DgResponse.Ok(arvattuLauta()) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value
        assertTrue("Arvattu asema ei ollut lauta: $tila", tila is BoardUiState.Loaded)
        assertTrue((tila as BoardUiState.Loaded).board.speculative)
    }

    @Test
    fun `tavallista lautaa ei merkita arvatuksi`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto()) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        val tila = malli.state.value
        assertTrue(tila is BoardUiState.Loaded)
        assertFalse((tila as BoardUiState.Loaded).board.speculative)
    }

    // --- Ottelun chat ---

    @Test
    fun `vastustajan viesti arkistoituu ennen kuin se on ruudulla`() {
        // Tämä on koko sovelluksen syy: sivusto ei säilytä ottelun chattia, ja 27.8.2026
        // asti `ChatParser`illa ei ollut yhtään kutsujaa, joten viesti heitettiin pois.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaChat()) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        val rivi = viestiDao.rows.value.single()
        assertEquals("hyva veto", rivi.body)
        assertEquals(MessageSource.GAME_MESSAGE, rivi.source)
        // Lähettäjä kootaan laudan pelaajapaneeleista: chat-lohko ei kerro kuka kirjoitti.
        assertEquals("coolsuit", rivi.sender)
        assertEquals("coolsuit", rivi.opponent)

        val tila = malli.state.value as BoardUiState.Loaded
        assertEquals("hyva veto", tila.chat?.incoming)
        assertEquals(rivi.id, tila.chat?.incomingId)
    }

    @Test
    fun `sama sivu ei kirjoita samaa viestia kahdesti`() {
        // Päivitys hakee saman sivun uudelleen. Ilman vahtia rivi kirjoittuisi toisen kerran,
        // koska `Message.id` sisältää saapumishetken eikä `insertIfNew` siis estäisi sitä.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaChat()) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals(1, viestiDao.rows.value.size)
    }

    @Test
    fun `chat-sivu ilman viestia tarjoaa silti kirjoituskentan`() {
        // Keskustelun aloittaminen: lomake on sivulla vaikka ketju on tyhjä.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaChat(viesti = null)) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        val chat = (malli.state.value as BoardUiState.Loaded).chat
        assertNotNull(chat)
        assertNull(chat!!.incoming)
        assertTrue(viestiDao.rows.value.isEmpty())
    }

    @Test
    fun `viesti lahtee sivun omalla lomakkeella ja sivun omalla napilla`() {
        val sender = RecordingSender { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(lautaJossaChat()) }, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.sendChat("kiitos", quote = true, submit = "To Top")
            advanceUntilIdle()
        }

        val lahetys = sender.sent.single()
        assertEquals("/bg/move/$OTTELU/1121", lahetys.action)
        assertEquals(FormMethod.POST, lahetys.method)
        // Piilokenttä, viesti, lainaus ja nappi, kaikki sivulta luettuina.
        assertEquals(
            mapOf("commit" to "1", "chat" to "kiitos", "quote" to "on", "submit" to "To Top"),
            lahetys.fields,
        )
    }

    @Test
    fun `keksittya nappia ei voi painaa chatilla`() {
        // Sama portti kuin `BoardForm.press`illä: nappi jota sivu ei tarjoa ei mene läpi.
        val sender = RecordingSender { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(lautaJossaChat()) }, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.sendChat("kiitos", quote = false, submit = "Submit Move")
            advanceUntilIdle()
        }

        assertTrue("Keksitty nappi lähti", sender.sent.isEmpty())
    }

    @Test
    fun `tyhja teksti on poistuminen ilman viestia eika hylatty lahetys`() {
        // Muutettu 27.8.2026 laiteajon jälkeen. Tässä väitettiin siihen asti ettei tyhjä
        // teksti lähde lainkaan, ja perustelu oli pikaviestin portti: tyhjä lähetys on
        // vahinko. Tämä lomake on eri laji, koska sen nappi on ainoa tapa päättää vuoro:
        // selaimessa tyhjä kenttä ja `To Top` on poistuminen ilman viestiä. Vanha ehto
        // esti poistumisen silloin kun ei ollut mitään sanottavaa.
        val sender = RecordingSender { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(lautaJossaChat()) }, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.sendChat("   ", quote = false, submit = "To Top")
            advanceUntilIdle()
        }

        val lahetys = sender.sent.single()
        // Kenttä lähtee mukana tyhjänä, koska selain tekee niin.
        assertEquals("   ", lahetys.fields["chat"])
        assertEquals("To Top", lahetys.fields["submit"])
        // Arkistoon ei silti kirjata viestiä jolla ei ole runkoa.
        assertTrue("Tyhjä viesti arkistoitui", viestiDao.rows.value.none { it.body.isBlank() })
    }

    @Test
    fun `oma viesti arkistoituu ja osoittaa siihen mihin se vastasi`() {
        // Yhteys kirjataan tässä tai ei koskaan: sivustolla ei ole ketjuja.
        val sender = RecordingSender { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(lautaJossaChat()) }, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.sendChat("kiitos", quote = true, submit = "To Top")
            advanceUntilIdle()
        }

        val (saapunut, oma) = viestiDao.rows.value
        assertEquals("kiitos", oma.body)
        assertEquals(OMA_NIMI, oma.sender)
        // Keskustelukumppani on sama kumpaankin suuntaan, lähettäjä ei.
        assertEquals("coolsuit", oma.opponent)
        assertEquals(saapunut.id, oma.replyTo)
    }

    @Test
    fun `epaonnistunut lahetys ei kirjaa omaa viestia`() {
        val sender = RecordingSender { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(lautaJossaChat()) }, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.sendChat("kiitos", quote = false, submit = "To Top")
            advanceUntilIdle()
        }

        // Vain saapunut viesti, ei omaa: lähetystä ei todistettu.
        assertEquals(1, viestiDao.rows.value.size)
    }

    // --- Paluu: vain teko vanhentaa luettelon ---

    @Test
    fun `katsomiskaynti ja paivitys eivat vanhenna luetteloa, teko vanhentaa`() {
        // Mitattu 16.9.2026 (LUEMINUT.md, "Otteluluettelo ei päivity paluussa"): nopat
        // heitettiin, paluu käsin, ja luettelo näytti yhä pelatun rivin. Paluu lukee tämän
        // lipun, ja se saa olla tosi vain teon jälkeen, koska katsomiskäynnin paluun haku
        // olisi juuri se turha pyyntö jonka docs/UI.md kieltää.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto(submit = "Roll Dice")) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            assertFalse("haku ei ole teko", malli.actedOnSite)
            malli.refresh()
            advanceUntilIdle()
            assertFalse("päivitys ei ole teko", malli.actedOnSite)
            malli.press("Roll Dice")
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertTrue(malli.actedOnSite)
    }

    @Test
    fun `verkkoon kaatunut teko vanhentaa luettelon silti`() {
        // Lähetys on voinut mennä perille vaikka vastaus katosi (siksi jono on olemassa),
        // joten rivi on epävarma jo yrityksestä.
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaJossaSiirto(submit = "Roll Dice")) }
        val sender = RecordingSender { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.press("Roll Dice")
            advanceUntilIdle()
        }

        assertTrue(malli.actedOnSite)
    }

    // --- Verkko ja istunto ---

    @Test
    fun `verkkovirheet erottuvat toisistaan`() {
        val offline = malli(RecordingFetcher { DgResponse.Offline(TEST_OFFLINE_CAUSE) })
        val palvelin = malli(RecordingFetcher { DgResponse.ServerError(503) })

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(BoardUiState.Failed(Failure.Offline), offline.state.value)
        assertEquals(BoardUiState.Failed(Failure.Server(503)), palvelin.state.value)
    }

    @Test
    fun `katkennut istunto ei poista tunnuksia taalla`() {
        // Tunnusten omistajuus on TopViewModelilla. Kahdesta poistajasta seuraisi kaksi
        // totuutta samasta asiasta, joten tämä vain kertoo tilanteesta.
        val malli = malli(RecordingFetcher { DgResponse.AuthFailed })

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(BoardUiState.SessionExpired, malli.state.value)
    }

    @Test
    fun `epaonnistunut paivitys ei havita nakyvaa lautaa`() {
        // Minuutin takainen asema on vanhentunutta mutta oikeaa tietoa. Virheruutu sen
        // tilalla olisi vähemmän.
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            fetcher.answer = { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
            malli.refresh()
            advanceUntilIdle()
        }

        val tila = malli.state.value
        assertTrue("Lauta katosi ruudulta", tila is BoardUiState.Loaded)
        assertEquals(false, (tila as BoardUiState.Loaded).refreshing)
    }

    // --- Muistutus: pelaajan oma rivi, ei sivuston teko ---

    @Test
    fun `muistutus kirjataan sille pelille jonka lauta kertoi`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaPistein(vastustaja = 8, oma = 7)) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.addReminder("Think about doubling")
            advanceUntilIdle()
        }

        val rivi = reminderDao.rows.value.single()
        assertEquals(MatchId(OTTELU), rivi.game.matchId)
        // Pistepari on laudalta luettu eikä kutsujan antama, samasta syystä kuin jonon
        // ottelutunniste: pyydetty ei määrää saatua.
        assertEquals(8, rivi.game.opponentScore)
        assertEquals(7, rivi.game.selfScore)
        assertEquals("Think about doubling", rivi.text)
        assertEquals(HETKI, rivi.createdAtEpochMillis)
        // Muistutus ei ole teko sivustolla: mitään ei lähetetty eikä haettu.
        assertTrue("Muistutus lähetti jotain", sender.sent.isEmpty())
        assertEquals(listOf(PELIPOLKU), fetcher.requested)
    }

    @Test
    fun `muistutusta ei kirjata kun sivu ei kertonut pisteita`() {
        // Lauta ilman pelaajapaneeleita, eli sivu josta pelin tunnistetta ei saa. Väärään
        // peliin kirjattu muistutus olisi pahempi kuin puuttuva muistutus.
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.addReminder("Think about doubling")
            advanceUntilIdle()
        }

        assertTrue("Rivi kirjattiin ilman pelin tunnistetta", reminderDao.rows.value.isEmpty())
    }

    @Test
    fun `pelin vaihtuminen vie muistutuksen nakyvista eika poista sita`() {
        var pisteet = 7
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaPistein(vastustaja = 8, oma = pisteet)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.addReminder("Think about doubling")
            advanceUntilIdle()
            assertEquals(listOf("Think about doubling"), malli.reminders.value.map { it.text })

            // Peli päättyi ja pistetilanne muuttui. Elinkaari on kyselyssä: rivi on yhä
            // kannassa, mutta se ei kuulu tähän peliin eikä siksi näy.
            pisteet = 8
            malli.refresh()
            advanceUntilIdle()

            assertTrue("Edellisen pelin muistutus näkyy yhä", malli.reminders.value.isEmpty())
            assertEquals(1, reminderDao.rows.value.size)
        }
    }

    @Test
    fun `muistutuksen poisto vie rivin`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaPistein(vastustaja = 8, oma = 7)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.addReminder("Think about doubling")
            advanceUntilIdle()
            malli.removeReminder(malli.reminders.value.single().id)
            advanceUntilIdle()
        }

        assertTrue(reminderDao.rows.value.isEmpty())
    }

    // --- Merkitty asema: pelaajan oma kirjanmerkki analyysiin, ei sivuston teko ---

    @Test
    fun `merkki kirjataan laudan pelille ja siirtonumerolle`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaPistein(vastustaja = 8, oma = 7)) }
        val sender = RecordingSender()
        val malli = malli(fetcher, sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.markPosition("  recube?  ")
            advanceUntilIdle()
        }

        val rivi = markDao.rows.value.single()
        assertEquals(MatchId(OTTELU), rivi.game.matchId)
        assertEquals(8, rivi.game.opponentScore)
        assertEquals(7, rivi.game.selfScore)
        // Siirtonumero on laudan otsikosta eikä kutsujan antama.
        val odotettu = (malli.state.value as BoardUiState.Loaded).board.moveNumber
        assertEquals(odotettu, rivi.moveNumber)
        assertEquals("recube?", rivi.note)
        assertEquals(HETKI, rivi.createdAtEpochMillis)
        assertEquals(listOf(rivi), malli.marks.value)
        // Merkki ei ole teko sivustolla: mitään ei lähetetty eikä haettu.
        assertTrue("Merkki lähetti jotain", sender.sent.isEmpty())
        assertEquals(listOf(PELIPOLKU), fetcher.requested)
    }

    @Test
    fun `merkkia ei kirjata kun sivu ei kertonut pisteita`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta(OTTELU)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.markPosition("")
            advanceUntilIdle()
        }

        assertTrue("Merkki kirjattiin ilman pelin tunnistetta", markDao.rows.value.isEmpty())
    }

    @Test
    fun `pelin vaihtuminen vie merkin laudan alta muttei kannasta`() {
        var pisteet = 7
        val fetcher = RecordingFetcher { DgResponse.Ok(lautaPistein(vastustaja = 8, oma = pisteet)) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.markPosition("")
            advanceUntilIdle()
            assertEquals(1, malli.marks.value.size)

            pisteet = 8
            malli.refresh()
            advanceUntilIdle()

            // Laudan alla ei enää, listalla yhä: elinkaari on käyttäjän poisto.
            assertTrue(malli.marks.value.isEmpty())
            assertEquals(1, markDao.rows.value.size)

            malli.removeMark(markDao.rows.value.single().id)
            advanceUntilIdle()
            assertTrue(markDao.rows.value.isEmpty())
        }
    }

    private companion object {
        /**
         * Kirjautunut käyttäjä. Sama nimi kuin `lautaPistein`in alemmassa paneelissa, koska
         * juuri siitä parista vastustaja tunnistetaan.
         */
        const val OMA_NIMI = "tommih"
        const val OTTELU = "7000002"
        const val PELIPOLKU = "/bg/move/$OTTELU/541"

        /** Kiinnitetty kello, jotta kirjatun teon hetkestä voi väittää jotain. */
        const val HETKI = 1_700_000_000_000L

        /**
         * Jonon pikaviestisivu lautapolun vastauksena, mitattu 13.9.2026 klo 01.00.16
         * (`sessio-13-9-yo/0008`, 435 tavua, osoite `/bg/move/<ottelu>/<siirto>`). Nimi
         * anonymisoitu, runko sanasta sanaan.
         */
        val PIKAVIESTI_LAUDALLA = """
            <html><head><title>DailyGammon Quick Message</title></head><body>
            <h3>You have received the following quick message from <a href="/bg/user/91004">vastapelaaja</a></h3>
            <pre>Thank you too. Good luck in your others.
            <hr><form action="/bg/sendmsg/91004" method="post"><input type="text" name="text" maxlength="80"><input type="submit" value="Send Reply"></form><hr>
            </pre><p><a href="/bg/nextgame">Next &gt;&gt;</a>
            </body></html>
        """.trimIndent()

        /**
         * Ottelun päättymissivu, mitattu 31.8.2026 (fixture `match_over.html`). Tulos,
         * pistetaulukko ja sivun omat linkit; ei chat-kenttää.
         */
        val PAATTYNYT = """
            <HTML><HEAD><TITLE>
            DailyGammon Match 7000006, Move 641
            </TITLE></HEAD><BODY>
            <center><h3><a href=/bg/event/900006>Slower Sevens #4562</a>, Round 3</h3></center>
            <h4>pelaaja wins 2 points and the match.</h4><table><caption><u>Score</u></caption>
            <tr><th>Length<th>7
            <tr><td><b><a href=/bg/user/90001>pelaaja</a></b><td>7
            <tr><td><b><a href=/bg/user/91002>vastapelaaja</a></b><td>6
            </table>
            <p><A HREF=/bg/nextgame><i>Next Game&gt;&gt</i></A>
            <P>
            <A HREF=/bg/game/7000006/1/list#end>&lt;&lt;Review Game</A> |
            <A HREF=/bg/nextgame?skip=7000006>Skip Game</A>
            </BODY></HTML>
        """.trimIndent()

        /** Sama sivu chat-kentällä, eli se muunnelma jolla kiitokset kirjoitetaan. */
        val PAATTYNYT_CHAT = """
            <HTML><HEAD><TITLE>
            DailyGammon Match 7000005, Move 929
            </TITLE></HEAD><BODY>
            <center><h3><a href=/bg/event/900005>Nine Lives #4271</a>, Round 5</h3></center>
            (<a href=/help/#predict>Predicted</a> result)
            <h4>vastapelaaja wins 2 points and the match.</h4><table><caption><u>Score</u></caption>
            <tr><th>Length<th>9
            <tr><td><b><a href=/bg/user/91002>vastapelaaja</a></b><td>9
            <tr><td><b><a href=/bg/user/90001>pelaaja</a></b><td>7
            </table>
            <form method=post action=/bg/move/7000005/2097><input type=hidden name=commit value=1>
            <b>You may chat with <i>vastapelaaja</i> here:</b><br>
            <textarea name=chat rows=5 cols=80 wrap=physical></textarea><br>
            <p><input type=submit name=submit value="Next Game">
            <input type=submit name=submit value="To Top"></form>
            <P>
            <A HREF=/bg/game/7000005/0/list#end>&lt;&lt;Review Game</A> |
            <A HREF=/bg/nextgame?skip=7000005>Skip Game</A>
            </BODY></HTML>
        """.trimIndent()

        /**
         * Päättymissivu jolla vastustaja on ehtinyt kirjoittaa. Ketju on `<pre>`-lohkossa
         * kuten lautasivullakin, koska lukija on sama.
         */
        val PAATTYNYT_VIESTI = PAATTYNYT_CHAT.replace(
            "<b>You may chat with <i>vastapelaaja</i> here:</b><br>",
            "<b>vastapelaaja says:</b><br><pre>gg, well played</pre>" +
                "<b>You may chat with <i>vastapelaaja</i> here:</b><br>",
        )

        /**
         * Kolmas muunnelma, mitattu laitteelta 1.9.2026: nappilomake **ilman chat-kenttää**.
         *
         * Juuri tämä sivu jätti ruudun umpikujaksi. `Next Game` ja `To Top` ovat lomakkeen
         * nappeja, ja kumpikaan lukija ei nähnyt niitä: chat-lukija vaatii tekstikentän ja
         * `MatchOverParser` etsi vain linkkiä.
         */
        val PAATTYNYT_NAPIT = PAATTYNYT.replace(
            "<p><A HREF=/bg/nextgame><i>Next Game&gt;&gt</i></A>",
            "<form method=post action=/bg/move/7000006/1491>" +
                "<input type=hidden name=commit value=1>" +
                "<p><input type=submit name=submit value=\"Next Game\"> " +
                "<input type=submit name=submit value=\"To Top\"></form>",
        )

        /**
         * Neljäs muunnelma, mitattu 7.9.2026: nappilomake **ja** vastustajan viesti, ei
         * kenttää. `<PRE>` jää sivulla sulkematta, ja se on tässä samoin.
         */
        val PAATTYNYT_SANOO = PAATTYNYT_NAPIT.replace(
            "<input type=hidden name=commit value=1>",
            "<input type=hidden name=commit value=1>" +
                "<b><i>vastapelaaja says:</i></b>\n<PRE>Another good win! Keep it going!\n",
        )

        /** Sivun antama siirtolinkki. Sama merkkijono sekä sivulla että väitteissä. */
        const val SIIRTOLINKKI = "$PELIPOLKU?move=x"

        /**
         * Lauta jolla on sekä sivun oma siirtolinkki että täysi lomake.
         *
         * Nappulakuva on ankkurin sisällä ja ALT kertoo nappulan, koska jäsennin pudottaa
         * siirtolinkin pisteeltä jolla ei ole nappuloita. Lomake on `method=get` niin kuin
         * sivustolla mitattuna on: piilokenttä, nappi, ei valintaruutua.
         */
        /**
         * Sivu jonka ainoa nappi on itsenäinen painallus, eli lomake ilman kokoamiskenttää.
         *
         * Uudelleenyritystä koskevat testit käyttävät tätä eivätkä kootun siirron sivua, ja
         * syy on mitattu 10.8.2026: koottu siirto ei kestä sivun uudelleenhakua, joten sitä
         * ei yritetä uudelleen lainkaan. Testi joka uusisi `Submit Move`n kuvaisi käytöstä
         * jota ei ole.
         */
        fun lautaJossaNoppa(): String = lautaJossaSiirto(pendingMove = null, submit = "Roll Dice")

        fun lautaJossaSiirto(
            pendingMove: String? = "rrmm",
            submit: String = "Submit Move",
        ): String = buildString {
            append("<HTML><HEAD><TITLE>Match $OTTELU, Move 579</TITLE></HEAD><BODY><TABLE>")
            append("<TR>")
            (13..24).forEach { append("<TD>$it</TD>") }
            append("</TR><TR>")
            (13..24).forEach {
                append("""<TD><A HREF="$SIIRTOLINKKI"><IMG SRC=/images/2/pt_y1_down.gif ALT="y1"></A></TD>""")
            }
            append("</TR><TR>")
            (13..24).forEach { append("""<TD><IMG SRC=/images/2/pt_y1_up.gif ALT="y1"></TD>""") }
            append("</TR><TR>")
            (12 downTo 1).forEach { append("<TD>$it</TD>") }
            append("</TR></TABLE>")
            append("""<FORM ACTION=$PELIPOLKU METHOD=GET>""")
            pendingMove?.let { append("""<INPUT TYPE=hidden NAME=move VALUE=$it>""") }
            append("""<INPUT TYPE=submit NAME=submit VALUE="$submit">""")
            append("</FORM></BODY></HTML>")
        }

        /**
         * Sivu jolla ei ole lautaa eikä mitään tunnistettavaa. 29.8.2026 asti tämä esitti
         * tyhjää Top Pagea; nyt se esittää sivua jota mikään predikaatti ei tunne, koska
         * Top Pagen tuntomerkki (`days_to_view`-linkki, ks. `DgPages.isTopPage`) siirtyi
         * [TOP_SIVU]lle.
         */
        const val EI_LAUTAA =
            "<HTML><HEAD><TITLE>DailyGammon</TITLE></HEAD>" +
                "<BODY>Something unrecognisable.</BODY></HTML>"

        /**
         * Tyhjän Top Pagen olennaiset piirteet: `days_to_view`-linkki jota ei ole millään
         * muulla sivulajilla (mitattu 21.8.2026 kaikista fixtureista), eikä lautaa.
         */
        const val TOP_SIVU =
            "<HTML><HEAD><TITLE>DailyGammon</TITLE></HEAD>" +
                "<BODY><A HREF=\"/bg/top?days_to_view=30\">30 days</A>" +
                "There are no matches where you can move.</BODY></HTML>"

        /**
         * Riittävän lauta ollakseen lauta: 24 pistekuvaa ja otsikko josta ottelutunnisteen
         * saa. Numerorivi antaa pisteille numerot.
         *
         * Kootaan silmukalla eikä kirjoiteta 24 riviä käsin: käsin kirjoitettuun listaan
         * jäisi ennen pitkää virhe joka näyttäisi jäsennysvirheeltä.
         */
        /**
         * Toisen ottelun lauta joka kantaa **oman lukutilan polkunsa** lomakkeen actionissa.
         *
         * Erillään [lauta]sta, koska ero on juuri se jota päivitys tarvitsee: vuorollinen
         * lauta tarjoaa lomakkeen, vuoroton ei tarjoa `/bg/move/`-osoitetta lainkaan.
         */
        fun lautaJossaOmaPolku(matchId: String, polku: String): String =
            lauta(matchId).replace(
                "</BODY></HTML>",
                "<FORM ACTION=$polku METHOD=GET>" +
                    """<INPUT TYPE=submit NAME=submit VALUE="Roll Dice"></FORM></BODY></HTML>""",
            )

        fun lauta(matchId: String): String = buildString {
            append("<HTML><HEAD><TITLE>Match $matchId, Move 579</TITLE></HEAD><BODY><TABLE>")
            append("<TR>")
            (13..24).forEach { append("<TD>$it</TD>") }
            append("</TR><TR>")
            (13..24).forEach { append("""<TD><IMG SRC=/images/2/pt_y1_down.gif ALT="_"></TD>""") }
            append("</TR><TR>")
            (13..24).forEach { append("""<TD><IMG SRC=/images/2/pt_y1_up.gif ALT="_"></TD>""") }
            append("</TR><TR>")
            (12 downTo 1).forEach { append("<TD>$it</TD>") }
            append("</TR></TABLE></BODY></HTML>")
        }

        /** Sama lauta sivun omalla kierrosotsikolla, muoto mitattu (`<h3>..., Round 4</h3>`). */
        fun lautaKierroksella(matchId: String, round: Int): String = lauta(matchId).replace(
            "<BODY>",
            "<BODY><center><h3><a href=/bg/event/900001>Some Event #1</a>, Round $round</h3></center>",
        )

        /**
         * Lauta jolla on molempien pelaajien paneelit, eli sivu josta pelin tunniste on
         * luettavissa. Paneelin muoto on `BoardParser`in vaatima: käyttäjälinkki ja samassa
         * solussa `score:`.
         */
        fun lautaPistein(vastustaja: Int, oma: Int): String = buildString {
            append("<HTML><HEAD><TITLE>Match $OTTELU, Move 579</TITLE></HEAD><BODY><TABLE>")
            append("<TR>")
            (13..24).forEach { append("<TD>$it</TD>") }
            append("</TR><TR>")
            (13..24).forEach { append("""<TD><IMG SRC=/images/2/pt_y1_down.gif ALT="_"></TD>""") }
            append("</TR><TR>")
            (13..24).forEach { append("""<TD><IMG SRC=/images/2/pt_y1_up.gif ALT="_"></TD>""") }
            append("</TR><TR>")
            (12 downTo 1).forEach { append("<TD>$it</TD>") }
            append("</TR><TR>")
            append("""<TD><A HREF=/bg/user/16674>coolsuit</A> 167 pips score: $vastustaja</TD>""")
            append("</TR><TR>")
            append("""<TD><A HREF=/bg/user/20311>tommih</A> 157 pips score: $oma</TD>""")
            append("</TR></TABLE></BODY></HTML>")
        }

        /**
         * Siirron jälkeinen sivu: lauta, pelaajapaneelit ja chat-lomake.
         *
         * Lomake on sivun oma muoto mitattuna (`raakasivut/`, fixture `chat_thread.html`):
         * POST lautaosoitteeseen, piilokenttä `commit=1`, `<pre>` jossa lainattu osa on
         * `>`-alkuisilla riveillä, oletuksena valittu `quote` ja napit `Next Game` ja
         * `To Top`. Lomakkeen osoite eroaa haetusta polusta tarkoituksella, koska niin se
         * eroaa sivullakin: 541 haettuna, 1121 lomakkeessa.
         */
        fun lautaJossaChat(viesti: String? = "hyva veto"): String =
            lautaPistein(vastustaja = 0, oma = 0).replace(
                "</TABLE></BODY></HTML>",
                buildString {
                    append("</TABLE>")
                    append("""<FORM METHOD=post ACTION="/bg/move/$OTTELU/1121">""")
                    append("""<INPUT TYPE=hidden NAME=commit VALUE=1>""")
                    append("<b><i>Chat:</i></b>")
                    append("<pre>&gt;vanha rivi\n\n")
                    viesti?.let { append(it) }
                    append("\n</pre>")
                    append("""<INPUT TYPE=checkbox NAME=quote checked> Quote previous message""")
                    append("""<TEXTAREA NAME=chat ROWS=5 COLS=80></TEXTAREA>""")
                    append("""<INPUT TYPE=submit NAME=submit VALUE="Next Game">""")
                    append("""<INPUT TYPE=submit NAME=submit VALUE="To Top">""")
                    append("</FORM></BODY></HTML>")
                },
            )

        /**
         * Lauta jonka sivu kysyy siirtoa **tulevasta** asemasta.
         *
         * Ero tavalliseen lautaan on yksi virke sivun tekstissä, ja se on tarkoituksella
         * ainoa ero: juuri se on koko portin peruste. Virke on `ChatParser`in mittaama
         * erotin eikä keksitty tähän.
         */
        fun arvattuLauta(): String = lautaJossaSiirto().replace(
            "<BODY>",
            "<BODY><b>What will you do if the game proceeds this way?</b><br>",
        )

        /**
         * Mini-skeeman olennainen piirre: pistekuvat ovat tallessa mutta numerorivi puuttuu,
         * joten yhtäkään pistettä ei voi numeroida.
         */
        /**
         * Lauta jolla on nappulat muttei pistenumerorivia.
         *
         * [hakemisto] on skeeman numero kuvapolussa: `3` on Mini eli sivuston oma kapean
         * näytön lauta, ja se on oletus koska se on ainoa tunnettu syy tähän tilanteeseen.
         * Muu arvo tuottaa saman tyhjän ilman tunnettua syytä.
         */
        fun lautaIlmanNumeroita(hakemisto: String = "3"): String = buildString {
            append("<HTML><HEAD><TITLE>Match $OTTELU, Move 579</TITLE></HEAD><BODY><TABLE><TR>")
            repeat(24) {
                append("""<TD><IMG SRC=/images/$hakemisto/pt_y1_up.gif ALT="y1"></TD>""")
            }
            append("</TR></TABLE></BODY></HTML>")
        }
    }
}

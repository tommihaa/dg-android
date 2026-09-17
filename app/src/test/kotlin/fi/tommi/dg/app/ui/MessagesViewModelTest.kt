package fi.tommi.dg.app.ui

import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.app.session.MessageFilterStore
import fi.tommi.dg.app.session.PhraseBook
import fi.tommi.dg.app.session.PhraseStore
import fi.tommi.dg.data.ReminderBook
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.domain.ReplyForm
import fi.tommi.dg.net.DgResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Viestiruudun näkymämalli, ja ennen kaikkea ne neljä ehtoa jotka suojaavat peruuttamatonta
 * tekoa: haku vain pyynnöstä, osoite luettuna, yksi kerrallaan, kirjoitus ennen näyttöä.
 *
 * HTML on synteettistä samasta syystä kuin `BoardViewModelTest`issä: jäsennystarkkuutta
 * koskevat väitteet asuvat core-scrapessa aitoja palvelintavuja vasten, ja nämä testit
 * väittävät vain haarautumisesta ja siitä mitä polkuja pyydettiin.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {

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
     * Kirjaa jokaisen lähetetyn lomakkeen, jotta testi voi väittää mitä EI lähetetty.
     *
     * Sama muoto kuin [RecordingFetcher]illa ja samasta syystä: lähetys on peruuttamaton
     * teko, joten vahvimmat väitteet koskevat sitä ettei sitä tapahtunut.
     */
    private class RecordingSender(var answer: (FormSubmission) -> DgResponse) : FormSender {
        val sent = mutableListOf<FormSubmission>()
        override fun send(submission: FormSubmission): DgResponse {
            sent += submission
            return answer(submission)
        }
    }

    private val dao = FakeArchive()

    @Before
    fun asetaDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun palautaDispatcher() {
        Dispatchers.resetMain()
    }

    /** Otteluluettelon kertoma polku. Testi voi siirtää sen arvoa kesken ajon. */
    private val upstream = MutableStateFlow<String?>(JONO)

    /** Suodattimen säilö muistissa, jotta testi voi väittää mitä talletettiin. */
    private class FakeFilterStore(private var stored: String? = null) : MessageFilterStore {
        override fun get(): String? = stored
        override fun save(opponent: String?) {
            stored = opponent
        }
    }

    /** Fraasien säilö muistissa. Alkaa oletuksesta kuten uusi asennus. */
    private class FakePhraseStore(var stored: List<String> = PhraseStore.DEFAULT) : PhraseStore {
        var saves = 0
        override fun get(): List<String> = stored
        override fun save(phrases: List<String>) {
            stored = phrases
            saves++
        }
    }

    private fun malli(
        fetcher: RecordingFetcher,
        queuePath: String? = JONO,
        sender: RecordingSender = RecordingSender { DgResponse.Ok(KUITTAUS) },
        filterStore: MessageFilterStore = FakeFilterStore(),
        phraseStore: PhraseStore = FakePhraseStore(),
        reminders: ReminderBook = FakeReminders(),
    ) = MessagesViewModel(
        pages = fetcher,
        forms = sender,
        archive = dao,
        queuePathUpstream = upstream.also { it.value = queuePath },
        now = { HETKI },
        self = { OMA_NIMI },
        filterStore = filterStore,
        phraseBook = PhraseBook(phraseStore),
        reminders = reminders,
        io = dispatcher,
    )

    // --- Ehto 1: haku vain käyttäjän pyynnöstä ---

    @Test
    fun `ruudun avaaminen ei hae jonosta mitaan`() {
        // Vahvin väite jonka tämä luokka voi tehdä. Muut näkymämallit hakevat käynnistyessään,
        // ja juuri se olisi tässä ruudussa kohteen syöminen ilman että kukaan pyysi.
        val fetcher = RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertTrue("Pyyntö lähti ilman että sitä pyydettiin", fetcher.requested.isEmpty())
        assertEquals(QueueUiState.Idle, malli.queue.value)
        assertEquals(0, dao.rows.value.size)
    }

    @Test
    fun `ilman linkkia ei haeta mitaan`() {
        // Osoitetta ei koota. Ilman sivun antamaa linkkiä hakua ei ole, eikä nappia näytetä.
        val fetcher = RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }
        val malli = malli(fetcher, queuePath = null)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertTrue(fetcher.requested.isEmpty())
        assertEquals(QueueUiState.Idle, malli.queue.value)
    }

    // --- Ehto 2: osoite luetaan sivulta ---

    @Test
    fun `myohassa saapuva polku ei jaa huomaamatta`() {
        // Mitattu vikamuoto 9.8.2026: ruutu luki polun kerran avautuessaan, joten ruudun
        // avaaminen ennen kuin luettelo oli latautunut jätti napin pois ja ruutu sanoi ettei
        // mitään odota. Hiljainen väärä nolla, eli se suunta jossa virhe luetaan vastaukseksi.
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, queuePath = null)

        runTest(dispatcher) {
            advanceUntilIdle()
            assertEquals(null, malli.queuePath.value)

            upstream.value = JONO
            advanceUntilIdle()
        }

        assertEquals(JONO, malli.queuePath.value)
    }

    @Test
    fun `ilmoituksen katoaminen vie napin`() {
        // Toinen suunta samasta säännöstä. Otteluluettelo on se joka tietää onko jotain
        // odottamassa, joten ruutu seuraa sitä eikä muista omaa käsitystään.
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            advanceUntilIdle()
            upstream.value = null
            advanceUntilIdle()
        }

        assertEquals(null, malli.queuePath.value)
    }

    @Test
    fun `haku osuu tasmalleen siihen polkuun jonka sivu antoi`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals(listOf(JONO), fetcher.requested)
    }

    @Test
    fun `seuraava haku kayttaa sivun omaa next-linkkia`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        // Jälkimmäinen polku on juuri haetun sivun `Next >>`, ei ensimmäisen toisto eikä
        // koottu osoite.
        assertEquals(listOf(JONO, SEURAAVA), fetcher.requested)
    }

    // --- Ehto 3: yksi kerrallaan ---

    @Test
    fun `kaksoisnapautus ei kuluta kahta kohdetta`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals(listOf(JONO), fetcher.requested)
        assertEquals(1, dao.rows.value.size)
    }

    // --- Ehto 4: kirjoitus ennen näyttöä ---

    @Test
    fun `viesti on kannassa siina hetkessa kun se on tilassa`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        val tila = malli.queue.value
        assertTrue("Tila ei ollut Saved: $tila", tila is QueueUiState.Saved)
        val viesti = (tila as QueueUiState.Saved).message
        assertEquals(MessageSource.QUICK_MESSAGE, viesti.source)
        assertEquals("Great match!", viesti.body)
        assertEquals(listOf(viesti.id), dao.rows.value.map { it.id })
        // Tilikohtainen arkisto (16.9.2026): haettu viesti kirjataan kirjautuneelle tilille,
        // jotta toisella tilillä kirjautunut ei näe sitä eikä vie sitä tiedostoonsa.
        assertEquals(listOf(OMA_NIMI), dao.accounts)
    }

    @Test
    fun `vienti nimeaa tilin jonka arkisto se on`() {
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            val json = malli.exportJson(HETKI)

            assertTrue(json.contains("\"account\": \"$OMA_NIMI\""))
        }
    }

    // --- Tuonti (Tommin tilaus 16.9.2026: laajennus muistutuksiin ja fraaseihin, vain lisää) ---

    @Test
    fun `vienti kantaa muistutukset ja fraasit ja tuonti lisaa vain puuttuvat`() {
        val muistutukset = FakeReminders()
        val fraasit = FakePhraseStore()
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, phraseStore = fraasit, reminders = muistutukset)
        val peli = fi.tommi.dg.domain.GameKey(fi.tommi.dg.domain.MatchId("5302842"), 8, 7)

        runTest(dispatcher) {
            muistutukset.add(peli, "Think about doubling", 2_000)
            malli.addPhrase("gl")
            malli.fetchNext()
            advanceUntilIdle()
            val json = malli.exportJson(HETKI)
            assertTrue(json.contains("Think about doubling"))
            assertTrue(json.contains("\"gl\""))

            // Sama tiedosto takaisin samalle laitteelle: mitään ei kirjoiteta, ja luvut
            // sanovat sen ääneen.
            malli.importJson(json)
            val sama = malli.import.value as ImportUiState.Done
            assertEquals(0, sama.messages)
            assertEquals(0, sama.reminders)
            assertEquals(0, sama.phrases)
            assertEquals(1, sama.alreadyHere)
            assertEquals(1, dao.rows.value.size)
            assertEquals(1, muistutukset.count())

            // Tiedosto jossa on uutta: vain uusi kirjoitetaan, laitteen omat pysyvät.
            val toinen = json
                .replace("Great match!", "Good roll")
                .replace("Think about doubling", "Ask about the bar point")
                .replace("\"gl\"", "\"nice\"")
            malli.importJson(toinen)
            val uusi = malli.import.value as ImportUiState.Done
            assertEquals(1, uusi.messages)
            assertEquals(1, uusi.reminders)
            assertEquals(1, uusi.phrases)
            assertEquals(0, uusi.alreadyHere)
            assertEquals(null, uusi.otherAccount)
            assertEquals(2, dao.rows.value.size)
            assertEquals(listOf(OMA_NIMI, OMA_NIMI), dao.accounts)
            assertEquals(2, muistutukset.count())
            assertEquals(listOf("hi", "gg", "ty gg u2", "gl", "nice"), malli.phrases.value)

            malli.importDismissed()
            assertEquals(ImportUiState.Idle, malli.import.value)
        }
    }

    @Test
    fun `toisen tilin tiedosto kirjataan sille tilille ja ruutu saa nimen`() {
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            malli.fetchNext()
            advanceUntilIdle()
            val json = malli.exportJson(HETKI)
                .replace("\"account\": \"$OMA_NIMI\"", "\"account\": \"toinen\"")
                .replace("Great match!", "Good roll")

            malli.importJson(json)

            val tulos = malli.import.value as ImportUiState.Done
            assertEquals(1, tulos.messages)
            assertEquals("toinen", tulos.otherAccount)
            assertEquals(listOf(OMA_NIMI, "toinen"), dao.accounts)
        }
    }

    @Test
    fun `vanha tiedosto ilman tilia kirjataan kirjautuneelle`() {
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            malli.fetchNext()
            advanceUntilIdle()
            val json = malli.exportJson(HETKI)
                .replace("\"account\": \"$OMA_NIMI\"", "\"account\": null")
                .replace("Great match!", "Good roll")

            malli.importJson(json)

            assertEquals(null, (malli.import.value as ImportUiState.Done).otherAccount)
            assertEquals(listOf(OMA_NIMI, OMA_NIMI), dao.accounts)
        }
    }

    @Test
    fun `muu tiedosto ei kirjoita mitaan`() {
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            malli.importJson("{\"format\": \"jokin.muu\"}")

            assertEquals(ImportUiState.NotAnArchive, malli.import.value)
            assertTrue(dao.rows.value.isEmpty())
        }
    }

    // --- Jonossa oli jotain muuta ---

    @Test
    fun `lauta jonossa ei tallenna mitaan`() {
        // Tavallinen tilanne eikä vika: sama osoite tarjoilee myös pelilautoja. Väärin
        // tallennettuna se olisi valeviesti arkistossa, eikä kulunutta kohdetta saa takaisin
        // tarkistamaan mikä se oli.
        val fetcher = RecordingFetcher { DgResponse.Ok(lauta()) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals(QueueUiState.NotAMessage(NotAMessageKind.Board), malli.queue.value)
        assertEquals(0, dao.rows.value.size)
    }

    @Test
    fun `tuntematon sivu ei tallenna mitaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok("<html><body><p>Ei mitään</p></body></html>") }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals(QueueUiState.NotAMessage(NotAMessageKind.Unreadable), malli.queue.value)
        assertEquals(0, dao.rows.value.size)
    }

    @Test
    fun `tuntematon otsikko tallennetaan silti`() {
        // Vastapari edelliselle, ja tässä on koko 9.8.2026 tehdyn sopimusmuutoksen arvo:
        // sivu jäsentyi viestiksi mutta otsikkoa ei tunnistettu. Se tallennetaan lajilla
        // UNKNOWN otsikko tallessa, koska toista tilaisuutta ei tule.
        val fetcher = RecordingFetcher { DgResponse.Ok(TUNTEMATON) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        val tila = malli.queue.value
        assertTrue("Tila ei ollut Saved: $tila", tila is QueueUiState.Saved)
        val viesti = (tila as QueueUiState.Saved).message
        assertEquals(MessageSource.UNKNOWN, viesti.source)
        assertEquals("You have received something entirely new", viesti.rawHeader)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `sivuston ilmoitusta ei tallenneta mutta se nakyy`() {
        // Tommin päätös 16.8.2026: tiedotteita tulee muutama päivässä, ja kuukausia
        // vanhassa arkistossa ne hukuttaisivat sen yksityisen keskustelun jonka vuoksi
        // sovellus on olemassa.
        val malli = malli(RecordingFetcher { DgResponse.Ok(TIEDOTE) })

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        val tila = malli.queue.value
        assertTrue("Tila ei ollut Announcement: ${'$'}tila", tila is QueueUiState.Announcement)
        assertEquals(MessageSource.ANNOUNCEMENT, (tila as QueueUiState.Announcement).message.source)
        // Kanta pysyi tyhjänä. Tämä on koko päätöksen mitattava puoli.
        assertEquals(0, dao.rows.value.size)
        // **Teksti kulkee silti tilan mukana**, koska haku kulutti kohteen sivustolla eikä
        // pudotettua saa takaisin. Ilman tätä ehtoa päätös olisi hiljainen hävitys.
        assertEquals(
            true,
            (tila).message.body.contains("has a winner"),
        )
        // Jono eteni, vaikka mitään ei tallennettu.
        assertEquals(SEURAAVA, malli.queuePath.value)
    }

    @Test
    fun `tallennettu viesti kantaa vastustajan nimen`() {
        // Arkiston pääsypolku on pelaaja, ja nimen on oltava viestissä itsessään: ottelun
        // tunniste orpoutuu kun sivusto pudottaa päättyneen ottelun listalta.
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals(listOf("vastapelaaja"), dao.rows.value.map { it.opponent })
    }

    // --- Verkko ja istunto ---

    @Test
    fun `verkkovirhe ei kuluta eika tallenna`() {
        val fetcher = RecordingFetcher { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals(QueueUiState.Failed(Failure.Offline), malli.queue.value)
        assertEquals(0, dao.rows.value.size)
        // Polku ei siirtynyt, joten uudelleenyritys osuu samaan kohteeseen.
        assertEquals(JONO, malli.queuePath.value)
    }

    @Test
    fun `katkennut istunto ei poista tunnuksia taalla`() {
        val malli = malli(RecordingFetcher { DgResponse.AuthFailed })

        runTest(dispatcher) {
            // Luettelon polku saapuu virtana, joten se on annettava mallille ennen kuin
            // nappia painetaan. Sama järjestys kuin oikeassa ruudussa.
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals(QueueUiState.SessionExpired, malli.queue.value)
    }

    // --- Vastauksen lahetys (22.8.2026) ---

    @Test
    fun `vastauslomake tulee sivulta ja tallentuu viestin mukana`() {
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        // Lomake on viestissä itsessään (30.8.2026), koska arkiston viestiin vastataan
        // jälkikäteen. Se on myös kannassa, eli säilyy uudelleenkäynnistyksen yli.
        val tila = malli.queue.value as QueueUiState.Saved
        val lomake = requireNotNull(tila.message.replyForm)
        assertEquals("/bg/sendmsg/91004", lomake.action)
        assertEquals(80, lomake.maxLength)
        assertEquals("/bg/sendmsg/91004", dao.rows.value.single().replyForm?.action)
        // Kenttä aukeaa juuri saapuneen alle itsestään, kuten ennen klikattavuutta.
        assertEquals(tila.message.id, malli.replyTarget.value?.id)
    }

    @Test
    fun `tiedotteeseen ei voi vastata`() {
        // Ilmoituksella ei ole lähettäjää eikä vastauslomaketta, ja tila on eri.
        // Vastauskenttä ei siis voi ilmestyä, vaikka ruutu kirjoitettaisiin väärin.
        val malli = malli(RecordingFetcher { DgResponse.Ok(TIEDOTE) })

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertTrue(malli.queue.value is QueueUiState.Announcement)
    }

    @Test
    fun `ilman haettua viestia ei laheteta mitaan`() {
        // Vahvin väite tässä osiossa: lähetys on sidottu tilaan eikä nappiin. Ruutu joka
        // kutsuisi tätä väärässä hetkessä ei tee mitään.
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.draftChanged("Moi")
            malli.sendReply()
            advanceUntilIdle()
        }

        assertTrue("Lähetys lähti ilman viestiä johon vastata", lahettaja.sent.isEmpty())
    }

    @Test
    fun `tyhjaa vastausta ei laheteta`() {
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.sendReply()
            advanceUntilIdle()
        }

        assertTrue("Tyhjä lähetys kuluttaisi teon", lahettaja.sent.isEmpty())
    }

    @Test
    fun `onnistunut vastaus menee arkistoon omana viestina`() {
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.draftChanged("Great match!")
            malli.sendReply()
            advanceUntilIdle()
        }

        assertEquals(ReplyUiState.Sent, malli.reply.value)
        assertEquals("Onnistunut lähetys tyhjentää luonnoksen", "", malli.draft.value)

        // Lomake meni sellaisenaan: osoite ja kentän nimi sivulta, ei koodista.
        val lahetetty = lahettaja.sent.single()
        assertEquals("/bg/sendmsg/91004", lahetetty.action)
        assertEquals(mapOf("text" to "Great match!"), lahetetty.fields)

        // Kaksi riviä: saapunut ja lähetetty. Sivusto ei säilytä kumpaakaan.
        assertEquals(2, dao.rows.value.size)
        val oma = dao.rows.value.last()
        assertEquals("Lähettäjä on käyttäjä itse", OMA_NIMI, oma.sender)
        assertEquals("Keskustelukumppani on sama kumpaankin suuntaan", "vastapelaaja", oma.opponent)
        assertEquals("Great match!", oma.body)
        // Otsikkorivi on kuittaussivun oma lause (Tommin päätös 22.8.2026), ei sovelluksen
        // kirjoittama. Suunta on siitä johdettavissa uudelleen.
        assertEquals("Your message has been sent to vastapelaaja", oma.rawHeader)
    }

    @Test
    fun `kaksoisnapautus lahettaa yhden viestin`() {
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.draftChanged("Moi")
            malli.sendReply()
            malli.sendReply()
            advanceUntilIdle()
        }

        assertEquals("Sama viesti lähti kahdesti", 1, lahettaja.sent.size)
    }

    @Test
    fun `tunnistamaton vastaus ei ole onnistuminen eika havita tekstia`() {
        // Palvelin vastasi 200:lla jotain muuta kuin kuittausta. Tässä ei tiedetä menikö
        // viesti perille, joten sitä ei arkistoida eikä luonnosta tyhjennetä.
        val lahettaja = RecordingSender { DgResponse.Ok("<html><body>jotain muuta</body></html>") }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.draftChanged("Moi")
            malli.sendReply()
            advanceUntilIdle()
        }

        assertEquals(ReplyUiState.Unconfirmed, malli.reply.value)
        assertEquals("Moi", malli.draft.value)
        assertEquals("Vain saapunut viesti on kannassa", 1, dao.rows.value.size)
    }

    @Test
    fun `katkennut istunto jattaa tekstin kenttaan`() {
        val lahettaja = RecordingSender { DgResponse.AuthFailed }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.draftChanged("Moi")
            malli.sendReply()
            advanceUntilIdle()
        }

        assertEquals(ReplyUiState.SessionExpired, malli.reply.value)
        assertEquals("Kirjoitettu teksti on ainoa kopio", "Moi", malli.draft.value)
    }

    @Test
    fun `luonnos sailyy kunnes jonosta otetaan seuraava kohde`() {
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.draftChanged("Kesken jaanyt")
            advanceUntilIdle()
        }

        assertEquals("Kesken jaanyt", malli.draft.value)

        runTest(dispatcher) {
            malli.fetchNext()
            advanceUntilIdle()
        }

        assertEquals("Uusi kohde korvaa vanhan luonnoksen", "", malli.draft.value)
    }

    // --- Arkistosta vastaaminen klikkaamalla (30.8.2026) ---

    @Test
    fun `arkiston viestiin vastataan talletetulla lomakkeella`() {
        // Jonoa ei haeta lainkaan: viesti on arkistossa entuudestaan, ja sen lomake on
        // talletettu saapumishetkellä. Juuri tämä oli 22.8.2026 rajattu pois ja purettiin
        // Tommin pyynnöstä 30.8.2026.
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openReply(ARKISTON_VIESTI)
            malli.draftChanged("Thanks again")
            malli.sendReply()
            advanceUntilIdle()
        }

        val lahetetty = lahettaja.sent.single()
        assertEquals("Osoite on talletettu, ei koottu", "/bg/sendmsg/91004", lahetetty.action)
        assertEquals(mapOf("text" to "Thanks again"), lahetetty.fields)
        // Oma vastaus arkistoituu ja kantaa yhteyden klikattuun viestiin.
        val oma = dao.rows.value.last()
        assertEquals(ARKISTON_VIESTI.id, oma.replyTo)
        assertEquals(OMA_NIMI, oma.sender)
    }

    @Test
    fun `lomakkeeton viesti ei avaa vastauskenttaa`() {
        // Vanha rivi (ennen 30.8.2026) tai ilmoitus: talletettua osoitetta ei ole, eikä
        // sellaista koota. Klikkaus ei tee mitään, joten lähetyskään ei voi lähteä.
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openReply(ARKISTON_VIESTI.copy(replyForm = null))
            malli.draftChanged("Moi")
            malli.sendReply()
            advanceUntilIdle()
        }

        assertEquals(null, malli.replyTarget.value)
        assertTrue("Lähetys lähti ilman lomaketta", lahettaja.sent.isEmpty())
    }

    @Test
    fun `toinen klikkaus sulkee kentan eika tyhjenna luonnosta`() {
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openReply(ARKISTON_VIESTI)
            malli.draftChanged("Kesken")
            malli.openReply(ARKISTON_VIESTI)
            advanceUntilIdle()
        }

        assertEquals(null, malli.replyTarget.value)
        // Kirjoitettu teksti on ainoa kopio, eikä kentän sulkeminen hävitä sitä.
        assertEquals("Kesken", malli.draft.value)
    }

    // --- Lainaus vastauksen mukana (30.8.2026) ---

    @Test
    fun `valittu lainaus liitetaan rivi rivilta etuliitettyna oman tekstin eteen`() {
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openReply(ARKISTON_VIESTI.copy(body = "rivi yksi\nrivi kaksi"))
            malli.quoteChanged(true)
            malli.draftChanged("oma vastaus")
            malli.sendReply()
            advanceUntilIdle()
        }

        // Muoto on DGText2Arean mitattu muoto: `> `-etuliite joka riville, oma teksti
        // perässä omalla rivillään. Arkistoon menee sama teksti joka lähti.
        val odotettu = "> rivi yksi\n> rivi kaksi\noma vastaus"
        assertEquals(mapOf("text" to odotettu), lahettaja.sent.single().fields)
        assertEquals(odotettu, dao.rows.value.last().body)
    }

    @Test
    fun `ilman rastia lainaus ei lahde`() {
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openReply(ARKISTON_VIESTI)
            malli.draftChanged("oma vastaus")
            malli.sendReply()
            advanceUntilIdle()
        }

        assertEquals(mapOf("text" to "oma vastaus"), lahettaja.sent.single().fields)
    }

    @Test
    fun `pelkka lainaus ilman omaa tekstia ei lahde`() {
        // Portti on kirjoitetussa tekstissä eikä kootussa: rasti päällä ja tyhjä kenttä
        // on yhä tyhjä lähetys, ja tyhjä lähetys on aina vahinko.
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openReply(ARKISTON_VIESTI)
            malli.quoteChanged(true)
            malli.sendReply()
            advanceUntilIdle()
        }

        assertTrue(lahettaja.sent.isEmpty())
    }

    // --- Pelaajasuodatin (30.8.2026) ---

    @Test
    fun `suodatinvalinta talletetaan ja luetaan sailosta`() {
        val sailo = FakeFilterStore()
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, filterStore = sailo)

        malli.filterChanged("vastapelaaja")

        assertEquals("vastapelaaja", malli.filter.value)
        // Uusi malli samasta säilöstä avautuu samaan valintaan: tämä on "muista valinta".
        val toinen = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, filterStore = sailo)
        assertEquals("vastapelaaja", toinen.filter.value)

        toinen.filterChanged(null)
        val kolmas = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, filterStore = sailo)
        assertEquals(null, kolmas.filter.value)
    }

    // --- Fraasinapit: lisäys ja poisto (3.9.2026) ---

    @Test
    fun `fraasi lisataan listan loppuun ja talletetaan`() {
        val sailo = FakePhraseStore()
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, phraseStore = sailo)

        malli.addPhrase("good luck")

        assertEquals(listOf("hi", "gg", "ty gg u2", "good luck"), malli.phrases.value)
        // Uusi malli samasta säilöstä näkee saman listan: nappi säilyy käynnistysten yli.
        val toinen = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, phraseStore = sailo)
        assertEquals(listOf("hi", "gg", "ty gg u2", "good luck"), toinen.phrases.value)
    }

    @Test
    fun `fraasi talletetaan yhtena rivina pienella ilman reunavaleja`() {
        // Kenttä on monirivinen, nappi ei. Rivinvaihto ja reunavälit eivät saa päätyä nappiin,
        // koska säilö erottaa fraasit rivinvaihdolla ja nappi olisi muuten eri leveä kuin tekstinsä.
        // Pienennys on Tommin päätös 3.9.2026: näppäimistö kirjoitti `Gl`, fraasit ovat pienellä.
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })

        malli.addPhrase("  Well\nPlayed  ")
        malli.draftChanged("GL")
        malli.addPhrase(malli.draft.value)

        assertEquals(listOf("well played", "gl"), malli.phrases.value.takeLast(2))
        // Luonnos jää siihen muotoon jossa se kirjoitettiin: pienennys koskee vain nappia.
        assertEquals("GL", malli.draft.value)
    }

    @Test
    fun `tyhja ja jo tallennettu fraasi eivat muuta listaa`() {
        val sailo = FakePhraseStore()
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, phraseStore = sailo)

        malli.addPhrase("   \n ")
        malli.addPhrase("gg")
        malli.addPhrase(" gg ")

        assertEquals(listOf("hi", "gg", "ty gg u2"), malli.phrases.value)
        assertEquals("Säilöön kirjoitettiin vaikka mikään ei muuttunut", 0, sailo.saves)
    }

    @Test
    fun `poisto vie fraasin listasta ja sailosta, ja tyhja lista pysyy tyhjana`() {
        val sailo = FakePhraseStore()
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, phraseStore = sailo)

        malli.removePhrase("gg")
        assertEquals(listOf("hi", "ty gg u2"), malli.phrases.value)

        malli.removePhrase("hi")
        malli.removePhrase("ty gg u2")
        assertEquals(emptyList<String>(), malli.phrases.value)

        // Viimeisen poisto ei ole paluu oletukseen: säilössä on tyhjä lista, ja seuraava
        // malli avautuu tyhjänä.
        assertEquals(emptyList<String>(), sailo.stored)
        val toinen = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, phraseStore = sailo)
        assertEquals(emptyList<String>(), toinen.phrases.value)
    }

    @Test
    fun `fraasin lisays ei koske luonnokseen eika laheta mitaan`() {
        // Tallennus lukee kentän muttei tyhjennä sitä: teksti on yhä luonnos ja lähtee vain
        // Send-napista. Sama ehto kuin fraasinapilla 24.8.2026: fraasi ei koskaan lähetä.
        val lahettaja = RecordingSender { DgResponse.Ok(KUITTAUS) }
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) }, sender = lahettaja)

        malli.draftChanged("good luck")
        malli.addPhrase(malli.draft.value)

        assertEquals("good luck", malli.draft.value)
        assertTrue(lahettaja.sent.isEmpty())
    }

    // --- Varmuuskopio lukee arkistosta eikä ruudun virrasta ---

    @Test
    fun `vienti sisaltaa arkiston vaikka ruudun virta on viela tyhja`() {
        // Mitattu vika 5.9.2026. Vuorokautinen kopio laukeaa ruudun avautuessa, eli juuri
        // silloin kun `messages` on vielä alkuarvossaan. Tiedostoon kirjoitettiin
        // `"messages": []` ja aika leimattiin tehdyksi, joten kopio esti itsensä
        // vuorokaudeksi ja ruutu sanoi arkiston olevan tallessa.
        val malli = malli(RecordingFetcher { DgResponse.Ok(PIKAVIESTI) })
        dao.rows.value = listOf(ARKISTON_VIESTI)

        runTest(dispatcher) {
            // Kukaan ei kerää virtaa, eli sama tilanne kuin ruudun ensimmäisellä
            // kokoonpanolla. Ilman tätä ehtoa testi ei mittaisi sitä mitä väittää.
            assertTrue("Virta ei ollut tyhjä, testi ei mittaa vikaa", malli.messages.value.isEmpty())

            val json = malli.exportJson(HETKI)

            assertTrue("Vienti kirjoitti tyhjän arkiston", json.contains("Great match!"))
        }
    }

    // --- Vastaanotettu kutsu jonon kärjessä (mitattu 14.9.2026, Tommin päätös: vaihtoehto 1) ---

    @Test
    fun `kutsu jonossa ei tallenna mitaan ja jaa jonon karkeen`() {
        // 14.9.2026: viisi painallusta, viisi riviä "Unrecognised message". Kutsu ei kulu,
        // joten se ei ole viesti eikä sitä kirjoiteta kantaan; polku jää ennalleen, koska
        // sivulla ei ole Next-linkkiä eikä jono edennyt.
        val fetcher = RecordingFetcher { DgResponse.Ok(KUTSU) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
        }

        val tila = malli.queue.value
        assertTrue("Tila ei ollut Invitation: $tila", tila is QueueUiState.Invitation)
        val kutsu = (tila as QueueUiState.Invitation).invitation
        assertEquals("ackammon", kutsu.from.name)
        assertEquals("a private 5 point match of backgammon with no time control.", kutsu.description)
        assertEquals(0, dao.rows.value.size)
        assertEquals(JONO, malli.queuePath.value)
    }

    @Test
    fun `hyvaksynta lahtee sivun omalla lomakkeella ja tyhjentaa jonon tilan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(KUTSU) }
        val sender = RecordingSender { DgResponse.Ok(TOP_PAGE) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.acceptInvitation()
            malli.acceptInvitation()
            advanceUntilIdle()
        }

        // Yksi lähetys kaksoisnapautuksesta huolimatta, GET action=accept sivun osoitteeseen.
        assertEquals(1, sender.sent.size)
        val lahetys = sender.sent.single()
        assertEquals("/bg/invite/490747", lahetys.action)
        assertEquals(FormMethod.GET, lahetys.method)
        assertEquals(mapOf("action" to "accept"), lahetys.fields)
        // Top Page vastauksena: lause on null, tila Answered, jono takaisin Idleen.
        assertEquals(
            InvitationActionUiState.Answered(InvitationAction.ACCEPT, null),
            malli.invitationAction.value,
        )
        assertEquals(QueueUiState.Idle, malli.queue.value)
        assertEquals(0, dao.rows.value.size)
    }

    @Test
    fun `hylkays kantaa syyn ja ilman kutsua ei laheteta mitaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(KUTSU) }
        val sender = RecordingSender { DgResponse.Ok("<html><head><title>X</title></head><body>Invitation declined.</body></html>") }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            // Ennen hakua ei ole kutsua, joten mitään ei lähde.
            malli.declineInvitation("no thanks")
            advanceUntilIdle()
            assertEquals(0, sender.sent.size)

            malli.fetchNext()
            advanceUntilIdle()
            malli.declineInvitation("no thanks")
            advanceUntilIdle()
        }

        val lahetys = sender.sent.single()
        assertEquals(FormMethod.POST, lahetys.method)
        assertEquals(mapOf("action" to "decline", "reason" to "no thanks"), lahetys.fields)
        assertEquals(
            InvitationActionUiState.Answered(InvitationAction.DECLINE, "Invitation declined."),
            malli.invitationAction.value,
        )
    }

    @Test
    fun `verkkovirhe vastauksessa jattaa kutsun ruudulle`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(KUTSU) }
        val sender = RecordingSender { DgResponse.Offline(TEST_OFFLINE_CAUSE) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.fetchNext()
            advanceUntilIdle()
            malli.acceptInvitation()
            advanceUntilIdle()
        }

        assertTrue(malli.queue.value is QueueUiState.Invitation)
        assertTrue(malli.invitationAction.value is InvitationActionUiState.Failed)
    }

    private companion object {
        const val JONO = "/bg/nextgame"
        const val SEURAAVA = "/bg/nextgame?seen=2"
        const val HETKI = 1_754_700_000_000L
        const val OMA_NIMI = "tommih"

        /**
         * Arkistossa entuudestaan oleva viesti lomakkeineen, eli rivi sellaisena kuin se on
         * 30.8.2026 jälkeen talletettu. Lomake on sivulta luettu saapumishetkellä.
         */
        val ARKISTON_VIESTI = Message(
            matchId = null,
            opponent = "vastapelaaja",
            sender = "vastapelaaja",
            timestampText = "",
            body = "Great match!",
            rawHeader = "You have received the following quick message from vastapelaaja",
            source = MessageSource.QUICK_MESSAGE,
            receivedAtEpochMillis = HETKI - 100_000,
            replyForm = ReplyForm(
                action = "/bg/sendmsg/91004",
                method = FormMethod.POST,
                field = "text",
                maxLength = 80,
            ),
        )

        /**
         * Lähetyksen kuittaussivu, rakenne aidosta (proxyn tavut 22.8.2026). Otsikko on se
         * josta onnistuminen tunnistetaan, ja runko se joka menee arkistoon otsikkoriviksi.
         */
        val KUITTAUS = """
            <HTML><HEAD><TITLE>Message Sent</TITLE></HEAD>
            <BODY>Your message has been sent to <a href=/bg/user/91004>vastapelaaja</a>
            <p><a href=/bg/nextgame>Next &gt;&gt;</a>
            </BODY></HTML>
        """.trimIndent()

        /** Vastaanotettu kutsu, rakenne aidosta (`sessio-14-9-kutsu/0004`, 14.9.2026). */
        val KUTSU = """
            <HTML><HEAD><TITLE>DailyGammon Invitation</TITLE></HEAD>
            <BODY BGCOLOR=#FFFFFF>
            <H3><A HREF=/bg/user/43348>ackammon</A> invites you to play</h3>
            a <i>private</i> 5 point match of backgammon with no time control
            .
            <FORM METHOD=GET ACTION=/bg/invite/490747><INPUT type=HIDDEN name=action value=accept>
            <INPUT type=submit value="Accept Invitation"></FORM>
            <HR><FORM METHOD=POST ACTION=/bg/invite/490747><INPUT type=hidden name=action value=decline>
            Comment:<INPUT type=text size=80 maxlength=80 name=reason><BR>
            <INPUT type=submit value="Decline Invitation"></FORM>
            </BODY></HTML>
        """.trimIndent()

        val TOP_PAGE = """
            <html><head><title>DailyGammon Top Page</title></head><body>
            <h3>Signed in as <a href=/bg/user/1>tommih</a></h3><a href="/bg/user/1?days_to_view=7">Finished</a>
            </body></html>
        """.trimIndent()

        val PIKAVIESTI = """
            <html><head><title>DailyGammon Quick Message</title></head><body>
            <h3>You have received the following quick message from <a href="/bg/user/91004">vastapelaaja</a></h3>
            <pre>Great match!
            <hr><form action="/bg/sendmsg/91004" method="post"><input type="text" name="text" maxlength="80"><input type="submit" value="Send Reply"></form><hr>
            </pre><p><a href="$SEURAAVA">Next &gt;&gt;</a>
            </body></html>
        """.trimIndent()

        val TIEDOTE = """
            <html><head><title>DailyGammon</title></head><body>
            <h3>You have received the following telegram message:</h3>
            <pre>Tournament Nine Lives #2222 has a winner.
            </pre><p><a href="$SEURAAVA">Next &gt;&gt;</a>
            </body></html>
        """.trimIndent()

        val TUNTEMATON = """
            <html><body><h3>You have received something entirely new</h3>
            <pre>Sisältö tallessa</pre></body></html>
        """.trimIndent()

        /** Riittävän lauta ollakseen lauta: 24 pistekuvaa, ja oma h3-otsikko kuten aidolla. */
        fun lauta(): String = buildString {
            append("<HTML><HEAD><TITLE>Match 7000002, Move 579</TITLE></HEAD><BODY>")
            append("<H3><A HREF=/bg/event/900002>Nine Lives #2222</A>, Round 4</H3><TABLE><TR>")
            repeat(24) { append("""<TD><IMG SRC=/images/2/pt_y1_up.gif ALT="y1"></TD>""") }
            append("</TR></TABLE></BODY></HTML>")
        }
    }
}

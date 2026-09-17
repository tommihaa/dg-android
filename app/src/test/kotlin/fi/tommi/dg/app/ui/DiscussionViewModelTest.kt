package fi.tommi.dg.app.ui

import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.FormSubmission
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Palstan näkymämalli. ~~Konstruktorissa ei ole `FormSender`iä, joten vahvin väite (mitään
 * ei lähetetä) on jo tyypissä eikä sitä voi testata rikki.~~ Malli saa lähettäjän 3.9.2026
 * alkaen (kirjoituslomakkeet), joten lukutestit väittävät nyt myös ettei lähettäjää kutsuta:
 * [RecordingSender] kirjaa jokaisen lähetyksen. Muut testit väittävät haarautumisesta ja
 * siitä että polut ovat sivun omia.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class RecordingFetcher(var answer: (String) -> DgResponse) : PageFetcher {
        val requested = mutableListOf<String>()
        override fun fetch(path: String): DgResponse {
            requested += path
            return answer(path)
        }
    }

    private class RecordingSender(var answer: (FormSubmission) -> DgResponse) : FormSender {
        val sent = mutableListOf<FormSubmission>()
        override fun send(submission: FormSubmission): DgResponse {
            sent += submission
            return answer(submission)
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

    private val upstream = MutableStateFlow<String?>(POLKU)

    private fun malli(
        fetcher: RecordingFetcher,
        path: String? = POLKU,
        sender: RecordingSender = RecordingSender { DgResponse.Ok(KETJU) },
    ) = DiscussionViewModel(
        pages = fetcher,
        forms = sender,
        forumPathUpstream = upstream.also { it.value = path },
        io = dispatcher,
    )

    @Test
    fun `polun saapuminen laukaisee haun ja indeksi jasentyy`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(INDEKSI) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(listOf(POLKU), fetcher.requested)
        val state = malli.state.value as DiscussionUiState.Loaded
        assertEquals(1, state.index.threads.size)
    }

    @Test
    fun `ilman polkua ei haeta mitaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(INDEKSI) }
        val malli = malli(fetcher, path = null)

        runTest(dispatcher) { advanceUntilIdle() }

        assertTrue(fetcher.requested.isEmpty())
        assertEquals(DiscussionUiState.NoPath, malli.state.value)
    }

    @Test
    fun `ketju avataan listan rivin omalla polulla`() {
        // Polku fragmentteineen sellaisenaan: rivin readPath on ainoa lähde.
        val fetcher = RecordingFetcher { path ->
            if (path == POLKU) DgResponse.Ok(INDEKSI) else DgResponse.Ok(KETJU)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            val thread = (malli.state.value as DiscussionUiState.Loaded).index.threads.single()
            malli.openThread(thread)
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU, "/bg/forum2/main/read/60001#3"), fetcher.requested)
        val open = malli.thread.value as ThreadUiState.Loaded
        assertEquals(1, open.page.posts.size)
    }

    @Test
    fun `sulkeminen palauttaa listalle eika hae mitaan`() {
        val fetcher = RecordingFetcher { path ->
            if (path == POLKU) DgResponse.Ok(INDEKSI) else DgResponse.Ok(KETJU)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openThread((malli.state.value as DiscussionUiState.Loaded).index.threads.single())
            advanceUntilIdle()
            malli.closeThread()
        }

        assertNull(malli.thread.value)
        assertEquals(2, fetcher.requested.size)
    }

    @Test
    fun `palstarivi luetaan sivulta ja valinta on nakyvilla oleva palsta`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(INDEKSI) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(listOf("General", "Politics"), malli.boards.value.map { it.name })
        assertEquals("General", malli.selectedBoard.value)
    }

    @Test
    fun `palstan vaihto hakee sivun oman polun`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path == POLITIIKAN_POLKU) POLITIIKKA else INDEKSI)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.selectBoard(malli.boards.value.first { it.name == "Politics" })
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU, POLITIIKAN_POLKU), fetcher.requested)
        assertEquals("Politics", malli.selectedBoard.value)
        // Palstarivi tulee uudelta sivulta, jolla General on linkki eikä nykyinen.
        assertEquals("/bg/forum2/main", malli.boards.value.first { it.name == "General" }.path)
        val state = malli.state.value as DiscussionUiState.Loaded
        assertEquals("Toinen ketju", state.index.threads.single().title)
    }

    @Test
    fun `nakyvilla olevan palstan napautus ei hae mitaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(INDEKSI) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.selectBoard(malli.boards.value.first { it.name == "General" })
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU), fetcher.requested)
    }

    @Test
    fun `ylajuoksun uusi polku ei vaihda valittua palstaa takaisin`() {
        // Top Pagen uusi lataus tuo aina General-polun, koska yläjuoksu tuntee vain sen.
        // Ilman käyttäjän valinnan etusijaa Refresh hyppäisi hiljaa toiselle palstalle.
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path == POLITIIKAN_POLKU) POLITIIKKA else INDEKSI)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.selectBoard(malli.boards.value.first { it.name == "Politics" })
            advanceUntilIdle()
            // Kaksi arvoa, koska StateFlow ei toista samaa: null ja takaisin, jotta
            // yläjuoksu todella emittoi eikä testi mene läpi pelkällä hiljaisuudella.
            upstream.value = null
            advanceUntilIdle()
            upstream.value = POLKU
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals(
            listOf(POLKU, POLITIIKAN_POLKU, POLITIIKAN_POLKU),
            fetcher.requested,
        )
        assertEquals("Politics", malli.selectedBoard.value)
    }

    @Test
    fun `vaara sivu ei jasenny tyhjaksi indeksiksi`() {
        val fetcher = RecordingFetcher { DgResponse.Ok("<html><body>jotain muuta</body></html>") }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(DiscussionUiState.NotAForumPage, malli.state.value)
    }

    @Test
    fun `lukeminen ei laheta mitaan`() {
        val sender = RecordingSender { DgResponse.Ok(KETJU) }
        val fetcher = RecordingFetcher { path ->
            if (path == POLKU) DgResponse.Ok(INDEKSI) else DgResponse.Ok(KETJU)
        }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openThread((malli.state.value as DiscussionUiState.Loaded).index.threads.single())
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun `kommentti lahetetaan sivun omalla lomakkeella ja todistetaan ketjusta`() {
        // Vastaus on tuntematon sivu, joten ketju haetaan uudelleen ja teksti löytyy.
        val fetcher = RecordingFetcher { path ->
            when (path) {
                POLKU -> DgResponse.Ok(INDEKSI)
                "/bg/forum2/main/add/60001" -> DgResponse.Ok(KOMMENTTILOMAKE)
                "/bg/forum2/main/read/60001#3" -> DgResponse.Ok(KETJU_VASTATTU)
                else -> DgResponse.Ok("<html><body>unknown</body></html>")
            }
        }
        val sender = RecordingSender { DgResponse.Ok("<html><body>ok</body></html>") }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openThread((malli.state.value as DiscussionUiState.Loaded).index.threads.single())
            advanceUntilIdle()
            malli.openComment()
            advanceUntilIdle()
            malli.setDraftComment("Vastaukseni tähän\ntoinen rivi")
            malli.post()
            advanceUntilIdle()
        }

        val sent = sender.sent.single()
        assertEquals("/bg/forum2/main/submitadd/60001", sent.action)
        assertEquals(
            mapOf("comment" to "Vastaukseni tähän\ntoinen rivi", "submit" to "Submit Comment"),
            sent.fields,
        )
        assertEquals(PostUiState.Done(ComposeKind.COMMENT), malli.post.value)
        assertNull(malli.compose.value)
        assertEquals("", malli.draftComment.value)
        assertEquals(2, (malli.thread.value as ThreadUiState.Loaded).page.posts.size)
    }

    @Test
    fun `vahvistamaton kommentti jattaa lomakkeen ja luonnoksen`() {
        val fetcher = RecordingFetcher { path ->
            when (path) {
                POLKU -> DgResponse.Ok(INDEKSI)
                "/bg/forum2/main/add/60001" -> DgResponse.Ok(KOMMENTTILOMAKE)
                else -> DgResponse.Ok(KETJU)
            }
        }
        val sender = RecordingSender { DgResponse.Ok(KETJU) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openThread((malli.state.value as DiscussionUiState.Loaded).index.threads.single())
            advanceUntilIdle()
            malli.openComment()
            advanceUntilIdle()
            malli.setDraftComment("Teksti jota ei näy")
            malli.post()
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(PostUiState.Unconfirmed(ComposeKind.COMMENT), malli.post.value)
        assertTrue(malli.compose.value is ComposeUiState.Loaded)
        assertEquals("Teksti jota ei näy", malli.draftComment.value)
    }

    @Test
    fun `uusi ketju lahetetaan otsikoineen ja todistetaan indeksista`() {
        val fetcher = RecordingFetcher { path ->
            when (path) {
                "/bg/forum2/main/new" -> DgResponse.Ok(UUSI_LOMAKE)
                else -> DgResponse.Ok(INDEKSI)
            }
        }
        // Vastaus on suoraan indeksi jossa uusi otsikko on rivillä.
        val sender = RecordingSender { DgResponse.Ok(INDEKSI_UUDELLA) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openNewThread()
            advanceUntilIdle()
            malli.setDraftTitle(" Uusi otsikko ")
            malli.setDraftComment("Avaus")
            malli.post()
            advanceUntilIdle()
        }

        val sent = sender.sent.single()
        assertEquals("/bg/forum2/main/submitnew", sent.action)
        assertEquals(
            mapOf("title" to "Uusi otsikko", "comment" to "Avaus", "submit" to "Create New Thread"),
            sent.fields,
        )
        assertEquals(PostUiState.Done(ComposeKind.NEW_THREAD), malli.post.value)
        assertEquals(2, (malli.state.value as DiscussionUiState.Loaded).index.threads.size)
        assertEquals("", malli.draftTitle.value)
    }

    @Test
    fun `luonnos on lomakekohtainen eika vuoda kommentista uuteen ketjuun`() {
        val fetcher = RecordingFetcher { path ->
            when (path) {
                POLKU -> DgResponse.Ok(INDEKSI)
                "/bg/forum2/main/new" -> DgResponse.Ok(UUSI_LOMAKE)
                "/bg/forum2/main/add/60001" -> DgResponse.Ok(KOMMENTTILOMAKE)
                else -> DgResponse.Ok(KETJU)
            }
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openNewThread()
            advanceUntilIdle()
            malli.setDraftTitle("Otsikko")
            malli.setDraftComment("Uuden ketjun runko")
            malli.closeCompose()
            malli.openThread((malli.state.value as DiscussionUiState.Loaded).index.threads.single())
            advanceUntilIdle()
            malli.openComment()
            advanceUntilIdle()
        }
        // Kommenttikenttä on tyhjä, eikä uuden ketjun teksti ole siinä.
        assertEquals("", malli.draftComment.value)
        assertEquals("", malli.draftTitle.value)

        runTest(dispatcher) {
            malli.setDraftComment("Kommentti")
            malli.closeCompose()
            malli.closeThread()
            malli.openNewThread()
            advanceUntilIdle()
        }
        // Uuden ketjun luonnos on tallessa sellaisenaan.
        assertEquals("Otsikko", malli.draftTitle.value)
        assertEquals("Uuden ketjun runko", malli.draftComment.value)
    }

    @Test
    fun `tyhja luonnos ei laheta mitaan`() {
        val fetcher = RecordingFetcher { path ->
            if (path == "/bg/forum2/main/new") DgResponse.Ok(UUSI_LOMAKE) else DgResponse.Ok(INDEKSI)
        }
        val sender = RecordingSender { DgResponse.Ok(INDEKSI) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openNewThread()
            advanceUntilIdle()
            malli.setDraftComment("Runko ilman otsikkoa")
            malli.post()
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
        assertEquals(PostUiState.Idle, malli.post.value)
    }

    private companion object {
        const val POLKU = "/bg/forum2"

        const val POLITIIKAN_POLKU = "/bg/forum2/politics"

        val INDEKSI = """
            <html><body>
            <h1>DailyGammon Forum: General</h1>
            <p><b>General</b>&nbsp;|&nbsp;<a href=/bg/forum2/politics>Politics</a><br>
            <table><tr><th>Title</th><th>#</th><th>Poster</th></tr>
            <tr><td><a href="/bg/forum2/main/read/60001#3">Ketju</a></td>
            <td align=center>3</td><td><i>joku</i></td></tr>
            </table>
            <p><a href="/bg/forum2/main/new">Add a New Thread</a>
            </body></html>
        """.trimIndent()

        val INDEKSI_UUDELLA = """
            <html><body>
            <h1>DailyGammon Forum: General</h1>
            <p><b>General</b>&nbsp;|&nbsp;<a href=/bg/forum2/politics>Politics</a><br>
            <table><tr><th>Title</th><th>#</th><th>Poster</th></tr>
            <tr><td><a href="/bg/forum2/main/read/60002">Uusi otsikko</a></td>
            <td align=center>1</td><td><i>minä</i></td></tr>
            <tr><td><a href="/bg/forum2/main/read/60001#3">Ketju</a></td>
            <td align=center>3</td><td><i>joku</i></td></tr>
            </table>
            <p><a href="/bg/forum2/main/new">Add a New Thread</a>
            </body></html>
        """.trimIndent()

        val UUSI_LOMAKE = """
            <html><body>
            <h1>DailyGammon Forum: General</h1>
            <h2>New Thread</h2>
            <form action=/bg/forum2/main/submitnew method=post>
            Title: <input type=text name=title size=80 maxlength=80>
            <textarea name=comment rows=10 cols=80></textarea>
            <input type=submit name=submit value="Preview">
            <input type=submit name=submit value="Create New Thread">
            </form>
            </body></html>
        """.trimIndent()

        val KOMMENTTILOMAKE = """
            <html><body>
            <h1>DailyGammon Forum: General</h1>
            <h2>Ketju</h2>
            <form action=/bg/forum2/main/submitadd/60001 method=post>
            <textarea name=comment rows=10 cols=80></textarea>
            <input type=submit name=submit value="Preview">
            <input type=submit name=submit value="Submit Comment">
            </form>
            </body></html>
        """.trimIndent()

        val KETJU_VASTATTU = """
            <html><body>
            <h2>Ketju</h2>
            <a name=1>Runko.
            <p><b>Posted by <a href=/bg/user/10001>joku</a> at Mon Aug 3 21:29:44 2026</b><hr>
            <a name=2>Vastaukseni tähän
            <p>toinen rivi
            <p><b>Posted by <a href=/bg/user/10002>minä</a> at Thu Sep 3 18:00:00 2026</b><hr>
            <a href="/bg/forum2/main/add/60001">Add a Comment</a>
            </body></html>
        """.trimIndent()

        val POLITIIKKA = """
            <html><body>
            <h1>DailyGammon Forum: Politics</h1>
            <p><a href=/bg/forum2/main>General</a>&nbsp;|&nbsp;<b>Politics</b><br>
            <table><tr><th>Title</th><th>#</th><th>Poster</th></tr>
            <tr><td><a href="/bg/forum2/politics/read/61001">Toinen ketju</a></td>
            <td align=center>5</td><td><i>toinen</i></td></tr>
            </table>
            </body></html>
        """.trimIndent()

        val KETJU = """
            <html><body>
            <h2>Ketju</h2>
            <a name=1>Runko.
            <p><b>Posted by <a href=/bg/user/10001>joku</a> at Mon Aug 3 21:29:44 2026</b><hr>
            <a href="/bg/forum2/main/add/60001">Add a Comment</a>
            </body></html>
        """.trimIndent()
    }
}

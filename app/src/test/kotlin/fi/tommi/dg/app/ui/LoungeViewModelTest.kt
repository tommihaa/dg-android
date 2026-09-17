package fi.tommi.dg.app.ui

import fi.tommi.dg.app.FormSender
import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.FormMethod
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Loungen näkymämalli, ja ennen kaikkea Joinin ehdot: lähetys on sivun oma href
 * sellaisenaan, yksi kerrallaan, eikä mitään lähetetä uudelleen. HTML on synteettistä
 * samasta syystä kuin [MessagesViewModelTest]issä: jäsennysväitteet asuvat core-scrapessa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoungeViewModelTest {

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
        sender: RecordingSender = RecordingSender { DgResponse.Ok(LOUNGE) },
    ) = LoungeViewModel(
        pages = fetcher,
        forms = sender,
        loungePathUpstream = upstream.also { it.value = path },
        io = dispatcher,
    )

    @Test
    fun `polun saapuminen laukaisee haun ja sivu jasentyy`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(listOf(POLKU), fetcher.requested)
        val state = malli.state.value as LoungeUiState.Loaded
        assertEquals(1, state.page.invitations.size)
    }

    @Test
    fun `ilman polkua ei haeta mitaan`() {
        // Osoitetta ei koota: ennen otteluluettelon latautumista ruutu sanoo syyn.
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val malli = malli(fetcher, path = null)

        runTest(dispatcher) { advanceUntilIdle() }

        assertTrue(fetcher.requested.isEmpty())
        assertEquals(LoungeUiState.NoPath, malli.state.value)
    }

    @Test
    fun `vaara sivu ei jasenny tyhjaksi loungeksi`() {
        val fetcher = RecordingFetcher { DgResponse.Ok("<html><body>jotain muuta</body></html>") }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        assertEquals(LoungeUiState.NotALoungePage, malli.state.value)
    }

    @Test
    fun `join lahettaa tasmalleen sivun oman hrefin`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok(LOUNGE) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            val invitation = (malli.state.value as LoungeUiState.Loaded).page.invitations.single()
            malli.join(invitation)
            advanceUntilIdle()
        }

        assertEquals(JOIN_HREF, sender.sent.single().action)
        assertTrue(sender.sent.single().fields.isEmpty())
        assertEquals(JoinUiState.Joined, malli.join.value)
    }

    @Test
    fun `tuntematon vastaus on unconfirmed ja lounge luetaan kerran uudelleen`() {
        // Accept-vastauksen muotoa ei ole mitattu, joten tuntematonta ei tulkita
        // kumpaankaan suuntaan eikä lähetetä uudelleen: yksi lähetys, yksi uudelleenluku.
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok("<html><body>outo sivu</body></html>") }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.join((malli.state.value as LoungeUiState.Loaded).page.invitations.single())
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(listOf(POLKU, POLKU), fetcher.requested)
        assertEquals(JoinUiState.Unconfirmed, malli.join.value)
    }

    @Test
    fun `kuittaussivu luetaan onnistumiseksi ja lounge haetaan kerran`() {
        // Sivuston oma vastaus hyväksyntään (mitattu 31.8.2026 raaoista tavuista):
        // yksi lause ja linkki, ei loungea. Se on onnistuminen eikä tuntematon, mutta
        // lista on haettava erikseen, koska kuittaussivu ei kanna tarjouksia.
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok(JOIN_CONFIRM) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.join((malli.state.value as LoungeUiState.Loaded).page.invitations.single())
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(listOf(POLKU, POLKU), fetcher.requested)
        assertEquals(JoinUiState.Joined, malli.join.value)
        assertTrue(malli.state.value is LoungeUiState.Loaded)
    }

    @Test
    fun `katkennut istunto ei lahetya uudelleen`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.AuthFailed }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.join((malli.state.value as LoungeUiState.Loaded).page.invitations.single())
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(LoungeUiState.SessionExpired, malli.state.value)
    }

    @Test
    fun `pelaajalistaa ei haeta ennen kuin se avataan`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path.startsWith(PLIST_POLKU)) PLIST else LOUNGE)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        // Lounge on haettu, pelaajalista ei: se on koko sivuston lista eikä sitä haeta
        // sen varalta että joku avaisi sen.
        assertEquals(listOf(POLKU), fetcher.requested)
        assertEquals(PlayerListUiState.NoPath, malli.players.value)
    }

    @Test
    fun `pelaajalista haetaan loungen omalla linkilla ensimmaisella avauksella`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path.startsWith(PLIST_POLKU)) PLIST else LOUNGE)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openPlayers()
            advanceUntilIdle()
            // Toinen avaus ei hae uudestaan: sivu pysyy kunnes Refresh pyytää.
            
            malli.openPlayers()
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU, PLIST_POLKU), fetcher.requested)
        val state = malli.players.value as PlayerListUiState.Loaded
        assertEquals("ekapelaaja", state.list.players.single().player.name)
    }

    @Test
    fun `sivutuslinkki haetaan sivun omalla hrefilla ja Refresh pysyy siina`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path.startsWith(PLIST_POLKU)) PLIST else LOUNGE)
        }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openPlayers()
            advanceUntilIdle()
            malli.openPlayerListLink(
                (malli.players.value as PlayerListUiState.Loaded).list.links.single()
            )
            advanceUntilIdle()
            malli.refreshPlayers()
            advanceUntilIdle()
        }

        // Refresh hakee sen sivun jolla ollaan, ei takaisin ensimmäiselle sadalle.
        assertEquals(
            listOf(POLKU, PLIST_POLKU, PLIST_SEURAAVA, PLIST_SEURAAVA),
            fetcher.requested,
        )
    }

    // --- Pelaajahaku (3.9.2026) ---

    @Test
    fun `haku lahettaa sivun lomakkeen kenttineen ja tulos luetaan pelaajalistana`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok(PLIST) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openPlayers()
            advanceUntilIdle()
            malli.searchPlayers(" eka ")
            advanceUntilIdle()
        }

        val sent = sender.sent.single()
        assertEquals("/bg/plist", sent.action)
        assertEquals(FormMethod.POST, sent.method)
        // Tekstikenttä trimmattuna ja piilokenttä sellaisenaan; mitään ei keksitä.
        assertEquals(mapOf("like" to "eka", "type" to "name"), sent.fields)
        assertEquals("eka", malli.search.value)
        val state = malli.players.value as PlayerListUiState.Loaded
        assertEquals("ekapelaaja", state.list.players.single().player.name)
    }

    @Test
    fun `tyhja haku ei lahetya mitaan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok(PLIST) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.searchPlayers("   ")
            advanceUntilIdle()
        }

        assertTrue(sender.sent.isEmpty())
        assertEquals(null, malli.search.value)
    }

    @Test
    fun `refresh toistaa haun ja show all palaa sivun omaan listaan`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path.startsWith(PLIST_POLKU)) PLIST else LOUNGE)
        }
        val sender = RecordingSender { DgResponse.Ok(PLIST) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openPlayers()
            advanceUntilIdle()
            malli.searchPlayers("eka")
            advanceUntilIdle()
            malli.refreshPlayers()
            advanceUntilIdle()
            malli.clearSearch()
            advanceUntilIdle()
        }

        // Kaksi lähetystä (haku ja sen Refresh), ja paluu hakee loungen oman listan uudestaan.
        assertEquals(2, sender.sent.size)
        assertEquals(listOf(POLKU, PLIST_POLKU, PLIST_POLKU), fetcher.requested)
        assertEquals(null, malli.search.value)
    }

    @Test
    fun `sivun oma linkki paattaa haun`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path.startsWith(PLIST_POLKU)) PLIST else LOUNGE)
        }
        val sender = RecordingSender { DgResponse.Ok(PLIST) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.searchPlayers("eka")
            advanceUntilIdle()
            malli.openPlayerListLink(
                (malli.players.value as PlayerListUiState.Loaded).list.links.single()
            )
            advanceUntilIdle()
        }

        assertEquals(null, malli.search.value)
        assertEquals(listOf(POLKU, PLIST_SEURAAVA), fetcher.requested)
    }

    @Test
    fun `ilman loungea pelaajalistalla ei ole polkua eika mitaan haeta`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val malli = malli(fetcher, path = null)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openPlayers()
            advanceUntilIdle()
        }

        // Pelaajalistan polku tulee loungen omalta sivulta; ilman sitä ruutu sanoo syyn
        // eikä kokoa osoitetta.
        assertTrue(fetcher.requested.isEmpty())
        assertEquals(PlayerListUiState.NoPath, malli.players.value)
    }

    @Test
    fun `sign up lahettaa sivun oman hrefin ja rivin cancel-linkki vahvistaa`() {
        // Vastausta ei ole kaapattu, joten onnistuminen luetaan loungen riviltä: Sign
        // Upin jälkeen rivillä on Cancel Signup. Tässä vastaus itse on se lounge.
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok(LOUNGE_SIGNED) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            val tournament = (malli.state.value as LoungeUiState.Loaded).page.tournaments.single()
            malli.signUp(tournament)
            advanceUntilIdle()
        }

        assertEquals(SIGNUP_HREF, sender.sent.single().action)
        assertTrue(sender.sent.single().fields.isEmpty())
        assertEquals(listOf(POLKU), fetcher.requested)
        assertEquals(SignupUiState.Done(SignupAction.SignUp, "Turnaus"), malli.signup.value)
        val row = (malli.state.value as LoungeUiState.Loaded).page.tournaments.single()
        assertEquals(CANCEL_HREF, row.cancelPath)
    }

    @Test
    fun `tuntematon vastaus sign upiin luetaan loungesta kerran ja rivi ratkaisee`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok("<html><body>outo sivu</body></html>") }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            val tournament = (malli.state.value as LoungeUiState.Loaded).page.tournaments.single()
            // Uudelleenluku näkee jo ilmoittaudutun rivin.
            fetcher.answer = { DgResponse.Ok(LOUNGE_SIGNED) }
            malli.signUp(tournament)
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(listOf(POLKU, POLKU), fetcher.requested)
        assertEquals(SignupUiState.Done(SignupAction.SignUp, "Turnaus"), malli.signup.value)
    }

    @Test
    fun `rivi ilman muutosta on unconfirmed eika mitaan laheteta uudelleen`() {
        // Vastaus on lounge mutta rivillä on yhä Sign Up: ei tulkita kumpaankaan suuntaan.
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.Ok(LOUNGE) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            val tournament = (malli.state.value as LoungeUiState.Loaded).page.tournaments.single()
            malli.signUp(tournament)
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(SignupUiState.Unconfirmed(SignupAction.SignUp), malli.signup.value)
    }

    @Test
    fun `peruutus lahettaa cancel-hrefin ja sign up -linkki rivilla vahvistaa`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE_SIGNED) }
        val sender = RecordingSender { DgResponse.Ok(LOUNGE) }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            val tournament = (malli.state.value as LoungeUiState.Loaded).page.tournaments.single()
            // Sign Up ei lähde riviltä jolla ei ole Sign Up -linkkiä.
            malli.signUp(tournament)
            advanceUntilIdle()
            assertTrue(sender.sent.isEmpty())
            malli.cancelSignup(tournament)
            advanceUntilIdle()
        }

        assertEquals(CANCEL_HREF, sender.sent.single().action)
        assertEquals(SignupUiState.Done(SignupAction.Cancel, "Turnaus"), malli.signup.value)
        val row = (malli.state.value as LoungeUiState.Loaded).page.tournaments.single()
        assertEquals(SIGNUP_HREF, row.signupPath)
    }

    @Test
    fun `sign up ei lahde katkenneella istunnolla uudelleen`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val sender = RecordingSender { DgResponse.AuthFailed }
        val malli = malli(fetcher, sender = sender)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.signUp((malli.state.value as LoungeUiState.Loaded).page.tournaments.single())
            advanceUntilIdle()
        }

        assertEquals(1, sender.sent.size)
        assertEquals(LoungeUiState.SessionExpired, malli.state.value)
        assertEquals(SignupUiState.Idle, malli.signup.value)
    }

    @Test
    fun `vaaraa sivua ei jasenneta pelaajalistaksi`() {
        // Loungen oma sivu menisi jäsentimen läpi ja tuottaisi rivejä joiden rating on
        // aikaraja, joten tunnistus on se joka estää sen.
        val fetcher = RecordingFetcher { DgResponse.Ok(LOUNGE) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.openPlayers()
            advanceUntilIdle()
        }

        assertEquals(PlayerListUiState.NotAPlayerList, malli.players.value)
    }

    private companion object {
        const val POLKU = "/bg/lounge"
        const val JOIN_HREF = "/bg/lounge?action=accept&id=11&userid=10001"
        const val SIGNUP_HREF = "/bg/lounge?action=signup&event=900001&userid=10001"
        const val CANCEL_HREF = "/bg/lounge?action=cancel&event=900001&userid=10001"
        const val USEREVENT_POLKU = "/bg/userevent/10001"

        /**
         * Synteettinen lounge: tarjouslomake (tunnistus), yksi joinable-rivi ja yksi
         * turnausrivi jonka viimeinen solu on [linkki] (Sign Up tai Cancel Signup).
         */
        fun lounge(linkki: String) = """
            <html><body>
            <h2>Welcome to the DailyGammon waiting lounge, pelaaja.</h2>
            <table><caption>Matches waiting for opponents:</caption>
            <tr><th></th><th></th><th>Type</th><th>Length</th><th>Player</th><th>Timeout</th><th>Comment</th></tr>
            <tr><td></td><td><a href="$JOIN_HREF">Join</a></td><td>backgammon</td>
            <td>1</td><td><a href=/bg/user/10002>vastustaja</a></td><td>Once a Day</td><td></td></tr>
            </table>
            <table><caption>Tournament Sign-Up</caption>
            <tr><th>Tournament Name<th>Variant<th>Length<th>Rounds<th colspan=2>Time<th>Grace
            <tr><td><a href=/bg/event/900001>Turnaus</a><td>backgammon<td>5<td>6<td>200:00<td>+4:00<td>24:00<td>$linkki
            </table>
            <hr>See a <a href=/bg/plist>List of Players </a> and find out how you're doing.
            <hr><form method=post action=/bg/plist>Find a player whose name starts with: <input type=text name=like maxlength=40 size=40><input type=hidden name=type value=name></form>
            <hr>Visit the <a href=/bg/thall>Tournament Hall</a>.
            <form action=/bg/lounge method=post><input type=submit value=Submit></form>
            </body></html>
        """.trimIndent()

        val LOUNGE = lounge("<a href=\"$SIGNUP_HREF\">Sign Up</a>")
        val LOUNGE_SIGNED = lounge("<a href=\"$CANCEL_HREF\">Cancel Signup</a>")

        /** Synteettinen oma turnauslista, sama muoto kuin PageViewModelTestissa. */
        val TURNAUSLISTA = """
            <html><head><title>DailyGammon Events For pelaaja</title></head><body>
            <h2>Active Tournaments for <a href=/bg/user/10001>pelaaja</a></h2>
            <table><tr><th><th>#<th><th align=left>Event<th>Wins
            <tr><td width=30><td>1.<td width=10><td><a href=/bg/event/111128>Turnaus</a>
            <td align=center>2</td></tr>
            </table>
            </body></html>
        """.trimIndent()

        /**
         * Sivuston kuittaus hyvaksynnalle, mitattu 31.8.2026. Sama muoto kuin
         * fixturessa `lounge_join_confirm.html`: yksi lause ja Next-linkki, ei loungea.
         */
        val JOIN_CONFIRM = """
            <html><head><title>DailyGammon</title></head><body>
            <br>You have successfully joined that game.
            <p><a href=/bg/nextgame>Next</a>
            </body></html>
        """.trimIndent()

        const val PLIST_POLKU = "/bg/plist"
        const val PLIST_SEURAAVA = "/bg/plist?type=rate&start=101&length=100"

        /** Synteettinen pelaajalista: otsikko tunnistusta varten, yksi rivi ja yksi linkki. */
        val PLIST = """
            <html><head><title>DailyGammon - Player List</title></head><body>
            <table><tr align=center><th><th>#<th><th align=left>Player<th>Rating<th><th>Experience
            <tr><td width=30><td>1.</td>
            <td width=10><td><a href=/bg/user/21666>ekapelaaja</a></td>
            <td>2326.39<td width=10><td align=right>10765
            </table>
            <a href="$PLIST_SEURAAVA">Next 100</a>
            </body></html>
        """.trimIndent()

    }
}

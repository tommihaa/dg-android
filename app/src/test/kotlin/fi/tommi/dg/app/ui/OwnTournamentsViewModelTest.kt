package fi.tommi.dg.app.ui

import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.data.MatchMemory
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.SeenMatch
import fi.tommi.dg.net.DgResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
 * Pelaajan omien turnausten näkymämalli. Väitteet ovat ne kaksi jotka olivat
 * `LoungeViewModelTest`issä siihen asti kun lista siirtyi otteluluetteloon 4.9.2026:
 * haku on laiska, ja ilman polkua ei kokoilla osoitetta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnTournamentsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class RecordingFetcher(val answer: (String) -> DgResponse) : PageFetcher {
        val requested = mutableListOf<String>()
        override fun fetch(path: String): DgResponse {
            requested += path
            return answer(path)
        }
    }

    /** Muisti listana: testi väittää mitä kirjoitettiin, ei miten se tallentui. */
    private class RecordingMemory : MatchMemory {
        val seen = mutableListOf<SeenMatch>()
        override fun observeAll(): Flow<Map<MatchId, SeenMatch>> = flowOf(seen.associateBy { it.matchId })
        override suspend fun remember(seen: SeenMatch) { this.seen += seen }
        override suspend fun count(): Int = seen.size
    }

    @Before
    fun asetaDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun palautaDispatcher() {
        Dispatchers.resetMain()
    }

    private fun malli(
        fetcher: RecordingFetcher,
        path: String? = POLKU,
        sink: MutableStateFlow<OwnTournamentCount?>? = null,
        profilePath: String? = null,
        memory: MatchMemory? = null,
    ) = OwnTournamentsViewModel(
        pages = fetcher,
        pathUpstream = MutableStateFlow(path),
        profilePathUpstream = MutableStateFlow(profilePath),
        matchMemory = memory,
        countSink = sink,
        io = dispatcher,
    )

    @Test
    fun `avaus hakee yhden sivun, refresh myos profiilin ja kirjoittaa sen ottelut muistiin`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path == PROFIILIPOLKU) PROFIILI else TURNAUSLISTA)
        }
        // Muistissa on jo jotain, joten tyhjän muistin kertatäyttö ei laukea.
        val muisti = RecordingMemory().apply { seen += NAHTY }
        val malli = malli(fetcher, profilePath = PROFIILIPOLKU, memory = muisti)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onOpened()
            advanceUntilIdle()
            // Avaus on laiska haku eikä painallus: profiilia ei haeta sen varalta.
            assertEquals(listOf(POLKU), fetcher.requested)
            assertEquals(listOf(NAHTY), muisti.seen)

            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU, POLKU, PROFIILIPOLKU), fetcher.requested)
        assertEquals(OpponentsRefreshState.Idle, malli.opponents.value)
        val rivi = muisti.seen.last()
        assertEquals(MatchId("5305249"), rivi.matchId)
        assertEquals("RickBlaine", rivi.opponent.name)
        assertEquals("4/5", rivi.round)
        assertEquals(21, rivi.matchLength)
    }

    @Test
    fun `tyhja muisti taytetaan profiilista kerran, ei toisella avauksella`() {
        val fetcher = RecordingFetcher { path ->
            DgResponse.Ok(if (path == PROFIILIPOLKU) PROFIILI else TURNAUSLISTA)
        }
        val muisti = RecordingMemory()
        val malli = malli(fetcher, profilePath = PROFIILIPOLKU, memory = muisti)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onOpened()
            advanceUntilIdle()
            malli.onOpened()
            advanceUntilIdle()
        }

        // Turnaussivu kerran ja profiili kerran: toinen avaus ei hae kumpaakaan.
        assertEquals(listOf(POLKU, PROFIILIPOLKU), fetcher.requested)
        assertEquals("RickBlaine", muisti.seen.single().opponent.name)
    }

    @Test
    fun `profiilin epaonnistuminen sanotaan eika jateta muistia vanhaksi hiljaa`() {
        val fetcher = RecordingFetcher { path ->
            if (path == PROFIILIPOLKU) DgResponse.ServerError(503) else DgResponse.Ok(TURNAUSLISTA)
        }
        val muisti = RecordingMemory().apply { seen += NAHTY }
        val malli = malli(fetcher, profilePath = PROFIILIPOLKU, memory = muisti)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onOpened()
            advanceUntilIdle()
            malli.refresh()
            advanceUntilIdle()
        }

        assertEquals(OpponentsRefreshState.Failed(Failure.Server(503)), malli.opponents.value)
        assertEquals(listOf(NAHTY), muisti.seen)
        // Turnauslista latautui silti: kaksi sivua, kaksi kohtaloa.
        assertTrue(malli.state.value is OwnTournamentsUiState.Loaded)
    }

    @Test
    fun `sivua ei haeta ennen kuin segmentti avataan`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(TURNAUSLISTA) }
        val malli = malli(fetcher)

        runTest(dispatcher) { advanceUntilIdle() }

        // Polku on tiedossa heti otteluluettelosta, mutta sivua ei haeta sen varalta
        // että joku avaisi segmentin.
        assertTrue(fetcher.requested.isEmpty())
        assertEquals(OwnTournamentsUiState.NoPath, malli.state.value)
    }

    @Test
    fun `avaus hakee sivun kerran ja lista jasentyy`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(TURNAUSLISTA) }
        val malli = malli(fetcher)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onOpened()
            advanceUntilIdle()
            // Toinen avaus ei hae uudelleen: sivu pysyy kunnes Refresh pyytää uuden.
            malli.onOpened()
            advanceUntilIdle()
        }

        assertEquals(listOf(POLKU), fetcher.requested)
        val page = (malli.state.value as OwnTournamentsUiState.Loaded).page
        assertEquals("pelaaja", page.player.name)
        assertEquals("Turnaus", page.rows.single().name)
    }

    @Test
    fun `ilman polkua ei haeta mitaan eika osoitetta kootaa`() {
        val fetcher = RecordingFetcher { DgResponse.Ok(TURNAUSLISTA) }
        val malli = malli(fetcher, path = null)

        runTest(dispatcher) {
            advanceUntilIdle()
            malli.onOpened()
            advanceUntilIdle()
        }

        assertTrue(fetcher.requested.isEmpty())
        assertEquals(OwnTournamentsUiState.NoPath, malli.state.value)
    }

    @Test
    fun `ladattu lista kirjoittaa lukeman polkuineen, epaonnistunut haku ei tyhjenna sita`() {
        var answer: DgResponse = DgResponse.Ok(TURNAUSLISTA)
        val fetcher = RecordingFetcher { answer }
        val sink = MutableStateFlow<OwnTournamentCount?>(null)
        val malli = malli(fetcher, sink = sink)

        runTest(dispatcher) {
            advanceUntilIdle()
            // Ennen avausta lukemaa ei ole, koska sivua ei ole haettu.
            assertEquals(null, sink.value)
            malli.onOpened()
            advanceUntilIdle()
            assertEquals(OwnTournamentCount(POLKU, 1), sink.value)

            answer = DgResponse.Offline("katko")
            malli.refresh()
            advanceUntilIdle()
        }

        assertTrue(malli.state.value is OwnTournamentsUiState.Failed)
        // Edellinen lista on yhä tuorein nähty, joten lukema jää.
        assertEquals(OwnTournamentCount(POLKU, 1), sink.value)
    }

    private companion object {
        const val POLKU = "/bg/userevent/10001"

        /** Synteettinen oma turnauslista, sama muoto kuin PageViewModelTestissa. */
        const val PROFIILIPOLKU = "/bg/user/10001"

        val NAHTY = SeenMatch(MatchId("1"), fi.tommi.dg.domain.PlayerRef(name = "joku"), "1/3", 5, null, 1_000)

        /** Oma profiili yhdellä käynnissä olevalla ottelulla, sarakkeet kuten sivulla. */
        val PROFIILI = """
            <html><head><title>DailyGammon: Info on player pelaaja</title></head><body>
            <table><tr><th align=right>Location</th><td>paikka</td></tr></table>
            <a href=/bg/userevent/10001>active tournaments</a>
            <TABLE CELLSPACING=3><CAPTION><FONT SIZE=+1>Active games:</font></caption>
            <TR><TH align=left>#</TH><TH align=left>Event</TH><TH>Grace</TH><TH>Time Pool</TH>
            <TH>Round</TH><TH align=right>Length</TH><TH align=left>Opponent</TH></TR>
            <TR><TD>1.</TD><TD><A HREF=/bg/event/111128>Turnaus</A></TD><TD>20:15</TD>
            <TD>632:00</TD><TD>4/5</TD><TD align=right>21</TD>
            <TD><A HREF=/bg/user/90002>RickBlaine</A></TD>
            <TD><A HREF=/bg/game/5305249/0/list>Review</A></TD></TR>
            </TABLE>
            </body></html>
        """.trimIndent()

        val TURNAUSLISTA = """
            <html><head><title>DailyGammon Events For pelaaja</title></head><body>
            <h2>Active Tournaments for <a href=/bg/user/10001>pelaaja</a></h2>
            <table><tr><th><th>#<th><th align=left>Event<th>Wins
            <tr><td width=30><td>1.<td width=10><td><a href=/bg/event/111128>Turnaus</a>
            <td align=center>2</td></tr>
            </table>
            </body></html>
        """.trimIndent()
    }
}

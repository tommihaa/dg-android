package fi.tommi.dg.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.data.db.DgDatabase
import fi.tommi.dg.domain.ConnectionDrop
import fi.tommi.dg.domain.MatchId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Katkohistoria oikeaa SQLiteä vasten.
 *
 * **Väitteet koskevat järjestystä ja rajaa eikä tallennusta.** Se mikä voi mennä väärin on
 * että lukija näkee vanhimman ensin, tai että taulu kasvaa rajatta laitteella jonka verkko
 * pätkii. Syy kulkee rivin mukana tekstinä, koska mikään ei lue sitä logiikaksi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DropLogTest {

    private lateinit var db: DgDatabase
    private lateinit var loki: DropLog

    @Before
    fun avaaKanta() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DgDatabase::class.java,
        ).build()
        loki = RoomDropLog(db.connectionDrops())
    }

    @After
    fun suljeKanta() {
        db.close()
    }

    @Test
    fun `uusin ensin ja syy kulkee mukana`() = runTest {
        loki.record(katko(1_000, "Roll Dice", "UnknownHostException"))
        loki.record(katko(2_000, "Submit Move", "SocketTimeoutException", ottelu = null))

        val rivit = loki.observeNewestFirst().first()
        assertEquals(2, rivit.size)
        assertEquals("Submit Move", rivit[0].submit)
        assertEquals("SocketTimeoutException", rivit[0].cause)
        assertNull(rivit[0].matchId)
        assertEquals(OTTELU, rivit[1].matchId)
    }

    @Test
    fun `sata uusinta jaa ja vanhin putoaa`() = runTest {
        repeat(DropLog.KEEP + 3) { i -> loki.record(katko(1_000L + i, "Roll Dice", "ConnectException")) }

        assertEquals(DropLog.KEEP, loki.count())
        val rivit = loki.observeNewestFirst().first()
        assertEquals(1_000L + DropLog.KEEP + 2, rivit.first().atEpochMillis)
        assertEquals(1_003L, rivit.last().atEpochMillis)
    }

    private fun katko(hetki: Long, nappi: String, syy: String, ottelu: MatchId? = OTTELU) =
        ConnectionDrop(id = 0, atEpochMillis = hetki, matchId = ottelu, submit = nappi, cause = syy)

    private companion object {
        val OTTELU = MatchId("5302842")
    }
}

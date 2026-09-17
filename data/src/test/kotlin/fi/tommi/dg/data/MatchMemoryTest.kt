package fi.tommi.dg.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.data.db.DgDatabase
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.PlayerRef
import fi.tommi.dg.domain.SeenMatch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ottelumuisti oikeaa SQLiteä vasten.
 *
 * **Väitteet koskevat yhdistämistä eikä tallennusta.** Se mikä voi mennä väärin on että
 * laudalta tullut vajaa havainto pyyhkii luettelosta kirjatun kokonaisen: lauta ei aina
 * tiedä vastustajaa eikä kokonaismäärää, ja null ei saa voittaa arvoa.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MatchMemoryTest {

    private lateinit var db: DgDatabase
    private lateinit var muisti: MatchMemory

    @Before
    fun avaaKanta() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DgDatabase::class.java,
        ).build()
        muisti = RoomMatchMemory(db.seenMatches())
    }

    @After
    fun suljeKanta() {
        db.close()
    }

    @Test
    fun `uusi kentta voittaa ja puuttuva jattaa vanhan`() = runTest {
        muisti.remember(
            SeenMatch(OTTELU, PlayerRef(name = "RickBlaine", userId = "90002"), "4/5", 21, "The Marathon #4305", 1_000),
        )
        // Lauta ilman omaa nimeä: vastustaja tyhjä, kierros edennyt, kokonaismäärää ei.
        muisti.remember(SeenMatch(OTTELU, PlayerRef(), "5", null, null, 2_000))

        val rivi = muisti.observeAll().first().getValue(OTTELU)
        assertEquals("RickBlaine", rivi.opponent.name)
        assertEquals("5", rivi.round)
        assertEquals(21, rivi.matchLength)
        assertEquals("The Marathon #4305", rivi.eventName)
        assertEquals(2_000, rivi.seenAtEpochMillis)
    }

    @Test
    fun `yksi rivi per ottelu`() = runTest {
        muisti.remember(SeenMatch(OTTELU, PlayerRef(name = "a"), "1/5", 21, null, 1_000))
        muisti.remember(SeenMatch(OTTELU, PlayerRef(name = "a"), "2/5", 21, null, 2_000))
        muisti.remember(SeenMatch(MatchId("2"), PlayerRef(name = "b"), "1/3", 5, null, 3_000))

        assertEquals(2, muisti.count())
        assertEquals("2/5", muisti.observeAll().first().getValue(OTTELU).round)
    }

    private companion object {
        val OTTELU = MatchId("5302842")
    }
}

package fi.tommi.dg.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.data.db.DgDatabase
import fi.tommi.dg.domain.CheckerPosition
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.MatchId
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
 * Merkityt asemat oikeaa SQLiteä vasten.
 *
 * **Väitteet koskevat elinkaarta**, kuten `ReminderBookTest`issä, mutta elinkaari on
 * päinvastainen: pelin vaihtuminen **ei** vie merkkiä listalta, ja vain poisto vie.
 * Jos kysely tai siivous kopioitaisiin muistutuksista sellaisenaan, merkki katoaisi juuri
 * silloin kun sitä tarvitaan, eli ottelun päätyttyä.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarkBookTest {

    private lateinit var db: DgDatabase
    private lateinit var kirja: MarkBook

    @Before
    fun avaaKanta() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DgDatabase::class.java,
        ).build()
        kirja = RoomMarkBook(db.markedPositions())
    }

    @After
    fun suljeKanta() {
        db.close()
    }

    @Test
    fun `tuonti lisaa vain puuttuvat merkit ja sailyttaa aseman`() = runTest {
        kirja.add(PELI, 377, "recube?", 1_000)
        val asema = CheckerPosition.START

        val lisatty = kirja.addMissing(
            listOf(
                fi.tommi.dg.domain.MarkedPosition(0, PELI, 377, "recube?", 1_000),
                fi.tommi.dg.domain.MarkedPosition(0, PELI, 380, "", 2_000, asema),
            ),
        )

        assertEquals(1, lisatty)
        assertEquals(2, kirja.count())
        assertEquals(asema, kirja.all().single { it.moveNumber == 380 }.position)
        // Sama tiedosto toiseen kertaan ei kirjoita mitään.
        assertEquals(0, kirja.addMissing(kirja.all()))
        assertEquals(2, kirja.count())
    }

    @Test
    fun `pelin merkit nakyvat siirtonumeron jarjestyksessa`() = runTest {
        kirja.add(PELI, 380, "recube?", 2_000)
        kirja.add(PELI, 377, "", 1_000)

        assertEquals(listOf(377, 380), kirja.observe(PELI).first().map { it.moveNumber })
    }

    @Test
    fun `toisen pelin merkki sailyy listalla vaikka se ei nay laudalla`() = runTest {
        kirja.add(PELI, 377, "", 1_000)
        kirja.add(SEURAAVA_PELI, 402, "", 2_000)
        kirja.add(TOINEN_OTTELU, 12, "", 3_000)

        // Laudan alla vain oma peli.
        assertEquals(listOf(377), kirja.observe(PELI).first().map { it.moveNumber })
        // Listalla kaikki, uusin ensin, eikä mitään siivottu.
        assertEquals(
            listOf(12, 402, 377),
            kirja.observeAll().first().map { it.moveNumber },
        )
        assertEquals(3, kirja.count())
    }

    @Test
    fun `asema kulkee kannan lapi samana ja puuttuva jaa nulliksi`() = runTest {
        kirja.add(PELI, 377, "", 1_000, CheckerPosition.START)
        kirja.add(PELI, 380, "", 2_000)

        val rivit = kirja.observe(PELI).first()
        assertEquals(CheckerPosition.START, rivit[0].position)
        assertEquals(null, rivit[1].position)
    }

    @Test
    fun `sana leikataan ja tyhja sana kelpaa`() = runTest {
        kirja.add(PELI, 377, "  think about the cube  ", 1_000)
        kirja.add(PELI, 380, "   ", 2_000)

        assertEquals(
            listOf("think about the cube", ""),
            kirja.observe(PELI).first().map { it.note },
        )
    }

    @Test
    fun `poisto vie vain oman rivinsa`() = runTest {
        val id = kirja.add(PELI, 377, "", 1_000)
        kirja.add(PELI, 380, "", 2_000)

        kirja.remove(id)

        assertEquals(listOf(380), kirja.observeAll().first().map { it.moveNumber })
    }

    private companion object {
        val PELI = GameKey(MatchId("5316472"), opponentScore = 1, selfScore = 5)
        val SEURAAVA_PELI = GameKey(MatchId("5316472"), opponentScore = 1, selfScore = 7)
        val TOINEN_OTTELU = GameKey(MatchId("5320443"), opponentScore = 8, selfScore = 5)
    }
}

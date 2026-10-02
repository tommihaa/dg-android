package fi.tommi.dg.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingActionDaoTest {

    private lateinit var db: DgDatabase
    private lateinit var dao: PendingActionDao

    @Before
    fun avaaKanta() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DgDatabase::class.java,
        ).build()
        dao = db.pendingActions()
    }

    @After
    fun suljeKanta() {
        db.close()
    }

    private fun toiminto(
        boardPath: String,
        submit: String = "Roll Dice",
        pendingMove: String? = null,
        matchId: String? = "5302842",
        createdAt: Long,
    ) = PendingActionEntity(
        matchId = matchId,
        boardPath = boardPath,
        submit = submit,
        pendingMove = pendingMove,
        verified = false,
        createdAtEpochMillis = createdAt,
    )

    @Test
    fun `jono purkautuu vanhin ensin`() = runTest {
        // Järjestys on oikeellisuutta: siirtosarjan polun tilatunniste kasvaa askeleittain,
        // joten väärässä järjestyksessä lähetetty askel osuisi tilaan jota ei ole.
        dao.enqueue(toiminto("/bg/move/5302842/561", createdAt = 2_000))
        dao.enqueue(toiminto("/bg/move/5302842/541", createdAt = 1_000))

        assertEquals(
            listOf("/bg/move/5302842/541", "/bg/move/5302842/561"),
            dao.oldestFirst().map { it.boardPath },
        )
    }

    @Test
    fun `samalla hetkella luodut sailyttavat lisaysjarjestyksen`() = runTest {
        // Kellon tarkkuus voi antaa kahdelle napautukselle saman millisekunnin.
        // Silloin juokseva id ratkaisee, eikä järjestys jää satunnaiseksi.
        val eka = dao.enqueue(toiminto("/bg/move/5302842/541", createdAt = 1_000))
        val toka = dao.enqueue(toiminto("/bg/move/5302842/551", createdAt = 1_000))

        assertEquals(listOf(eka, toka), dao.oldestFirst().map { it.id })
    }

    @Test
    fun `painallus talletetaan sivun omilla sanoilla`() = runTest {
        // Rivi kuvaa painallusta eikä valmista osoitetta: napin nimi ja kokoamistila ovat
        // sivulta luettuja, ja niistä lähetys rakennetaan uudelleen tuoreelta sivulta. Jos
        // tähän ilmestyy koottu osoite, portin tae on purkautunut kannan kautta.
        dao.enqueue(
            toiminto("/bg/move/5302842/561", submit = "Submit Move", pendingMove = "rrmm", createdAt = 1_000)
        )

        val rivi = dao.oldestFirst().single()
        assertEquals("/bg/move/5302842/561", rivi.boardPath)
        assertEquals("Submit Move", rivi.submit)
        assertEquals("rrmm", rivi.pendingMove)
        assertFalse(rivi.verified)
    }

    @Test
    fun `kokoamistilan puuttuminen sailyy nullina`() = runTest {
        // Null ja tyhjä eivät ole sama asia: lomake jolla ei ole piilokenttää on eri kysymys
        // kuin lomake jolla on, ja uudelleenyritys vertaa juuri tätä arvoa tuoreeseen sivuun.
        dao.enqueue(toiminto("/bg/move/5302842/541", submit = "Roll Dice", createdAt = 1_000))

        assertNull(dao.oldestFirst().single().pendingMove)
    }

    @Test
    fun `uusi toiminto on ilman katkoksen syyta`() = runTest {
        // Syy tulee riville vasta kun kutsuja kirjaa sen (`ActionQueue.record`), eikä kanta
        // keksi sitä itse. Yrityslaskuria ei enää kasvateta mistään: uusintanappi poistui
        // 8.9.2026, ja sarake jäi kantaan vain migraation välttämiseksi.
        dao.enqueue(toiminto("/bg/move/5302842/541", createdAt = 1_000))

        val rivi = dao.oldestFirst().single()
        assertEquals(0, rivi.attempts)
        assertNull(rivi.lastErrorText)
    }

    @Test
    fun `onnistunut lahetys poistaa rivin jonosta`() = runTest {
        val id = dao.enqueue(toiminto("/bg/move/5302842/541", createdAt = 1_000))

        dao.remove(id)

        assertEquals(0, dao.count())
        assertEquals(emptyList<PendingActionEntity>(), dao.observeOldestFirstOnce())
    }

    @Test
    fun `ottelun rivit poistuvat yhdella kutsulla ja muut jaavat`() = runTest {
        // Tämä on korvaamisen puolikas: uusi kirjaus poistaa ottelun vanhat rivit, koska ne
        // kuvaisivat samaa tekemätöntä tekoa toistamiseen.
        dao.enqueue(toiminto("/bg/move/5302842/541", createdAt = 1_000))
        dao.enqueue(toiminto("/bg/move/7000002/117", matchId = "7000002", createdAt = 2_000))

        dao.removeByMatch("5302842")

        assertEquals(listOf("7000002"), dao.oldestFirst().map { it.matchId })
    }

    @Test
    fun `ottelun rivit haetaan erikseen`() = runTest {
        dao.enqueue(toiminto("/bg/move/5302842/541", createdAt = 1_000))
        dao.enqueue(toiminto("/bg/move/7000002/117", matchId = "7000002", createdAt = 2_000))

        assertEquals(
            listOf("/bg/move/7000002/117"),
            dao.observeByMatchOnce("7000002").map { it.boardPath },
        )
    }

    @Test
    fun `ottelutta luotu toiminto on sallittu`() = runTest {
        // Kaikki muuttavat toiminnot eivät liity otteluun, esimerkiksi profiilin kautta
        // lähetetty Quick Message.
        dao.enqueue(
            toiminto("/bg/sendmsg/1234", submit = "Send", matchId = null, createdAt = 1)
        )

        assertNull(dao.oldestFirst().single().matchId)
    }
}

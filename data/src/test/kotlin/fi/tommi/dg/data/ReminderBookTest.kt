package fi.tommi.dg.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.data.db.DgDatabase
import fi.tommi.dg.domain.GameKey
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Muistutukset oikeaa SQLiteä vasten.
 *
 * **Väitteet koskevat elinkaarta eikä tallennusta.** Rivin kirjoittaminen ja lukeminen ei
 * voi mennä väärin ilman että Room valittaa käännösaikana. Se mikä voi mennä väärin on
 * pelin vaihtuminen: jos kysely osuisi väärään pistepariin, muistutus näkyisi pelissä johon
 * se ei kuulu, ja jos siivous osuisi liian laajalle, se veisi rivin jonka piti näkyä.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderBookTest {

    private lateinit var db: DgDatabase
    private lateinit var kirja: ReminderBook

    @Before
    fun avaaKanta() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DgDatabase::class.java,
        ).build()
        kirja = RoomReminderBook(db.reminders())
    }

    @After
    fun suljeKanta() {
        db.close()
    }

    @Test
    fun `saman pelin muistutukset nakyvat kirjoitusjarjestyksessa`() = runTest {
        kirja.add(PELI, "Think about doubling", 1_000)
        kirja.add(PELI, "Ask about the bar point", 2_000)

        assertEquals(
            listOf("Think about doubling", "Ask about the bar point"),
            kirja.observe(PELI).first().map { it.text },
        )
    }

    @Test
    fun `pisteiden muuttuminen vie muistutukset nakyvista`() = runTest {
        kirja.add(PELI, "Think about doubling", 1_000)

        // Sama ottelu, seuraava peli: vastustaja voitti edellisen. Mitään ei poistettu,
        // eikä tarvinnut: elinkaari on kyselyssä.
        val seuraava = PELI.copy(opponentScore = PELI.opponentScore + 1)
        assertTrue(kirja.observe(seuraava).first().isEmpty())
    }

    @Test
    fun `toisen ottelun muistutus ei nay tassa`() = runTest {
        kirja.add(PELI.copy(matchId = MatchId("5312554")), "Toinen ottelu", 1_000)
        assertTrue(kirja.observe(PELI).first().isEmpty())
    }

    @Test
    fun `poisto vie tasan yhden rivin`() = runTest {
        kirja.add(PELI, "Ensimmäinen", 1_000)
        kirja.add(PELI, "Toinen", 2_000)
        val rivit = kirja.observe(PELI).first()

        kirja.remove(rivit.first().id)

        assertEquals(listOf("Toinen"), kirja.observe(PELI).first().map { it.text })
    }

    @Test
    fun `uusi kirjoitus siivoaa saman ottelun vanhentuneet rivit`() = runTest {
        kirja.add(PELI, "Vanha peli", 1_000)
        val seuraava = PELI.copy(selfScore = PELI.selfScore + 1)

        kirja.add(seuraava, "Uusi peli", 2_000)

        // Siivous koskee vain samaa ottelua ja vain muita pelejä. Näkyvyys ei muutu tästä,
        // koska vanha rivi oli jo näkymätön; kanta ei vain kasva loputtomiin.
        assertEquals(1, kirja.count())
        assertEquals(listOf("Uusi peli"), kirja.observe(seuraava).first().map { it.text })
    }

    @Test
    fun `siivous ei koske toiseen otteluun`() = runTest {
        val toinen = GameKey(MatchId("5312554"), opponentScore = 0, selfScore = 0)
        kirja.add(toinen, "Toisen ottelun rivi", 1_000)

        kirja.add(PELI, "Tämän ottelun rivi", 2_000)

        assertEquals(listOf("Toisen ottelun rivi"), kirja.observe(toinen).first().map { it.text })
    }

    @Test
    fun `tyhja teksti ei tallennu`() = runTest {
        assertFalse(kirja.add(PELI, "   ", 1_000))
        assertEquals(0, kirja.count())
    }

    // --- Tuonti (16.9.2026): vain lisää ---

    @Test
    fun `tuonti kirjoittaa vain puuttuvat eika koske siivoukseen`() = runTest {
        // Laitteella on jo yksi rivi ja toisen pelin vanha rivi. Tuonti tuo saman rivin
        // uudestaan (ohitetaan), uuden rivin (kirjoitetaan) ja kolmannen pelin rivin
        // (kirjoitetaan): saman ottelun muita pelejä ei siivota, koska tuotu rivi ei ole
        // todiste meneillään olevasta pelistä.
        val vanhaPeli = GameKey(MatchId("5302842"), opponentScore = 7, selfScore = 7)
        kirja.add(vanhaPeli, "Vanha rivi", 500)
        kirja.add(PELI, "Sama rivi", 1_000)
        assertEquals(1, kirja.count()) // add siivosi vanhan pelin, kuten kuuluu

        val lisatty = kirja.addMissing(
            listOf(
                Reminder(0, PELI, "Sama rivi", 1_000),
                Reminder(0, PELI, "Uusi rivi", 2_000),
                Reminder(0, vanhaPeli, "Vanha rivi", 500),
                Reminder(0, PELI, "   ", 3_000),
            ),
        )

        assertEquals(2, lisatty)
        assertEquals(3, kirja.count())
        assertEquals(listOf("Sama rivi", "Uusi rivi"), kirja.observe(PELI).first().map { it.text })
        // Toinen tuonti samasta tiedostosta ei kirjoita mitään.
        assertEquals(0, kirja.addMissing(listOf(Reminder(0, PELI, "Uusi rivi", 2_000))))
        assertEquals(3, kirja.count())
    }

    @Test
    fun `all antaa viennille kaikki rivit id ja teksti mukana`() = runTest {
        kirja.add(PELI, "Eka", 1_000)
        kirja.add(PELI, "Toka", 2_000)

        val kaikki = kirja.all()

        assertEquals(listOf("Eka", "Toka"), kaikki.map { it.text })
        assertTrue(kaikki.all { it.id != 0L })
    }

    private companion object {
        val PELI = GameKey(MatchId("5302842"), opponentScore = 8, selfScore = 7)
    }
}

package fi.tommi.dg.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room ajetaan Robolectricillä tavallisena JVM-testinä, koska laitetta ei ole. Se riittää:
 * kanta on oikea SQLite ja Roomin generoima koodi on sama joka menee laitteelle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DgDatabaseTest {

    private companion object {
        /** Kaikki rivit samalle tilille: tilirajaus on `MessageArchiveTest`in väite, ei tämän. */
        const val TILI = "tommih"
    }

    private lateinit var db: DgDatabase

    @Before
    fun avaaKanta() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DgDatabase::class.java,
        ).build()
    }

    @After
    fun suljeKanta() {
        db.close()
    }

    private fun viesti(
        body: String = "Good roll",
        sender: String = "vastustaja",
        matchId: String? = "5302842",
        source: MessageSource = MessageSource.GAME_MESSAGE,
        rawHeader: String = "You have received the following quick message from vastustaja",
        receivedAt: Long = 1_000,
        opponent: String? = "vastustaja",
    ) = Message(
        matchId = matchId?.let(::MatchId),
        opponent = opponent,
        sender = sender,
        timestampText = "Jul 29 2026 20:14",
        body = body,
        rawHeader = rawHeader,
        source = source,
        receivedAtEpochMillis = receivedAt,
    )

    @Test
    fun `sama viesti kahdesti tuottaa yhden rivin`() = runTest {
        val dao = db.messages()
        val sama = viesti()

        dao.insertIfNew(sama.toEntity(TILI))
        dao.insertIfNew(sama.toEntity(TILI))

        assertEquals(1, dao.count())
    }

    @Test
    fun `sama teksti eri saapumishetkella on eri rivi`() = runTest {
        // Sopimusmuutos 1.8.2026: kohteliaisuus toistuu, joten "Great match!" kahdesti
        // samalta pelaajalta on kaksi viestiä eikä yksi. Pikaviestisivulla ei ole
        // aikaleimaa, joten ilman saapumishetkeä nämä olisivat erottamattomat.
        val dao = db.messages()

        dao.insertIfNew(viesti(body = "Great match!", receivedAt = 1_000).toEntity(TILI))
        dao.insertIfNew(viesti(body = "Great match!", receivedAt = 2_000).toEntity(TILI))

        assertEquals(2, dao.count())
    }

    @Test
    fun `uudelleentallennus ei siirra saapumisaikaa`() = runTest {
        // IGNORE eikä REPLACE. Sama olio tallennettuna kahdesti on yhä sama viesti, ja
        // uusinnan on oltava turvallinen: epäonnistunut kirjoitus yritetään uudelleen.
        val dao = db.messages()
        val sama = viesti(receivedAt = 1_000)

        assertNotEquals(-1L, dao.insertIfNew(sama.toEntity(TILI)))
        val toinenTulos = dao.insertIfNew(sama.toEntity(TILI))

        assertEquals(-1L, toinenTulos)
        assertEquals(1_000L, dao.byId(sama.id)!!.storedAtEpochMillis)
    }

    @Test
    fun `eri sisalto tuottaa eri rivin`() = runTest {
        val dao = db.messages()

        dao.insertIfNew(viesti(body = "Good roll").toEntity(TILI))
        dao.insertIfNew(viesti(body = "Nice roll").toEntity(TILI))

        assertEquals(2, dao.count())
    }

    @Test
    fun `kierros kannan lapi sailyttaa kentat`() = runTest {
        val dao = db.messages()
        val alkuperainen = viesti(source = MessageSource.ANNOUNCEMENT, matchId = null)

        dao.insertIfNew(alkuperainen.toEntity(TILI))
        val palautettu = dao.byId(alkuperainen.id)!!.toDomain()

        assertEquals(alkuperainen, palautettu)
        assertNull(palautettu.matchId)
        assertEquals(MessageSource.ANNOUNCEMENT, palautettu.source)
        // Tunniste syntyy uudelleen samana, eli saapumishetki kulkee kannan läpi ehjänä.
        assertEquals(alkuperainen.id, palautettu.id)
    }

    @Test
    fun `laji talletetaan nimena eika numerona`() = runTest {
        val dao = db.messages()
        val ilmoitus = viesti(source = MessageSource.ANNOUNCEMENT)

        dao.insertIfNew(ilmoitus.toEntity(TILI))

        assertEquals("ANNOUNCEMENT", dao.byId(ilmoitus.id)!!.source)
    }

    @Test
    fun `sama teksti eri lajilla on eri viesti`() = runTest {
        // Lajilla on merkitystä juuri siksi että ilmoitukset eivät saa hukuttaa
        // henkilöviestejä. Siksi sen on kuuluttava tunnisteeseen.
        val peliviesti = viesti(source = MessageSource.GAME_MESSAGE)
        val ilmoitus = viesti(source = MessageSource.ANNOUNCEMENT)

        assertNotEquals(peliviesti.id, ilmoitus.id)

        val dao = db.messages()
        dao.insertIfNew(listOf(peliviesti.toEntity(TILI), ilmoitus.toEntity(TILI)))
        assertEquals(2, dao.count())
    }

    @Test
    fun `tuntematon otsikko sailyy raakana ja omana lajinaan`() = runTest {
        // Päätös 9.8.2026: oma kenttä ja oma laji. Tämä testi on se lupaus jonka
        // InboxParserin kommentti antoi ennen kuin se oli pidettävissä: tuntemattoman
        // otsikon saanut viesti voidaan tallentaa menettämättä otsikkoa.
        val dao = db.messages()
        val outo = viesti(
            source = MessageSource.UNKNOWN,
            rawHeader = "You have received something entirely new",
        )

        dao.insertIfNew(outo.toEntity(TILI))
        val palautettu = dao.byId(outo.id)!!.toDomain()

        assertEquals("You have received something entirely new", palautettu.rawHeader)
        assertEquals(MessageSource.UNKNOWN, palautettu.source)
        assertEquals(outo.id, palautettu.id)
    }

    @Test
    fun `sama teksti eri otsikolla on eri rivi`() = runTest {
        // Otsikko on osa tunnistetta. Ilman tätä kaksi eri otsikon alla saapunutta
        // samasanaista viestiä sulautuisivat yhdeksi ja jälkimmäinen katoaisi hiljaa,
        // eli sama vika jonka saapumishetki korjasi 1.8.2026.
        val dao = db.messages()

        dao.insertIfNew(viesti(rawHeader = "telegram").toEntity(TILI))
        dao.insertIfNew(viesti(rawHeader = "quick").toEntity(TILI))

        assertEquals(2, dao.count())
    }

    @Test
    fun `viestit listataan uusin ensin`() = runTest {
        val dao = db.messages()
        dao.insertIfNew(viesti(body = "vanha", receivedAt = 1_000).toEntity(TILI))
        dao.insertIfNew(viesti(body = "uusi", receivedAt = 2_000).toEntity(TILI))

        val rivit = dao.observeAllOnce()

        assertEquals(listOf("uusi", "vanha"), rivit.map { it.body })
    }

    @Test
    fun `ottelun viestit listataan vanhin ensin ja vain omasta ottelusta`() = runTest {
        // Ottelunäkymä on keskustelu, joten siellä lukusuunta on toinen kuin listalla.
        val dao = db.messages()
        dao.insertIfNew(viesti(body = "eka", matchId = "1", receivedAt = 1_000).toEntity(TILI))
        dao.insertIfNew(viesti(body = "toka", matchId = "1", receivedAt = 2_000).toEntity(TILI))
        dao.insertIfNew(viesti(body = "muu ottelu", matchId = "2", receivedAt = 3_000).toEntity(TILI))

        val rivit = dao.observeByMatchOnce("1")

        assertEquals(listOf("eka", "toka"), rivit.map { it.body })
    }

    @Test
    fun `tyhja arkisto ei anna reunapaivaa`() = runTest {
        // Null eikä nolla. Nolla olisi vuoden 1970 päivämäärä, eli tyhjä arkisto
        // väittäisi ulottuvansa johonkin. Juuri sitä väitettä tämä kysely on vastaan.
        assertNull(db.messages().newestStoredAtOnce())
    }

    @Test
    fun `reunapaiva on uusin kirjoitushetki`() = runTest {
        val dao = db.messages()
        dao.insertIfNew(viesti(body = "vanha", receivedAt = 1_000).toEntity(TILI))
        dao.insertIfNew(viesti(body = "uusi", receivedAt = 5_000).toEntity(TILI))
        dao.insertIfNew(viesti(body = "valissa", receivedAt = 3_000).toEntity(TILI))

        // Viimeksi kirjoitettu ei ole uusin, ja reuna on silti uusin eikä viimeksi
        // kirjoitettu. Ero on olemassa, koska jonoon kertyneet viestit noudetaan yhdessä.
        assertEquals(5_000L, dao.newestStoredAtOnce())
    }
}

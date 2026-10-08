package fi.tommi.dg.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fi.tommi.dg.data.db.DgDatabase
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.Message
import fi.tommi.dg.domain.MessageSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessageArchiveTest {

    private lateinit var db: DgDatabase
    private lateinit var arkisto: MessageArchive

    private companion object {
        const val TILI = "tommih"
        const val TOINEN_TILI = "sisko"
    }

    @Before
    fun avaaKanta() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DgDatabase::class.java,
        ).build()
        // Ei kelloa: arkisto ei enää leimaa aikaa. Saapumishetki on osa tunnistetta, joten
        // se annetaan viestiä muodostettaessa. Jos arkisto leimaisi, sama viesti saisi eri
        // tunnisteen joka yrityksellä ja uusinta tuottaisi duplikaatin.
        arkisto = RoomMessageArchive(db.messages())
    }

    @After
    fun suljeKanta() {
        db.close()
    }

    private fun viesti(
        body: String,
        receivedAt: Long = 1_000,
        opponent: String? = "vastustaja",
    ) = Message(
        matchId = MatchId("5302842"),
        opponent = opponent,
        sender = "vastustaja",
        timestampText = "Jul 29 2026 20:14",
        body = body,
        rawHeader = "You have received the following quick message from vastustaja",
        source = MessageSource.GAME_MESSAGE,
        receivedAtEpochMillis = receivedAt,
    )

    @Test
    fun `viesti on kannassa jo silla hetkella kun se palautetaan naytettavaksi`() = runTest {
        // Tämä on koko arkiston olemassaolon syy. Haku on tuhoava, joten näytetty mutta
        // tallentamatta jäänyt viesti olisi mennyt lopullisesti.
        val naytettava = arkisto.archive(viesti("Good roll"), TILI)

        assertEquals(1, arkisto.count())
        assertEquals("Good roll", naytettava.body)
    }

    @Test
    fun `saman olion tallennus kahdesti ei kasvata kantaa`() = runTest {
        // Tämä on se idempotenssi jota oikeasti tarvitaan: epäonnistuneen kirjoituksen
        // uusiminen samalla oliolla on turvallista.
        val sama = viesti("Good roll")

        arkisto.archive(sama, TILI)
        val toinen = arkisto.archive(sama, TILI)

        assertEquals(1, arkisto.count())
        assertEquals(sama, toinen)
    }

    @Test
    fun `sama kohteliaisuus kahdesti sailyy kahtena`() = runTest {
        // Sopimusmuutos 1.8.2026. Ilman saapumishetkeä jälkimmäinen katoaisi hiljaa, ja
        // juuri lyhyt toistuva viesti on se jota tämä sovellus on tehty säilyttämään.
        arkisto.archive(viesti("Great match!", receivedAt = 1_000), TILI)
        arkisto.archive(viesti("Great match!", receivedAt = 2_000), TILI)

        assertEquals(2, arkisto.count())
    }

    @Test
    fun `tuonti kirjoittaa vain puuttuvat ja kertoo maaran`() = runTest {
        // Tommin päätös 16.9.2026: tuonti vain lisää. Jo kannassa oleva rivi ei muutu edes
        // tililtään, vaikka tiedosto sanoisi toisen tilin.
        val vanha = viesti("Good roll", receivedAt = 1_000)
        arkisto.archive(vanha, TILI)

        val lisatty = arkisto.addMissing(listOf(vanha, viesti("Great match!", receivedAt = 2_000)), "toinen")

        assertEquals(1, lisatty)
        assertEquals(2, arkisto.count())
        assertEquals(listOf("Good roll"), arkisto.observeAll(TILI).first().map { it.body })
        assertEquals(listOf("Great match!"), arkisto.observeAll("toinen").first().map { it.body })
        assertEquals(0, arkisto.addMissing(listOf(vanha), TILI))
    }

    @Test
    fun `tyhja era ei kirjoita mitaan`() = runTest {
        assertEquals(emptyList<Message>(), arkisto.archive(emptyList(), TILI))
        assertEquals(0, arkisto.count())
    }

    @Test
    fun `era tallentuu kokonaan`() = runTest {
        val era = listOf(viesti("eka"), viesti("toka"), viesti("kolmas"))

        val palautettu = arkisto.archive(era, TILI)

        assertEquals(3, arkisto.count())
        assertEquals(era, palautettu)
    }

    @Test
    fun `toisen tilin viestit eivat nay eivatka vie kumppanilistaan`() = runTest {
        // Tommin päätös 15.9.2026: perheenjäsenet pelaavat samalta laitteelta eri tileillä,
        // ja sama pelaaja voi pelata eri turnauksia eri tileillä. Kanta on yhteinen, luku ei.
        arkisto.archive(viesti("oma", opponent = "vastustaja"), TILI)
        arkisto.archive(viesti("siskon", receivedAt = 2_000, opponent = "toinen"), TOINEN_TILI)

        assertEquals(listOf("oma"), arkisto.observeAll(TILI).first().map { it.body })
        assertEquals(listOf("siskon"), arkisto.observeAll(TOINEN_TILI).first().map { it.body })
        assertEquals(listOf("vastustaja"), arkisto.observeOpponents(TILI).first())
        assertEquals(emptyList<Message>(), arkisto.observeByOpponent("toinen", TILI).first())
        // Reunapäivä on tilin oma: siskon uudempi viesti ei siirrä Tommin reunaa.
        assertEquals(1_000L, arkisto.observeNewestStoredAt(TILI).first())
        // Yhteinen luku on yhä kaikkien tilien summa.
        assertEquals(2, arkisto.count())
    }

    @Test
    fun `kirjautumaton nakee tyhjan arkiston eika kaikkien yhteista`() = runTest {
        arkisto.archive(viesti("oma"), TILI)

        assertEquals(emptyList<Message>(), arkisto.observeAll(null).first())
        assertNull(arkisto.observeNewestStoredAt(null).first())
    }

    @Test
    fun `omistajaton rivi siirtyy ensimmaiselle kirjautujalle kerran`() = runTest {
        // Ennen 16.9.2026 tallennettu rivi on kannassa ilman tiliä (migraatio 9→10 ei tiedä
        // nimeä). Ensimmäinen kirjautuminen ottaa sen; toinen tili ei enää saa mitään.
        arkisto.archive(viesti("vanha"), account = null)
        assertEquals(emptyList<Message>(), arkisto.observeAll(TILI).first())

        assertEquals(1, arkisto.claimUnowned(TILI))
        assertEquals(listOf("vanha"), arkisto.observeAll(TILI).first().map { it.body })

        assertEquals(0, arkisto.claimUnowned(TOINEN_TILI))
        assertEquals(emptyList<Message>(), arkisto.observeAll(TOINEN_TILI).first())
    }

    @Test
    fun `ottelun keskustelu ei rajaudu tililla`() = runTest {
        // Ottelunumero on sivustolla tilikohtainen jo itsessään, joten lautachat luetaan
        // numerolla eikä tilillä. Sama numero kahdella tilillä ei ole mahdollinen tilanne.
        arkisto.archive(viesti("laudalta"), TILI)

        assertEquals(1, arkisto.observeByMatch("5302842").first().size)
    }

    @Test
    fun `poisto piilottaa vain annetut viestit ja vain taman tilin`() = runTest {
        // Keskustelun poisto 6.10.2026: tili rajaa poiston, vaikka tunniste osuisi toisen
        // tilin riviin, ja muut keskustelut jäävät koskematta.
        val oma = arkisto.archive(viesti("oma", opponent = "ackammon"), TILI)
        val muu = arkisto.archive(viesti("muu", opponent = "kolmas"), TILI)
        val toisen = arkisto.archive(viesti("toisen", opponent = "ackammon"), TOINEN_TILI)

        val poistettu = arkisto.delete(listOf(oma.id, toisen.id), TILI, at = 5_000)

        assertEquals(1, poistettu)
        assertEquals(listOf(muu.id), arkisto.observeAll(TILI).first().map { it.id })
        assertEquals(listOf(toisen.id), arkisto.observeAll(TOINEN_TILI).first().map { it.id })
        // Pehmeä poisto: rivi on yhä kannassa ja roskakorissa poistoaikoineen.
        assertEquals(3, arkisto.count())
        assertEquals(
            listOf(DeletedMessage(oma, 5_000)),
            arkisto.observeDeleted(TILI).first(),
        )
    }

    @Test
    fun `poistettu ei nay missaan ruudun kyselyssa`() = runTest {
        val poistettava = arkisto.archive(viesti("pois", opponent = "ackammon"), TILI)

        arkisto.delete(listOf(poistettava.id), TILI, at = 5_000)

        assertEquals(emptyList<Message>(), arkisto.observeByMatch("5302842").first())
        assertEquals(emptyList<Message>(), arkisto.observeByOpponent("ackammon", TILI).first())
        assertEquals(emptyList<String>(), arkisto.observeOpponents(TILI).first())
        assertNull(arkisto.observeNewestStoredAt(TILI).first())
    }

    @Test
    fun `palautus tuo viestin takaisin ja tyhjentaa roskakorin`() = runTest {
        val viesti = arkisto.archive(viesti("takaisin"), TILI)
        arkisto.delete(listOf(viesti.id), TILI, at = 5_000)

        assertEquals(1, arkisto.restore(listOf(viesti.id), TILI))

        assertEquals(listOf(viesti.id), arkisto.observeAll(TILI).first().map { it.id })
        assertEquals(emptyList<DeletedMessage>(), arkisto.observeDeleted(TILI).first())
    }

    @Test
    fun `toinen poisto ei siirra ensimmaisen poiston aikaa`() = runTest {
        // Roskakorin ryhmä ja 30 päivän laskuri ovat ensimmäisen poiston mukaisia.
        val viesti = arkisto.archive(viesti("kahdesti"), TILI)
        arkisto.delete(listOf(viesti.id), TILI, at = 5_000)

        assertEquals(0, arkisto.delete(listOf(viesti.id), TILI, at = 9_000))
        assertEquals(5_000L, arkisto.observeDeleted(TILI).first().single().deletedAtEpochMillis)
    }

    @Test
    fun `tuonti ei palauta poistettua`() = runTest {
        // Tuonti ohittaa kannassa jo olevan rivin, ja poistettu on yhä kannassa. Palautus
        // kulkee roskakorin kautta.
        val viesti = arkisto.archive(viesti("tuotava"), TILI)
        arkisto.delete(listOf(viesti.id), TILI, at = 5_000)

        assertEquals(0, arkisto.addMissing(listOf(viesti), TILI))
        assertEquals(emptyList<Message>(), arkisto.observeAll(TILI).first())
    }

    @Test
    fun `siivous poistaa vain rajaa vanhemmat poistetut`() = runTest {
        val vanha = arkisto.archive(viesti("vanha", receivedAt = 1_000), TILI)
        val tuore = arkisto.archive(viesti("tuore", receivedAt = 2_000), TILI)
        arkisto.archive(viesti("arkistossa", receivedAt = 3_000), TILI)
        arkisto.delete(listOf(vanha.id), TILI, at = 5_000)
        arkisto.delete(listOf(tuore.id), TILI, at = 9_000)

        assertEquals(1, arkisto.purgeDeletedBefore(cutoff = 6_000))

        assertEquals(2, arkisto.count())
        assertEquals(listOf(tuore.id), arkisto.observeDeleted(TILI).first().map { it.message.id })
    }

    @Test
    fun `tyhja poisto ei poista mitaan`() = runTest {
        arkisto.archive(viesti("jää"), TILI)

        assertEquals(0, arkisto.delete(emptyList(), TILI, at = 5_000))
        assertEquals(1, arkisto.observeAll(TILI).first().size)
    }
}

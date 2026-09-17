package fi.tommi.dg.net

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Merkistö molempiin suuntiin, mitattuna 4.8.2026 eikä arvattuna.
 *
 * Nämä testit kuvaavat kaksi virhettä jotka eivät kaadu vaan tallentavat viestin rikki:
 *
 * 1. **Luku UTF-8:na.** Palvelin ei kerro charsettia, joten OkHttpin `string()` olettaa
 *    UTF-8:aa. Palstasivuilta mitatut tavut eivät ole kelvollista UTF-8:aa, joten ne
 *    muuttuisivat korvausmerkiksi ennen jäsennintä. Menetys on peruuttamaton.
 * 2. **Kirjoitus UTF-8:na.** `FormBody` prosenttikoodaa aina UTF-8:na, jolloin `ä` lähtisi
 *    kahtena tavuna ja palvelin tallentaisi kaksi merkkiä yhden sijaan.
 *
 * `ISO-8859-1` ei kelpaa kummassakaan suunnassa, vaikka se on lähellä: neljä mitatuista
 * tavuista on siinä kontrollimerkkejä.
 */
class DgClientCharsetTest {

    private lateinit var server: MockWebServer
    private lateinit var client: DgClient

    /** Tavut ovat oikean sivuston omia (keskustelupalsta, 4.8.2026). */
    private val forumBytes = byteArrayOf(
        0x93.toByte(), 'C'.code.toByte(), 'h'.code.toByte(), 'a'.code.toByte(),
        'm'.code.toByte(), 'p'.code.toByte(), 's'.code.toByte(), 0x94.toByte(),
        ' '.code.toByte(), 'I'.code.toByte(), 't'.code.toByte(), 0x92.toByte(),
        's'.code.toByte(), ' '.code.toByte(), 0x97.toByte(), ' '.code.toByte(),
        'J'.code.toByte(), 0xF6.toByte(), 'n'.code.toByte(),
    )

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = DgClient(
            credentials = { DgCredentials("testi", "salainen") },
            baseUrl = server.url("/"),
            minRequestIntervalMillis = 0,
        )
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun enqueueRaw(body: ByteArray) {
        // Ilman charsetia, kuten oikea palvelin: "Content-Type: text/html".
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody(Buffer().write(body))
        )
    }

    @Test
    fun `ei-ASCII-tavut puretaan windows-1252na eika UTF-8na`() {
        enqueueRaw(forumBytes)

        val response = assertInstanceOf(DgResponse.Ok::class.java, client.fetch("/bg/forum2"))

        assertTrue(response.html.contains("“Champs”"), response.html)
        assertTrue(response.html.contains("It’s"), response.html)
        assertTrue(response.html.contains("—"), response.html)
        assertTrue(response.html.contains("Jön"), response.html)
    }

    @Test
    fun `korvausmerkkia ei saa syntya`() {
        // Tämä on se vika joka oli koodissa 4.8.2026 asti. Se ei kaada mitään.
        enqueueRaw(forumBytes)

        val response = assertInstanceOf(DgResponse.Ok::class.java, client.fetch("/bg/forum2"))

        assertFalse(response.html.contains('�'), "UTF-8-oletus hukkasi tavun")
    }

    @Test
    fun `Latin-1 ei kelpaisi, ja tama testi kertoo miksi`() {
        // Sama tavu kahdella merkistöllä. Latin-1 ei kaadu vaan tuottaa hiljaa
        // kontrollimerkin, joten ero ei näkyisi missään ennen arkistoa.
        val quote = byteArrayOf(0x92.toByte())
        assertEquals("’", String(quote, DgClient.DG_CHARSET))
        assertEquals("", String(quote, Charsets.ISO_8859_1))
    }

    @Test
    fun `lahetetty aakkonen menee yhtena tavuna`() {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<HTML>ok</HTML>"))

        client.submitForm("/bg/move/1/2", mapOf("chat" to "häh? tämä on ö"))

        val sent = server.takeRequest().body.readUtf8()
        // %E4 on yksi tavu. UTF-8 antaisi %C3%A4, eli palvelin tallentaisi kaksi merkkiä.
        assertTrue(sent.contains("%E4"), sent)
        assertFalse(sent.contains("%C3%A4"), "Lomake lähti UTF-8:na: $sent")
    }

    @Test
    fun `merkki jota merkisto ei tunne menee numeerisena viittauksena kuten selaimella`() {
        // Mitattu 5.8.2026 profiilin Location-kentällä: selain lähetti emojin muodossa
        // &#N; ja ääkkösen samassa lomakkeessa yhtenä tavuna. Tämä testi lukitsee sen
        // että teemme saman. Aiempi ?-korvaus oli väliaikaisuus ajalta jolloin oikeaa
        // muotoa ei tiedetty.
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<HTML>ok</HTML>"))

        client.submitForm("/bg/move/1/2", mapOf("chat" to "hyvin pelattu 😂"))

        val sent = server.takeRequest().body.readUtf8()
        // & on lomakkeen kenttäerotin, joten viittauksen oma & lähtee URL-koodattuna.
        assertTrue(sent.endsWith("%26%23128514%3B"), sent)
        assertFalse(sent.contains("%3F"), "Merkki hukkui kysymysmerkiksi: $sent")
    }

    @Test
    fun `emoji menee yhtena viittauksena eika kahtena sijaisparin puolikkaana`() {
        // Emoji on Javassa sijaispari. Merkki kerrallaan luettuna tästä syntyisi kaksi
        // viittausta alueelta D800-DFFF, ja lopputulos näyttäisi kelvolliselta. Testi on
        // olemassa siksi että vika olisi muuten hiljainen.
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<HTML>ok</HTML>"))

        client.submitForm("/bg/move/1/2", mapOf("chat" to "😂"))

        val sent = server.takeRequest().body.readUtf8()
        assertEquals("chat=%26%23128514%3B", sent)
    }
}
